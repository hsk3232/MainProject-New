package edu.pnu.repo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

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

	@Query("SELECT eh FROM EventHistory eh JOIN FETCH eh.epc e JOIN FETCH e.product p JOIN FETCH eh.location l WHERE eh.csv.fileId = :fileId")
	List<EventHistory> findAllByCsv_FileIdWithEpcAndProduct(@Param("fileId") Long fileId);

	Optional<EventHistory> findFirstByLocationOrderByEventTimeDesc(Location l);

	List<EventHistory> findAllByOrderByEpc_EpcCodeAscEventTimeAsc();

	@Modifying
	@Query("UPDATE EventHistory e SET e.anomaly = true WHERE e.eventId IN :ids")
	int bulkUpdatseAnomaly(@Param("ids") List<Long> ids);

	List<EventHistory> findByEventIdIn(List<Long> eventId); // eventId 컬럼이 eventIds 리스트에 포함되는 것만 검색

	   // [수정] KPI 통계 집계 쿼리 수정
    // 1. (JOIN) `product` 테이블을 'p'라는 별칭으로 추가 JOIN하여 제품 정보를 가져옵니다. (ON e.product_id = p.product_id)
    // 2. (SELECT) 제품 종류 수를 셀 때 `e.epc_product` 대신, 새로 JOIN한 `p` 테이블의 고유 ID인 `p.product_id`를 기준으로 COUNT하도록 변경했습니다.
	@Query(value = """
		    SELECT
		        -- 전체 trip 수
		        COUNT(*) AS totalTripCount,

		        -- 공장에서 생산된 제품 종류 수
		        COUNT(DISTINCT CASE WHEN eh.business_step = 'Factory' THEN p.product_id END) AS uniqueProductCount,

		        -- 공장에서 생성된 개수
		        SUM(CASE WHEN eh.business_step = 'Factory' THEN 1 ELSE 0 END) AS codeCount,

		        -- 이상(anomaly = true) 이벤트 수
		        COUNT(DISTINCT CASE WHEN eh.anomaly = true THEN eh.event_id END) AS anomalyCount,

		        -- 판매(pos_sell) 처리된 EPC 수
		        COUNT(DISTINCT CASE WHEN eh.event_type = 'pos_sell' THEN eh.epc_code END) AS salesCount,

		        -- W_Stock 입고 건수
		        COUNT(CASE WHEN eh.business_step = 'W_Stock_Inbound' THEN 1 END) AS warehouseIn,

		        -- W_Stock 출고 건수
		        COUNT(CASE WHEN eh.business_step = 'W_Stock_Outbound' THEN 1 END) AS warehouseOut,

		        -- EPC별 리드타임 평균 (초 단위)
		        (
		            SELECT AVG(leadTimeSec) FROM (
		                SELECT TIMESTAMPDIFF(SECOND, MIN(eh2.event_time), MAX(eh2.event_time))/ 86400.0 AS leadTimeSec
		                FROM eventhistory eh2
		                WHERE eh2.file_id = :fileId
		                GROUP BY eh2.epc_code
		            ) AS leadTimeTable
		        ) AS avgLeadTime

		    FROM eventhistory eh
		    LEFT JOIN epc e ON eh.epc_code = e.epc_code
		    LEFT JOIN product p ON e.product_id = p.product_id
		    WHERE eh.file_id = :fileId
		""", nativeQuery = true)
		Map<String, Object> getKpiAggregates(@Param("fileId") Long fileId);
	
	 // [추가] epc 코드를 기준으로 가장 첫 번째(시간상 가장 빠른) 이벤트를 찾는 쿼리
    Optional<EventHistory> findFirstByEpc_EpcCodeOrderByEventTimeAsc(String epcCode);
    
 // [추가] EPC 코드 리스트를 받아, 관련된 모든 EventHistory를 연관 엔티티와 함께 조회
    // EPC 코드와 이벤트 시간 순서로 정렬하여 경로 추적을 용이하게 함
    @Query("SELECT eh FROM EventHistory eh " +
            "JOIN FETCH eh.epc e " +
            "JOIN FETCH e.product p " +
            "JOIN FETCH eh.location l " +
            "WHERE e.epcCode IN :epcCodes " +
            "ORDER BY e.epcCode ASC, eh.eventTime ASC")
     List<EventHistory> findFullHistoriesByEpcCodes(@Param("epcCodes") List<String> epcCodes);
    
    
    @Query("""
    	    SELECT e FROM EventHistory e
    	    WHERE e.csv.fileId = :fileId
    	    ORDER BY e.epc.epcCode ASC, e.eventTime ASC
    	""")
    	Stream<EventHistory> streamByCsvFileIdOrderByEpcCodeAndEventTime(@Param("fileId") Long fileId);
}
