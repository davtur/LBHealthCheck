/*
 * To change this template, choose Tools | Templates
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
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author david
 */
@Entity
@Table(name = "monitoringcharts")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Monitoringcharts.findAll", query = "SELECT m FROM Monitoringcharts m"),
    @NamedQuery(name = "Monitoringcharts.findById", query = "SELECT m FROM Monitoringcharts m WHERE m.id = :id"),
    @NamedQuery(name = "Monitoringcharts.findByType", query = "SELECT m FROM Monitoringcharts m WHERE m.type = :type"),
    @NamedQuery(name = "Monitoringcharts.findByDate", query = "SELECT m FROM Monitoringcharts m WHERE m.date = :date")})
public class Monitoringcharts implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "type")
    private int type;
    @Basic(optional = false)
    @NotNull
    @Column(name = "date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date date;
    @Basic(optional = false)
    @NotNull
    @Lob
    @Column(name = "chart")
    private byte[] chart;

    public Monitoringcharts() {
    }

    public Monitoringcharts(Integer id) {
        this.id = id;
    }

    public Monitoringcharts(Integer id, int type, Date date, byte[] chart) {
        this.id = id;
        this.type = type;
        this.date = date;
        this.chart = chart;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public byte[] getChart() {
        return chart;
    }

    public void setChart(byte[] chart) {
        this.chart = chart;
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
        if (!(object instanceof Monitoringcharts)) {
            return false;
        }
        Monitoringcharts other = (Monitoringcharts) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.Monitoringcharts[ id=" + id + " ]";
    }
    
}
