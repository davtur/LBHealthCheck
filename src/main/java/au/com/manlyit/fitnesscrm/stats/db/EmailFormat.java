/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.db;

import au.com.manlyit.fitnesscrm.stats.classes.util.BaseEntity;
import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.eclipse.persistence.annotations.Cache;
import org.eclipse.persistence.annotations.CacheCoordinationType;
import org.eclipse.persistence.annotations.CacheType;
import org.eclipse.persistence.annotations.DatabaseChangeNotificationType;
import org.eclipse.persistence.config.CacheIsolationType;

/**
 *
 * @author david
 */
@Cache(
        type = CacheType.SOFT_WEAK,// Cache everything in memory as this object is mostly read only and will be called hundreds of time on each page.
        size = 1000, // Use 64,000 as the initial cache size.
        //expiry = 36000000, // 10 minutes // by default it never expires which is what we want for this table
        coordinationType = CacheCoordinationType.INVALIDATE_CHANGED_OBJECTS,
        databaseChangeNotificationType = DatabaseChangeNotificationType.INVALIDATE,
        isolation = CacheIsolationType.SHARED
)
@Entity
@Table(name = "email_format")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "EmailFormat.findAll", query = "SELECT e FROM EmailFormat e"),
    @NamedQuery(name = "EmailFormat.findById", query = "SELECT e FROM EmailFormat e WHERE e.id = :id"),
    @NamedQuery(name = "EmailFormat.findByEmailFormat", query = "SELECT e FROM EmailFormat e WHERE e.emailFormat = :emailFormat")})
public class EmailFormat implements  BaseEntity, Serializable {

    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 45)
    @Column(name = "email_format")
    private String emailFormat;
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @OneToMany(mappedBy = "emailFormat")
    private Collection<Customers> customersCollection;

    public EmailFormat() {
    }

    public EmailFormat(Integer id) {
        this.id = id;
    }

    public EmailFormat(Integer id, String emailFormat) {
        this.id = id;
        this.emailFormat = emailFormat;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmailFormat() {
        return emailFormat;
    }

    public void setEmailFormat(String emailFormat) {
        this.emailFormat = emailFormat;
    }

    @XmlTransient
    public Collection<Customers> getCustomersCollection() {
        return customersCollection;
    }

    public void setCustomersCollection(Collection<Customers> customersCollection) {
        this.customersCollection = customersCollection;
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
        if (!(object instanceof EmailFormat)) {
            return false;
        }
        EmailFormat other = (EmailFormat) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return emailFormat;
    }

   
    
}