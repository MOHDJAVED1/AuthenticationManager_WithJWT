package com.spring.authentication.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.spring.authentication.entity.AdminInvitation;

public interface AdminInvitationRepository extends JpaRepository<AdminInvitation, Long> {

	Optional<AdminInvitation> findByTokenHash(String tokenHash);
	
	List<AdminInvitation> findByEmailIgnoreCase(String email);
	
	boolean existsByTokenHash(String tokenHash);
}
