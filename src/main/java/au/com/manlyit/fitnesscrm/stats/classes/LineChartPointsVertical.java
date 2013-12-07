/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.db.Stat;
import java.util.ArrayList;

/**
 *
 * @author david
 */
public class LineChartPointsVertical {

    private ArrayList<Stat> verticalPoints = new ArrayList<Stat>();
private String timeTaken;
    /**
     * @return the labels
     */
    public Stat getStat(int id) {
        return verticalPoints.get(id);
    }

    /**
     * @param labels the labels to set
     */
    public void addStat(Stat st) {
        verticalPoints.add(st);
    }
    /**
     * @return the timeTaken
     */
    public String getTimeTaken() {
        return timeTaken;
    }

    /**
     * @param timeTaken the timeTaken to set
     */
    public void setTimeTaken(String timeTaken) {
        this.timeTaken = timeTaken;
    }

    /**
     * @return the stats
     */
    public ArrayList<Stat> getVerticalPoints() {
        return verticalPoints;
    }
}
