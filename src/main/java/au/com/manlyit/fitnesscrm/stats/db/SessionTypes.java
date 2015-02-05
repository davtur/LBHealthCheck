/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.db;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
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
@Table(name = "session_types")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "SessionTypes.findAll", query = "SELECT s FROM SessionTypes s"),
    @NamedQuery(name = "SessionTypes.findById", query = "SELECT s FROM SessionTypes s WHERE s.id = :id"),
    @NamedQuery(name = "SessionTypes.findByName", query = "SELECT s FROM SessionTypes s WHERE s.name = :name"),
    @NamedQuery(name = "SessionTypes.findByDescription", query = "SELECT s FROM SessionTypes s WHERE s.description = :description")})
public class SessionTypes implements Serializable {
    @OneToMany(mappedBy = "sessionType")
    private Collection<Plan> planCollection;
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 32)
    @Column(name = "name")
    private String name;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 96)
    @Column(name = "description")
    private String description;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "sessionTypesId")
    private Collection<SessionHistory> sessionHistoryCollection;
    
    public SessionTypes() {
    }

    public SessionTypes(Integer id) {
        this.id = id;
    }

    public SessionTypes(Integer id, String name, String description) {
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

    @XmlTransient
    public Collection<SessionHistory> getSessionHistoryCollection() {
        return sessionHistoryCollection;
    }

    public void setSessionHistoryCollection(Collection<SessionHistory> sessionHistoryCollection) {
        this.sessionHistoryCollection = sessionHistoryCollection;
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
        if (!(object instanceof SessionTypes)) {
            return false;
        }
        SessionTypes other = (SessionTypes) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return name;
    }

    @XmlTransient
    public Collection<Plan> getPlanCollection() {
        return planCollection;
    }

    public void setPlanCollection(Collection<Plan> planCollection) {
        this.planCollection = planCollection;
    }

}
