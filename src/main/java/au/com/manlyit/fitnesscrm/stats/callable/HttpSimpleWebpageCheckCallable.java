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
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;


/**
 *
 * @author david
 */
public class HttpSimpleWebpageCheckCallable implements Callable<CallableTaskResults> {

    private final JobDataMap paramMap;
 
    public HttpSimpleWebpageCheckCallable(JobDataMap parameters) {
        this.paramMap = parameters;
    }

    @Override
    public CallableTaskResults call() {

        return run_url_check();
    }

    private synchronized CallableTaskResults run_url_check() {
        //       jTextArea1.setText("");
        HttpURLConnection connection = null;
        CallableTaskResults result = new CallableTaskResults();
        java.net.CookieManager cm = new java.net.CookieManager();
        java.net.CookieHandler.setDefault(cm);
        cm.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        String url1 = paramMap.getString("url1");
        String successIfFound = paramMap.getString("successIfFound");
        //-------------------------------------------------------------------
        //   Post login info to  login page
        //-------------------------------------------------------------------
        result.setResultData("Beginning Login");


        String dt = "";
        try {
            URL url2 = new URL(url1);
             connection = (HttpURLConnection) url2.openConnection();
            InputStream is = connection.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String lineThatWasRead = "";
            while ((lineThatWasRead = br.readLine()) != null) {
                //     java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO,conInputLine);
                result.setResultData(result.getResultData() + "\r\n" + lineThatWasRead);
                if(lineThatWasRead.indexOf(successIfFound) != -1 ){
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
 

        return result;
    }
}
