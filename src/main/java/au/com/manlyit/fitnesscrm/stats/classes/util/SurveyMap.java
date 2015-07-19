/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import au.com.manlyit.fitnesscrm.stats.db.SurveyAnswers;
import au.com.manlyit.fitnesscrm.stats.db.Surveys;
import java.util.List;

/**
 *
 * @author david
 */
public class SurveyMap {
    private Surveys survey;
    private List<SurveyAnswers> answers;
    
    public SurveyMap(Surveys survey,List<SurveyAnswers> answers){
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
    public List<SurveyAnswers> getAnswers() {
        return answers;
    }

    /**
     * @param answers the answers to set
     */
    public void setAnswers(List<SurveyAnswers> answers) {
        this.answers = answers;
    }
    
}
