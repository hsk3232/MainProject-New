package edu.pnu.Repo;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.pnu.domain.KPIAnalysis;

public interface KPIAnalysisRepository extends JpaRepository<KPIAnalysis, Long> {
	KPIAnalysis findByCsv_FileId(Long fileId);
}
