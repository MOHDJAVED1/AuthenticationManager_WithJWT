package com.spring.authentication.exceptions;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
	
	@ExceptionHandler
	public ResponseEntity<Map<String, Object>> validationFaliure(MethodArgumentNotValidException ex){
		
		Map<String, Object> response = new HashMap<>();
		Map<String, String> errors = new HashMap<>();
		
		ex.getBindingResult().getFieldErrors().forEach(error ->{
			errors.putIfAbsent(error.getField(), error.getDefaultMessage());
		});
		
		response.put("Status", 400);
		response.put("message", "Validation Failed");
		
		response.put("error", errors);
		
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

}
