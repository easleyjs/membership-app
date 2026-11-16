package com.easleyjs.membership.dto;

import java.util.UUID;

public record UserMemberMatch(UUID id, Long memberNumber, String firstName, String lastName, String email) {}
