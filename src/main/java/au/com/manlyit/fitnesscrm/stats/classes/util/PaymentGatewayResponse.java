/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

/**
 *
 * @author david
 */
public class PaymentGatewayResponse {
    
    private Object data;
    private String textData;
    private String errorCode;
    private String errorMessage;
    private boolean operationSuccessful;
    
    public PaymentGatewayResponse(boolean isOperationSuccessful,Object data,String textData,String errorCode,String errorMessage){
        this.data = data;
        this.textData = textData;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.operationSuccessful = isOperationSuccessful;
    }

    /**
     * @return the data
     */
    public Object getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(Object data) {
        this.data = data;
    }

    /**
     * @return the errorCode
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * @param errorCode the errorCode to set
     */
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * @return the errorMessage
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @param errorMessage the errorMessage to set
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * @return the isOperationSuccessful
     */
    public boolean isOperationSuccessful() {
        return operationSuccessful;
    }

    /**
     * @param isOperationSuccessful the isOperationSuccessful to set
     */
    public void setOperationSuccessful(boolean isOperationSuccessful) {
        this.operationSuccessful = isOperationSuccessful;
    }

    /**
     * @return the textData
     */
    public String getTextData() {
        return textData;
    }

    /**
     * @param textData the textData to set
     */
    public void setTextData(String textData) {
        this.textData = textData;
    }
    
    
}
