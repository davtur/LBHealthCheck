/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.db;

import au.com.manlyit.fitnesscrm.stats.classes.util.BaseEntity;
import java.io.Serializable;
import java.util.Collection;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.eclipse.persistence.annotations.CacheCoordinationType;
import org.eclipse.persistence.annotations.CacheType;
import org.eclipse.persistence.annotations.DatabaseChangeNotificationType;
import org.eclipse.persistence.config.CacheIsolationType;

/**
 *
 * @author david
 */
@org.eclipse.persistence.annotations.Cache(
        type = CacheType.SOFT_WEAK,// Cache everything in memory as this object is mostly read only and will be called hundreds of time on each page.
        size = 1000, // Use 64,000 as the initial cache size.
        //expiry = 36000000, // 10 minutes // by default it never expires which is what we want for this table
        coordinationType = CacheCoordinationType.INVALIDATE_CHANGED_OBJECTS,
        databaseChangeNotificationType = DatabaseChangeNotificationType.INVALIDATE,
        isolation = CacheIsolationType.SHARED
)
@Entity
@Table(name = "tasks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Tasks.findAll", query = "SELECT t FROM Tasks t"),
    @NamedQuery(name = "Tasks.findByIdtasks", query = "SELECT t FROM Tasks t WHERE t.idtasks = :idtasks"),
    @NamedQuery(name = "Tasks.findByName", query = "SELECT t FROM Tasks t WHERE t.name = :name"),
    @NamedQuery(name = "Tasks.findByCronEntry", query = "SELECT t FROM Tasks t WHERE t.cronEntry = :cronEntry"),
    @NamedQuery(name = "Tasks.findByTaskClassName", query = "SELECT t FROM Tasks t WHERE t.taskClassName = :taskClassName")})
public class Tasks implements  BaseEntity, Serializable {
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "parentTask")
    private Collection<JobConfigMap> jobConfigMapCollection;
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @NotNull
    @Column(name = "idtasks")
    private Integer idtasks;
    @Size(max = 255)
    @Column(name = "name")
    private String name;
    @Lob
    @Size(max = 65535)
    @Column(name = "description")
    private String description;
   
    @Size(max = 255)
    @Column(name = "cronEntry")
    private String cronEntry;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "taskClassName")
    private String taskClassName;

    public Tasks() {
    }

    public Tasks(Integer idtasks) {
        this.idtasks = idtasks;
    }

    public Tasks(Integer idtasks, String taskClassName) {
        this.idtasks = idtasks;
        this.taskClassName = taskClassName;
    }

    public Integer getIdtasks() {
        return idtasks;
    }

    public void setIdtasks(Integer idtasks) {
        this.idtasks = idtasks;
    }
    @Override
     public Integer getId() {
        return idtasks;
    }

    @Override
    public void setId(Integer idtasks) {
        this.idtasks = idtasks;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCronEntry() {
        return cronEntry;
    }

    public void setCronEntry(String cronEntry) {
        this.cronEntry = cronEntry;
    }

    public String getTaskClassName() {
        return taskClassName;
    }

    public void setTaskClassName(String taskClassName) {
        this.taskClassName = taskClassName;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idtasks != null ? idtasks.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Tasks)) {
            return false;
        }
        Tasks other = (Tasks) object;
        if ((this.idtasks == null && other.idtasks != null) || (this.idtasks != null && !this.idtasks.equals(other.idtasks))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return name;
    }

    @XmlTransient
    public Collection<JobConfigMap> getJobConfigMapCollection() {
        return jobConfigMapCollection;
    }

    public void setJobConfigMapCollection(Collection<JobConfigMap> jobConfigMapCollection) {
        this.jobConfigMapCollection = jobConfigMapCollection;
    }
    
}
