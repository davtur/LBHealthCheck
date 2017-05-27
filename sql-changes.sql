



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


ALTER TABLE `fitnessStats`.`customers` 
ADD COLUMN `emergency_contact_name` VARCHAR(255) NOT NULL AFTER `terms_conditions_accepted`,
ADD COLUMN `emergency_contact_phone` VARCHAR(45) NOT NULL AFTER `emergency_contact_name`;
UPDATE fitnessStats.customers set emergency_contact_name = "Not Provided", emergency_contact_phone ="Not Provided" where emergency_contact_name ="";


ALTER TABLE `fitnessStats`.`session_timetable` 
ADD COLUMN `session_casual_rate` DECIMAL(22,2) NULL DEFAULT '25.00' AFTER `show_signup_button`,
ADD COLUMN `session_members_rate` DECIMAL(22,2) NULL DEFAULT '20.00' AFTER `session_casual_rate`;

CREATE TABLE `fitnessStats`.`session_bookings` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `customer_id` INT(11) NOT NULL,
  `session_history_id` INT(11) NOT NULL,
  `payment_id` INT(11) NULL,
  `booking_time` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `payment_id_UNIQUE` (`payment_id` ASC));

ALTER TABLE `fitnessStats`.`session_bookings` 
ADD INDEX `fk_session_bookings_1_idx` (`customer_id` ASC),
ADD INDEX `fk_session_bookings_2_idx` (`session_history_id` ASC);
ALTER TABLE `fitnessStats`.`session_bookings` 
ADD CONSTRAINT `fk_session_bookings_1`
  FOREIGN KEY (`customer_id`)
  REFERENCES `fitnessStats`.`customers` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION,
ADD CONSTRAINT `fk_session_bookings_2`
  FOREIGN KEY (`session_history_id`)
  REFERENCES `fitnessStats`.`session_history` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION,
ADD CONSTRAINT `fk_session_bookings_3`
  FOREIGN KEY (`payment_id`)
  REFERENCES `fitnessStats`.`payments` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;

ALTER TABLE `fitnessStats`.`session_bookings` 
ADD COLUMN `status` VARCHAR(255) NULL AFTER `booking_time`;
ALTER TABLE `fitnessStats`.`session_bookings` 
CHANGE COLUMN `status` `status` VARCHAR(16) NOT NULL DEFAULT 'NEW' ,
ADD COLUMN `status_description` VARCHAR(255) NULL AFTER `status`;


ALTER TABLE `fitnessStats`.`session_timetable` 
ADD COLUMN `session_style_classes` TEXT NULL COMMENT '' AFTER `session_timetable_status`;


ALTER TABLE `fitnessStats`.`email_templates` 
ADD COLUMN `subject` TEXT NOT NULL AFTER `template`,
ADD COLUMN `type` INT(11) NOT NULL DEFAULT 0 AFTER `subject`,
ADD COLUMN `deleted` BIT(1) NULL AFTER `type`,
ADD COLUMN `deletedDate` DATETIME NULL AFTER `deleted`;


ALTER TABLE `fitnessStats`.`customers` 
CHANGE COLUMN `street_address` `street_address` VARCHAR(127) NULL DEFAULT '' ,
CHANGE COLUMN `suburb` `suburb` VARCHAR(127) NULL DEFAULT '' ,
CHANGE COLUMN `postcode` `postcode` VARCHAR(10) NULL DEFAULT '' ,
CHANGE COLUMN `city` `city` VARCHAR(127) NULL DEFAULT '' ,
CHANGE COLUMN `addr_state` `addr_state` VARCHAR(32) NULL DEFAULT '' ;



CREATE TABLE `expense_types` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `expense_type_name` varchar(127) DEFAULT NULL, 
  `description` text,

  PRIMARY KEY (`id`)
  
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=latin1;


CREATE TABLE `payment_methods` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `ayment_method_name` varchar(127) DEFAULT NULL, 
  `description` text,

  PRIMARY KEY (`id`)
  
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=latin1;

CREATE TABLE `suppliers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `supplier_name` varchar(127) DEFAULT NULL, 
  `description` text,
  `supplier_company_number` varchar(127) DEFAULT NULL, 
  `supplier_company_number_type` varchar(127) DEFAULT NULL, 

  PRIMARY KEY (`id`)
  
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=latin1;

CREATE TABLE `invoice_images` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `created_timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expense_id` int(11) NOT NULL,
  `image_type` int(4) NOT NULL DEFAULT '0',
  
  `image` longblob NOT NULL,
  `mimeType` varchar(127) NOT NULL,
  `image_file_name` varchar(127) NOT NULL,
  `image_description` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=latin1;


CREATE TABLE `expenses` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `created_timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `expense_incurred_timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `expense_type_id` int(11) NOT NULL,
  `invoice_image_id` int(11) DEFAULT NULL,
  `invoice_number` varchar(70) DEFAULT NULL,
  `supplier_id` int(11) NOT NULL,
  `payment_method_id` int(11) NOT NULL,
  `receipt_number` varchar(70) DEFAULT NULL,
  `expense_amount` decimal(22,10) NOT NULL,
  `business_use_amount` decimal(22,10) NOT NULL,
  `description` text,
  `percent_for_business_use` float DEFAULT '100',
  `expense_amount_gst` decimal(22,10) NOT NULL,
  `business_use_amount_gst` decimal(22,10) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_expenses_1_idx` (`payment_method_id`),
  KEY `fk_expenses_2_idx` (`expense_type_id`),
  KEY `fk_expenses_3_idx` (`invoice_image_id`),
  KEY `fk_expenses_4_idx` (`supplier_id`),
  CONSTRAINT `fk_expenses_1` FOREIGN KEY (`payment_method_id`) REFERENCES `payment_methods` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_expenses_2` FOREIGN KEY (`expense_type_id`) REFERENCES `expense_types` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_expenses_3` FOREIGN KEY (`invoice_image_id`) REFERENCES `invoice_images` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_expenses_4` FOREIGN KEY (`supplier_id`) REFERENCES `suppliers` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=latin1;


INSERT INTO `fitnessStats`.`payment_methods`
(`id`,
`payment_method_name`,
`description`)
VALUES
(1,
'Cash',
'Cash was used to pay for the expense'),
(2,
'Credit Card',
'A Credit Card was used to pay for the expense'),
(3,
'Direct Deposit',
'A Direct Deposit was used to pay for the expense'),
(4,
'Cheque',
'A Cheque was used to pay for the expense')
;

INSERT INTO `fitnessStats`.`expense_types`
(`id`,
`expense_type_name`,
`description`)
VALUES
(1,
'Fuel for Car',
'Fuel used in work vehicle'),
(2,
'Work Vehicle Loan Repayment',
'A loan or lease payment on a vehicle used by the business'),
(3,
'Office Supplies',
'Stationary and office supplies such as paper, printer cartridges etc.'),
(4,
'Home Offices Expense',
'Home office related expenses such as phones, electricty, rent,internet etc.')
;

ALTER TABLE `fitnessStats`.`expenses` 
CHANGE COLUMN `expense_amount` `expense_amount` DECIMAL(22,10) NOT NULL DEFAULT 0 ,
CHANGE COLUMN `business_use_amount` `business_use_amount` DECIMAL(22,10) NOT NULL DEFAULT 0 ,
CHANGE COLUMN `expense_amount_gst` `expense_amount_gst` DECIMAL(22,10) NOT NULL DEFAULT 0 ,
CHANGE COLUMN `business_use_amount_gst` `business_use_amount_gst` DECIMAL(22,10) NOT NULL DEFAULT 0 ;


ALTER TABLE `fitnessStats`.`expenses` 
DROP FOREIGN KEY `fk_expenses_3`;
ALTER TABLE `fitnessStats`.`expenses` 
ADD CONSTRAINT `fk_expenses_3`
  FOREIGN KEY (`invoice_image_id`)
  REFERENCES `fitnessStats`.`invoice_images` (`id`)
  ON DELETE CASCADE
  ON UPDATE CASCADE;


ALTER TABLE `fitnessStats`.`invoice_images` 
ADD CONSTRAINT `fk_invoice_images_1`
  FOREIGN KEY (`id`)
  REFERENCES `fitnessStats`.`expenses` (`invoice_image_id`)
  ON DELETE CASCADE
  ON UPDATE CASCADE;

DELETE FROM   `fitnessStats`.`invoice_images`;
ALTER TABLE `fitnessStats`.`expenses` 
DROP FOREIGN KEY `fk_expenses_3`;
ALTER TABLE `fitnessStats`.`expenses` 
DROP INDEX `fk_expenses_3_idx` ;

ALTER TABLE `fitnessStats`.`invoice_images` 
DROP FOREIGN KEY `fk_invoice_images_1`;
ALTER TABLE `fitnessStats`.`invoice_images` 
ADD CONSTRAINT `fk_invoice_images_1`
  FOREIGN KEY (`id`)
  REFERENCES `fitnessStats`.`expenses` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;

ALTER TABLE `fitnessStats`.`expenses` 
DROP COLUMN `invoice_image_id`,
DROP INDEX `fk_expenses_3_idx` ;

ALTER TABLE `fitnessStats`.`session_history` 
ADD COLUMN `expense_id` INT(11) NULL AFTER `session_template`,
ADD UNIQUE INDEX `expense_id_UNIQUE` (`expense_id` ASC);




ALTER TABLE `fitnessStats`.`suppliers` 
ADD COLUMN `internal_contractor_id` INT(11) NULL AFTER `supplier_company_number_type`;


ALTER TABLE `fitnessStats`.`suppliers` 
ADD INDEX `fk_suppliers_1_idx` (`internal_contractor_id` ASC);
ALTER TABLE `fitnessStats`.`suppliers` 
ADD CONSTRAINT `fk_suppliers_1`
  FOREIGN KEY (`internal_contractor_id`)
  REFERENCES `fitnessStats`.`customers` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;


ALTER TABLE `fitnessStats`.`session_history` 
DROP FOREIGN KEY `fk_session_history_3`;
ALTER TABLE `fitnessStats`.`session_history` 
ADD CONSTRAINT `fk_session_history_3`
  FOREIGN KEY (`expense_id`)
  REFERENCES `fitnessStats`.`expenses` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;

CREATE TABLE `contractor_rates` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `rate` decimal(22,10) NOT NULL DEFAULT '0.0000000000',
  `bonusAmount` decimal(22,10) NOT NULL DEFAULT '0.0000000000',
  `bonusInteger` int(11) NOT NULL,
  `name` varchar(127) DEFAULT NULL,
  `description` text,
  `supplier_id` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=latin1;

ALTER TABLE `fitnessStats`.`contractor_rates` 
ADD INDEX `fk_contractor_rates_1_idx` (`supplier_id` ASC);
ALTER TABLE `fitnessStats`.`contractor_rates` 
ADD CONSTRAINT `fk_contractor_rates_1`
  FOREIGN KEY (`supplier_id`)
  REFERENCES `fitnessStats`.`suppliers` (`id`)
  ON DELETE CASCADE
  ON UPDATE CASCADE;

# 6th Jan 2017



ALTER TABLE `fitnessStats`.`customers` 
ADD COLUMN `payment_parameters_id` INT(11) NULL AFTER `emergency_contact_phone`;

UPDATE  fitnessStats.customers set payment_parameters_id = fitnessStats.paymentParameters.id where customers.id = paymentParameters.loggedInUser;

ALTER TABLE `fitnessStats`.`paymentParameters` 
DROP FOREIGN KEY `fk_paymentParameters_1`;
ALTER TABLE `fitnessStats`.`paymentParameters` 
DROP INDEX `loggedInUser` ;
ALTER TABLE `fitnessStats`.`paymentParameters` 
DROP COLUMN `loggedInUser`,
DROP INDEX `fk_paymentParameters_1_idx` ;



ALTER TABLE `fitnessStats`.`customers` 
ADD INDEX `fk_customers_pay_params_idx` (`payment_parameters_id` ASC);
ALTER TABLE `fitnessStats`.`customers` 
ADD CONSTRAINT `fk_customers_pay_params`
  FOREIGN KEY (`payment_parameters_id`)
  REFERENCES `fitnessStats`.`paymentParameters` (`id`)
  ON DELETE CASCADE
  ON UPDATE CASCADE;

ALTER TABLE `fitnessStats`.`customers` 
ADD UNIQUE INDEX `payment_parameters_id_UNIQUE` (`payment_parameters_id` ASC);



ALTER TABLE `fitnessStats`.`paymentParameters` 
CHANGE COLUMN `contractStartDate` `contractStartDate` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ,
ADD COLUMN `lastUpdatedFromPaymentGateway` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `paymentsRegularTotalNumberOfPayments`,
ADD COLUMN `cancellationDate` DATETIME NULL AFTER `lastUpdatedFromPaymentGateway`;
