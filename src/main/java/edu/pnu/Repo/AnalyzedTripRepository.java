package edu.pnu.Repo;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.pnu.domain.AnalyzedTrip;

public interface AnalyzedTripRepository extends JpaRepository<AnalyzedTrip, Long> {
	boolean existsByFromLocationIdAndToLocationIdAndFromEventTypeAndToEventType(
		    Long fromLocationId, Long toLocationId,
		    String fromEventType, String toEventType
		);
}
