/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.db;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author david
 */
@Entity
@Table(name = "invoice")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Invoice.findAll", query = "SELECT i FROM Invoice i"),
    @NamedQuery(name = "Invoice.findById", query = "SELECT i FROM Invoice i WHERE i.id = :id"),
    @NamedQuery(name = "Invoice.findByCreateDatetime", query = "SELECT i FROM Invoice i WHERE i.createDatetime = :createDatetime"),
    @NamedQuery(name = "Invoice.findByUserId", query = "SELECT i FROM Invoice i WHERE i.userId = :userId"),
    @NamedQuery(name = "Invoice.findByStatusId", query = "SELECT i FROM Invoice i WHERE i.statusId = :statusId"),
    @NamedQuery(name = "Invoice.findByDueDate", query = "SELECT i FROM Invoice i WHERE i.dueDate = :dueDate"),
    @NamedQuery(name = "Invoice.findByTotal", query = "SELECT i FROM Invoice i WHERE i.total = :total"),
    @NamedQuery(name = "Invoice.findByPaymentAttempts", query = "SELECT i FROM Invoice i WHERE i.paymentAttempts = :paymentAttempts"),
    @NamedQuery(name = "Invoice.findByBalance", query = "SELECT i FROM Invoice i WHERE i.balance = :balance"),
    @NamedQuery(name = "Invoice.findByCarriedBalance", query = "SELECT i FROM Invoice i WHERE i.carriedBalance = :carriedBalance"),
    @NamedQuery(name = "Invoice.findByInProcessPayment", query = "SELECT i FROM Invoice i WHERE i.inProcessPayment = :inProcessPayment"),
    @NamedQuery(name = "Invoice.findByIsReview", query = "SELECT i FROM Invoice i WHERE i.isReview = :isReview"),
    @NamedQuery(name = "Invoice.findByDeleted", query = "SELECT i FROM Invoice i WHERE i.deleted = :deleted"),
    @NamedQuery(name = "Invoice.findByCustomerNotes", query = "SELECT i FROM Invoice i WHERE i.customerNotes = :customerNotes"),
    @NamedQuery(name = "Invoice.findByPublicNumber", query = "SELECT i FROM Invoice i WHERE i.publicNumber = :publicNumber"),
    @NamedQuery(name = "Invoice.findByLastReminder", query = "SELECT i FROM Invoice i WHERE i.lastReminder = :lastReminder"),
    @NamedQuery(name = "Invoice.findByOverdueStep", query = "SELECT i FROM Invoice i WHERE i.overdueStep = :overdueStep"),
    @NamedQuery(name = "Invoice.findByCreateTimestamp", query = "SELECT i FROM Invoice i WHERE i.createTimestamp = :createTimestamp")})
public class Invoice implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "create_datetime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createDatetime;
    @Column(name = "user_id")
    private Integer userId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "status_id")
    private int statusId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "due_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dueDate;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "total")
    private BigDecimal total;
    @Basic(optional = false)
    @NotNull
    @Column(name = "payment_attempts")
    private int paymentAttempts;
    @Column(name = "balance")
    private BigDecimal balance;
    @Basic(optional = false)
    @NotNull
    @Column(name = "carried_balance")
    private BigDecimal carriedBalance;
    @Basic(optional = false)
    @NotNull
    @Column(name = "in_process_payment")
    private short inProcessPayment;
    @Basic(optional = false)
    @NotNull
    @Column(name = "is_review")
    private int isReview;
    @Basic(optional = false)
    @NotNull
    @Column(name = "deleted")
    private short deleted;
    @Size(max = 1000)
    @Column(name = "customer_notes")
    private String customerNotes;
    @Size(max = 40)
    @Column(name = "public_number")
    private String publicNumber;
    @Column(name = "last_reminder")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastReminder;
    @Column(name = "overdue_step")
    private Integer overdueStep;
    @Basic(optional = false)
    @NotNull
    @Column(name = "create_timestamp")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTimestamp;
    @OneToMany(mappedBy = "invoiceId")
    private Collection<InvoiceLine> invoiceLineCollection;
    @OneToMany(mappedBy = "delegatedInvoiceId")
    private Collection<Invoice> invoiceCollection;
    @JoinColumn(name = "delegated_invoice_id", referencedColumnName = "id")
    @ManyToOne
    private Invoice delegatedInvoiceId;
    @JoinColumn(name = "currency_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Currency currencyId;

    public Invoice() {
    }

    public Invoice(Integer id) {
        this.id = id;
    }

    public Invoice(Integer id, Date createDatetime, int statusId, Date dueDate, BigDecimal total, int paymentAttempts, BigDecimal carriedBalance, short inProcessPayment, int isReview, short deleted, Date createTimestamp) {
        this.id = id;
        this.createDatetime = createDatetime;
        this.statusId = statusId;
        this.dueDate = dueDate;
        this.total = total;
        this.paymentAttempts = paymentAttempts;
        this.carriedBalance = carriedBalance;
        this.inProcessPayment = inProcessPayment;
        this.isReview = isReview;
        this.deleted = deleted;
        this.createTimestamp = createTimestamp;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getCreateDatetime() {
        return createDatetime;
    }

    public void setCreateDatetime(Date createDatetime) {
        this.createDatetime = createDatetime;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public int getStatusId() {
        return statusId;
    }

    public void setStatusId(int statusId) {
        this.statusId = statusId;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public int getPaymentAttempts() {
        return paymentAttempts;
    }

    public void setPaymentAttempts(int paymentAttempts) {
        this.paymentAttempts = paymentAttempts;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getCarriedBalance() {
        return carriedBalance;
    }

    public void setCarriedBalance(BigDecimal carriedBalance) {
        this.carriedBalance = carriedBalance;
    }

    public short getInProcessPayment() {
        return inProcessPayment;
    }

    public void setInProcessPayment(short inProcessPayment) {
        this.inProcessPayment = inProcessPayment;
    }

    public int getIsReview() {
        return isReview;
    }

    public void setIsReview(int isReview) {
        this.isReview = isReview;
    }

    public short getDeleted() {
        return deleted;
    }

    public void setDeleted(short deleted) {
        this.deleted = deleted;
    }

    public String getCustomerNotes() {
        return customerNotes;
    }

    public void setCustomerNotes(String customerNotes) {
        this.customerNotes = customerNotes;
    }

    public String getPublicNumber() {
        return publicNumber;
    }

    public void setPublicNumber(String publicNumber) {
        this.publicNumber = publicNumber;
    }

    public Date getLastReminder() {
        return lastReminder;
    }

    public void setLastReminder(Date lastReminder) {
        this.lastReminder = lastReminder;
    }

    public Integer getOverdueStep() {
        return overdueStep;
    }

    public void setOverdueStep(Integer overdueStep) {
        this.overdueStep = overdueStep;
    }

    public Date getCreateTimestamp() {
        return createTimestamp;
    }

    public void setCreateTimestamp(Date createTimestamp) {
        this.createTimestamp = createTimestamp;
    }

    @XmlTransient
    public Collection<InvoiceLine> getInvoiceLineCollection() {
        return invoiceLineCollection;
    }

    public void setInvoiceLineCollection(Collection<InvoiceLine> invoiceLineCollection) {
        this.invoiceLineCollection = invoiceLineCollection;
    }

    @XmlTransient
    public Collection<Invoice> getInvoiceCollection() {
        return invoiceCollection;
    }

    public void setInvoiceCollection(Collection<Invoice> invoiceCollection) {
        this.invoiceCollection = invoiceCollection;
    }

    public Invoice getDelegatedInvoiceId() {
        return delegatedInvoiceId;
    }

    public void setDelegatedInvoiceId(Invoice delegatedInvoiceId) {
        this.delegatedInvoiceId = delegatedInvoiceId;
    }

    public Currency getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Currency currencyId) {
        this.currencyId = currencyId;
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
        if (!(object instanceof Invoice)) {
            return false;
        }
        Invoice other = (Invoice) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.db.Invoice[ id=" + id + " ]";
    }
    
}
