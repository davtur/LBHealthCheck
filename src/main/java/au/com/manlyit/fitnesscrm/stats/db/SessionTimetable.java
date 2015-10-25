/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.db;

import au.com.manlyit.fitnesscrm.stats.classes.util.BaseEntity;
import au.com.manlyit.fitnesscrm.stats.db.SessionTypes;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
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
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.primefaces.model.map.DefaultMapModel;
import org.primefaces.model.map.LatLng;
import org.primefaces.model.map.MapModel;
import org.primefaces.model.map.Marker;

/**
 *
 * @author david
 */
@Entity
@Table(name = "session_timetable")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "SessionTimetable.findAll", query = "SELECT s FROM SessionTimetable s"),
    @NamedQuery(name = "SessionTimetable.findById", query = "SELECT s FROM SessionTimetable s WHERE s.id = :id"),
    @NamedQuery(name = "SessionTimetable.findBySessiondate", query = "SELECT s FROM SessionTimetable s WHERE s.sessiondate = :sessiondate")})
public class SessionTimetable implements BaseEntity, Serializable {

    @OneToMany(mappedBy = "sessionTemplate")
    private Collection<SessionHistory> sessionHistoryCollection;
    @Basic(optional = false)
    @NotNull
    @Column(name = "duration_minutes")
    private int durationMinutes;
    @Basic(optional = false)
    @NotNull
    @Lob
    @Size(min = 1, max = 65535)
    @Column(name = "session_title")
    private String sessionTitle;
    @Basic(optional = false)
    @NotNull
    @Lob
    @Size(min = 1, max = 65535)
    @Column(name = "session_location_label")
    private String sessionLocationLabel;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 128)
    @Column(name = "session_location_gps")
    private String sessionLocationGps;
    @JoinColumn(name = "trainer_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Customers trainerId;
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "sessiondate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date sessiondate;
    @Lob
    @Size(max = 65535)
    @Column(name = "comments")
    private String comments;
    @Lob
    @Size(max = 65535)
    @Column(name = "admin_notes")
    private String adminNotes;
    @JoinColumn(name = "session_types_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private SessionTypes sessionTypesId;
    @JoinColumn(name = "recurrance_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private SessionRecurrance recurranceId;

    public SessionTimetable() {
    }

    public SessionTimetable(Integer id) {
        this.id = id;
    }

    public SessionTimetable(Integer id, Date sessiondate) {
        this.id = id;
        this.sessiondate = sessiondate;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getSessiondate() {
        return sessiondate;
    }

    public void setSessiondate(Date sessiondate) {
        this.sessiondate = sessiondate;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }

    public SessionTypes getSessionTypesId() {
        return sessionTypesId;
    }

    public void setSessionTypesId(SessionTypes sessionTypesId) {
        this.sessionTypesId = sessionTypesId;
    }

    public SessionRecurrance getRecurranceId() {
        return recurranceId;
    }

    public void setRecurranceId(SessionRecurrance recurranceId) {
        this.recurranceId = recurranceId;
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
        if (!(object instanceof SessionTimetable)) {
            return false;
        }
        SessionTimetable other = (SessionTimetable) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.beans.SessionTimetable[ id=" + id + " ]";
    }

    public Customers getTrainerId() {
        return trainerId;
    }

    public void setTrainerId(Customers trainerId) {
        this.trainerId = trainerId;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getSessionTitle() {
        return sessionTitle;
    }

    public void setSessionTitle(String sessionTitle) {
        this.sessionTitle = sessionTitle;
    }

    public String getSessionLocationLabel() {
        return sessionLocationLabel;
    }

    public void setSessionLocationLabel(String sessionLocationLabel) {
        this.sessionLocationLabel = sessionLocationLabel;
    }

    public String getSessionLocationGps() {
        return sessionLocationGps;
    }

    public void setSessionLocationGps(String sessionLocationGps) {
        this.sessionLocationGps = sessionLocationGps;
    }

    @XmlTransient
    public Collection<SessionHistory> getSessionHistoryCollection() {
        return sessionHistoryCollection;
    }

    public void setSessionHistoryCollection(Collection<SessionHistory> sessionHistoryCollection) {
        this.sessionHistoryCollection = sessionHistoryCollection;
    }

    public MapModel getSimpleModel() {

        MapModel simpleModel = new DefaultMapModel();
        if (sessionLocationGps != null && sessionLocationGps.trim().isEmpty() == false) {

            String[] cooards = sessionLocationGps.split(",");
            if (cooards.length == 2) {
                Double lat = new Double(cooards[0]);
                Double lng = new Double(cooards[1]);
                LatLng coord1 = new LatLng(lat, lng);
                 
                simpleModel.addOverlay(new Marker(coord1, sessionLocationLabel));
            }
        }

        return simpleModel;
    }

}
