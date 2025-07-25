package edu.pnu.Repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.pnu.domain.AiData;
import edu.pnu.domain.EventHistory;

public interface AiDataRepository extends JpaRepository<AiData, Long> {
	Optional<AiData> findByEventHistory(EventHistory eventHistory);

	Optional<AiData> findByEventHistory_EventId(Long eventId);

	
	List<AiData> findByEventHistory_EventIdIn(List<Long> eventIds);
	

}
