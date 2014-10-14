package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade;
import au.com.manlyit.fitnesscrm.stats.db.EmailQueue;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.PaginationHelper;
import au.com.manlyit.fitnesscrm.stats.beans.EmailQueueFacade;
import au.com.manlyit.fitnesscrm.stats.classes.util.SendHTMLEmailWithFileAttached;
import au.com.manlyit.fitnesscrm.stats.db.Activation;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import javax.inject.Inject;
import javax.faces.application.FacesMessage;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DualListModel;

@Named("emailQueueController")
@SessionScoped
public class EmailQueueController implements Serializable {

    private EmailQueue current;
    private DataModel items = null;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.EmailQueueFacade ejbFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbCustomerFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ActivationFacade ejbActivationFacade;
     @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.EmailTemplatesFacade ejbEmailTemplatesFacade;
    @Inject
    private ConfigMapFacade configMapFacade;
    private PaginationHelper pagination;
    private int selectedItemIndex;
    private String templateName;
    private String templateDescription;
    private DualListModel<Customers> participants;

    public EmailQueueController() {
    }

    public EmailQueue getSelected() {
        if (current == null) {
            current = new EmailQueue();
            selectedItemIndex = -1;
        }
        return current;
    }

    private EmailQueueFacade getFacade() {
        return ejbFacade;
    }

    public static boolean isUserInRole(String roleName) {
        boolean inRole = false;
        inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;

    }

    protected Properties emailServerProperties() {
        Properties props = new Properties();
        
        props.put("mail.smtp.host", configMapFacade.getConfig("mail.smtp.host"));
        props.put("mail.smtp.auth", configMapFacade.getConfig("mail.smtp.auth"));
        props.put("mail.debug", configMapFacade.getConfig("mail.debug"));
        props.put("mail.smtp.port", configMapFacade.getConfig("mail.smtp.port"));
        props.put("mail.smtp.socketFactory.port", configMapFacade.getConfig("mail.smtp.socketFactory.port"));
        props.put("mail.smtp.socketFactory.class", configMapFacade.getConfig("mail.smtp.socketFactory.class"));
        props.put("mail.smtp.socketFactory.fallback", configMapFacade.getConfig("mail.smtp.socketFactory.fallback"));
        props.put("mail.smtp.ssluser", configMapFacade.getConfig("mail.smtp.ssluser"));
        props.put("mail.smtp.sslpass", configMapFacade.getConfig("mail.smtp.sslpass"));

        return props;

    }

    public PaginationHelper getPagination() {
        if (pagination == null) {
            pagination = new PaginationHelper(1000000) {

                @Override
                public int getItemsCount() {
                    return getFacade().count();
                }

                @Override
                public DataModel createPageDataModel() {
                    return new ListDataModel(getFacade().findRange(new int[]{getPageFirstItem(), getPageFirstItem() + getPageSize()}));
                }
            };
        }
        return pagination;
    }

    public String prepareList() {
        recreateModel();
        return "List";
    }

    public String prepareView() {
        current = (EmailQueue) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "View";
    }

    public String prepareCreate() {
        current = new EmailQueue();
        selectedItemIndex = -1;
        addParticipants();
        current.setFromaddress("noreply@purefitnessmanly.com.au");
        return "Create";
    }

    public String create() {
        try {
            current.setId(0);
            current.setStatus(0);
            current.setCreateDate(new Date());
            List<Customers> targets = participants.getTarget();
            current.setToaddresses(getCSVListOfCustomerEmailAddresses(targets));

            getFacade().create(current);

            JsfUtil.addSuccessMessage(configMapFacade.getConfig("EmailQueueCreated"));
            return prepareCreate();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }
 
    public void handleDateSelect(SelectEvent event) {
        Date date = (Date) event.getObject();
        SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy HH:mm");

        FacesContext facesContext = FacesContext.getCurrentInstance();
        facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Date Selected", format.format(date)));
    }

    public String prepareEdit() {
        current = (EmailQueue) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "Edit";
    }

    public String update() {
        try {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("EmailQueueUpdated"));
            return "View";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public String destroy() {
        current = (EmailQueue) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        performDestroy();
        recreateModel();
        return "List";
    }

    public String destroyAndView() {
        performDestroy();
        recreateModel();
        updateCurrentItem();
        if (selectedItemIndex >= 0) {
            return "View";
        } else {
            // all items were removed - go back to list
            recreateModel();
            return "List";
        }
    }

    private void performDestroy() {
        try {
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("EmailQueueDeleted"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
    }

    private void updateCurrentItem() {
        int count = getFacade().count();
        if (selectedItemIndex >= count) {
            // selected index cannot be bigger than number of items:
            selectedItemIndex = count - 1;
            // go to previous page if last page disappeared:
            if (pagination.getPageFirstItem() >= count) {
                pagination.previousPage();
            }
        }
        if (selectedItemIndex >= 0) {
            current = getFacade().findRange(new int[]{selectedItemIndex, selectedItemIndex + 1}).get(0);
        }
    }

    private String getCSVListOfCustomerEmailAddresses(List<Customers> targets) {
        String toAddresses = "";
        for (Customers custmr : targets) {
            if (toAddresses.length() > 0) {
                toAddresses += "," + custmr.getEmailAddress();
            } else {
                toAddresses = custmr.getEmailAddress();
            }
        }
        return toAddresses;
    }

    public void sendTheEmail() {

        List<Customers> targets = participants.getTarget();

        current.setToaddresses(getCSVListOfCustomerEmailAddresses(targets));
        current.setId(0);
        current.setStatus(0);
        current.setCreateDate(new Date());
        getFacade().create(current);

        SendHTMLEmailWithFileAttached emailAgent = new SendHTMLEmailWithFileAttached();
        emailAgent.send(current.getToaddresses(), current.getCcaddresses(), current.getFromaddress(), current.getSubject(), current.getMessage(), null,emailServerProperties(), false);

    }

    public void sendTestEmail() {
        Customers cust = ejbCustomerFacade.findCustomerByUsername("david.turner");

        List<Customers> targets = participants.getTarget();

        String toAddresses = "";
        for (Customers custmr : targets) {
            if (toAddresses.length() > 0) {
                toAddresses += "," + custmr.getEmailAddress();
            } else {
                toAddresses = custmr.getEmailAddress();
            }

        }

        //valid user that wants the password reset
        //generate link and send
        String nonce = "7777" + generateUniqueToken(10);
        Activation act = new Activation(0, nonce, new Date());
        act.setCustomer(cust);
        String urlLink = configMapFacade.getConfig("PasswordResetURL");
        urlLink += nonce;
        ejbActivationFacade.create(act);

        SendHTMLEmailWithFileAttached emailAgent = new SendHTMLEmailWithFileAttached();
        String htmlText = "<table width=\"600\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">  <tr>    <td><img src=\"cid:logoimg_cid\"/></td>  </tr>  <tr>    <td height=\"220\"> <p>Pure Fitness Manly</p>      <p>Please click the following link to reset your password:</p><p>To reset your password click <a href=\"" + urlLink + "\">here</a>.</p></td>  </tr>  <tr>    <td height=\"50\" align=\"center\" valign=\"middle\" bgcolor=\"#CCCCCC\">www.purefitnessmanly.com.au | sarah@purefitnessmanly.com.au | +61433818067</td>  </tr></table>";
// TODO String htmlText = ejbEmailTemplatesFacade.findTemplateByName(templateName).getTemplate();
        //String host, String to, String ccAddress, String from, String emailSubject, String message, String theAttachedfileName, boolean debug
        emailAgent.send("david@manlyit.com.au", "", "noreply@purefitnessmanly.com.au", "Password Reset", htmlText, null,emailServerProperties(), true);

    }

    public static synchronized String generateUniqueToken(Integer length) {

        byte random[] = new byte[length];
        Random randomGenerator = new Random();
        StringBuilder buffer = new StringBuilder();

        randomGenerator.nextBytes(random);

        for (int j = 0; j < random.length; j++) {
            byte b1 = (byte) ((random[j] & 0xf0) >> 4);
            byte b2 = (byte) (random[j] & 0x0f);
            if (b1 < 10) {
                buffer.append((char) ('0' + b1));
            } else {
                buffer.append((char) ('A' + (b1 - 10)));
            }
            if (b2 < 10) {
                buffer.append((char) ('0' + b2));
            } else {
                buffer.append((char) ('A' + (b2 - 10)));
            }
        }

        return (buffer.toString());
    }

    private void addParticipants() {
        List<Customers> sourceParticipants = ejbCustomerFacade.findAll(true);
        List<Customers> targetParticipants = new ArrayList<Customers>();
        participants = new DualListModel<Customers>(sourceParticipants, targetParticipants);
    }

    /**
     * @return the participants
     */
    public DualListModel<Customers> getParticipants() {
        if (participants == null) {
            addParticipants();
        }
        return participants;
    }

    /**
     * @param participants the participants to set
     */
    public void setParticipants(DualListModel<Customers> participants) {
        this.participants = participants;
    }

    public DataModel getItems() {
        if (items == null) {
            items = getPagination().createPageDataModel();
        }
        return items;
    }

    private void recreateModel() {
        items = null;
    }

    public String next() {
        getPagination().nextPage();
        recreateModel();
        return "List";
    }

    public String previous() {
        getPagination().previousPage();
        recreateModel();
        return "List";
    }

    public SelectItem[] getItemsAvailableSelectMany() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), false);
    }

    public SelectItem[] getItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), true);
    }

    /**
     * @return the templateName
     */
    public String getTemplateName() {
        return templateName;
    }

    /**
     * @param templateName the templateName to set
     */
    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    /**
     * @return the templateDescription
     */
    public String getTemplateDescription() {
        return templateDescription;
    }

    /**
     * @param templateDescription the templateDescription to set
     */
    public void setTemplateDescription(String templateDescription) {
        this.templateDescription = templateDescription;
    }

    @FacesConverter(forClass = EmailQueue.class)
    public static class EmailQueueControllerConverter implements Converter {

        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            EmailQueueController controller = (EmailQueueController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "emailQueueController");
            return controller.ejbFacade.find(getKey(value));
        }

        java.lang.Integer getKey(String value) {
            java.lang.Integer key;
            key = Integer.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Integer value) {
            StringBuffer sb = new StringBuffer();
            sb.append(value);
            return sb.toString();
        }

        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof EmailQueue) {
                EmailQueue o = (EmailQueue) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + EmailQueueController.class.getName());
            }
        }
    }
}
