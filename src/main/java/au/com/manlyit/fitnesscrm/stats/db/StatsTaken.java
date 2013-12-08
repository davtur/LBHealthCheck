/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.db;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
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
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author david
 */
@Entity
@Table(name = "stats_taken")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "StatsTaken.findAll", query = "SELECT s FROM StatsTaken s"),
    @NamedQuery(name = "StatsTaken.findById", query = "SELECT s FROM StatsTaken s WHERE s.id = :id"),
    @NamedQuery(name = "StatsTaken.findByDateRecorded", query = "SELECT s FROM StatsTaken s WHERE s.dateRecorded = :dateRecorded"),
    @NamedQuery(name = "StatsTaken.findByImageId", query = "SELECT s FROM StatsTaken s WHERE s.imageId = :imageId")})
public class StatsTaken implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "date_recorded")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateRecorded;
    @Lob
    @Size(max = 65535)
    @Column(name = "trainer_comments")
    private String trainerComments;
    @Column(name = "image_id")
    private Integer imageId;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "statsTakenId")
    private Collection<Stat> statCollection;
    @JoinColumn(name = "customer_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Customers customerId;

    public StatsTaken() {
    }

    public StatsTaken(Integer id) {
        this.id = id;
    }

    public StatsTaken(Integer id, Date dateRecorded) {
        this.id = id;
        this.dateRecorded = dateRecorded;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getDateRecorded() {
        return dateRecorded;
    }

    public void setDateRecorded(Date dateRecorded) {
        this.dateRecorded = dateRecorded;
    }

    public String getTrainerComments() {
        return trainerComments;
    }

    public void setTrainerComments(String trainerComments) {
        this.trainerComments = trainerComments;
    }

    public Integer getImageId() {
        return imageId;
    }

    public void setImageId(Integer imageId) {
        this.imageId = imageId;
    }

    @XmlTransient
    public Collection<Stat> getStatCollection() {
        return statCollection;
    }

    public void setStatCollection(Collection<Stat> statCollection) {
        this.statCollection = statCollection;
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
        if (!(object instanceof StatsTaken)) {
            return false;
        }
        StatsTaken other = (StatsTaken) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.StatsTaken[ id=" + id + " ]";
    }
    
}
