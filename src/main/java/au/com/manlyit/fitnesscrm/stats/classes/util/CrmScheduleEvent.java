/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.primefaces.model.DefaultScheduleEvent;

public class CrmScheduleEvent extends DefaultScheduleEvent {

    private String id;
    private String title;
    private Date startDate;
    private Date endDate;
    private Date reminderDate ;
    private boolean allDay = false;
    private String styleClass;
    private Object data;
    private boolean editable = true;
    private boolean addReminder = false;
    private int databasePK = 0;

    public CrmScheduleEvent() {
        
    }

    public CrmScheduleEvent(String title, Date start, Date end) {
        this.title = title;
        this.startDate = start;
        this.endDate = end;
    }

    public CrmScheduleEvent(String title, Date start, Date end, boolean allDay) {
        this.title = title;
        this.startDate = start;
        this.endDate = end;
        this.allDay = allDay;
    }

    public CrmScheduleEvent(String title, Date start, Date end, String styleClass) {
        this.title = title;
        this.startDate = start;
        this.endDate = end;
        this.styleClass = styleClass;
    }

    public CrmScheduleEvent(String title, Date start, Date end, Object data) {
        this.title = title;
        this.startDate = start;
        this.endDate = end;
        this.data = data;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public Date getStartDate() {
        return startDate;
    }

    @Override
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @Override
    public Date getEndDate() {
        return endDate;
    }

    @Override
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    @Override
    public boolean isAllDay() {
        return allDay;
    }

    @Override
    public void setAllDay(boolean allDay) {
        this.allDay = allDay;
    }

    @Override
    public void setStyleClass(String styleClass) {
        this.styleClass = styleClass;
    }

    @Override
    public String getStyleClass() {
        return styleClass;
    }

    @Override
    public Object getData() {
        return data;
    }

    @Override
    public void setData(Object data) {
        this.data = data;
    }

    public String getStringData() {
        if (data == null) {
            return "";
        } else {           
            if (data.getClass() == String.class) {
                return (String) data;
            } else {
                return "Not a Text Value";
            }
        }
    }

    public void setStringData(String sData) {
   
            // byte[] ba = data.getBytes("UTF-8");
            this.data = sData;
       
    }

    @Override
    public boolean isEditable() {
        return editable;
    }

    @Override
    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CrmScheduleEvent other = (CrmScheduleEvent) obj;
        if ((this.title == null) ? (other.title != null) : !this.title.equals(other.title)) {
            return false;
        }
        if (this.startDate != other.startDate && (this.startDate == null || !this.startDate.equals(other.startDate))) {
            return false;
        }
        if (this.endDate != other.endDate && (this.endDate == null || !this.endDate.equals(other.endDate))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 61 * hash + (this.title != null ? this.title.hashCode() : 0);
        hash = 61 * hash + (this.startDate != null ? this.startDate.hashCode() : 0);
        hash = 61 * hash + (this.endDate != null ? this.endDate.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "DefaultScheduleEvent{title=" + title + ",startDate=" + startDate + ",endDate=" + endDate + "}";
    }

    /**
     * @return the databasePK
     */
    public int getDatabasePK() {
        return databasePK;
    }

    /**
     * @param databasePK the databasePK to set
     */
    public void setDatabasePK(int databasePK) {
        this.databasePK = databasePK;
    }

    /**
     * @return the addReminder
     */
    public boolean isAddReminder() {
        return addReminder;
    }

    /**
     * @param addReminder the addReminder to set
     */
    public void setAddReminder(boolean addReminder) {
        this.addReminder = addReminder;
    }

    /**
     * @return the reminderDate
     */
    public Date getReminderDate() {
        return reminderDate;
    }

    /**
     * @param reminderDate the reminderDate to set
     */
    public void setReminderDate(Date reminderDate) {
        this.reminderDate = reminderDate;
    }
}
