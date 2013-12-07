/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.callable;

import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.Monitoringcharts;
import au.com.manlyit.fitnesscrm.stats.db.WebsiteMonitor;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.quartz.JobDataMap;

/**
 *
 * @author david
 */
public class renderMonitoringChartsCallable1_old implements Callable<CallableTaskResults> {
    
   
    private EntityManager em;

    private final JobDataMap paramMap;
    private static final Logger logger = Logger.getLogger(renderMonitoringChartsCallable1_old.class.getName());
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//2010-01-01 00:00:00
    private SimpleDateFormat sdf2 = new SimpleDateFormat("dd/MM/yyy HH:mm:ss");
    private SimpleDateFormat sdf3 = new SimpleDateFormat("EEEE, d MMM yyyy");
    private SimpleDateFormat sdf4 = new SimpleDateFormat("HHmm");
    private SimpleDateFormat sdf5 = new SimpleDateFormat("yyyy-MM-dd");//2010-01-01
    private Connection con = null;
    
    public renderMonitoringChartsCallable1_old(JobDataMap parameters) {
        this.paramMap = parameters;
    }
    
    @Override
    public CallableTaskResults call() {
        CallableTaskResults result = new CallableTaskResults();
        try {
            EntityManagerFactory emf = Persistence.createEntityManagerFactory("FitnessStatsPU");
            em = emf.createEntityManager();
            if(em != null){
                logger.log(Level.INFO, "Yay!!!!");  
            }else{
                logger.log(Level.INFO, "Boooooo!!!!");  
            }
            con = getAMySqlDBConnection(paramMap.getString("dbConnectURL"), paramMap.getString("dbUsername"), paramMap.getString("dbPassword"));
            createChart(0);
            result.setIsSuccessful(true);
            result.setResultCode(0);
            result.setResultData("Successfully Created Chart");
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Closing DB connection", ex);
                }
            }
            if(em != null){
                em.close();
            }
             return result;
        }
    }
    
    public void createChart(int day) {
        
        
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
       // int offsetForEarlierDate = hours * -1 * (day + 1);
        int offsetForlatestDate = hours * -1 * day;
        
        
        //earlierDate.add(Calendar.HOUR, offsetForEarlierDate);
        //earlierDate.add(Calendar.SECOND, 1);
        if (hours > 0) {
            latestDate.add(Calendar.HOUR, offsetForlatestDate);
        }
        
        latestDate.set(Calendar.HOUR, 0);
        latestDate.set(Calendar.MINUTE, 0);
        latestDate.set(Calendar.SECOND, 0);
        latestDate.set(Calendar.MILLISECOND, 0);
        earlierDate.setTime(latestDate.getTime());
        earlierDate.add(Calendar.HOUR, hours);
        earlierDate.add(Calendar.MILLISECOND, -1);
        
        String earlierDateString = sdf2.format(earlierDate.getTime());
        String latestDateString = sdf2.format(latestDate.getTime());
        String message = "Day:" + day + ",  earlierDate:" + sdf2.format(earlierDate.getTime()) + ",  latestDate:" + sdf2.format(latestDate.getTime()) + ".";
        logger.log(Level.INFO, message);
        ArrayList<WebsiteMonitor> successfulTestResults = null;
        ArrayList<WebsiteMonitor> failedTestResults = null;
        try {
            successfulTestResults = (ArrayList<WebsiteMonitor>) findSuccessfulTestResultsBetweenTwoDates(1, latestDate.getTime(), earlierDate.getTime());
            failedTestResults = (ArrayList<WebsiteMonitor>) findFailedTestResultsBetweenTwoDates(1, latestDate.getTime(), earlierDate.getTime());
            
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
            axis.setDateFormatOverride(sdf4);

            //File chartFile = new File("/tmp/dynamichart" + day + ".chart");
            //ChartUtilities.saveChartAsPNG(chartFile, jfreechart, 800, 500);


            // chart = new DefaultStreamedContent(new FileInputStream(chartFile), "image/png");

            BufferedImage bufImg = jfreechart.createBufferedImage(800, 500);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            
            //JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(os);
            //encoder.encode(bufImg);
            ImageIO.write(bufImg, "jpeg", os);
            // InputStream is = new ByteArrayInputStream(os.toByteArray());
            //  Blob blb = bufImg.Image png = Image.getInstance(
            saveChartToDB(os, 1, new Date());

            //chart = new DefaultStreamedContent(is, "image/png");

            //chart =  new DefaultStreamedContent(new ByteArrayInputStream(png.getRawData()), "image/png"); // or whatever your image mime type

        } catch (Exception e) {
            
            
            logger.log(Level.WARNING, "bundle_key", e);
        }


        //return chart;
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
    
    private synchronized Connection getAMySqlDBConnection(String connectUrl, String user, String pass) {
        Connection dbCon = null;
        try {
            //String dbUrl = "jdbc:mysql://" + failFromServer + ":3306/" + db;
            try {
                Class.forName("com.mysql.jdbc.Driver").newInstance();
            } catch (Exception ex) {
                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING, "Couldnt load the com.mysql.jdbc.Driver class\r\n {0}\r\n{1}", new Object[]{connectUrl, ex.getMessage()});
            }
            try {
                dbCon = DriverManager.getConnection(connectUrl, user, pass);
            } catch (SQLException ex) {
                
                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING, "Couldnt get a connection :{0}\r\n{1}", new Object[]{connectUrl, ex.getMessage()});
            } catch (Exception ex) {
                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING, "Couldnt get a connection :{0}\r\n{1}", new Object[]{connectUrl, ex.getMessage()});
            }
            if (dbCon == null) {
                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING, "Couldnt get a Database Connection to {0}\r\n{1},{2}", new Object[]{connectUrl, user, pass});
            }
        } finally {
            return dbCon;
        }
    }
    
    public List<WebsiteMonitor> findSuccessfulTestResultsBetweenTwoDates(int test_id, Date top, Date bottom) {
        
        String SQL = "SELECT * FROM website_monitor WHERE start_time > '" + sdf.format(bottom) + "' and start_time < '" + sdf.format(top) + "' and test_type = '" + test_id + "' and result = '0'  order by id DESC";
        return parseResults(executeDBQuery(SQL));
    }
    
    public List<WebsiteMonitor> findFailedTestResultsBetweenTwoDates(int test_id, Date top, Date bottom) {
        
        String SQL = "SELECT * FROM website_monitor WHERE start_time > '" + sdf.format(bottom) + "' and start_time < '" + sdf.format(top) + "' and test_type = '" + test_id + "' and result > '0'  order by id DESC";
        
        return parseResults(executeDBQuery(SQL));
        
    }
    
    private List<WebsiteMonitor> parseResults(ResultSet rs) {
        
        ArrayList<WebsiteMonitor> results = new ArrayList();
        WebsiteMonitor wm = null;
        try {
            while (rs.next()) {
                wm = new WebsiteMonitor();
                wm.setId(rs.getInt("id"));
                wm.setDuration(rs.getInt("duration"));
                wm.setJobToRunOnFail(rs.getInt("job_to_run_on_fail"));
                wm.setNotify(rs.getInt("notify"));
                wm.setTestType(rs.getInt("test_type"));
                wm.setDescription(rs.getString("description"));
                wm.setStartTime(rs.getDate("job_to_run_on_fail"));
                wm.setResult(rs.getInt("result"));
                results.add(wm);                
            }
        } catch (SQLException sQLException) {
            logger.log(Level.WARNING, "Couldn't get chart results from database", sQLException);
        }
        return results;
    }
    
    private ResultSet executeDBQuery(String query) {
        
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        
        try {
            prepStmt = con.prepareStatement(query);
            rs = prepStmt.executeQuery();
            
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.SEVERE, "Could not persist Chart results", e);
            
        } catch (Exception ex) {
            String message = "The query threw an exception : " + query + "\r\n" + ex.getMessage();
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, message);
        } finally {
            try {
                if (prepStmt != null) {
                    prepStmt.close();
                }
                prepStmt = null;
                
                
            } catch (Exception e) {
                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING, "Error:Couldn't close th eprepared statement!!", e);
            }
        }
        return rs;
    }
      private Monitoringcharts getTodaysChartIfItExists(Date chartDate) {
        String queryToRun = "Select * from monitoringcharts where date >= '" + sdf5.format(chartDate) + " 00:00:00' AND date <= '" + sdf5.format(chartDate) + " 23:59:59' order by date ASC";
 
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        Monitoringcharts mc = new Monitoringcharts(-1);
         int count = 0;
        try {
            prepStmt = con.prepareStatement(queryToRun);
            rs = prepStmt.executeQuery();
         try {
            while (rs.next()) {
                mc.setId(rs.getInt("id"));
                mc.setDate(rs.getDate("date"));
                mc.setType(rs.getInt("type"));
                mc.setChart(rs.getBytes("chart"));
                count++;
            }
        } catch (SQLException sQLException) {
            logger.log(Level.INFO, "Couldn't get a count",sQLException);
        }
        if(count > 1){
            logger.log(Level.INFO, "More than 1 chart existed so using the latest for Date {0}", sdf5.format(chartDate));
        }
        if(count == 0){
            logger.log(Level.INFO, "No charts found for Date {0}", sdf5.format(chartDate));
        }

            
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.SEVERE, "Could not persist Chart results", e);
            
        } catch (Exception ex) {
            String message = "The query threw an exception : " + queryToRun + "\r\n" + ex.getMessage();
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, message);
        } finally {
            try {
                if (prepStmt != null) {
                    prepStmt.close();
                }
                prepStmt = null;
                
                
            } catch (Exception e) {
                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING, "Error:Couldn't close th eprepared statement!!", e);
            }
        }
        return mc;
    }
    
    private void saveChartToDB(ByteArrayOutputStream image, int type, Date chartDate) {
        Monitoringcharts mc = getTodaysChartIfItExists(chartDate);
        
        PreparedStatement prepStmt = null;
        String insertQuery = "INSERT INTO monitoringcharts (id, type, date, chart) "
                + "VALUES ( ?,?,?,?);";
        if(mc.getId() == -1){
            insertQuery = "UPDATE monitoringcharts set  type=?, date=?, chart=? where id=? ";
        }
        
        try {
            // if we use jpa we can use this
            Monitoringcharts wsm = new Monitoringcharts();
            if(mc.getId() != -1){
              wsm.setId(mc.getId());  
            }else{
            wsm.setId(0);
            }
            wsm.setType(type);
            wsm.setChart(image.toByteArray());
            wsm.setDate(chartDate);
            prepStmt = con.prepareStatement(insertQuery);
            prepStmt.setInt(1, wsm.getId());
            prepStmt.setInt(2, wsm.getType());
            prepStmt.setDate(3, new java.sql.Date(wsm.getDate().getTime()));
            prepStmt.setBytes(4, image.toByteArray());
            prepStmt.executeUpdate();
            
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.SEVERE, "Could not persist Chart results", e);
            
            
        } catch (Exception ex) {
            String message = "The query threw an exception : " + insertQuery + "\r\n" + ex.getMessage();
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, message);
        } finally {
            try {
                if (prepStmt != null) {
                    prepStmt.close();
                }
                prepStmt = null;
                
            } catch (Exception e) {
                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING, "Error:Couldn't close th eprepared statement!!", e);
            } finally {
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException ex) {
                        Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Closing DB connection", ex);
                    }
                }
                
            }
            
        }
    }
}
