package edu.pnu.repo;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.pnu.domain.AnalyzedTrip;

public interface AnalyzedTripRepository extends JpaRepository<AnalyzedTrip, Long> {
	boolean existsByFromLocation_LocationIdAndToLocation_LocationIdAndFromEventTypeAndToEventType(
		    Long fromLocationId, Long toLocationId, String fromEventType, String toEventType);


	// 검색 없이 전체 페이지네이션
	@Query("""
			    SELECT a FROM AnalyzedTrip a
			    WHERE EXISTS (
			        SELECT eh FROM EventHistory eh
			        WHERE eh.epc = a.epc
			          AND eh.anomaly = true
			          )
			    ORDER BY a.roadId DESC
			""")
	Page<AnalyzedTrip> findAllAnomalies(Pageable pageable);

	// 검색 포함 페이지네이션
	@Query("""
			    SELECT a FROM AnalyzedTrip a
			    WHERE EXISTS (
			        SELECT eh FROM EventHistory eh
			        WHERE eh.epc = a.epc
			          AND eh.anomaly = true
			    )
			      AND (
			     a.epc.epcLot LIKE CONCAT('%', :epcLot, '%')
			      )
			    ORDER BY a.roadId DESC
			""")
	Page<AnalyzedTrip> findAnomaliesWithEpcLot(@Param("epcLot") String epcLot, Pageable pageable);

	// 검색 포함 페이지네이션
	@Query("""
			    SELECT a FROM AnalyzedTrip a
			    WHERE EXISTS (
			        SELECT eh FROM EventHistory eh
			        WHERE eh.epc = a.epc
			          AND eh.anomaly = true
			    )
			      AND (
			     a.epc.epcCode LIKE CONCAT('%', :epcCode, '%')
			      )
			    ORDER BY a.roadId DESC
			""")
	Page<AnalyzedTrip> findAnomaliesWithEpcCode(@Param("epcCode") String epcCode, Pageable pageable);

	// 검색 포함 페이지네이션
	@Query("""
			    SELECT a FROM AnalyzedTrip a
			    WHERE EXISTS (
			        SELECT eh FROM EventHistory eh
			        WHERE eh.epc = a.epc
			          AND eh.anomaly = true
			    )
			      AND (
			        a.epc.epcCode LIKE CONCAT('%', :epcCode, '%')
			        OR a.epc.epcLot LIKE CONCAT('%', :epcLot, '%')
			      )
			    ORDER BY a.roadId DESC
			""")
	Page<AnalyzedTrip> findAnomaliesWithSearch(@Param("epcLot") String epcLot, @Param("epcCode") String epcCode,
			Pageable pageable);
	
	
	@Query("""
		    SELECT a FROM AnalyzedTrip a
		    WHERE (:cursor IS NULL OR a.roadId < :cursor)
		      AND (:epcCode IS NULL OR a.epc.epcCode LIKE CONCAT('%', :epcCode, '%'))
		      AND (:epcLot IS NULL OR a.epc.epcLot LIKE CONCAT('%', :epcLot, '%'))
		      AND EXISTS (
		        SELECT eh FROM EventHistory eh
		        WHERE eh.epc = a.epc
		          AND eh.anomaly = true
		      )
		    ORDER BY a.roadId DESC
		""")
		List<AnalyzedTrip> findAnomaliesWithCursorAndSearch(
		    @Param("cursor") Long cursor,
		    @Param("epcCode") String epcCode,
		    @Param("epcLot") String epcLot,
		    Pageable pageable
		);
}
