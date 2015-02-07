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
public enum PaymentPeriod {//Read more: http://javarevisited.blogspot.com/2011/08/enum-in-java-example-tutorial.html#ixzz3Jl4MQSbZ
        MONTHLY("M"), WEEKLY("w"), FORTNIGHTLY("F"), FOUR_WEEKLY("4"),QUARTERLY("Q"),ANNUALLY("Y"),SIX_MONTHLY("H"),WEEKDAY_IN_MONTH("N");
        private final String value;

        private PaymentPeriod(String value) {
                this.value = value;
        }
        public String value(){
            return value;
        }
};