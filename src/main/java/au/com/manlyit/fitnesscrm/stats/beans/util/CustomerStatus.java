/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans.util;

/**
 *
 * @author david
 */
public enum CustomerStatus {
        ACTIVE("ACTIVE"), CANCELLED("CANCELLED"), ON_HOLD("ON HOLD"), LEAD("LEAD");
        private final String value;

        private CustomerStatus(String value) {
                this.value = value;
        }
        public String value(){
            return value;
        }
}; 