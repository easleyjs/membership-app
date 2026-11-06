package com.example.adminportal.controller;

import com.example.adminportal.dto.EventCreateRequest;
import com.example.adminportal.dto.EventResponse;
import com.example.adminportal.service.EventService;
import jakarta.validation.Valid;
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
    public EventResponse getEventById(@RequestParam Long id) {
        return eventService.getEvent(id);
    }

    @PostMapping("")
    public EventResponse createEvent(@Valid EventCreateRequest eventRequest) {
        return eventService.createEvent(eventRequest);
    }

    @PatchMapping("/{id}")
    public EventResponse updateEvent(@RequestParam Long id, @Valid @RequestBody EventCreateRequest eventRequest) {
        return eventService.updateEvent(id, eventRequest);
    }

    @PatchMapping("/{id}/disable")
    public EventResponse disableEvent(@RequestParam Long id) {
        return eventService.disableEvent(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@RequestParam Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

}
