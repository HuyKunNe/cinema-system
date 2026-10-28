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
CREATE TABLE `financial_audit_records` (
  `id` binary(16) NOT NULL,
  `payment_id` binary(16) NOT NULL,
  `action` varchar(50) NOT NULL,
  `actor_type` varchar(50) NOT NULL,
  `actor_id` varchar(255) NOT NULL,
  `reason` varchar(500) DEFAULT NULL,
  `metadata` varchar(2000) DEFAULT NULL,
  `correlation_id` binary(16) NOT NULL,
  `occurred_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_financial_audit_payment_occurred` (`payment_id`,`occurred_at`),
  KEY `idx_financial_audit_actor_occurred` (`actor_type`,`actor_id`,`occurred_at`),
  KEY `idx_financial_audit_action_occurred` (`action`,`occurred_at`),
  CONSTRAINT `fk_financial_audit_records_payment` FOREIGN KEY (`payment_id`) REFERENCES `payments` (`id`),
  CONSTRAINT `chk_financial_audit_records_action` CHECK ((`action` in (_utf8mb4'REFUND_REQUESTED',_utf8mb4'REFUND_SUCCEEDED',_utf8mb4'REFUND_FAILED',_utf8mb4'RECONCILIATION_OPENED',_utf8mb4'RECONCILIATION_RESOLVED',_utf8mb4'RECONCILIATION_REJECTED'))),
  CONSTRAINT `chk_financial_audit_records_actor_id` CHECK ((char_length(trim(`actor_id`)) > 0)),
  CONSTRAINT `chk_financial_audit_records_actor_type` CHECK ((`actor_type` in (_utf8mb4'USER',_utf8mb4'SERVICE',_utf8mb4'SYSTEM')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `outbox_events` (
  `id` binary(16) NOT NULL,
  `aggregate_type` varchar(50) NOT NULL,
  `aggregate_id` binary(16) NOT NULL,
  `event_type` varchar(100) NOT NULL,
  `event_version` varchar(20) NOT NULL,
  `topic` varchar(100) NOT NULL,
  `partition_key` varchar(100) NOT NULL,
  `occurred_at` datetime(6) NOT NULL,
  `correlation_id` binary(16) DEFAULT NULL,
  `causation_id` binary(16) DEFAULT NULL,
  `payload` longtext NOT NULL,
  `status` varchar(20) NOT NULL,
  `retry_count` int NOT NULL DEFAULT '0',
  `next_attempt_at` datetime(6) DEFAULT NULL,
  `last_error` varchar(2000) DEFAULT NULL,
  `processing_owner` varchar(150) DEFAULT NULL,
  `processing_started_at` datetime(6) DEFAULT NULL,
  `processing_expires_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_outbox_events_claim` (`status`,`next_attempt_at`,`processing_expires_at`,`created_at`),
  KEY `idx_outbox_events_processing_owner` (`processing_owner`,`status`),
  KEY `idx_outbox_events_aggregate` (`aggregate_type`,`aggregate_id`),
  KEY `idx_outbox_events_correlation` (`correlation_id`),
  CONSTRAINT `chk_outbox_events_processing_lease` CHECK ((((`status` = _utf8mb4'PROCESSING') and (`processing_owner` is not null) and (`processing_started_at` is not null) and (`processing_expires_at` is not null)) or ((`status` <> _utf8mb4'PROCESSING') and (`processing_owner` is null) and (`processing_started_at` is null) and (`processing_expires_at` is null)))),
  CONSTRAINT `chk_outbox_events_publication` CHECK ((((`status` = _utf8mb4'SENT') and (`published_at` is not null) and (`next_attempt_at` is null)) or ((`status` <> _utf8mb4'SENT') and (`published_at` is null)))),
  CONSTRAINT `chk_outbox_events_retry_count` CHECK ((`retry_count` >= 0)),
  CONSTRAINT `chk_outbox_events_status` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'PROCESSING',_utf8mb4'SENT',_utf8mb4'FAILED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_provider_webhook_events` (
  `id` binary(16) NOT NULL,
  `payment_transaction_id` binary(16) NOT NULL,
  `provider` varchar(50) NOT NULL,
  `provider_event_id` varchar(255) NOT NULL,
  `provider_reference` varchar(255) NOT NULL,
  `outcome` varchar(50) NOT NULL,
  `amount` decimal(19,2) NOT NULL,
  `currency` varchar(3) NOT NULL,
  `occurred_at` datetime(6) NOT NULL,
  `processed_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_provider_webhook_events_provider_event` (`provider`,`provider_event_id`),
  KEY `idx_payment_provider_webhook_events_transaction` (`payment_transaction_id`,`processed_at`),
  CONSTRAINT `fk_payment_provider_webhook_events_transaction` FOREIGN KEY (`payment_transaction_id`) REFERENCES `payment_transactions` (`id`),
  CONSTRAINT `chk_payment_provider_webhook_events_amount` CHECK ((`amount` >= 0)),
  CONSTRAINT `chk_payment_provider_webhook_events_currency` CHECK (regexp_like(`currency`,_utf8mb4'^[A-Z]{3}$',_utf8mb4'c')),
  CONSTRAINT `chk_payment_provider_webhook_events_outcome` CHECK ((`outcome` in (_utf8mb4'SUCCEEDED',_utf8mb4'FAILED',_utf8mb4'PENDING',_utf8mb4'UNKNOWN')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_transactions` (
  `id` binary(16) NOT NULL,
  `payment_id` binary(16) NOT NULL,
  `provider` varchar(50) NOT NULL,
  `transaction_type` varchar(50) NOT NULL,
  `attempt_number` int NOT NULL,
  `status` varchar(50) NOT NULL,
  `amount` decimal(19,2) NOT NULL,
  `currency` varchar(3) NOT NULL,
  `idempotency_key` varchar(200) NOT NULL,
  `provider_reference` varchar(255) DEFAULT NULL,
  `provider_event_id` varchar(255) DEFAULT NULL,
  `failure_code` varchar(100) DEFAULT NULL,
  `failure_message` varchar(500) DEFAULT NULL,
  `requested_at` datetime(6) NOT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `processing_owner` varchar(150) DEFAULT NULL,
  `processing_expires_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_transactions_provider_idempotency` (`provider`,`idempotency_key`),
  UNIQUE KEY `uk_payment_transactions_provider_event` (`provider`,`provider_event_id`),
  UNIQUE KEY `uk_payment_transactions_provider_reference` (`provider`,`provider_reference`),
  KEY `idx_payment_transactions_payment` (`payment_id`),
  KEY `idx_payment_transactions_claim` (`status`,`processing_expires_at`,`requested_at`,`id`),
  KEY `idx_payment_transactions_ready_claim` (`status`,`requested_at`,`id`),
  CONSTRAINT `fk_payment_transactions_payment` FOREIGN KEY (`payment_id`) REFERENCES `payments` (`id`),
  CONSTRAINT `chk_payment_transactions_amount` CHECK ((`amount` >= 0)),
  CONSTRAINT `chk_payment_transactions_attempt` CHECK ((`attempt_number` > 0)),
  CONSTRAINT `chk_payment_transactions_completion` CHECK ((((`status` in (_utf8mb4'SUCCEEDED',_utf8mb4'FAILED')) and (`completed_at` is not null)) or ((`status` not in (_utf8mb4'SUCCEEDED',_utf8mb4'FAILED')) and (`completed_at` is null)))),
  CONSTRAINT `chk_payment_transactions_currency` CHECK (regexp_like(`currency`,_utf8mb4'^[A-Z]{3}$',_utf8mb4'c')),
  CONSTRAINT `chk_payment_transactions_processing_lease` CHECK ((((`status` = _utf8mb4'PROCESSING') and (`processing_owner` is not null) and (`processing_expires_at` is not null)) or ((`status` <> _utf8mb4'PROCESSING') and (`processing_owner` is null) and (`processing_expires_at` is null)))),
  CONSTRAINT `chk_payment_transactions_status` CHECK ((`status` in (_utf8mb4'READY',_utf8mb4'PROCESSING',_utf8mb4'PENDING_PROVIDER',_utf8mb4'SUCCEEDED',_utf8mb4'FAILED'))),
  CONSTRAINT `chk_payment_transactions_type` CHECK ((`transaction_type` in (_utf8mb4'CHARGE',_utf8mb4'REFUND',_utf8mb4'RECONCILIATION')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payments` (
  `id` binary(16) NOT NULL,
  `booking_id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL,
  `payment_attempt` int NOT NULL,
  `amount` decimal(19,2) NOT NULL,
  `currency` varchar(3) NOT NULL,
  `provider` varchar(50) NOT NULL,
  `status` varchar(50) NOT NULL,
  `refund_status` varchar(50) NOT NULL,
  `provider_reference` varchar(255) DEFAULT NULL,
  `failure_code` varchar(100) DEFAULT NULL,
  `failure_message` varchar(500) DEFAULT NULL,
  `hold_expires_at` datetime(6) NOT NULL,
  `requested_at` datetime(6) NOT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `source_event_id` binary(16) NOT NULL,
  `correlation_id` binary(16) NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payments_booking_attempt` (`booking_id`,`payment_attempt`),
  UNIQUE KEY `uk_payments_source_event` (`source_event_id`),
  UNIQUE KEY `uk_payments_provider_reference` (`provider`,`provider_reference`),
  KEY `idx_payments_booking` (`booking_id`),
  KEY `idx_payments_user_created` (`user_id`,`created_at`),
  KEY `idx_payments_status_hold_expiration` (`status`,`hold_expires_at`),
  CONSTRAINT `chk_payments_amount` CHECK ((`amount` >= 0)),
  CONSTRAINT `chk_payments_attempt` CHECK ((`payment_attempt` > 0)),
  CONSTRAINT `chk_payments_completion` CHECK (((`completed_at` is null) or (`completed_at` >= `requested_at`))),
  CONSTRAINT `chk_payments_currency` CHECK (regexp_like(`currency`,_utf8mb4'^[A-Z]{3}$',_utf8mb4'c')),
  CONSTRAINT `chk_payments_hold_window` CHECK ((`hold_expires_at` > `requested_at`)),
  CONSTRAINT `chk_payments_refund_status` CHECK ((`refund_status` in (_utf8mb4'NOT_REQUESTED',_utf8mb4'PENDING',_utf8mb4'SUCCEEDED',_utf8mb4'FAILED'))),
  CONSTRAINT `chk_payments_status` CHECK ((`status` in (_utf8mb4'RECEIVED',_utf8mb4'PROCESSING',_utf8mb4'PENDING_PROVIDER',_utf8mb4'SUCCEEDED',_utf8mb4'FAILED',_utf8mb4'EXPIRED',_utf8mb4'RECONCILIATION_REQUIRED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `processed_events` (
  `id` binary(16) NOT NULL,
  `event_id` binary(16) NOT NULL,
  `consumer_name` varchar(100) NOT NULL,
  `event_type` varchar(100) NOT NULL,
  `event_version` varchar(20) NOT NULL,
  `processed_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_processed_events_event_consumer` (`event_id`,`consumer_name`),
  KEY `idx_processed_events_processed_at` (`processed_at`),
  KEY `idx_processed_events_type` (`event_type`,`processed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reconciliation_cases` (
  `id` binary(16) NOT NULL,
  `payment_id` binary(16) NOT NULL,
  `payment_transaction_id` binary(16) NOT NULL,
  `status` varchar(50) NOT NULL,
  `reason` varchar(100) NOT NULL,
  `resolution` varchar(100) DEFAULT NULL,
  `provider` varchar(50) NOT NULL,
  `provider_reference` varchar(255) DEFAULT NULL,
  `opened_at` datetime(6) NOT NULL,
  `resolved_at` datetime(6) DEFAULT NULL,
  `resolved_by_type` varchar(50) DEFAULT NULL,
  `resolved_by` varchar(255) DEFAULT NULL,
  `resolution_reason` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reconciliation_cases_transaction` (`payment_transaction_id`),
  KEY `idx_reconciliation_cases_payment` (`payment_id`),
  KEY `idx_reconciliation_cases_status_opened` (`status`,`opened_at`),
  KEY `idx_reconciliation_cases_provider_reference` (`provider`,`provider_reference`),
  CONSTRAINT `fk_reconciliation_cases_payment` FOREIGN KEY (`payment_id`) REFERENCES `payments` (`id`),
  CONSTRAINT `fk_reconciliation_cases_transaction` FOREIGN KEY (`payment_transaction_id`) REFERENCES `payment_transactions` (`id`),
  CONSTRAINT `chk_reconciliation_cases_reason` CHECK ((`reason` in (_utf8mb4'REFUND_PROVIDER_PENDING',_utf8mb4'REFUND_PROVIDER_UNKNOWN'))),
  CONSTRAINT `chk_reconciliation_cases_resolution` CHECK (((`resolution` is null) or (`resolution` in (_utf8mb4'REFUND_SUCCEEDED',_utf8mb4'REFUND_FAILED')))),
  CONSTRAINT `chk_reconciliation_cases_resolution_state` CHECK ((((`status` = _utf8mb4'OPEN') and (`resolution` is null) and (`resolved_at` is null) and (`resolved_by_type` is null) and (`resolved_by` is null)) or ((`status` = _utf8mb4'RESOLVED') and (`resolution` is not null) and (`resolved_at` is not null) and (`resolved_by_type` is not null) and (`resolved_by` is not null)) or ((`status` = _utf8mb4'REJECTED') and (`resolution` is null) and (`resolved_at` is not null) and (`resolved_by_type` is not null) and (`resolved_by` is not null)))),
  CONSTRAINT `chk_reconciliation_cases_resolver_type` CHECK (((`resolved_by_type` is null) or (`resolved_by_type` in (_utf8mb4'USER',_utf8mb4'SERVICE',_utf8mb4'SYSTEM')))),
  CONSTRAINT `chk_reconciliation_cases_status` CHECK ((`status` in (_utf8mb4'OPEN',_utf8mb4'RESOLVED',_utf8mb4'REJECTED')))
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
