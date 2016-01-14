/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import java.util.Date;
import org.primefaces.model.DefaultScheduleEvent;

public class TimetableScheduleEvent extends DefaultScheduleEvent {

    private static final long serialVersionUID = 1L;

    private boolean addReminder = false;
    private int databasePK = 0;
    private Date reminderDate;

    public TimetableScheduleEvent() {

    }

    public TimetableScheduleEvent(String title, Date start, Date end) {
        setTitle(title);
        setStartDate(start);
        setEndDate(end);

    }

    public TimetableScheduleEvent(String title, Date start, Date end, boolean allDay) {
        setTitle(title);
        setStartDate(start);
        setEndDate(end);
        setAllDay(allDay);
    }

    public TimetableScheduleEvent(String title, Date start, Date end, String styleClass, boolean editable, String description, boolean allDay, Object data) {
        setTitle(title);
        setStartDate(start);
        setEndDate(end);
        setStyleClass(styleClass);
        setEditable(editable);
        setDescription(description);
        setAllDay(allDay);
        setData(end);
    }

    public TimetableScheduleEvent(String title, Date start, Date end, Object data) {
        setTitle(title);
        setStartDate(start);
        setEndDate(end);
        setData(data);
    }

    public String getStringData() {
        if (getData() == null) {
            return "";
        } else {
            if (getData().getClass() == String.class) {
                return (String) getData();
            } else {
                return "Not a Text Value";
            }
        }
    }

    public void setStringData(String sData) {

        // byte[] ba = data.getBytes("UTF-8");
        super.setData(sData);

    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TimetableScheduleEvent other = (TimetableScheduleEvent) obj;
        if ((getTitle() == null) ? (other.getTitle() != null) : !this.getTitle().equals(other.getTitle())) {
            return false;
        }
        if (this.getStartDate() != other.getStartDate() && (this.getStartDate() == null || !this.getStartDate().equals(other.getStartDate()))) {
            return false;
        }
        if (this.getEndDate() != other.getEndDate() && (this.getEndDate() == null || !this.getEndDate().equals(other.getEndDate()))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 61 * hash + (this.getTitle() != null ? this.getTitle().hashCode() : 0);
        hash = 61 * hash + (this.getStartDate() != null ? this.getStartDate().hashCode() : 0);
        hash = 61 * hash + (this.getEndDate() != null ? this.getEndDate().hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "DefaultScheduleEvent{title=" + getTitle() + ",startDate=" + getStartDate() + ",endDate=" + getEndDate() + "}";
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
