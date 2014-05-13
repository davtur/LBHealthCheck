/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.classes.util;

import java.util.Date;

/**
 *
 * @author david
 */
public class ScheduledPaymentPojo {
    private int id;
    private String ezidebitCustomerID;
    private boolean manuallyAddedPayment;
    private double paymentAmount;
    private Date paymentDate;
    private String paymentReference;
    private String yourGeneralReference;
    private String yourSystemReference;

    public ScheduledPaymentPojo(int id,String ezidebitCustomerID, boolean manuallyAddedPayment, double paymentAmount, Date paymentDate, String paymentReference, String yourGeneralReference, String yourSystemReference) {
        this.ezidebitCustomerID = ezidebitCustomerID;
        this.manuallyAddedPayment = manuallyAddedPayment;
        this.paymentAmount = paymentAmount;
        this.paymentDate = paymentDate;
        this.paymentReference = paymentReference;
        this.yourGeneralReference = yourGeneralReference;
        this.yourSystemReference = yourSystemReference;
        this.id = id;
    }

    /**
     * @return the ezidebitCustomerID
     */
    public String getEzidebitCustomerID() {
        return ezidebitCustomerID;
    }

    /**
     * @param ezidebitCustomerID the ezidebitCustomerID to set
     */
    public void setEzidebitCustomerID(String ezidebitCustomerID) {
        this.ezidebitCustomerID = ezidebitCustomerID;
    }

    /**
     * @return the manuallyAddedPayment
     */
    public boolean isManuallyAddedPayment() {
        return manuallyAddedPayment;
    }

    /**
     * @param manuallyAddedPayment the manuallyAddedPayment to set
     */
    public void setManuallyAddedPayment(boolean manuallyAddedPayment) {
        this.manuallyAddedPayment = manuallyAddedPayment;
    }

    /**
     * @return the paymentAmount
     */
    public double getPaymentAmount() {
        return paymentAmount;
    }

    /**
     * @param paymentAmount the paymentAmount to set
     */
    public void setPaymentAmount(double paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    /**
     * @return the paymentDate
     */
    public Date getPaymentDate() {
        return paymentDate;
    }

    /**
     * @param paymentDate the paymentDate to set
     */
    public void setPaymentDate(Date paymentDate) {
        this.paymentDate = paymentDate;
    }

    /**
     * @return the paymentReference
     */
    public String getPaymentReference() {
        return paymentReference;
    }

    /**
     * @param paymentReference the paymentReference to set
     */
    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    /**
     * @return the yourGeneralReference
     */
    public String getYourGeneralReference() {
        return yourGeneralReference;
    }

    /**
     * @param yourGeneralReference the yourGeneralReference to set
     */
    public void setYourGeneralReference(String yourGeneralReference) {
        this.yourGeneralReference = yourGeneralReference;
    }

    /**
     * @return the yourSystemReference
     */
    public String getYourSystemReference() {
        return yourSystemReference;
    }

    /**
     * @param yourSystemReference the yourSystemReference to set
     */
    public void setYourSystemReference(String yourSystemReference) {
        this.yourSystemReference = yourSystemReference;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }
    
    
    
}
