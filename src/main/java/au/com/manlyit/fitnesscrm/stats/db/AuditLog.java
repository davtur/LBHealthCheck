/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.db;

import au.com.manlyit.fitnesscrm.stats.classes.util.BaseEntity;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
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
import org.eclipse.persistence.annotations.Cache;
import org.eclipse.persistence.annotations.CacheCoordinationType;
import org.eclipse.persistence.annotations.CacheType;
import org.eclipse.persistence.config.CacheIsolationType;

/**
 *
 * @author david
 */
@Cache(
        type = CacheType.SOFT_WEAK,// Cache everything in memory as this object is mostly read only and will be called hundreds of time on each page.
        size = 64000, // Use 64,000 as the initial cache size.
        //expiry = 36000000, // 10 minutes // by default it never expires which is what we want for this table
        coordinationType = CacheCoordinationType.INVALIDATE_CHANGED_OBJECTS,
        isolation = CacheIsolationType.SHARED
)
@Entity
@Table(name = "audit_log")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "AuditLog.findAll", query = "SELECT a FROM AuditLog a"),
    @NamedQuery(name = "AuditLog.findById", query = "SELECT a FROM AuditLog a WHERE a.id = :id"),
    @NamedQuery(name = "AuditLog.findByChangedFrom", query = "SELECT a FROM AuditLog a WHERE a.changedFrom = :changedFrom"),
    @NamedQuery(name = "AuditLog.findByChangedTo", query = "SELECT a FROM AuditLog a WHERE a.changedTo = :changedTo"),
    @NamedQuery(name = "AuditLog.findByTypeOfChange", query = "SELECT a FROM AuditLog a WHERE a.typeOfChange = :typeOfChange"),
    @NamedQuery(name = "AuditLog.findByTimestampOfChange", query = "SELECT a FROM AuditLog a WHERE a.timestampOfChange = :timestampOfChange")})
public class AuditLog implements BaseEntity,Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Lob
    @Size(max = 65535)
    @Column(name = "details_of_change")
    private String detailsOfChange;
    @Size(max = 255)
    @Column(name = "changed_from")
    private String changedFrom;
    @Size(max = 255)
    @Column(name = "changed_to")
    private String changedTo;
    @Size(max = 255)
    @Column(name = "type_of_change")
    private String typeOfChange;
    @Basic(optional = false)
    @NotNull
    @Column(name = "timestamp_of_change")
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestampOfChange;
    @JoinColumn(name = "changed_by", referencedColumnName = "id")
    @ManyToOne
    private Customers changedBy;
    @JoinColumn(name = "customer", referencedColumnName = "id")
    @ManyToOne
    private Customers customer;

    public AuditLog() {
    }

    public AuditLog(Integer id) {
        this.id = id;
    }

    public AuditLog(Integer id, Date timestampOfChange) {
        this.id = id;
        this.timestampOfChange = timestampOfChange;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDetailsOfChange() {
        return detailsOfChange;
    }

    public void setDetailsOfChange(String detailsOfChange) {
        this.detailsOfChange = detailsOfChange;
    }

    public String getChangedFrom() {
        return changedFrom;
    }

    public void setChangedFrom(String changedFrom) {
        this.changedFrom = changedFrom;
    }

    public String getChangedTo() {
        return changedTo;
    }

    public void setChangedTo(String changedTo) {
        this.changedTo = changedTo;
    }

    public String getTypeOfChange() {
        return typeOfChange;
    }

    public void setTypeOfChange(String typeOfChange) {
        this.typeOfChange = typeOfChange;
    }

    public Date getTimestampOfChange() {
        return timestampOfChange;
    }

    public void setTimestampOfChange(Date timestampOfChange) {
        this.timestampOfChange = timestampOfChange;
    }

    public Customers getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(Customers changedBy) {
        this.changedBy = changedBy;
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
        if (!(object instanceof AuditLog)) {
            return false;
        }
        AuditLog other = (AuditLog) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.beans.AuditLog[ id=" + id + " ]";
    }
    
}
