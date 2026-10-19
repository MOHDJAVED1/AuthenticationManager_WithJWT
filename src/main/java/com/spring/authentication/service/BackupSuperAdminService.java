package com.spring.authentication.service;

import java.nio.file.AccessDeniedException;
import java.util.Locale;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.spring.authentication.entity.BackupSuperAdminRequest;
import com.spring.authentication.entity.Role;
import com.spring.authentication.entity.User;
import com.spring.authentication.entity.UserResponseDTO;
import com.spring.authentication.repository.UserRepository;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BackupSuperAdminService {

	private final PasswordEncoder passwordEncoder;
	private final UserRepository userRepository;

	@Transactional
	public UserResponseDTO createBackupSuperUser(@Valid BackupSuperAdminRequest request, String authenticationEmail) throws AccessDeniedException {

		String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);

		User creator = userRepository.findByEmail(authenticationEmail).orElseThrow(()->
		new IllegalArgumentException("Creator not found"));
		
		Optional<User> existingUser = userRepository.findByEmail(normalizedEmail);

		User user;
		
		if(creator.getRole() != Role.ROLE_SUPER_ADMIN || !"PRIMARY_SUPER_ADMIN".equals(creator.getAccountType())) {
			throw new AccessDeniedException("Only the primary user cand create a backup user");
		}

		if (existingUser.isPresent()) {

			user = existingUser.get();

			if (Role.ROLE_SUPER_ADMIN.equals(user.getRole())) {
				throw new IllegalArgumentException("This email already belongs to a super admin");
			}

			if (user.getRole() != Role.ROLE_USER && user.getRole() != Role.ROLE_ADMIN) {
				throw new IllegalArgumentException("This account can't be promoted");
			}

			user.setRole(Role.ROLE_SUPER_ADMIN);
			user.setAccountType("BACKUP_SUPER_ADMIN");
		} else {
			user = new User();
			user.setName(request.getName().trim());
			user.setEmail(normalizedEmail);
			user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

			user.setRole(Role.ROLE_SUPER_ADMIN);
			user.setAccountType("BACKUP_SUPER_ADMIN");
			user.setEnabled(true);

		}

		User savedUser = userRepository.save(user);

		return new UserResponseDTO(savedUser.getId(), savedUser.getName(), savedUser.getEmail(), savedUser.getRole().name(),
				savedUser.isEnabled());

	}

}
