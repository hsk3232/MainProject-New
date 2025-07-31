package edu.pnu.repo;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.pnu.domain.AnalyzedTrip;
import edu.pnu.dto.AnalyzedTripDTO;

public interface AnalyzedTripRepository extends JpaRepository<AnalyzedTrip, Long> {

	// [DashboardController].getAnomaliesList - 검색 없이 전체 페이지네이션
	@Query("""
			    SELECT DISTINCT new edu.pnu.dto.AnalyzedTripDTO(
			        a.fromScanLocation, fromLoc.longitude, fromLoc.latitude, a.fromEventTime, a.fromBusinessStep,
			        a.toScanLocation, toLoc.longitude, toLoc.latitude, a.toEventTime, a.toBusinessStep,
			        ad.csv.fileId, e.epcCode, p.productName, e.epcLot, a.fromEventType, a.roadId,
			        ad.eventHistory.eventTime    
			    )
			    FROM AnalyzedTrip a
			    LEFT JOIN Location fromLoc ON a.fromLocationId = fromLoc.locationId
			    LEFT JOIN Location toLoc ON a.toLocationId = toLoc.locationId
			    LEFT JOIN a.epc e
			    LEFT JOIN e.product p
			    LEFT JOIN AiData ad ON ad.analyzedTrip = a AND ad.csv.fileId = :fileId
			    WHERE (:cursor IS NULL OR a.roadId < :cursor)
			      AND ad.aiId IS NOT NULL
			    ORDER BY e.epcCode ASC, ad.eventHistory.eventTime DESC
			""")
	List<AnalyzedTripDTO> getFindAnomaliesWithCursor(@Param("fileId") Long fileId, @Param("cursor") Long cursor,
			Pageable pageable);

	// [DashboardController].getAllAnomaliesList
	@Query("""
			    SELECT DISTINCT new edu.pnu.dto.AnalyzedTripDTO(
			        a.fromScanLocation, fromLoc.longitude, fromLoc.latitude, a.fromEventTime, a.fromBusinessStep,
			        a.toScanLocation, toLoc.longitude, toLoc.latitude, a.toEventTime, a.toBusinessStep,
			        eh.csv.fileId, e.epcCode, p.productName, e.epcLot, a.fromEventType, a.roadId,
			        eh.eventTime             
			    )
			    FROM AnalyzedTrip a
			    LEFT JOIN Location fromLoc ON a.fromLocationId = fromLoc.locationId
			    LEFT JOIN Location toLoc ON a.toLocationId = toLoc.locationId
			    LEFT JOIN a.epc e
			    LEFT JOIN e.product p
			    JOIN EventHistory eh ON eh.epc = a.epc
			    WHERE eh.csv.fileId = :fileId
			      AND eh.anomaly = true
			    ORDER BY e.epcCode ASC, eh.eventTime DESC
			""")
	List<AnalyzedTripDTO> getAllAnomaliesList(@Param("fileId") Long fileId);

	// 단일 파일의 모든 Trip을 DTO(좌표 포함)로 반환
	@Query("""
			    SELECT DISTINCT new edu.pnu.dto.AnalyzedTripDTO(
			        a.fromScanLocation, fromLoc.longitude, fromLoc.latitude, a.fromEventTime, a.fromBusinessStep,
			        a.toScanLocation, toLoc.longitude, toLoc.latitude, a.toEventTime, a.toBusinessStep,
			        eh.csv.fileId, e.epcCode, p.productName, e.epcLot, a.fromEventType, a.roadId,
			        eh.eventTime              
			    )
			    FROM AnalyzedTrip a
			    LEFT JOIN Location fromLoc ON a.fromLocationId = fromLoc.locationId
			    LEFT JOIN Location toLoc ON a.toLocationId = toLoc.locationId
			    LEFT JOIN a.epc e
			    LEFT JOIN e.product p
			    JOIN EventHistory eh ON eh.epc = a.epc
			    WHERE eh.csv.fileId = :fileId
			    ORDER BY e.epcCode ASC, eh.eventTime DESC
			""")
	List<AnalyzedTripDTO> findDtoWithCoordsByFileId(@Param("fileId") Long fileId);

	// 이상 Trip DTO 페이지네이션
	@Query("""
			    SELECT DISTINCT new edu.pnu.dto.AnalyzedTripDTO(
			        a.fromScanLocation, fromLoc.longitude, fromLoc.latitude, a.fromEventTime, a.fromBusinessStep,
			        a.toScanLocation, toLoc.longitude, toLoc.latitude, a.toEventTime, a.toBusinessStep,
			        eh.csv.fileId, e.epcCode, p.productName, e.epcLot, a.fromEventType, a.roadId,
			        eh.eventTime              
			    )
			    FROM AnalyzedTrip a
			    LEFT JOIN Location fromLoc ON a.fromLocationId = fromLoc.locationId
			    LEFT JOIN Location toLoc ON a.toLocationId = toLoc.locationId
			    LEFT JOIN a.epc e
			    LEFT JOIN e.product p
			    JOIN EventHistory eh ON eh.epc = a.epc
			    WHERE eh.csv.fileId = :fileId
			      AND (:cursor IS NULL OR a.roadId < :cursor)
			      AND (
			          (:epcCode IS NULL OR e.epcCode LIKE CONCAT('%', :epcCode, '%'))
			          OR
			          (:epcLot IS NULL OR e.epcLot LIKE CONCAT('%', :epcLot, '%'))
			      )
			    ORDER BY e.epcCode ASC, eh.eventTime DESC
			""")
	List<AnalyzedTripDTO> getFindTripWithCursorAndSearch(@Param("cursor") Long cursor, @Param("epcCode") String epcCode,
			@Param("epcLot") String epcLot, Pageable pageable, @Param("fileId") Long fileId);

	@Query("""
			    SELECT DISTINCT new edu.pnu.dto.AnalyzedTripDTO(
			        a.fromScanLocation, fromLoc.longitude, fromLoc.latitude, a.fromEventTime, a.fromBusinessStep,
			        a.toScanLocation, toLoc.longitude, toLoc.latitude, a.toEventTime, a.toBusinessStep,
			        eh.csv.fileId, e.epcCode, p.productName, e.epcLot, a.fromEventType, a.roadId,
			        eh.eventTime               
			    )
			    FROM AnalyzedTrip a
			    LEFT JOIN Location fromLoc ON a.fromLocationId = fromLoc.locationId
			    LEFT JOIN Location toLoc ON a.toLocationId = toLoc.locationId
			    LEFT JOIN a.epc e
			    LEFT JOIN e.product p
			    JOIN EventHistory eh ON eh.epc = a.epc
			    WHERE eh.csv.fileId = :fileId
			      AND eh.anomaly = true
			    ORDER BY e.epcCode ASC, eh.eventTime DESC
			""")
	List<AnalyzedTripDTO> findAllAnomali(@Param("fileId") Long fileId);

	// 분석용 엔티티 반환, Fetch Join 최적화
	@Query("""
			    SELECT a
			    FROM AnalyzedTrip a
			    JOIN FETCH a.epc
			    WHERE a.epc.epcCode IN :epcCodes
			    ORDER BY a.fromEventTime
			""")
	List<AnalyzedTrip> findFullTripsByEpcCodes(@Param("epcCodes") List<String> epcCodes);

	@Query("""
			    SELECT DISTINCT a.toScanLocation
			    FROM AnalyzedTrip a
			    WHERE a.csv.fileId = :fileId
			      AND a.fromScanLocation LIKE CONCAT('%', :scanLocation, '%')
			      AND a.toScanLocation IS NOT NULL
			    ORDER BY a.toScanLocation ASC
			""")
	List<String> findDistinctToScanLocationsByFromScanLocation(@Param("fileId") Long fileId,
			@Param("scanLocation") String scanLocation);

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

}