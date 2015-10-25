/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.db;

import au.com.manlyit.fitnesscrm.stats.classes.util.BaseEntity;
import java.io.Serializable;
import java.util.Collection;
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
@Table(name = "session_history")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "SessionHistory.findAll", query = "SELECT s FROM SessionHistory s"),
    @NamedQuery(name = "SessionHistory.findById", query = "SELECT s FROM SessionHistory s WHERE s.id = :id"),
    @NamedQuery(name = "SessionHistory.findBySessiondate", query = "SELECT s FROM SessionHistory s WHERE s.sessiondate = :sessiondate")})
public class SessionHistory implements BaseEntity, Serializable {
    @JoinColumn(name = "session_template", referencedColumnName = "id")
    @ManyToOne
    private SessionTimetable sessionTemplate;

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
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "sessionHistoryId")
    private Collection<Participants> participantsCollection;

    @JoinColumn(name = "session_types_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private SessionTypes sessionTypesId;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "sessionHistoryId")
    private Collection<SessionTrainers> sessionTrainersCollection;

    public SessionHistory() {
    }

    public SessionHistory(Integer id) {
        this.id = id;
    }

    public SessionHistory(Integer id, Date sessiondate) {
        this.id = id;
        this.sessiondate = sessiondate;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
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

    @XmlTransient
    public Collection<Participants> getParticipantsCollection() {
        return participantsCollection;
    }

    public void setParticipantsCollection(Collection<Participants> participantsCollection) {
        this.participantsCollection = participantsCollection;
    }

    @XmlTransient
    public String getParticipantsAsString() {
        String particpants = "";
        for (Participants p : participantsCollection) {
            if (particpants.isEmpty() == false) {
                particpants += ", ";
            }
            particpants += p.getCustomerId().getFirstname() + " " + p.getCustomerId().getLastname();
        }

        return particpants;
    }

    public SessionTypes getSessionTypesId() {
        return sessionTypesId;
    }

    public void setSessionTypesId(SessionTypes sessionTypesId) {
        this.sessionTypesId = sessionTypesId;
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
        if (!(object instanceof SessionHistory)) {
            return false;
        }
        SessionHistory other = (SessionHistory) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.SessionHistory[ id=" + id + " ]";
    }

    @XmlTransient
    public Collection<SessionTrainers> getSessionTrainersCollection() {
        return sessionTrainersCollection;
    }

    public void setSessionTrainersCollection(Collection<SessionTrainers> sessionTrainersCollection) {
        this.sessionTrainersCollection = sessionTrainersCollection;
    }

    /**
     * @return the adminNotes
     */
    public String getAdminNotes() {
        return adminNotes;
    }

    /**
     * @param adminNotes the adminNotes to set
     */
    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }

    public SessionTimetable getSessionTemplate() {
        return sessionTemplate;
    }

    public void setSessionTemplate(SessionTimetable sessionTemplate) {
        this.sessionTemplate = sessionTemplate;
    }
    
    

}
