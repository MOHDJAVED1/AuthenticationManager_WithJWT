package com.spring.authentication.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserResponseDTO {
	
	private Long id;
	private String name;
	private String email;
	private String role;
	private boolean enabled;

}
