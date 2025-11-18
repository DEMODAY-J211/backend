package com.tikitta.backend.controller;

import com.tikitta.backend.domain.Location;
import com.tikitta.backend.dto.ApiResponse;
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
    public ResponseEntity<ApiResponse<Map<String, Long>>> registerVenue(
            @RequestPart("venueRegisterRequest") VenueRegisterRequest request,
            @RequestPart("locationPicture") MultipartFile locationPicture) {
        Location location = locationService.registerLocation(request, locationPicture);
        return ResponseEntity.ok(new ApiResponse<>(200, "공연장 등록: success", Map.of("location_id", location.getId())));
    }

    @PostMapping("/seatmap")
    public ResponseEntity<ApiResponse<Map<String, Long>>> registerSeatmap(@RequestBody VenueSeatmapRequest request) {
        locationService.registerSeatmap(request);
        return ResponseEntity.ok(new ApiResponse<>(200, "success: 저장 완료~", Map.of("location_id", request.getLocationId())));
    }
}