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
@Table(name = "leads")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Leads.findAll", query = "SELECT l FROM Leads l")
    , @NamedQuery(name = "Leads.findById", query = "SELECT l FROM Leads l WHERE l.id = :id")
    , @NamedQuery(name = "Leads.findByFirstname", query = "SELECT l FROM Leads l WHERE l.firstname = :firstname")
    , @NamedQuery(name = "Leads.findByLastname", query = "SELECT l FROM Leads l WHERE l.lastname = :lastname")
    , @NamedQuery(name = "Leads.findByEmail", query = "SELECT l FROM Leads l WHERE l.email = :email")
    , @NamedQuery(name = "Leads.findByMobile", query = "SELECT l FROM Leads l WHERE l.mobile = :mobile")
    , @NamedQuery(name = "Leads.findByCreated", query = "SELECT l FROM Leads l WHERE l.created = :created")
    , @NamedQuery(name = "Leads.findByNotes", query = "SELECT l FROM Leads l WHERE l.notes = :notes")})
public class Leads implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Size(max = 127)
    @Column(name = "firstname")
    private String firstname;
    @Size(max = 127)
    @Column(name = "lastname")
    private String lastname;
    // @Pattern(regexp="[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", message="Invalid email")//if the field contains email address consider using this annotation to enforce field validation
    @Size(max = 255)
    @Column(name = "email")
    private String email;
    @Size(max = 45)
    @Column(name = "mobile")
    private String mobile;
    @Basic(optional = false)
    @NotNull
    @Column(name = "created")
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;
    @Lob
    @Size(max = 65535)
    @Column(name = "message")
    private String message;
    @Size(max = 255)
    @Column(name = "notes")
    private String notes;

    public Leads() {
    }

    public Leads(Integer id) {
        this.id = id;
    }

    public Leads(Integer id, Date created) {
        this.id = id;
        this.created = created;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
        if (!(object instanceof Leads)) {
            return false;
        }
        Leads other = (Leads) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.Leads[ id=" + id + " ]";
    }
    
}
