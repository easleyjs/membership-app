package com.example.adminportal.service;

import com.example.adminportal.dto.EventCreateRequest;
import com.example.adminportal.dto.EventResponse;
import com.example.adminportal.entity.Event;
import com.example.adminportal.entity.EventLocation;
import com.example.adminportal.entity.EventStatus;
import com.example.adminportal.exception.ResourceNotFoundException;
import com.example.adminportal.repository.EventRepository;
import jakarta.validation.Valid;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
public class EventService {
    EventRepository eventRepository;

    public EventService(
            EventRepository eventRepository
    ) {
        this.eventRepository = eventRepository;
    }

    @Transactional
    public EventResponse createEvent(@Valid EventCreateRequest request) {
        Event newEvent = new Event();

        newEvent.setName(request.name());
        newEvent.setStartsAt(request.startsAt());
        newEvent.setEndsAt(request.endsAt());
        newEvent.setMembershipRequired(request.membershipRequired());
        newEvent.setCoverRequired(request.coverRequired());
        newEvent.setLocation(request.location());
        newEvent.setStatus(EventStatus.ACTIVE);

        Event savedEvent = eventRepository.save(newEvent);

        return EventResponse.from(savedEvent);
    }

    public EventResponse getEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException(
                        "Event not found"
                ));

        return EventResponse.from(event);
    }

    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll()
                .stream()
                .map(EventResponse::from)
                .toList();
    }

    @Transactional
    public EventResponse updateEvent(
            Long id,
            @Valid EventCreateRequest request
    ) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event not found: " + id
                ));

        event.setName(request.name());
        event.setStartsAt(request.startsAt());
        event.setEndsAt(request.endsAt());
        event.setMembershipRequired(request.membershipRequired());
        event.setCoverRequired(request.coverRequired());
        event.setLocation(request.location());

        return EventResponse.from(event);
    }
    // TODO: Future partial edit/ PATCH route

    @Transactional
    public EventResponse disableEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event not found: " + eventId
                ));

        event.setStatus(EventStatus.ARCHIVED);

        return EventResponse.from(event);
    }

    @Transactional
    public void deleteEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event not found: " + eventId
                ));

        eventRepository.delete(event);
    }

}
