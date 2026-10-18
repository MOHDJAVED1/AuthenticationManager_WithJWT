package com.spring.authentication.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spring.authentication.entity.CreateAdminRequest;
import com.spring.authentication.service.AdminInvitationService;
import com.spring.authentication.service.EmailService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/super-admin")
@RequiredArgsConstructor
public class SuperAdminController {
	
	private final AdminInvitationService adminInvitationService;
	private final EmailService emailService;

	@GetMapping("/dashboard")
	public Map<String, String> dashboard(){
		return Map.of(
				"message", "Welcome to Super Admin Dashboard",
				"role", "ROLE_SUPER_ADMIN"
				);
	}
	
	@PostMapping("/admin-invitations")
	public ResponseEntity<?> createAdminInvitation(@RequestParam String email, Authentication authentication){
		
		String createdByEmail = authentication.getName();
		
		String token = adminInvitationService.createAdminInvitation(email, createdByEmail);
		
		return ResponseEntity.ok(Map.of(
				"message", "Admin invitation created successfully",
				"email", email,
				"token", token,
				"expiresInMinutes", 30
				));
	}
	
	@PostMapping("/admin")
	public ResponseEntity<?> createAdmin(@Valid @RequestBody CreateAdminRequest request){
		adminInvitationService.createAdminDirectly(
				request.getName(),
				request.getEmail(),
				request.getPassword()
				);
		
		return ResponseEntity.status(201).body(Map.of(
				"message", "Admin created successfully",
				"email", request.getEmail(),
				"role","ROLE_ADMIN"
				));
	}
	
	@PostMapping("/send-email")
	public ResponseEntity<?> sendTestEmail(@RequestParam String to){
		emailService.sendEmail(to, "SMTP Test - Secure Auth API",  "Congratualtions! Your Spring Boot Gmail SMTP" + "configuration is working");
		return ResponseEntity.ok(Map.of("message", "Test email sent successfully"));
	}
	
}
