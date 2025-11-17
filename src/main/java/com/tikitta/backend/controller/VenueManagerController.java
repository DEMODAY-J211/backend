package com.tikitta.backend.controller;

import com.tikitta.backend.dto.venue.VenueRegisterRequest;
import com.tikitta.backend.dto.venue.VenueSeatmapRequest;
import com.tikitta.backend.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

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

    @PostMapping("/seatmap")
    public ResponseEntity<?> registerSeatmap(@RequestBody VenueSeatmapRequest request) {
        locationService.registerSeatmap(request);
        return ResponseEntity.ok().body(Map.of(
                "success", true,
                "code", 200,
                "message", "success: 저장 완료~",
                "data", Map.of("location", request.getLocation())
        ));
    }
}