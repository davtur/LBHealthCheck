/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.db;

import au.com.manlyit.fitnesscrm.stats.classes.util.BaseEntity;
import java.io.Serializable;
import java.math.BigDecimal;
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
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
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
@Table(name = "invoice_line")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "InvoiceLine.findAll", query = "SELECT i FROM InvoiceLine i"),
    @NamedQuery(name = "InvoiceLine.findById", query = "SELECT i FROM InvoiceLine i WHERE i.id = :id"),
    @NamedQuery(name = "InvoiceLine.findByAmount", query = "SELECT i FROM InvoiceLine i WHERE i.amount = :amount"),
    @NamedQuery(name = "InvoiceLine.findByQuantity", query = "SELECT i FROM InvoiceLine i WHERE i.quantity = :quantity"),
    @NamedQuery(name = "InvoiceLine.findByPrice", query = "SELECT i FROM InvoiceLine i WHERE i.price = :price"),
    @NamedQuery(name = "InvoiceLine.findByDeleted", query = "SELECT i FROM InvoiceLine i WHERE i.deleted = :deleted"),
    @NamedQuery(name = "InvoiceLine.findByDescription", query = "SELECT i FROM InvoiceLine i WHERE i.description = :description"),
    @NamedQuery(name = "InvoiceLine.findBySourceUserId", query = "SELECT i FROM InvoiceLine i WHERE i.sourceUserId = :sourceUserId"),
    @NamedQuery(name = "InvoiceLine.findByIsPercentage", query = "SELECT i FROM InvoiceLine i WHERE i.isPercentage = :isPercentage")})
public class InvoiceLine implements  BaseEntity,Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private Integer id;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "amount")
    private BigDecimal amount;
    @Column(name = "quantity")
    private BigDecimal quantity;
    @Column(name = "price")
    private BigDecimal price;
    @Basic(optional = false)
    @NotNull
    @Column(name = "deleted")
    private short deleted;
    @Size(max = 1000)
    @Column(name = "description")
    private String description;
    @Column(name = "source_user_id")
    private Integer sourceUserId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "is_percentage")
    private short isPercentage;
    @JoinColumn(name = "item_id", referencedColumnName = "id")
    @ManyToOne
    private Item itemId;
    @JoinColumn(name = "invoice_id", referencedColumnName = "id")
    @ManyToOne
    private Invoice invoiceId;
    @JoinColumn(name = "type_id", referencedColumnName = "id")
    @ManyToOne
    private InvoiceLineType typeId;

    public InvoiceLine() {
    }

    public InvoiceLine(Integer id) {
        this.id = id;
    }

    public InvoiceLine(Integer id, BigDecimal amount, short deleted, short isPercentage) {
        this.id = id;
        this.amount = amount;
        this.deleted = deleted;
        this.isPercentage = isPercentage;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public short getDeleted() {
        return deleted;
    }

    public void setDeleted(short deleted) {
        this.deleted = deleted;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSourceUserId() {
        return sourceUserId;
    }

    public void setSourceUserId(Integer sourceUserId) {
        this.sourceUserId = sourceUserId;
    }

    public short getIsPercentage() {
        return isPercentage;
    }

    public void setIsPercentage(short isPercentage) {
        this.isPercentage = isPercentage;
    }

    public Item getItemId() {
        return itemId;
    }

    public void setItemId(Item itemId) {
        this.itemId = itemId;
    }

    public Invoice getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Invoice invoiceId) {
        this.invoiceId = invoiceId;
    }

    public InvoiceLineType getTypeId() {
        return typeId;
    }

    public void setTypeId(InvoiceLineType typeId) {
        this.typeId = typeId;
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
        if (!(object instanceof InvoiceLine)) {
            return false;
        }
        InvoiceLine other = (InvoiceLine) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.db.InvoiceLine[ id=" + id + " ]";
    }
    
}
