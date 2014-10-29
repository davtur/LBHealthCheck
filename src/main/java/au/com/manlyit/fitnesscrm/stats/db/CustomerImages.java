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
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import javax.faces.context.FacesContext;
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
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author david
 */
@Entity
@Table(name = "customer_images")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "CustomerImages.findAll", query = "SELECT c FROM CustomerImages c"),
    @NamedQuery(name = "CustomerImages.findById", query = "SELECT c FROM CustomerImages c WHERE c.id = :id"),
    @NamedQuery(name = "CustomerImages.findByImageType", query = "SELECT c FROM CustomerImages c WHERE c.imageType = :imageType"),
    @NamedQuery(name = "CustomerImages.findByDatetaken", query = "SELECT c FROM CustomerImages c WHERE c.datetaken = :datetaken")})
public class CustomerImages implements Serializable {
    @Lob
    @Column(name = "image")
    private byte[] image;
    @OneToOne(mappedBy = "profileImage")
    private Customers customers;
    
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "image_type")
    private int imageType;
    @Basic(optional = false)
    @NotNull
    @Column(name = "datetaken")
    @Temporal(TemporalType.TIMESTAMP)
    private Date datetaken;
    @JoinColumn(name = "customer_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Customers customerId;

    public CustomerImages() {
    }

    public CustomerImages(Integer id) {
        this.id = id;
    }

    public CustomerImages(Integer id, int imageType, Date datetaken) {
        this.id = id;
        this.imageType = imageType;
        this.datetaken = datetaken;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getImageType() {
        return imageType;
    }

    public void setImageType(int imageType) {
        this.imageType = imageType;
    }
    public String getFormattedDate(){
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy");
        String fd = sdf.format(datetaken);
        return fd;
    }
    public Date getDatetaken() {
        if(datetaken == null){
            datetaken = new Date();
        }
        return datetaken;
    }

    public void setDatetaken(Date datetaken) {
        this.datetaken = datetaken;
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


    public Customers getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Customers customerId) {
        this.customerId = customerId;
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
        if (!(object instanceof CustomerImages)) {
            return false;
        }
        CustomerImages other = (CustomerImages) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.CustomerImages[ id=" + id + " ]";
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public Customers getCustomers() {
        return customers;
    }

    public void setCustomers(Customers customers) {
        this.customers = customers;
    }
}
