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
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author david
 */
@Entity
@Table(name = "session_bookings")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "SessionBookings.findAll", query = "SELECT s FROM SessionBookings s"),
    @NamedQuery(name = "SessionBookings.findById", query = "SELECT s FROM SessionBookings s WHERE s.id = :id"),
    @NamedQuery(name = "SessionBookings.findByBookingTime", query = "SELECT s FROM SessionBookings s WHERE s.bookingTime = :bookingTime")})
public class SessionBookings implements Serializable {
    @Size(max = 255)
    @Column(name = "status_description")
    private String statusDescription;
    @Size(max = 255)
    @Column(name = "status")
    private String status;
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Column(name = "booking_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date bookingTime;
    @JoinColumn(name = "customer_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Customers customerId;
    @JoinColumn(name = "session_history_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private SessionHistory sessionHistoryId;
    @JoinColumn(name = "payment_id", referencedColumnName = "id")
    @OneToOne
    private Payments paymentId;

    public SessionBookings() {
    }

    public SessionBookings(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getBookingTime() {
        return bookingTime;
    }

    public void setBookingTime(Date bookingTime) {
        this.bookingTime = bookingTime;
    }

    public Customers getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Customers customerId) {
        this.customerId = customerId;
    }

    public SessionHistory getSessionHistoryId() {
        return sessionHistoryId;
    }

    public void setSessionHistoryId(SessionHistory sessionHistoryId) {
        this.sessionHistoryId = sessionHistoryId;
    }

    public Payments getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Payments paymentId) {
        this.paymentId = paymentId;
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
        if (!(object instanceof SessionBookings)) {
            return false;
        }
        SessionBookings other = (SessionBookings) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.SessionBookings[ id=" + id + " ]";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusDescription() {
        return statusDescription;
    }

    public void setStatusDescription(String statusDescription) {
        this.statusDescription = statusDescription;
    }
    
}