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
@Table(name = "survey_answers")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "SurveyAnswers.findAll", query = "SELECT s FROM SurveyAnswers s"),
    @NamedQuery(name = "SurveyAnswers.findById", query = "SELECT s FROM SurveyAnswers s WHERE s.id = :id")})
public class SurveyAnswers implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    
    @Basic(optional = false)
    @NotNull
    @Lob
    @Size(min = 0, max = 65535)
    @Column(name = "answer")
    private String answer;
    
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "answerId")
    private Collection<SurveyAnswerSubitems> surveyAnswerSubitemsCollection;
    
   
    
    @JoinColumn(name = "question_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private SurveyQuestions questionId;
    
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Customers userId;
    
    @JoinColumn(name = "answerType_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private SurveyQuestionTypes answerTypeid;

    public SurveyAnswers() {
    }

    public SurveyAnswers(Integer id) {
        this.id = id;
    }

    public SurveyAnswers(Integer id, String answer) {
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
    public Collection<SurveyAnswerSubitems> getSurveyAnswerSubitemsCollection() {
        return surveyAnswerSubitemsCollection;
    }

    public void setSurveyAnswerSubitemsCollection(Collection<SurveyAnswerSubitems> surveyAnswerSubitemsCollection) {
        this.surveyAnswerSubitemsCollection = surveyAnswerSubitemsCollection;
    }

   

    public SurveyQuestions getQuestionId() {
        return questionId;
    }

    public void setQuestionId(SurveyQuestions questionId) {
        this.questionId = questionId;
    }

    public Customers getUserId() {
        return userId;
    }

    public void setUserId(Customers userId) {
        this.userId = userId;
    }

    public SurveyQuestionTypes getAnswerTypeid() {
        return answerTypeid;
    }

    public void setAnswerTypeid(SurveyQuestionTypes answerTypeid) {
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
        if (!(object instanceof SurveyAnswers)) {
            return false;
        }
        SurveyAnswers other = (SurveyAnswers) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.SurveyAnswers[ id=" + id + " ]";
    }
    
}
