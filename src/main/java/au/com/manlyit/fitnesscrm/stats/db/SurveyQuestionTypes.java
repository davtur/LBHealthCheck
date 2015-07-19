/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.db;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
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
@Table(name = "Survey_question_types")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "SurveyQuestionTypes.findAll", query = "SELECT s FROM SurveyQuestionTypes s"),
    @NamedQuery(name = "SurveyQuestionTypes.findById", query = "SELECT s FROM SurveyQuestionTypes s WHERE s.id = :id"),
    @NamedQuery(name = "SurveyQuestionTypes.findByType", query = "SELECT s FROM SurveyQuestionTypes s WHERE s.type = :type")})
public class SurveyQuestionTypes implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "type")
    private String type;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "questionType")
    private Collection<SurveyQuestions> surveyquestionsCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "answerTypeid")
    private Collection<SurveyAnswers> surveyanswersCollection;

    public SurveyQuestionTypes() {
    }

    public SurveyQuestionTypes(Integer id) {
        this.id = id;
    }

    public SurveyQuestionTypes(Integer id, String type) {
        this.id = id;
        this.type = type;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @XmlTransient
    public Collection<SurveyQuestions> getSurveyquestionsCollection() {
        return surveyquestionsCollection;
    }

    public void setSurveyquestionsCollection(Collection<SurveyQuestions> surveyquestionsCollection) {
        this.surveyquestionsCollection = surveyquestionsCollection;
    }

    @XmlTransient
    public Collection<SurveyAnswers> getSurveyanswersCollection() {
        return surveyanswersCollection;
    }

    public void setSurveyanswersCollection(Collection<SurveyAnswers> surveyanswersCollection) {
        this.surveyanswersCollection = surveyanswersCollection;
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
        if (!(object instanceof SurveyQuestionTypes)) {
            return false;
        }
        SurveyQuestionTypes other = (SurveyQuestionTypes) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return type;
    }
    
}
