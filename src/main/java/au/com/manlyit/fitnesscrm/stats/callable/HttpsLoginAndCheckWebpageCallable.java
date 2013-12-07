/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.callable;

import java.util.concurrent.Callable;
import org.quartz.JobDataMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;


/**
 *
 * @author david
 */
public class HttpsLoginAndCheckWebpageCallable implements Callable<CallableTaskResults> {

    private JobDataMap paramMap;
    private Map<String, List<String>> headMap = null;
    private Map<String, List<String>> requestMap = null;
    private Map<String, Map<String, String>> cookieMap = new HashMap<String, Map<String, String>>();
    private String cookies = "";
    private static final Logger logger = Logger.getLogger(HttpsLoginAndCheckWebpageCallable.class.getName());
    public HttpsLoginAndCheckWebpageCallable(JobDataMap parameters) {
        if(parameters == null){
            logger.log(Level.WARNING, "   !!Job parameters are null!!  ");
        }
        this.paramMap = parameters;
    }

    @Override
    public CallableTaskResults call() {

        return run_memberservices_login_and_Test();
    }

    private synchronized CallableTaskResults run_memberservices_login_and_Test() {
        //       jTextArea1.setText("");
        HttpsURLConnection connection = null;
        CallableTaskResults result = new CallableTaskResults();
        java.net.CookieManager cm = new java.net.CookieManager();
        java.net.CookieHandler.setDefault(cm);
        cm.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        
        String loginURL1 = getParamMap().getString("loginURL1");
        String loginURL2 = getParamMap().getString("loginURL2");
        String loginPostQuery = getParamMap().getString("loginPostQuery");
        String loginReferer = getParamMap().getString("loginReferer");
        String testURL1 = getParamMap().getString("testURL1");
        String testOKCriteria1 = getParamMap().getString("testOKCriteria1");
        String testOKCriteria2 = getParamMap().getString("testOKCriteria2");
        String logoutURL1 = getParamMap().getString("logoutURL1");
        String userAgent = getParamMap().getString("userAgent");
        //-------------------------------------------------------------------
        //   Post login info to  login page
        //-------------------------------------------------------------------
        result.setResultData("Beginning Login");
        GregorianCalendar testTimer = null;
        GregorianCalendar loginTimer = new GregorianCalendar();
        long loginTime = -1;
        long testTime = -1;
        try {
            URL url = new URL(loginURL1);
            connection = (HttpsURLConnection) url.openConnection();
             connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", userAgent);
 

            URL url_post = new URL(loginURL2);
            connection = (HttpsURLConnection) url_post.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");

            //String query = "spEntityID=https%3A%2F%2Fmemberservices.optuszoo.com.au%2Fshibboleth&j_username=david.turner10&j_password=Surf2day%21%21&rememberMe=on&j_principal_type=ISP&j_security_check=true";
            connection.setRequestProperty("Content-length", String.valueOf(loginPostQuery.length()));
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setRequestProperty("User-Agent", userAgent);
            connection.setRequestProperty("Referer", loginReferer);

            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
            wr.write(loginPostQuery);
            wr.flush();
            wr.close();


            // java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO,"\r\nResp Code:" + connection.getResponseCode());
            // java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO,"\r\nResp Message:" + connection.getResponseMessage());
            // java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO,"\r\n\r\n");


            //-------------------------------------------------------------------
            //   Read the reponse
            //-------------------------------------------------------------------


            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String conInputLine;

            while ((conInputLine = rd.readLine()) != null) {
                //     java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO,conInputLine);
                result.setResultData(result.getResultData() + "\r\n" + conInputLine);
            }
            // Get the cookies and session id
            cookies = connection.getHeaderField("Set-Cookie");
            headMap = connection.getHeaderFields();
            List cookieList = headMap.get("Set-Cookie");
            String cook = "";
            cookies = "";
            for (Object o : cookieList) {
                cook = (String) o;
                cookies += cook + ";";
                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, "\r\nCookie:{0}\r\n", cook);
                result.setResultData(result.getResultData() + "\r\n" + cook);
            }
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, "\r\nResponse Cookies\r\n{0}\r\n\r\n", cookies);

            rd.close();
        } catch (IOException iOException) {
            try {

                int respCode = ((HttpURLConnection) connection).getResponseCode();
                result.setResultCode(respCode);
                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, "Processing Exception, response code :  {0}\r\n", respCode);
                BufferedReader es = new BufferedReader(new InputStreamReader(((HttpURLConnection) connection).getErrorStream()));
                int ret = 0;
                // read the response body
                String ln;

                while ((ln = es.readLine()) != null) {
                    java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, ln);
                    result.setResultData(result.getResultData() + "\r\n" + ln);
                }
                // close the errorstream
                es.close();
            } catch (IOException ex) {
                // deal with the exception
                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, "Processing Exception in exception block\r\n{0}\r\n", ex.getMessage());
                result.setResultData(result.getResultData() + "\r\n" + ex);
            }

        }
        loginTime = new GregorianCalendar().getTimeInMillis() - loginTimer.getTimeInMillis();
        testTimer = new GregorianCalendar();
        String dt = "";
        try {
            URL url2 = new URL(testURL1);
            HttpsURLConnection connection2 = (HttpsURLConnection) url2.openConnection();
            InputStream is = connection2.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String lineThatWasRead = "";
            while ((lineThatWasRead = br.readLine()) != null) {
                //     java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO,conInputLine);
                result.setResultData(result.getResultData() + "\r\n" + lineThatWasRead);
                if(lineThatWasRead.indexOf(testOKCriteria1) != -1 || lineThatWasRead.indexOf(testOKCriteria2) != -1){
                    result.setIsSuccessful(true);
                }
            }

        } catch (IOException iOException) {
            try {

                int respCode = ((HttpURLConnection) connection).getResponseCode();
                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, "Processing Exception, response code :  {0}\r\n", respCode);
                BufferedReader es = new BufferedReader(new InputStreamReader(((HttpURLConnection) connection).getErrorStream()));
                int ret = 0;
                // read the response body
                String ln;

                while ((ln = es.readLine()) != null) {
                    java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, ln);
                }
                // close the errorstream
                es.close();
            } catch (IOException ex) {
                // deal with the exception
                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, "Processing Exception in exception block\r\n{0}\r\n", ex.getMessage());
                result.setResultData(result.getResultData() + "\r\n" + ex);
            }

        }
        testTime = new GregorianCalendar().getTimeInMillis() - testTimer.getTimeInMillis() + loginTime;
        try {
            URL url2 = new URL(logoutURL1);
            HttpsURLConnection connection2 = (HttpsURLConnection) url2.openConnection();
            InputStream is = connection2.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String lineThatWasRead = "";
            while ((lineThatWasRead = br.readLine()) != null) {
                //     java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO,conInputLine);
                result.setResultData(result.getResultData() + "\r\n" + lineThatWasRead);
            }

         result.setResultData("Login duration (ms) = "+ loginTime + ", Test duration (ms) = " + testTime +  "\r\n" + result.getResultData());
         result.setLongResult1(loginTime);
         result.setLongResult2(testTime);
        } catch (IOException iOException) {
            try {

                int respCode = ((HttpURLConnection) connection).getResponseCode();
                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, "Processing Exception, response code :  {0}\r\n", respCode);
                BufferedReader es = new BufferedReader(new InputStreamReader(((HttpURLConnection) connection).getErrorStream()));
                int ret = 0;
                // read the response body
                String ln;

                while ((ln = es.readLine()) != null) {
                    java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, ln);
                }
                // close the errorstream
                es.close();
            } catch (IOException ex) {
                // deal with the exception
                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, "Processing Exception in exception block\r\n{0}\r\n", ex.getMessage());
                result.setResultData(result.getResultData() + "\r\n" + ex);
            }

        }

        return result;
    }

    /**
     * @return the paramMap
     */
    public JobDataMap getParamMap() {
        return paramMap;
    }

    /**
     * @param paramMap the paramMap to set
     */
    public void setParamMap(JobDataMap paramMap) {
        this.paramMap = paramMap;
    }
}
