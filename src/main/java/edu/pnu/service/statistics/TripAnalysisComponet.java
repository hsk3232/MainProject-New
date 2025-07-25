package edu.pnu.service.statistics;

import org.springframework.stereotype.Component;

import edu.pnu.service.batch.BatchTriggerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TripAnalysisComponet implements StatisticsInterface{
	
	private final BatchTriggerService batchTriggerService;
	
	@Override
	public String getProcessorName() {
		return "이동 경로 분석";
	}
	
	@Override
	public void process(Long fileId) {
		batchTriggerService.analyzeAndSaveAllTrips();
		log.info("[완료] : [BatchTriggerService] → [TripAnalysisComponet]  AnalyzedTrip DB 저장 완료");
	}
	
	@Override
	public int getOrder() {
		return 1;
	}
}
