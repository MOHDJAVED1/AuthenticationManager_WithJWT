package com.spring.authentication.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.spring.authentication.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);
	
	Optional<User> findByEmailAndRole(String email, String role);
	
	boolean existsByEmail(String email);
	
	boolean existsByEmailAndRole(String email, String role);
	
	List<User> findAll();
}
