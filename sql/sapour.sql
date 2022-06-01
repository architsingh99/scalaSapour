-- -------------------------------------------------------------
-- TablePlus 4.6.6(422)
--
-- https://tableplus.com/
--
-- Database: sapour
-- Generation Time: 2022-06-01 23:39:23.4090
-- -------------------------------------------------------------


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

CREATE Database sapour;
DROP TABLE IF EXISTS `question_categories`;
CREATE TABLE `question_categories` (
  `id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL,
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1 - Active, 0 - Inactive',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `question_options`;
CREATE TABLE `question_options` (
  `id` int NOT NULL AUTO_INCREMENT,
  `question_id` int NOT NULL,
  `option` varchar(255) NOT NULL,
  `is_correct_option` tinyint NOT NULL DEFAULT '0' COMMENT '1 - Correct Option, 0 - Incorrect Option',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1 - Active, 0 - Inactive',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `FK_question_id` (`question_id`),
  CONSTRAINT `FK_question_id` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `questions`;
CREATE TABLE `questions` (
  `id` int NOT NULL AUTO_INCREMENT,
  `category_id` int NOT NULL,
  `question` text NOT NULL,
  `image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1 - Active, 0 - Inactive',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `FK_category_id` (`category_id`),
  CONSTRAINT `FK_category_id` FOREIGN KEY (`category_id`) REFERENCES `question_categories` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `user_auth_data`;
CREATE TABLE `user_auth_data` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `encrypted_password` varchar(50) NOT NULL,
  `salt` varchar(50) NOT NULL,
  `remember_token` varchar(100) NOT NULL,
  `remember_token_expires_at` datetime NOT NULL,
  `secure_code` varchar(100) NOT NULL,
  `secure_code_expires_at` datetime NOT NULL,
  `app_remember_token` varchar(100) NOT NULL,
  `app_remember_token_expires_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `remember_token` (`remember_token`),
  KEY `app_remember_token` (`app_remember_token`),
  KEY `FK_users_id` (`user_id`),
  CONSTRAINT `FK_users_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `user_login_history`;
CREATE TABLE `user_login_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `logged_on` datetime NOT NULL,
  `device` varchar(15) NOT NULL DEFAULT 'web',
  PRIMARY KEY (`id`),
  KEY `logged_user` (`logged_on`),
  KEY `FK_logged_user_id1` (`user_id`),
  CONSTRAINT `FK_logged_user_id1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `user_question_answers`;
CREATE TABLE `user_question_answers` (
  `id` int NOT NULL AUTO_INCREMENT,
  `question_id` int NOT NULL,
  `user_id` int NOT NULL,
  `selected_option` int NOT NULL,
  `is_correct` tinyint NOT NULL DEFAULT '0' COMMENT '1 - Correct, 0 - Incorrect',
  `paper_id` varchar(255) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `is_attempted` tinyint NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `FK_question_id` (`question_id`),
  KEY `FK_paper_question_id` (`question_id`),
  KEY `FK_paper_uer_id` (`user_id`),
  KEY `FK_paper_user_id` (`question_id`),
  KEY `FK_paper_option_id` (`selected_option`),
  CONSTRAINT `FK_paper_option_id` FOREIGN KEY (`selected_option`) REFERENCES `question_options` (`id`),
  CONSTRAINT `FK_paper_question_id` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`),
  CONSTRAINT `FK_paper_uer_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `mobile` varchar(100) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '1',
  `user_type` tinyint NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_login_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_email` (`email`,`user_type`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `question_categories` (`id`, `title`, `status`, `created_at`, `updated_at`) VALUES
(1, 'java', 1, '2022-05-25 18:15:04', '2022-05-25 18:15:04'),
(2, 'angular', 1, '2022-06-01 18:12:23', '2022-06-01 18:12:23');

INSERT INTO `question_options` (`id`, `question_id`, `option`, `is_correct_option`, `status`, `created_at`, `updated_at`) VALUES
(1, 1, 'Bytecode is executed by JVM', 1, 1, '2022-05-25 18:17:40', '2022-05-25 18:17:40'),
(2, 1, 'The applet makes the Java code secure and portable', 0, 1, '2022-05-25 18:18:02', '2022-05-25 18:18:02'),
(3, 1, 'Use of exception handling', 0, 1, '2022-05-25 18:18:10', '2022-05-25 18:18:10'),
(4, 1, 'Dynamic binding between objects', 0, 1, '2022-05-25 18:18:17', '2022-05-25 18:18:17'),
(5, 2, 'Dynamic', 0, 1, '2022-05-25 18:23:44', '2022-05-25 18:23:44'),
(6, 2, 'Architecture Neutral', 0, 1, '2022-05-25 18:23:54', '2022-05-25 18:23:54'),
(7, 2, 'Use of pointers', 1, 1, '2022-05-25 18:24:02', '2022-05-25 18:24:02'),
(8, 2, 'Object-oriented', 0, 1, '2022-05-25 18:24:15', '2022-05-25 18:24:15'),
(9, 3, 'Unicode escape sequence', 1, 1, '2022-05-25 18:24:37', '2022-05-25 18:24:37'),
(10, 3, 'Octal escape', 0, 1, '2022-05-25 18:24:45', '2022-05-25 18:24:45'),
(11, 3, 'Hexadecimal', 0, 1, '2022-05-25 18:24:52', '2022-05-25 18:24:52'),
(12, 3, 'Line feed', 0, 1, '2022-05-25 18:25:01', '2022-05-25 18:25:01');

INSERT INTO `questions` (`id`, `category_id`, `question`, `image`, `status`, `created_at`, `updated_at`) VALUES
(1, 1, 'Which of the following option leads to the portability and security of Java?', NULL, 1, '2022-05-25 18:16:15', '2022-05-25 18:16:15'),
(2, 1, 'Which of the following is not a Java features?', NULL, 1, '2022-05-25 18:16:43', '2022-05-25 18:16:43'),
(3, 1, 'The \\u0021 article referred to as a', NULL, 1, '2022-05-25 18:17:09', '2022-05-25 18:17:09');

INSERT INTO `user_auth_data` (`id`, `user_id`, `encrypted_password`, `salt`, `remember_token`, `remember_token_expires_at`, `secure_code`, `secure_code_expires_at`, `app_remember_token`, `app_remember_token_expires_at`) VALUES
(1, 1, '213ef12142c75d8a848aa7bcf4f492e64c39364a', 'e6ac439266ca14b67a63813422db5b1d', '00d7d8bf-b509-4f29-b1f7-b4cec3af4de5', '2023-06-01 19:27:38', 'HgprqvoE5TPLahx3tgt', '2025-05-25 15:16:00', 'dcff9dd6-cd6b-493f-ab8c-3cc28ee1183a', '2023-06-01 17:55:42'),
(2, 2, '2e2954da6008c99912e28f3e4004d99632f6d968', 'da0b2f3d40ba554aab96022624c91425', 'bc9fdbb4-8f1f-4b07-9769-60d22b0fb536', '2023-06-01 18:27:39', 'mzgOdhCHgtmLATxT2ax', '2025-06-01 18:27:39', 'bc9fdbb4-8f1f-4b07-9769-60d22b0fb536', '2023-06-01 18:27:39');

INSERT INTO `user_login_history` (`id`, `user_id`, `logged_on`, `device`) VALUES
(1, 1, '2022-05-25 18:32:56', 'web'),
(2, 1, '2022-06-01 14:16:08', 'web'),
(3, 1, '2022-06-01 14:40:11', 'web'),
(4, 1, '2022-06-01 17:44:28', 'web'),
(5, 1, '2022-06-01 17:45:48', 'web'),
(6, 1, '2022-06-01 17:55:42', 'web');

INSERT INTO `user_question_answers` (`id`, `question_id`, `user_id`, `selected_option`, `is_correct`, `paper_id`, `created_at`, `updated_at`, `is_attempted`) VALUES
(1, 3, 1, 10, 0, '1_1653485055390', '2022-05-25 18:54:15', '2022-05-25 18:54:15', 1),
(2, 1, 1, 1, 1, '1_1653485055390', '2022-05-25 18:54:15', '2022-05-25 18:54:15', 1),
(3, 2, 1, 7, 1, '1_1653485055390', '2022-05-25 18:54:15', '2022-05-25 18:54:15', 1),
(4, 1, 1, 1, 1, '1_1654086535927', '2022-06-01 17:58:56', '2022-06-01 17:58:56', 1),
(5, 3, 1, 9, 1, '1_1654086623884', '2022-06-01 18:00:23', '2022-06-01 18:00:23', 1),
(6, 1, 1, 1, 1, '1_1654086623884', '2022-06-01 18:00:23', '2022-06-01 18:00:23', 1),
(7, 2, 1, 5, 0, '1_1654086623884', '2022-06-01 18:00:23', '2022-06-01 18:00:23', 1),
(8, 2, 1, 5, 0, '1_1654086903278', '2022-06-01 18:05:03', '2022-06-01 18:05:03', 1),
(9, 1, 1, 1, 1, '1_1654086903278', '2022-06-01 18:05:03', '2022-06-01 18:05:03', 1),
(10, 3, 1, 9, 1, '1_1654086903278', '2022-06-01 18:05:03', '2022-06-01 18:05:03', 1),
(11, 1, 1, 1, 1, '1_1654087267947', '2022-06-01 18:11:07', '2022-06-01 18:11:07', 1),
(12, 2, 1, 5, 0, '1_1654087267947', '2022-06-01 18:11:07', '2022-06-01 18:11:07', 1),
(13, 3, 1, 9, 1, '1_1654087267947', '2022-06-01 18:11:08', '2022-06-01 18:11:08', 1),
(14, 2, 1, 5, 0, '1_1654087579456', '2022-06-01 18:16:19', '2022-06-01 18:16:19', 1),
(15, 3, 1, 9, 1, '1_1654087579456', '2022-06-01 18:16:19', '2022-06-01 18:16:19', 1),
(16, 1, 1, 1, 1, '1_1654087579456', '2022-06-01 18:16:19', '2022-06-01 18:16:19', 1),
(17, 2, 1, 5, 0, '1_1654089641035', '2022-06-01 18:50:41', '2022-06-01 18:50:41', 1),
(18, 3, 1, 9, 1, '1_1654089641035', '2022-06-01 18:50:41', '2022-06-01 18:50:41', 1),
(19, 1, 1, 1, 1, '1_1654089641035', '2022-06-01 18:50:41', '2022-06-01 18:50:41', 1);

INSERT INTO `users` (`id`, `name`, `mobile`, `email`, `status`, `user_type`, `created_at`, `updated_at`, `last_login_at`) VALUES
(1, 'Archit', '7002088304', 'architsingh99@gmail.com', 1, 1, '2022-05-25 15:16:00', '2022-05-25 15:16:00', '2022-05-25 15:16:00'),
(2, 'Archit', NULL, 'arch@gmail.com', 1, 1, '2022-06-01 18:27:39', '2022-06-01 18:27:39', '2022-06-01 18:27:39');



/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;