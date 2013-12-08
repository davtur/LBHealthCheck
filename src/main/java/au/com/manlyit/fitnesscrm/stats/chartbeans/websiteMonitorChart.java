/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.chartbeans;

import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.WebsiteMonitor;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.primefaces.model.chart.CartesianChartModel;

/**
 *
 * @author david
 */
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.primefaces.model.chart.LineChartSeries;

@Named("websiteMonitorChart")
@RequestScoped
public class websiteMonitorChart implements Serializable {

    private ArrayList<CartesianChartModel> models;
    private SimpleDateFormat sdf = new SimpleDateFormat("HHmm");
    private static final Logger logger = Logger.getLogger(websiteMonitorChart.class.getName());

    /** Creates a new instance of MeasurementsChart */
    public websiteMonitorChart() {
    }
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.WebsiteMonitorFacade WebsiteMonitorFacade;

    @PostConstruct
    public void createChart() {
    }

    public CartesianChartModel createChart(int day, int maxSize) {
        CartesianChartModel model = new CartesianChartModel();

        GregorianCalendar latestDate = new GregorianCalendar();
        GregorianCalendar earlierDate = new GregorianCalendar();
        int hours = 24;
        /*int day = 0;
        try {
        //day = Integer.parseInt(iDay);
        } catch (NumberFormatException numberFormatException) {
        day = 0;
        logger.log(Level.INFO, "The day offset for the chart wasn't supplied, using day 0 by default.", numberFormatException);
        
        }*/
        int offsetForEarlierDate = hours * -1 * (day + 1);
        int offsetForlatestDate = hours * -1 * day;
        earlierDate.add(Calendar.HOUR, offsetForEarlierDate);
        latestDate.add(Calendar.HOUR, offsetForlatestDate);

        ArrayList<WebsiteMonitor> successfulTestResults = null;
        ArrayList<WebsiteMonitor> failedTestResults = null;
        try {
            logger.log(Level.FINE, "Day: " + day + "LatestDate: {0}, earlier date:{1}.", new Object[]{latestDate.toString(), earlierDate.toString()});

            successfulTestResults = (ArrayList<WebsiteMonitor>) WebsiteMonitorFacade.findSuccessfulTestResultsBetweenTwoDates(1, latestDate.getTime(), earlierDate.getTime());
            failedTestResults = (ArrayList<WebsiteMonitor>) WebsiteMonitorFacade.findFailedTestResultsBetweenTwoDates(1, latestDate.getTime(), earlierDate.getTime());

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "WebsiteMonitor Chart Critical Error", " database.");
        }
        // cut them down to size for a day we will get 720 points which is overkill. Take a sample of say 3 points and take the highest vale

        successfulTestResults = cutSeriesDownToAnAppropriateSize(successfulTestResults, maxSize);
        failedTestResults = cutSeriesDownToAnAppropriateSize(failedTestResults, maxSize);

        LineChartSeries responseTimeSeries = new LineChartSeries();
        LineChartSeries responseTimeSeries2 = new LineChartSeries();
        responseTimeSeries.setLabel("Successful Response Times");
        responseTimeSeries2.setLabel("Failed Tests");
        //responseTimeSeries.setStyle("monitorSeriesSuccess");
        //responseTimeSeries2.setStyle("monitorSeriesFailed");

        try {
            if (successfulTestResults != null) {
                for (WebsiteMonitor wm : successfulTestResults) {
                    String xAxixValue = sdf.format(wm.getStartTime());
                    int yVal = wm.getDuration();
                    try {
                        responseTimeSeries.set(xAxixValue, yVal);
                    } catch (Exception e) {
                        logger.log(Level.INFO, "bundle_key",e);
                    }
                }
            }


        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Website Monitor Chart Critical Error", "Couldn't set up succesful tests on the chart.");
        }

        try {
            if (failedTestResults != null) {
                for (WebsiteMonitor wm : failedTestResults) {
                    String xAxixValue = sdf.format(wm.getStartTime());
                    int yVal = wm.getDuration();
                    responseTimeSeries2.set(xAxixValue, yVal);
                }
            }

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Website Monitor Chart Critical Error", "Couldn't set up failed results on the chart.");
        }

        model = new CartesianChartModel();
        if (responseTimeSeries != null) {
            model.addSeries(responseTimeSeries);
        }
        if (failedTestResults != null) {
            model.addSeries(responseTimeSeries2);
        }
        return model;
    }

    private ArrayList<WebsiteMonitor> cutSeriesDownToAnAppropriateSize(ArrayList<WebsiteMonitor> largeArray, int smallArraySize) {
        ArrayList<WebsiteMonitor> cutdownArray = new ArrayList<WebsiteMonitor>();
        int sizeOfBigArray = largeArray.size();
        if (smallArraySize >= sizeOfBigArray) {
            return largeArray;
        }
        float step = sizeOfBigArray / smallArraySize;
        int stepInt = (int) step;
        int stepCount = 0;

        WebsiteMonitor wm = null;
        WebsiteMonitor wm2 = null;

        for (int c = 0; c < sizeOfBigArray; c++) {
            wm = largeArray.get(c);
            if (wm2 == null) {
                wm2 = wm;
            }
            if (wm.getDuration() > wm2.getDuration()) {
                wm2 = wm;
            }
            stepCount++;
            if (stepCount > stepInt) {
                cutdownArray.add(wm2);
                stepCount = 0;
                wm2 = null;
            }

        }

        return cutdownArray;
    }

    public ArrayList<CartesianChartModel> getModel() {
        if (models == null) {
            models = new ArrayList<CartesianChartModel>();
            models.add(createChart(0, 240));
            models.add(createChart(1, 240));
            models.add(createChart(2, 240));
            models.add(createChart(3, 240));
            models.add(createChart(4, 240));
            models.add(createChart(5, 240));
            models.add(createChart(6, 240));

        }
        return models;
    }
}
