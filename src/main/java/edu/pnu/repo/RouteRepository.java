package edu.pnu.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.pnu.domain.Route;

public interface RouteRepository extends JpaRepository<Route, Long> {

}
