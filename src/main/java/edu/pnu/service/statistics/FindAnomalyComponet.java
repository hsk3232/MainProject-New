package edu.pnu.service.statistics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.pnu.domain.AiData;
import edu.pnu.domain.AnalyzedTrip;
import edu.pnu.domain.AssetProduct;
import edu.pnu.domain.Epc;
import edu.pnu.domain.EventHistory;
import edu.pnu.dto.Triples;
import edu.pnu.repo.AiDataRepository;
import edu.pnu.repo.AssetProductRepository;
import edu.pnu.repo.EventHistoryRepository;
import edu.pnu.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FindAnomalyComponet implements StatisticsInterface {

	// --- 의존성 주입 필드 ---
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

		// 1. 기준 정보 자산(Asset) 로드
		List<AssetProduct> assetProducts = assetProductRepo.findAll();

		// [1-1] 완전 일치 검출용 Key (product+company+name)의 Set
		Set<Triples> fullMatchSet = assetProducts.stream()
				.map(p -> new Triples(StringUtils.normalize(p.getEpcProduct()),
						StringUtils.normalize(p.getEpcCompany()), StringUtils.normalize(p.getProductName())))

				// safe(p.getEpcProduct()), safe(p.getEpcCompany()), safe(p.getProductName())))
				.collect(Collectors.toSet());

		// [1-2] 부분 일치 판단용 Set
		Set<String> knownProducts = assetProducts.stream().map(p -> StringUtils.normalize(p.getEpcProduct())).collect(Collectors.toSet());
		Set<String> knownCompanies = assetProducts.stream().map(p -> StringUtils.normalize(p.getEpcCompany())).collect(Collectors.toSet());
		Set<String> knownNames = assetProducts.stream().map(p -> StringUtils.normalize(p.getProductName())).collect(Collectors.toSet());
		Set<String> knownLots = epcSerialValidatorService.getAllKnownLots();
		

		// 2. 현재 파일에서 AI가 이상으로 판단한 이벤트 목록 확보
		List<EventHistory> anomalousEventsInFile = eventHistoryRepo.findAllByCsv_FileIdWithEpcAndProduct(fileId)
				.stream().filter(EventHistory::isAnomaly).toList();

		// 3. 분석 대상 EPC 코드 목록 추출
		Set<String> epcCodesToCheck = anomalousEventsInFile.stream().map(event -> event.getEpc().getEpcCode())
				.collect(Collectors.toSet());

		if (epcCodesToCheck.isEmpty()) {
			log.info("[완료] 분석할 이상 데이터가 없습니다.");
			return;
		}

		// --- 'clone' 판별을 위한 전체 이력 조회 (성능 최적화) ---
		// 4. 추출된 EPC들의 전체 이동 경로를 DB에서 조회 (시간순 정렬)
		List<EventHistory> fullHistories = eventHistoryRepo
				.findFullHistoriesByEpcCodes(new ArrayList<>(epcCodesToCheck));

		// 5. EPC 기준으로 이벤트 이력을 묶음
		Map<String, List<EventHistory>> historiesByEpc = fullHistories.stream()
				.collect(Collectors.groupingBy(event -> event.getEpc().getEpcCode()));

		// 6. 각 EPC의 이동 경로를 검증하여 'clone'으로 판별된 이벤트 ID들을 저장
		Set<Long> cloneEventIds = new HashSet<>();
		historiesByEpc.forEach((epcCode, eventList) -> {
            if (eventList == null || eventList.size() < 2) {
                // 경로를 구성할 이벤트가 2개 미만이면 검사할 수 없음
                return; 
            }

            // [핵심 수정] List<EventHistory>를 List<AnalyzedTrip>으로 변환합니다.
            List<AnalyzedTrip> trips = new ArrayList<>();
            for (int i = 0; i < eventList.size() - 1; i++) {
                EventHistory fromEvent = eventList.get(i);
                EventHistory toEvent = eventList.get(i+1);

                AnalyzedTrip trip = new AnalyzedTrip();
                // AnalyzedTrip의 ID는 보통 'to' 이벤트의 ID를 따라갑니다.
                // 또는 from/to의 event_id를 조합하여 고유 ID를 생성할 수도 있습니다.
                trip.setRoadId(toEvent.getEventId()); 

                // From 정보 설정
                trip.setFromBusinessStep(fromEvent.getBusinessStep());
                trip.setFromEventType(fromEvent.getEventType());
                trip.setFromLocation(fromEvent.getLocation());;

                // To 정보 설정
                trip.setToBusinessStep(toEvent.getBusinessStep());
                trip.setToEventType(toEvent.getEventType());
                trip.setToLocation(toEvent.getLocation());;
                
                trips.add(trip);
            }
            
            // 이제 변환된 'trips' 리스트로 검증을 수행합니다.
            if (!trips.isEmpty()) {
                cloneEventIds.addAll(logisticsFlowValidatorService.findViolations(trips));
            }
		});

		// --- 'clone' 판별 로직 종료 ---

		// ------ AI 이상 이벤트 분류: 위조, 변조, 복제, 오탐 ------------//
		// 결과 저장용 객체 리스트
		List<AiData> result = new ArrayList<>();

		// 7. 현재 파일의 이상 이벤트들을 하나씩 최종 판별
		for (EventHistory event : anomalousEventsInFile) {
			String anomalyType;
			Epc epc = event.getEpc();

			// [수정] 1순위: 'clone' 판별
			if (cloneEventIds.contains(event.getEventId())) {
				anomalyType = "clone";
			} else {
				// 'clone'이 아닌 경우, 제품 정보 및 태그 유효성 판별
				Triples current = new Triples(
						StringUtils.normalize(epc.getProduct().getEpcProduct()),
						StringUtils.normalize(epc.getProduct().getEpcCompany()),
						StringUtils.normalize(epc.getProduct().getProductName()));

				// 2순위 판별: 제품 정보 정상 여부 (제품 정보가 정상이 아닐 경우)
				if (!fullMatchSet.contains(current)) {

					int serial = -1;
					try {
						serial = Integer.parseInt(epc.getEpcSerial());
					} catch (NumberFormatException e) {
						log.warn("tamper/fake 판별 중 시리얼 번호 파싱 실패. event_id: {}, serial: {}", event.getEventId(),
								epc.getEpcSerial());
					}
					
					if (knownProducts.contains(current.getEpcProduct()) || knownCompanies.contains(current.getEpcCompany())
                            || knownNames.contains(current.getProductName()) || knownLots.contains(epc.getEpcLot())
                            || epcSerialValidatorService.isPotentiallyValidSerial(serial)) {
						anomalyType = "tamper"; // 5개 중 하나라도 도용
					} else {
						anomalyType = "fake"; // 5개 모두 완전 창작
					}

				} else {
					// --- 3순위: AI 오탐 여부 ('error'/'tamper') ---
					String originalHubType = historiesByEpc.get(epc.getEpcCode()).get(0).getHubType();;
					String factory = epcSerialValidatorService.extractFactoryFromName(originalHubType);
					String lot = epc.getEpcLot();
					
				
					try {
						int serialNumber = Integer.parseInt(epc.getEpcSerial());
						if (epcSerialValidatorService.isValid(factory, lot, serialNumber)) {
							anomalyType = "error"; // 모든 것이 완벽히 정상
						} else {
							anomalyType = "tamper"; // 정상 제품에 EPC 태그만 위조
						}
					} catch (NumberFormatException e) {
						anomalyType = "tamper";
						log.warn("시리얼 번호 파싱 실패. event_id: {}, serial: {}", event.getEventId(), epc.getEpcSerial());
					}
				}
			}
			result.add(AiData.builder().eventHistory(event).anomalyType(anomalyType).build());
		}
		aiDataRepo.saveAll(result);
		log.info("[완료] Product 이상 분석 완료 - 저장된 AiData 수: {}", result.size());
	}

	@Override
	public int getOrder() {
		return 2;
	}

}
