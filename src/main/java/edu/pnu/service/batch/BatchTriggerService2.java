//package edu.pnu.service.batch;
//
//import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import edu.pnu.domain.AnalyzedTrip;
//import edu.pnu.domain.EventHistory;
//import edu.pnu.repo.AnalyzedTripRepository;
//import edu.pnu.repo.EventHistoryRepository;
//import jakarta.persistence.EntityManager;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class BatchTriggerService2 {
//
//	private final AnalyzedTripRepository analyzedTripRepo;
//	private final EventHistoryRepository eventHistoryRepo;
//	private final EntityManager entityManager;
//
//	private static final int BATCH_SIZE = 1000;
//
//	@Transactional
//    public void analyzeAndSaveAllTripsBatch(Long fileId) {
//        log.info("[시작] : [BatchTriggerService] AnalyzedTrip JPA 배치 insert 시작");
//        final int[] total = {0}; // 전체 저장 건수 카운터 -> 람다식 안에서 쓰려고 배열로 선언함.
//        final EventHistory[] prevArr = new EventHistory[1];
//        List<AnalyzedTrip> trips = new ArrayList<>(BATCH_SIZE);
//        
//      //[추가] 'Factory' 스텝만 있는 EPC를 미리 처리하기 위한 전처리 단계
//      		List<EventHistory> allEventsInFile = eventHistoryRepo.findAllByCsv_FileIdWithEpcAndProduct(fileId);
//      		Map<String, List<EventHistory>> eventsByEpc = allEventsInFile.stream()
//      				.collect(Collectors.groupingBy(event -> event.getEpc().getEpcCode()));
//
//      		Set<String> factoryOnlyEpcs = new HashSet<>();
//      		for (Map.Entry<String, List<EventHistory>> entry : eventsByEpc.entrySet()) {
//      			List<EventHistory> epcEvents = entry.getValue();
//      			boolean isOnlyFactory = epcEvents.stream().allMatch(e -> "Factory".equals(e.getBusinessStep()));
//      			
//      			if (isOnlyFactory) {
//      				//[추가] 'Factory' 스텝만 있다면, 이 EPC는 특별 처리 대상입니다.
//      				String epcCode = entry.getKey();
//      				factoryOnlyEpcs.add(epcCode);
//
//      				//[추가] 이 EPC의 가장 마지막 이벤트를 찾아 'to'가 null인 Trip을 생성합니다.
//      				epcEvents.stream()
//      					.max(Comparator.comparing(EventHistory::getEventTime))
//      					.ifPresent(lastFactoryEvent -> {
//      						trips.add(AnalyzedTrip.builder()
//      								.epc(lastFactoryEvent.getEpc())
//      								.fromLocationId(lastFactoryEvent.getLocation().getLocationId())
//      								.toLocationId(null) // 요청사항: 'to' 관련 정보는 NULL 처리
//      								.fromScanLocation(lastFactoryEvent.getLocation().getScanLocation())
//      								.toScanLocation(null)
//      								.fromBusinessStep(lastFactoryEvent.getBusinessStep())
//      								.toBusinessStep(null)
//      								.fromEventType(lastFactoryEvent.getEventType())
//      								.toEventType(null)
//      								.fromHubType(lastFactoryEvent.getHubType())
//      								.toHubType(null)
//      								.fromEventTime(lastFactoryEvent.getEventTime())
//      								.toEventTime(null)
//      								.csv(lastFactoryEvent.getCsv())
//      								.build());
//      					});
//      			}
//      		}
//        
//        log.debug("[시작] : [BatchTriggerService] stream 객체 검색 시작");
//        try (Stream<EventHistory> stream = eventHistoryRepo.streamByCsvFileIdOrderByEpcCodeAndEventTime(fileId)) {
//        	
//            stream.forEach(curr -> {
//            	EventHistory previousEvent = null;
//            	// --- [수정] EPC 그룹의 첫 이벤트를 만났을 때, DB에서 이전 이력을 조회하여 연결고리를 만듭니다. ---
//            	boolean isFirstEventOfEpcInStream = (prevArr[0] == null || !prevArr[0].getEpc().getEpcCode().equals(curr.getEpc().getEpcCode()));
//
//                if (isFirstEventOfEpcInStream) {
//                    // [DB 조회] 이 EPC의 "진짜" 이전 이벤트를 전체 DB에서 찾습니다.
//                    previousEvent = eventHistoryRepo.findFirstByEpc_EpcCodeAndEventTimeLessThanOrderByEventTimeDesc(
//                        curr.getEpc().getEpcCode(),
//                        curr.getEventTime()
//                    ).orElse(null);
//
//                } else {
//                    // 이 스트림에서 같은 EPC의 두 번째 이후 이벤트라면, 메모리에 있는 prevArr[0]를 그대로 사용 (빠름)
//                    previousEvent = prevArr[0];
//                }
//
//                // previousEvent가 존재할 때만 AnalyzedTrip을 생성합니다.
//                if (previousEvent != null) {
//                    trips.add(AnalyzedTrip.builder()
//                        .epc(previousEvent.getEpc())
//                        .fromLocationId(previousEvent.getLocation().getLocationId())
//                        .toLocationId(curr.getLocation().getLocationId())
//                        .fromScanLocation(previousEvent.getLocation().getScanLocation())
//                        .toScanLocation(curr.getLocation().getScanLocation())
//                        .fromBusinessStep(previousEvent.getBusinessStep())
//                        .toBusinessStep(curr.getBusinessStep())
//                        .fromEventType(previousEvent.getEventType())
//                        .toEventType(curr.getEventType())
//                        .fromHubType(previousEvent.getHubType())
//                        .toHubType(curr.getHubType())
//                        .fromEventTime(previousEvent.getEventTime())
//                        .toEventTime(curr.getEventTime())
//                        .csv(curr.getCsv())
//                        .build());
//                }
//                else {
//                	boolean isOnlyFactory = "Factory".equals(curr.getBusinessStep());
//                    boolean isInbound = curr.getEventType() != null && curr.getEventType().endsWith("_InBound");
//
//                    if (isOnlyFactory || isInbound) {
//                        // '공장' or 'InBound'만 있는 경우: STAY
//                        trips.add(AnalyzedTrip.builder()
//                            .epc(curr.getEpc())
//                            .fromLocationId(curr.getLocation().getLocationId())
//                            .toLocationId(curr.getLocation().getLocationId())
//                            .fromScanLocation(curr.getLocation().getScanLocation())
//                            .toScanLocation("stay")
//                            .fromBusinessStep(curr.getBusinessStep())
//                            .toBusinessStep("stay")
//                            .fromEventType(curr.getEventType())
//                            .toEventType("stay")
//                            .fromHubType(curr.getHubType())
//                            .toHubType("stay")
//                            .fromEventTime(curr.getEventTime())
//                            .toEventTime(null)
//                            .csv(curr.getCsv())
//                            .build());
//                    } 
//                    else {
//                        // Factory도 아니고 InBound도 아니면서 연결도 없는 진짜 이상치
//                        trips.add(AnalyzedTrip.builder()
//                            .epc(curr.getEpc())
//                            .fromLocationId(curr.getLocation().getLocationId())
//                            .toLocationId(curr.getLocation().getLocationId())
//                            .fromScanLocation(curr.getLocation().getScanLocation())
//                            .toScanLocation(null)
//                            .fromBusinessStep(curr.getBusinessStep())
//                            .toBusinessStep(null)
//                            .fromEventType(curr.getEventType())
//                            .toEventType(null)
//                            .fromHubType(curr.getHubType())
//                            .toHubType(null)
//                            .fromEventTime(curr.getEventTime())
//                            .toEventTime(null)
//                            .csv(curr.getCsv())
//                            .build());
//                    }
//                
//                }
//
//                // 배치 처리 로직 (변경 없음)
//                if (trips.size() >= BATCH_SIZE) {
//                    saveAndClear(trips, total);
//                }
//                prevArr[0] = curr;
//            });
//
//         // 마지막 남은 데이터 저장 (변경 없음)
//            if (!trips.isEmpty()) {
//                saveAndClear(trips, total);
//            }
//        }
//        log.info("[완료] : fileId={} 배치 insert 종료 (총 저장: {}건)", fileId, total[0]);
//    }
//        // 중복 코드를 줄이기 위한 헬퍼 메소드
//	private void saveAndClear(List<AnalyzedTrip> trips, int[] total) {
//		analyzedTripRepo.saveAll(trips);
//		analyzedTripRepo.flush();
//		entityManager.clear();
//		total[0] += trips.size();
//		log.info("[진행] : {}건 저장 (누적: {}건)", trips.size(), total[0]);
//		trips.clear();
//	}
//
//}