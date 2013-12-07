/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.db;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Lob;
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
@Table(name = "QRTZ_JOB_DETAILS")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "QrtzJobDetails.findAll", query = "SELECT q FROM QrtzJobDetails q"),
    @NamedQuery(name = "QrtzJobDetails.findBySchedName", query = "SELECT q FROM QrtzJobDetails q WHERE q.qrtzJobDetailsPK.schedName = :schedName"),
    @NamedQuery(name = "QrtzJobDetails.findByJobName", query = "SELECT q FROM QrtzJobDetails q WHERE q.qrtzJobDetailsPK.jobName = :jobName"),
    @NamedQuery(name = "QrtzJobDetails.findByJobGroup", query = "SELECT q FROM QrtzJobDetails q WHERE q.qrtzJobDetailsPK.jobGroup = :jobGroup"),
    @NamedQuery(name = "QrtzJobDetails.findByDescription", query = "SELECT q FROM QrtzJobDetails q WHERE q.description = :description"),
    @NamedQuery(name = "QrtzJobDetails.findByJobClassName", query = "SELECT q FROM QrtzJobDetails q WHERE q.jobClassName = :jobClassName"),
    @NamedQuery(name = "QrtzJobDetails.findByIsDurable", query = "SELECT q FROM QrtzJobDetails q WHERE q.isDurable = :isDurable"),
    @NamedQuery(name = "QrtzJobDetails.findByIsNonconcurrent", query = "SELECT q FROM QrtzJobDetails q WHERE q.isNonconcurrent = :isNonconcurrent"),
    @NamedQuery(name = "QrtzJobDetails.findByIsUpdateData", query = "SELECT q FROM QrtzJobDetails q WHERE q.isUpdateData = :isUpdateData"),
    @NamedQuery(name = "QrtzJobDetails.findByRequestsRecovery", query = "SELECT q FROM QrtzJobDetails q WHERE q.requestsRecovery = :requestsRecovery")})
public class QrtzJobDetails implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected QrtzJobDetailsPK qrtzJobDetailsPK;
    @Size(max = 250)
    @Column(name = "DESCRIPTION")
    private String description;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 250)
    @Column(name = "JOB_CLASS_NAME")
    private String jobClassName;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 1)
    @Column(name = "IS_DURABLE")
    private String isDurable;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 1)
    @Column(name = "IS_NONCONCURRENT")
    private String isNonconcurrent;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 1)
    @Column(name = "IS_UPDATE_DATA")
    private String isUpdateData;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 1)
    @Column(name = "REQUESTS_RECOVERY")
    private String requestsRecovery;
    @Lob
    @Column(name = "JOB_DATA")
    private byte[] jobData;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "qrtzJobDetails")
    private Collection<QrtzTriggers> qrtzTriggersCollection;

    public QrtzJobDetails() {
    }

    public QrtzJobDetails(QrtzJobDetailsPK qrtzJobDetailsPK) {
        this.qrtzJobDetailsPK = qrtzJobDetailsPK;
    }

    public QrtzJobDetails(QrtzJobDetailsPK qrtzJobDetailsPK, String jobClassName, String isDurable, String isNonconcurrent, String isUpdateData, String requestsRecovery) {
        this.qrtzJobDetailsPK = qrtzJobDetailsPK;
        this.jobClassName = jobClassName;
        this.isDurable = isDurable;
        this.isNonconcurrent = isNonconcurrent;
        this.isUpdateData = isUpdateData;
        this.requestsRecovery = requestsRecovery;
    }

    public QrtzJobDetails(String schedName, String jobName, String jobGroup) {
        this.qrtzJobDetailsPK = new QrtzJobDetailsPK(schedName, jobName, jobGroup);
    }

    public QrtzJobDetailsPK getQrtzJobDetailsPK() {
        return qrtzJobDetailsPK;
    }

    public void setQrtzJobDetailsPK(QrtzJobDetailsPK qrtzJobDetailsPK) {
        this.qrtzJobDetailsPK = qrtzJobDetailsPK;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getJobClassName() {
        return jobClassName;
    }

    public void setJobClassName(String jobClassName) {
        this.jobClassName = jobClassName;
    }

    public String getIsDurable() {
        return isDurable;
    }

    public void setIsDurable(String isDurable) {
        this.isDurable = isDurable;
    }

    public String getIsNonconcurrent() {
        return isNonconcurrent;
    }

    public void setIsNonconcurrent(String isNonconcurrent) {
        this.isNonconcurrent = isNonconcurrent;
    }

    public String getIsUpdateData() {
        return isUpdateData;
    }

    public void setIsUpdateData(String isUpdateData) {
        this.isUpdateData = isUpdateData;
    }

    public String getRequestsRecovery() {
        return requestsRecovery;
    }

    public void setRequestsRecovery(String requestsRecovery) {
        this.requestsRecovery = requestsRecovery;
    }

    public byte[] getJobData() {
        return jobData;
    }

    public void setJobData(byte[] jobData) {
        this.jobData = jobData;
    }

    @XmlTransient
    public Collection<QrtzTriggers> getQrtzTriggersCollection() {
        return qrtzTriggersCollection;
    }

    public void setQrtzTriggersCollection(Collection<QrtzTriggers> qrtzTriggersCollection) {
        this.qrtzTriggersCollection = qrtzTriggersCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (qrtzJobDetailsPK != null ? qrtzJobDetailsPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof QrtzJobDetails)) {
            return false;
        }
        QrtzJobDetails other = (QrtzJobDetails) object;
        if ((this.qrtzJobDetailsPK == null && other.qrtzJobDetailsPK != null) || (this.qrtzJobDetailsPK != null && !this.qrtzJobDetailsPK.equals(other.qrtzJobDetailsPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.QrtzJobDetails[ qrtzJobDetailsPK=" + qrtzJobDetailsPK + " ]";
    }
    
}
