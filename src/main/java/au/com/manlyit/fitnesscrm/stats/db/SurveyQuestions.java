/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.db;

import au.com.manlyit.fitnesscrm.stats.classes.util.BaseEntity;
import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
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
@Table(name = "survey_questions")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "SurveyQuestions.findAll", query = "SELECT s FROM SurveyQuestions s"),
    @NamedQuery(name = "SurveyQuestions.findById", query = "SELECT s FROM SurveyQuestions s WHERE s.id = :id")})
public class SurveyQuestions implements  BaseEntity, Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Lob
    @Size(min = 1, max = 65535)
    @Column(name = "question")
    private String question;
    @JoinColumn(name = "question_type", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private SurveyQuestionTypes questionType;
    @JoinColumn(name = "survey_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Surveys surveyId;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "questionId")
    private Collection<SurveyQuestionSubitems> surveyQuestionSubitemsCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "questionId")
    private Collection<SurveyAnswers> surveyAnswersCollection;

    public SurveyQuestions() {
    }

    public SurveyQuestions(Integer id) {
        this.id = id;
    }

    public SurveyQuestions(Integer id, String question) {
        this.id = id;
        this.question = question;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public SurveyQuestionTypes getQuestionType() {
        return questionType;
    }

    public void setQuestionType(SurveyQuestionTypes questionType) {
        this.questionType = questionType;
    }

    public Surveys getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(Surveys surveyId) {
        this.surveyId = surveyId;
    }

    @XmlTransient
    public Collection<SurveyQuestionSubitems> getSurveyQuestionSubitemsCollection() {
        return surveyQuestionSubitemsCollection;
    }

    public void setSurveyQuestionSubitemsCollection(Collection<SurveyQuestionSubitems> surveyQuestionSubitemsCollection) {
        this.surveyQuestionSubitemsCollection = surveyQuestionSubitemsCollection;
    }

    @XmlTransient
    public Collection<SurveyAnswers> getSurveyAnswersCollection() {
        return surveyAnswersCollection;
    }

    public void setSurveyAnswersCollection(Collection<SurveyAnswers> surveyAnswersCollection) {
        this.surveyAnswersCollection = surveyAnswersCollection;
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
        if (!(object instanceof SurveyQuestions)) {
            return false;
        }
        SurveyQuestions other = (SurveyQuestions) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.SurveyQuestions[ id=" + id + " ]";
    }
    
}
