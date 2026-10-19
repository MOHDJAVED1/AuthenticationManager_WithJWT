package com.spring.authentication.controller;

import java.nio.file.AccessDeniedException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spring.authentication.entity.BackupSuperAdminRequest;
import com.spring.authentication.entity.UserResponseDTO;
import com.spring.authentication.service.BackupSuperAdminService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/super-admin/backup-admins")
@RequiredArgsConstructor
public class BackupSuperAdminContoller {

	private final BackupSuperAdminService backupSuperAdminService;
	
	@PostMapping
	public ResponseEntity<UserResponseDTO> createBackupSuperAdmin(@Valid @RequestBody BackupSuperAdminRequest request, Authentication authentication) throws AccessDeniedException{
		UserResponseDTO resposne = backupSuperAdminService.createBackupSuperUser(request, authentication.getName());
		return ResponseEntity.status(HttpStatus.CREATED).body(resposne);
	}
}
