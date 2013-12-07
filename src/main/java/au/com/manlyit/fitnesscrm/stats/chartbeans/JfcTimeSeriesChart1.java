/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.chartbeans;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.TimeSeriesCollection;

/**
 *
 * @author david
 */
public class JfcTimeSeriesChart1 {

    private SimpleDateFormat sdf = new SimpleDateFormat("HHmm");
    private SimpleDateFormat sdf3 = new SimpleDateFormat("EEEE, d MMM yyyy");
    private SimpleDateFormat sdf2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public JFreeChart createChart(TimeSeriesCollection dataset, Date startDate, Date endDate) {

        String endDateString = sdf2.format(endDate);
        String startDateString = sdf2.format(startDate);

        String xLabel = "Time: " + startDateString + " to " + endDateString;
        String titleDateLabel = "Website Response Times " + sdf3.format(startDate);
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
        renderer.setSeriesShapesVisible(0, false);
        renderer.setSeriesShapesVisible(1, false);
        renderer.setSeriesShapesVisible(2, false);      
        renderer.setSeriesShapesVisible(3, false);
        renderer.setSeriesLinesVisible(4, false);
        renderer.setSeriesLinesVisible(5, false);
       
        renderer.setSeriesPaint(0, new Color(32, 115, 44));// good
        renderer.setSeriesPaint(1, new Color(32, 115, 88));// good
        renderer.setSeriesPaint(2, new Color(115, 115, 132));// good
        renderer.setSeriesPaint(3, new Color(32, 75, 115)); // google OK
        renderer.setSeriesPaint(4, Color.red.brighter());// failed 
        renderer.setSeriesPaint(5, Color.orange.brighter()); // failed to google

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
        return jfreechart;
    }
}
