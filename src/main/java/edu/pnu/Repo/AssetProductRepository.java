package edu.pnu.Repo;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import edu.pnu.domain.AssetProduct;

public interface AssetProductRepository extends JpaRepository<AssetProduct, String> {
    @Query("select a.epcProduct from AssetProduct a")
    Set<String> findAllPK();
}