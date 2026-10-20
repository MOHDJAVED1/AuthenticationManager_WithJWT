package com.spring.authentication.config;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {
	
	private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
	
	@Value("${security.rate-limit.general.capacity}")
	private int generalCapacity;
	
	@Value("${security.rate-limit.general.refill-minutes}")
	private long generalRefillMinutes;
	
	@Value("${security.rate-limit.login.capcity}")
	private int loginCapacity;
	
	@Value("${security.rate-limit.login.refill-minutes}")
	private long loginrefillMinutes;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		
		String clientIp = request.getRemoteAddr();
		log.debug("client IP : " + clientIp);
		
		String path = request.getRequestURI();
		System.out.println("Path : " + path);
		
		boolean isLogin = "POST".equalsIgnoreCase(request.getMethod())
				&& "/api/v1/auth/login".endsWith(path);
		System.out.println("Is login : " + isLogin);
				
		int capacity = isLogin ? loginCapacity : generalCapacity;
		System.out.println("Capacity : " + capacity);
		
		long refillMinutes = isLogin ? loginrefillMinutes : generalRefillMinutes;
		System.out.println("Refill : " + refillMinutes);
		
		String key = clientIp + ":" + (isLogin ? "LOGIN" : path);
		
		Bucket bucket = buckets.computeIfAbsent(key, k ->
			Bucket.builder()
			.addLimit(Bandwidth.classic(
					capacity, Refill.intervally(
							capacity, Duration.ofMinutes(refillMinutes)))).build()
			
				);
		
		if(bucket.tryConsume(1)) {
			filterChain.doFilter(request, response);
		}else {
			response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			response.setContentType("application/json");
			response.getWriter().write("{\"message\" : \"Too many request. Please try again later.\"}");
		}
		
		
	}
}
