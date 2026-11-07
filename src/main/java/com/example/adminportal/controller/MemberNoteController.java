package com.example.adminportal.controller;

import com.example.adminportal.dto.MemberNoteCreateRequest;
import com.example.adminportal.dto.MemberNoteResponse;
import com.example.adminportal.service.MemberNoteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members/{memberNumber}/notes")
public class MemberNoteController {
    MemberNoteService noteService;

    public MemberNoteController(MemberNoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    public Page<MemberNoteResponse> getNotes(
            @PathVariable Long memberNumber,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        return noteService.getNotes(memberNumber, pageable);
    }

    @PostMapping
    public ResponseEntity<MemberNoteResponse> createMemberNote(
            @PathVariable Long memberNumber,
            @Valid @RequestBody MemberNoteCreateRequest request
    ) {
        MemberNoteResponse createdNote = noteService.createMemberNote(memberNumber, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdNote);
    }

}
