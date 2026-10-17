package com.spring.authentication.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegisterUser {

	@NotBlank(message = "Name is required")
	@Size(max = 100, message = "Name can't be esceed 100 characters")
	private String name;
	
	@NotBlank(message = "Email is required")
	@Size(max = 190, message = "Email can't be exceed 190 characters")
	private String email;
	
	@NotBlank(message = "Password is required")
	@Size(min = 12, max = 72, message = "Password must be between 12 and 72 characters")
	private String password;
}
