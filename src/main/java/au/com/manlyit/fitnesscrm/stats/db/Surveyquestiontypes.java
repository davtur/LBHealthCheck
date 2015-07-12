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
    @NamedQuery(name = "Surveyquestiontypes.findAll", query = "SELECT s FROM Surveyquestiontypes s"),
    @NamedQuery(name = "Surveyquestiontypes.findById", query = "SELECT s FROM Surveyquestiontypes s WHERE s.id = :id"),
    @NamedQuery(name = "Surveyquestiontypes.findByType", query = "SELECT s FROM Surveyquestiontypes s WHERE s.type = :type")})
public class Surveyquestiontypes implements Serializable {
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
    private Collection<Surveyquestions> surveyquestionsCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "answerTypeid")
    private Collection<Surveyanswers> surveyanswersCollection;

    public Surveyquestiontypes() {
    }

    public Surveyquestiontypes(Integer id) {
        this.id = id;
    }

    public Surveyquestiontypes(Integer id, String type) {
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
    public Collection<Surveyquestions> getSurveyquestionsCollection() {
        return surveyquestionsCollection;
    }

    public void setSurveyquestionsCollection(Collection<Surveyquestions> surveyquestionsCollection) {
        this.surveyquestionsCollection = surveyquestionsCollection;
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
        if (!(object instanceof Surveyquestiontypes)) {
            return false;
        }
        Surveyquestiontypes other = (Surveyquestiontypes) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.Surveyquestiontypes[ id=" + id + " ]";
    }
    
}
