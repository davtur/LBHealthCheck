package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade;
import au.com.manlyit.fitnesscrm.stats.db.EmailQueue;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.PaginationHelper;
import au.com.manlyit.fitnesscrm.stats.beans.EmailQueueFacade;
import au.com.manlyit.fitnesscrm.stats.classes.util.SendHTMLEmailWithFileAttached;
import au.com.manlyit.fitnesscrm.stats.db.Activation;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.EmailAttachments;
import au.com.manlyit.fitnesscrm.stats.db.EmailTemplates;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.faces.application.FacesMessage;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import static javax.ws.rs.client.Entity.entity;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TransferEvent;
import org.primefaces.model.DualListModel;
import org.primefaces.model.UploadedFile;

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
    private au.com.manlyit.fitnesscrm.stats.beans.EmailAttachmentsFacade ejbEmailAttachmentsFacade;
    @Inject
    private ConfigMapFacade configMapFacade;
    private PaginationHelper pagination;
    private int selectedItemIndex;
    private String templateName;
    private EmailTemplates selectedTemplate;
    private ArrayList<EmailAttachments> emailAttachmentsList;
    private String templateDescription;
    private String messagePreview;
    private UploadedFile file;
    private DualListModel<Customers> participants;

    public EmailQueueController() {
    }

    public EmailQueue getSelected() {
        if (current == null) {
            current = new EmailQueue();
            current.setSendDate(new Date());
            current.setFromaddress(configMapFacade.getConfig("PasswordResetFromEmailAddress"));
            current.setCcaddresses(configMapFacade.getConfig("PasswordResetCCEmailAddress"));
            current.setBccaddresses("");
            current.setSubject(getSelectedTemplate().getSubject());
            current.setMessage("");
            
            

            selectedItemIndex = -1;
        }
        return current;
    }
     public void upload() {
        if(file != null) {
            FacesMessage message = new FacesMessage("Succesful", file.getFileName() + " is uploaded.");
            FacesContext.getCurrentInstance().addMessage(null, message);
        }
    }
     
    public void handleFileUpload(FileUploadEvent event) {
        EmailAttachments ea = new EmailAttachments();
        ea.setId(0);
        ea.setFile(event.getFile().getContents());
        ea.setFileName(event.getFile().getFileName());
        
        emailAttachmentsList.add(ea);
        FacesMessage msg = new FacesMessage("Succesful", event.getFile().getFileName() + " is uploaded.");
        FacesContext.getCurrentInstance().addMessage(null, msg);
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
        current = null;
        
        return "CreateEmail";
    }

    public String create() {
        try {
            current.setId(0);
            current.setStatus(0);
            current.setCreateDate(new Date());
            List<Customers> targets = participants.getTarget();
            current.setToaddresses(getCSVListOfCustomerEmailAddresses(targets));

            // use a template
            /*  String templateLinkPlaceholder = configMapFacade.getConfig("login.password.reset.templateLinkPlaceholder");
             //   String templateTemporaryPasswordPlaceholder = configMapFacade.getConfig("login.password.reset.templateTemporaryPasswordPlaceholder");
              //  String templateUsernamePlaceholder = configMapFacade.getConfig("login.password.reset.templateUsernamePlaceholder");
                //String htmlText = configMapFacade.getConfig(templateName);
                String htmlText = ejbEmailTemplatesFacade.findTemplateByName(templateName).getTemplate();
               

                htmlText = htmlText.replace(templateLinkPlaceholder, current.getMessage());
               current.setMessage(htmlText);*/
            getFacade().create(current);
            for(EmailAttachments ea: emailAttachmentsList){
                ea.setEmailQueue(current);
                ejbEmailAttachmentsFacade.create(ea);
            }

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
            // all items were removed - go back to listtemplateName
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

    public void selectOneMenuValueChangeListener(ValueChangeEvent vce) {
        Object template = vce.getNewValue();
        if (template.getClass() == EmailTemplates.class) {
            setSelectedTemplate((EmailTemplates) template);
        }
    }
    
     public void editorValueChangeListener(ValueChangeEvent vce) {
        Object template = vce.getNewValue();
        if (template.getClass() == String.class) {
            PrimeFaces.current().ajax().update("@(.emailPreview) ");
        }
    }

    /**
     * @return the messagePreview
     */
    public String getMessagePreview() {

        String templateLinkPlaceholder = configMapFacade.getConfig("login.password.reset.templateLinkPlaceholder");
        //   String templateTemporaryPasswordPlaceholder = configMapFacade.getConfig("login.password.reset.templateTemporaryPasswordPlaceholder");
        //  String templateUsernamePlaceholder = configMapFacade.getConfig("login.password.reset.templateUsernamePlaceholder");
        //String htmlText = configMapFacade.getConfig(templateName);
        String htmlText = "";
        if (getSelectedTemplate() != null) {
            htmlText = getSelectedTemplate().getTemplate();
        }
        if (htmlText == null) {
            EmailTemplates selTemp = ejbEmailTemplatesFacade.findTemplateByName(getTemplateName());
            if (selTemp != null) {
                htmlText = selTemp.getTemplate();
            } else {
                return getSelected().getMessage();
            }

        }

        messagePreview = htmlText.replace(templateLinkPlaceholder, getSelected().getMessage());

        return messagePreview;
    }

    /**
     * @param messagePreview the messagePreview to set
     */
    public void setMessagePreview(String messagePreview) {
        this.messagePreview = messagePreview;
    }

    /**
     * @return the selectedTemplate
     */
    public EmailTemplates getSelectedTemplate() {
        if (selectedTemplate == null) {
            selectedTemplate = ejbEmailTemplatesFacade.findTemplateByName(getTemplateName());
            selectedItemIndex = -1;
        }
        return selectedTemplate;
    }

    /**
     * @param selectedTemplate the selectedTemplate to set
     */
    public void setSelectedTemplate(EmailTemplates selectedTemplate) {
        this.selectedTemplate = selectedTemplate;
    }

    public void onTransfer(TransferEvent event) {
        List<Customers> targets = participants.getTarget();

        getSelected().setBccaddresses(getCSVListOfCustomerEmailAddresses(targets));
        
      /*  StringBuilder builder = new StringBuilder();
        boolean isFirst = true;
        for (Object item : event.getItems()) {
            if (isFirst) {
                builder.append(((Customers) item).getEmailAddress());
                isFirst = false;
            } else {
                builder.append(",");
                builder.append(((Customers) item).getEmailAddress());
            }
        }

        getSelected().setBccaddresses(builder.toString());*/

        
        /*FacesMessage msg = new FacesMessage();
        msg.setSeverity(FacesMessage.SEVERITY_INFO);
        msg.setSummary("Items Transferred");
        msg.setDetail(builder.toString());
         
        FacesContext.getCurrentInstance().addMessage(null, msg);*/
    }

    public void sendTheBulkEmail() {

        //send email
        List<Customers> targets = participants.getTarget();

        //current.setBccaddresses(getCSVListOfCustomerEmailAddresses(targets));
        current.setToaddresses(current.getCcaddresses());
        current.setId(0);
        current.setStatus(0);
        current.setCreateDate(new Date());

        boolean valid = true;
        String message = "";
        String[] toAddressesArray = null;
        if (current.getToaddresses() == null) {
            valid = false;
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "To email addresses is NULL");
            message = "To email addresses are empty!";

        } else {
            toAddressesArray = current.getToaddresses().split(",");
            for (String s : toAddressesArray) {
                if (validateEmailAddress(s) == false) {
                    valid = false;
                    Logger.getLogger(getClass().getName()).log(Level.WARNING, "One or more To email addresses are invalid:{0}", s);
                    message = "One or more To email addresses are invalid:" + s;
                }
            }
        }
        if (current.getFromaddress() == null) {
            valid = false;
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "From email addresses is NULL");
            message = "From email addresses are empty!";

        }

        if (current.getBccaddresses() != null) {
            String[] bccAddressesArray = current.getBccaddresses().split(",");
            for (String s : bccAddressesArray) {
                if (validateEmailAddress(s) == false) {
                    valid = false;
                    Logger.getLogger(getClass().getName()).log(Level.WARNING, "One or more BCC email addresses are invalid:{0}", s);
                    message = "One or more BCC email addresses are invalid:" + s;
                }
            }
        }
        if (validateEmailAddress(current.getFromaddress()) == false) {
            valid = false;
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "From email address is invalid");
            message = "From email address is invalid:" + current.getFromaddress();
        }
        if (valid) {
            current.setMessage(getMessagePreview());
            getFacade().create(current);
            for(EmailAttachments ea: emailAttachmentsList){
                ea.setEmailQueue(current);
                ejbEmailAttachmentsFacade.create(ea);
                current.getEmailAttachmentCollection().add(ea);
                getFacade().edit(current);
            }
            current = null;
            participants = null;
            setMessagePreview("");
            JsfUtil.addSuccessMessage("Your email has been put in the queue and will sent as soon as the send date is reached.");
        } else {
            JsfUtil.addErrorMessage(message);
        }

    }

    private boolean validateEmailAddress(String email) {

        Pattern p = Pattern.compile(".+@.+\\.[a-z]+");

        //Match the given string with the pattern
        Matcher m = p.matcher(email);

        //Check whether match is found
        return m.matches();

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
        String htmlText = "<table width=\"600\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">  <tr>    <td><img src=\"cid:logoimg_cid\"/></td>  </tr>  <tr>    <td height=\"220\"> <p>Manly Beach Female Fitness</p>      <p>Please click the following link to reset your password:</p><p>To reset your password click <a href=\"" + urlLink + "\">here</a>.</p></td>  </tr>  <tr>    <td height=\"50\" align=\"center\" valign=\"middle\" bgcolor=\"#CCCCCC\">www.manlybeachfemalefitness.com.au | sarah@manlybeachfemalefitness.com.au | +61433818067</td>  </tr></table>";
// TODO String htmlText = ejbEmailTemplatesFacade.findTemplateByName(templateName).getTemplate();
        //String host, String to, String ccAddress, String from, String emailSubject, String message, String theAttachedfileName, boolean debug
        emailAgent.send("david@manlyit.com.au", "", "", "noreply@manlybeachfemalefitness.com.au", "Password Reset", htmlText, null, emailServerProperties(), true,null);

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
        List<Customers> sourceParticipants = ejbCustomerFacade.findAllActiveCustomersAndStaff(true);
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
        if (templateName == null) {
            templateName = "system.email.admin.newsletter";
        }
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

    @FacesConverter(value = "emailQueueControllerConverter")
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

    /**
     * @return the file
     */
    public UploadedFile getFile() {
        return file;
    }

    /**
     * @param file the file to set
     */
    public void setFile(UploadedFile file) {
        this.file = file;
    }

    /**
     * @return the emailAttachmentsList
     */
    public ArrayList<EmailAttachments> getEmailAttachmentsList() {
        if(emailAttachmentsList == null){
            emailAttachmentsList = new ArrayList<>();
        }
        return emailAttachmentsList;
    }

    /**
     * @param emailAttachmentsList the emailAttachmentsList to set
     */
    public void setEmailAttachmentsList(ArrayList<EmailAttachments> emailAttachmentsList) {
        this.emailAttachmentsList = emailAttachmentsList;
    }

}
