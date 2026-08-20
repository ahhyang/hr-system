package com.hr.controller;

import com.hr.dto.Dtos;
import com.hr.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Dtos.AuthResponse register(@Valid @RequestBody Dtos.RegisterTenantRequest request) {
        return authService.registerTenant(request);
    }

    @PostMapping("/login")
    public Dtos.AuthResponse login(@Valid @RequestBody Dtos.LoginRequest request) {
        return authService.login(request);
    }
}
