package com.spring.authentication.config;

import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.spring.authentication.service.JwtService;

import io.jsonwebtoken.lang.Collections;

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
						"/api/v1/auth/logout",
						"/api/v1/auth/admin-registration"
						).permitAll()
						.requestMatchers("/error").permitAll()
						
				.requestMatchers("/api/v1/super-admin/**").hasRole("SUPER_ADMIN")
				.requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN","SUPER_ADMIN")
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtService.jwtDecoder())
				.jwtAuthenticationConverter(jwtauthenticationConverter())
						))
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
	
	@Bean
	public JwtAuthenticationConverter jwtauthenticationConverter() {
		
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		
		converter.setJwtGrantedAuthoritiesConverter(jwt ->{
			@Nullable
			String role = jwt.getClaimAsString("role");
			
			if(role == null || !Set.of(
					"ROLE_USER",
					"ROLE_ADMIN",
					"ROLE_SUPER_ADMIN"
					).contains(role)) {
				return Collections.emptyList();
			}
			
			return List.of(
					new org.springframework.security.core.authority.SimpleGrantedAuthority(role)
					);
		});
		
		return converter;
	}
}
