package com.spring.authentication.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		 BCryptPasswordEncoder encodedPassword = new BCryptPasswordEncoder(12);
		 System.out.println("Encrypted password : " + encodedPassword);
		 return encodedPassword; 
	}

}
