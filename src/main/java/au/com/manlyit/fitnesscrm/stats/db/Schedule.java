/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.db;

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

/**
 *
 * @author david
 */
@Entity
@Table(name = "schedule")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Schedule.findAll", query = "SELECT s FROM Schedule s"),
    @NamedQuery(name = "Schedule.findByIdSchedule", query = "SELECT s FROM Schedule s WHERE s.idSchedule = :idSchedule"),
    @NamedQuery(name = "Schedule.findByShedStartdate", query = "SELECT s FROM Schedule s WHERE s.shedStartdate = :shedStartdate"),
    @NamedQuery(name = "Schedule.findByShedEnddate", query = "SELECT s FROM Schedule s WHERE s.shedEnddate = :shedEnddate"),
    @NamedQuery(name = "Schedule.findByShedAllday", query = "SELECT s FROM Schedule s WHERE s.shedAllday = :shedAllday"),
    @NamedQuery(name = "Schedule.findByShedstyleClass", query = "SELECT s FROM Schedule s WHERE s.shedstyleClass = :shedstyleClass"),
    @NamedQuery(name = "Schedule.findBySchedEditable", query = "SELECT s FROM Schedule s WHERE s.schedEditable = :schedEditable")})
public class Schedule implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id_schedule")
    private Integer idSchedule;
    @Basic(optional = false)
    @NotNull
    @Lob
    @Size(min = 1, max = 65535)
    @Column(name = "shed_title")
    private String shedTitle;
    @Basic(optional = false)
    @NotNull
    @Column(name = "shed_startdate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date shedStartdate;
    @Basic(optional = false)
    @NotNull
    @Column(name = "shed_enddate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date shedEnddate;
    @Column(name = "shed_allday")
    private Boolean shedAllday;
    @Size(max = 255)
    @Column(name = "shed_styleClass")
    private String shedstyleClass;
    @Lob
    @Column(name = "sched_data")
    private byte[] schedData;
    @Column(name = "sched_editable")
    private Boolean schedEditable;

    public Schedule() {
    }

    public Schedule(Integer idSchedule) {
        this.idSchedule = idSchedule;
    }

    public Schedule(Integer idSchedule, String shedTitle, Date shedStartdate, Date shedEnddate) {
        this.idSchedule = idSchedule;
        this.shedTitle = shedTitle;
        this.shedStartdate = shedStartdate;
        this.shedEnddate = shedEnddate;
    }

    public Integer getId() {
        return idSchedule;
    }

    public void setId(Integer idSchedule) {
        this.idSchedule = idSchedule;
    }

    public String getShedTitle() {
        return shedTitle;
    }

    public void setShedTitle(String shedTitle) {
        this.shedTitle = shedTitle;
    }

    public Date getShedStartdate() {
        return shedStartdate;
    }

    public void setShedStartdate(Date shedStartdate) {
        this.shedStartdate = shedStartdate;
    }

    public Date getShedEnddate() {
        return shedEnddate;
    }

    public void setShedEnddate(Date shedEnddate) {
        this.shedEnddate = shedEnddate;
    }

    public Boolean getShedAllday() {
        return shedAllday;
    }

    public void setShedAllday(Boolean shedAllday) {
        this.shedAllday = shedAllday;
    }

    public String getShedstyleClass() {
        return shedstyleClass;
    }

    public void setShedstyleClass(String shedstyleClass) {
        this.shedstyleClass = shedstyleClass;
    }

    public byte[] getSchedData() {
        return schedData;
    }

    public void setSchedData(byte[] schedData) {
        this.schedData = schedData;
    }

    public Boolean getSchedEditable() {
        return schedEditable;
    }

    public void setSchedEditable(Boolean schedEditable) {
        this.schedEditable = schedEditable;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idSchedule != null ? idSchedule.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Schedule)) {
            return false;
        }
        Schedule other = (Schedule) object;
        if ((this.idSchedule == null && other.idSchedule != null) || (this.idSchedule != null && !this.idSchedule.equals(other.idSchedule))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.Schedule[ idSchedule=" + idSchedule + " ]";
    }
    
}
