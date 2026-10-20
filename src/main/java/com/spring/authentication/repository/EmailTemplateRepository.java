package com.spring.authentication.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.spring.authentication.entity.EmailTemplate;


public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

	Optional<EmailTemplate> findByTemplateCode(String templateCode);
}
