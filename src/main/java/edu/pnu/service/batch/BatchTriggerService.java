package edu.pnu.service.batch;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.pnu.domain.AnalyzedTrip;
import edu.pnu.domain.EventHistory;
import edu.pnu.repo.AnalyzedTripRepository;
import edu.pnu.repo.EventHistoryRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchTriggerService {

    private final AnalyzedTripRepository analyzedTripRepo;
    private final EventHistoryRepository eventHistoryRepo;
    private final EntityManager entityManager;

    private static final int BATCH_SIZE = 1000;

    @Transactional
    public void analyzeAndSaveAllTripsBatch(Long fileId) {
        log.info("[시작] : [BatchTriggerService] AnalyzedTrip JPA 배치 insert 시작");
        final int[] total = {0}; // 전체 저장 건수 카운터 -> 람다식 안에서 쓰려고 배열로 선언함.
        final EventHistory[] prevArr = new EventHistory[1];
        List<AnalyzedTrip> trips = new ArrayList<>(BATCH_SIZE);
        
        log.debug("[시작] : [BatchTriggerService] stream 객체 검색 시작");
        try (Stream<EventHistory> stream = eventHistoryRepo.streamByFileIdOrderByEpcCodeAndEventTime(fileId)) {
        	
            stream.forEach(curr -> {
                if (prevArr[0] != null && prevArr[0].getEpc().getEpcCode().equals(curr.getEpc().getEpcCode())) {
                    trips.add(AnalyzedTrip.builder()
                        .epc(prevArr[0].getEpc())
                        .fromScanLocation(prevArr[0].getLocation().getScanLocation())
                        .toScanLocation(curr.getLocation().getScanLocation())
                        .fromLocation(prevArr[0].getLocation())
                        .toLocation(curr.getLocation())
                        .fromBusinessStep(prevArr[0].getBusinessStep())
                        .toBusinessStep(curr.getBusinessStep())
                        .fromEventType(prevArr[0].getEventType())
                        .toEventType(curr.getEventType())
                        .fromEventTime(prevArr[0].getEventTime())
                        .toEventTime(curr.getEventTime())
                        .csv(curr.getCsv()) // fileId 연관 정보 남기려면
                        .build());
                }
                if (trips.size() >= BATCH_SIZE) {
                    analyzedTripRepo.saveAll(trips);
                    analyzedTripRepo.flush();
                    entityManager.clear();
                    total[0] += trips.size();
                    log.info("[진행] : {}건 저장 (누적: {}건)", BATCH_SIZE, total[0]);
                    trips.clear();
                }
                prevArr[0] = curr;
            });

            // 마지막 남은 데이터 저장
            if (!trips.isEmpty()) {
                analyzedTripRepo.saveAll(trips);
                analyzedTripRepo.flush();
                entityManager.clear();
                total[0] += trips.size();
                log.info("[마지막] : 남은 {}건 저장 (총 누적: {}건)", trips.size(), total[0]);
            }
        }

        log.info("[완료] : fileId={} 배치 insert 종료 (총 저장: {}건)", fileId, total[0]);
    }
}