package com.tikitta.backend.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/kakao")
public class AuthRoleController {
    @PostMapping("/select-role")
    public ResponseEntity<String> selectRole(
            @RequestParam String role,
            HttpSession session) {

        // 선택한 role만 저장 (회원가입 여부는 없어도 됨)
        session.setAttribute("selectedRole", role.toUpperCase());
        return ResponseEntity.ok("Role saved to session");
    }
}
