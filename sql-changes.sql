



ALTER TABLE `fitnessStats`.`notes` ADD COLUMN `created_by` INT(11) NOT NULL DEFAULT 3 AFTER `deleted`;
ALTER TABLE `fitnessStats`.`notes` 
ADD INDEX `fk_notes_1_idx` (`created_by` ASC);
ALTER TABLE `fitnessStats`.`notes` 
ADD CONSTRAINT `fk_notes_1`
  FOREIGN KEY (`created_by`)
  REFERENCES `fitnessStats`.`customers` (`id`)
  ON DELETE RESTRICT
  ON UPDATE RESTRICT;



ALTER TABLE `fitnessStats`.`customer_images` 
CHANGE COLUMN `image` `image` LONGBLOB NOT NULL ,
ADD COLUMN `mimeType` VARCHAR(127) NOT NULL AFTER `image`,
ADD COLUMN `image_file_name` VARCHAR(127) NOT NULL AFTER `mimeType`,
ADD COLUMN `image_description` TEXT NULL AFTER `image_file_name`;



DROP TABLE IF EXISTS`session_recurrance` ;
CREATE TABLE `session_recurrance` (
  `id` int(4) NOT NULL AUTO_INCREMENT,
  `name` varchar(127) NOT NULL DEFAULT '',
  `description` text NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=latin1;

INSERT INTO `fitnessStats`.`session_recurrance`
(`id`,`name`,`description`)
VALUES
(0,"Weekly","This session occurs every week."),
(0,"Fortnightly","This session occurs every second week."),
(0,"3 Weekly","This session occurs every third week."),
(0,"4 Weekly","This session occurs every fourth week."),
(0,"Monthly","This session occurs every month.");


CREATE TABLE `session_timetable` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `sessiondate` datetime NOT NULL DEFAULT '1900-01-01 00:00:00',
  `session_types_id` int(11) NOT NULL DEFAULT '0',
  `comments` text,
  `admin_notes` text,
  `recurrance_id` int(11) NOT NULL DEFAULT '1',
  `trainer_id` int(11) NOT NULL DEFAULT '4',
  PRIMARY KEY (`id`),
  KEY `fk_session_timetable_1` (`session_types_id`),
  KEY `fk_session_timetable_2` (`recurrance_id`),
  KEY `fk_session_timetable_3_idx` (`trainer_id`),
  CONSTRAINT `fk_session_timetable_1` FOREIGN KEY (`session_types_id`) REFERENCES `session_types` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_session_timetable_2` FOREIGN KEY (`recurrance_id`) REFERENCES `session_recurrance` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_session_timetable_3` FOREIGN KEY (`trainer_id`) REFERENCES `customers` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=latin1;

ALTER TABLE `fitnessStats`.`session_types` 
ADD COLUMN `session_duration_minutes` INT(4) NOT NULL DEFAULT 60 AFTER `description`;

ALTER TABLE `fitnessStats`.`groups` 
DROP FOREIGN KEY `fk_groups_1`;
ALTER TABLE `fitnessStats`.`groups` 
ADD CONSTRAINT `fk_groups_1`
  FOREIGN KEY (`username`)
  REFERENCES `fitnessStats`.`customers` (`username`)
  ON DELETE NO ACTION
  ON UPDATE CASCADE;


ALTER TABLE `fitnessStats`.`participants` 
DROP FOREIGN KEY `fk_participants_2`;
ALTER TABLE `fitnessStats`.`participants` 
ADD CONSTRAINT `fk_participants_2`
  FOREIGN KEY (`session_history_id`)
  REFERENCES `fitnessStats`.`session_history` (`id`)
  ON DELETE NO ACTION
  ON UPDATE CASCADE;

ALTER TABLE `fitnessStats`.`paymentParameters` 
DROP FOREIGN KEY `fk_paymentParameters_1`;
ALTER TABLE `fitnessStats`.`paymentParameters` 
ADD CONSTRAINT `fk_paymentParameters_1`
  FOREIGN KEY (`loggedInUser`)
  REFERENCES `fitnessStats`.`customers` (`id`)
  ON DELETE NO ACTION
  ON UPDATE CASCADE;



ALTER TABLE `fitnessStats`.`session_timetable` 
ADD COLUMN `duration_minutes` INT(4) NOT NULL DEFAULT 60 AFTER `trainer_id`,
ADD COLUMN `session_title` TEXT NOT NULL AFTER `duration_minutes`,
ADD COLUMN `session_location_label` TEXT NOT NULL AFTER `session_title`,
ADD COLUMN `session_location_gps` VARCHAR(128) NOT NULL AFTER `session_location_label`;



ALTER TABLE `fitnessStats`.`session_history` 
ADD COLUMN `session_template` INT(11) NULL AFTER `admin_notes`;


ALTER TABLE `fitnessStats`.`session_history` 
ADD INDEX `fk_session_history_2_idx` (`session_template` ASC);
ALTER TABLE `fitnessStats`.`session_history` 
ADD CONSTRAINT `fk_session_history_2`
  FOREIGN KEY (`session_template`)
  REFERENCES `fitnessStats`.`session_timetable` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;

ALTER TABLE `fitnessStats`.`customers` 
ADD COLUMN `last_login_time` DATETIME NULL DEFAULT NULL AFTER `profile_image`,
ADD COLUMN `login_attempts` INT(4) NULL DEFAULT 0 AFTER `last_login_time`,
ADD COLUMN `must_reset_password` TINYINT(1) NULL DEFAULT 0 AFTER `login_attempts`;

ALTER TABLE `fitnessStats`.`session_timetable` 
ADD COLUMN `show_booking_button` TINYINT(1) NULL DEFAULT '0' AFTER `session_location_gps`,
ADD COLUMN `show_signup_button` TINYINT(1) NULL DEFAULT '0' AFTER `show_booking_button`;


ALTER TABLE `fitnessStats`.`customers` 
ADD COLUMN `terms_conditions_accepted` TINYINT(1) NULL DEFAULT '0' AFTER `must_reset_password`;


