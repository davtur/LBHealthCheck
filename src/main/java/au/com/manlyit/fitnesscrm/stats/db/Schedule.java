/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.db;

import au.com.manlyit.fitnesscrm.stats.classes.util.BaseEntity;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
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
@Table(name = "schedule")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Schedule.findAll", query = "SELECT s FROM Schedule s"),
    @NamedQuery(name = "Schedule.findByIdSchedule", query = "SELECT s FROM Schedule s WHERE s.idSchedule = :idSchedule"),
    @NamedQuery(name = "Schedule.findByShedStartdate", query = "SELECT s FROM Schedule s WHERE s.shedStartdate = :shedStartdate"),
    @NamedQuery(name = "Schedule.findByShedEnddate", query = "SELECT s FROM Schedule s WHERE s.shedEnddate = :shedEnddate"),
    @NamedQuery(name = "Schedule.findByShedAllday", query = "SELECT s FROM Schedule s WHERE s.shedAllday = :shedAllday"),
    @NamedQuery(name = "Schedule.findBySchedRemindDate", query = "SELECT s FROM Schedule s WHERE s.schedRemindDate = :schedRemindDate"),
    @NamedQuery(name = "Schedule.findBySchedReminder", query = "SELECT s FROM Schedule s WHERE s.schedReminder = :schedReminder"),
    @NamedQuery(name = "Schedule.findByShedstyleClass", query = "SELECT s FROM Schedule s WHERE s.shedstyleClass = :shedstyleClass"),
    @NamedQuery(name = "Schedule.findBySchedEditable", query = "SELECT s FROM Schedule s WHERE s.schedEditable = :schedEditable")})
public class Schedule implements  BaseEntity, Serializable {

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
    
    @Column(name = "sched_remindDate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date schedRemindDate;
    
    @Column(name = "sched_reminder")
    private Boolean schedReminder;
    
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

    public Object getDataObject() {
        if (schedData == null) {
            return null;
        }
        try {
            return deserialize(schedData);
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(Schedule.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public void setDataObject(Object obj) {
        try {
            this.schedData = serialize(obj);
        } catch (IOException ex) {
            Logger.getLogger(Schedule.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Boolean getSchedEditable() {
        return schedEditable;
    }

    public void setSchedEditable(Boolean schedEditable) {
        this.schedEditable = schedEditable;
    }

    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
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

    /**
     * @return the schedRemindDate
     */
    public Date getSchedRemindDate() {
        return schedRemindDate;
    }

    /**
     * @param schedRemindDate the schedRemindDate to set
     */
    public void setSchedRemindDate(Date schedRemindDate) {
        this.schedRemindDate = schedRemindDate;
    }

    /**
     * @return the schedReminder
     */
    public Boolean getSchedReminder() {
        return schedReminder;
    }

    /**
     * @param schedReminder the schedReminder to set
     */
    public void setSchedReminder(Boolean schedReminder) {
        this.schedReminder = schedReminder;
    }

}
