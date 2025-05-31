package com.dailycodework.pz_4_1.service;

import com.dailycodework.pz_4_1.model.Venue;
import com.dailycodework.pz_4_1.repository.VenueRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Getter
@Service
public class VenueService {

    private final VenueRepository venueRepository;

    public VenueService(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    @Transactional
    public Venue createVenue(Venue venue) {

        return venueRepository.save(venue);
    }

    @Transactional(readOnly = true)
    public Optional<Venue> getVenueById(Long id) {
        return venueRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Venue getVenueByIdOrThrow(Long id) {
        return venueRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Venue not found with id: " + id));
    }


    @Transactional(readOnly = true)
    public List<Venue> getAllVenues() {
        return venueRepository.findAll();
    }

    @Transactional
    public Venue updateVenue(Long id, @org.jetbrains.annotations.NotNull Venue venueDetails) {
        Venue existingVenue = venueRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Venue not found with id: " + id + " for update"));

        existingVenue.setName(venueDetails.getName());
        existingVenue.setAddress(venueDetails.getAddress());
        existingVenue.setCapacity(venueDetails.getCapacity());

        return venueRepository.save(existingVenue);
    }

    @Transactional
    public void deleteVenue(Long id) {
        if (!venueRepository.existsById(id)) {
            throw new EntityNotFoundException("Venue not found with id: " + id + " for deletion");
        }
        venueRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public boolean venueExists(Long id) {
        return venueRepository.existsById(id);
    }

}