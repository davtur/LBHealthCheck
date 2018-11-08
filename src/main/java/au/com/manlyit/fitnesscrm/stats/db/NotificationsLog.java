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
@Table(name = "notifications_log")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "NotificationsLog.findAll", query = "SELECT n FROM NotificationsLog n")
    , @NamedQuery(name = "NotificationsLog.findById", query = "SELECT n FROM NotificationsLog n WHERE n.id = :id")
    , @NamedQuery(name = "NotificationsLog.findByTypeOfNotification", query = "SELECT n FROM NotificationsLog n WHERE n.typeOfNotification = :typeOfNotification")
    , @NamedQuery(name = "NotificationsLog.findByTimestampOfNotification", query = "SELECT n FROM NotificationsLog n WHERE n.timestampOfNotification = :timestampOfNotification")})
public class NotificationsLog implements   BaseEntity, Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Lob
    @Size(max = 65535)
    @Column(name = "message")
    private String message;
    @Size(max = 32)
    @Column(name = "type_of_notification")
    private String typeOfNotification;
    @Basic(optional = false)
    @NotNull
    @Column(name = "timestamp_of_notification")
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestampOfNotification;
    @JoinColumn(name = "customer", referencedColumnName = "id")
    @ManyToOne
    private Customers customer;

    public NotificationsLog() {
    }

    public NotificationsLog(Integer id) {
        this.id = id;
    }

    public NotificationsLog(Integer id, Date timestampOfNotification) {
        this.id = id;
        this.timestampOfNotification = timestampOfNotification;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTypeOfNotification() {
        return typeOfNotification;
    }

    public void setTypeOfNotification(String typeOfNotification) {
        this.typeOfNotification = typeOfNotification;
    }

    public Date getTimestampOfNotification() {
        return timestampOfNotification;
    }

    public void setTimestampOfNotification(Date timestampOfNotification) {
        this.timestampOfNotification = timestampOfNotification;
    }

    public Customers getCustomer() {
        return customer;
    }

    public void setCustomer(Customers customer) {
        this.customer = customer;
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
        if (!(object instanceof NotificationsLog)) {
            return false;
        }
        NotificationsLog other = (NotificationsLog) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.NotificationsLog[ id=" + id + " ]";
    }
    
}
