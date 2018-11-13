/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import java.util.Calendar;

/**
 *
 * @author david
 */
public class CalendarUtil {
    
    
     //dayOfWeekToSet is a constant from the Calendar class
    //c is the calendar instance
    public static void SetToNextDayOfWeek(int dayOfWeekToSet, Calendar c) {
        int currentDayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        //add 1 day to the current day until we get to the day we want
        while (currentDayOfWeek != dayOfWeekToSet) {
            c.add(Calendar.DAY_OF_WEEK, 1);
            currentDayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        }
    }

    public static void SetToLastDayOfWeek(int dayOfWeekToSet, Calendar c) {
        int currentDayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        //add 1 day to the current day until we get to the day we want
        while (currentDayOfWeek != dayOfWeekToSet) {
            c.add(Calendar.DAY_OF_WEEK, -1);
            currentDayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        }
    }

    public static void SetTimeToMidnight(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.set(Calendar.SECOND, 0);
    }

}
