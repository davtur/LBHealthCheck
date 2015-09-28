/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.db;

import au.com.manlyit.fitnesscrm.stats.classes.util.BaseEntity;
import java.io.Serializable;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author david
 */
@Entity
@Table(name = "jobConfigMap")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "JobConfigMap.findAll", query = "SELECT j FROM JobConfigMap j"),
    @NamedQuery(name = "JobConfigMap.findByIdjobConfigMap", query = "SELECT j FROM JobConfigMap j WHERE j.idjobConfigMap = :idjobConfigMap"),
    @NamedQuery(name = "JobConfigMap.findByKey", query = "SELECT j FROM JobConfigMap j WHERE j.basicKey = :basicKey")})
public class JobConfigMap implements  Serializable {
    @Lob
    @Size(max = 65535)
    @Column(name = "basicValue")
    private String basicValue;
    
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "basicKey")
    private String basicKey;
    
    @JoinColumn(name = "parentTask", referencedColumnName = "idtasks")
    @ManyToOne(optional = false)
    private Tasks parentTask;
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @NotNull
    @Column(name = "idjobConfigMap")
    private Integer idjobConfigMap;
    
      @JoinColumn(name = "configMapKey", referencedColumnName = "id")
    @ManyToOne
    private ConfigMap configMapKey;

    public JobConfigMap() {
    }

    public JobConfigMap(Integer idjobConfigMap) {
        this.idjobConfigMap = idjobConfigMap;
    }

    public JobConfigMap(Integer idjobConfigMap, String key,String val,Tasks tsk) {
        this.idjobConfigMap = idjobConfigMap;
        this.basicKey = key;
        this.basicValue = val;
        this.parentTask = tsk;
    }
  public JobConfigMap(Integer idjobConfigMap, ConfigMap cm,Tasks tsk) {
        this.idjobConfigMap = idjobConfigMap;
        this.configMapKey = cm;
        this.parentTask = tsk;
   
    }
    public Integer getIdjobConfigMap() {
        return idjobConfigMap;
    }

    public void setIdjobConfigMap(Integer idjobConfigMap) {
        this.idjobConfigMap = idjobConfigMap;
    }

  
    public ConfigMap getConfigMapKey() {
        return configMapKey;
    }

    public void setConfigMapKey(ConfigMap configMapKey) {
        this.configMapKey = configMapKey;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idjobConfigMap != null ? idjobConfigMap.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof JobConfigMap)) {
            return false;
        }
        JobConfigMap other = (JobConfigMap) object;
        if ((this.idjobConfigMap == null && other.idjobConfigMap != null) || (this.idjobConfigMap != null && !this.idjobConfigMap.equals(other.idjobConfigMap))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.optusnet.mcas.JobConfigMap[ idjobConfigMap=" + idjobConfigMap + " ]";
    }

    public Tasks getParentTask() {
        return parentTask;
    }

    public void setParentTask(Tasks parentTask) {
        this.parentTask = parentTask;
    }

    public String getBasicKey() {
        return basicKey;
    }

    public void setBasicKey(String basicKey) {
        this.basicKey = basicKey;
    }

    public String getBasicValue() {
        return basicValue;
    }

    public void setBasicValue(String basicValue) {
        this.basicValue = basicValue;
    }
    
}
