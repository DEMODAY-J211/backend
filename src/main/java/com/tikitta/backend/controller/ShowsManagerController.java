package com.tikitta.backend.controller;

import com.tikitta.backend.dto.venue.SeatDeleteRequest;
import com.tikitta.backend.dto.venue.SeatDeleteResponse;
import com.tikitta.backend.dto.venue.VenueSeatmapResponse;
import com.tikitta.backend.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/manager/shows")
@RequiredArgsConstructor
public class ShowsManagerController {

    private final LocationService locationService;

    @GetMapping("/{locationId}/seatmap")
    public ResponseEntity<?> getSeatmap(@PathVariable Long locationId) {
        VenueSeatmapResponse response = locationService.getSeatmap(locationId);
        return ResponseEntity.ok().body(Map.of(
                "success", true,
                "code", 200,
                "message", "success",
                "data", response
        ));
    }

    @PostMapping("/{locationId}/seatmap/delete")
    public ResponseEntity<?> updateSeatmap(
            @PathVariable Long locationId,
            @RequestBody SeatDeleteRequest request) {
        SeatDeleteResponse response = locationService.updateSeatmap(locationId, request);
        return ResponseEntity.ok().body(Map.of(
                "success", true,
                "code", 200,
                "message", "success",
                "data", response
        ));
    }
}