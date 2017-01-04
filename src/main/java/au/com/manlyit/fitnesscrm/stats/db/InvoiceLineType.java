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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
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
@Table(name = "invoice_line_type")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "InvoiceLineType.findAll", query = "SELECT i FROM InvoiceLineType i"),
    @NamedQuery(name = "InvoiceLineType.findById", query = "SELECT i FROM InvoiceLineType i WHERE i.id = :id"),
    @NamedQuery(name = "InvoiceLineType.findByDescription", query = "SELECT i FROM InvoiceLineType i WHERE i.description = :description"),
    @NamedQuery(name = "InvoiceLineType.findByOrderPosition", query = "SELECT i FROM InvoiceLineType i WHERE i.orderPosition = :orderPosition")})
public class InvoiceLineType implements  BaseEntity,Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "description")
    private String description;
    @Basic(optional = false)
    @NotNull
    @Column(name = "order_position")
    private int orderPosition;
    @OneToMany(mappedBy = "typeId")
    private Collection<InvoiceLine> invoiceLineCollection;

    public InvoiceLineType() {
    }

    public InvoiceLineType(Integer id) {
        this.id = id;
    }

    public InvoiceLineType(Integer id, String description, int orderPosition) {
        this.id = id;
        this.description = description;
        this.orderPosition = orderPosition;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getOrderPosition() {
        return orderPosition;
    }

    public void setOrderPosition(int orderPosition) {
        this.orderPosition = orderPosition;
    }

    @XmlTransient
    public Collection<InvoiceLine> getInvoiceLineCollection() {
        return invoiceLineCollection;
    }

    public void setInvoiceLineCollection(Collection<InvoiceLine> invoiceLineCollection) {
        this.invoiceLineCollection = invoiceLineCollection;
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
        if (!(object instanceof InvoiceLineType)) {
            return false;
        }
        InvoiceLineType other = (InvoiceLineType) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.db.InvoiceLineType[ id=" + id + " ]";
    }
    
}
