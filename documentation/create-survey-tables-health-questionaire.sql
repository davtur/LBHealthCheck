-- MySQL dump 10.13  Distrib 5.6.24, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: fitnessStats
-- ------------------------------------------------------
-- Server version	5.6.24-0ubuntu2

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `survey_answer_subitems`
--

DROP TABLE IF EXISTS `survey_answer_subitems`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `survey_answer_subitems` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `answer_id` int(11) NOT NULL,
  `subitem_text` text NOT NULL,
  `subitem_bool` tinyint(1) DEFAULT '0',
  `subitem_int` int(2) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `fk_survey_answer_subitems_1_idx` (`answer_id`),
  CONSTRAINT `fk_survey_answer_subitems_1` FOREIGN KEY (`answer_id`) REFERENCES `survey_answers` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=404 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `survey_answer_subitems`
--

LOCK TABLES `survey_answer_subitems` WRITE;
/*!40000 ALTER TABLE `survey_answer_subitems` DISABLE KEYS */;
/*!40000 ALTER TABLE `survey_answer_subitems` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `survey_answers`
--

DROP TABLE IF EXISTS `survey_answers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `survey_answers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `question_id` int(11) NOT NULL,
  `answer` text NOT NULL,
  `answerType_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_survey_answers_2_idx` (`question_id`),
  KEY `fk_survey_answers_3_idx` (`user_id`),
  KEY `fk_survey_answers_4_idx` (`answerType_id`),
  CONSTRAINT `fk_survey_answers_2` FOREIGN KEY (`question_id`) REFERENCES `survey_questions` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_survey_answers_3` FOREIGN KEY (`user_id`) REFERENCES `customers` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_survey_answers_4` FOREIGN KEY (`answerType_id`) REFERENCES `survey_question_types` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=131 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `survey_answers`
--

LOCK TABLES `survey_answers` WRITE;
/*!40000 ALTER TABLE `survey_answers` DISABLE KEYS */;
/*!40000 ALTER TABLE `survey_answers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `survey_question_subitems`
--

DROP TABLE IF EXISTS `survey_question_subitems`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `survey_question_subitems` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `question_id` int(11) NOT NULL,
  `subitem_text` text NOT NULL,
  `subitem_bool` tinyint(1) DEFAULT '0',
  `subitem_int` int(2) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `fk_survey_question_subitems_1_idx` (`question_id`),
  CONSTRAINT `fk_survey_question_subitems_1` FOREIGN KEY (`question_id`) REFERENCES `survey_questions` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `survey_question_subitems`
--

LOCK TABLES `survey_question_subitems` WRITE;
/*!40000 ALTER TABLE `survey_question_subitems` DISABLE KEYS */;
INSERT INTO `survey_question_subitems` VALUES (11,8,'I need more energy',NULL,NULL),(12,8,'I need a healthy eating plan',NULL,NULL),(13,8,'I need to improve my self confidence',NULL,NULL),(14,8,'I need to get fitter',NULL,NULL),(15,8,'I need to build muscle',NULL,NULL),(16,8,'I need to lose weight',NULL,NULL),(17,8,'I need more muscle tone',NULL,NULL),(18,8,'I have a specific sporting goal',NULL,NULL),(19,14,'Chest Discomfort with exertion',NULL,NULL),(20,14,'Heart Failure',NULL,NULL),(21,14,'Congenial Heart Disease',NULL,NULL),(22,14,'Pacemaker',NULL,NULL),(23,14,'Heart Transplant',NULL,NULL),(24,14,'Heart Surgery',NULL,NULL),(25,14,'Unreasonable breathlessness',NULL,NULL),(26,14,'Heart Valve disease',NULL,NULL),(27,14,'Heart Attack',NULL,NULL),(28,15,'Take heart medications',NULL,NULL),(29,15,'Are pregnant',NULL,NULL),(30,15,'Do you take prescription medicine',NULL,NULL),(31,15,'Trying to conceive',NULL,NULL),(32,16,'Are postmenopausal',NULL,NULL),(33,16,'Have family history of heart attack',NULL,NULL),(34,16,'Have epilepsy',NULL,NULL),(35,16,'Are physically inactive',NULL,NULL),(36,16,'Have cholesterol>240mg/dl',NULL,NULL),(37,16,'Have asthma',NULL,NULL),(38,16,'Are male, over 45 years',NULL,NULL),(39,16,'Are diabetic',NULL,NULL),(40,16,'Have BP>140/90 mmHg',NULL,NULL),(41,16,'Are a smoker',NULL,NULL);
/*!40000 ALTER TABLE `survey_question_subitems` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `survey_question_types`
--

DROP TABLE IF EXISTS `survey_question_types`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `survey_question_types` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `type` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `survey_question_types`
--

LOCK TABLES `survey_question_types` WRITE;
/*!40000 ALTER TABLE `survey_question_types` DISABLE KEYS */;
INSERT INTO `survey_question_types` VALUES (1,'Text'),(2,'Yes/No'),(3,'Multiple Choice');
/*!40000 ALTER TABLE `survey_question_types` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `survey_questions`
--

DROP TABLE IF EXISTS `survey_questions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `survey_questions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `question` text NOT NULL,
  `question_type` int(11) NOT NULL DEFAULT '0',
  `survey_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_survey_questions_1_idx` (`question_type`),
  KEY `fk_survey_questions_2_idx` (`survey_id`),
  CONSTRAINT `fk_survey_questions_1` FOREIGN KEY (`question_type`) REFERENCES `survey_question_types` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_survey_questions_2` FOREIGN KEY (`survey_id`) REFERENCES `surveys` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `survey_questions`
--

LOCK TABLES `survey_questions` WRITE;
/*!40000 ALTER TABLE `survey_questions` DISABLE KEYS */;
INSERT INTO `survey_questions` VALUES (8,'Tell us what you need to do to increase your quality of life?',2,1),(9,'How did you hear about Pure Fitness Manly? ',1,1),(10,'Why is it important to you to make these changes?',1,1),(11,'When would you like to have these changes made by?',1,1),(12,'Will you achieve your goals if you keep up with your current eating and exercise habits?',1,1),(13,'Do you have any medical problems that may prevent you from exercising? If yes please state',1,1),(14,'History – have you ever had:',2,1),(15,'Other health problems:',2,1),(16,'Assess your cardiovascular risk ',2,1),(17,'Name 3 goals and when you would like to achieve them',1,1);
/*!40000 ALTER TABLE `survey_questions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `surveys`
--

DROP TABLE IF EXISTS `surveys`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `surveys` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` text NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name_UNIQUE` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `surveys`
--

LOCK TABLES `surveys` WRITE;
/*!40000 ALTER TABLE `surveys` DISABLE KEYS */;
INSERT INTO `surveys` VALUES (1,'Health Questionaire','The standard purefitness health questionaire');
/*!40000 ALTER TABLE `surveys` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2015-07-21  0:46:17
