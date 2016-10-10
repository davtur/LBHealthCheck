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
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author david
 */
@Entity
@Table(name = "suppliers")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Suppliers.findAll", query = "SELECT s FROM Suppliers s"),
    @NamedQuery(name = "Suppliers.findById", query = "SELECT s FROM Suppliers s WHERE s.id = :id"),
    @NamedQuery(name = "Suppliers.findBySupplierName", query = "SELECT s FROM Suppliers s WHERE s.supplierName = :supplierName"),
    @NamedQuery(name = "Suppliers.findBySupplierCompanyNumber", query = "SELECT s FROM Suppliers s WHERE s.supplierCompanyNumber = :supplierCompanyNumber"),
    @NamedQuery(name = "Suppliers.findBySupplierCompanyNumberType", query = "SELECT s FROM Suppliers s WHERE s.supplierCompanyNumberType = :supplierCompanyNumberType")})
public class Suppliers implements Serializable,BaseEntity {

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "supplierId")
    private Collection<ContractorRateToTaskMap> contractorRateToTaskMapCollection;
    

    @JoinColumn(name = "internal_contractor_id", referencedColumnName = "id")
    @ManyToOne
    private Customers internalContractorId;

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Size(max = 127)
    @Column(name = "supplier_name")
    private String supplierName;
    @Lob
    @Size(max = 65535)
    @Column(name = "description")
    private String description;
    @Size(max = 127)
    @Column(name = "supplier_company_number")
    private String supplierCompanyNumber;
    @Size(max = 127)
    @Column(name = "supplier_company_number_type")
    private String supplierCompanyNumberType;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "supplierId")
    private Collection<Expenses> expensesCollection;
    

    public Suppliers() {
    }

    public Suppliers(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSupplierCompanyNumber() {
        return supplierCompanyNumber;
    }

    public void setSupplierCompanyNumber(String supplierCompanyNumber) {
        this.supplierCompanyNumber = supplierCompanyNumber;
    }

    public String getSupplierCompanyNumberType() {
        return supplierCompanyNumberType;
    }

    public void setSupplierCompanyNumberType(String supplierCompanyNumberType) {
        this.supplierCompanyNumberType = supplierCompanyNumberType;
    }

    @XmlTransient
    public Collection<Expenses> getExpensesCollection() {
        return expensesCollection;
    }

    public void setExpensesCollection(Collection<Expenses> expensesCollection) {
        this.expensesCollection = expensesCollection;
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
        if (!(object instanceof Suppliers)) {
            return false;
        }
        Suppliers other = (Suppliers) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return supplierName;
    }

    public Customers getInternalContractorId() {
        return internalContractorId;
    }

    public void setInternalContractorId(Customers internalContractorId) {
        this.internalContractorId = internalContractorId;
    }

    @XmlTransient
    public Collection<ContractorRateToTaskMap> getContractorRateToTaskMapCollection() {
        return contractorRateToTaskMapCollection;
    }
  
    public void setContractorRateToTaskMapCollection(Collection<ContractorRateToTaskMap> contractorRateToTaskMapCollection) {
        this.contractorRateToTaskMapCollection = contractorRateToTaskMapCollection;
    }

   
    
}
