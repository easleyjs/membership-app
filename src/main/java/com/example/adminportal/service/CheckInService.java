package com.example.adminportal.service;

import com.example.adminportal.dto.CheckInResponse;
import com.example.adminportal.entity.CheckIn;
import com.example.adminportal.repository.CheckInRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CheckInService {
    CheckInRepository checkInRepository;


    public CheckInService(CheckInRepository checkInRepository) {
        this.checkInRepository = checkInRepository;
    }

    //Perform a check-in


    public List<CheckInResponse> getCheckInsByMember(UUID memberId) {
        return checkInRepository.findByMember_Id(memberId)
                .stream()
                .map(CheckInResponse::from)
                .toList();
    }

    //TODO: Later, will probably need a date range search and by-event and such.

}
