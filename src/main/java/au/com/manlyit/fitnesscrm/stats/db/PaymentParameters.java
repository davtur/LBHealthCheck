/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.db;

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
import javax.persistence.Lob;
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
@Table(name = "paymentParameters")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "PaymentParameters.findAll", query = "SELECT p FROM PaymentParameters p"),
    @NamedQuery(name = "PaymentParameters.findById", query = "SELECT p FROM PaymentParameters p WHERE p.id = :id"),
    @NamedQuery(name = "PaymentParameters.findByContractStartDate", query = "SELECT p FROM PaymentParameters p WHERE p.contractStartDate = :contractStartDate"),
    @NamedQuery(name = "PaymentParameters.findByMobilePhoneNumber", query = "SELECT p FROM PaymentParameters p WHERE p.mobilePhoneNumber = :mobilePhoneNumber"),
    @NamedQuery(name = "PaymentParameters.findBySmsPaymentReminder", query = "SELECT p FROM PaymentParameters p WHERE p.smsPaymentReminder = :smsPaymentReminder"),
    @NamedQuery(name = "PaymentParameters.findBySmsFailedNotification", query = "SELECT p FROM PaymentParameters p WHERE p.smsFailedNotification = :smsFailedNotification"),
    @NamedQuery(name = "PaymentParameters.findBySmsExpiredCard", query = "SELECT p FROM PaymentParameters p WHERE p.smsExpiredCard = :smsExpiredCard"),
    @NamedQuery(name = "PaymentParameters.findByPaymentGatewayName", query = "SELECT p FROM PaymentParameters p WHERE p.paymentGatewayName = :paymentGatewayName")})
public class PaymentParameters implements Serializable {
    @Column(name = "paymentRegularAmount")
    private BigDecimal paymentRegularAmount;
    @Column(name = "paymentRegularTotalPaymentsAmount")
    private BigDecimal paymentRegularTotalPaymentsAmount;
    @Column(name = "paymentRegularDuration")
    private Integer paymentRegularDuration;
    @Column(name = "paymentsRegularTotalNumberOfPayments")
    private Integer paymentsRegularTotalNumberOfPayments;
    @JoinColumn(name = "nextScheduledPayment", referencedColumnName = "id")
    @OneToOne
    private Payments nextScheduledPayment;
    @JoinColumn(name = "lastSuccessfulScheduledPayment", referencedColumnName = "id")
    @OneToOne
    private Payments lastSuccessfulScheduledPayment;
    @Size(max = 255)
    @Column(name = "addressLine1")
    private String addressLine1;
    @Size(max = 255)
    @Column(name = "addressLine2")
    private String addressLine2;
    @Size(max = 10)
    @Column(name = "addressPostCode")
    private String addressPostCode;
    @Size(max = 127)
    @Column(name = "addressState")
    private String addressState;
    @Size(max = 127)
    @Column(name = "addressSuburb")
    private String addressSuburb;
    @Size(max = 255)
    @Column(name = "customerFirstName")
    private String customerFirstName;
    @Size(max = 255)
    @Column(name = "customerName")
    private String customerName;
    // @Pattern(regexp="[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", message="Invalid email")//if the field contains email address consider using this annotation to enforce field validation
    @Size(max = 255)
    @Column(name = "email")
    private String email;
    @Size(max = 50)
    @Column(name = "ezidebitCustomerID")
    private String ezidebitCustomerID;
    @Size(max = 15)
    @Column(name = "paymentMethod")
    private String paymentMethod;
    @Size(max = 15)
    @Column(name = "paymentPeriod")
    private String paymentPeriod;
    @Size(max = 15)
    @Column(name = "paymentPeriodDayOfMonth")
    private String paymentPeriodDayOfMonth;
    @Size(max = 15)
    @Column(name = "paymentPeriodDayOfWeek")
    private String paymentPeriodDayOfWeek;
    @Size(max = 50)
    @Column(name = "statusCode")
    private String statusCode;
    @Size(max = 255)
    @Column(name = "statusDescription")
    private String statusDescription;
    @Column(name = "totalPaymentsFailed")
    private Integer totalPaymentsFailed;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Column(name = "totalPaymentsFailedAmount")
    private BigDecimal totalPaymentsFailedAmount;
    @Column(name = "totalPaymentsSuccessful")
    private Integer totalPaymentsSuccessful;
    @Column(name = "totalPaymentsSuccessfulAmount")
    private BigDecimal totalPaymentsSuccessfulAmount;
    @Size(max = 50)
    @Column(name = "yourGeneralReference")
    private String yourGeneralReference;
    @Size(max = 50)
    @Column(name = "yourSystemReference")
    private String yourSystemReference;

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "contractStartDate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date contractStartDate;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "mobilePhoneNumber")
    private String mobilePhoneNumber;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 5)
    @Column(name = "SmsPaymentReminder")
    private String smsPaymentReminder;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 5)
    @Column(name = "SmsFailedNotification")
    private String smsFailedNotification;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 5)
    @Column(name = "SmsExpiredCard")
    private String smsExpiredCard;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 45)
    @Column(name = "paymentGatewayName")
    private String paymentGatewayName;
    @JoinColumn(name = "loggedInUser", referencedColumnName = "id")
    @OneToOne(optional = false)
    private Customers loggedInUser;
    @Lob
    @Size(min = 1, max = 65535)
    @Column(name = "webddrUrl")
    private String webddrUrl;

    public PaymentParameters() {
    }

    public PaymentParameters(Integer id) {
        this.id = id;
    }

    public PaymentParameters(Integer id, Date contractStartDate, String mobilePhoneNumber, String smsPaymentReminder, String smsFailedNotification, String smsExpiredCard, String paymentGatewayName) {
        this.id = id;
        this.contractStartDate = contractStartDate;
        this.mobilePhoneNumber = mobilePhoneNumber;
        this.smsPaymentReminder = smsPaymentReminder;
        this.smsFailedNotification = smsFailedNotification;
        this.smsExpiredCard = smsExpiredCard;
        this.paymentGatewayName = paymentGatewayName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getContractStartDate() {
        return contractStartDate;
    }

    public void setContractStartDate(Date contractStartDate) {
        this.contractStartDate = contractStartDate;
    }

    public String getMobilePhoneNumber() {
        return mobilePhoneNumber;
    }

    public void setMobilePhoneNumber(String mobilePhoneNumber) {
        this.mobilePhoneNumber = mobilePhoneNumber;
    }

    public String getSmsPaymentReminder() {
        return smsPaymentReminder;
    }

    public void setSmsPaymentReminder(String smsPaymentReminder) {
        this.smsPaymentReminder = smsPaymentReminder;
    }

    public String getSmsFailedNotification() {
        return smsFailedNotification;
    }

    public void setSmsFailedNotification(String smsFailedNotification) {
        this.smsFailedNotification = smsFailedNotification;
    }

    public String getSmsExpiredCard() {
        return smsExpiredCard;
    }

    public void setSmsExpiredCard(String smsExpiredCard) {
        this.smsExpiredCard = smsExpiredCard;
    }

    public String getPaymentGatewayName() {
        return paymentGatewayName;
    }

    public void setPaymentGatewayName(String paymentGatewayName) {
        this.paymentGatewayName = paymentGatewayName;
    }

    public Customers getLoggedInUser() {
        return loggedInUser;
    }

    public void setLoggedInUser(Customers loggedInUser) {
        this.loggedInUser = loggedInUser;
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
        if (!(object instanceof PaymentParameters)) {
            return false;
        }
        PaymentParameters other = (PaymentParameters) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.PaymentParameters[ id=" + id + " ]";
    }

    /**
     * @return the shedTitle
     */
    public String getWebddrUrl() {
        return webddrUrl;
    }

    /**
     * @param shedTitle the shedTitle to set
     */
    public void setWebddrUrl(String shedTitle) {
        this.webddrUrl = shedTitle;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getAddressPostCode() {
        return addressPostCode;
    }

    public void setAddressPostCode(String addressPostCode) {
        this.addressPostCode = addressPostCode;
    }

    public String getAddressState() {
        return addressState;
    }

    public void setAddressState(String addressState) {
        this.addressState = addressState;
    }

    public String getAddressSuburb() {
        return addressSuburb;
    }

    public void setAddressSuburb(String addressSuburb) {
        this.addressSuburb = addressSuburb;
    }

    public String getCustomerFirstName() {
        return customerFirstName;
    }

    public void setCustomerFirstName(String customerFirstName) {
        this.customerFirstName = customerFirstName;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEzidebitCustomerID() {
        return ezidebitCustomerID;
    }

    public void setEzidebitCustomerID(String ezidebitCustomerID) {
        this.ezidebitCustomerID = ezidebitCustomerID;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentPeriod() {
        return paymentPeriod;
    }

    public void setPaymentPeriod(String paymentPeriod) {
        this.paymentPeriod = paymentPeriod;
    }

    public String getPaymentPeriodDayOfMonth() {
        return paymentPeriodDayOfMonth;
    }

    public void setPaymentPeriodDayOfMonth(String paymentPeriodDayOfMonth) {
        this.paymentPeriodDayOfMonth = paymentPeriodDayOfMonth;
    }

    public String getPaymentPeriodDayOfWeek() {
        return paymentPeriodDayOfWeek;
    }

    public void setPaymentPeriodDayOfWeek(String paymentPeriodDayOfWeek) {
        this.paymentPeriodDayOfWeek = paymentPeriodDayOfWeek;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusDescription() {
        return statusDescription;
    }

    public void setStatusDescription(String statusDescription) {
        this.statusDescription = statusDescription;
    }

    public Integer getTotalPaymentsFailed() {
        return totalPaymentsFailed;
    }

    public void setTotalPaymentsFailed(Integer totalPaymentsFailed) {
        this.totalPaymentsFailed = totalPaymentsFailed;
    }

    public BigDecimal getTotalPaymentsFailedAmount() {
        return totalPaymentsFailedAmount;
    }

    public void setTotalPaymentsFailedAmount(BigDecimal totalPaymentsFailedAmount) {
        this.totalPaymentsFailedAmount = totalPaymentsFailedAmount;
    }

    public Integer getTotalPaymentsSuccessful() {
        return totalPaymentsSuccessful;
    }

    public void setTotalPaymentsSuccessful(Integer totalPaymentsSuccessful) {
        this.totalPaymentsSuccessful = totalPaymentsSuccessful;
    }

    public BigDecimal getTotalPaymentsSuccessfulAmount() {
        return totalPaymentsSuccessfulAmount;
    }

    public void setTotalPaymentsSuccessfulAmount(BigDecimal totalPaymentsSuccessfulAmount) {
        this.totalPaymentsSuccessfulAmount = totalPaymentsSuccessfulAmount;
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

    public Payments getNextScheduledPayment() {
        return nextScheduledPayment;
    }

    public void setNextScheduledPayment(Payments nextScheduledPayment) {
        this.nextScheduledPayment = nextScheduledPayment;
    }

    public Payments getLastSuccessfulScheduledPayment() {
        return lastSuccessfulScheduledPayment;
    }

    public void setLastSuccessfulScheduledPayment(Payments lastSuccessfulScheduledPayment) {
        this.lastSuccessfulScheduledPayment = lastSuccessfulScheduledPayment;
    }

    public BigDecimal getPaymentRegularAmount() {
        return paymentRegularAmount;
    }

    public void setPaymentRegularAmount(BigDecimal paymentRegularAmount) {
        this.paymentRegularAmount = paymentRegularAmount;
    }

    public BigDecimal getPaymentRegularTotalPaymentsAmount() {
        return paymentRegularTotalPaymentsAmount;
    }

    public void setPaymentRegularTotalPaymentsAmount(BigDecimal paymentRegularTotalPaymentsAmount) {
        this.paymentRegularTotalPaymentsAmount = paymentRegularTotalPaymentsAmount;
    }

    public Integer getPaymentRegularDuration() {
        return paymentRegularDuration;
    }

    public void setPaymentRegularDuration(Integer paymentRegularDuration) {
        this.paymentRegularDuration = paymentRegularDuration;
    }

    public Integer getPaymentsRegularTotalNumberOfPayments() {
        return paymentsRegularTotalNumberOfPayments;
    }

    public void setPaymentsRegularTotalNumberOfPayments(Integer paymentsRegularTotalNumberOfPayments) {
        this.paymentsRegularTotalNumberOfPayments = paymentsRegularTotalNumberOfPayments;
    }

}
