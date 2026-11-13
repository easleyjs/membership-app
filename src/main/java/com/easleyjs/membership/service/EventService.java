package com.easleyjs.membership.service;

import com.easleyjs.membership.dto.EventCreateRequest;
import com.easleyjs.membership.dto.EventResponse;
import com.easleyjs.membership.entity.Event;
import com.easleyjs.membership.entity.EventStatus;
import com.easleyjs.membership.exception.AppUserNotFoundException;
import com.easleyjs.membership.exception.ResourceNotFoundException;
import com.easleyjs.membership.repository.AppUserRepository;
import com.easleyjs.membership.repository.EventRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@Service
@Validated
public class EventService {
    private final EventRepository eventRepository;
    private final AppUserRepository appUserRepository;

    public EventService(EventRepository eventRepository, AppUserRepository appUserRepository) {
        this.eventRepository = eventRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public EventResponse createEvent(@Valid EventCreateRequest request, UUID creatorId) {
        var creator = appUserRepository.findById(creatorId)
                .orElseThrow(() -> new AppUserNotFoundException("Event creator account not found"));
        Event newEvent = new Event();

        newEvent.setName(request.name());
        newEvent.setStartsAt(request.startsAt());
        newEvent.setEndsAt(request.endsAt());
        newEvent.setMembershipRequired(request.membershipRequired());
        newEvent.setCoverRequired(request.coverRequired());
        newEvent.setLocation(request.location());
        newEvent.setStatus(EventStatus.ACTIVE);
        newEvent.setCreatedBy(creator);

        Event savedEvent = eventRepository.save(newEvent);

        return EventResponse.from(savedEvent);
    }

    @Transactional(readOnly = true)
    public EventResponse getEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event not found"
                ));

        return EventResponse.from(event);
    }

    @Transactional(readOnly = true)
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
