drop table Survey_answers;
CREATE TABLE `survey_answers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `survey_id` int(11) NOT NULL,
  `question_id` int(11) NOT NULL,
  `answer` text NOT NULL,
  `answerType_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_Survey_answers_1_idx` (`survey_id`),
  KEY `fk_Survey_answers_2_idx` (`question_id`),
  KEY `fk_Survey_answers_3_idx` (`user_id`),
  KEY `fk_Survey_answers_4_idx` (`answerType_id`),
  CONSTRAINT `fk_Survey_answers_1` FOREIGN KEY (`survey_id`) REFERENCES `surveys` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_Survey_answers_2` FOREIGN KEY (`question_id`) REFERENCES `survey_questions` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_Survey_answers_3` FOREIGN KEY (`user_id`) REFERENCES `customers` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_Survey_answers_4` FOREIGN KEY (`answerType_id`) REFERENCES `survey_question_types` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

drop table Survey_answer_subitems;
CREATE TABLE `survey_answer_subitems` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `answer_id` int(11) NOT NULL,
  `subitem_text` text NOT NULL,
  `subitem_bool` tinyint(1) DEFAULT '0',
  `subitem_int` int(2) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `fk_Survey_answer_subitems_1_idx` (`answer_id`),
  CONSTRAINT `fk_Survey_answer_subitems_1` FOREIGN KEY (`answer_id`) REFERENCES `survey_answers` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

drop table Survey_questions;
CREATE TABLE `survey_questions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `question` text NOT NULL,
  `question_type` int(11) NOT NULL DEFAULT '0',
  `survey_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_Survey_questions_1_idx` (`question_type`),
  KEY `fk_Survey_questions_2_idx` (`survey_id`),
  CONSTRAINT `fk_Survey_questions_1` FOREIGN KEY (`question_type`) REFERENCES `survey_question_types` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_Survey_questions_2` FOREIGN KEY (`survey_id`) REFERENCES `surveys` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=latin1;

drop table Survey_question_subitems;
CREATE TABLE `survey_question_subitems` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `question_id` int(11) NOT NULL,
  `subitem_text` text NOT NULL,
  `subitem_bool` tinyint(1) DEFAULT '0',
  `subitem_int` int(2) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `fk_Survey_question_subitems_1_idx` (`question_id`),
  CONSTRAINT `fk_Survey_question_subitems_1` FOREIGN KEY (`question_id`) REFERENCES `survey_questions` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=latin1;

drop table Survey_question_types;
CREATE TABLE `survey_question_types` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `type` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=latin1;

drop table Surveys;
CREATE TABLE `surveys` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` text NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name_UNIQUE` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1;
