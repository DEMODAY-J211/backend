package com.tikitta.backend.controller;

import com.tikitta.backend.dto.venue.VenueRegisterRequest;
import com.tikitta.backend.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/manager/venue")
@RequiredArgsConstructor
public class VenueManagerController {

    private final LocationService locationService;

    @PostMapping("/register")
    public ResponseEntity<?> registerVenue(
            @RequestPart("request") VenueRegisterRequest request,
            @RequestPart("locationPicture") MultipartFile locationPicture) {
        locationService.registerLocation(request, locationPicture);
        return ResponseEntity.ok().body("공연장 등록: success");
    }
}