package edu.pnu.Repo;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import edu.pnu.domain.AssetLocation;

public interface AssetLocationRepository extends JpaRepository<AssetLocation, Long> {
    @Query("select a.locationId from AssetLocation a")
    Set<Long> findAllPK();
}