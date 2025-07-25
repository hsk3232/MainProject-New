package edu.pnu.Repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import edu.pnu.domain.EpcAnomalyStats;

public interface EpcAnomalyStatsRepository extends JpaRepository<EpcAnomalyStats, Long> {
	
	// 전체 이동(Trip) 건수
		long countBy();

		// 이상 이벤트 건수
		long countByTotalEventsIsNotNull();
		
		// epcCode 전체 리스트 (distinct)
		@Query("select distinct e.epc.epcCode from EventHistory e")
		List<String> findAllDistinctEpcCodes();
		
}
