package edu.pnu.repo;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.pnu.domain.AnalyzedTrip;
import edu.pnu.dto.AnalyzedTripDTO;

public interface AnalyzedTripRepository extends JpaRepository<AnalyzedTrip, Long> {
//	boolean existsByFromLocation_LocationIdAndToLocation_LocationIdAndFromEventTypeAndToEventType(
//		    Long fromLocationId, Long toLocationId, String fromEventType, String toEventType);


//	// 검색 없이 전체 페이지네이션
//	@Query("""
//			    SELECT a FROM AnalyzedTrip a
//			    WHERE EXISTS (
//			        SELECT eh FROM EventHistory eh
//			        WHERE eh.epc = a.epc
//			          AND eh.anomaly = true
//			          )
//			    ORDER BY a.roadId DESC
//			""")
//	Page<AnalyzedTrip> findAllAnomalies(Pageable pageable);
//
//	// 검색 포함 페이지네이션
//	@Query("""
//			    SELECT a FROM AnalyzedTrip a
//			    WHERE EXISTS (
//			        SELECT eh FROM EventHistory eh
//			        WHERE eh.epc = a.epc
//			          AND eh.anomaly = true
//			    )
//			      AND (
//			     a.epc.epcLot LIKE CONCAT('%', :epcLot, '%')
//			      )
//			    ORDER BY a.roadId DESC
//			""")
//	Page<AnalyzedTrip> findAnomaliesWithEpcLot(@Param("epcLot") String epcLot, Pageable pageable);
//
//	// 검색 포함 페이지네이션
//	@Query("""
//			    SELECT a FROM AnalyzedTrip a
//			    WHERE EXISTS (
//			        SELECT eh FROM EventHistory eh
//			        WHERE eh.epc = a.epc
//			          AND eh.anomaly = true
//			    )
//			      AND (
//			     a.epc.epcCode LIKE CONCAT('%', :epcCode, '%')
//			      )
//			    ORDER BY a.roadId DESC
//			""")
//	Page<AnalyzedTrip> findAnomaliesWithEpcCode(@Param("epcCode") String epcCode, Pageable pageable);
//
//	// 검색 포함 페이지네이션
//	@Query("""
//			    SELECT a FROM AnalyzedTrip a
//			    WHERE EXISTS (
//			        SELECT eh FROM EventHistory eh
//			        WHERE eh.epc = a.epc
//			          AND eh.anomaly = true
//			    )
//			      AND (
//			        a.epc.epcCode LIKE CONCAT('%', :epcCode, '%')
//			        OR a.epc.epcLot LIKE CONCAT('%', :epcLot, '%')
//			      )
//			    ORDER BY a.roadId DESC
//			""")
//	Page<AnalyzedTrip> findAnomaliesWithSearch(@Param("epcLot") String epcLot, @Param("epcCode") String epcCode,
//			Pageable pageable);
//	
//	
//	@Query("""
//			SELECT new edu.pnu.dto.AnalyzedTripFlatDTO(
//			    a.fromScanLocation, fromLoc.longitude, fromLoc.latitude,
//			    a.fromEventTime, a.fromBusinessStep,
//			    a.toScanLocation, toLoc.longitude, toLoc.latitude,
//			    a.toEventTime, a.toBusinessStep,
//			    a.roadId, a.epc.epcCode, a.fromEventType, a.csv.fileId
//			)
//			FROM AnalyzedTrip a
//			LEFT JOIN Location fromLoc ON a.fromLocationId = fromLoc.locationId
//			LEFT JOIN Location toLoc ON a.toLocationId = toLoc.locationId
//			WHERE a.csv.fileId = :fileId
//			""")
//			List<AnalyzedTripDTO>  findDtoWithCoordsByFileId(@Param("fileId") Long fileId);
//	
//	@Query("""
//			SELECT new edu.pnu.dto.AnalyzedTripDTO(
//			    a.fromScanLocation, fromLoc.longitude, fromLoc.latitude, a.fromEventTime, a.fromBusinessStep,
//			    a.toScanLocation, toLoc.longitude, toLoc.latitude, a.toEventTime, a.toBusinessStep,
//			    a.csv.fileId, a.epc.epcCode, p.productName, a.epc.epcLot, a.fromEventType, a.roadId
//			)
//			FROM AnalyzedTrip a
//			LEFT JOIN Location fromLoc ON a.fromLocationId = fromLoc.locationId
//			LEFT JOIN Location toLoc ON a.toLocationId = toLoc.locationId
//			LEFT JOIN Epc e ON a.epc.epcCode = e.epcCode
//			LEFT JOIN Product p ON e.product.id = p.id
//			WHERE (a.roadId < :cursor OR :cursor IS NULL)
//			  AND (:epcCode IS NULL OR e.epcCode = :epcCode)
//			  AND (:epcLot IS NULL OR e.epcLot = :epcLot)
//			ORDER BY a.roadId DESC
//			""")
//			List<AnalyzedTripDTO> findAnomaliesWithCursorAndSearch(
//			    @Param("cursor") Long cursor,
//			    @Param("epcCode") String epcCode,
//			    @Param("epcLot") String epcLot,
//			    Pageable pageable
//			);
	
	 // --- 1. 분석용으로 사용 (엔티티 반환, Fetch Join으로 최적화) ---
	  // 1. [분석 컨텍스트용] FindAnomalyComponet에서 'clone' 판별을 위해 EPC의 전체 경로를 조회합니다.
    @Query("SELECT a FROM AnalyzedTrip a JOIN FETCH a.epc WHERE a.epc.epcCode IN :epcCodes ORDER BY a.fromEventTime")
    List<AnalyzedTrip> findFullTripsByEpcCodes(@Param("epcCodes") List<String> epcCodes);



    // --- 2. API 응답용으로 사용 (DTO 반환, JOIN WITH으로 최적화) ---
    // DashboardService 와 API 컨트롤러에서 사용합니다.
    @Query("""
            SELECT new edu.pnu.dto.AnalyzedTripDTO(
                a.fromScanLocation, fromLoc.longitude, fromLoc.latitude, a.fromEventTime, a.fromBusinessStep,
                a.toScanLocation, toLoc.longitude, toLoc.latitude, a.toEventTime, a.toBusinessStep,
                a.csv.fileId, e.epcCode, p.productName, e.epcLot, a.fromEventType, a.roadId
            )
            FROM AnalyzedTrip a
            LEFT JOIN Location fromLoc WITH a.fromLocationId = fromLoc.id
            LEFT JOIN Location toLoc WITH a.toLocationId = toLoc.id
            LEFT JOIN a.epc e
            LEFT JOIN e.product p
            WHERE (a.roadId < :cursor OR :cursor IS NULL)
              AND (:epcCode IS NULL OR e.epcCode = :epcCode)
              AND (:epcLot IS NULL OR e.epcLot = :epcLot)
              AND EXISTS (
                  SELECT 1 FROM EventHistory eh 
                  WHERE eh.epc = a.epc AND eh.anomaly = true
              )
            ORDER BY a.roadId DESC
            """)
    List<AnalyzedTripDTO> findAnomaliesAsDTOWithCursor(
        @Param("cursor") Long cursor,
        @Param("epcCode") String epcCode,
        @Param("epcLot") String epcLot,
        Pageable pageable
    );
}
