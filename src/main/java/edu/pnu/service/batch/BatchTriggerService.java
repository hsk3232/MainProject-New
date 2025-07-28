package edu.pnu.service.batch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.pnu.domain.AnalyzedTrip;
import edu.pnu.domain.EventHistory;
import edu.pnu.exception.NoDataFoundException;
import edu.pnu.repo.AnalyzedTripRepository;
import edu.pnu.repo.EventHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchTriggerService {

	private final AnalyzedTripRepository analyzedTripRepo;
	private final EventHistoryRepository eventHistoryRepo;

	/**
	 * [분석 및 저장] EventHistory 전체 데이터를 epcCode+eventTime 기준으로 정렬 후 연속된 이동 쌍(from→to)을
	 * 찾아서 AnalyzedTrip로 변환 & 중복 없이 저장
	 */

	@Transactional
	public List<AnalyzedTrip> analyzeAndSaveAllTrips() {

		log.info("[진입] : [BatchTriggerService] AnalyzedTrip DB 추출 진입");

		// 1. 전체 EventHistory를 epcCode, eventTime 기준 정렬 조회
		List<EventHistory> allEvents = eventHistoryRepo.findAllByOrderByEpc_EpcCodeAscEventTimeAsc();
		List<AnalyzedTrip> trips = new ArrayList<>(); // 새롭게 만들어질 AnalyzedTrip 객체 모아 놓는 리스트

		EventHistory prev = null; // 이전 이벤트 정보를 저장.
		
		// 이벤트가 하나도 없으면 바로 예외 발생(데이터가 하나도 없다는 뜻)
		if(allEvents.isEmpty()) throw new NoDataFoundException("[오류] : [BatchTriggerService][analyzeAndSaveAllTrips] 정렬 조회 실패 ");
		
			for (EventHistory curr : allEvents) {
				// 2. 이전과 현재가 같은 EPC코드(=같은 상품의 이동 기록)라면
				if (prev != null && prev.getEpc().getEpcCode().equals(curr.getEpc().getEpcCode())) {
										
	                trips.add(AnalyzedTrip.builder()
	                		.epc(prev.getEpc())
	                        .fromScanLocation(prev.getLocation().getScanLocation())
	                        .toScanLocation(curr.getLocation().getScanLocation())
	                        .fromLocationId(prev.getLocation().getLocationId())
	                        .toLocationId(curr.getLocation().getLocationId())
	                        .fromBusinessStep(prev.getBusinessStep())
	                        .toBusinessStep(curr.getBusinessStep())
	                        .fromEventType(prev.getEventType())
	                        .toEventType(curr.getEventType())
	                        .fromEventTime(prev.getEventTime())
	                        .toEventTime(curr.getEventTime())
	                        .build());
	            }
	            prev = curr;
	        }

			// 5. 새로 생성된 이동경로를 일괄 저장 (saveAll)
			return analyzedTripRepo.saveAll(trips);

		}
	
}
