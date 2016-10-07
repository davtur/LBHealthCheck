/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.db;

import au.com.manlyit.fitnesscrm.stats.db.SessionTypes;
import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author david
 */
@Entity
@Table(name = "contractor_rate_to_task_map")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ContractorRateToTaskMap.findAll", query = "SELECT c FROM ContractorRateToTaskMap c"),
    @NamedQuery(name = "ContractorRateToTaskMap.findById", query = "SELECT c FROM ContractorRateToTaskMap c WHERE c.id = :id")})
public class ContractorRateToTaskMap implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @JoinColumn(name = "contractor_rate_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private ContractorRates contractorRateId;
    @JoinColumn(name = "task_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private SessionTypes taskId;

    public ContractorRateToTaskMap() {
    }

    public ContractorRateToTaskMap(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public ContractorRates getContractorRateId() {
        return contractorRateId;
    }

    public void setContractorRateId(ContractorRates contractorRateId) {
        this.contractorRateId = contractorRateId;
    }

    public SessionTypes getTaskId() {
        return taskId;
    }

    public void setTaskId(SessionTypes taskId) {
        this.taskId = taskId;
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
        if (!(object instanceof ContractorRateToTaskMap)) {
            return false;
        }
        ContractorRateToTaskMap other = (ContractorRateToTaskMap) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.beans.ContractorRateToTaskMap[ id=" + id + " ]";
    }
    
}
