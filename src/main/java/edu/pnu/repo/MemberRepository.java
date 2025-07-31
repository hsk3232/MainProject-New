package edu.pnu.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.pnu.domain.Member;
import edu.pnu.domain.Role;

public interface MemberRepository extends JpaRepository<Member, Long> {
	Optional<Member> findByUserId(String userId);
	
	
	boolean existsByUserId(String userId);
	Role findByRole(String role);
	
	List<Member> findAll();
	
	@Query("SELECT m.status FROM Member m WHERE m.userId = :userId")
	String findStatusByUserId(@Param("userId") String userId);


}
