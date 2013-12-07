/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.db;

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

/**
 *
 * @author david
 */
@Entity
@Table(name = "session_history")
@NamedQueries({
    @NamedQuery(name = "SessionHistory.findAll", query = "SELECT s FROM SessionHistory s"),
    @NamedQuery(name = "SessionHistory.findById", query = "SELECT s FROM SessionHistory s WHERE s.id = :id"),
    @NamedQuery(name = "SessionHistory.findBySessiondate", query = "SELECT s FROM SessionHistory s WHERE s.sessiondate = :sessiondate")})
public class SessionHistory implements Serializable {

    @JoinColumn(name = "trainer", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Customers customers;
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @Column(name = "sessiondate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date sessiondate;
    @Lob
    @Column(name = "comments")
    private String comments;
    @JoinColumn(name = "session_types_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private SessionTypes sessionTypes;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "sessionHistory")
    private Collection<Participants> participantsCollection;

    public SessionHistory() {
    }

    public SessionHistory(Integer id) {
        this.id = id;
    }

    public SessionHistory(Integer id, Date sessiondate) {
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

    public SessionTypes getSessionTypes() {
        return sessionTypes;
    }

    public void setSessionTypes(SessionTypes sessionTypes) {
        this.sessionTypes = sessionTypes;
    }

    public Collection<Participants> getParticipantsCollection() {
        return participantsCollection;
    }

    public void setParticipantsCollection(Collection<Participants> participantsCollection) {
        this.participantsCollection = participantsCollection;
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
        return "au.com.manlyit.fitnesscrm.stats.db.SessionHistory[id=" + id + "]";
    }

    public Customers getCustomers() {
        return customers;
    }

    public void setCustomers(Customers customers) {
        this.customers = customers;
    }
}
