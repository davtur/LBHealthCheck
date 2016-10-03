/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.db;

import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import javax.faces.context.FacesContext;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
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
import org.eclipse.persistence.jpa.config.Cascade;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author david
 */
@Entity
@Table(name = "invoice_images")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "InvoiceImages.findAll", query = "SELECT i FROM InvoiceImages i"),
    @NamedQuery(name = "InvoiceImages.findById", query = "SELECT i FROM InvoiceImages i WHERE i.id = :id"),
    @NamedQuery(name = "InvoiceImages.findByCreatedTimestamp", query = "SELECT i FROM InvoiceImages i WHERE i.createdTimestamp = :createdTimestamp"),
    @NamedQuery(name = "InvoiceImages.findByExpenseId", query = "SELECT i FROM InvoiceImages i WHERE i.expenseId = :expenseId"),
    @NamedQuery(name = "InvoiceImages.findByImageType", query = "SELECT i FROM InvoiceImages i WHERE i.imageType = :imageType"),
    @NamedQuery(name = "InvoiceImages.findByMimeType", query = "SELECT i FROM InvoiceImages i WHERE i.mimeType = :mimeType"),
    @NamedQuery(name = "InvoiceImages.findByImageFileName", query = "SELECT i FROM InvoiceImages i WHERE i.imageFileName = :imageFileName")})
public class InvoiceImages implements Serializable {

    @Basic(optional = false)
    @NotNull
    @Lob
    @Column(name = "image")
    private byte[] image;

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "created_timestamp")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdTimestamp;
    @Basic(optional = false)
    @NotNull
    @Column(name = "expense_id")
    private int expenseId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "image_type")
    private int imageType;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 127)
    @Column(name = "mimeType")
    private String mimeType;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 127)
    @Column(name = "image_file_name")
    private String imageFileName;
    @Lob
    @Size(max = 65535)
    @Column(name = "image_description")
    private String imageDescription;
    @OneToMany(mappedBy = "invoiceImageId",cascade = CascadeType.PERSIST)
    private Collection<Expenses> expensesCollection;

    public InvoiceImages() {
    }

    public InvoiceImages(Integer id) {
        this.id = id;
    }

    public InvoiceImages(Integer id, Date createdTimestamp, int expenseId, int imageType, byte[] image, String mimeType, String imageFileName) {
        this.id = id;
        this.createdTimestamp = createdTimestamp;
        this.expenseId = expenseId;
        this.imageType = imageType;
        this.image = image;
        this.mimeType = mimeType;
        this.imageFileName = imageFileName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Date createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public int getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(int expenseId) {
        this.expenseId = expenseId;
    }

    public int getImageType() {
        return imageType;
    }

    public void setImageType(int imageType) {
        this.imageType = imageType;
    }


    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getImageFileName() {
        return imageFileName;
    }

    public void setImageFileName(String imageFileName) {
        this.imageFileName = imageFileName;
    }

    public String getImageDescription() {
        return imageDescription;
    }

    public void setImageDescription(String imageDescription) {
        this.imageDescription = imageDescription;
    }
    public StreamedContent getImageStream() {
        StreamedContent sc = null;
        if (getImage() == null) {
            try {
                // get default image
                InputStream stream = FacesContext.getCurrentInstance().getExternalContext().getResourceAsStream("/resources/images/Barefoot-image_100_by_100.jpg");
                sc = new DefaultStreamedContent(stream, "image/jpeg");
            } catch (Exception e) {
                JsfUtil.addErrorMessage(e, "Trying to set Barefoot-image_100_by_100.jpg as the defailt image failed!");
            }
        } else {
            String type = "image/jpeg";
            switch (getImageType()) {
                case 0:
                    type = "image/gif";
                    break;
                case 1:
                    type = "image/png";
                    break;
                case 2:
                    type = "image/jpeg";
                    break;
            }
            ByteArrayInputStream is = new ByteArrayInputStream(getImage());
            sc = new DefaultStreamedContent(is, type);
        }

        return sc;
    }

    @XmlTransient
    public Collection<Expenses> getExpensesCollection() {
        return expensesCollection;
    }

    public void setExpensesCollection(Collection<Expenses> expensesCollection) {
        this.expensesCollection = expensesCollection;
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
        if (!(object instanceof InvoiceImages)) {
            return false;
        }
        InvoiceImages other = (InvoiceImages) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.beans.InvoiceImages[ id=" + id + " ]";
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }
    
}
