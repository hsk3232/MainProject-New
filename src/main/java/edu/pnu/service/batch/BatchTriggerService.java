package edu.pnu.service.batch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.pnu.Repo.AnalyzedTripRepository;
import edu.pnu.Repo.EventHistoryRepository;
import edu.pnu.domain.AnalyzedTrip;
import edu.pnu.domain.EventHistory;
import edu.pnu.exception.NoDataFoundException;
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

		try {
			// Set으로 중복 조합 관리
			Set<String> comboKeySet = new HashSet<>();
			
			
			for (EventHistory curr : allEvents) {
				// 2. 이전과 현재가 같은 EPC코드(=같은 상품의 이동 기록)라면
				if (prev != null && prev.getEpc().getEpcCode().equals(curr.getEpc().getEpcCode())) {
					
					// 조합키 만들기 (필요한 필드 모두)
					String comboKey = prev.getLocation().getLocationId() + "_" +
							curr.getLocation().getLocationId() + "_" +
							prev.getEventType() + "_" +
							curr.getEventType();
					
					// Set으로 중복 체크
	                if (comboKeySet.contains(comboKey)) {
	                    prev = curr;
	                    continue; // 이미 있으면 패스!
	                }
	                comboKeySet.add(comboKey); // 처음 나온 조합만 추가

	                trips.add(AnalyzedTrip.builder()
	                        .fromScanLocation(prev.getLocation().getScanLocation())
	                        .toScanLocation(curr.getLocation().getScanLocation())
	                        .fromLocationId(prev.getLocation().getLocationId())
	                        .toLocationId(curr.getLocation().getLocationId())
	                        .fromBusinessStep(prev.getBusinessStep())
	                        .toBusinessStep(curr.getBusinessStep())
	                        .fromEventType(prev.getEventType())
	                        .toEventType(curr.getEventType())
	                        .build());
	            }
	            prev = curr;
	        }

			// 5. 새로 생성된 이동경로를 일괄 저장 (saveAll)
			return analyzedTripRepo.saveAll(trips);

		} catch (Exception e) {
			log.error("[오류] : [BatchTriggerService] trips이 빈배열임. ");
			throw new NoDataFoundException("저장된 데이터가 없습니다.");
		}
	}
}
