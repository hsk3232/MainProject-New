package edu.pnu.Repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.pnu.domain.Csv;

public interface CsvRepository extends JpaRepository<Csv, Long> {
	
	// findBy + [조건1] + And + [조건2] + OrderBy + [정렬필드] + Desc
	
	List<Csv> findByMemberUserId(String userId);
	
	// 커서 없는 기본 검색 (파일명 포함)
	List<Csv> findByFileNameContainingOrderByFileIdDesc(String fileName, Pageable pageable);

	// 커서 + 파일명 검색
	List<Csv> findByFileIdLessThanAndFileNameContainingOrderByFileIdDesc(Long cursor, String fileName, Pageable pageable);

	// 커서 없는 전체 목록
	List<Csv> findAllByOrderByFileIdDesc(Pageable pageable);

	// 커서만(검색 없이)
	List<Csv> findByFileIdLessThanOrderByFileIdDesc(Long cursor, Pageable pageable);
	
	Optional<Csv> findById(Long fileId);
	
	Optional<Csv> findTopByOrderByFileIdDesc();

	
}
