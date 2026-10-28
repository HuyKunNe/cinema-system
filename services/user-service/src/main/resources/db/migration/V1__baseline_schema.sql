-- Fresh-database schema baseline.
-- Consolidated from the previous versioned migrations.

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `email_verification_tokens` (
  `id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL,
  `token_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `used_at` datetime(6) DEFAULT NULL,
  `revoked_at` datetime(6) DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_email_verification_tokens_hash` (`token_hash`),
  KEY `idx_email_verification_tokens_user_active` (`user_id`,`used_at`,`revoked_at`,`expires_at`),
  KEY `idx_email_verification_tokens_expires_at` (`expires_at`),
  CONSTRAINT `fk_email_verification_tokens_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `chk_email_verification_token_expiration` CHECK ((`expires_at` > `created_at`)),
  CONSTRAINT `chk_email_verification_token_hash` CHECK (regexp_like(`token_hash`,_ascii'^[0-9a-f]{64}$')),
  CONSTRAINT `chk_email_verification_token_terminal_state` CHECK (((`used_at` is null) or (`revoked_at` is null)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oauth2_authorization` (
  `id` varchar(100) NOT NULL,
  `registered_client_id` varchar(100) NOT NULL,
  `principal_name` varchar(200) NOT NULL,
  `authorization_grant_type` varchar(100) NOT NULL,
  `authorized_scopes` varchar(1000) DEFAULT NULL,
  `attributes` blob,
  `state` varchar(500) DEFAULT NULL,
  `authorization_code_value` blob,
  `authorization_code_issued_at` timestamp NULL DEFAULT NULL,
  `authorization_code_expires_at` timestamp NULL DEFAULT NULL,
  `authorization_code_metadata` blob,
  `access_token_value` blob,
  `access_token_issued_at` timestamp NULL DEFAULT NULL,
  `access_token_expires_at` timestamp NULL DEFAULT NULL,
  `access_token_metadata` blob,
  `access_token_type` varchar(100) DEFAULT NULL,
  `access_token_scopes` varchar(1000) DEFAULT NULL,
  `oidc_id_token_value` blob,
  `oidc_id_token_issued_at` timestamp NULL DEFAULT NULL,
  `oidc_id_token_expires_at` timestamp NULL DEFAULT NULL,
  `oidc_id_token_metadata` blob,
  `refresh_token_value` blob,
  `refresh_token_issued_at` timestamp NULL DEFAULT NULL,
  `refresh_token_expires_at` timestamp NULL DEFAULT NULL,
  `refresh_token_metadata` blob,
  `user_code_value` blob,
  `user_code_issued_at` timestamp NULL DEFAULT NULL,
  `user_code_expires_at` timestamp NULL DEFAULT NULL,
  `user_code_metadata` blob,
  `device_code_value` blob,
  `device_code_issued_at` timestamp NULL DEFAULT NULL,
  `device_code_expires_at` timestamp NULL DEFAULT NULL,
  `device_code_metadata` blob,
  PRIMARY KEY (`id`),
  KEY `idx_oauth2_authorization_principal` (`principal_name`),
  KEY `idx_oauth2_authorization_client` (`registered_client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oauth2_authorization_consent` (
  `registered_client_id` varchar(100) NOT NULL,
  `principal_name` varchar(200) NOT NULL,
  `authorities` varchar(1000) NOT NULL,
  PRIMARY KEY (`registered_client_id`,`principal_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oauth2_refresh_token_history` (
  `id` binary(16) NOT NULL,
  `authorization_id` varchar(100) NOT NULL,
  `registered_client_id` varchar(100) NOT NULL,
  `principal_name` varchar(200) NOT NULL,
  `token_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `status` varchar(20) NOT NULL,
  `issued_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `rotated_at` datetime(6) DEFAULT NULL,
  `revoked_at` datetime(6) DEFAULT NULL,
  `reused_at` datetime(6) DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_oauth2_refresh_token_history_hash` (`token_hash`),
  KEY `idx_oauth2_refresh_token_history_authorization` (`authorization_id`),
  KEY `idx_oauth2_refresh_token_history_client_principal` (`registered_client_id`,`principal_name`),
  KEY `idx_oauth2_refresh_token_history_status_expires` (`status`,`expires_at`),
  CONSTRAINT `fk_oauth2_refresh_token_history_authorization` FOREIGN KEY (`authorization_id`) REFERENCES `oauth2_authorization` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_oauth2_refresh_token_history_hash` CHECK (regexp_like(`token_hash`,_ascii'^[0-9a-f]{64}$')),
  CONSTRAINT `chk_oauth2_refresh_token_history_reused_time` CHECK (((`reused_at` is null) or ((`rotated_at` is not null) and (`reused_at` >= `rotated_at`)))),
  CONSTRAINT `chk_oauth2_refresh_token_history_revoked_time` CHECK (((`revoked_at` is null) or (`revoked_at` >= `issued_at`))),
  CONSTRAINT `chk_oauth2_refresh_token_history_rotated_time` CHECK (((`rotated_at` is null) or (`rotated_at` >= `issued_at`))),
  CONSTRAINT `chk_oauth2_refresh_token_history_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'ROTATED',_utf8mb4'REVOKED',_utf8mb4'REUSED'))),
  CONSTRAINT `chk_oauth2_refresh_token_history_time` CHECK ((`expires_at` > `issued_at`)),
  CONSTRAINT `chk_oauth2_refresh_token_history_transition_time` CHECK ((((`status` = _utf8mb4'ACTIVE') and (`rotated_at` is null) and (`revoked_at` is null) and (`reused_at` is null)) or ((`status` = _utf8mb4'ROTATED') and (`rotated_at` is not null) and (`revoked_at` is null) and (`reused_at` is null)) or ((`status` = _utf8mb4'REVOKED') and (`revoked_at` is not null) and (`reused_at` is null)) or ((`status` = _utf8mb4'REUSED') and (`rotated_at` is not null) and (`reused_at` is not null) and (`revoked_at` is null))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oauth2_registered_client` (
  `id` varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `client_id` varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `client_secret` varchar(200) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  `client_id_issued_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `client_secret_expires_at` timestamp(6) NULL DEFAULT NULL,
  `client_name` varchar(200) NOT NULL,
  `client_authentication_methods` varchar(1000) NOT NULL,
  `authorization_grant_types` varchar(1000) NOT NULL,
  `redirect_uris` varchar(1000) DEFAULT NULL,
  `post_logout_redirect_uris` varchar(1000) DEFAULT NULL,
  `scopes` varchar(1000) NOT NULL,
  `client_settings` varchar(2000) NOT NULL,
  `token_settings` varchar(2000) NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_oauth2_registered_client_client_id` (`client_id`),
  KEY `idx_oauth2_registered_client_active` (`active`),
  CONSTRAINT `chk_oauth2_registered_client_client_id` CHECK ((char_length(trim(`client_id`)) > 0)),
  CONSTRAINT `chk_oauth2_registered_client_id` CHECK ((char_length(trim(`id`)) > 0)),
  CONSTRAINT `chk_oauth2_registered_client_name` CHECK ((char_length(trim(`client_name`)) > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oauth2_revocation_audit_events` (
  `id` binary(16) NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `target_type` varchar(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `target_reference` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `reason` varchar(50) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `actor_user_id` binary(16) DEFAULT NULL,
  `actor_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `revoked_authorization_count` int NOT NULL,
  `occurred_at` timestamp(6) NOT NULL,
  `created_at` timestamp(6) NOT NULL,
  `updated_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_oauth2_revocation_audit_target` (`target_type`,`target_reference`,`occurred_at`),
  KEY `idx_oauth2_revocation_audit_actor` (`actor_user_id`,`occurred_at`),
  KEY `idx_oauth2_revocation_audit_reason` (`reason`,`occurred_at`),
  CONSTRAINT `chk_oauth2_revocation_audit_count` CHECK ((`revoked_authorization_count` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permissions` (
  `id` binary(16) NOT NULL,
  `code` varchar(100) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permissions_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_permissions` (
  `role_id` binary(16) NOT NULL,
  `permission_id` binary(16) NOT NULL,
  `assigned_at` datetime(6) NOT NULL,
  `assigned_by_user_id` binary(16) DEFAULT NULL,
  PRIMARY KEY (`role_id`,`permission_id`),
  KEY `idx_role_permissions_permission` (`permission_id`),
  KEY `idx_role_permissions_assigned_by` (`assigned_by_user_id`),
  CONSTRAINT `fk_role_permissions_assigned_by` FOREIGN KEY (`assigned_by_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_role_permissions_permission` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`),
  CONSTRAINT `fk_role_permissions_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id` binary(16) NOT NULL,
  `name` varchar(50) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_roles_name` (`name`),
  CONSTRAINT `chk_roles_name` CHECK ((`name` in (_utf8mb4'USER',_utf8mb4'STAFF',_utf8mb4'ADMIN',_utf8mb4'SERVICE')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `security_audit_events` (
  `id` binary(16) NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `event_type` varchar(60) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `actor_type` varchar(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `actor_reference` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `target_type` varchar(30) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  `target_reference` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `outcome` varchar(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `correlation_id` varchar(100) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  `reason` varchar(100) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  `metadata` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `occurred_at` timestamp(6) NOT NULL,
  `created_at` timestamp(6) NOT NULL,
  `updated_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_security_audit_event_time` (`event_type`,`occurred_at`),
  KEY `idx_security_audit_actor` (`actor_type`,`actor_reference`,`occurred_at`),
  KEY `idx_security_audit_target` (`target_type`,`target_reference`,`occurred_at`),
  KEY `idx_security_audit_outcome` (`outcome`,`occurred_at`),
  KEY `idx_security_audit_correlation` (`correlation_id`),
  CONSTRAINT `chk_security_audit_target_pair` CHECK ((((`target_type` is null) and (`target_reference` is null)) or ((`target_type` is not null) and (`target_reference` is not null))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_credentials` (
  `id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `password_hash_algorithm` varchar(50) NOT NULL,
  `password_changed_at` datetime(6) NOT NULL,
  `failed_attempt_count` int NOT NULL DEFAULT '0',
  `last_failed_at` datetime(6) DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_credentials_user` (`user_id`),
  CONSTRAINT `fk_user_credentials_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `chk_user_credentials_failed_attempts` CHECK ((`failed_attempt_count` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_profiles` (
  `id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL,
  `first_name` varchar(100) DEFAULT NULL,
  `last_name` varchar(100) DEFAULT NULL,
  `phone_number` varchar(32) DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_profiles_user` (`user_id`),
  CONSTRAINT `fk_user_profiles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles` (
  `user_id` binary(16) NOT NULL,
  `role_id` binary(16) NOT NULL,
  `assigned_at` datetime(6) NOT NULL,
  `assigned_by_user_id` binary(16) DEFAULT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `idx_user_roles_role` (`role_id`),
  KEY `idx_user_roles_assigned_by` (`assigned_by_user_id`),
  CONSTRAINT `fk_user_roles_assigned_by` FOREIGN KEY (`assigned_by_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_user_roles_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`),
  CONSTRAINT `fk_user_roles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` binary(16) NOT NULL,
  `email` varchar(320) NOT NULL,
  `normalized_email` varchar(320) NOT NULL,
  `username` varchar(100) NOT NULL,
  `normalized_username` varchar(100) NOT NULL,
  `status` varchar(50) NOT NULL DEFAULT 'PENDING_VERIFICATION',
  `email_verified_at` datetime(6) DEFAULT NULL,
  `locked_at` datetime(6) DEFAULT NULL,
  `disabled_at` datetime(6) DEFAULT NULL,
  `last_login_at` datetime(6) DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_normalized_email` (`normalized_email`),
  UNIQUE KEY `uk_users_normalized_username` (`normalized_username`),
  KEY `idx_users_status` (`status`),
  CONSTRAINT `chk_users_status` CHECK ((`status` in (_utf8mb4'PENDING_VERIFICATION',_utf8mb4'ACTIVE',_utf8mb4'LOCKED',_utf8mb4'DISABLED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
