package edu.pnu.service.statistics;

public class FindAnomalyComponet implements StatisticsInterface {

	@Override
	public String getProcessorName() {
		return "이상 종류 판별기";
	}

	@Override
	public void process(Long fileId) {
		
		

	}

	@Override
	public int getOrder() {

		return 0;
	}

}
