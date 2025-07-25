package edu.pnu.service.statistics;


import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import edu.pnu.websocket.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsAdminService {
	
	private final List<StatisticsInterface> statisticsInterface;
	private final WebSocketService webSocketService;
	
	public void processAllStatistics(Long fileId, String userId) {
		List<StatisticsInterface> sorted = statisticsInterface.stream()
				.sorted(Comparator.comparingInt(StatisticsInterface::getOrder))
				.toList();
		
		log.info("[시작] 통계 생성 시작 - fileId: {}, 처리기 개수: {}", fileId, sorted.size());
		
		for(StatisticsInterface implementsClss : sorted) {
			try {
				log.info("[진행] {} 처리 시작", implementsClss.getProcessorName());
				webSocketService.sendMessage(userId, implementsClss.getProcessorName() + " 생성 중");
				
				implementsClss.process(fileId);
				
			}  catch (Exception e) {
                log.error("[오류] {} 처리 실패: {}", implementsClss.getProcessorName(), e.getMessage(), e);
                webSocketService.sendMessage(userId, implementsClss.getProcessorName() + " 생성 실패: " + e.getMessage());
                // 하나 실패해도 계속 진행
            }
		}
		log.info("[완료] 모든 통계 생성 완료 - fileId: {}", fileId);
	}
}
