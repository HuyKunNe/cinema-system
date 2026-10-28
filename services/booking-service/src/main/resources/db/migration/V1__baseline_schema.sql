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
CREATE TABLE `booking_seats` (
  `id` binary(16) NOT NULL,
  `booking_id` binary(16) NOT NULL,
  `inventory_seat_id` binary(16) DEFAULT NULL,
  `showtime_id` binary(16) NOT NULL,
  `seat_number` varchar(20) NOT NULL,
  `seat_type` varchar(50) DEFAULT NULL,
  `price` decimal(19,2) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_booking_seats_booking_seat` (`booking_id`,`showtime_id`,`seat_number`),
  KEY `idx_booking_seats_booking` (`booking_id`),
  CONSTRAINT `fk_booking_seats_booking` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`),
  CONSTRAINT `chk_booking_seats_price_non_negative` CHECK (((`price` is null) or (`price` >= 0))),
  CONSTRAINT `chk_booking_seats_snapshot` CHECK ((((`inventory_seat_id` is null) and (`seat_type` is null) and (`price` is null)) or ((`inventory_seat_id` is not null) and (`seat_type` is not null) and (`price` is not null))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bookings` (
  `id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL,
  `showtime_id` binary(16) NOT NULL,
  `client_request_id` varchar(100) NOT NULL,
  `request_fingerprint` char(64) NOT NULL,
  `status` varchar(50) NOT NULL,
  `total_amount` decimal(19,2) DEFAULT NULL,
  `currency` varchar(3) DEFAULT NULL,
  `expires_at` datetime(6) NOT NULL,
  `confirmed_at` datetime(6) DEFAULT NULL,
  `cancelled_at` datetime(6) DEFAULT NULL,
  `rejection_reason` varchar(500) DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bookings_user_client_request` (`user_id`,`client_request_id`),
  KEY `idx_bookings_user_created` (`user_id`,`created_at`),
  KEY `idx_bookings_status_expiration` (`status`,`expires_at`),
  CONSTRAINT `chk_bookings_money_snapshot` CHECK ((((`total_amount` is null) and (`currency` is null)) or ((`total_amount` is not null) and (`currency` is not null) and (char_length(`currency`) = 3)))),
  CONSTRAINT `chk_bookings_total_non_negative` CHECK (((`total_amount` is null) or (`total_amount` >= 0)))
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
  KEY `idx_outbox_events_aggregate` (`aggregate_type`,`aggregate_id`),
  KEY `idx_outbox_events_correlation` (`correlation_id`),
  KEY `idx_outbox_events_claim` (`status`,`next_attempt_at`,`processing_expires_at`,`created_at`),
  KEY `idx_outbox_events_processing_owner` (`processing_owner`,`status`),
  CONSTRAINT `chk_outbox_events_retry_count` CHECK ((`retry_count` >= 0)),
  CONSTRAINT `chk_outbox_events_status` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'PROCESSING',_utf8mb4'SENT',_utf8mb4'FAILED')))
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
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
