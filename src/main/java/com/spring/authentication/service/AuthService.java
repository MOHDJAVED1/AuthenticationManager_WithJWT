package com.spring.authentication.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.spring.authentication.entity.EmailCode;
import com.spring.authentication.entity.LoginRequest;
import com.spring.authentication.entity.LoginResponse;
import com.spring.authentication.entity.RegisterResponse;
import com.spring.authentication.entity.RegisterUser;
import com.spring.authentication.entity.ResetPasswordRequest;
import com.spring.authentication.entity.Role;
import com.spring.authentication.entity.User;
import com.spring.authentication.repository.EmailTemplateRepository;
import com.spring.authentication.repository.UserRepository;

import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final EmailTemplateRepository emailTemplateRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final EmailService emailService;
	
	@Value("${orgnization.name}")
	private String orgnisationName;
	
	@Value("${orgnization.address}")
	private String orgnisationAddress;
	
	@Value("${security.login.max-failed-attempts}")
	private int maxFailedAttempts;
	
	@Value("${security.login.lock-duration-minutes}")
	private long lockDurationMinutes;

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
		
		Map<String, Object> emailInput = new HashMap<>();
		emailInput.put("company_name", orgnisationName);
		emailInput.put("username", email);
		emailInput.put("current_year", LocalDateTime.now().getYear());
		emailInput.put("company_address", orgnisationAddress);
		
		emailService.sendEmail(EmailCode.USER_CREATED.toString(), email, emailInput);

		return new RegisterResponse(savedUser.getId(), savedUser.getName(), savedUser.getEmail(),
				"User registered successfully");
	}

	public LoginResponse login(LoginRequest request) {

		String email = request.getEmail().trim().toLowerCase(java.util.Locale.ROOT);

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

		if(isAccountLocked(user)) {
			throw new ResponseStatusException(HttpStatus.LOCKED, 
					"Account is temporarily locked. Please try again later");
		}
		
		if (!user.isEnabled()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is disabled");
		}
		
		if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
			recordFailedLogin(user);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
		}
		
		user.setFailedLoginAttempts(0);
		user.setLockTime(null);
		userRepository.save(user);
		
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

//		emailService.sendEmail(
//		        email,
//		        "Reset Your Password - Secure Auth API",
//		        "Hello,\n\n"
//		        + "A password reset was requested for your account.\n\n"
//		        + "Import the following cURL command into Postman "
//		        + "using Import -> Raw text:\n\n"
//		        + curlCommand
//		        + "\n\nChange newPassword to your desired password "
//		        + "before sending the request.\n"
//		        + "This reset token expires according to your "
//		        + "existing reset-token expiry policy."
//		);

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
	
	// Is accout locked
	private boolean isAccountLocked(User user) {
		
		LocalDateTime lockTime = user.getLockTime();
		
		if(lockTime == null) {
			return false;
		}
		
		LocalDateTime unloackTime = lockTime.plusMinutes(lockDurationMinutes);
		
		if(LocalDateTime.now().isBefore(unloackTime)) {
			return true;
		}
		
		user.setFailedLoginAttempts(0);
		user.setLockTime(null);
		userRepository.save(user);
				
		return false;
	}
	
	// Record incorrect attemps
	
	private void recordFailedLogin(User user) {
		
		Integer attempts = user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts();
	
		attempts++;
		
		user.setFailedLoginAttempts(attempts);
		
		if(attempts >= maxFailedAttempts) {
			
			user.setLockTime(LocalDateTime.now());
			
			userRepository.save(user);
			
			throw new ResponseStatusException(HttpStatus.LOCKED, 
					"Account locked due to too many failed login attemps");
		}
		
		userRepository.save(user);
	}
	
	private record ResetEntry(String email, Instant expiresAt) {}

}
