package com.spring.authentication.entity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ResetPasswordRequest {

	@NotBlank
	@Email
	private String email;
	
	@NotBlank
	private String resetToken;
	
	@NotBlank
	@Size(min=12, max = 72)
	private String newPassword;
}
