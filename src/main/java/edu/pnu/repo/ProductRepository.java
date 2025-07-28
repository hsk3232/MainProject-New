package edu.pnu.repo;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.pnu.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

	// 제품명으로 검색
	List<Product> findByProductNameContaining(String name);

	@Query("SELECT p.epcProduct FROM Product p")
	Set<String> findAllPK();

	// 고유 상품 종류 카운트
	long count();

	@Query("""
			    SELECT p.epcProduct, p.epcCompany, p.productName, p.productId
			    FROM Product p
			""")
	List<Object[]> findAllProductKeyIdMap();

	@Query("SELECT p.productId FROM Product p WHERE p.epcProduct = :epcProduct AND p.epcCompany = :epcCompany AND p.productName = :productName")
	Long findProductIdByCompositeKey(@Param("epcProduct") String epcProduct, @Param("epcCompany") String epcCompany,
			@Param("productName") String productName);

}
