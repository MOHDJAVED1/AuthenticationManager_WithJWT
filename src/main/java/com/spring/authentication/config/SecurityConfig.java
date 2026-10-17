package com.spring.authentication.config;

import com.spring.authentication.service.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final JwtService jwtService;

	SecurityConfig(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) {
		return http.csrf(
//						Customizer.withDefaults()

				AbstractHttpConfigurer::disable).httpBasic(
//						Customizer.withDefaults()

						AbstractHttpConfigurer::disable) // Enable basic authentication
				.formLogin(
//						Customizer.withDefaults()
						AbstractHttpConfigurer::disable) // Enable csrf with jsessionid

				.authorizeHttpRequests(authorize -> authorize.requestMatchers(
						org.springframework.http.HttpMethod.POST,
						"/api/v1/auth/register", 
						"/api/v1/auth/login",
						"/api/v1/auth/forgot-password",
						"/api/v1/auth/reset-password",
						"/api/v1/auth/logout"
						).permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtService.jwtDecoder())))
				.build();
	}

//	@Bean
//	public UserDetailsService userDetailsService() {
//
//		UserDetails user = User.withDefaultPasswordEncoder()
//				.username("user")
//				.password("password")
//				.roles("User")
//				.build();
//		
//		return new InMemoryUserDetailsManager(user);
//	}
}
