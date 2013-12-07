/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.chartbeans.websiteMonitorChart;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.WebsiteMonitor;
import java.awt.Color;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.faces.bean.ManagedBean;
import javax.imageio.ImageIO;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author david
 */
@ManagedBean(name = "jFreeChartWebMonBeanold")
@RequestScoped
public class JFreeChartWebMonBean_Old implements Serializable {

    private ArrayList<StreamedContent> models;
    private SimpleDateFormat sdf = new SimpleDateFormat("HHmm");
    private SimpleDateFormat sdf2 = new SimpleDateFormat("dd/MM/yyy HH:mm:ss");
    private SimpleDateFormat sdf3 = new SimpleDateFormat("EEEE, d MMM yyyy");
    private static final Logger logger = Logger.getLogger(websiteMonitorChart.class.getName());

    /** Creates a new instance of MeasurementsChart */
    public JFreeChartWebMonBean_Old() {
    }
    @EJB
    private au.com.manlyit.fitnesscrm.stats.beans.WebsiteMonitorFacade WebsiteMonitorFacade;

    @PostConstruct
    public void createChart() {
    }

    public StreamedContent createChart(int day, int maxSize) {
        StreamedContent chart = null;

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
        earlierDate.add(Calendar.SECOND, 1);
        if (hours > 0) {
            latestDate.add(Calendar.HOUR, offsetForlatestDate);
        }
        String earlierDateString = sdf2.format(earlierDate.getTime());
        String latestDateString = sdf2.format(latestDate.getTime());
        String message = "Day:" + day + ",  earlierDate:" + sdf2.format(earlierDate.getTime()) + ",  latestDate:" + sdf2.format(latestDate.getTime()) + ".";
        logger.log(Level.INFO, message);
        ArrayList<WebsiteMonitor> successfulTestResults = null;
        ArrayList<WebsiteMonitor> failedTestResults = null;
        try {
            successfulTestResults = (ArrayList<WebsiteMonitor>) WebsiteMonitorFacade.findSuccessfulTestResultsBetweenTwoDates(1, latestDate.getTime(), earlierDate.getTime());
            failedTestResults = (ArrayList<WebsiteMonitor>) WebsiteMonitorFacade.findFailedTestResultsBetweenTwoDates(1, latestDate.getTime(), earlierDate.getTime());

        } catch (Exception e) {

            JsfUtil.addErrorMessage(e, "WebsiteMonitor Chart Critical Error", " database.");
        }

        //XYSeries responseTimeSeries = new XYSeries("Successful Response Times");
        // XYSeries responseTimeSeries2 = new XYSeries("Failed Tests");

        //  XYSeries responseTimeSeries = new XYSeries("Successful Response Times");
        //XYSeries responseTimeSeries2 = new XYSeries("Failed Tests");
        //TimeSeries responseTimeSeries = new TimeSeries("Successful Response Times");
        //TimeSeries responseTimeSeries2 = new TimeSeries("Failed Tests");

        TimeSeries responseTimeSeries = populateSeries("Successful Response Times", successfulTestResults);
        TimeSeries responseTimeSeries2 = populateSeries("Failed Tests", failedTestResults);

        // XYSeriesCollection dataset = new XYSeriesCollection();
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(responseTimeSeries2);
        dataset.addSeries(responseTimeSeries);



        try {

            String xLabel = "Time: " + earlierDateString + " to " + latestDateString;
            String titleDateLabel = "Website Response Times " + sdf3.format(latestDate.getTime());
            // create the chart...
            // JFreeChart jfreechart = ChartFactory.createXYLineChart(
            JFreeChart jfreechart = ChartFactory.createTimeSeriesChart(
                    titleDateLabel, // chart title
                    xLabel, // x axis label
                    "milliseconds", // y axis label
                    dataset, // data
                    //PlotOrientation.VERTICAL,
                    true, // include legend
                    true, // tooltips
                    false // urls
                    );

            // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
            jfreechart.setBackgroundPaint(Color.white);

//        final StandardLegend legend = (StandardLegend) chart.getLegend();
            //      legend.setDisplaySeriesShapes(true);

            // get a reference to the plot for further customisation...
            XYPlot plot = jfreechart.getXYPlot();
            plot.setBackgroundPaint(Color.lightGray);
            //    plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
            plot.setDomainGridlinePaint(Color.white);
            plot.setRangeGridlinePaint(Color.white);

            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
            renderer.setSeriesLinesVisible(0, false);
            renderer.setSeriesShapesVisible(1, false);
            plot.setRenderer(renderer);

            // change the auto tick unit selection to integer units only...
            // NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            // rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
            // OPTIONAL CUSTOMISATION COMPLETED.

            // Tell the chart how we would like dates to read
            DateAxis axis = (DateAxis) plot.getDomainAxis();
            axis.setDateFormatOverride(sdf);

            //File chartFile = new File("/tmp/dynamichart" + day + ".chart");
            //ChartUtilities.saveChartAsPNG(chartFile, jfreechart, 800, 500);


            // chart = new DefaultStreamedContent(new FileInputStream(chartFile), "image/png");

            BufferedImage bufImg = jfreechart.createBufferedImage(800, 500);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(bufImg, "jpg", os);
            InputStream is = new ByteArrayInputStream(os.toByteArray());
            //  Blob blb = bufImg.Image png = Image.getInstance(


            chart = new DefaultStreamedContent(is, "image/jpg");

            //chart =  new DefaultStreamedContent(new ByteArrayInputStream(png.getRawData()), "image/png"); // or whatever your image mime type

        } catch (Exception e) {


            logger.log(Level.WARNING, "bundle_key", e);
        }


        return chart;
    }

    private TimeSeries populateSeries(String label, ArrayList<WebsiteMonitor> sqlResults) {
        TimeSeries series = null;
        try {

            series = new TimeSeries(label);
            int size = sqlResults.size();
            for (int i = 0; i < size; i++) {
                WebsiteMonitor wm = sqlResults.get(i);
                double yVal = wm.getDuration();
                //String xAxixValue = sdf.format(wm.getStartTime());
                //double xVal = Integer.parseInt(xAxixValue);
                try {
                    series.add(new Minute(wm.getStartTime()), yVal);
                } catch (Exception e) {
                    if (e.getMessage().indexOf("Duplicates are not permitted") != -1) {
                        logger.log(Level.INFO, "Duplicate time detected for series point id: {0}. Skipping!!", wm.getId());
                    } else {
                        logger.log(Level.WARNING, "Couldn't add series point!!", e);
                    }
                }

            }

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Populate Series Method, Critical Error", "Couldn't populate the series with SQL results.");
        }


        return series;
    }

    public ArrayList<StreamedContent> getModel() {
        if (models == null) {
            models = new ArrayList<StreamedContent>();
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
