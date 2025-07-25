package edu.pnu.service.statistics;

// 1. 통계 서비스 인터페이스
public interface StatisticsInterface {
	String getProcessorName();
	void process(Long fileId); // 이곳에 집계 로직이 들어감!
	int getOrder(); //실행순서
}
