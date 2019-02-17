/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import java.io.Serializable;

/**
 *
 * @author david
 */
public class TicketSummary implements Serializable{

    private static final long serialVersionUID = 1L;
    private  final String ticketName;
    private int numberOfTickets = 0;
    public TicketSummary(String ticketName,int numberOfTickets){
        this.numberOfTickets = numberOfTickets;
        this.ticketName = ticketName;
    }

    /**
     * @return the ticketName
     */
    public String getTicketName() {
        return ticketName;
    }

    public void incrementCount(){
        numberOfTickets += 1;
    }

    public String getNumberOfTicketsAsString() {
        return Integer.toString(numberOfTickets);
    }
    /**
     * @return the numberOfTickets
     */
    public int getNumberOfTickets() {
        return numberOfTickets;
    }

    /**
     * @param numberOfTickets the numberOfTickets to set
     */
    public void setNumberOfTickets(int numberOfTickets) {
        this.numberOfTickets = numberOfTickets;
    }
  
    
}
