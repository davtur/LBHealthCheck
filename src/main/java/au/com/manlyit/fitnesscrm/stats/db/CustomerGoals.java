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
@Table(name = "customer_goals")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "CustomerGoals.findAll", query = "SELECT c FROM CustomerGoals c"),
    @NamedQuery(name = "CustomerGoals.findById", query = "SELECT c FROM CustomerGoals c WHERE c.id = :id"),
    @NamedQuery(name = "CustomerGoals.findByGoal", query = "SELECT c FROM CustomerGoals c WHERE c.goal = :goal"),
    @NamedQuery(name = "CustomerGoals.findByStartdate", query = "SELECT c FROM CustomerGoals c WHERE c.startdate = :startdate"),
    @NamedQuery(name = "CustomerGoals.findByFinishdate", query = "SELECT c FROM CustomerGoals c WHERE c.finishdate = :finishdate")})
public class CustomerGoals implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 256)
    @Column(name = "goal")
    private String goal;
    @Basic(optional = false)
    @NotNull
    @Column(name = "startdate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startdate;
    @Basic(optional = false)
    @NotNull
    @Column(name = "finishdate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date finishdate;
    @JoinColumn(name = "customer_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Customers customerId;

    public CustomerGoals() {
    }

    public CustomerGoals(Integer id) {
        this.id = id;
    }

    public CustomerGoals(Integer id, String goal, Date startdate, Date finishdate) {
        this.id = id;
        this.goal = goal;
        this.startdate = startdate;
        this.finishdate = finishdate;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public Date getStartdate() {
        return startdate;
    }

    public void setStartdate(Date startdate) {
        this.startdate = startdate;
    }

    public Date getFinishdate() {
        return finishdate;
    }

    public void setFinishdate(Date finishdate) {
        this.finishdate = finishdate;
    }

    public Customers getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Customers customerId) {
        this.customerId = customerId;
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
        if (!(object instanceof CustomerGoals)) {
            return false;
        }
        CustomerGoals other = (CustomerGoals) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return goal;
    }

}
