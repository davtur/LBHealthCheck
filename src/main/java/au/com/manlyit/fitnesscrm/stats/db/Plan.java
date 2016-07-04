/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.db;

import au.com.manlyit.fitnesscrm.stats.classes.util.BaseEntity;
import au.com.manlyit.fitnesscrm.stats.classes.util.PfSelectableDataModel;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
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
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author david
 */
@Entity
@Table(name = "plan")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Plan.findAll", query = "SELECT p FROM Plan p"),
    @NamedQuery(name = "Plan.findById", query = "SELECT p FROM Plan p WHERE p.id = :id"),
    @NamedQuery(name = "Plan.findByPlanName", query = "SELECT p FROM Plan p WHERE p.planName = :planName"),
    @NamedQuery(name = "Plan.findByPlanPrice", query = "SELECT p FROM Plan p WHERE p.planPrice = :planPrice"),
    @NamedQuery(name = "Plan.findByPlanActive", query = "SELECT p FROM Plan p WHERE p.planActive = :planActive"),
    @NamedQuery(name = "Plan.findByPlanDiscount", query = "SELECT p FROM Plan p WHERE p.planDiscount = :planDiscount")})
public class Plan implements BaseEntity, Serializable {

    @JoinColumn(name = "session_Type", referencedColumnName = "id")
    @ManyToOne
    private SessionTypes sessionType;

    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "plan_price")
    private BigDecimal planPrice;
    @Column(name = "plan_discount")
    private BigDecimal planDiscount;
    @OneToMany(mappedBy = "parent")
    private Collection<Plan> planCollection;
    @JoinColumn(name = "parent", referencedColumnName = "id")
    @ManyToOne
    private Plan parent;
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 128)
    @Column(name = "plan_name")
    private String planName;
    @Lob
    @Size(max = 65535)
    @Column(name = "plan_description")
    private String planDescription;
    @Basic(optional = false)
    @NotNull
    @Column(name = "plan_active")
    private short planActive;
    @OneToMany(mappedBy = "groupPricing")
    private Collection<Customers> customersCollection;
    @Size(min = 0, max = 1)
    @Column(name = "plan_time_period")
    private String planTimePeriod;

    public Plan() {
    }

    public Plan(Integer id) {
        this.id = id;
    }

    public Plan(Integer id, String planName, BigDecimal planPrice, short planActive) {
        this.id = id;
        this.planName = planName;
        this.planPrice = planPrice;
        this.planActive = planActive;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public BigDecimal getPlanPrice() {
        return planPrice;
    }

    public void setPlanPrice(BigDecimal planPrice) {
        this.planPrice = planPrice;
    }

    public String getPlanDescription() {
        return planDescription;
    }

    public void setPlanDescription(String planDescription) {
        this.planDescription = planDescription;
    }

    public short getPlanActive() {
        return planActive;
    }

    public void setPlanActive(short planActive) {
        this.planActive = planActive;
    }

    public BigDecimal getPlanDiscount() {
        return planDiscount;
    }

    public void setPlanDiscount(BigDecimal planDiscount) {
        this.planDiscount = planDiscount;
    }

    @XmlTransient
    public Collection<Customers> getCustomersCollection() {
        return customersCollection;
    }

    public void setCustomersCollection(Collection<Customers> customersCollection) {
        this.customersCollection = customersCollection;
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
        if (!(object instanceof Plan)) {
            return false;
        }
        Plan other = (Plan) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return planName;
    }

    @XmlTransient
    public PfSelectableDataModel<Plan> getPlanCollectionModel() {
        ArrayList<Plan> alp = new ArrayList<>(planCollection);
        return new PfSelectableDataModel<>(alp);
    }

    @XmlTransient
    public Collection<Plan> getPlanCollection() {
        return planCollection;
    }

    public void setPlanCollection(Collection<Plan> planCollection) {
        this.planCollection = planCollection;
    }

    public Plan getParent() {
        return parent;
    }

    public void setParent(Plan parent) {
        this.parent = parent;
    }

    public SessionTypes getSessionType() {
        return sessionType;
    }

    public void setSessionType(SessionTypes sessionType) {
        this.sessionType = sessionType;
    }

    /**
     * @return the planTimePeriod
     */
    public String getPlanTimePeriod() {
        return planTimePeriod;
    }

    /**
     * @param planTimePeriod the planTimePeriod to set
     */
    public void setPlanTimePeriod(String planTimePeriod) {
        this.planTimePeriod = planTimePeriod;
    }

}
