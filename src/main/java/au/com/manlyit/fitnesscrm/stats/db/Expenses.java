/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.db;

import java.io.Serializable;
import java.math.BigDecimal;
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
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author david
 */
@Entity
@Table(name = "expenses")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Expenses.findAll", query = "SELECT e FROM Expenses e"),
    @NamedQuery(name = "Expenses.findById", query = "SELECT e FROM Expenses e WHERE e.id = :id"),
    @NamedQuery(name = "Expenses.findByCreatedTimestamp", query = "SELECT e FROM Expenses e WHERE e.createdTimestamp = :createdTimestamp"),
    @NamedQuery(name = "Expenses.findByUpdatedTimestamp", query = "SELECT e FROM Expenses e WHERE e.updatedTimestamp = :updatedTimestamp"),
    @NamedQuery(name = "Expenses.findByExpenseIncurredTimestamp", query = "SELECT e FROM Expenses e WHERE e.expenseIncurredTimestamp = :expenseIncurredTimestamp"),
    @NamedQuery(name = "Expenses.findByInvoiceNumber", query = "SELECT e FROM Expenses e WHERE e.invoiceNumber = :invoiceNumber"),
    @NamedQuery(name = "Expenses.findByReceiptNumber", query = "SELECT e FROM Expenses e WHERE e.receiptNumber = :receiptNumber"),
    @NamedQuery(name = "Expenses.findByExpenseAmount", query = "SELECT e FROM Expenses e WHERE e.expenseAmount = :expenseAmount"),
    @NamedQuery(name = "Expenses.findByBusinessUseAmount", query = "SELECT e FROM Expenses e WHERE e.businessUseAmount = :businessUseAmount"),
    @NamedQuery(name = "Expenses.findByPercentForBusinessUse", query = "SELECT e FROM Expenses e WHERE e.percentForBusinessUse = :percentForBusinessUse"),
    @NamedQuery(name = "Expenses.findByExpenseAmountGst", query = "SELECT e FROM Expenses e WHERE e.expenseAmountGst = :expenseAmountGst"),
    @NamedQuery(name = "Expenses.findByBusinessUseAmountGst", query = "SELECT e FROM Expenses e WHERE e.businessUseAmountGst = :businessUseAmountGst")})
public class Expenses implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "created_timestamp")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdTimestamp;
    @Basic(optional = false)
    @NotNull
    @Column(name = "updated_timestamp")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedTimestamp;
    @Basic(optional = false)
    @NotNull
    @Column(name = "expense_incurred_timestamp")
    @Temporal(TemporalType.TIMESTAMP)
    private Date expenseIncurredTimestamp;
    @Size(max = 70)
    @Column(name = "invoice_number")
    private String invoiceNumber;
    @Size(max = 70)
    @Column(name = "receipt_number")
    private String receiptNumber;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "expense_amount")
    private BigDecimal expenseAmount;
    @Basic(optional = false)
    @NotNull
    @Column(name = "business_use_amount")
    private BigDecimal businessUseAmount;
    @Lob
    @Size(max = 65535)
    @Column(name = "description")
    private String description;
    @Column(name = "percent_for_business_use")
    private Float percentForBusinessUse;
    @Basic(optional = false)
    @NotNull
    @Column(name = "expense_amount_gst")
    private BigDecimal expenseAmountGst;
    @Basic(optional = false)
    @NotNull
    @Column(name = "business_use_amount_gst")
    private BigDecimal businessUseAmountGst;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "expenses")
    private InvoiceImages invoiceImages;
    @JoinColumn(name = "payment_method_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private PaymentMethods paymentMethodId;
    @JoinColumn(name = "expense_type_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private ExpenseTypes expenseTypeId;
    @JoinColumn(name = "supplier_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Suppliers supplierId;

    public Expenses() {
    }

    public Expenses(Integer id) {
        this.id = id;
    }

    public Expenses(Integer id, Date createdTimestamp, Date updatedTimestamp, Date expenseIncurredTimestamp, BigDecimal expenseAmount, BigDecimal businessUseAmount, BigDecimal expenseAmountGst, BigDecimal businessUseAmountGst) {
        this.id = id;
        this.createdTimestamp = createdTimestamp;
        this.updatedTimestamp = updatedTimestamp;
        this.expenseIncurredTimestamp = expenseIncurredTimestamp;
        this.expenseAmount = expenseAmount;
        this.businessUseAmount = businessUseAmount;
        this.expenseAmountGst = expenseAmountGst;
        this.businessUseAmountGst = businessUseAmountGst;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Date createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public Date getUpdatedTimestamp() {
        return updatedTimestamp;
    }

    public void setUpdatedTimestamp(Date updatedTimestamp) {
        this.updatedTimestamp = updatedTimestamp;
    }

    public Date getExpenseIncurredTimestamp() {
        return expenseIncurredTimestamp;
    }

    public void setExpenseIncurredTimestamp(Date expenseIncurredTimestamp) {
        this.expenseIncurredTimestamp = expenseIncurredTimestamp;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public BigDecimal getExpenseAmount() {
        if(expenseAmount == null){
            expenseAmount = BigDecimal.ZERO;
        }
        return expenseAmount;
    }

    public void setExpenseAmount(BigDecimal expenseAmount) {
        this.expenseAmount = expenseAmount;
    }

    public BigDecimal getBusinessUseAmount() {
         if(businessUseAmount == null){
            businessUseAmount = BigDecimal.ZERO;
        }
        return businessUseAmount;
    }

    public void setBusinessUseAmount(BigDecimal businessUseAmount) {
        this.businessUseAmount = businessUseAmount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Float getPercentForBusinessUse() {
         if(percentForBusinessUse == null){
            percentForBusinessUse = (float)1;
        }
        return percentForBusinessUse;
    }

    public void setPercentForBusinessUse(Float percentForBusinessUse) {
        this.percentForBusinessUse = percentForBusinessUse;
    }

    public BigDecimal getExpenseAmountGst() {
         if(expenseAmountGst == null){
            expenseAmountGst = BigDecimal.ZERO;
        }
        return expenseAmountGst;
    }

    public void setExpenseAmountGst(BigDecimal expenseAmountGst) {
        this.expenseAmountGst = expenseAmountGst;
    }

    public BigDecimal getBusinessUseAmountGst() {
         if(businessUseAmountGst == null){
            businessUseAmountGst = BigDecimal.ZERO;
        }
        return businessUseAmountGst;
    }

    public void setBusinessUseAmountGst(BigDecimal businessUseAmountGst) {
        this.businessUseAmountGst = businessUseAmountGst;
    }

    public InvoiceImages getInvoiceImages() {
        return invoiceImages;
    }

    public void setInvoiceImages(InvoiceImages invoiceImages) {
        this.invoiceImages = invoiceImages;
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
        if (!(object instanceof Expenses)) {
            return false;
        }
        Expenses other = (Expenses) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.Expenses[ id=" + id + " ]";
    }
    
}
