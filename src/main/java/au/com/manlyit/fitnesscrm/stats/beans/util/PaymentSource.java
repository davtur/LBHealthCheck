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

    
public enum PaymentSource {//Read more: http://javarevisited.blogspot.com/2011/08/enum-in-java-example-tutorial.html#ixzz3Jl4MQSbZ
        CASH("CASH"), CREDIT_CARD("CARD"), BANK_ACCOUNT("BANK"), DIRECT_DEBIT("DDEB"),DIRECT_TRANSFER("DTFR"),OTHER("OTHR");
        private final String value;

        private PaymentSource(String value) {
                this.value = value;
        }
        public String value(){
            return value;
        }
}; 