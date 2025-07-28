package edu.pnu.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.pnu.domain.FileAnomalyStats;

public interface FileAnomalyStatsRepository extends JpaRepository<FileAnomalyStats, Long> {
	
}
