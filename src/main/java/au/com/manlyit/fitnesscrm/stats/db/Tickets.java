/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.db;

import au.com.manlyit.fitnesscrm.stats.classes.util.BaseEntity;
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
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author david
 */
@Entity
@Table(name = "tickets")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Tickets.findAll", query = "SELECT t FROM Tickets t")
    , @NamedQuery(name = "Tickets.findById", query = "SELECT t FROM Tickets t WHERE t.id = :id")
    , @NamedQuery(name = "Tickets.findBySessionType", query = "SELECT t FROM Tickets t WHERE t.sessionType = :sessionType")
    , @NamedQuery(name = "Tickets.findByDatePurchased", query = "SELECT t FROM Tickets t WHERE t.datePurchased = :datePurchased")
    , @NamedQuery(name = "Tickets.findByDateUsed", query = "SELECT t FROM Tickets t WHERE t.dateUsed = :dateUsed")
    , @NamedQuery(name = "Tickets.findByValidFrom", query = "SELECT t FROM Tickets t WHERE t.validFrom = :validFrom")
    , @NamedQuery(name = "Tickets.findByExpires", query = "SELECT t FROM Tickets t WHERE t.expires = :expires")})
public class Tickets implements BaseEntity,Serializable {

    @Basic(optional = false)
    @NotNull
    @Column(name = "date_purchased")
    @Temporal(TemporalType.TIMESTAMP)
    private Date datePurchased;

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    
    @JoinColumn(name = "customer", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Customers customer;
    
    
    @JoinColumn(name = "session_type", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private SessionTypes sessionType;
    
    @Column(name = "date_used")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateUsed;
    @Column(name = "valid_from")
    @Temporal(TemporalType.TIMESTAMP)
    private Date validFrom;
    @Column(name = "expires")
    @Temporal(TemporalType.TIMESTAMP)
    private Date expires;

    public Tickets() {
    }

    public Tickets(Integer id) {
        this.id = id;
    }

    public Tickets(Integer id, SessionTypes sessionType, Date datePurchased) {
        this.id = id;
        this.sessionType = sessionType;
        this.datePurchased = datePurchased;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public SessionTypes getSessionType() {
        return sessionType;
    }

    public void setSessionType(SessionTypes sessionType) {
        this.sessionType = sessionType;
    }

    public Date getDatePurchased() {
        return datePurchased;
    }

    public void setDatePurchased(Date datePurchased) {
        this.datePurchased = datePurchased;
    }

    public Date getDateUsed() {
        return dateUsed;
    }

    public void setDateUsed(Date dateUsed) {
        this.dateUsed = dateUsed;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public Date getExpires() {
        return expires;
    }

    public void setExpires(Date expires) {
        this.expires = expires;
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
        if (!(object instanceof Tickets)) {
            return false;
        }
        Tickets other = (Tickets) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.Tickets[ id=" + id + " ]";
    }

    /**
     * @return the customer
     */
    public Customers getCustomer() {
        return customer;
    }

    /**
     * @param customer the customer to set
     */
    public void setCustomer(Customers customer) {
        this.customer = customer;
    }

    

}
