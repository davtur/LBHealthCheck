/*
 * To change this template, choose Tools | Templates
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
    @NamedQuery(name = "Activation.findByTimestamp", query = "SELECT a FROM Activation a WHERE a.act_timestamp = :act_timestamp")})
public class Activation implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @NotNull
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
    private Date act_timestamp;
    @JoinColumn(name = "customer", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Customers customerId;

    public Activation() {
    }

    public Activation(Integer id) {
        this.id = id;
    }

    public Activation(Integer id, String nonce, Date timestamp) {
        this.id = id;
        this.nonce = nonce;
        this.act_timestamp = timestamp;
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

    public Date getAct_timestamp() {
        return act_timestamp;
    }

    public void setAct_timestamp(Date act_timestamp) {
        this.act_timestamp = act_timestamp;
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

    /**
     * @return the customerId
     */
    public Customers getCustomerId() {
        return customerId;
    }

    /**
     * @param customerId the customerId to set
     */
    public void setCustomerId(Customers customerId) {
        this.customerId = customerId;
    }
    
}
