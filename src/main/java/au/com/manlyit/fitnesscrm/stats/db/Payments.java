/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.db;

import au.com.manlyit.fitnesscrm.stats.classes.util.BaseEntity;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author david
 */
@Entity
@Table(name = "payments")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Payments.findAll", query = "SELECT p FROM Payments p"),
    @NamedQuery(name = "Payments.findById", query = "SELECT p FROM Payments p WHERE p.id = :id"),
    @NamedQuery(name = "Payments.findByCreateDatetime", query = "SELECT p FROM Payments p WHERE p.createDatetime = :createDatetime"),
    @NamedQuery(name = "Payments.findByLastUpdatedDatetime", query = "SELECT p FROM Payments p WHERE p.lastUpdatedDatetime = :lastUpdatedDatetime"),
    @NamedQuery(name = "Payments.findByBankFailedReason", query = "SELECT p FROM Payments p WHERE p.bankFailedReason = :bankFailedReason"),
    @NamedQuery(name = "Payments.findByBankReceiptID", query = "SELECT p FROM Payments p WHERE p.bankReceiptID = :bankReceiptID"),
    @NamedQuery(name = "Payments.findByBankReturnCode", query = "SELECT p FROM Payments p WHERE p.bankReturnCode = :bankReturnCode"),
    @NamedQuery(name = "Payments.findByDebitDate", query = "SELECT p FROM Payments p WHERE p.debitDate = :debitDate"),
    @NamedQuery(name = "Payments.findByEzidebitCustomerID", query = "SELECT p FROM Payments p WHERE p.ezidebitCustomerID = :ezidebitCustomerID"),
    @NamedQuery(name = "Payments.findByInvoiceID", query = "SELECT p FROM Payments p WHERE p.invoiceID = :invoiceID"),
    @NamedQuery(name = "Payments.findByManuallyAddedPayment", query = "SELECT p FROM Payments p WHERE p.manuallyAddedPayment = :manuallyAddedPayment"),
    @NamedQuery(name = "Payments.findByPaymentAmount", query = "SELECT p FROM Payments p WHERE p.paymentAmount = :paymentAmount"),
    @NamedQuery(name = "Payments.findByPaymentDate", query = "SELECT p FROM Payments p WHERE p.paymentDate = :paymentDate"),
    @NamedQuery(name = "Payments.findByPaymentID", query = "SELECT p FROM Payments p WHERE p.paymentID = :paymentID"),
    @NamedQuery(name = "Payments.findByPaymentMethod", query = "SELECT p FROM Payments p WHERE p.paymentMethod = :paymentMethod"),
    @NamedQuery(name = "Payments.findByPaymentReference", query = "SELECT p FROM Payments p WHERE p.paymentReference = :paymentReference"),
    @NamedQuery(name = "Payments.findByPaymentSource", query = "SELECT p FROM Payments p WHERE p.paymentSource = :paymentSource"),
    @NamedQuery(name = "Payments.findByPaymentStatus", query = "SELECT p FROM Payments p WHERE p.paymentStatus = :paymentStatus"),
    @NamedQuery(name = "Payments.findByScheduledAmount", query = "SELECT p FROM Payments p WHERE p.scheduledAmount = :scheduledAmount"),
    @NamedQuery(name = "Payments.findBySettlementDate", query = "SELECT p FROM Payments p WHERE p.settlementDate = :settlementDate"),
    @NamedQuery(name = "Payments.findByTransactionFeeClient", query = "SELECT p FROM Payments p WHERE p.transactionFeeClient = :transactionFeeClient"),
    @NamedQuery(name = "Payments.findByTransactionFeeCustomer", query = "SELECT p FROM Payments p WHERE p.transactionFeeCustomer = :transactionFeeCustomer"),
    @NamedQuery(name = "Payments.findByTransactionTime", query = "SELECT p FROM Payments p WHERE p.transactionTime = :transactionTime"),
    @NamedQuery(name = "Payments.findByYourGeneralReference", query = "SELECT p FROM Payments p WHERE p.yourGeneralReference = :yourGeneralReference"),
    @NamedQuery(name = "Payments.findByYourSystemReference", query = "SELECT p FROM Payments p WHERE p.yourSystemReference = :yourSystemReference")})
public class Payments implements  BaseEntity,Serializable {
    @OneToOne(mappedBy = "paymentId")
    private SessionBookings sessionBookings;
    @OneToOne(mappedBy = "nextScheduledPayment")
    private PaymentParameters paymentParameters;
    @OneToOne(mappedBy = "lastSuccessfulScheduledPayment")
    private PaymentParameters paymentParameters1;
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "create_datetime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createDatetime;
    @Basic(optional = false)
    @NotNull
    @Column(name = "last_updated_datetime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUpdatedDatetime;
    @Size(max = 70)
    @Column(name = "bank_Failed_Reason")
    private String bankFailedReason;
    @Size(max = 70)
    @Column(name = "bank_Receipt_ID")
    private String bankReceiptID;
    @Size(max = 70)
    @Column(name = "bank_Return_Code")
    private String bankReturnCode;
    @Column(name = "debit_Date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date debitDate;
    @Size(max = 50)
    @Column(name = "ezidebitCustomerID")
    private String ezidebitCustomerID;
    @Size(max = 50)
    @Column(name = "invoiceID")
    private String invoiceID;
    @Column(name = "manuallyAddedPayment")
    private Boolean manuallyAddedPayment;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Column(name = "paymentAmount")
    private BigDecimal paymentAmount;
    @Column(name = "paymentDate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date paymentDate;
    @Size(max = 50)
    @Column(name = "paymentID")
    private String paymentID;
    @Size(max = 50)
    @Column(name = "paymentMethod")
    private String paymentMethod;
    @Size(max = 50)
    @Column(name = "paymentReference")
    private String paymentReference;
    @Size(max = 50)
    @Column(name = "paymentSource")
    private String paymentSource;
    @Size(max = 50)
    @Column(name = "paymentStatus")
    private String paymentStatus;
    @Column(name = "scheduledAmount")
    private BigDecimal scheduledAmount;
    @Column(name = "settlementDate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date settlementDate;
    @Column(name = "transactionFeeClient")
    private BigDecimal transactionFeeClient;
    @Column(name = "transactionFeeCustomer")
    private BigDecimal transactionFeeCustomer;
    @Column(name = "transactionTime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date transactionTime;
    @Size(max = 50)
    @Column(name = "yourGeneralReference")
    private String yourGeneralReference;
    @Size(max = 50)
    @Column(name = "yourSystemReference")
    private String yourSystemReference;
    @JoinColumn(name = "customer_Name", referencedColumnName = "id")
    @ManyToOne
    private Customers customerName;

    public Payments() {
    }

    public Payments(Integer id) {
        this.id = id;
    }

    public Payments(Integer id, Date createDatetime, Date lastUpdatedDatetime) {
        this.id = id;
        this.createDatetime = createDatetime;
        this.lastUpdatedDatetime = lastUpdatedDatetime;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getCreateDatetime() {
        return createDatetime;
    }

    public void setCreateDatetime(Date createDatetime) {
        this.createDatetime = createDatetime;
    }

    public Date getLastUpdatedDatetime() {
        return lastUpdatedDatetime;
    }

    public void setLastUpdatedDatetime(Date lastUpdatedDatetime) {
        this.lastUpdatedDatetime = lastUpdatedDatetime;
    }

    public String getBankFailedReason() {
        return bankFailedReason;
    }

    public void setBankFailedReason(String bankFailedReason) {
        this.bankFailedReason = bankFailedReason;
    }

    public String getBankReceiptID() {
        return bankReceiptID;
    }

    public void setBankReceiptID(String bankReceiptID) {
        this.bankReceiptID = bankReceiptID;
    }

    public String getBankReturnCode() {
        return bankReturnCode;
    }

    public void setBankReturnCode(String bankReturnCode) {
        this.bankReturnCode = bankReturnCode;
    }

    public Date getDebitDate() {
        return debitDate;
    }

    public void setDebitDate(Date debitDate) {
        this.debitDate = debitDate;
    }

    public String getEzidebitCustomerID() {
        return ezidebitCustomerID;
    }

    public void setEzidebitCustomerID(String ezidebitCustomerID) {
        this.ezidebitCustomerID = ezidebitCustomerID;
    }

    public String getInvoiceID() {
        return invoiceID;
    }

    public void setInvoiceID(String invoiceID) {
        this.invoiceID = invoiceID;
    }

    public Boolean getManuallyAddedPayment() {
        return manuallyAddedPayment;
    }

    public void setManuallyAddedPayment(Boolean manuallyAddedPayment) {
        this.manuallyAddedPayment = manuallyAddedPayment;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(BigDecimal paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public Date getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Date paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getPaymentID() {
        return paymentID;
    }

    public void setPaymentID(String paymentID) {
        this.paymentID = paymentID;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getPaymentSource() {
        return paymentSource;
    }

    public void setPaymentSource(String paymentSource) {
        this.paymentSource = paymentSource;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public BigDecimal getScheduledAmount() {
        return scheduledAmount;
    }

    public void setScheduledAmount(BigDecimal scheduledAmount) {
        this.scheduledAmount = scheduledAmount;
    }

    public Date getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(Date settlementDate) {
        this.settlementDate = settlementDate;
    }

    public BigDecimal getTransactionFeeClient() {
        return transactionFeeClient;
    }

    public void setTransactionFeeClient(BigDecimal transactionFeeClient) {
        this.transactionFeeClient = transactionFeeClient;
    }

    public BigDecimal getTransactionFeeCustomer() {
        return transactionFeeCustomer;
    }

    public void setTransactionFeeCustomer(BigDecimal transactionFeeCustomer) {
        this.transactionFeeCustomer = transactionFeeCustomer;
    }

    public Date getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(Date transactionTime) {
        this.transactionTime = transactionTime;
    }

    public String getYourGeneralReference() {
        return yourGeneralReference;
    }

    public void setYourGeneralReference(String yourGeneralReference) {
        this.yourGeneralReference = yourGeneralReference;
    }

    public String getYourSystemReference() {
        return yourSystemReference;
    }

    public void setYourSystemReference(String yourSystemReference) {
        this.yourSystemReference = yourSystemReference;
    }

    public Customers getCustomerName() {
        return customerName;
    }

    public void setCustomerName(Customers customerName) {
        this.customerName = customerName;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Payments)) {
            return false;
        }
        Payments other = (Payments) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.Payments[ id=" + id + " ]";
    }

    public PaymentParameters getPaymentParameters() {
        return paymentParameters;
    }

    public void setPaymentParameters(PaymentParameters paymentParameters) {
        this.paymentParameters = paymentParameters;
    }

    public PaymentParameters getPaymentParameters1() {
        return paymentParameters1;
    }

    public void setPaymentParameters1(PaymentParameters paymentParameters1) {
        this.paymentParameters1 = paymentParameters1;
    }

    public SessionBookings getSessionBookings() {
        return sessionBookings;
    }

    public void setSessionBookings(SessionBookings sessionBookings) {
        this.sessionBookings = sessionBookings;
    }
    
}
