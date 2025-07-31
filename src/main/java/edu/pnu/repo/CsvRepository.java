package edu.pnu.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.pnu.domain.Csv;
import edu.pnu.dto.ReportCoverDTO;

public interface CsvRepository extends JpaRepository<Csv, Long> {
	
	// findBy + [조건1] + And + [조건2] + OrderBy + [정렬필드] + Desc
	
	Csv findByFileId(Long LocationId);
	
	List<Csv> findByMemberUserId(String userId);
	
	// 커서 없이, locationId 기준 + 검색어
	List<Csv> findByMember_LocationIdAndFileNameContainingOrderByFileIdDesc(
	    Long locationId, String fileName, Pageable pageable);

	// 커서+검색어
	List<Csv> findByMember_LocationIdAndFileIdLessThanAndFileNameContainingOrderByFileIdDesc(
	    Long locationId, Long cursor, String fileName, Pageable pageable);

	// 커서 없이 전체
	List<Csv> findByMember_LocationIdOrderByFileIdDesc(
	    Long locationId, Pageable pageable);

	// 커서만
	List<Csv> findByMember_LocationIdAndFileIdLessThanOrderByFileIdDesc(
	    Long locationId, Long cursor, Pageable pageable);

	// 커서 + 파일명 검색
	List<Csv> findByFileIdLessThanAndFileNameContainingOrderByFileIdDesc(Long cursor, String fileName, Pageable pageable);

	// 커서 없는 전체 목록
	List<Csv> findAllByOrderByFileIdDesc(Pageable pageable);

	// 커서만(검색 없이)
	List<Csv> findByFileIdLessThanOrderByFileIdDesc(Long cursor, Pageable pageable);
	
	Optional<Csv> findById(Long fileId);
	
	Optional<Csv> findTopByOrderByFileIdDesc();

	@Query("""
	        SELECT new edu.pnu.dto.ReportCoverDTO(
	            c.fileName,
	            m.userName,
	            l.locationId,
	            c.createdAt,
	            (SELECT MIN(e.eventTime) FROM EventHistory e WHERE e.csv.fileId = c.fileId),
	            (SELECT MAX(e.eventTime) FROM EventHistory e WHERE e.csv.fileId = c.fileId)
	        )
	        FROM Csv c
	        JOIN c.member m
	        JOIN EventHistory e ON e.csv.fileId = c.fileId
	        JOIN e.location l
	        WHERE c.fileId = :fileId
	        """)
	    Optional<ReportCoverDTO> findReportCoverByFileId(@Param("fileId") Long fileId);
    
}
