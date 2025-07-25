package edu.pnu.Repo;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import edu.pnu.domain.Epc;

public interface EpcRepository extends JpaRepository<Epc, String> {
	
	Optional<Epc> findById(String EpcCode);
	
	
	//PK 칼럼의 값만 set으로 모음
	 @Query("SELECT e.epcCode FROM Epc e")
	    Set<String> findAllPK();
}
