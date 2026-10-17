package com.spring.authentication.service;

import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.spring.authentication.entity.LoginRequest;
import com.spring.authentication.entity.RegisterResponse;
import com.spring.authentication.entity.RegisterUser;
import com.spring.authentication.entity.ResetPasswordRequest;
import com.spring.authentication.entity.User;
import com.spring.authentication.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	
	private final ConcurrentHashMap<String, ResetEntry> resetToken = new ConcurrentHashMap<>();

	@Transactional
	public RegisterResponse registerUser(RegisterUser request) {

		String email = request.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
		System.out.println("Email id : " + email);
		
		if (userRepository.existsByEmail(email)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
		}

		User user = new User();
		user.setName(request.getName().trim());
		user.setEmail(email);
		user.setRole("ROLE_USER");
		user.setEnabled(true);
		user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

		User savedUser = userRepository.save(user);

		return new RegisterResponse(savedUser.getId(), savedUser.getName(), savedUser.getEmail(),
				"User registered successfully");
	}

	public String login(LoginRequest request) {
		
		String email = request.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
		
		User user = userRepository.findByEmail(email).orElseThrow(
				() -> new ResponseStatusException(HttpStatus
						.UNAUTHORIZED, "Invalid email or password"));
		
		if(!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
			throw new ResponseStatusException(HttpStatus
					.UNAUTHORIZED, "Invalid email or password");
		}
		
		if(!user.isEnabled()) {
			throw new ResponseStatusException(HttpStatus
					.UNAUTHORIZED, "Invalid email or password");
		}
		
		return jwtService.generateToken(email);
	}

	public void logout(String token) {
		jwtService.revokedToken(token);
	}

	public String requestReset(String email) {
		
		String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
		
		User user = userRepository.findByEmail(normalizedEmail).orElseThrow(() -> 
		new ResponseStatusException(
				HttpStatus.NOT_FOUND, "User not found"));
		
		byte[] bytes = new byte[32];
		
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		resetToken.put(token, new ResetEntry(user.getEmail(), Instant.now().plusSeconds(600)));

		return token;
	}
	
	
	public void resetPassowrd(ResetPasswordRequest request) {
		
		String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
		
		ResetEntry entry = resetToken.get(request.getResetToken());
		
		if(entry == null || !entry.email.equalsIgnoreCase(email) || !entry.expiresAt.isAfter(Instant.now())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token");
		}
		
		User user = userRepository.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token"));
		
		user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
		
		userRepository.save(user);
		
		
		// One time use only
		resetToken.remove(request.getResetToken());
	}
	
	private record ResetEntry(String email, Instant expiresAt) {
		
	}
}
