package com.spring.authentication.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

	@GetMapping("/dashboard")
	public Map<String, String> dashboard(){
		return Map.of(
				"message", "Welcome to Admin dashboard",
				"role", "ROLE_ADMIN"
				);
	}
}
