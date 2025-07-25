package edu.pnu.Repo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.pnu.domain.EventHistory;
import edu.pnu.domain.Location;

public interface EventHistoryRepository extends JpaRepository<EventHistory, Long> {

	// 검색(필터) + 커서 페이징
	List<EventHistory> findByEventTypeAndEventIdLessThanOrderByEventIdDesc(String eventType, Long cursor,
			Pageable pageable);

	List<EventHistory> findByEpc_EpcCodeAndEventIdLessThanOrderByEventIdDesc(String epcCode, Long cursor,
			Pageable pageable);

	List<EventHistory> findByBusinessStepAndEventTimeBetweenAndEventIdLessThanOrderByEventIdDesc(String businessStep,
			LocalDateTime min, LocalDateTime max, Long cursor, Pageable pageable);

	// epcCode별 이벤트 시간순 정렬
	List<EventHistory> findByEpc_EpcCodeOrderByEventTimeAsc(String epcCode);

	@Query("SELECT e FROM EventHistory e " +
		       "JOIN FETCH e.epc ep " +
		       "JOIN FETCH ep.product " +
		       "WHERE e.csv.fileId = :fileId")
		List<EventHistory> findAllByCsv_FileIdWithEpcAndProduct(@Param("fileId") Long fileId);

	Optional<EventHistory> findFirstByLocationOrderByEventTimeDesc(Location l);

	List<EventHistory> findAllByOrderByEpc_EpcCodeAscEventTimeAsc();
	
	 @Modifying
	 @Query("UPDATE EventHistory e SET e.anomaly = true WHERE e.eventId IN :ids")
	    int bulkUpdatseAnomaly(@Param("ids") List<Long> ids);
	

	List<EventHistory> findByEventIdIn(List<Long> eventId); // eventId 컬럼이 eventIds 리스트에 포함되는 것만 검색

	@Query(value = """
			  SELECT
			    COUNT(*) AS totalTripCount,
			    COUNT(DISTINCT e.epc_product) AS uniqueProductCount,
			    SUM(CASE WHEN eh.business_step = 'Factory' THEN 1 ELSE 0 END) AS codeCount,
			    COUNT(DISTINCT CASE WHEN eh.anomaly = true THEN eh.event_id END) AS anomalyCount,
			    COUNT(DISTINCT CASE WHEN eh.event_type = 'pos_sell' THEN eh.epc_code END) AS salesCount
			  FROM eventhistory eh
			  LEFT JOIN epc e ON eh.epc_code = e.epc_code
			  WHERE eh.file_id = :fileId
			""", nativeQuery = true)
	Map<String, Object> getKpiAggregates(@Param("fileId") Long fileId);

}
