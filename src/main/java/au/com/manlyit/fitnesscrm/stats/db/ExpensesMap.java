/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.db;

import au.com.manlyit.fitnesscrm.stats.classes.util.BaseEntity;
import au.com.manlyit.fitnesscrm.stats.db.ExpenseTypes;
import au.com.manlyit.fitnesscrm.stats.db.PaymentMethods;
import au.com.manlyit.fitnesscrm.stats.db.Suppliers;
import java.io.Serializable;
import javax.persistence.Basic;
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
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author david
 */
@Entity
@Table(name = "expenses_map")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ExpensesMap.findAll", query = "SELECT e FROM ExpensesMap e")
    , @NamedQuery(name = "ExpensesMap.findById", query = "SELECT e FROM ExpensesMap e WHERE e.id = :id")
    , @NamedQuery(name = "ExpensesMap.findBySearchString", query = "SELECT e FROM ExpensesMap e WHERE e.searchString = :searchString")})
public class ExpensesMap implements BaseEntity,Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 30)
    @Column(name = "search_string")
    private String searchString;
    @Lob
    @Size(max = 65535)
    @Column(name = "description")
    private String description;
    @JoinColumn(name = "payment_method_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private PaymentMethods paymentMethodId;
    @JoinColumn(name = "expense_type_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private ExpenseTypes expenseTypeId;
    @JoinColumn(name = "supplier_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Suppliers supplierId;

    public ExpensesMap() {
    }

    public ExpensesMap(Integer id) {
        this.id = id;
    }

    public ExpensesMap(Integer id, String searchString) {
        this.id = id;
        this.searchString = searchString;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PaymentMethods getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(PaymentMethods paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public ExpenseTypes getExpenseTypeId() {
        return expenseTypeId;
    }

    public void setExpenseTypeId(ExpenseTypes expenseTypeId) {
        this.expenseTypeId = expenseTypeId;
    }

    public Suppliers getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Suppliers supplierId) {
        this.supplierId = supplierId;
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
        if (!(object instanceof ExpensesMap)) {
            return false;
        }
        ExpensesMap other = (ExpensesMap) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.beans.ExpensesMap[ id=" + id + " ]";
    }
    
}
