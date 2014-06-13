/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author david
 */
public class PaymentPojo implements Serializable {

    public PaymentPojo() {

    }
    private String bankFailedReason;
    private String bankReceiptID;
    private String bankReturnCode;
    private String customerName;
    private Date debitDate;
    private String ezidebitCustomerID;
    private String invoiceID;
    private Double paymentAmount;
    private String paymentID;
    private String paymentMethod;
    private String paymentReference;
    private String paymentSource;
    private String paymentStatus;
    private Double scheduledAmount;
    private Date settlementDate;
    private Double transactionFeeClient;
    private Double transactionFeeCustomer;
    private Date transactionTime;
    private String yourGeneralReference;
    private String yourSystemReference;

    /**
     * @return the bankFailedReason
     */
    public String getBankFailedReason() {
        return bankFailedReason;
    }

    /**
     * @param bankFailedReason the bankFailedReason to set
     */
    public void setBankFailedReason(String bankFailedReason) {
        this.bankFailedReason = bankFailedReason;
    }

    /**
     * @return the bankReceiptID
     */
    public String getBankReceiptID() {
        return bankReceiptID;
    }

    /**
     * @param bankReceiptID the bankReceiptID to set
     */
    public void setBankReceiptID(String bankReceiptID) {
        this.bankReceiptID = bankReceiptID;
    }

    /**
     * @return the bankReturnCode
     */
    public String getBankReturnCode() {
        return bankReturnCode;
    }

    /**
     * @param bankReturnCode the bankReturnCode to set
     */
    public void setBankReturnCode(String bankReturnCode) {
        this.bankReturnCode = bankReturnCode;
    }

    /**
     * @return the customerName
     */
    public String getCustomerName() {
        return customerName;
    }

    /**
     * @param customerName the customerName to set
     */
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    /**
     * @return the debitDate
     */
    public Date getDebitDate() {
        return debitDate;
    }

    /**
     * @param debitDate the debitDate to set
     */
    public void setDebitDate(Date debitDate) {
        this.debitDate = debitDate;
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
     * @return the invoiceID
     */
    public String getInvoiceID() {
        return invoiceID;
    }

    /**
     * @param invoiceID the invoiceID to set
     */
    public void setInvoiceID(String invoiceID) {
        this.invoiceID = invoiceID;
    }

    /**
     * @return the paymentAmount
     */
    public Double getPaymentAmount() {
        return paymentAmount;
    }

    /**
     * @param paymentAmount the paymentAmount to set
     */
    public void setPaymentAmount(Double paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    /**
     * @return the paymentID
     */
    public String getPaymentID() {
        return paymentID;
    }

    /**
     * @param paymentID the paymentID to set
     */
    public void setPaymentID(String paymentID) {
        this.paymentID = paymentID;
    }

    /**
     * @return the paymentMethod
     */
    public String getPaymentMethod() {
        return paymentMethod;
    }

    /**
     * @param paymentMethod the paymentMethod to set
     */
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
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
     * @return the paymentSource
     */
    public String getPaymentSource() {
        return paymentSource;
    }

    /**
     * @param paymentSource the paymentSource to set
     */
    public void setPaymentSource(String paymentSource) {
        this.paymentSource = paymentSource;
    }

    /**
     * @return the paymentStatus
     */
    public String getPaymentStatus() {
        return paymentStatus;
    }

    /**
     * @param paymentStatus the paymentStatus to set
     */
    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    /**
     * @return the scheduledAmount
     */
    public Double getScheduledAmount() {
        return scheduledAmount;
    }

    /**
     * @param scheduledAmount the scheduledAmount to set
     */
    public void setScheduledAmount(Double scheduledAmount) {
        this.scheduledAmount = scheduledAmount;
    }

    /**
     * @return the settlementDate
     */
    public Date getSettlementDate() {
        return settlementDate;
    }

    /**
     * @param settlementDate the settlementDate to set
     */
    public void setSettlementDate(Date settlementDate) {
        this.settlementDate = settlementDate;
    }

    /**
     * @return the transactionFeeClient
     */
    public Double getTransactionFeeClient() {
        return transactionFeeClient;
    }

    /**
     * @param transactionFeeClient the transactionFeeClient to set
     */
    public void setTransactionFeeClient(Double transactionFeeClient) {
        this.transactionFeeClient = transactionFeeClient;
    }

    /**
     * @return the transactionFeeCustomer
     */
    public Double getTransactionFeeCustomer() {
        return transactionFeeCustomer;
    }

    /**
     * @param transactionFeeCustomer the transactionFeeCustomer to set
     */
    public void setTransactionFeeCustomer(Double transactionFeeCustomer) {
        this.transactionFeeCustomer = transactionFeeCustomer;
    }

    /**
     * @return the transactionTime
     */
    public Date getTransactionTime() {
        return transactionTime;
    }

    /**
     * @param transactionTime the transactionTime to set
     */
    public void setTransactionTime(Date transactionTime) {
        this.transactionTime = transactionTime;
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
}
