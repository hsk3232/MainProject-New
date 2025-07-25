package edu.pnu.Repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.pnu.domain.KPIAnalysis;

public interface KPIAnalysisRepository extends JpaRepository<KPIAnalysis, Long> {
	List<KPIAnalysis> findByCsv_FileId(Long fileId);
}
