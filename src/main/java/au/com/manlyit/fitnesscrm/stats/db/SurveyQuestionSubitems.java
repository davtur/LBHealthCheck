/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.db;

import au.com.manlyit.fitnesscrm.stats.classes.util.BaseEntity;
import java.io.Serializable;
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
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author david
 */
@Entity
@Table(name = "survey_question_subitems")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "SurveyQuestionSubitems.findAll", query = "SELECT s FROM SurveyQuestionSubitems s"),
    @NamedQuery(name = "SurveyQuestionSubitems.findById", query = "SELECT s FROM SurveyQuestionSubitems s WHERE s.id = :id"),
    @NamedQuery(name = "SurveyQuestionSubitems.findBySubitemBool", query = "SELECT s FROM SurveyQuestionSubitems s WHERE s.subitemBool = :subitemBool"),
    @NamedQuery(name = "SurveyQuestionSubitems.findBySubitemInt", query = "SELECT s FROM SurveyQuestionSubitems s WHERE s.subitemInt = :subitemInt")})
public class SurveyQuestionSubitems implements  BaseEntity, Serializable {
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
    @Column(name = "subitem_text")
    private String subitemText;
    @Column(name = "subitem_bool")
    private Boolean subitemBool;
    @Column(name = "subitem_int")
    private Integer subitemInt;
    @JoinColumn(name = "question_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private SurveyQuestions questionId;

    public SurveyQuestionSubitems() {
    }

    public SurveyQuestionSubitems(Integer id) {
        this.id = id;
    }

    public SurveyQuestionSubitems(Integer id, String subitemText) {
        this.id = id;
        this.subitemText = subitemText;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSubitemText() {
        return subitemText;
    }

    public void setSubitemText(String subitemText) {
        this.subitemText = subitemText;
    }

    public Boolean getSubitemBool() {
        return subitemBool;
    }

    public void setSubitemBool(Boolean subitemBool) {
        this.subitemBool = subitemBool;
    }

    public Integer getSubitemInt() {
        return subitemInt;
    }

    public void setSubitemInt(Integer subitemInt) {
        this.subitemInt = subitemInt;
    }

    public SurveyQuestions getQuestionId() {
        return questionId;
    }

    public void setQuestionId(SurveyQuestions questionId) {
        this.questionId = questionId;
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
        if (!(object instanceof SurveyQuestionSubitems)) {
            return false;
        }
        SurveyQuestionSubitems other = (SurveyQuestionSubitems) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.SurveyQuestionSubitems[ id=" + id + " ]";
    }
    
}
