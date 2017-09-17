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
@Table(name = "expense_types")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ExpenseTypes.findAll", query = "SELECT e FROM ExpenseTypes e"),
    @NamedQuery(name = "ExpenseTypes.findById", query = "SELECT e FROM ExpenseTypes e WHERE e.id = :id"),
    @NamedQuery(name = "ExpenseTypes.findByExpenseTypeName", query = "SELECT e FROM ExpenseTypes e WHERE e.expenseTypeName = :expenseTypeName")})
public class ExpenseTypes implements Serializable,BaseEntity {

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "expenseTypeId")
    private Collection<ExpensesMap> expensesMapCollection;

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Size(max = 127)
    @Column(name = "expense_type_name")
    private String expenseTypeName;
    @Lob
    @Size(max = 65535)
    @Column(name = "description")
    private String description;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "expenseTypeId")
    private Collection<Expenses> expensesCollection;

    public ExpenseTypes() {
    }

    public ExpenseTypes(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getExpenseTypeName() {
        return expenseTypeName;
    }

    public void setExpenseTypeName(String expenseTypeName) {
        this.expenseTypeName = expenseTypeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
        if (!(object instanceof ExpenseTypes)) {
            return false;
        }
        ExpenseTypes other = (ExpenseTypes) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return expenseTypeName;
    }

    @XmlTransient
    public Collection<ExpensesMap> getExpensesMapCollection() {
        return expensesMapCollection;
    }

    public void setExpensesMapCollection(Collection<ExpensesMap> expensesMapCollection) {
        this.expensesMapCollection = expensesMapCollection;
    }
    
}
