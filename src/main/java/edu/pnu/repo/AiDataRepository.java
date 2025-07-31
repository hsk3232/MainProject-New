package edu.pnu.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.pnu.domain.AiData;
import edu.pnu.domain.EventHistory;
import edu.pnu.dto.ByProductDTO;

public interface AiDataRepository extends JpaRepository<AiData, Long> {
	Optional<AiData> findByEventHistory(EventHistory eventHistory);

	 List<AiData> findByEventHistory_EventIdIn(List<Long> eventIds);
	 
	 
	 
//	[DashboardService].getNodeList
	 List<AiData> findByAnalyzedTrip_RoadIdIn(List<Long> eventIds);

	 @Query("""
	            SELECT ad.anomalyType
	            FROM AiData ad JOIN ad.eventHistory eh
	            WHERE eh.csv.fileId = :fileId AND ad.anomalyType IS NOT NULL
	            ORDER BY ad.anomalyType
	            """)
	    List<String> findDistincByFileId(@Param("fileId") Long fileId);

	 
	 
	 @Query("""
			    SELECT ad
			    FROM AiData ad
			    JOIN FETCH ad.eventHistory eh
			    JOIN FETCH eh.epc e
			    WHERE eh.csv.fileId = :fileId
			    ORDER BY e.epcCode ASC, eh.eventTime ASC
			    """)
			List<AiData> findDistinctAnomalyTypesByFileId(@Param("fileId") Long fileId);
	 
	 
	 // [StatisticsFindService].getByProduct
	 @Query("""
	            SELECT new edu.pnu.dto.ByProductDTO(
	                p.productName,
	                SUM(CASE WHEN ad.anomalyType = 'fake' THEN 1L ELSE 0L END),
	                SUM(CASE WHEN ad.anomalyType = 'tamper' THEN 1L ELSE 0L END),
	                SUM(CASE WHEN ad.anomalyType = 'clone' THEN 1L ELSE 0L END),
	                SUM(CASE WHEN ad.anomalyType = 'other' THEN 1L ELSE 0L END),
	                COUNT(ad)
	            )
	            FROM AiData ad
	            JOIN ad.eventHistory eh
	            JOIN eh.epc e
	            JOIN e.product p
	            WHERE eh.csv.fileId = :fileId
	            GROUP BY p.productName
	            ORDER BY p.productName
	            """)
	    List<ByProductDTO> countByProduct(@Param("fileId") Long fileId);

	 
	 
}
