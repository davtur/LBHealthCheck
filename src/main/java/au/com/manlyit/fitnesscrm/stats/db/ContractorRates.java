/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.db;

import au.com.manlyit.fitnesscrm.stats.db.Suppliers;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
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
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author david
 */
@Entity
@Table(name = "contractor_rates")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ContractorRates.findAll", query = "SELECT c FROM ContractorRates c"),
    @NamedQuery(name = "ContractorRates.findById", query = "SELECT c FROM ContractorRates c WHERE c.id = :id"),
    @NamedQuery(name = "ContractorRates.findByRate", query = "SELECT c FROM ContractorRates c WHERE c.rate = :rate"),
    @NamedQuery(name = "ContractorRates.findByBonusAmount", query = "SELECT c FROM ContractorRates c WHERE c.bonusAmount = :bonusAmount"),
    @NamedQuery(name = "ContractorRates.findByBonusInteger", query = "SELECT c FROM ContractorRates c WHERE c.bonusInteger = :bonusInteger"),
    @NamedQuery(name = "ContractorRates.findByName", query = "SELECT c FROM ContractorRates c WHERE c.name = :name")})
public class ContractorRates implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "rate")
    private BigDecimal rate;
    @Basic(optional = false)
    @NotNull
    @Column(name = "bonusAmount")
    private BigDecimal bonusAmount;
    @Basic(optional = false)
    @NotNull
    @Column(name = "bonusInteger")
    private int bonusInteger;
    @Size(max = 127)
    @Column(name = "name")
    private String name;
    @Lob
    @Size(max = 65535)
    @Column(name = "description")
    private String description;
    
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "contractorRateId")
    private Collection<ContractorRateToTaskMap> contractorRateToTaskMapCollection;

    public ContractorRates() {
    }

    public ContractorRates(Integer id) {
        this.id = id;
    }

    public ContractorRates(Integer id, BigDecimal rate, BigDecimal bonusAmount, int bonusInteger) {
        this.id = id;
        this.rate = rate;
        this.bonusAmount = bonusAmount;
        this.bonusInteger = bonusInteger;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public BigDecimal getBonusAmount() {
        return bonusAmount;
    }

    public void setBonusAmount(BigDecimal bonusAmount) {
        this.bonusAmount = bonusAmount;
    }

    public int getBonusInteger() {
        return bonusInteger;
    }

    public void setBonusInteger(int bonusInteger) {
        this.bonusInteger = bonusInteger;
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
    public Collection<ContractorRateToTaskMap> getContractorRateToTaskMapCollection() {
        return contractorRateToTaskMapCollection;
    }

    public void setContractorRateToTaskMapCollection(Collection<ContractorRateToTaskMap> contractorRateToTaskMapCollection) {
        this.contractorRateToTaskMapCollection = contractorRateToTaskMapCollection;
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
        if (!(object instanceof ContractorRates)) {
            return false;
        }
        ContractorRates other = (ContractorRates) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.beans.ContractorRates[ id=" + id + " ]";
    }
    
}
