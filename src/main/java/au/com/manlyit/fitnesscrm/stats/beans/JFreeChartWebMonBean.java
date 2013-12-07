/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.chartbeans.JfcTimeSeriesChart1;
import au.com.manlyit.fitnesscrm.stats.chartbeans.websiteMonitorChart;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.Monitoringcharts;
import au.com.manlyit.fitnesscrm.stats.db.WebsiteMonitor;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.SessionScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.event.ActionEvent;
import javax.imageio.ImageIO;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author david
 */
@ManagedBean(name = "jFreeChartWebMonBean")
@SessionScoped
public class JFreeChartWebMonBean implements Serializable {

    private static final Logger logger = Logger.getLogger(websiteMonitorChart.class.getName());
    private SimpleDateFormat sdf2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private SimpleDateFormat sdf4 = new SimpleDateFormat("HH:mm:ss");
    private SimpleDateFormat sdf3 = new SimpleDateFormat("EEEE, d MMM yyyy");
    private static int DAYS_TO_DISPLAY = 7;
    private String lastUpdateTime = "Last Updated: " + sdf4.format(new Date());
    private ArrayList<Date> latestChartDates = new ArrayList<Date>();
    private byte[] chart0;
    private byte[] chart1;
    private byte[] chart2;
    private byte[] chart3;
    private byte[] chart4;
    private byte[] chart5;
    private byte[] chart6;

    /** Creates a new instance of MeasurementsChart */
    public JFreeChartWebMonBean() {
        GregorianCalendar cal = new GregorianCalendar();
        for (int j = 0; j < DAYS_TO_DISPLAY; j++) {
            latestChartDates.add(cal.getTime());
            cal.add(Calendar.HOUR_OF_DAY, -24);
        }

    }
    @EJB
    private au.com.manlyit.fitnesscrm.stats.beans.MonitoringchartsFacade monitoringchartsFacadeFacade;
    @EJB
    private au.com.manlyit.fitnesscrm.stats.beans.WebsiteMonitorFacade WebsiteMonitorFacade;

    @PostConstruct
    public void createChart() {
    }

    public void chartDateChangeListener(SelectEvent event) {
        Date date = (Date)event.getObject();
        String id = event.getComponent().getId();
        if (latestChartDates.size() > 0) {
            latestChartDates.set(0, date);

        } else {
            latestChartDates.add(date);
        }
        chart0 = null;
        // }
        String msg = "New Date is " + sdf3.format(date);
        JsfUtil.addSuccessMessage("Chart Date Changed", msg);
    }

    public StreamedContent getLatestchart(int latestDate) {
        ArrayList<Date> ald = getLatestChartDates();
        Date d = ald.get(latestDate);
        return convertChartToStreamedContent(getChartFromDB(d));
    }

    private StreamedContent convertChartToStreamedContent(byte[] image) {
        StreamedContent chart = null;
        ByteArrayInputStream is = null;
        if (image != null) {

            is = new ByteArrayInputStream(image);
            chart = new DefaultStreamedContent(is, "image/png");

        } else {
            logger.log(Level.FINE, "!!! Chart content is null !!!");
        }
        return chart;

    }

    public byte[] getChartFromDB(Date chartDate) {
        byte[] ba = null;
        GregorianCalendar startDate = new GregorianCalendar();
        startDate.setTime(chartDate);
        GregorianCalendar endDate = new GregorianCalendar();


        startDate.set(Calendar.HOUR_OF_DAY, 0);
        startDate.set(Calendar.MINUTE, 0);
        startDate.set(Calendar.SECOND, 0);
        startDate.set(Calendar.MILLISECOND, 0);
        endDate.setTime(startDate.getTime());
        endDate.add(Calendar.HOUR_OF_DAY, 24);
        endDate.add(Calendar.MILLISECOND, -1);


        List<Monitoringcharts> charts = null;
        try {
            charts = monitoringchartsFacadeFacade.findChartForDate(startDate.getTime(), endDate.getTime(), true);
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "WebsiteMonitor Chart Critical Error", " database.");
        }
        if (charts != null) {
            try {
                int count = 0;
                for (Monitoringcharts mc : charts) {
                    ba = mc.getChart();
                    if (ba != null) {
                        setLastUpdateTime("Last Updated: " + sdf4.format(mc.getDate()));
                        count++;
                    } else {
                        logger.log(Level.FINE, "!!! Chart content is null !!!{0}", mc.getId());
                    }
                }
                if (count > 1) {
                    logger.log(Level.WARNING, "Had an issue coverting the byte array to a Streamed Content: Too many charts returned={0}", count);
                }
                if (count == 0) {
                    logger.log(Level.WARNING, "Had an issue coverting the byte array to a Streamed Content: No charts found for date:{0}", sdf3.format(chartDate.getTime()));

                }

            } catch (Exception e) {


                logger.log(Level.WARNING, "Had an issue coverting the byte array to a Streamed Content", e);
            }
        }
        return ba;
    }

    private byte[] getChart(int j) {
        byte[] dsc = getChartFromDB(getLatestChartDates().get(j));
        logger.log(Level.INFO, "ChartBean: Got chart from DB for Date {0}", sdf2.format(getLatestChartDates().get(j)));
        if (dsc == null) {
            //otherwise create it and stote itin the db for later
            dsc = createChart(getLatestChartDates().get(j));
            logger.log(Level.INFO, "Created Chart for Date {0}", sdf2.format(getLatestChartDates().get(j)));
        }
        return dsc;
    }

    public byte[] createChart(Date chartDate) {
        //StreamedContent chart = null;
        byte[] ba = null;
        GregorianCalendar startDate = new GregorianCalendar();
        startDate.setTime(chartDate);
        GregorianCalendar endDate = new GregorianCalendar();


        startDate.set(Calendar.HOUR_OF_DAY, 0);
        startDate.set(Calendar.MINUTE, 0);
        startDate.set(Calendar.SECOND, 0);
        startDate.set(Calendar.MILLISECOND, 0);
        endDate.setTime(startDate.getTime());
        endDate.add(Calendar.HOUR_OF_DAY, 24);
        endDate.add(Calendar.MILLISECOND, -1);

        String message = "ChartBean Day:" + sdf2.format(chartDate) + ",  startDate:" + sdf2.format(startDate.getTime()) + ",  endDate:" + sdf2.format(endDate.getTime()) + ".";
        logger.log(Level.INFO, message);
        List<WebsiteMonitor> successfulTestResults = null;
        List<WebsiteMonitor> failedTestResults = null;
        List<WebsiteMonitor> successfulModeratorResults = null;
        List<WebsiteMonitor> failedModeratorResults = null;
        try {
            //successfulTestResults = WebsiteMonitorFacade.findSuccessfulTestResultsBetweenTwoDates(1, endDate.getTime(), startDate.getTime());
            //failedTestResults = WebsiteMonitorFacade.findFailedTestResultsBetweenTwoDates(1, endDate.getTime(),startDate.getTime());
            successfulTestResults = WebsiteMonitorFacade.findDateRangeOfWebsiteMonitors(startDate.getTime(), endDate.getTime(), true, true, 1);
            failedTestResults = WebsiteMonitorFacade.findDateRangeOfWebsiteMonitors(startDate.getTime(), endDate.getTime(), false, true, 1);
            successfulModeratorResults = WebsiteMonitorFacade.findDateRangeOfWebsiteMonitors(startDate.getTime(), endDate.getTime(), true, true, 3);
            failedModeratorResults = WebsiteMonitorFacade.findDateRangeOfWebsiteMonitors(startDate.getTime(), endDate.getTime(), false, true, 3);
        } catch (Exception e) {

            JsfUtil.addErrorMessage(e, "WebsiteMonitor Chart Critical Error", " database.");
        }

        //XYSeries responseTimeSeries = new XYSeries("Successful Response Times");
        // XYSeries responseTimeSeries2 = new XYSeries("Failed Tests");

        //  XYSeries responseTimeSeries = new XYSeries("Successful Response Times");
        //XYSeries responseTimeSeries2 = new XYSeries("Failed Tests");
        //TimeSeries responseTimeSeries = new TimeSeries("Successful Response Times");
        //TimeSeries responseTimeSeries2 = new TimeSeries("Failed Tests");
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        if (successfulTestResults != null) {
            for (TimeSeries ts : populateSeries("Total test time", "Login Time", "Request Time", null, successfulTestResults)) {
                dataset.addSeries(ts);
            }
        }
        if (successfulModeratorResults != null) {
            for (TimeSeries ts : populateSeries("Google.com.au", null, null, null, successfulModeratorResults)) {
                dataset.addSeries(ts);
            }
        }

        if (failedTestResults != null) {
            for (TimeSeries ts : populateSeries("Failed Tests", null, null, null, failedTestResults)) {
                dataset.addSeries(ts);
            }
        }

        if (failedModeratorResults != null) {
            for (TimeSeries ts : populateSeries("Failed Google", null, null, null, failedModeratorResults)) {
                dataset.addSeries(ts);
            }
        }

        if (failedTestResults == null && successfulTestResults == null) {
            JsfUtil.addSuccessMessage("No Chart Data Found ", "The database didn't have any entries for " + sdf3.format(chartDate) + ". Try a more recent date.");
        }
        if (successfulModeratorResults == null && failedModeratorResults == null) {
            JsfUtil.addSuccessMessage("No Moderator Chart Data Found ", "The database didn't have any entries for " + sdf3.format(chartDate) + ". Try a more recent date.");
        }


        try {
            JFreeChart jfreechart = new JfcTimeSeriesChart1().createChart(dataset, startDate.getTime(), endDate.getTime());

            BufferedImage bufImg = jfreechart.createBufferedImage(800, 500);
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            //JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(os);
            //encoder.encode(bufImg);
            ImageIO.write(bufImg, "png", os);
            saveChartToDB(os, 1, chartDate);
            //encoder = JPEGCodec.createJPEGEncoder(os);
            //encoder.encode(bufImg);
            //ImageIO.write(bufImg, "jpg", os);
            ba = os.toByteArray();
            // is = new ByteArrayInputStream(os.toByteArray());

            //chart = new DefaultStreamedContent(is, "image/png");

            //chart =  new DefaultStreamedContent(new ByteArrayInputStream(png.getRawData()), "image/png"); // or whatever your image mime type

        } catch (Exception e) {


            logger.log(Level.WARNING, "bundle_key", e);
        }


        return ba;
    }

    private ArrayList<TimeSeries> populateSeries(String label, String label2, String label3, String label4, List<WebsiteMonitor> sqlResults) {
        ArrayList<TimeSeries> alts = new ArrayList<TimeSeries>();
        TimeSeries series = null;
        TimeSeries series2 = null;
        TimeSeries series3 = null;
        TimeSeries series4 = null;
        if (label2 == null) {
            label2 = "Series 2";
        }
        if (label3 == null) {
            label3 = "Series 3";
        }
        if (label4 == null) {
            label4 = "Series 4";
        }
        try {

            series = new TimeSeries(label);
            series2 = new TimeSeries(label2);
            series3 = new TimeSeries(label3);
            series4 = new TimeSeries(label4);
            int size = sqlResults.size();
            for (int i = 0; i < size; i++) {
                WebsiteMonitor wm = sqlResults.get(i);
                double yVal = wm.getDuration();
                double yVal2 = wm.getDuration2();
                double yVal3 = wm.getDuration3();
                double yVal4 = wm.getDuration4();
                try {
                    series.add(new Minute(wm.getStartTime()), yVal);
                    if (yVal2 != -1) {
                        series2.add(new Minute(wm.getStartTime()), yVal2);
                    }
                    if (yVal3 != -1) {
                        series3.add(new Minute(wm.getStartTime()), yVal3);
                    }
                    if (yVal4 != -1) {
                        series4.add(new Minute(wm.getStartTime()), yVal4);
                    }
                } catch (Exception e) {
                    if (e.getMessage().indexOf("Duplicates are not permitted") != -1) {
                        logger.log(Level.FINE, "Duplicate time detected for series point id: {0}. Skipping!!", wm.getId());
                    } else {
                        logger.log(Level.WARNING, "Couldn't add series point!!", e);
                    }
                }
            }

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Populate Series Method, Critical Error", "Couldn't populate the series with SQL results.");
        }
        alts.add(series);
        if (!series2.isEmpty()) {
            alts.add(series2);
        }
        if (!series3.isEmpty()) {
            alts.add(series3);
        }
        if (!series4.isEmpty()) {
            alts.add(series4);
        }

        return alts;
    }

    private void saveChartToDB(ByteArrayOutputStream image, int type, Date chartDate) {
        try {
            // if we use jpa we can use this
            Monitoringcharts wsm = new Monitoringcharts();
            wsm.setId(0);
            wsm.setType(type);
            wsm.setChart(image.toByteArray());
            wsm.setDate(chartDate);
            setLastUpdateTime("Last Updated: " + sdf4.format(chartDate));
            monitoringchartsFacadeFacade.create(wsm);

        } catch (Exception e) {
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.SEVERE, "Could not persist Chart results", e);


        }
    }

    public void updateChart() {
        chart0 = getChart(0);
    }

    /**
     * @return the lastUpdateTime
     */
    public String getLastUpdateTime() {
        return lastUpdateTime;
    }

    /**
     * @param lastUpdateTime the lastUpdateTime to set
     */
    public void setLastUpdateTime(String lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    /**
     * @return the latestChartDates
     */
    public ArrayList<Date> getLatestChartDates() {
        return latestChartDates;
    }

    /**
     * @param latestChartDates the latestChartDates to set
     */
    public void setLatestChartDates(ArrayList<Date> latestChartDates) {
        this.latestChartDates = latestChartDates;
    }

    /**
     * @param models the models to set
     */
    /* public void setModels(ArrayList<StreamedContent> models) {
    this.models = models;
    }/*
    
    /**
     * @return the chart0
     */
    public StreamedContent getChart0() {

        if (chart0 == null) {
            chart0 = getChart(0);
        }
        return convertChartToStreamedContent(chart0);
    }

    /**
     * @param chart0 the chart0 to set
     */
    public void setChart0(byte[] chart0) {
        this.chart0 = chart0;
    }

    /**
     * @return the chart1
     */
    public StreamedContent getChart1() {
        if (chart1 == null) {
            chart1 = getChart(1);
        }

        return convertChartToStreamedContent(chart1);
    }

    /**
     * @param chart1 the chart1 to set
     */
    public void setChart1(byte[] chart1) {
        this.chart1 = chart1;
    }

    /**
     * @return the chart2
     */
    public StreamedContent getChart2() {
        if (chart2 == null) {
            chart2 = getChart(2);
        }

        return convertChartToStreamedContent(chart2);
    }

    /**
     * @param chart2 the chart2 to set
     */
    public void setChart2(byte[] chart2) {
        this.chart2 = chart2;
    }

    /**
     * @return the chart3
     */
    public StreamedContent getChart3() {
        if (chart3 == null) {
            chart3 = getChart(3);
        }

        return convertChartToStreamedContent(chart3);
    }

    /**
     * @param chart3 the chart3 to set
     */
    public void setChart3(byte[] chart3) {
        this.chart3 = chart3;
    }

    /**
     * @return the chart4
     */
    public StreamedContent getChart4() {
        if (chart4 == null) {
            chart4 = getChart(4);
        }

        return convertChartToStreamedContent(chart4);
    }

    /**
     * @param chart4 the chart4 to set
     */
    public void setChart4(byte[] chart4) {
        this.chart4 = chart4;
    }

    /**
     * @return the chart5
     */
    public StreamedContent getChart5() {
        if (chart5 == null) {
            chart5 = getChart(5);
        }

        return convertChartToStreamedContent(chart5);
    }

    /**
     * @param chart5 the chart5 to set
     */
    public void setChart5(byte[] chart5) {
        this.chart5 = chart5;
    }

    /**
     * @return the chart6
     */
    public StreamedContent getChart6() {
        if (chart6 == null) {
            chart6 = getChart(6);
        }

        return convertChartToStreamedContent(chart6);
    }

    /**
     * @param chart6 the chart6 to set
     */
    public void setChart6(byte[] chart6) {
        this.chart6 = chart6;
    }
}
