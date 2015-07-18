/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import au.com.manlyit.fitnesscrm.stats.db.Surveyanswers;
import au.com.manlyit.fitnesscrm.stats.db.Surveys;
import java.util.List;

/**
 *
 * @author david
 */
public class SurveyMap {
    private Surveys survey;
    private List<Surveyanswers> answers;
    
    public SurveyMap(Surveys survey,List<Surveyanswers> answers){
        this.answers = answers;
        this.survey = survey;
    }

    /**
     * @return the survey
     */
    public Surveys getSurvey() {
        return survey;
    }

    /**
     * @param survey the survey to set
     */
    public void setSurvey(Surveys survey) {
        this.survey = survey;
    }

    /**
     * @return the answers
     */
    public List<Surveyanswers> getAnswers() {
        return answers;
    }

    /**
     * @param answers the answers to set
     */
    public void setAnswers(List<Surveyanswers> answers) {
        this.answers = answers;
    }
    
}
