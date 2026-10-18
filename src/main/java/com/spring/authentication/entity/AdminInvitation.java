package com.spring.authentication.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "admin_invitations")
@Getter
@Setter
public class AdminInvitation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false)
	private String email;
	
	@Column(nullable = false, unique = true, length = 64)
	private String tokenHash;
	
	@Column(nullable = false)
	private Instant expiresAt;
	
	private Instant usedAt;
	
	@Column(nullable = false)
	private String createdByEmail;
	
	@Column(nullable = false)
	private Instant createdAt = Instant.now();
}
