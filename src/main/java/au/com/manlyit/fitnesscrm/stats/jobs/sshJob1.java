/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.jobs;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 *
 * @author david
 */
public class sshJob1 implements Job {

    public sshJob1() {
    }
    private String username = null;
    private String passphrase = null;
    private String host = null;
    private String key = null;
    private String command = null;
    private int timeout = -1;

    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {


        java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, "Executing SSH Job 1");
        // this section is entered once per day
         /* try { 

            JobDataMap dataMap = context.getMergedJobDataMap(); // Note the difference from the previous example
            
            setUsername(dataMap.getString("Username"));
            setPassphrase(dataMap.getString("Password"));
            setHost(dataMap.getString("Host"));
            setKey(dataMap.getString("Key"));
            setCommand(dataMap.getString("Command"));
            setTimeout(dataMap.getInt("Timeout"));
            if (timeout <= 0) {
            setTimeout(30000);
            }
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "An exception occurred getting data from the merged trigger datamap", e);

        }*/
        JSch jsch = new JSch();

        try {
            jsch.addIdentity(key, passphrase);
            Session session = jsch.getSession(username, host, 22);
            session.setConfig("StrictHostKeyChecking", "no");
            //session.setPassword(passphrase); 
            session.connect(timeout);

            Channel channel = session.openChannel("exec");
            ChannelExec channelExec = (ChannelExec) channel;
            channelExec.setCommand(command);
            channel.connect();

// read channel.getInputStream() here if you want to capture the output

            channel.disconnect();
            session.disconnect();
        } catch (JSchException jSchException) {
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING, "Jsch Error: " + jSchException.getMessage());

        }

        //jsch.setKnownHosts("/home/foo/.ssh/known_hosts");

        //Session session = jsch.getSession(user, host, 22);

        //session.setPassword("your password");

        // username and password will be given via UserInfo interface.

        //session.set // session.setConfig("StrictHostKeyChecking", "no");
        //session.connect();
        //         session
        // .connect(30000);   // making a connection with timeout.

        // Channel channel = session.openChannel("shell");

        // Enable agent-forwarding.
        //((ChannelShell)channel).setAgentForwarding(true);

        //  channel.setInputStream(System.in);
        /*
        // a hack for MS-DOS prompt on Windows.
        channel.setInputStream(new FilterInputStream(System.in){
        public int read(byte[] b, int off, int len)throws IOException{
        return in.read(b, off, (len>1024?1024:len));
        }
        });
         */

        //  channel.setOutputStream(System.out);

        /*
        // Choose the pty-type "vt102".
        ((ChannelShell)channel).setPtyType("vt102");
         */

        /*
        // Set environment variable "LANG" as "ja_JP.eucJP".
        ((ChannelShell)channel).setEnv("LANG", "ja_JP.eucJP");
         */

        //channel.connect();


    }

    synchronized private Connection getAMySqlDBConnection(String connectUrl, String user, String pass) {
        Connection con = null;
        try {
            //String dbUrl = "jdbc:mysql://" + failFromServer + ":3306/" + db;
              java.util.Properties properties = new java.util.Properties();
            properties.setProperty("user", user);
            properties.setProperty("password", pass);
            properties.setProperty("useSSL", "false");
            properties.setProperty("autoReconnect", "true");
            try {
                Class.forName("com.mysql.jdbc.Driver").newInstance();
            } catch (Exception ex) {
                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING, "Couldnt load the com.mysql.jdbc.Driver class\r\n " + connectUrl + "\r\n" + ex.getMessage());
            }
            try {
                con = DriverManager.getConnection(connectUrl, properties);
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
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @param passphrase the passphrase to set
     */
    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    /**
     * @param host the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @param key the key to set
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * @param command the command to set
     */
    public void setCommand(String command) {
        this.command = command;
    }

    /**
     * @param timeout the timeout to set
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}