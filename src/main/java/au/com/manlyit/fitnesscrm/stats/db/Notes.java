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
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
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
@Table(name = "notes")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Notes.findAll", query = "SELECT n FROM Notes n"),
    @NamedQuery(name = "Notes.findById", query = "SELECT n FROM Notes n WHERE n.id = :id"),
    @NamedQuery(name = "Notes.findByCreateTimestamp", query = "SELECT n FROM Notes n WHERE n.createTimestamp = :createTimestamp"),
    @NamedQuery(name = "Notes.findByDeleted", query = "SELECT n FROM Notes n WHERE n.deleted = :deleted")})
public class Notes implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private Integer id;
    @Lob
    @Size(max = 65535)
    @Column(name = "note")
    private String note;
    @Basic(optional = false)
    @NotNull
    @Column(name = "create_timestamp")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTimestamp;
    @Basic(optional = false)
    @NotNull
    @Column(name = "deleted")
    private short deleted;
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @ManyToOne
    private Customers userId;

    public Notes() {
    }

    public Notes(Integer id) {
        this.id = id;
    }

    public Notes(Integer id, Date createTimestamp, short deleted) {
        this.id = id;
        this.createTimestamp = createTimestamp;
        this.deleted = deleted;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Date getCreateTimestamp() {
        return createTimestamp;
    }

    public void setCreateTimestamp(Date createTimestamp) {
        this.createTimestamp = createTimestamp;
    }

    public short getDeleted() {
        return deleted;
    }

    public void setDeleted(short deleted) {
        this.deleted = deleted;
    }

    public Customers getUserId() {
        return userId;
    }

    public void setUserId(Customers userId) {
        this.userId = userId;
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
        if (!(object instanceof Notes)) {
            return false;
        }
        Notes other = (Notes) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.Notes[ id=" + id + " ]";
    }
    
}
