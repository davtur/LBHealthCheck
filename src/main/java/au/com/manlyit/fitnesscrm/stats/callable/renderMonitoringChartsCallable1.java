/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.callable;

import au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade;
import au.com.manlyit.fitnesscrm.stats.chartbeans.JfcTimeSeriesChart1;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.Monitoringcharts;
import au.com.manlyit.fitnesscrm.stats.db.WebsiteMonitor;

import java.awt.image.BufferedImage;

import java.io.ByteArrayOutputStream;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.imageio.ImageIO;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.quartz.JobDataMap;

/**
 *
 * @author david
 */
public class renderMonitoringChartsCallable1 implements Callable<CallableTaskResults> {

    @EJB
    private ConfigMapFacade configMapFacade;

    //@PersistenceContext(unitName="FitnessStatsPU")
    //private EntityManagerFactory emf;
    private EntityManager entityManager;
    private final JobDataMap paramMap;
    private static final Logger logger = Logger.getLogger(renderMonitoringChartsCallable1.class.getName());
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//2010-01-01 00:00:00
    private SimpleDateFormat sdf2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private SimpleDateFormat sdf3 = new SimpleDateFormat("EEEE, d MMM yyyy");
    private SimpleDateFormat sdf4 = new SimpleDateFormat("HHmm");
    private SimpleDateFormat sdf5 = new SimpleDateFormat("yyyy-MM-dd");//2010-01-01

    public renderMonitoringChartsCallable1(JobDataMap parameters) {
        this.paramMap = parameters;
    }

    @Override
    public CallableTaskResults call() {
        CallableTaskResults result = new CallableTaskResults();
        try {
            EntityManagerFactory emf = Persistence.createEntityManagerFactory("FitnessStatsPU2");
            entityManager = emf.createEntityManager();

            if (entityManager == null) {

                logger.log(Level.SEVERE, "Entity Manager for PU  is NULL in {0}", this.getClass().getName());
            }
            createChart(new Date());
            result.setIsSuccessful(true);
            result.setResultCode(0);
            result.setResultData("Successfully Created Chart");
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
            return result;
        }
    }

    public void createChart(Date chartDate) {

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

        String endDateString = sdf2.format(endDate.getTime());
        String startDateString = sdf2.format(startDate.getTime());
        String message = "Cronjob - Day:" + sdf2.format(chartDate) + ",  startDate:" + sdf2.format(startDate.getTime()) + ",  endDate:" + sdf2.format(endDate.getTime()) + ".";
        logger.log(Level.INFO, message);
        List<WebsiteMonitor> successfulTestResults = null;
        List<WebsiteMonitor> failedTestResults = null;
        List<WebsiteMonitor> successfulModeratorResults = null;
        List<WebsiteMonitor> failedModeratorResults = null;
        try {
            //successfulTestResults = WebsiteMonitorFacade.findSuccessfulTestResultsBetweenTwoDates(1, endDate.getTime(), startDate.getTime());
            //failedTestResults = WebsiteMonitorFacade.findFailedTestResultsBetweenTwoDates(1, endDate.getTime(),startDate.getTime());
            successfulTestResults = findDateRangeOfWebsiteMonitors(startDate.getTime(), endDate.getTime(), true, true, 1);
            failedTestResults = findDateRangeOfWebsiteMonitors(startDate.getTime(), endDate.getTime(), false, true, 1);
            successfulModeratorResults = findDateRangeOfWebsiteMonitors(startDate.getTime(), endDate.getTime(), true, true, 3);
            failedModeratorResults = findDateRangeOfWebsiteMonitors(startDate.getTime(), endDate.getTime(), false, true, 3);
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
            // InputStream is = new ByteArrayInputStream(os.toByteArray());
            //  Blob blb = bufImg.Image png = Image.getInstance(
            if (os != null) {
                saveChartToDB(os, 1, new Date());
            } else {
                logger.log(Level.WARNING, "Chart creation error, chart output stream is null:");
            }
            //chart = new DefaultStreamedContent(is, "image/png");

            //chart =  new DefaultStreamedContent(new ByteArrayInputStream(png.getRawData()), "image/png"); // or whatever your image mime type
        } catch (Exception e) {

            logger.log(Level.WARNING, "Chart creation error:", e);
        }

        //return chart;
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

    private Monitoringcharts getTodaysChartIfItExists(Date chartDate) {
        GregorianCalendar startDate = new GregorianCalendar();
        GregorianCalendar endDate = new GregorianCalendar();
        startDate.set(Calendar.HOUR_OF_DAY, 0);
        startDate.set(Calendar.MINUTE, 0);
        startDate.set(Calendar.SECOND, 0);
        startDate.set(Calendar.MILLISECOND, 0);
        endDate.setTime(startDate.getTime());
        endDate.add(Calendar.HOUR_OF_DAY, 24);
        endDate.add(Calendar.MILLISECOND, -1);
        Monitoringcharts mc = null;
        try {
            List<Monitoringcharts> chartsFound = findDateRangeMonitoringcharts(startDate.getTime(), endDate.getTime(), true);
            int count = 0;
            try {
                for (Monitoringcharts mc2 : chartsFound) {
                    mc = mc2;
                    count++;
                }
            } catch (Exception sQLException) {
                logger.log(Level.INFO, "Couldn't get a count", sQLException);
            }
            if (count > 1) {
                logger.log(Level.INFO, "More than 1 chart existed so using the latest for Date {0}", sdf5.format(chartDate));
            }
            if (count == 0) {
                logger.log(Level.INFO, "No charts found for Date {0}", sdf5.format(chartDate));
            }

        } catch (Exception ex) {
            String message = "getTodaysChartIfItExists threw an exception \r\n" + ex.getMessage();
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, message);
        }
        return mc;
    }

    private void saveChartToDB(ByteArrayOutputStream image, int type, Date chartDate) {
        try {
            Monitoringcharts mc = getTodaysChartIfItExists(chartDate);
            if (mc == null) {
                getEntityManager().getTransaction().begin();
                Monitoringcharts wsm = new Monitoringcharts();
                wsm.setId(0);
                wsm.setType(type);
                wsm.setChart(image.toByteArray());
                wsm.setDate(chartDate);
                getEntityManager().persist(wsm);
                getEntityManager().getTransaction().commit();
            } else {
                getEntityManager().getTransaction().begin();
                mc.setChart(image.toByteArray());
                mc.setDate(chartDate);
                getEntityManager().merge(mc);
                getEntityManager().getTransaction().commit();

            }
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.SEVERE, "Could not persist Chart results", e);
        }
    }

    public List<Monitoringcharts> findDateRangeMonitoringcharts(Date startDate, Date endDate, boolean sortAsc) {
        List<Monitoringcharts> retList = null;

        try {
            CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
            CriteriaQuery<Monitoringcharts> cq = cb.createQuery(Monitoringcharts.class);
            Root<Monitoringcharts> rt = cq.from(Monitoringcharts.class);
            Expression<Date> createDate = rt.get("date");

            cq.where(cb.between(createDate, startDate, endDate));
            cq.select(rt);
            Expression<Date> express = rt.get("date");
            if (sortAsc) {
                cq.orderBy(cb.asc(express));
            } else {
                cq.orderBy(cb.desc(express));
            }
            Query q = getEntityManager().createQuery(cq);
            retList = (List<Monitoringcharts>) q.getResultList();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
        return retList;
    }

    public List<WebsiteMonitor> findDateRangeOfWebsiteMonitors(Date startDate, Date endDate, boolean successful, boolean sortAsc, int testType) {
        List<WebsiteMonitor> retList = null;
        Integer successfulResult = 0;
        try {
            CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
            CriteriaQuery<WebsiteMonitor> cq = cb.createQuery(WebsiteMonitor.class);
            Root<WebsiteMonitor> rt = cq.from(WebsiteMonitor.class);
            Expression<Date> stime = rt.get("startTime");
            Expression<Integer> res = rt.get("result");
            Expression<Integer> ttype = rt.get("testType");
            Predicate condition = null;
            if (successful) {
                condition = cb.equal(res, successfulResult);
            } else {
                condition = cb.equal(res, successfulResult).not();
            }
            Predicate condition2 = cb.between(stime, startDate, endDate);
            Predicate condition3 = cb.equal(ttype, testType);
            cq.where(cb.and(condition, condition2, condition3));
            cq.select(rt);
            Expression<Date> express = rt.get("startTime");
            if (sortAsc) {
                cq.orderBy(cb.asc(express));
            } else {
                cq.orderBy(cb.desc(express));
            }
            Query q = getEntityManager().createQuery(cq);
            retList = (List<WebsiteMonitor>) q.getResultList();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Persistence Error - findDateRangeOfWebsiteMonitors", e);
        }
        if (retList != null) {
            if (retList.isEmpty()) {
                retList = null;
            }
        }
        return retList;
    }

    /**
     * @return the entityManager
     */
    public EntityManager getEntityManager() {
        return entityManager;
    }
}
