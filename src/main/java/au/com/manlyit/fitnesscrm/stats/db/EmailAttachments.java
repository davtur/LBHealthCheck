/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.db;

import java.io.Serializable;
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
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import au.com.manlyit.fitnesscrm.stats.classes.util.BaseEntity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.Size;

/**
 *
 * @author david
 */
@Entity
@Table(name = "email_attachments")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "EmailAttachments.findAll", query = "SELECT e FROM EmailAttachments e")
    , @NamedQuery(name = "EmailAttachments.findById", query = "SELECT e FROM EmailAttachments e WHERE e.id = :id")})
public class EmailAttachments implements BaseEntity,Serializable {

    @Basic(optional = false)
    @NotNull
    @Lob
    @Column(name = "file")
    private byte[] file;
    
    @Size(max = 256)
    @Column(name = "file_name")
    private String fileName;
    
    @JoinColumn(name = "email_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private EmailQueue emailQueue;

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;

    public EmailAttachments() {
    }

    public EmailAttachments(Integer id) {
        this.id = id;
    }

    public EmailAttachments(Integer id, byte[] file) {
        this.id = id;
        this.file = file;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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
        if (!(object instanceof EmailAttachments)) {
            return false;
        }
        EmailAttachments other = (EmailAttachments) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.EmailAttachments[ id=" + id + " ]";
    }

    public byte[] getFile() {
        return file;
    }

    public void setFile(byte[] file) {
        this.file = file;
    }


    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public EmailQueue getEmailQueue() {
        return emailQueue;
    }

    public void setEmailQueue(EmailQueue emailQueue) {
        this.emailQueue = emailQueue;
    }
    
}
