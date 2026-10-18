package com.spring.authentication.entity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AdminRegistraionRequest {

	@NotBlank
	@Size(max = 100)
	private String name;
	
	@NotBlank
	@Email
	@Size(max = 255)
	private String email;
	
	@NotBlank
	@Size(min = 12, max = 72)
	private String password;
	
	@NotBlank
	private String token;
}
