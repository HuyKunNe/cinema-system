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
CREATE TABLE `genres` (
  `id` binary(16) NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_genres_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `movie_genres` (
  `movie_id` binary(16) NOT NULL,
  `genre_id` binary(16) NOT NULL,
  PRIMARY KEY (`movie_id`,`genre_id`),
  KEY `idx_movie_genres_genre_id` (`genre_id`),
  CONSTRAINT `fk_movie_genres_genre` FOREIGN KEY (`genre_id`) REFERENCES `genres` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_movie_genres_movie` FOREIGN KEY (`movie_id`) REFERENCES `movies` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `movie_screening_versions` (
  `id` binary(16) NOT NULL,
  `movie_id` binary(16) NOT NULL,
  `content_format` varchar(20) NOT NULL,
  `audio_language` varchar(100) NOT NULL,
  `presentation_type` varchar(30) NOT NULL,
  `subtitle_language` varchar(100) NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_movie_screening_version_identity` (`movie_id`,`content_format`,`audio_language`,`presentation_type`,`subtitle_language`),
  CONSTRAINT `fk_movie_screening_versions_movie` FOREIGN KEY (`movie_id`) REFERENCES `movies` (`id`),
  CONSTRAINT `chk_movie_screening_version_format` CHECK ((`content_format` in (_utf8mb4'TWO_D',_utf8mb4'THREE_D'))),
  CONSTRAINT `chk_movie_screening_version_presentation` CHECK ((((`presentation_type` = _utf8mb4'SUBTITLED') and (`subtitle_language` <> _utf8mb4'')) or ((`presentation_type` in (_utf8mb4'DUBBED',_utf8mb4'NO_SUBTITLES')) and (`subtitle_language` = _utf8mb4''))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `movies` (
  `id` binary(16) NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` text,
  `duration_minutes` int NOT NULL,
  `release_date` date DEFAULT NULL,
  `poster_url` varchar(500) DEFAULT NULL,
  `trailer_url` varchar(500) DEFAULT NULL,
  `status` varchar(30) NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `backdrop_url` varchar(500) DEFAULT NULL,
  `age_rating` varchar(10) DEFAULT NULL,
  `hero_enabled` tinyint(1) NOT NULL DEFAULT '0',
  `hero_priority` int NOT NULL DEFAULT '0',
  `hero_starts_at` datetime(6) DEFAULT NULL,
  `hero_ends_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_movies_title` (`title`),
  KEY `idx_movies_status` (`status`),
  KEY `idx_movies_release_date` (`release_date`),
  KEY `idx_movies_hero_selection` (`hero_enabled`,`status`,`hero_priority`,`id`),
  CONSTRAINT `chk_movies_age_rating` CHECK (((`age_rating` is null) or (`age_rating` in (_utf8mb4'P',_utf8mb4'K',_utf8mb4'T13',_utf8mb4'T16',_utf8mb4'T18')))),
  CONSTRAINT `chk_movies_duration` CHECK ((`duration_minutes` > 0)),
  CONSTRAINT `chk_movies_hero_period` CHECK (((`hero_starts_at` is null) or (`hero_ends_at` is null) or (`hero_ends_at` > `hero_starts_at`))),
  CONSTRAINT `chk_movies_hero_priority` CHECK ((`hero_priority` >= 0))
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
