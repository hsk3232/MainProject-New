package edu.pnu.service.statistics;

import org.springframework.stereotype.Component;

import edu.pnu.repo.EventHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryComponet implements StatisticsInterface {
	
	 private final EventHistoryRepository eventHistoryRepo;
	
	@Override
	public String getProcessorName() {

		return "제고 조회";
	}

	@Override
	public void process(Long fileId) {
		// TODO Auto-generated method stub
		
		eventHistoryRepo.calculateInventoryByBusinessStep(fileId);	
	}

	@Override
	public int getOrder() {
		// TODO Auto-generated method stub
		return 0;
	}

}
