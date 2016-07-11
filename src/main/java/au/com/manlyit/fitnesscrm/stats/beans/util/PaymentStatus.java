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
public enum PaymentStatus {//Read more: http://javarevisited.blogspot.com/2011/08/enum-in-java-example-tutorial.html#ixzz3Jl4MQSbZ
        SCHEDULED("N"), SENT_TO_GATEWAY("U"), SUCESSFUL("S"), PENDING("P"),DISHONOURED("D"),FATAL_DISHONOUR("F"),WAITING("W"),MISSING_IN_PGW("X"),DELETE_REQUESTED("Y"),REJECTED_BY_GATEWAY("R");
        private final String value;

        private PaymentStatus(String value) {
                this.value = value;
        }
        public String value(){
            return value;
        }
}; 