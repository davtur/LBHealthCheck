package au.com.manlyit.fitnesscrm.stats.classes.util;

import au.com.manlyit.fitnesscrm.stats.classes.CustomersController;
import au.com.manlyit.fitnesscrm.stats.db.AuditLog;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.model.SelectItem;
import org.apache.commons.lang.StringEscapeUtils;
import org.primefaces.push.EventBus;
import org.primefaces.push.EventBusFactory;

public class JsfUtil implements Serializable{

    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbCustomerFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.AuditLogFacade ejbAuditLogFacade;

    public static SelectItem[] getSelectItems(List<?> entities, boolean selectOne) {
        int size = selectOne ? entities.size() + 1 : entities.size();
        SelectItem[] items = new SelectItem[size];
        int i = 0;
        if (selectOne) {
            items[0] = new SelectItem("", "---");
            i++;
        }
        for (Object x : entities) {
            items[i++] = new SelectItem(x, x.toString());
        }
        return items;
    }

    public static SelectItem[] getSelectItemsCustomersById(List<?> entities, boolean selectOne) {
        int size = selectOne ? entities.size() + 1 : entities.size();
        SelectItem[] items = new SelectItem[size];
        int i = 0;
        if (selectOne) {
            items[0] = new SelectItem("", "---");
            i++;
        }
        try {
            for (Object x : entities) {
                Customers cust = (Customers) x;
                items[i++] = new SelectItem(cust.getId(), cust.toString());
            }
        } catch (Exception e) {
            addErrorMessage(e, "JSFUtil.getSelectItemsCustomersById - Object returned from DB is not a Customer!");
        }
        return items;
    }

    public static void addErrorMessage(Exception ex, String defaultMsg) {
        String msg = ex.getLocalizedMessage();
        if (msg != null && msg.length() > 0) {
            addErrorMessage(msg);
        } else {
            addErrorMessage(defaultMsg);
        }
        Logger.getLogger(JsfUtil.class.getName()).severe(defaultMsg);
        Logger.getLogger(JsfUtil.class.getName()).severe(ex.getMessage());
    }

    public static void addErrorMessage(Exception ex, String summary, String defaultMsg) {
        String msg = ex.getLocalizedMessage();
        if (msg != null && msg.length() > 0) {
            msg = msg + "; " + defaultMsg;
            addErrorMessage(summary, msg);
        } else {
            addErrorMessage(summary, defaultMsg);
        }
        Logger.getLogger(JsfUtil.class.getName()).severe(defaultMsg);
        Logger.getLogger(JsfUtil.class.getName()).severe(ex.getMessage());
    }

    public static void addErrorMessages(List<String> messages) {
        for (String message : messages) {
            addErrorMessage(message);
        }
    }

    public static void addErrorMessage(String msg) {
        FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR,"Error" , msg);
        FacesContext.getCurrentInstance().addMessage(null, facesMsg);
        Logger.getLogger(JsfUtil.class.getName()).severe(msg);
    }

    public static void addErrorMessage(String summary, String message) {
        FacesMessage facesMsg;
        if (summary.contains(message)) {
            facesMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null);
        } else {
            facesMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, message);
        }

        FacesContext.getCurrentInstance().addMessage(null, facesMsg);
        String msg = summary + "; " + message;
        Logger.getLogger(JsfUtil.class.getName()).severe(msg);
    }

    public static void addSuccessMessage(String msg) {
        FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Notification", msg);
        FacesContext.getCurrentInstance().addMessage("successInfo", facesMsg);
    }

    public static void addSuccessMessage(String summary, String message) {
        FacesMessage facesMsg;
        if (summary.contains(message)) {
            facesMsg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Notification", message);
        } else {
            facesMsg = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, message);
        }
        FacesContext.getCurrentInstance().addMessage("successInfo", facesMsg);
    }

    public static void pushSuccessMessage(String channel, String summary, String detail) {
        EventBus eventBus = EventBusFactory.getDefault().eventBus();
        eventBus.publish(channel, new FacesMessage(FacesMessage.SEVERITY_INFO, StringEscapeUtils.escapeHtml(summary), StringEscapeUtils.escapeHtml(detail)));
    }

    public static void pushErrorMessage(String channel, String summary, String detail) {
        EventBus eventBus = EventBusFactory.getDefault().eventBus();
        eventBus.publish(channel, new FacesMessage(FacesMessage.SEVERITY_ERROR, StringEscapeUtils.escapeHtml(summary), StringEscapeUtils.escapeHtml(detail)));
    }

    public static String getRequestParameter(String key) {
        return FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(key);
    }

    public static Object getObjectFromRequestParameter(String requestParameterName, Converter converter, UIComponent component) {
        String theId = JsfUtil.getRequestParameter(requestParameterName);
        return converter.getAsObject(FacesContext.getCurrentInstance(), component, theId);
    }

   /* public synchronized int getUser() {
       return ejbCustomerFacade.findCustomerByUsername(FacesContext.getCurrentInstance().getExternalContext().getRemoteUser()).getId();
    }*/

    /*public synchronized Customers getCustomer() {
        Customers cust;
        FacesContext context = FacesContext.getCurrentInstance();
        CustomersController custController = (CustomersController) context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
        cust = ejbCustomerFacade.findCustomerByUsername(custController.getSelected().getUsername());

        return cust;
    }*/

    public static boolean isValidationFailed() {
        return FacesContext.getCurrentInstance().isValidationFailed();
    }

    public static boolean isAdminUser() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        boolean isAdmin = facesContext.getExternalContext().isUserInRole("ADMIN");
        return isAdmin;
    }

   

}
