package com.spring.authentication.service;

import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import com.spring.authentication.entity.RevokeToken;
import com.spring.authentication.repository.RevokeTokenRepository;

@Service
public class JwtService {

	private final SecretKey secretKey;
	private final long expirationMs;
	private final RevokeTokenRepository revokedTokenRepository;

	public JwtService(@Value("${app.jwt.secret}") String secret, 
			@Value("${app.jwt.expiration-ms}") long expirationMs, RevokeTokenRepository revokedTokenRepository) {

		byte[] keyBytes = Base64.getDecoder().decode(secret);
		this.secretKey = Keys.hmacShaKeyFor(keyBytes);
		this.expirationMs = expirationMs;
		this.revokedTokenRepository= revokedTokenRepository; 
	}

	public String generateToken(String email) {

		Date now = new Date();
		Date expiry = new Date(now.getTime() + expirationMs);

		return Jwts.builder()
				.id(UUID.randomUUID().toString()) // by using this token will be rovoked
				
				.subject(email)
				.issuedAt(now)
				.expiration(expiry)
				.signWith(secretKey)
				.compact();
	}
	
	public JwtDecoder jwtDecoder() {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
				.build();
		
		OAuth2TokenValidator<Jwt> revokedTokenValidator = jwt ->{
			String jti = jwt.getId();
			
			if(jti != null && revokedTokenRepository.existsByJti(jti)) {
				return OAuth2TokenValidatorResult.failure(new OAuth2Error(
						"Invalid_token",
						"Token has been revoked",
						null
					)
				);
			}
			
			if(jti == null) {
				return OAuth2TokenValidatorResult.failure(new OAuth2Error(
						"Invalid_token",
						"Token has been revoked",
						null
					)
				);
			}
			
			return OAuth2TokenValidatorResult.success();
		};
		
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefault(),
				revokedTokenValidator
				)
			);
		
		return decoder;
	}
	
	public void revokedToken(String token) {
		var claims = Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
		
		String jti = claims.getId();
		Date expiration = claims.getExpiration();
		
		if(jti == null || expiration == null) {
			throw new IllegalArgumentException("Token ID or expiration is missing");
		}
		
		if(expiration.toInstant().isAfter(java.time.Instant.now())) {
			revokedTokenRepository
			.save(
					new RevokeToken(jti, 
							expiration.toInstant()));
		}
	}

}
