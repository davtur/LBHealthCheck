/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.db;

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

/**
 *
 * @author david
 */
@Entity
@Table(name = "demograhic_types")
@NamedQueries({
    @NamedQuery(name = "DemograhicTypes.findAll", query = "SELECT d FROM DemograhicTypes d"),
    @NamedQuery(name = "DemograhicTypes.findById", query = "SELECT d FROM DemograhicTypes d WHERE d.id = :id"),
    @NamedQuery(name = "DemograhicTypes.findByName", query = "SELECT d FROM DemograhicTypes d WHERE d.name = :name"),
    @NamedQuery(name = "DemograhicTypes.findByDescription", query = "SELECT d FROM DemograhicTypes d WHERE d.description = :description")})
public class DemograhicTypes implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @Column(name = "name")
    private String name;
    @Basic(optional = false)
    @Column(name = "description")
    private String description;
    @OneToMany(mappedBy = "demograhicTypes")
    private Collection<Customers> customersCollection;

    public DemograhicTypes() {
    }

    public DemograhicTypes(Integer id) {
        this.id = id;
    }

    public DemograhicTypes(Integer id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
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
        if (!(object instanceof DemograhicTypes)) {
            return false;
        }
        DemograhicTypes other = (DemograhicTypes) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.DemograhicTypes[id=" + id + "]";
    }

}
