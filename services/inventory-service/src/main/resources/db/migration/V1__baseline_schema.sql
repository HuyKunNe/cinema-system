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
CREATE TABLE `cinema_amenities` (
  `cinema_id` binary(16) NOT NULL,
  `amenity` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`cinema_id`,`amenity`),
  CONSTRAINT `fk_cinema_amenities_cinema` FOREIGN KEY (`cinema_id`) REFERENCES `cinemas` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cinema_pricing_policies` (
  `id` binary(16) NOT NULL,
  `creation_cinema_id` binary(16) NOT NULL,
  `client_request_id` binary(16) NOT NULL,
  `creation_fingerprint` varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `creation_scope_fingerprint` varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pricing_policy_creation_request` (`creation_cinema_id`,`client_request_id`),
  CONSTRAINT `fk_pricing_policy_creation_cinema` FOREIGN KEY (`creation_cinema_id`) REFERENCES `cinemas` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cinema_pricing_policy_versions` (
  `id` binary(16) NOT NULL,
  `policy_id` binary(16) NOT NULL,
  `revision_number` bigint NOT NULL,
  `name` varchar(120) NOT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `day_group` varchar(20) NOT NULL,
  `status` varchar(20) NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `fare_matrix_revision` bigint NOT NULL DEFAULT '0',
  `published_at` datetime(6) DEFAULT NULL,
  `published_by` varchar(255) DEFAULT NULL,
  `cinema_scope_revision` bigint NOT NULL DEFAULT '0',
  `draft_source_version_id` binary(16) DEFAULT NULL,
  `draft_client_request_id` binary(16) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cinema_pricing_policy_versions_revision` (`policy_id`,`revision_number`),
  UNIQUE KEY `uk_pricing_version_draft_request` (`policy_id`,`draft_client_request_id`),
  KEY `idx_pricing_version_draft_source` (`draft_source_version_id`),
  CONSTRAINT `fk_cinema_pricing_policy_versions_policy` FOREIGN KEY (`policy_id`) REFERENCES `cinema_pricing_policies` (`id`),
  CONSTRAINT `fk_pricing_version_draft_source` FOREIGN KEY (`draft_source_version_id`) REFERENCES `cinema_pricing_policy_versions` (`id`),
  CONSTRAINT `chk_cinema_pricing_policy_day_group` CHECK ((`day_group` in (_utf8mb4'WEEKDAY',_utf8mb4'WEEKEND',_utf8mb4'HOLIDAY'))),
  CONSTRAINT `chk_cinema_pricing_policy_revision` CHECK ((`revision_number` >= 1)),
  CONSTRAINT `chk_cinema_pricing_policy_status` CHECK ((`status` in (_utf8mb4'DRAFT',_utf8mb4'PUBLISHED',_utf8mb4'RETIRED'))),
  CONSTRAINT `chk_pricing_version_draft_origin` CHECK ((((`draft_source_version_id` is null) and (`draft_client_request_id` is null)) or ((`draft_source_version_id` is not null) and (`draft_client_request_id` is not null)))),
  CONSTRAINT `chk_pricing_version_draft_source` CHECK (((`draft_source_version_id` is null) or (`draft_source_version_id` <> `id`)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cinemas` (
  `id` binary(16) NOT NULL,
  `name` varchar(150) NOT NULL,
  `address` varchar(500) NOT NULL,
  `city` varchar(100) NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `phone` varchar(32) DEFAULT NULL,
  `email` varchar(254) DEFAULT NULL,
  `opening_hours` varchar(150) DEFAULT NULL,
  `directions` varchar(1000) DEFAULT NULL,
  `time_zone` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_cinemas_city_active` (`city`,`active`)
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
  CONSTRAINT `chk_outbox_events_retry_count` CHECK ((`retry_count` >= 0)),
  CONSTRAINT `chk_outbox_events_status` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'PROCESSING',_utf8mb4'SENT',_utf8mb4'FAILED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pricing_fare_cells` (
  `id` binary(16) NOT NULL,
  `policy_version_id` binary(16) NOT NULL,
  `experience_type` varchar(30) NOT NULL,
  `content_format` varchar(20) NOT NULL,
  `seat_type` varchar(20) NOT NULL,
  `state` varchar(24) NOT NULL,
  `amount` decimal(12,2) DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `time_band_id` binary(16) NOT NULL,
  `ticket_group_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pricing_fare_cell_definitions` (`policy_version_id`,`experience_type`,`content_format`,`time_band_id`,`ticket_group_id`,`seat_type`),
  KEY `fk_pricing_fare_cells_seat_type` (`policy_version_id`,`seat_type`),
  KEY `fk_pricing_fare_cells_time_band_definition` (`policy_version_id`,`time_band_id`),
  KEY `fk_pricing_fare_cells_ticket_group` (`policy_version_id`,`ticket_group_id`),
  CONSTRAINT `fk_pricing_fare_cells_scope` FOREIGN KEY (`policy_version_id`, `experience_type`, `content_format`) REFERENCES `pricing_fare_scopes` (`policy_version_id`, `experience_type`, `content_format`),
  CONSTRAINT `fk_pricing_fare_cells_seat_type` FOREIGN KEY (`policy_version_id`, `seat_type`) REFERENCES `pricing_fare_seat_types` (`policy_version_id`, `seat_type`),
  CONSTRAINT `fk_pricing_fare_cells_ticket_group` FOREIGN KEY (`policy_version_id`, `ticket_group_id`) REFERENCES `pricing_ticket_groups` (`policy_version_id`, `id`),
  CONSTRAINT `fk_pricing_fare_cells_time_band_definition` FOREIGN KEY (`policy_version_id`, `time_band_id`) REFERENCES `pricing_time_bands` (`policy_version_id`, `id`),
  CONSTRAINT `fk_pricing_fare_cells_version` FOREIGN KEY (`policy_version_id`) REFERENCES `cinema_pricing_policy_versions` (`id`),
  CONSTRAINT `chk_pricing_fare_cell_amount` CHECK ((((`state` = _utf8mb4'PRICED') and (`amount` is not null) and (`amount` >= 0)) or ((`state` = _utf8mb4'NOT_APPLICABLE') and (`amount` is null))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pricing_fare_scopes` (
  `policy_version_id` binary(16) NOT NULL,
  `experience_type` varchar(30) NOT NULL,
  `content_format` varchar(20) NOT NULL,
  PRIMARY KEY (`policy_version_id`,`experience_type`,`content_format`),
  CONSTRAINT `fk_pricing_fare_scopes_version` FOREIGN KEY (`policy_version_id`) REFERENCES `cinema_pricing_policy_versions` (`id`),
  CONSTRAINT `chk_pricing_fare_scope_experience` CHECK ((`experience_type` in (_utf8mb4'STANDARD',_utf8mb4'PREMIUM_CINEMA',_utf8mb4'IMAX',_utf8mb4'FOUR_DX',_utf8mb4'SCREEN_X'))),
  CONSTRAINT `chk_pricing_fare_scope_format` CHECK ((`content_format` in (_utf8mb4'TWO_D',_utf8mb4'THREE_D')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pricing_fare_seat_types` (
  `policy_version_id` binary(16) NOT NULL,
  `seat_type` varchar(20) NOT NULL,
  PRIMARY KEY (`policy_version_id`,`seat_type`),
  CONSTRAINT `fk_pricing_fare_seat_types_version` FOREIGN KEY (`policy_version_id`) REFERENCES `cinema_pricing_policy_versions` (`id`),
  CONSTRAINT `chk_pricing_fare_seat_type` CHECK ((`seat_type` in (_utf8mb4'STANDARD',_utf8mb4'VIP',_utf8mb4'COUPLE',_utf8mb4'ACCESSIBLE')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pricing_policy_version_cinemas` (
  `policy_version_id` binary(16) NOT NULL,
  `cinema_id` binary(16) NOT NULL,
  PRIMARY KEY (`policy_version_id`,`cinema_id`),
  KEY `idx_pricing_version_cinemas_cinema` (`cinema_id`,`policy_version_id`),
  CONSTRAINT `fk_pricing_version_cinemas_cinema` FOREIGN KEY (`cinema_id`) REFERENCES `cinemas` (`id`),
  CONSTRAINT `fk_pricing_version_cinemas_version` FOREIGN KEY (`policy_version_id`) REFERENCES `cinema_pricing_policy_versions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pricing_ticket_groups` (
  `id` binary(16) NOT NULL,
  `policy_version_id` binary(16) NOT NULL,
  `name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `eligibility_description` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `default_group` tinyint(1) NOT NULL,
  `sort_order` int NOT NULL,
  `legacy_category` varchar(30) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pricing_ticket_groups_owner` (`policy_version_id`,`id`),
  UNIQUE KEY `uk_pricing_ticket_groups_legacy` (`policy_version_id`,`legacy_category`),
  CONSTRAINT `fk_pricing_ticket_groups_version` FOREIGN KEY (`policy_version_id`) REFERENCES `cinema_pricing_policy_versions` (`id`),
  CONSTRAINT `chk_pricing_ticket_groups_default` CHECK ((`default_group` in (0,1))),
  CONSTRAINT `chk_pricing_ticket_groups_default_condition` CHECK (((`default_group` = 0) or (`eligibility_description` is null))),
  CONSTRAINT `chk_pricing_ticket_groups_legacy` CHECK (((`legacy_category` is null) or (`legacy_category` in (_utf8mb4'ADULT',_utf8mb4'SENIOR_55_PLUS',_utf8mb4'STUDENT_U22',_utf8mb4'CHILD')))),
  CONSTRAINT `chk_pricing_ticket_groups_order` CHECK ((`sort_order` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pricing_time_bands` (
  `id` binary(16) NOT NULL,
  `policy_version_id` binary(16) NOT NULL,
  `name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `start_minute` int NOT NULL,
  `end_minute` int NOT NULL,
  `sort_order` int NOT NULL,
  `legacy_code` varchar(20) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pricing_time_bands_owner` (`policy_version_id`,`id`),
  UNIQUE KEY `uk_pricing_time_bands_legacy` (`policy_version_id`,`legacy_code`),
  CONSTRAINT `fk_pricing_time_bands_version` FOREIGN KEY (`policy_version_id`) REFERENCES `cinema_pricing_policy_versions` (`id`),
  CONSTRAINT `chk_pricing_time_bands_end` CHECK (((`end_minute` >= 0) and (`end_minute` <= 1440))),
  CONSTRAINT `chk_pricing_time_bands_legacy` CHECK (((`legacy_code` is null) or (`legacy_code` in (_utf8mb4'BEFORE_17',_utf8mb4'FROM_17')))),
  CONSTRAINT `chk_pricing_time_bands_order` CHECK ((`sort_order` >= 0)),
  CONSTRAINT `chk_pricing_time_bands_range` CHECK ((`start_minute` <> `end_minute`)),
  CONSTRAINT `chk_pricing_time_bands_start` CHECK (((`start_minute` >= 0) and (`start_minute` < 1440)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
CREATE TABLE `room_layout_elements` (
  `id` binary(16) NOT NULL,
  `layout_id` binary(16) NOT NULL,
  `kind` varchar(30) NOT NULL,
  `label` varchar(150) DEFAULT NULL,
  `x` decimal(12,3) NOT NULL,
  `y` decimal(12,3) NOT NULL,
  `width` decimal(12,3) NOT NULL,
  `height` decimal(12,3) NOT NULL,
  `rotation_degrees` decimal(9,3) NOT NULL DEFAULT '0.000',
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_room_layout_elements_layout` (`layout_id`),
  CONSTRAINT `fk_room_layout_elements_layout` FOREIGN KEY (`layout_id`) REFERENCES `room_layouts` (`id`),
  CONSTRAINT `chk_room_layout_elements_kind` CHECK ((`kind` in (_utf8mb4'SCREEN',_utf8mb4'AISLE',_utf8mb4'EXIT'))),
  CONSTRAINT `chk_room_layout_elements_position` CHECK (((`x` >= 0) and (`y` >= 0))),
  CONSTRAINT `chk_room_layout_elements_size` CHECK (((`width` > 0) and (`height` > 0)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_layout_matrix_cells` (
  `id` binary(16) NOT NULL,
  `layout_id` binary(16) NOT NULL,
  `seat_id` binary(16) DEFAULT NULL,
  `row_index` int NOT NULL,
  `column_index` int NOT NULL,
  `seat_number_in_row` int NOT NULL,
  `seat_type` varchar(50) NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_layout_matrix_cells_position` (`layout_id`,`row_index`,`column_index`),
  UNIQUE KEY `uk_room_layout_matrix_cells_number` (`layout_id`,`row_index`,`seat_number_in_row`),
  UNIQUE KEY `uk_room_layout_matrix_cells_seat` (`layout_id`,`seat_id`),
  KEY `fk_room_layout_matrix_cells_seat` (`seat_id`),
  CONSTRAINT `fk_room_layout_matrix_cells_layout` FOREIGN KEY (`layout_id`) REFERENCES `room_layouts` (`id`),
  CONSTRAINT `fk_room_layout_matrix_cells_seat` FOREIGN KEY (`seat_id`) REFERENCES `seats` (`id`),
  CONSTRAINT `chk_room_layout_matrix_cells_indices` CHECK (((`row_index` between 0 and 99) and (`column_index` between 0 and 99))),
  CONSTRAINT `chk_room_layout_matrix_cells_number` CHECK ((`seat_number_in_row` between 1 and 100)),
  CONSTRAINT `chk_room_layout_matrix_cells_type` CHECK ((`seat_type` in (_utf8mb4'STANDARD',_utf8mb4'VIP',_utf8mb4'COUPLE',_utf8mb4'ACCESSIBLE')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_layout_seats` (
  `id` binary(16) NOT NULL,
  `layout_id` binary(16) NOT NULL,
  `seat_id` binary(16) NOT NULL,
  `seat_number_snapshot` varchar(20) NOT NULL,
  `row_label_snapshot` varchar(10) NOT NULL,
  `seat_type_snapshot` varchar(50) NOT NULL,
  `x` decimal(12,3) NOT NULL,
  `y` decimal(12,3) NOT NULL,
  `width` decimal(12,3) NOT NULL,
  `height` decimal(12,3) NOT NULL,
  `rotation_degrees` decimal(9,3) NOT NULL DEFAULT '0.000',
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_layout_seats_layout_seat` (`layout_id`,`seat_id`),
  KEY `fk_room_layout_seats_seat` (`seat_id`),
  CONSTRAINT `fk_room_layout_seats_layout` FOREIGN KEY (`layout_id`) REFERENCES `room_layouts` (`id`),
  CONSTRAINT `fk_room_layout_seats_seat` FOREIGN KEY (`seat_id`) REFERENCES `seats` (`id`),
  CONSTRAINT `chk_room_layout_seats_position` CHECK (((`x` >= 0) and (`y` >= 0))),
  CONSTRAINT `chk_room_layout_seats_size` CHECK (((`width` > 0) and (`height` > 0))),
  CONSTRAINT `chk_room_layout_seats_type` CHECK ((`seat_type_snapshot` in (_utf8mb4'STANDARD',_utf8mb4'VIP',_utf8mb4'COUPLE',_utf8mb4'ACCESSIBLE')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_layouts` (
  `id` binary(16) NOT NULL,
  `room_id` binary(16) NOT NULL,
  `layout_version` bigint NOT NULL,
  `status` varchar(30) NOT NULL,
  `canvas_width` decimal(12,3) NOT NULL,
  `canvas_height` decimal(12,3) NOT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `content_revision` bigint NOT NULL DEFAULT '0',
  `row_count` int DEFAULT NULL,
  `column_count` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_layouts_room_version` (`room_id`,`layout_version`),
  KEY `idx_room_layouts_room_status` (`room_id`,`status`),
  CONSTRAINT `fk_room_layouts_room` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `chk_room_layouts_canvas` CHECK (((`canvas_width` > 0) and (`canvas_height` > 0))),
  CONSTRAINT `chk_room_layouts_content_revision` CHECK ((`content_revision` >= 0)),
  CONSTRAINT `chk_room_layouts_layout_version` CHECK ((`layout_version` > 0)),
  CONSTRAINT `chk_room_layouts_matrix_dimensions` CHECK ((((`row_count` is null) and (`column_count` is null)) or ((`row_count` is not null) and (`column_count` is not null) and (`row_count` between 1 and 100) and (`column_count` between 1 and 100)))),
  CONSTRAINT `chk_room_layouts_publication` CHECK ((((`status` = _utf8mb4'DRAFT') and (`published_at` is null)) or ((`status` = _utf8mb4'PUBLISHED') and (`published_at` is not null)))),
  CONSTRAINT `chk_room_layouts_status` CHECK ((`status` in (_utf8mb4'DRAFT',_utf8mb4'PUBLISHED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_supported_content_formats` (
  `room_id` binary(16) NOT NULL,
  `content_format` varchar(20) NOT NULL,
  PRIMARY KEY (`room_id`,`content_format`),
  CONSTRAINT `fk_room_supported_content_formats_room` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `chk_room_supported_content_format` CHECK ((`content_format` in (_utf8mb4'TWO_D',_utf8mb4'THREE_D')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rooms` (
  `id` binary(16) NOT NULL,
  `cinema_id` binary(16) NOT NULL,
  `name` varchar(100) NOT NULL,
  `room_type` varchar(50) NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rooms_cinema_name` (`cinema_id`,`name`),
  KEY `idx_rooms_cinema_active` (`cinema_id`,`active`),
  CONSTRAINT `fk_rooms_cinema` FOREIGN KEY (`cinema_id`) REFERENCES `cinemas` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `seats` (
  `id` binary(16) NOT NULL,
  `room_id` binary(16) NOT NULL,
  `seat_number` varchar(20) NOT NULL,
  `row_label` varchar(10) NOT NULL,
  `seat_type` varchar(50) NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `layout_managed` tinyint(1) NOT NULL DEFAULT '0',
  `legacy_seat_number` varchar(20) GENERATED ALWAYS AS ((case when (`layout_managed` = false) then `seat_number` else NULL end)) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_seats_room_legacy_number` (`room_id`,`legacy_seat_number`),
  KEY `idx_seats_room_active` (`room_id`,`active`),
  CONSTRAINT `fk_seats_room` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `show_seats` (
  `id` binary(16) NOT NULL,
  `showtime_id` binary(16) NOT NULL,
  `seat_id` binary(16) NOT NULL,
  `seat_number` varchar(20) NOT NULL,
  `seat_type` varchar(50) NOT NULL,
  `price` decimal(12,2) NOT NULL,
  `status` varchar(30) NOT NULL,
  `held_by_booking_id` binary(16) DEFAULT NULL,
  `hold_expires_at` datetime(6) DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_show_seats_showtime_seat` (`showtime_id`,`seat_id`),
  UNIQUE KEY `uk_show_seats_showtime_number` (`showtime_id`,`seat_number`),
  KEY `fk_show_seats_seat` (`seat_id`),
  KEY `idx_show_seats_showtime_status` (`showtime_id`,`status`),
  KEY `idx_show_seats_booking` (`held_by_booking_id`),
  KEY `idx_show_seats_expired_hold` (`status`,`hold_expires_at`),
  CONSTRAINT `fk_show_seats_seat` FOREIGN KEY (`seat_id`) REFERENCES `seats` (`id`),
  CONSTRAINT `fk_show_seats_showtime` FOREIGN KEY (`showtime_id`) REFERENCES `showtimes` (`id`),
  CONSTRAINT `chk_show_seats_price` CHECK ((`price` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `showtime_batch_requests` (
  `client_request_id` binary(16) NOT NULL,
  `request_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `response_json` json DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`client_request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `showtimes` (
  `id` binary(16) NOT NULL,
  `movie_id` binary(16) NOT NULL,
  `room_id` binary(16) NOT NULL,
  `starts_at` datetime(6) NOT NULL,
  `ends_at` datetime(6) NOT NULL,
  `status` varchar(30) NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `room_layout_id` binary(16) DEFAULT NULL,
  `movie_screening_version_id` binary(16) DEFAULT NULL,
  `movie_screening_version_revision` bigint DEFAULT NULL,
  `content_format` varchar(20) DEFAULT NULL,
  `audio_language` varchar(100) DEFAULT NULL,
  `presentation_type` varchar(30) DEFAULT NULL,
  `subtitle_language` varchar(100) DEFAULT NULL,
  `schedule_movie_revision` bigint DEFAULT NULL,
  `movie_duration_minutes` int DEFAULT NULL,
  `break_minutes` int DEFAULT NULL,
  `ready_at` datetime(6) DEFAULT NULL,
  `pricing_policy_version_id` binary(16) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_showtimes_movie_start` (`movie_id`,`starts_at`),
  KEY `idx_showtimes_room_status_start` (`room_id`,`status`,`starts_at`),
  KEY `idx_showtimes_room_layout` (`room_layout_id`),
  KEY `idx_showtimes_screening_version_start` (`movie_screening_version_id`,`starts_at`),
  KEY `idx_showtimes_room_start` (`room_id`,`starts_at`),
  KEY `idx_showtimes_pricing_policy_version` (`pricing_policy_version_id`),
  CONSTRAINT `fk_showtimes_pricing_policy_version` FOREIGN KEY (`pricing_policy_version_id`) REFERENCES `cinema_pricing_policy_versions` (`id`),
  CONSTRAINT `fk_showtimes_room` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `fk_showtimes_room_layout` FOREIGN KEY (`room_layout_id`) REFERENCES `room_layouts` (`id`),
  CONSTRAINT `chk_showtimes_scheduling_details` CHECK ((((`schedule_movie_revision` is null) and (`movie_duration_minutes` is null) and (`break_minutes` is null) and (`ready_at` is null)) or ((`schedule_movie_revision` is not null) and (`schedule_movie_revision` >= 0) and (`movie_duration_minutes` is not null) and (`movie_duration_minutes` > 0) and (`break_minutes` is not null) and (`break_minutes` between 0 and 120) and (`ready_at` is not null) and (`ready_at` >= `ends_at`)))),
  CONSTRAINT `chk_showtimes_screening_snapshot` CHECK ((((`movie_screening_version_id` is null) and (`movie_screening_version_revision` is null) and (`content_format` is null) and (`audio_language` is null) and (`presentation_type` is null) and (`subtitle_language` is null)) or ((`movie_screening_version_id` is not null) and (`movie_screening_version_revision` is not null) and (`movie_screening_version_revision` >= 0) and (`content_format` is not null) and (`content_format` in (_utf8mb4'TWO_D',_utf8mb4'THREE_D')) and (`audio_language` is not null) and (char_length(trim(`audio_language`)) > 0) and (`presentation_type` is not null) and (((`presentation_type` = _utf8mb4'SUBTITLED') and (`subtitle_language` is not null) and (char_length(trim(`subtitle_language`)) > 0)) or ((`presentation_type` in (_utf8mb4'DUBBED',_utf8mb4'NO_SUBTITLES')) and (`subtitle_language` is null)))))),
  CONSTRAINT `chk_showtimes_time` CHECK ((`ends_at` > `starts_at`))
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
