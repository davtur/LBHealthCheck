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
@Table(name = "Survey_questions")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Surveyquestions.findAll", query = "SELECT s FROM Surveyquestions s"),
    @NamedQuery(name = "Surveyquestions.findById", query = "SELECT s FROM Surveyquestions s WHERE s.id = :id")})
public class Surveyquestions implements Serializable {
    @JoinColumn(name = "survey_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Surveys surveyId;
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
    private Surveyquestiontypes questionType;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "questionId")
    private Collection<Surveyquestionsubitems> surveyquestionsubitemsCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "questionId")
    private Collection<Surveyanswers> surveyanswersCollection;

    public Surveyquestions() {
    }

    public Surveyquestions(Integer id) {
        this.id = id;
    }

    public Surveyquestions(Integer id, String question) {
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

    public Surveyquestiontypes getQuestionType() {
        return questionType;
    }

    public void setQuestionType(Surveyquestiontypes questionType) {
        this.questionType = questionType;
    }

    @XmlTransient
    public Collection<Surveyquestionsubitems> getSurveyquestionsubitemsCollection() {
        return surveyquestionsubitemsCollection;
    }

    public void setSurveyquestionsubitemsCollection(Collection<Surveyquestionsubitems> surveyquestionsubitemsCollection) {
        this.surveyquestionsubitemsCollection = surveyquestionsubitemsCollection;
    }

    @XmlTransient
    public Collection<Surveyanswers> getSurveyanswersCollection() {
        return surveyanswersCollection;
    }

    public void setSurveyanswersCollection(Collection<Surveyanswers> surveyanswersCollection) {
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
        if (!(object instanceof Surveyquestions)) {
            return false;
        }
        Surveyquestions other = (Surveyquestions) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.Surveyquestions[ id=" + id + " ]";
    }

    public Surveys getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(Surveys surveyId) {
        this.surveyId = surveyId;
    }
    
}
