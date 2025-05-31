package com.dailycodework.pz_4_1.service;

import com.dailycodework.pz_4_1.model.Event;
import com.dailycodework.pz_4_1.model.Venue;
import com.dailycodework.pz_4_1.repository.EventRepository;
import com.dailycodework.pz_4_1.repository.VenueRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;

    public EventService(EventRepository eventRepository, VenueRepository venueRepository) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
    }

    @Transactional
    public Event createEvent(Event event, Long venueId) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new EntityNotFoundException("Venue not found with id: " + venueId));
        event.setVenue(venue);
        return eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public Optional<Event> getEventById(Long id) {
        return eventRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Event getEventByIdOrThrow(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    @Transactional
    public Event updateEvent(Long id, Event eventDetails, Long venueId) {
        Event existingEvent = getEventByIdOrThrow(id);
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new EntityNotFoundException("Venue not found with id: " + venueId));

        existingEvent.setName(eventDetails.getName());
        existingEvent.setEventDate(eventDetails.getEventDate());
        existingEvent.setVenue(venue);

        return eventRepository.save(existingEvent);
    }

    @Transactional
    public void deleteEvent(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new EntityNotFoundException("Event not found with id: " + id + " for deletion");
        }

        eventRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public boolean eventExists(Long id) {
        return eventRepository.existsById(id);
    }


    @Transactional(readOnly = true)
    public List<Event> findEventsByName(String name) {
        return eventRepository.findByName(name);
    }

    @Transactional(readOnly = true)
    public List<Event> findEventsByVenueId(Long venueId) {
        return eventRepository.findByVenueId(venueId);
    }

    @Transactional(readOnly = true)
    public List<Event> findEventsAfterDate(LocalDate date) {
        return eventRepository.findByEventDateAfter(date);
    }

    @Transactional(readOnly = true)
    public List<Event> findEventsByVenueNameOrderedByDate(String venueName) {
        return eventRepository.findByVenueNameOrderByEventDateDesc(venueName);
    }
    @Transactional(readOnly = true)
    public List<Event> findEventsByNameIgnoreCase(String name) {
        return eventRepository.findByNameIgnoreCase(name);
    }

}