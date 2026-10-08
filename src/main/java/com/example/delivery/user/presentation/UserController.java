package com.example.delivery.user.presentation;

import com.example.delivery.user.application.UserFacade;
import com.example.delivery.user.application.dto.LoginResponse;
import com.example.delivery.user.application.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserFacade userFacade;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signup(@Valid @RequestBody SignupRequest request) {
        return userFacade.signup(request.toCommand());
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return userFacade.login(request.toCommand());
    }
}
