/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.db;

import au.com.manlyit.fitnesscrm.stats.classes.util.BaseEntity;
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
        type = CacheType.FULL, // Cache everything in memory as this object is mostly read only and will be called hundreds of time on each page.
        size = 64000, // Use 64,000 as the initial cache size.
        //expiry = 36000000, // 10 minutes // by default it never expires which is what we want for this table
        coordinationType = CacheCoordinationType.INVALIDATE_CHANGED_OBJECTS,
        databaseChangeNotificationType = DatabaseChangeNotificationType.INVALIDATE,
        isolation = CacheIsolationType.SHARED
)

@Entity
@Table(name = "configMap")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ConfigMap.findAll", query = "SELECT c FROM ConfigMap c"),
    @NamedQuery(name = "ConfigMap.findById", query = "SELECT c FROM ConfigMap c WHERE c.id = :id"),
    @NamedQuery(name = "ConfigMap.findByConfigkey", query = "SELECT c FROM ConfigMap c WHERE c.configkey = :configkey")})
public class ConfigMap implements BaseEntity, Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "configkey")
    private String configkey;
    @Basic(optional = false)
    @NotNull
    @Lob
    @Size(min = 0, max = 65535)
    @Column(name = "configvalue")
    private String configvalue;

    public ConfigMap() {
    }

    public ConfigMap(Integer id) {
        this.id = id;
    }

    public ConfigMap(Integer id, String configkey, String configvalue) {
        this.id = id;
        this.configkey = configkey;
        this.configvalue = configvalue;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getConfigkey() {
        return configkey;
    }

    public void setConfigkey(String configkey) {
        this.configkey = configkey;
    }

    public String getConfigvalue() {
        return configvalue;
    }

    public void setConfigvalue(String configvalue) {
        this.configvalue = configvalue;
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
        if (!(object instanceof ConfigMap)) {
            return false;
        }
        ConfigMap other = (ConfigMap) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return configkey;
    }

}
