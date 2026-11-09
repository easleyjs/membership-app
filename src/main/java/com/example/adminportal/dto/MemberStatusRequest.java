package com.example.adminportal.dto;

import com.example.adminportal.entity.Member;
import com.example.adminportal.entity.MemberStatus;

public record MemberStatusRequest(
        String reason
) {}
