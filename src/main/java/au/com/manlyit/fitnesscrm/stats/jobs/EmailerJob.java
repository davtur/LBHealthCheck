/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.jobs;

import au.com.manlyit.fitnesscrm.stats.classes.util.SendHTMLEmailWithFileAttached;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 *
 * @author david
 */
public class EmailerJob implements Job {

    public EmailerJob() {
    }
      private  String dbUsername = null;
      private  String dbPassword = null;
      private String dbConnectURL = null;

    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {


        java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, "Executing Emailer Job");
        // this section is entered once per day
       /* String mailHost = null;
        String toEmailAddress = null;
        String ccEmailAddress = null;
        String fromEmailAddress = null;
 
        try {

            JobDataMap dataMap = context.getJobDetail().getJobDataMap(); ; // Note the difference from the previous example

            setDbUsername(dataMap.getString("Username"));
            setDbPassword(dataMap.getString("Password"));
            //mailHost = dataMap.getString("MailHost");
            //fromEmailAddress = dataMap.getString("FromEmailAddress");
            //toEmailAddress = dataMap.getString("ToEmailAddress");
            //ccEmailAddress = dataMap.getString("CCEmailAddress");
            setDbConnectURL(dataMap.getString("dbConnectURL"));

        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "An exception occurred getting data from the merged trigger datamap", e);

        }*/

        GregorianCalendar calender1 = new GregorianCalendar();
        GregorianCalendar calender2 = new GregorianCalendar();
        calender1.set(Calendar.HOUR_OF_DAY, 0);
        calender1.set(Calendar.MINUTE, 0);
        calender1.set(Calendar.SECOND, 0);
//-------------------------------------------------------------------------------------------------
        PreparedStatement prepStmt = null;
        PreparedStatement prepStmt2 = null;
        int count = 0;
        int rowcountAffected = 0;

        String sqlQuery = "SELECT * FROM emailQueue where status = 0 LIMIT 100";
        String updateQuery = "UPDATE emailQueue SET status = '1' WHERE id = ?";
        Connection con = null;
        ResultSet rs = null;
        ResultSetMetaData rsmd = null;
        try {
            con = getAMySqlDBConnection(dbConnectURL, dbUsername, dbPassword);
            // run the SQL query and store the result in a list();
            try {
                prepStmt = con.prepareStatement(sqlQuery);
                prepStmt2 = con.prepareStatement(updateQuery);
                
                //Timestamp ts2 = new Timestamp(calender1.getTime().getTime());
                // prepStmt.setTimestamp(1, ts2);
                Logger.getLogger(getClass().getName()).log(Level.INFO, "Getting emails for sending from the queue.");
                rs = prepStmt.executeQuery();
                int sent = 0;
                SendHTMLEmailWithFileAttached emailAgent = new SendHTMLEmailWithFileAttached();

                //rsmd = rs.getMetaData();
                //int columnsCount = rsmd.getColumnCount();
                while (rs.next()) {
                    int id = rs.getInt("id");
                    int status = rs.getInt("status");
                    String to = rs.getString("toaddresses");
                    String from = rs.getString("fromaddress");
                    String cc = rs.getString("ccaddresses");
                    String subject = rs.getString("subject");
                    String msg = rs.getString("message");
                    Timestamp sendDate = rs.getTimestamp("sendDate");
                    Timestamp createDate = rs.getTimestamp("createDate");
                    if (sendDate == null) {
                        sendDate = new Timestamp(new Date().getTime());
                    }
                    calender2.setTimeInMillis(sendDate.getTime());
                    if (status == 0) {
                        if (calender2.compareTo(calender1) <= 0) {
                            //send the email 
                            if(cc.trim().length() == 0){
                                cc = null;
                            }
                            try {
                                emailAgent.send(to, cc, from, subject, msg, null, false);
                            } catch (Exception e) {
                                String message = "There was a problem sending the email id: "+Integer.toString(id) +", to: "+to+", cc: "+cc+", from: "+from+", subject: "+subject+".The exception is: " + updateQuery + "\r\n" + e.getMessage();
                                Logger.getLogger(getClass().getName()).log(Level.SEVERE, message);

                            }
                            sent++;
                            try {
                                prepStmt2.setInt(1, id);
                                prepStmt2.executeUpdate();
                            } catch (SQLException sQLException) {
                                String message = "The email status update query threw an exception : " + updateQuery + "\r\n" + sQLException.getMessage();
                                Logger.getLogger(getClass().getName()).log(Level.SEVERE, message);

                            }
                        }
                    }
                }
                String message = "The Emailer sent " + Integer.toString(sent) + " scheduled emails.";
                Logger.getLogger(getClass().getName()).log(Level.INFO, message);
            } catch (Exception ex) {
                String message = "The query threw an exception : " + sqlQuery + "\r\n" + ex.getMessage();
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, message);
            } finally {
                if (rs != null) {
                    rs.close();
                }
                rs = null;
                if (prepStmt != null) {
                    prepStmt.close();
                }
                prepStmt = null;
                 if (prepStmt2 != null) {
                    prepStmt2.close();
                }
                prepStmt2 = null;
            }
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING, "Error: Have added " + Integer.toString(count) + " Daily Tasks to the Queue for today", e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Closing DB connection", ex);
                }
            }
        }
        String msg = "Emailer is finished for now and going to sleep ";
        java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, msg);

    }

    synchronized private Connection getAMySqlDBConnection(String connectUrl, String user, String pass) {
        Connection con = null;
        try {
            //String dbUrl = "jdbc:mysql://" + failFromServer + ":3306/" + db;
            try {
                Class.forName("com.mysql.jdbc.Driver").newInstance();
            } catch (Exception ex) {
                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING, "Couldnt load the com.mysql.jdbc.Driver class\r\n " + connectUrl + "\r\n" + ex.getMessage());
            }
            try {
                con = DriverManager.getConnection(connectUrl, user, pass);
            } catch (SQLException ex) {

                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING, "Couldnt get a connection :" + connectUrl + "\r\n" + ex.getMessage());
            } catch (Exception ex) {
                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING, "Couldnt get a connection :" + connectUrl + "\r\n" + ex.getMessage());
            }
            if (con == null) {
                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING, "Couldnt get a Database Connection to " + connectUrl + "\r\n" + user + "," + pass);
            }
        } finally {
            return con;
        }
    }

    /**
     * @param dbUsername the dbUsername to set
     */
    public void setDbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
    }

    /**
     * @param dbPassword the dbPassword to set
     */
    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    /**
     * @param dbConnectURL the dbConnectURL to set
     */
    public void setDbConnectURL(String dbConnectURL) {
        this.dbConnectURL = dbConnectURL;
    }
}