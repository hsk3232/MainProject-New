package edu.pnu.Repo;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import edu.pnu.domain.Location;

public interface LocationRepository extends JpaRepository<Location, Long> {
	List<Location> findByScanLocationContaining(String scanLocation);
	
	@Query("SELECT l.locationId FROM Location l")
	Set<Long> findAllPK();
}
