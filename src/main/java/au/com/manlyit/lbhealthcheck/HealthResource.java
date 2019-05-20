/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.lbhealthcheck;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.OperatingSystemMXBean;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PUT;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST Web Service
 *
 * @author david
 */
@Path("/health")
public class HealthResource {

    @Context
    private UriInfo context;
    private static final int MEGABYTE = 1048576;
    private static final int GIGABYTE = 1073741824;
    private static final BigDecimal BD_MEGABYTE = new BigDecimal(MEGABYTE);
    private static final BigDecimal BD_GIGABYTE = new BigDecimal(GIGABYTE);
    /*
    A value of 1 here indicates 100% usage on each cpu. A value greater than one indicates tasks are queued up each cpu.
    see java doc for OperatingSystemMXBean for more info. It may need to be tweaked per OS. 
    
    */
    private static final BigDecimal MAX_1MIN_LOAD_AVERAGE_PER_CPU = new BigDecimal(3);
    private static final BigDecimal MIN_FREE_DISK_SPACE_IN_BYTES = BD_MEGABYTE.multiply(new BigDecimal(10));
    
    private static final Logger LOGGER = Logger.getLogger(HealthResource.class.getName());

    /**
     * Creates a new instance of HealthResource
     */
    public HealthResource() {
    }

    /**
     * Retrieves representation of an instance of
     * au.com.manlyit.lbhealthcheck.HealthResource
     *
     * @return an instance of java.lang.String
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String getHtml() {
        GregorianCalendar checkStart = new GregorianCalendar();
        Runtime rt = Runtime.getRuntime();
        String responseText = "OK";
        String failedText  = "FAIL<BR/>";
        DecimalFormat df2 = new DecimalFormat("#0.00");
        DecimalFormat df3 = new DecimalFormat("#0.000");
        boolean serverHealthOK = true;
        boolean heapMemoryOk = true;
        boolean diskSpaceOK = true;
        boolean loadOk = true;

        // check heap memory
        String memoryStats = "";
        for (MemoryPoolMXBean mpBean : ManagementFactory.getMemoryPoolMXBeans()) {
            if (mpBean.getType() == MemoryType.HEAP) {
                BigDecimal committed = new BigDecimal(mpBean.getUsage().getCommitted());
                BigDecimal used = new BigDecimal(mpBean.getUsage().getUsed());
                BigDecimal remaining = committed.subtract(used);

                BigDecimal commMb = committed.divide(BD_MEGABYTE);
                BigDecimal remMb = remaining.divide(BD_MEGABYTE);

                String name = mpBean.getName();
                memoryStats += name + ": total:" + df3.format(commMb) + "Mb, Remaining:" + df3.format(remMb) + "Mb, \n";

            }
        }
        BigDecimal freeMemMb = new BigDecimal(rt.freeMemory()).divide(BD_MEGABYTE) ;
        String freeMemory = "Free memory: " + df3.format(freeMemMb) + "Mb";

        try {
            byte[] testArray = new byte[100000];
            testArray = null;
        } catch (Exception e) {
            heapMemoryOk = false;
            failedText += " MEMORY LOW: " + rt.freeMemory() + " bytes free .... " + memoryStats +"<BR/>";
        }

        // check load
        String loadAverage;
        OperatingSystemMXBean mpBean = ManagementFactory.getOperatingSystemMXBean();

        BigDecimal systemLoadAverage = new BigDecimal(mpBean.getSystemLoadAverage());
        BigDecimal availableProcessors = new BigDecimal(mpBean.getAvailableProcessors());
        BigDecimal averageLoadPerCore = systemLoadAverage.divide(availableProcessors);

        loadAverage = "System Load Average (1Min): " + df2.format(systemLoadAverage) + ", Available CPUs:" + df2.format(availableProcessors) + ", Average Load Per Core: " + df2.format(averageLoadPerCore) + " \n";
        if (averageLoadPerCore.compareTo(MAX_1MIN_LOAD_AVERAGE_PER_CPU) > 0 && systemLoadAverage.compareTo(BigDecimal.ZERO) > 0) { //  getSystemLoadAverage returns -1 if it cant get any data from the OS
            loadOk = false;
            failedText += " LOAD AVERAGE TOO HIGH: " + loadAverage +"<BR/>";
        }

        // check temp file creation
        File tempFile = null;
        try {
            tempFile = File.createTempFile("health-", "-check");
        } catch (IOException ex) {

            diskSpaceOK = false;
            responseText = "FAILED TO CREATE TEMP FILE: " + ex.getMessage();
        }
        if (tempFile != null) {
            tempFile.delete();
        }

        // disk space
        File testFile = new File("/");
        BigDecimal freeSpaceOnDiskInBytes = new BigDecimal(testFile.getFreeSpace());

        BigDecimal doublefreeDiskMb = freeSpaceOnDiskInBytes.divide(BD_MEGABYTE);

        String diskSpace = "Free bytes on root (/) disk: " + df3.format(doublefreeDiskMb) + "Mb";

        if (freeSpaceOnDiskInBytes.compareTo(MIN_FREE_DISK_SPACE_IN_BYTES) < 0) {
            diskSpaceOK = false;
            failedText += " DISK SPACE TOO LOW: " + df3.format(doublefreeDiskMb) + " Mb free <BR/>";
        }
        Level logLevel = Level.INFO;
        if (heapMemoryOk == false || diskSpaceOK == false || loadOk == false) {
            serverHealthOK = false;
            logLevel = Level.SEVERE;
            responseText = failedText;
        }

        String responseHtml = "<HTML><BODY><H1>" + responseText + "</H1></BODY></HTML>";

        GregorianCalendar checkDone = new GregorianCalendar();
        long checkLengthInMilli = checkDone.getTimeInMillis() - checkStart.getTimeInMillis();

        LOGGER.log(logLevel, "\nHealth Check Completed in {0} milliseconds. Response: {1}. \n{4}\nJVM Memory: \n{2} \n{3} \n{5} \n{6}", new Object[]{checkLengthInMilli, responseHtml, memoryStats, loadAverage, freeMemory, diskSpace,context.getAbsolutePath()});

        if (serverHealthOK == false) {
            java.lang.Throwable cause = new Throwable(failedText);
            throw new WebApplicationException(cause,Response.Status.SERVICE_UNAVAILABLE);
        }

        return responseHtml;

    }

    /**
     * PUT method for updating or creating an instance of HealthResource
     *
     * @param content representation for the resource
     */
    @PUT
    @Consumes(MediaType.TEXT_HTML)
    public void putHtml(String content) {
    }
}
