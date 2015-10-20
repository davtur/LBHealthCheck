/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.SessionHistory;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 *
 * @author david
 */
public class TimetableRows implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<SessionHistory> sessions ;
    private Date startDate;
   
    public TimetableRows(Date startDate,List<SessionHistory> sessions){
      this.sessions = sessions;
      this.startDate = startDate;
      
    }

    /**
     * @return the sessions
     */
    public List<SessionHistory> getSessions() {
        return sessions;
    }

    /**
     * @param sessions the sessions to set
     */
    public void setSessions(List<SessionHistory> sessions) {
        this.sessions = sessions;
    }

    /**
     * @return the startDate
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * @param startDate the startDate to set
     */
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    
}
