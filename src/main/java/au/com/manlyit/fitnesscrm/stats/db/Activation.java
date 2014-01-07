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
@Table(name = "activation")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Activation.findAll", query = "SELECT a FROM Activation a"),
    @NamedQuery(name = "Activation.findById", query = "SELECT a FROM Activation a WHERE a.id = :id"),
    @NamedQuery(name = "Activation.findByNonce", query = "SELECT a FROM Activation a WHERE a.nonce = :nonce"),
    @NamedQuery(name = "Activation.findByActTimestamp", query = "SELECT a FROM Activation a WHERE a.actTimestamp = :actTimestamp")})
public class Activation implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 127)
    @Column(name = "nonce")
    private String nonce;
    @Basic(optional = false)
    @NotNull
    @Column(name = "act_timestamp")
    @Temporal(TemporalType.TIMESTAMP)
    private Date actTimestamp;
    @JoinColumn(name = "customer", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Customers customer;

    public Activation() {
    }

    public Activation(Integer id) {
        this.id = id;
    }

    public Activation(Integer id, String nonce, Date actTimestamp) {
        this.id = id;
        this.nonce = nonce;
        this.actTimestamp = actTimestamp;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public Date getActTimestamp() {
        return actTimestamp;
    }

    public void setActTimestamp(Date actTimestamp) {
        this.actTimestamp = actTimestamp;
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
        if (!(object instanceof Activation)) {
            return false;
        }
        Activation other = (Activation) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.Activation[ id=" + id + " ]";
    }
    
}
