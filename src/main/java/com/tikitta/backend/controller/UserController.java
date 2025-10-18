package com.tikitta.backend.controller;

import com.tikitta.backend.repository.ManagerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final ManagerRepository managerRepository;

    @GetMapping("/{managerId}/main")
    public ResponseEntity<String> getMainPage(@PathVariable("managerId") Long managerId){
        return ResponseEntity.ok("생성 성공");
    }
}
