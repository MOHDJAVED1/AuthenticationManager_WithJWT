package com.spring.authentication.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spring.authentication.entity.LoginRequest;
import com.spring.authentication.entity.RegisterResponse;
import com.spring.authentication.entity.RegisterUser;
import com.spring.authentication.entity.ResetPasswordRequest;
import com.spring.authentication.service.AuthenticationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthenticationService service;

	@PostMapping("/login")
	public String login(@RequestBody LoginRequest request) {
		return service.login(request);
	}

	@PostMapping("/logout")
	public ResponseEntity<String> logout(
			@RequestHeader(name = "Authorization", required = false) String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			return ResponseEntity.status(401).body("Bearer token is required");
		}

		String token = authorization.substring(7);
		service.logout(token);
		return ResponseEntity.ok("Logout successful");
	}

	@PostMapping("/register")
	public ResponseEntity<RegisterResponse> registerUser(@RequestBody @Valid RegisterUser request) {
		RegisterResponse response = service.registerUser(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/testingToken")
	public String testingToken() {
		return "Hellow tokent";
	}

	@PostMapping("/reset-password")
	public ResponseEntity<Map<String, String>> resetPassword(@RequestBody ResetPasswordRequest request) {
		service.resetPassowrd(request);
		return ResponseEntity.ok(Map.of("message", "Password reset successful"));
	}

	@PostMapping("/forgot-password")
	public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> request) {
		String email = request.get("email");

		if (email == null || email.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
		}

		String token = service.requestReset(email);

		return ResponseEntity.ok(Map.of("message", "Reset token generated for development", "resetToken", token));

	}

}
