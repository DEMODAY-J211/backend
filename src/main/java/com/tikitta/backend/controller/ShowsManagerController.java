package com.tikitta.backend.controller;

import com.tikitta.backend.dto.venue.VenueSeatmapResponse;
import com.tikitta.backend.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}