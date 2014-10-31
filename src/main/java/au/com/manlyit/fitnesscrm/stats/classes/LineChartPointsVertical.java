/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.db.Stat;
import java.io.Serializable;
import java.util.ArrayList;

/**
 *
 * @author david
 */
public class LineChartPointsVertical implements Serializable {

    private final ArrayList<Stat> verticalPoints = new ArrayList<>();
    private String timeTaken;

    public Stat getStat(int id) {
        return verticalPoints.get(id);
    }

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

    public ArrayList<Stat> getVerticalPoints() {
        return verticalPoints;
    }
}
