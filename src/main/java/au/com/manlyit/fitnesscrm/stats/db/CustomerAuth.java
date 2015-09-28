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

/**
 *
 * @author david
 */
@Entity
@Table(name = "customer_auth")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "CustomerAuth.findAll", query = "SELECT c FROM CustomerAuth c"),
    @NamedQuery(name = "CustomerAuth.findById", query = "SELECT c FROM CustomerAuth c WHERE c.id = :id"),
    @NamedQuery(name = "CustomerAuth.findByCustomerAuthType", query = "SELECT c FROM CustomerAuth c WHERE c.customerAuthType = :customerAuthType")})
public class CustomerAuth implements  BaseEntity, Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 45)
    @Column(name = "customer_auth_type")
    private String customerAuthType;
    @OneToMany(mappedBy = "auth")
    private Collection<Customers> customersCollection;

    public CustomerAuth() {
    }

    public CustomerAuth(Integer id) {
        this.id = id;
    }

    public CustomerAuth(Integer id, String customerAuthType) {
        this.id = id;
        this.customerAuthType = customerAuthType;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getCustomerAuthType() {
        return customerAuthType;
    }

    public void setCustomerAuthType(String customerAuthType) {
        this.customerAuthType = customerAuthType;
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
        if (!(object instanceof CustomerAuth)) {
            return false;
        }
        CustomerAuth other = (CustomerAuth) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return customerAuthType;
    }
    
}
