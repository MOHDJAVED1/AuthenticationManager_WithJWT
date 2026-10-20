package com.spring.authentication.config;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.spring.authentication.entity.Role;
import com.spring.authentication.entity.User;
import com.spring.authentication.repository.UserRepository;

@Configuration
public class SuperAdminBootstrap {
	
	@Bean
	@ConditionalOnProperty(name = "app.bootstrap-super-admin-enabled", havingValue = "true")
	CommandLineRunner createSuperAdmin(UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			@Value("${app.bootstrap.super-admin.email:}")
			String email,
			@Value("${app.bootstrap.super-admin.password:}")
			String password
			) {
		return args ->{
			if(email.isBlank() || password.isBlank()) {
				return;
			}
			
			String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
			
			if(userRepository.existsByEmail(normalizedEmail)) {
				return;
			}
			
			User user = new User();
			user.setName("Super Admin");
			user.setEmail(normalizedEmail);
			user.setPasswordHash(passwordEncoder.encode(password));
			user.setRole(Role.ROLE_SUPER_ADMIN);
			user.setAccountType("PRIMARY_SUPER_ADMIN");
			user.setEnabled(true);
			userRepository.save(user);
		};
	}
}
