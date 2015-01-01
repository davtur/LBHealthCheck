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
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private String dbUsername = null;
    private String dbPassword = null;
    private String dbConnectURL = null;

    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, "Executing Emailer Job");
        Properties props = new Properties();
        JobDataMap dataMap = context.getMergedJobDataMap();

        try {
            props.put("mail.smtp.host", dataMap.getString("mail.smtp.host"));
            props.put("mail.smtp.auth", dataMap.getString("mail.smtp.auth"));
            props.put("mail.debug", dataMap.getString("mail.debug"));
            props.put("mail.smtp.port", dataMap.getString("mail.smtp.port"));
            props.put("mail.smtp.socketFactory.port", dataMap.getString("mail.smtp.socketFactory.port"));
            props.put("mail.smtp.socketFactory.class", dataMap.getString("mail.smtp.socketFactory.class"));
            props.put("mail.smtp.socketFactory.fallback", dataMap.getString("mail.smtp.socketFactory.fallback"));
            props.put("mail.smtp.ssluser", dataMap.getString("mail.smtp.ssluser"));
            props.put("mail.smtp.sslpass", dataMap.getString("mail.smtp.sslpass"));
            props.put("mail.smtp.headerimage.url", dataMap.getString("mail.smtp.headerimage.url"));

            dbUsername = dataMap.getString("db.fitness.username");
            dbPassword = dataMap.getString("db.fitness.password");
            dbConnectURL = dataMap.getString("db.fitness.url");

        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "The Job Datamap may be missing config values.Check that the emailer job was supplied all of the properties it needs.", e);
        }
        GregorianCalendar calCurrentTime = new GregorianCalendar();
        GregorianCalendar calEmailSendDateTime = new GregorianCalendar();
        // calCurrentTime.set(Calendar.HOUR_OF_DAY, 0);
        // calCurrentTime.set(Calendar.MINUTE, 0);
        //  calCurrentTime.set(Calendar.SECOND, 0);
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

                //Timestamp ts2 = new Timestamp(calCurrentTime.getTime().getTime());
                // prepStmt.setTimestamp(1, ts2);
                Logger.getLogger(getClass().getName()).log(Level.FINE, "Getting emails for sending from the queue.");
                rs = prepStmt.executeQuery();
                int sent = 0;
                SendHTMLEmailWithFileAttached emailAgent = new SendHTMLEmailWithFileAttached();

                //rsmd = rs.getMetaData();
                //int columnsCount = rsmd.getColumnCount();
                while (rs.next()) {
                    boolean valid = true;
                    int id = rs.getInt("id");
                    int status = rs.getInt("status");
                    String to = rs.getString("toaddresses");
                    String from = rs.getString("fromaddress");
                    String cc = rs.getString("ccaddresses");
                    String subject = rs.getString("subject");
                    String msg = rs.getString("message");
                    Timestamp sendDate = rs.getTimestamp("sendDate");
                    Timestamp createDate = rs.getTimestamp("createDate");
                    if (validateEmailAddress(to) == false) {
                        valid = false;
                        Logger.getLogger(getClass().getName()).log(Level.WARNING, "To email address is invalid");
                    }
                    if (validateEmailAddress(from) == false) {
                        valid = false;
                        Logger.getLogger(getClass().getName()).log(Level.WARNING, "From email address is invalid");
                    }
                    if (cc != null) {
                        if (cc.trim().isEmpty()) {
                            cc = null;
                        }
                        if (cc != null) {
                            if (validateEmailAddress(cc) == false) {
                                valid = false;
                                Logger.getLogger(getClass().getName()).log(Level.WARNING, "CC email address is invalid");
                            }
                        }
                    }
                    if (subject == null) {
                        valid = false;
                        Logger.getLogger(getClass().getName()).log(Level.WARNING, "Subject is NULL");
                    }
                    if (msg == null) {
                        valid = false;
                        Logger.getLogger(getClass().getName()).log(Level.WARNING, "Message is NULL");
                    }
                    if (sendDate == null) {
                        sendDate = new Timestamp(new Date().getTime());
                    }

                    calEmailSendDateTime.setTimeInMillis(sendDate.getTime());
                    if (status == 0 && valid) {
                        if (calCurrentTime.compareTo(calEmailSendDateTime) > 0) {
                            //send the email 
                            String cc2 = cc;
                            if (cc == null) {
                                cc2 = "Null";
                            }
                            try {
                                String message = "Sending email to:" + to + ", from:" + from + ", cc:" + cc2 + ", subject:" + subject + ", message Length:" + msg.length() + ", sendDate:" + sendDate + ", createDate:" + createDate + ".";
                                Logger.getLogger(getClass().getName()).log(Level.INFO, message);
                                emailAgent.send(to, cc, from, subject, msg, null, props, false);
                            } catch (Exception e) {
                                String message2 = "There was a problem sending the email id: " + Integer.toString(id) + ", to: " + to + ", cc: " + cc + ", from: " + from + ", subject: " + subject + ".The exception is: " + updateQuery + "\r\n" + e.getMessage();
                                Logger.getLogger(getClass().getName()).log(Level.SEVERE, message2);

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
                    } else {
                        if (valid == false) {
                            Logger.getLogger(getClass().getName()).log(Level.WARNING, "MESSAGE FAILED TO SEND - Invalid email parameters. Check emailQueue DB table row, ID:{0}.",id);
                        }
                    }
                }
                String message = "The Emailer sent " + Integer.toString(sent) + " scheduled emails.";
                Logger.getLogger(getClass().getName()).log(Level.INFO, message);
            } catch (SQLException ex) {
                String message = "The query threw an SQL exception : " + sqlQuery + "\r\n" + ex.getMessage();
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, message);
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
        } catch (SQLException e) {
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

    private boolean validateEmailAddress(String email) {

        Pattern p = Pattern.compile(".+@.+\\.[a-z]+");

        //Match the given string with the pattern
        Matcher m = p.matcher(email);

        //Check whether match is found
        return m.matches();

    }

    synchronized private Connection getAMySqlDBConnection(String connectUrl, String user, String pass) {
        Connection con = null;
        try {
            //String dbUrl = "jdbc:mysql://" + failFromServer + ":3306/" + db;
            try {
                Class.forName("com.mysql.jdbc.Driver").newInstance();
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException ex) {
                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING, "Couldnt load the com.mysql.jdbc.Driver class\r\n {0}\r\n{1}", new Object[]{connectUrl, ex.getMessage()});
            }
            try {
                con = DriverManager.getConnection(connectUrl, user, pass);
            } catch (SQLException ex) {

                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING, "Couldnt get a connection :{0}\r\n{1}", new Object[]{connectUrl, ex.getMessage()});
            }
            if (con == null) {
                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING, "Couldnt get a Database Connection to {0}\r\n{1},{2}", new Object[]{connectUrl, user, pass});
            }
        } finally {
            return con;
        }
    }

}
