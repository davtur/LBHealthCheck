/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.callable;

/**
 *
 * @author david
 */
public class CallableTaskResults {
    private boolean isSuccessful = false;
    private String resultData = "";
    private int resultCode = -1;
    private Object resultObject = null;
    private long  longResult1 = -1;
     private long  longResult2 = -1;
      private long  longResult3 = -1;

    /**
     * @return the isSuccessful
     */
    public boolean isIsSuccessful() {
        return isSuccessful;
    }

    /**
     * @param isSuccessful the isSuccessful to set
     */
    public void setIsSuccessful(boolean isSuccessful) {
        this.isSuccessful = isSuccessful;
    }

    /**
     * @return the resultData
     */
    public String getResultData() {
        return resultData;
    }

    /**
     * @param resultData the resultData to set
     */
    public void setResultData(String resultData) {
        this.resultData = resultData;
    }

    /**
     * @return the resultCode
     */
    public int getResultCode() {
        return resultCode;
    }

    /**
     * @param resultCode the resultCode to set
     */
    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }

    /**
     * @return the resultObject
     */
    public Object getResultObject() {
        return resultObject;
    }

    /**
     * @param resultObject the resultObject to set
     */
    public void setResultObject(Object resultObject) {
        this.resultObject = resultObject;
    }

    /**
     * @return the longResult1
     */
    public long getLongResult1() {
        return longResult1;
    }

    /**
     * @param longResult1 the longResult1 to set
     */
    public void setLongResult1(long longResult1) {
        this.longResult1 = longResult1;
    }

    /**
     * @return the longResult2
     */
    public long getLongResult2() {
        return longResult2;
    }

    /**
     * @param longResult2 the longResult2 to set
     */
    public void setLongResult2(long longResult2) {
        this.longResult2 = longResult2;
    }

    /**
     * @return the longResult3
     */
    public long getLongResult3() {
        return longResult3;
    }

    /**
     * @param longResult3 the longResult3 to set
     */
    public void setLongResult3(long longResult3) {
        this.longResult3 = longResult3;
    }
    
}
