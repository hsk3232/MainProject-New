package edu.pnu.service.statistics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.pnu.domain.AiData;
import edu.pnu.domain.AnalyzedTrip;
import edu.pnu.domain.AssetProduct;
import edu.pnu.domain.Epc;
import edu.pnu.domain.EventHistory;
import edu.pnu.dto.Triples;
import edu.pnu.repo.AiDataRepository;
import edu.pnu.repo.AnalyzedTripRepository;
import edu.pnu.repo.AssetProductRepository;
import edu.pnu.repo.EventHistoryRepository;
import edu.pnu.util.StringUtils; // 사용자 정의 유틸리티 클래스
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FindAnomalyComponet implements StatisticsInterface {

    // --- 의존성 주입 필드 ---
    private final AnalyzedTripRepository analyzedTripRepo;
    private final EventHistoryRepository eventHistoryRepo;
    private final AssetProductRepository assetProductRepo;
    private final AiDataRepository aiDataRepo;
    private final EpcSerialValidatorService epcSerialValidatorService;
    private final LogisticsFlowValidatorService logisticsFlowValidatorService;

    @Override
    public String getProcessorName() {
        return "이상 종류 판별";
    }

    @Override
    public void process(Long fileId) {
        log.info("[시작] Product 이상 분석 완료 코드 분류 작업 시작 - fileId: {}", fileId);
        
        //[추가] ★★★ 가장 중요한 해결책: 분석 시작 전, 이전 분석 결과(물)를 모두 비웁니다. ★★★
//        aiDataRepo.deleteByFileId(fileId);
        
        // 1. 기준 정보 자산(Asset) 전체 로드
        List<AssetProduct> assetProducts = assetProductRepo.findAll();

        // [1-1] 완전 일치 검출용 Key (product+company+name)의 Set
        // → DB 내 자산(Product)과 완전히 일치해야만 '정상'으로 간주
        Set<Triples> fullMatchSet = assetProducts.stream()
            .map(p -> new Triples(
                StringUtils.normalize(p.getEpcProduct()),
                StringUtils.normalize(p.getEpcCompany()),
                StringUtils.normalize(p.getProductName())
            ))
            .collect(Collectors.toSet());

        // [1-2] 부분 일치 판단용 Set (예: product, company, name 단일 기준)
        Set<String> knownProducts = assetProducts.stream().map(p -> StringUtils.normalize(p.getEpcProduct())).collect(Collectors.toSet());
        Set<String> knownCompanies = assetProducts.stream().map(p -> StringUtils.normalize(p.getEpcCompany())).collect(Collectors.toSet());
        Set<String> knownNames = assetProducts.stream().map(p -> StringUtils.normalize(p.getProductName())).collect(Collectors.toSet());
        Set<String> knownLots = epcSerialValidatorService.getAllKnownLots();

        // 2. 현재 파일에서 AI가 '이상'으로 판단한 이벤트 전체 조회 (EventHistory)
        List<EventHistory> anomalousEventsInFile = eventHistoryRepo.findAllByCsv_FileIdAndAnomalyIsTrue(fileId);

        if (anomalousEventsInFile.isEmpty()) {
            log.info("[완료] 분석할 이상 데이터가 없습니다.");
            return;
        }

        // 3-1. 분석 대상 EPC 코드 추출 (중복 방지 Set)
        Set<String> epcCodesToCheck = anomalousEventsInFile.stream()
            .map(event -> event.getEpc().getEpcCode())
            .collect(Collectors.toSet());

        // 3-2. EPC별 전체 이동 경로(Trip) 조회
        // - BatchTriggerService가 저장한 AnalyzedTrip이 신뢰 원본이므로 이를 활용
        List<AnalyzedTrip> fullTripsForEpc = analyzedTripRepo.findFullTripsByEpcCodes(new ArrayList<>(epcCodesToCheck));
        Map<String, List<AnalyzedTrip>> tripsByEpc = fullTripsForEpc.stream()
            .collect(Collectors.groupingBy(trip -> trip.getEpc().getEpcCode()));

        // ★★★ [핵심 최적화] AnalyzedTrip lookup map 생성 ★★★
        // - 각 EventHistory가 Trip의 도착점인지 빠르게 조회하기 위한 복합키 Map
        //   (epcCode + toEventTime의 문자열 조합으로 O(1) 매칭)
        Map<String, AnalyzedTrip> tripLookupMap = fullTripsForEpc.stream()
            .collect(Collectors.toMap(
                trip -> trip.getEpc().getEpcCode() + "|" +  (trip.getToEventTime() != null ? trip.getToEventTime().toString() : "NULL"),
                Function.identity(),
                (existing, replacement) -> existing // 키 중복은 없게 설계돼 있지만 혹시나 대비
            ));

        // 4. clone 검증(이동 경로 위조) - 비정상 Trip의 roadId 목록 확보
        Set<Long> cloneRoadIds = new HashSet<>();
        tripsByEpc.forEach((epcCode, tripList) -> {
            if (tripList != null && !tripList.isEmpty()) {
                cloneRoadIds.addAll(logisticsFlowValidatorService.findViolations(tripList));
            }
        });

        // [추가] 기준 자산(Product) Map - (epcProduct|epcCompany|productName) 복합키로 빠른 접근
        Map<String, AssetProduct> assetProductKeyToProduct = assetProducts.stream()
            .collect(Collectors.toMap(
                p -> StringUtils.normalize(p.getEpcProduct()) + "|" +
                     StringUtils.normalize(p.getEpcCompany()) + "|" +
                     StringUtils.normalize(p.getProductName()),
                Function.identity(),
                (existing, replacement) -> existing
            ));

        // 5. 모든 이상 Event에 대해 최종 anomalyType 판별/저장
        List<AiData> result = new ArrayList<>();
        for (EventHistory event : anomalousEventsInFile) {
            // (1) Trip 매핑키 생성
            String lookupKey = event.getEpc().getEpcCode() + "|" + event.getEventTime().toString();
            AnalyzedTrip correspondingTrip = tripLookupMap.get(lookupKey);
            String anomalyType;
            Epc epc = event.getEpc();
            
            if (correspondingTrip == null) {
            	if (!"Factory".equals(event.getBusinessStep())) {
            		anomalyType = "other";
            		result.add(AiData.builder()
            	            .eventHistory(event)
            	            .anomalyType(anomalyType)
            	            .analyzedTrip(null)
            	            .csv(event.getCsv())
            	            .build());
                log.warn("이상 이벤트(ID: {})에 해당하는 이동(AnalyzedTrip)을 찾지 못해 'other'로 분류합니다.", event.getEventId());
            	}
                continue;
            }

            

            // (2) EPC에 연결된 Product가 없는 경우: DB 설계/파싱 문제로 간주, 무조건 tamper 처리
            if (epc.getProduct() == null) {
                anomalyType = "tamper";
                log.warn("이상 이벤트(ID: {})의 EPC(Code: {})에 연결된 Product 정보가 없습니다. 'tamper'로 분류합니다.", event.getEventId(), epc.getEpcCode());
            } else {
                // (3) [1순위] clone: 정상적이지 않은 물류 이동 경로 탐지
                if (cloneRoadIds.contains(correspondingTrip.getRoadId())) {
                    anomalyType = "clone";
                }
                // (4) [2순위] 제품 정보(Product 기준) 불일치 - tamper/fake
                else {
                    Triples current = new Triples(
                        StringUtils.normalize(epc.getProduct().getEpcProduct()),
                        StringUtils.normalize(epc.getProduct().getEpcCompany()),
                        StringUtils.normalize(epc.getProduct().getProductName())
                    );

                    if (!fullMatchSet.contains(current)) {
                        // 완전 일치하는 자산이 없을 때: 부분 일치 + 기타 조건으로 tamper/fake 구분
                        int serial = -1;
                        try {
                            serial = Integer.parseInt(epc.getEpcSerial());
                        } catch (NumberFormatException e) {
                            log.warn("tamper/fake 판별 중 시리얼 번호 파싱 실패. event_id: {}, serial: {}", event.getEventId(), epc.getEpcSerial());
                        }
                        if (knownProducts.contains(current.getEpcProduct()) 
                        		// [수정사항] 이 부분 else if로 바꿔서 세부 사항 별로 라벨링 다 따로 하기. table 세부 type column 추가
                                || knownCompanies.contains(current.getEpcCompany())
                                || knownNames.contains(current.getProductName())
                                || knownLots.contains(epc.getEpcLot())
                                || epcSerialValidatorService.isPotentiallyValidSerial(serial)) {
                            anomalyType = "tamper";
                        } else {
                            anomalyType = "fake";
                        }
                    }
                    // (5) [3순위] 완전 일치할 경우에도 EPC 태그 정보 위변조 여부 추가 검사
                    else {
                        String eventProductKey =
                            StringUtils.normalize(epc.getProduct().getEpcProduct()) + "|" +
                            StringUtils.normalize(epc.getProduct().getEpcCompany()) + "|" +
                            StringUtils.normalize(epc.getProduct().getProductName());
                        AssetProduct assetProduct = assetProductKeyToProduct.get(eventProductKey);

                        // (5-1) EPC 태그에 기재된 product/company가 실제 기준 자산 Product와 일치하는지 재검증
                        boolean isTagConsistent = false;
                        if (assetProduct != null) {
                            isTagConsistent = StringUtils.normalize(assetProduct.getEpcCompany())
                                    .equals(StringUtils.normalize(epc.getProduct().getEpcCompany()))
                                    && StringUtils.normalize(assetProduct.getEpcProduct())
                                        .equals(StringUtils.normalize(epc.getProduct().getEpcProduct()));
                        }
                        if (!isTagConsistent) {
                            anomalyType = "tamper";
                        } else {
                            // (5-2) EPC 태그 정보까지 일치할 때: 생산공장, lot, serial 일치까지 검증
                            List<AnalyzedTrip> epcTrips = tripsByEpc.get(epc.getEpcCode());
                            String originalHubType = "UNKNOWN";
                            if (epcTrips != null && !epcTrips.isEmpty()) {
                                originalHubType = epcTrips.get(0).getFromHubType();
                            }
                            String factory = epcSerialValidatorService.extractFactoryFromName(originalHubType);
                            String lot = epc.getEpcLot();
                            try {
                                int serialNumber = Integer.parseInt(epc.getEpcSerial());
                                if (epcSerialValidatorService.isValid(factory, lot, serialNumber)) {
                                    anomalyType = "other";   // (5-2-1) 모든 조건이 일치하면 error(=AI 오탐)로 분류
                                } else {
                                    anomalyType = "tamper"; // (5-2-2) lot/serial이 실제 규칙과 다르면 tamper
                                }
                            } catch (NumberFormatException e) {
                                anomalyType = "tamper";
                                log.warn("시리얼 번호 파싱 실패. event_id: {}, serial: {}", event.getEventId(), epc.getEpcSerial());
                            }
                        }
                    }
                }
            }
            // [결과 저장] : EventHistory, anomalyType, Trip 정보 함께 AiData로 저장
            result.add(AiData.builder()
                .eventHistory(event)
                .anomalyType(anomalyType)
                .analyzedTrip(correspondingTrip)
                .csv(event.getCsv())
                .build());
        }

        // 6. 결과를 DB에 일괄 저장
        aiDataRepo.saveAll(result);
        log.info("[완료] Product 이상 분석 완료 - 저장된 AiData 수: {}", result.size());
    }

    @Override
    public int getOrder() {
        return 2;
    }
}
