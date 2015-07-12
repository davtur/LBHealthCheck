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
@Table(name = "Survey_answer_subitems")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Surveyanswersubitems.findAll", query = "SELECT s FROM Surveyanswersubitems s"),
    @NamedQuery(name = "Surveyanswersubitems.findById", query = "SELECT s FROM Surveyanswersubitems s WHERE s.id = :id")})
public class Surveyanswersubitems implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Lob
    @Size(min = 0, max = 65535)
    @Column(name = "subitem_text")
    private String subitemText;
    @Column(name = "subitem_bool")
    private Boolean subitemBool;
    @Column(name = "subitem_int")
    private Integer subitemInt;
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    
    @JoinColumn(name = "answer_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Surveyanswers answerId;

    public Surveyanswersubitems() {
    }

    public Surveyanswersubitems(Integer id) {
        this.id = id;
    }

    public Surveyanswersubitems(Integer id, String subitem) {
        this.id = id;
        this.subitemText = subitem;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

   

    public Surveyanswers getAnswerId() {
        return answerId;
    }

    public void setAnswerId(Surveyanswers answerId) {
        this.answerId = answerId;
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
        if (!(object instanceof Surveyanswersubitems)) {
            return false;
        }
        Surveyanswersubitems other = (Surveyanswersubitems) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "au.com.manlyit.fitnesscrm.stats.db.Surveyanswersubitems[ id=" + id + " ]";
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
    
}
