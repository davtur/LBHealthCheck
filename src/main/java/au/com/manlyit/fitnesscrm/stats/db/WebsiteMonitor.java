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
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
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
@Table(name = "website_monitor")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "WebsiteMonitor.findAll", query = "SELECT w FROM WebsiteMonitor w"),
    @NamedQuery(name = "WebsiteMonitor.findById", query = "SELECT w FROM WebsiteMonitor w WHERE w.id = :id"),
    @NamedQuery(name = "WebsiteMonitor.findByTestType", query = "SELECT w FROM WebsiteMonitor w WHERE w.testType = :testType"),
    @NamedQuery(name = "WebsiteMonitor.findByResult", query = "SELECT w FROM WebsiteMonitor w WHERE w.result = :result"),
    @NamedQuery(name = "WebsiteMonitor.findByDuration", query = "SELECT w FROM WebsiteMonitor w WHERE w.duration = :duration"),
    @NamedQuery(name = "WebsiteMonitor.findByStartTime", query = "SELECT w FROM WebsiteMonitor w WHERE w.startTime = :startTime"),
    @NamedQuery(name = "WebsiteMonitor.findByNotify", query = "SELECT w FROM WebsiteMonitor w WHERE w.notify = :notify"),
    @NamedQuery(name = "WebsiteMonitor.findByJobToRunOnFail", query = "SELECT w FROM WebsiteMonitor w WHERE w.jobToRunOnFail = :jobToRunOnFail"),
    @NamedQuery(name = "WebsiteMonitor.findByDescription", query = "SELECT w FROM WebsiteMonitor w WHERE w.description = :description"),
    @NamedQuery(name = "WebsiteMonitor.findByDuration2", query = "SELECT w FROM WebsiteMonitor w WHERE w.duration2 = :duration2"),
    @NamedQuery(name = "WebsiteMonitor.findByDuration3", query = "SELECT w FROM WebsiteMonitor w WHERE w.duration3 = :duration3"),
    @NamedQuery(name = "WebsiteMonitor.findByDuration4", query = "SELECT w FROM WebsiteMonitor w WHERE w.duration4 = :duration4")})
public class WebsiteMonitor implements BaseEntity,Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "test_type")
    private int testType;
    @Basic(optional = false)
    @NotNull
    @Column(name = "result")
    private int result;
    @Basic(optional = false)
    @NotNull
    @Column(name = "duration")
    private int duration;
    @Basic(optional = false)
    @NotNull
    @Column(name = "start_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startTime;
    @Column(name = "notify")
    private Integer notify;
    @Column(name = "job_to_run_on_fail")
    private Integer jobToRunOnFail;
    @Size(max = 45)
    @Column(name = "description")
    private String description;
    @Column(name = "duration2")
    private Integer duration2;
    @Column(name = "duration3")
    private Integer duration3;
    @Column(name = "duration4")
    private Integer duration4;

    public WebsiteMonitor() {
    }

    public WebsiteMonitor(Integer id) {
        this.id = id;
    }

    public WebsiteMonitor(Integer id, int testType, int result, int duration, Date startTime) {
        this.id = id;
        this.testType = testType;
        this.result = result;
        this.duration = duration;
        this.startTime = startTime;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getTestType() {
        return testType;
    }

    public void setTestType(int testType) {
        this.testType = testType;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Integer getNotify() {
        return notify;
    }

    public void setNotify(Integer notify) {
        this.notify = notify;
    }

    public Integer getJobToRunOnFail() {
        return jobToRunOnFail;
    }

    public void setJobToRunOnFail(Integer jobToRunOnFail) {
        this.jobToRunOnFail = jobToRunOnFail;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDuration2() {
        return duration2;
    }

    public void setDuration2(Integer duration2) {
        this.duration2 = duration2;
    }

    public Integer getDuration3() {
        return duration3;
    }

    public void setDuration3(Integer duration3) {
        this.duration3 = duration3;
    }

    public Integer getDuration4() {
        return duration4;
    }

    public void setDuration4(Integer duration4) {
        this.duration4 = duration4;
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
        if (!(object instanceof WebsiteMonitor)) {
            return false;
        }
        WebsiteMonitor other = (WebsiteMonitor) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.WebsiteMonitor[ id=" + id + " ]";
    }
    
}
