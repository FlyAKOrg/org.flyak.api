package org.nzvirtual.api.data.repository;

import org.nzvirtual.api.data.entity.Airline;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AirlineRepository extends CrudRepository<Airline, Long> {
    Optional<Airline> findByIcao(String icao);
}
