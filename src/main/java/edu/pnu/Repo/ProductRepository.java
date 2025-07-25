package edu.pnu.Repo;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import edu.pnu.domain.Product;

public interface ProductRepository extends JpaRepository<Product, String> {
	
	// 제품명으로 검색
	List<Product> findByProductNameContaining(String name);
	
	@Query("SELECT p.epcProduct FROM Product p")
	Set<String> findAllPK();

	// 고유 상품 종류 카운트
	long count();
}
