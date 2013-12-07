/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.db;

import java.io.Serializable;
import java.math.BigInteger;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author david
 */
@Entity
@Table(name = "QRTZ_TRIGGERS")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "QrtzTriggers.findAll", query = "SELECT q FROM QrtzTriggers q"),
    @NamedQuery(name = "QrtzTriggers.findBySchedName", query = "SELECT q FROM QrtzTriggers q WHERE q.qrtzTriggersPK.schedName = :schedName"),
    @NamedQuery(name = "QrtzTriggers.findByTriggerName", query = "SELECT q FROM QrtzTriggers q WHERE q.qrtzTriggersPK.triggerName = :triggerName"),
    @NamedQuery(name = "QrtzTriggers.findByTriggerGroup", query = "SELECT q FROM QrtzTriggers q WHERE q.qrtzTriggersPK.triggerGroup = :triggerGroup"),
    @NamedQuery(name = "QrtzTriggers.findByDescription", query = "SELECT q FROM QrtzTriggers q WHERE q.description = :description"),
    @NamedQuery(name = "QrtzTriggers.findByNextFireTime", query = "SELECT q FROM QrtzTriggers q WHERE q.nextFireTime = :nextFireTime"),
    @NamedQuery(name = "QrtzTriggers.findByPrevFireTime", query = "SELECT q FROM QrtzTriggers q WHERE q.prevFireTime = :prevFireTime"),
    @NamedQuery(name = "QrtzTriggers.findByPriority", query = "SELECT q FROM QrtzTriggers q WHERE q.priority = :priority"),
    @NamedQuery(name = "QrtzTriggers.findByTriggerState", query = "SELECT q FROM QrtzTriggers q WHERE q.triggerState = :triggerState"),
    @NamedQuery(name = "QrtzTriggers.findByTriggerType", query = "SELECT q FROM QrtzTriggers q WHERE q.triggerType = :triggerType"),
    @NamedQuery(name = "QrtzTriggers.findByStartTime", query = "SELECT q FROM QrtzTriggers q WHERE q.startTime = :startTime"),
    @NamedQuery(name = "QrtzTriggers.findByEndTime", query = "SELECT q FROM QrtzTriggers q WHERE q.endTime = :endTime"),
    @NamedQuery(name = "QrtzTriggers.findByCalendarName", query = "SELECT q FROM QrtzTriggers q WHERE q.calendarName = :calendarName"),
    @NamedQuery(name = "QrtzTriggers.findByMisfireInstr", query = "SELECT q FROM QrtzTriggers q WHERE q.misfireInstr = :misfireInstr")})
public class QrtzTriggers implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected QrtzTriggersPK qrtzTriggersPK;
    @Size(max = 250)
    @Column(name = "DESCRIPTION")
    private String description;
    @Column(name = "NEXT_FIRE_TIME")
    private BigInteger nextFireTime;
    @Column(name = "PREV_FIRE_TIME")
    private BigInteger prevFireTime;
    @Column(name = "PRIORITY")
    private Integer priority;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 16)
    @Column(name = "TRIGGER_STATE")
    private String triggerState;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 8)
    @Column(name = "TRIGGER_TYPE")
    private String triggerType;
    @Basic(optional = false)
    @NotNull
    @Column(name = "START_TIME")
    private long startTime;
    @Column(name = "END_TIME")
    private BigInteger endTime;
    @Size(max = 200)
    @Column(name = "CALENDAR_NAME")
    private String calendarName;
    @Column(name = "MISFIRE_INSTR")
    private Short misfireInstr;
    @Lob
    @Column(name = "JOB_DATA")
    private byte[] jobData;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "qrtzTriggers")
    private QrtzSimpleTriggers qrtzSimpleTriggers;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "qrtzTriggers")
    private QrtzCronTriggers qrtzCronTriggers;
    @JoinColumns({
        @JoinColumn(name = "SCHED_NAME", referencedColumnName = "SCHED_NAME", insertable = false, updatable = false),
        @JoinColumn(name = "JOB_NAME", referencedColumnName = "JOB_NAME"),
        @JoinColumn(name = "JOB_GROUP", referencedColumnName = "JOB_GROUP")})
    @ManyToOne(optional = false)
    private QrtzJobDetails qrtzJobDetails;

    public QrtzTriggers() {
    }

    public QrtzTriggers(QrtzTriggersPK qrtzTriggersPK) {
        this.qrtzTriggersPK = qrtzTriggersPK;
    }

    public QrtzTriggers(QrtzTriggersPK qrtzTriggersPK, String triggerState, String triggerType, long startTime) {
        this.qrtzTriggersPK = qrtzTriggersPK;
        this.triggerState = triggerState;
        this.triggerType = triggerType;
        this.startTime = startTime;
    }

    public QrtzTriggers(String schedName, String triggerName, String triggerGroup) {
        this.qrtzTriggersPK = new QrtzTriggersPK(schedName, triggerName, triggerGroup);
    }

    public QrtzTriggersPK getQrtzTriggersPK() {
        return qrtzTriggersPK;
    }

    public void setQrtzTriggersPK(QrtzTriggersPK qrtzTriggersPK) {
        this.qrtzTriggersPK = qrtzTriggersPK;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigInteger getNextFireTime() {
        return nextFireTime;
    }

    public void setNextFireTime(BigInteger nextFireTime) {
        this.nextFireTime = nextFireTime;
    }

    public BigInteger getPrevFireTime() {
        return prevFireTime;
    }

    public void setPrevFireTime(BigInteger prevFireTime) {
        this.prevFireTime = prevFireTime;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getTriggerState() {
        return triggerState;
    }

    public void setTriggerState(String triggerState) {
        this.triggerState = triggerState;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public BigInteger getEndTime() {
        return endTime;
    }

    public void setEndTime(BigInteger endTime) {
        this.endTime = endTime;
    }

    public String getCalendarName() {
        return calendarName;
    }

    public void setCalendarName(String calendarName) {
        this.calendarName = calendarName;
    }

    public Short getMisfireInstr() {
        return misfireInstr;
    }

    public void setMisfireInstr(Short misfireInstr) {
        this.misfireInstr = misfireInstr;
    }

    public byte[] getJobData() {
        return jobData;
    }

    public void setJobData(byte[] jobData) {
        this.jobData = jobData;
    }

    public QrtzSimpleTriggers getQrtzSimpleTriggers() {
        return qrtzSimpleTriggers;
    }

    public void setQrtzSimpleTriggers(QrtzSimpleTriggers qrtzSimpleTriggers) {
        this.qrtzSimpleTriggers = qrtzSimpleTriggers;
    }

    public QrtzCronTriggers getQrtzCronTriggers() {
        return qrtzCronTriggers;
    }

    public void setQrtzCronTriggers(QrtzCronTriggers qrtzCronTriggers) {
        this.qrtzCronTriggers = qrtzCronTriggers;
    }

    public QrtzJobDetails getQrtzJobDetails() {
        return qrtzJobDetails;
    }

    public void setQrtzJobDetails(QrtzJobDetails qrtzJobDetails) {
        this.qrtzJobDetails = qrtzJobDetails;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (qrtzTriggersPK != null ? qrtzTriggersPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof QrtzTriggers)) {
            return false;
        }
        QrtzTriggers other = (QrtzTriggers) object;
        if ((this.qrtzTriggersPK == null && other.qrtzTriggersPK != null) || (this.qrtzTriggersPK != null && !this.qrtzTriggersPK.equals(other.qrtzTriggersPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.QrtzTriggers[ qrtzTriggersPK=" + qrtzTriggersPK + " ]";
    }
    
}
