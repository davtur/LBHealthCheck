/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.db;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
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
    @ManyToOne(optional = false)
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

}
