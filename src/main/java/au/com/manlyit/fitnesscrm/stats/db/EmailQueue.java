/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.db;

import au.com.manlyit.fitnesscrm.stats.classes.util.BaseEntity;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
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
@Table(name = "emailQueue")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "EmailQueue.findAll", query = "SELECT e FROM EmailQueue e"),
    @NamedQuery(name = "EmailQueue.findById", query = "SELECT e FROM EmailQueue e WHERE e.id = :id"),
    @NamedQuery(name = "EmailQueue.findByFromaddress", query = "SELECT e FROM EmailQueue e WHERE e.fromaddress = :fromaddress"),
    @NamedQuery(name = "EmailQueue.findByStatus", query = "SELECT e FROM EmailQueue e WHERE e.status = :status"),
    @NamedQuery(name = "EmailQueue.findBySendDate", query = "SELECT e FROM EmailQueue e WHERE e.sendDate = :sendDate"),
    @NamedQuery(name = "EmailQueue.findByCreateDate", query = "SELECT e FROM EmailQueue e WHERE e.createDate = :createDate")})
public class EmailQueue implements  BaseEntity, Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Lob
    @Size(min = 1, max = 65535)
    @Column(name = "toaddresses")
    private String toaddresses;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 127)
    @Column(name = "fromaddress")
    private String fromaddress;
    @Lob
    @Size(max = 65535)
    @Column(name = "ccaddresses")
    private String ccaddresses;
    @Lob
    @Size(max = 65535)
    @Column(name = "bccaddresses")
    private String bccaddresses;
    @Basic(optional = false)
    @NotNull
    @Lob
    @Size(min = 1, max = 65535)
    @Column(name = "subject")
    private String subject;
    @Basic(optional = false)
    @NotNull
    @Lob
    @Size(min = 1, max = 65535)
    @Column(name = "message")
    private String message;
    @Basic(optional = false)
    @NotNull
    @Column(name = "status")
    private int status;
    @Column(name = "sendDate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date sendDate;
    @Basic(optional = false)
    @NotNull
    @Column(name = "createDate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createDate;

    public EmailQueue() {
    }

    public EmailQueue(Integer id) {
        this.id = id;
    }

    public EmailQueue(Integer id, String toaddresses, String fromaddress, String subject, String message, int status, Date createDate) {
        this.id = id;
        this.toaddresses = toaddresses;
        this.fromaddress = fromaddress;
        this.subject = subject;
        this.message = message;
        this.status = status;
        this.createDate = createDate;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getToaddresses() {
        return toaddresses;
    }

    public void setToaddresses(String toaddresses) {
        this.toaddresses = toaddresses;
    }

    public String getFromaddress() {
        return fromaddress;
    }

    public void setFromaddress(String fromaddress) {
        this.fromaddress = fromaddress;
    }

    public String getCcaddresses() {
        return ccaddresses;
    }

    public void setCcaddresses(String ccaddresses) {
        this.ccaddresses = ccaddresses;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        if(message == null){
            message = "";
        }
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Date getSendDate() {
        return sendDate;
    }

    public void setSendDate(Date sendDate) {
        this.sendDate = sendDate;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
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
        if (!(object instanceof EmailQueue)) {
            return false;
        }
        EmailQueue other = (EmailQueue) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.EmailQueue[ id=" + id + " ]";
    }

    /**
     * @return the bccaddresses
     */
    public String getBccaddresses() {
        return bccaddresses;
    }

    /**
     * @param bccaddresses the bccaddresses to set
     */
    public void setBccaddresses(String bccaddresses) {
        this.bccaddresses = bccaddresses;
    }
    
}
