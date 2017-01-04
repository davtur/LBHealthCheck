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
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
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
@Table(name = "fitness_test_charts")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "FitnessTestCharts.findAll", query = "SELECT f FROM FitnessTestCharts f"),
    @NamedQuery(name = "FitnessTestCharts.findById", query = "SELECT f FROM FitnessTestCharts f WHERE f.id = :id"),
    @NamedQuery(name = "FitnessTestCharts.findByName", query = "SELECT f FROM FitnessTestCharts f WHERE f.name = :name"),
    @NamedQuery(name = "FitnessTestCharts.findByYaxisLabel", query = "SELECT f FROM FitnessTestCharts f WHERE f.yaxisLabel = :yaxisLabel")})
public class FitnessTestCharts implements  BaseEntity, Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 256)
    @Column(name = "name")
    private String name;
    @Basic(optional = false)
    @NotNull
    @Lob
    @Size(min = 1, max = 65535)
    @Column(name = "description")
    private String description;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "yaxis_label")
    private String yaxisLabel;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "chart")
    private Collection<StatTypes> statTypesCollection;

    public FitnessTestCharts() {
    }

    public FitnessTestCharts(Integer id) {
        this.id = id;
    }

    public FitnessTestCharts(Integer id, String name, String description, String yaxisLabel) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.yaxisLabel = yaxisLabel;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getYaxisLabel() {
        return yaxisLabel;
    }

    public void setYaxisLabel(String yaxisLabel) {
        this.yaxisLabel = yaxisLabel;
    }

    @XmlTransient
    public Collection<StatTypes> getStatTypesCollection() {
        return statTypesCollection;
    }

    public void setStatTypesCollection(Collection<StatTypes> statTypesCollection) {
        this.statTypesCollection = statTypesCollection;
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
        if (!(object instanceof FitnessTestCharts)) {
            return false;
        }
        FitnessTestCharts other = (FitnessTestCharts) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.FitnessTestCharts[ id=" + id + " ]";
    }
    
}
