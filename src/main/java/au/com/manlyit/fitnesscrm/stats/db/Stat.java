/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.db;

import java.io.Serializable;
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
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author david
 */
@Entity
@Table(name = "stat")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Stat.findAll", query = "SELECT s FROM Stat s"),
    @NamedQuery(name = "Stat.findById", query = "SELECT s FROM Stat s WHERE s.id = :id"),
    @NamedQuery(name = "Stat.findByValue", query = "SELECT s FROM Stat s WHERE s.value = :value")})
public class Stat implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "value")
    private float value;
    @JoinColumn(name = "stats_taken_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private StatsTaken statsTakenId;
    @JoinColumn(name = "stat_type", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private StatTypes statType;

    public Stat() {
    }

    public Stat(Integer id) {
        this.id = id;
    }

    public Stat(Integer id, float value) {
        this.id = id;
        this.value = value;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public StatsTaken getStatsTakenId() {
        return statsTakenId;
    }

    public void setStatsTakenId(StatsTaken statsTakenId) {
        this.statsTakenId = statsTakenId;
    }

    public StatTypes getStatType() {
        return statType;
    }

    public void setStatType(StatTypes statType) {
        this.statType = statType;
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
        if (!(object instanceof Stat)) {
            return false;
        }
        Stat other = (Stat) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.Stat[ id=" + id + " ]";
    }

}
