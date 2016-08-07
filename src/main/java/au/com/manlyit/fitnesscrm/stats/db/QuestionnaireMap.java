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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author david
 */
@Entity
@Table(name = "questionnaire_map")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "QuestionnaireMap.findAll", query = "SELECT q FROM QuestionnaireMap q"),
    @NamedQuery(name = "QuestionnaireMap.findById", query = "SELECT q FROM QuestionnaireMap q WHERE q.id = :id"),
    @NamedQuery(name = "QuestionnaireMap.findByEnabled", query = "SELECT q FROM QuestionnaireMap q WHERE q.enabled = :enabled")})
public class QuestionnaireMap implements Serializable {

    @Basic(optional = false)
    @NotNull
    @Column(name = "questionnaire_completed")
    private boolean questionnaireCompleted;

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "enabled")
    private boolean enabled;
    @JoinColumn(name = "customer_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Customers customerId;
    @JoinColumn(name = "surveys_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Surveys surveysId;

    public QuestionnaireMap() {
    }

    public QuestionnaireMap(Integer id) {
        this.id = id;
    }

    public QuestionnaireMap(Integer id, boolean enabled) {
        this.id = id;
        this.enabled = enabled;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Customers getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Customers customerId) {
        this.customerId = customerId;
    }

    public Surveys getSurveysId() {
        return surveysId;
    }

    public void setSurveysId(Surveys surveysId) {
        this.surveysId = surveysId;
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
        if (!(object instanceof QuestionnaireMap)) {
            return false;
        }
        QuestionnaireMap other = (QuestionnaireMap) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.QuestionnaireMap[ id=" + id + " ]";
    }

    public boolean getQuestionnaireCompleted() {
        return questionnaireCompleted;
    }

    public void setQuestionnaireCompleted(boolean questionnaireCompleted) {
        this.questionnaireCompleted = questionnaireCompleted;
    }
    
}
