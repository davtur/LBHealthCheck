



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
