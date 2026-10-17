package com.spring.authentication.entity;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "revoked_tokens")
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class RevokeToken {

	@Id
	private String jti;
	
	private Instant expiresAt;
}
