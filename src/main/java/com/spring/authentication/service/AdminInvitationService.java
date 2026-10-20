package com.spring.authentication.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.spring.authentication.entity.AdminInvitation;
import com.spring.authentication.entity.Role;
import com.spring.authentication.entity.User;
import com.spring.authentication.entity.UserResponseDTO;
import com.spring.authentication.repository.AdminInvitationRepository;
import com.spring.authentication.repository.UserRepository;

import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminInvitationService {
	
	@Value("${orgnization.name}")
    private String organizationName;
	
	@Value("${orgnization.email}")
	private String orgnizationEmail;
	
	@Value("${orgnization.phoneNumber}")
	private String ornizationPhoneNumber;

	private final PasswordEncoder passwordEncoder;
	private final AdminInvitationRepository adminInvitationRepository;
	private final UserRepository userRepository;
	private final EmailService emailService;
	
	private final SecureRandom secureRandom = new SecureRandom();
	
	@Transactional
	public String createAdminInvitation(String email, String createdByEmail) throws MessagingException {
		
		String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
		
		byte[] randomeBytes = new byte[32];
		
		secureRandom.nextBytes(randomeBytes);
		
		String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomeBytes);
		
		String tokenHash = hashToken(rawToken);
		
		AdminInvitation invitation = new AdminInvitation();
		invitation.setEmail(normalizedEmail);
		invitation.setTokenHash(tokenHash);
		invitation.setCreatedByEmail(createdByEmail);
		invitation.setExpiresAt(Instant.now().plus(90, ChronoUnit.MINUTES));
		
		adminInvitationRepository.save(invitation);
		
		String curlCommand =
		        "curl --location 'http://localhost:8080/api/v1/auth/admin-registration' "
		        + "--header 'Content-Type: application/json' "
		        + "--data-raw '{\"name\":\"Invited Admin\","
		        + "\"email\":\"" + email + "\","
		        + "\"password\":\"ChangeMe@123\","
		        + "\"token\":\"" + rawToken + "\"}'";

//		emailService.sendEmail(
//		        email,
//		        "Admin Registration Invitation",
//		        "Copy the command below into Postman using Import -> Raw text:\n\n"
//		                + curlCommand
//		);
		
		return rawToken;
	}
	
	private String hashToken(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		}catch(Exception ex) {
			throw new IllegalStateException("Unable to hash invitation token", ex);
		}
	}

	@Transactional
	public void registerAdmin(String name, String email, String password, String token) {
		String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
		
		String tokenHash = hashToken(token);
		
		AdminInvitation invitation = adminInvitationRepository.findByTokenHash(tokenHash).orElseThrow(() -> new IllegalArgumentException("Invalid invitation token"));
		
		if(!invitation.getEmail().equalsIgnoreCase(normalizedEmail)) {
			throw new IllegalArgumentException("Invitation email does not match");
		}
		
		if(invitation.getUsedAt() != null) {
			throw new IllegalArgumentException("Invitation has already been used");
		}
		
		if(userRepository.existsByEmail(normalizedEmail)) {
			throw new IllegalArgumentException("An account already exists for this email");
		}
		
		User user = new User();
		user.setName(name.trim());
		user.setEmail(normalizedEmail);
		user.setPasswordHash(passwordEncoder.encode(password));
		user.setRole(Role.ROLE_ADMIN);
		user.setAccountType("ADMIN");
		user.setEnabled(true);
		
		userRepository.save(user);
		
		invitation.setUsedAt(Instant.now());
		adminInvitationRepository.save(invitation);
		
		
	}

	public void createAdminDirectly(String name, String email, String password) {
		
		String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
		
		if(userRepository.existsByEmail(normalizedEmail)) {
			throw new IllegalArgumentException("An account already exist for this email");
		}
		
		User user = new User();
		user.setName(name);
		user.setEmail(normalizedEmail);
		user.setPasswordHash(passwordEncoder.encode(password));
		user.setRole(Role.ROLE_ADMIN);
		user.setAccountType("ADMIN");
		user.setEnabled(true);
		
		userRepository.save(user);
		
	}

	public List<UserResponseDTO> fetchAllUsers() {
		return userRepository.findAll()
				.stream()
				.map(this::mapToResponse)
				.toList();
		
	}
	
	// Hide user password we can't reveal it due to security reason
	private UserResponseDTO mapToResponse(User user) {
		return new UserResponseDTO(
				user.getId(),
				user.getName(),
				user.getEmail(),
				user.getRole().name(),
				user.isEnabled());
	}

	@Transactional
	public UserResponseDTO enableOrDisabledUsers(String email, String authenticatedEmail) throws MessagingException {
		String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
		
		User user = userRepository.findByEmail(normalizedEmail).orElseThrow(() ->
			new RuntimeException("User not found"));
		
		User authenticateUser = userRepository.findByEmail(authenticatedEmail).orElseThrow(() -> new RuntimeException("User not found"));
		
		if (user.getEmail().equalsIgnoreCase(authenticatedEmail)) {
	        throw new RuntimeException("You cannot change your own account status");
	    }
		
		if (authenticateUser.getRole() != Role.ROLE_SUPER_ADMIN) {
	        throw new RuntimeException("Unauthorized: Only super admins can change user status");
	    }
		
		boolean isPrimary = "PRIMARY_SUPER_ADMIN".equals(authenticateUser.getAccountType());
	    boolean isBackup = "BACKUP_SUPER_ADMIN".equals(authenticateUser.getAccountType());
		
	    if (isBackup) {
	        // Backup Super Admin CANNOT modify any Super Admin accounts
	        if (user.getRole() == Role.ROLE_SUPER_ADMIN) {
	            throw new RuntimeException("Backup super admins cannot enable or disable super admin accounts");
	        }
	    } else if (!isPrimary) {
	        // Fallback if accountType is something else entirely
	        throw new RuntimeException("Unauthorized account type");
	    }
		
		user.setEnabled(!user.isEnabled());

		User updatedUser = userRepository.save(user);
		
		String subject = !user.isEnabled() ? "Your Account Has Been Disabled" : "Your Account Has Been Re-enabled";

		String emailBody = !user.isEnabled() ? 
			    "<html><body>"
			    + "Hello "
			    + user.getName()
			    + "<br><br>"
			    + "Your account with "
			    + organizationName
			    + " has been disabled by the organization. You will not be able to access protected services while your account remains disabled"
			    + "<br><br>"
			    + "If you believe this action was taken in error or require assistance, please contact the organization using the details below."
			    + "<br><br>"
			    + "<h6>Organization Contact Details</h6>"
			    + "<ul>"
			    + "<li>Email: " + orgnizationEmail + " </li>"
			    + "<li>Phone Number: " + ornizationPhoneNumber + "</li>"
			    + "</ul>"
			    + "<br>"
			    + "Regards,<br>"
			    + organizationName + " Support Team"
			    + "</body></html>"
			    
			    : 
			    
			    "<html><body>"
			    + "Hello "
			    + user.getName()
			    + "<br><br>"
			    + "Good news! Your account with "
			    + organizationName
			    + " has been enabled by the organization. You can now log in and access the service permitted by your account role"
			    + "<br><br>"
			    + "If you need assistance, please contact us using the details below."
			    + "<br><br>"
			    + "<h6>Organization Contact Details</h6>"
			    + "<ul>"
			    + "<li>Email: " + orgnizationEmail + " </li>"
			    + "<li>Phone Number: " + ornizationPhoneNumber + "</li>"
			    + "</ul>"
			    + "<br>"
			    + "Regards,<br>"
			    + organizationName + " Support Team"
			    + "</body></html>";
		
//		emailService.sendEmail(normalizedEmail, subject, emailBody);
		
		return mapToResponse(updatedUser);
	}
}
