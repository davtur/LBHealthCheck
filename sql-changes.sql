



ALTER TABLE `fitnessStats`.`notes` ADD COLUMN `created_by` INT(11) NOT NULL DEFAULT 3 AFTER `deleted`;
ALTER TABLE `fitnessStats`.`notes` 
ADD INDEX `fk_notes_1_idx` (`created_by` ASC);
ALTER TABLE `fitnessStats`.`notes` 
ADD CONSTRAINT `fk_notes_1`
  FOREIGN KEY (`created_by`)
  REFERENCES `fitnessStats`.`customers` (`id`)
  ON DELETE RESTRICT
  ON UPDATE RESTRICT;



