package com.spring.authentication.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.spring.authentication.entity.RevokeToken;

public interface RevokeTokenRepository extends JpaRepository<RevokeToken, String> {

	boolean existsByJti(String jti);
}
