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
@Table(name = "Survey_answers")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Surveyanswers.findAll", query = "SELECT s FROM Surveyanswers s"),
    @NamedQuery(name = "Surveyanswers.findById", query = "SELECT s FROM Surveyanswers s WHERE s.id = :id")})
public class Surveyanswers implements Serializable {
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
    @Column(name = "answer")
    private String answer;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "answerId")
    private Collection<Surveyanswersubitems> surveyanswersubitemsCollection;
    @JoinColumn(name = "survey_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Surveys surveyId;
    @JoinColumn(name = "question_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Surveyquestions questionId;
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Customers userId;
    @JoinColumn(name = "answerType_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Surveyquestiontypes answerTypeid;

    public Surveyanswers() {
    }

    public Surveyanswers(Integer id) {
        this.id = id;
    }

    public Surveyanswers(Integer id, String answer) {
        this.id = id;
        this.answer = answer;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    @XmlTransient
    public Collection<Surveyanswersubitems> getSurveyanswersubitemsCollection() {
        return surveyanswersubitemsCollection;
    }

    public void setSurveyanswersubitemsCollection(Collection<Surveyanswersubitems> surveyanswersubitemsCollection) {
        this.surveyanswersubitemsCollection = surveyanswersubitemsCollection;
    }

    public Surveys getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(Surveys surveyId) {
        this.surveyId = surveyId;
    }

    public Surveyquestions getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Surveyquestions questionId) {
        this.questionId = questionId;
    }

    public Customers getUserId() {
        return userId;
    }

    public void setUserId(Customers userId) {
        this.userId = userId;
    }

    public Surveyquestiontypes getAnswerTypeid() {
        return answerTypeid;
    }

    public void setAnswerTypeid(Surveyquestiontypes answerTypeid) {
        this.answerTypeid = answerTypeid;
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
        if (!(object instanceof Surveyanswers)) {
            return false;
        }
        Surveyanswers other = (Surveyanswers) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.Surveyanswers[ id=" + id + " ]";
    }
    
}
