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
import com.spring.authentication.entity.LoginResponse;
import com.spring.authentication.entity.RegisterResponse;
import com.spring.authentication.entity.RegisterUser;
import com.spring.authentication.entity.ResetPasswordRequest;
import com.spring.authentication.entity.Role;
import com.spring.authentication.entity.User;
import com.spring.authentication.repository.UserRepository;

import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final EmailService emailService;

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
		user.setRole(Role.ROLE_USER);
		user.setAccountType("USER");
		user.setEnabled(true);
		user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

		User savedUser = userRepository.save(user);

		return new RegisterResponse(savedUser.getId(), savedUser.getName(), savedUser.getEmail(),
				"User registered successfully");
	}

	public LoginResponse login(LoginRequest request) {

		String email = request.getEmail().trim().toLowerCase(java.util.Locale.ROOT);

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

		if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
		}

		if (!user.isEnabled()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is disabled");
		}
		
		String token = jwtService.generateToken(user.getEmail(), user.getRole());

		return new LoginResponse(
				"Login Successful",
				token,
				user.getEmail(),
				user.getRole().name()
				);
	}

	public void logout(String token) {
		jwtService.revokedToken(token);
	}

	public String requestReset(String email) throws MessagingException {

		String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

		User user = userRepository.findByEmail(normalizedEmail)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		byte[] bytes = new byte[32];

		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		resetToken.put(token, new ResetEntry(user.getEmail(), Instant.now().plusSeconds(600)));
		
		String resetUrl =
		        "http://localhost:8080/api/v1/auth/reset-password";

		String curlCommand =
		        "curl --location '" + resetUrl + "' "
		        + "--header 'Content-Type: application/json' "
		        + "--data-raw '{\"email\":\"" + email + "\","
		        + "\"resetToken\":\"" + token + "\","
		        + "\"newPassword\":\"ChangeMe123!\"}'";

		emailService.sendEmail(
		        email,
		        "Reset Your Password - Secure Auth API",
		        "Hello,\n\n"
		        + "A password reset was requested for your account.\n\n"
		        + "Import the following cURL command into Postman "
		        + "using Import -> Raw text:\n\n"
		        + curlCommand
		        + "\n\nChange newPassword to your desired password "
		        + "before sending the request.\n"
		        + "This reset token expires according to your "
		        + "existing reset-token expiry policy."
		);

		return token;
	}

	public void resetPassowrd(ResetPasswordRequest request) {

		String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

		ResetEntry entry = resetToken.get(request.getResetToken());

		if (entry == null || !entry.email.equalsIgnoreCase(email) || !entry.expiresAt.isAfter(Instant.now())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token");
		}

		User user = userRepository.findByEmail(email).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token"));

		user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

		userRepository.save(user);

		// One time use only
		resetToken.remove(request.getResetToken());
	}

	private record ResetEntry(String email, Instant expiresAt) {

	}
}
