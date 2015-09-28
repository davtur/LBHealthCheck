/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.classes.util;

import au.com.manlyit.fitnesscrm.stats.db.SurveyQuestionSubitems;
import java.io.Serializable;

import javax.inject.Named;

/**
 *
 * @author david
 */
@Named ("surveyquestionsubitemsConverter")

public class SurveyquestionsubitemsConverter extends GenericConverter<SurveyQuestionSubitems> implements Serializable {
    private static final long serialVersionUID = 1L;

   public SurveyquestionsubitemsConverter(){
   }
}
