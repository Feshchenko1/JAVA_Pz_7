package com.dailycodework.pz_4_1.repository;


import com.dailycodework.pz_4_1.model.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VenueRepository extends JpaRepository<Venue, Long> {

}