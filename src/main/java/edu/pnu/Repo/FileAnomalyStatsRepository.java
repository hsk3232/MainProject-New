package edu.pnu.Repo;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.pnu.domain.FileAnomalyStats;

public interface FileAnomalyStatsRepository extends JpaRepository<FileAnomalyStats, Long> {
	
}
