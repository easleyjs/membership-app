package com.example.adminportal.controller;

import com.example.adminportal.dto.EventCreateRequest;
import com.example.adminportal.dto.EventResponse;
import com.example.adminportal.entity.Event;
import com.example.adminportal.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {
    EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("")
    public List<EventResponse> getAll() {
        return eventService.getAllEvents();
    }

    @GetMapping("/{id}")
    public EventResponse getEventById(@PathVariable Long id) {
        return eventService.getEvent(id);
    }

    @PostMapping("")
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody EventCreateRequest eventRequest
    ) {
        EventResponse createdEvent = eventService.createEvent(eventRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdEvent);
    }

    @PutMapping("/{id}")
    public EventResponse updateEvent(@PathVariable Long id, @Valid @RequestBody EventCreateRequest eventRequest) {
        return eventService.updateEvent(id, eventRequest);
    }

    @PatchMapping("/{id}/disable")
    public EventResponse disableEvent(@PathVariable Long id) {
        return eventService.disableEvent(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

}
