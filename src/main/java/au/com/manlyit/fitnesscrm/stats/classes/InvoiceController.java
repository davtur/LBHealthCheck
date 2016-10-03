package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.beans.CurrencyFacade;
import au.com.manlyit.fitnesscrm.stats.db.Invoice;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.PaginationHelper;
import au.com.manlyit.fitnesscrm.stats.beans.InvoiceFacade;
import au.com.manlyit.fitnesscrm.stats.beans.PaymentsFacade;
import au.com.manlyit.fitnesscrm.stats.beans.SessionHistoryFacade;
import au.com.manlyit.fitnesscrm.stats.beans.SessionTypesFacade;
import au.com.manlyit.fitnesscrm.stats.classes.util.PfSelectableDataModel;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.InvoiceLine;
import au.com.manlyit.fitnesscrm.stats.db.InvoiceLineType;
import au.com.manlyit.fitnesscrm.stats.db.PaymentParameters;
import au.com.manlyit.fitnesscrm.stats.db.Payments;
import au.com.manlyit.fitnesscrm.stats.db.Plan;
import au.com.manlyit.fitnesscrm.stats.db.SessionHistory;
import au.com.manlyit.fitnesscrm.stats.db.SessionTypes;
import com.lowagie.text.BadElementException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;

import com.lowagie.text.Paragraph;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.io.Serializable;
import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.faces.event.ValueChangeEvent;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.primefaces.context.RequestContext;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.RowEditEvent;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

@Named("invoiceController")
@SessionScoped
public class InvoiceController implements Serializable {

    private static final Logger logger = Logger.getLogger(InvoiceController.class.getName());
    private String htmlInvoiceSource = "";
    private String htmlInvoiceEmailToAddress = "";
    private String htmlInvoiceEmailEditSaveButtonText = "Edit";
    private boolean htmlInvoiceEmailToAddressEnabled = false;
    private Invoice current;
    private Invoice lastGeneratedInvoice;
    private InvoiceLine selectedInvoiceLineItem;
    private List<Invoice> invoiceFilteredItems;
    private List<InvoiceLine> invoiceLineFilteredItems;
    private PfSelectableDataModel items = null;
    private String invoiceFileName = "invoice-download";
    private Customers selectedCustomer;
    private Date invoiceStartDate;
    private Date invoiceEndDate;
    private boolean invoiceUseSettlementDate = false;
    private boolean invoiceShowSuccessful = true;
    private boolean invoiceShowFailed = false;
    private boolean invoiceShowPending = false;
    private boolean invoiceShowScheduled = false;
    private boolean invoiceGenerated = false;
    private int invoiceType = 0;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.InvoiceFacade ejbFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade;

    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.EmailQueueFacade emailQueueFacade;
    @Inject
    private SessionHistoryFacade sessionHistoryFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.InvoiceLineTypeFacade invoiceLineTypeFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.InvoiceLineFacade invoiceLineFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.InvoiceFacade invoiceFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.EmailTemplatesFacade ejbEmailTemplatesFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.PaymentBean ejbPaymentBean;
    @Inject
    private SessionTypesFacade sessionTypesFacade;
    @Inject
    private PaymentsFacade paymentsFacade;
    @Inject
    private CurrencyFacade currencyFacade;
    private PaginationHelper pagination;
    private int selectedItemIndex;

    public InvoiceController() {
    }

    @PostConstruct
    private void initDates() {

        GregorianCalendar cal = new GregorianCalendar();
        invoiceEndDate = cal.getTime();
        cal.add(Calendar.DAY_OF_YEAR, -28);
        invoiceStartDate = cal.getTime();
    }

    public static boolean isUserInRole(String roleName) {
        boolean inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;
    }

    public void invoiceParametersChanged() {
        JsfUtil.addSuccessMessage("Invoice Updated", "Successfully Updated Invoice Parameters");
        RequestContext.getCurrentInstance().update(":growl");
    }

    public void editEmailAddress() {
        htmlInvoiceEmailToAddressEnabled = !htmlInvoiceEmailToAddressEnabled;
        if (htmlInvoiceEmailToAddressEnabled) {
            htmlInvoiceEmailEditSaveButtonText = configMapFacade.getConfig("previewHtmlInvoiceSaveEmailLink");
        } else {
            htmlInvoiceEmailEditSaveButtonText = configMapFacade.getConfig("previewHtmlInvoiceEditEmailLink");
        }
    }

    public void generateInvoice() {
        try {
            current = generateInvoiceForCustomer(selectedCustomer, invoiceUseSettlementDate, invoiceShowSuccessful, invoiceShowFailed, invoiceShowPending, isInvoiceShowScheduled(), invoiceStartDate, invoiceEndDate);
            current.setCreateTimestamp(new Date());
            current.setCreateDatetime(invoiceStartDate);
            current.setStatusId(1);
            current.setDueDate(invoiceEndDate);
            current.setPaymentAttempts(0);
            current.setCarriedBalance(BigDecimal.ZERO);
            current.setInProcessPayment((short) 0);
            current.setIsReview(0);
            current.setCurrencyId(currencyFacade.find(10));
            current.setId(0);
            getFacade().create(current);
            invoiceLineFilteredItems = null;
            updateTableData(current);
            htmlInvoiceSource = generateHtmlInvoice(current);
            htmlInvoiceEmailToAddress = current.getUserId().getEmailAddress();
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("InvoiceCreated"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }

        //RequestContext.getCurrentInstance().update("InvoicelistForm1");
    }

    public Invoice generateInvoiceForCustomer(Customers cust, boolean useSettlementDate, boolean showSuccessful, boolean showFailed, boolean showPending, boolean showScheduled, Date startDate, Date endDate) {
        Invoice inv = new Invoice();
        inv.setInvoiceLineCollection(new ArrayList<>());
        inv.setUserId(cust);
//add plan line item to invoice with number of billable sessions
        InvoiceLineType ilt = invoiceLineTypeFacade.findAll().get(1);
        InvoiceLine il = new InvoiceLine(0);
        BigDecimal paymentsTotal;
        BigDecimal bankFeesTotal;
        PaymentParameters pp = cust.getPaymentParameters();
        String ppPlanPaymentDetails = "Unknown";
        if (pp != null) {
            try {
                String period = getPaymentPeriodString(pp.getPaymentPeriod());
                String dom = ", DOM: " + pp.getPaymentPeriodDayOfMonth() + " ";
                String dow = ", DOW: " + pp.getPaymentPeriodDayOfWeek();
                if (pp.getPaymentPeriodDayOfMonth().contentEquals("0")) {
                    dom = "";
                }

                ppPlanPaymentDetails = "Period: " + period + dom + dow;
            } catch (Exception e) {
                ppPlanPaymentDetails = "Unknown";
            }
        }
        BigDecimal productsAndServicesTotal = new BigDecimal(0);
        il.setTypeId(ilt);

        il.setQuantity(new BigDecimal(1));
        il.setDescription("Plan: " + cust.getGroupPricing().getPlanName());
        il.setPrice(cust.getGroupPricing().getPlanPrice());
        il.setAmount(cust.getGroupPricing().getPlanPrice());
        productsAndServicesTotal = productsAndServicesTotal.add(il.getAmount());
        inv.getInvoiceLineCollection().add(il);
        //get payments for customer
        List<Payments> pl = paymentsFacade.findPaymentsByDateRange(useSettlementDate, showSuccessful, showFailed, showPending, showScheduled, startDate, endDate, false, cust);
        //get sessions for customer
        List<SessionTypes> sessionTypesList = sessionTypesFacade.findAll();
        for (SessionTypes sessType : sessionTypesList) {
            List<Date> weeks = getWeeksInMonth(startDate, endDate);
            Date weekStart = startDate;
            int billableSessionsTotal = 0;
            for (Date weekEnd : weeks) {
                List<SessionHistory> sessions = sessionHistoryFacade.findSessionsByParticipantAndDateRange(cust, weekStart, weekEnd, true);

                //for each session type count the sessions and bill as a line item if they are not included in the plan
                int count = 0;

                for (SessionHistory sess : sessions) {
                    String type = sess.getSessionTypesId().getName();
                    if (type.contains(sessType.getName())) {
                        count++;
                        //total = total + sess.getSessionTypesId().
                    }
                }
                if (count > 0) {
                    billableSessionsTotal += checkSessionsAgainstPlanWeek(count, cust, sessType);
                }
                weekStart = weekEnd;
            }
            //add line item to invoice with number of billable sessions
            ilt = invoiceLineTypeFacade.findAll().get(2);
            il = new InvoiceLine(0);
            il.setTypeId(ilt);
            BigDecimal bdBillableSessionsTotal = new BigDecimal(billableSessionsTotal);
            Plan sessionPlan = sessType.getPlan();
            BigDecimal cdPrice = new BigDecimal(0);
            if (sessionPlan != null) {
                cdPrice = sessionPlan.getPlanPrice();
            }
            il.setQuantity(bdBillableSessionsTotal);
            il.setDescription(sessType.getName());
            il.setPrice(cdPrice);
            il.setAmount(bdBillableSessionsTotal.multiply(cdPrice));
            if (cdPrice.compareTo(new BigDecimal(0)) > 0 && bdBillableSessionsTotal.compareTo(new BigDecimal(0)) > 0) {
                inv.getInvoiceLineCollection().add(il);
                productsAndServicesTotal = productsAndServicesTotal.add(il.getAmount());
            }

        }
        //add payments line item to invoice with number of billable sessions
        ilt = invoiceLineTypeFacade.findAll().get(0);
        paymentsTotal = new BigDecimal(0);

        bankFeesTotal = new BigDecimal(0);
        for (Payments p : pl) {
            il = new InvoiceLine(0);
            il.setTypeId(ilt);

            int numberOfPayments = 0;

            BigDecimal fee = p.getTransactionFeeCustomer();
            if (fee == null) {
                fee = new BigDecimal(0);
            }
            paymentsTotal = paymentsTotal.add(p.getPaymentAmount());
            bankFeesTotal = bankFeesTotal.add(fee);
            numberOfPayments++;

            // add scheduled payment amounts 
            //il.setQuantity(new BigDecimal(numberOfPayments));
            il.setQuantity(new BigDecimal(1));
            il.setDescription("Payment(s)" + " --- " + ppPlanPaymentDetails);
            productsAndServicesTotal = productsAndServicesTotal.add(bankFeesTotal);
           // productsAndServicesTotal = productsAndServicesTotal.subtract(paymentsTotal);
            // il.setPrice(paymentsTotal);
            il.setPrice(p.getPaymentAmount());
            paymentsTotal = paymentsTotal.negate();
            //il.setAmount(paymentsTotal);
            il.setAmount(p.getPaymentAmount());

            // add line item for bank fees
            InvoiceLine il2 = new InvoiceLine(0);
            il2.setTypeId(ilt);
            //il2.setQuantity(new BigDecimal(numberOfPayments));
            il2.setQuantity(new BigDecimal(numberOfPayments));
            il2.setDescription("Bank Transaction Fees");
            //il2.setPrice(bankFeesTotal);
            // il2.setAmount(bankFeesTotal); 
            il2.setPrice(fee);
            il2.setAmount(fee);
            inv.getInvoiceLineCollection().add(il2);
            inv.getInvoiceLineCollection().add(il);
        }
        inv.setBalance(paymentsTotal);
        inv.setTotal(productsAndServicesTotal);
        return inv;
    }

    private String getPaymentPeriodString(String key) {
        String period = "";
        switch (key) {
            case "4":
                period = "4 Weekly";
                break;
            case "W":
                period = "Weekly";
                break;
            case "M":
                period = "Monthly";
                break;
            case "F":
                period = "Fortnightly";
                break;
            case "Z":
                period = "No Schedule";
                break;
            case "Q":
                period = "Quarterly";
                break;
            case "H":
                period = "Half Yearly";
                break;
            case "Y":
                period = "Annually";
                break;
            case "N":
                period = "Weekday In Month";
                break;
        }
        return period;
    }

    private List<Date> getWeeksInMonth(Date start, Date end) {
        List<Date> dl = new ArrayList<>();
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(start);
        while (gc.getTime().compareTo(end) <= 0) {
            gc.add(Calendar.DAY_OF_WEEK, 7);
            if (gc.getTime().compareTo(end) >= 0) {
                dl.add(end);
            } else {
                dl.add(gc.getTime());
            }

        }

        return dl;
    }

    private int checkSessionsAgainstPlanWeek(int count, Customers cust, SessionTypes sessType) {
        int billableSessions = 0;
        Plan plan = cust.getGroupPricing();
        PaymentParameters pp = cust.getPaymentParameters();
        String paymentPeriod = pp.getPaymentPeriod();
        List<Plan> plans = new ArrayList<>(plan.getPlanCollection());
        int includedInPlanCount = 0;
        for (Plan p : plans) {
            SessionTypes st = p.getSessionType();
            if (st != null) {
                if (sessType.getName().contentEquals(st.getName())) {
                    includedInPlanCount++;
                }
            }
        }
        billableSessions = count - includedInPlanCount;
        if (billableSessions < 0) {
            billableSessions = 0;
        }

        /* if (paymentPeriod.contentEquals(PaymentPeriod.MONTHLY.value())) {

         }
         if (paymentPeriod.contentEquals(PaymentPeriod.FOUR_WEEKLY.value())) {

         }
         if (paymentPeriod.contentEquals(PaymentPeriod.WEEKLY.value())) {

         }
         if (paymentPeriod.contentEquals(PaymentPeriod.FORTNIGHTLY.value())) {

         }*/
        logger.log(Level.INFO, "checkSessionsAgainstPlanWeek: Billed Sessions:{0}, Session Count:{1}, Customer: {2}, Plan: {5}, Session Type: {3}, Session Plan: {4}", new Object[]{billableSessions, count, cust.getUsername(), sessType.getName(), sessType.getPlan(), cust.getGroupPricing().getPlanName()});
        return billableSessions;
    }

    public void preProcessPDF(Object document) throws IOException,
            BadElementException, DocumentException {
        Document pdf = (Document) document;
        pdf.setPageSize(PageSize.A4.rotate());

        //FacesContext context = FacesContext.getCurrentInstance();
        //ExternalContext ec = context.getExternalContext();
        //HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
        String logo = servletContext.getContextPath() + File.separator + "resources" + File.separator + "images"
                + File.separator + "logo.png";
        URL imageResource = servletContext.getResource(File.separator + "resources" + File.separator + "images"
                + File.separator + "logo.png");
        pdf.open();
        String urlForLogo = configMapFacade.getConfig("system.email.logo");
        try {
            pdf.add(Image.getInstance(new URL(urlForLogo)));
        } catch (IOException | DocumentException iOException) {
            logger.log(Level.WARNING, "Logo URL error", iOException);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy");
        String type = "Invoice";
        if (invoiceType == 0) {
            type += " (GST Included)";
        }
        if (invoiceType == 1) {
            type += " (No GST) ";
        }
        String reportTitle = type + " invoice for the period " + sdf.format(invoiceStartDate) + " to " + sdf.format(invoiceEndDate);
        pdf.addTitle(reportTitle);
        pdf.add(new Paragraph(reportTitle));
        pdf.add(new Paragraph(" "));
        com.lowagie.text.Font font = new com.lowagie.text.Font(com.lowagie.text.Font.NORMAL);
        font.setSize(8);
    }

    public void postProcessPDF(Object document) throws IOException,
            BadElementException, DocumentException {

    }

    public void preProcessXLS(Object document) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy");

        String type = "Invoice";
        if (invoiceType == 0) {
            type += " (GST Included)";
        }
        if (invoiceType == 1) {
            type += " (No GST) ";
        }
        String reportTitle = type + " invoice for the period " + sdf.format(invoiceStartDate) + " to " + sdf.format(invoiceEndDate);
        HSSFWorkbook wb = (HSSFWorkbook) document;
        HSSFSheet sheet = wb.getSheetAt(0);
        HSSFRow row = sheet.createRow(0);
        HSSFCell cell = row.createCell(0);
        cell.setCellValue(reportTitle);
        HSSFCellStyle cellStyle = wb.createCellStyle();
        DataFormat df = wb.createDataFormat();
        cellStyle.setFillForegroundColor(HSSFColor.BLUE.index);
        cellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        for (int i = 1; i < row.getPhysicalNumberOfCells(); i++) {
            row.getCell(i).setCellStyle(cellStyle);
        }
        cellStyle.setDataFormat(df.getFormat("YYYY-MM-DD HH:MM:SS"));

        cell = row.createCell(1);
        cell.setCellValue(new Date());
        cell.setCellStyle(cellStyle);
        sheet.createRow(1);
        sheet.createRow(2);

    }

    public void postProcessXLS(Object document) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy");

        String type = "Invoice";
        if (invoiceType == 0) {
            type += " (GST Included)";
        }
        if (invoiceType == 1) {
            type += " (No GST) ";
        }
        String reportTitle = type + " report for the period " + sdf.format(invoiceStartDate) + " to " + sdf.format(invoiceEndDate);
        HSSFWorkbook wb = (HSSFWorkbook) document;
        HSSFSheet sheet = wb.getSheetAt(0);
        sheet.shiftRows(0, sheet.getLastRowNum(), 7);
        HSSFRow row = sheet.createRow(0);
        HSSFCell cell = row.createCell(0);
        HSSFCellStyle cellStyle = wb.createCellStyle();
        DataFormat df = wb.createDataFormat();
        cellStyle.setDataFormat(df.getFormat("YYYY-MM-DD HH:MM:SS"));
        cell.setCellValue(reportTitle);
        //first row (0-based),last row  (0-based),first column (0-based),last column  (0-based)
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));
        CellStyle style = wb.createCellStyle();
        CellStyle style2 = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.BLACK.getIndex());
        //style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setFillBackgroundColor(IndexedColors.AQUA.getIndex());
        style.setFillPattern(CellStyle.BIG_SPOTS);
        style2.setFillForegroundColor(IndexedColors.BLACK.getIndex());
        //style2.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style2.setFillBackgroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        style2.setFillPattern(CellStyle.BIG_SPOTS);

        HSSFCell cell3 = row.createCell(3);
        cell3.setCellValue(new Date());
        cell3.setCellStyle(cellStyle);
        //sheet.createRow(1);
        //sheet.createRow(2);
        boolean rowColor = false;
        for (Row row2 : sheet) {
            if (row2.getRowNum() > 2) {
                rowColor = !(rowColor);
                for (Cell cell2 : row2) {
                    //cell2.setCellValue(cell.getStringCellValue().toUpperCase());
                    if (rowColor) {
                        cell2.setCellStyle(style);
                    } else {
                        cell2.setCellStyle(style2);
                    }

                }
            }
        }
        for (int c = 0; c < row.getLastCellNum() + 1; c++) {
            sheet.autoSizeColumn(c);
        }
    }

    public Invoice getSelected() {
        /*if (current == null) {
         current = new Invoice();
         selectedItemIndex = -1;
         }*/
        return current;
    }

    public void setSelected(Invoice selected) {
        if (selected != null) {
            current = selected;
            selectedItemIndex = -1;
        }

    }

    private InvoiceFacade getFacade() {
        return ejbFacade;
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
        //current = (Invoice)getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "View";
    }

    public String prepareCreate() {
        current = new Invoice();
        selectedItemIndex = -1;
        return "Create";
    }

    public String create() {
        try {
            /*for(InvoiceLine il:current.getInvoiceLineCollection()){
             il.setInvoiceId(current);
             invoiceLineFacade.create(il);
             }*/
            if (current.getId() == null) {
                current.setId(0);
            }
            getFacade().create(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("InvoiceCreated"));
            return prepareCreate();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public void createDialogue(ActionEvent actionEvent) {
        try {
            current.setId(0);
            getFacade().create(current);
            setLastGeneratedInvoice(current);
            // add to existing lists rather than recreate entire model
            updateTableData(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("InvoiceCreated"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
    }

    private void updateTableData(Invoice inv) {
        if (inv == null) {
            logger.log(Level.WARNING, "updateTableData: Invoice is null! Nothing updated.");
            return;
        }
        // add to existing lists rather than recreate entire model
        if (items != null) {
            List<Invoice> il = (List<Invoice>) items.getWrappedData();
            il.add(inv);
            items.setWrappedData(il);
        }
        if (invoiceFilteredItems != null) {
            invoiceFilteredItems.add(inv);
        }
        // RequestContext.getCurrentInstance().execute("PF('invoiceControllerTable').filter();");

    }

    public String prepareEdit() {
        //current = (Invoice)getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "Edit";
    }

    public void selectOneMenuValueChangeListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
    }

    public void selectManyMenuValueChangeListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
    }

    public String update() {
        try {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("InvoiceUpdated"));
            return "View";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public void destroyDialogue() {
        if (current != null) {
            performDestroy();
            recreateModel();
            current = null;

        }
    }

    public String destroy() {
        current = (Invoice) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        destroyDialogue();
        return "List";
    }

    public String destroyAndView() {
        destroyDialogue();
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
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("InvoiceDeleted"));
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

    public PfSelectableDataModel getItems() {
        if (items == null) {
            items = new PfSelectableDataModel(getFacade().findAll());
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

    public void handleDateSelect(SelectEvent event) {

        Date date = (Date) event.getObject();

        //Add facesmessage
    }

    public void onEdit(RowEditEvent event) {
        Invoice cm = (Invoice) event.getObject();
        getFacade().edit(cm);
        recreateModel();
        JsfUtil.addSuccessMessage("Row Edit Successful");
    }

    public void onCancel(RowEditEvent event) {
        JsfUtil.addErrorMessage("Row Edit Cancelled");
    }

    /**
     * @return the invoiceFileName
     */
    public String getInvoiceFileName() {
        return invoiceFileName;
    }

    /**
     * @param invoiceFileName the invoiceFileName to set
     */
    public void setInvoiceFileName(String invoiceFileName) {
        this.invoiceFileName = invoiceFileName;
    }

    /**
     * @return the selectedCustomer
     */
    public Customers getSelectedCustomer() {
        return selectedCustomer;
    }

    /**
     * @param selectedCustomer the selectedCustomer to set
     */
    public void setSelectedCustomer(Customers selectedCustomer) {

        //current = new Invoice(0);
        //current.setUserId(selectedCustomer);
        current = null;
        htmlInvoiceSource = null;
        this.selectedCustomer = selectedCustomer;
    }

    /**
     * @return the invoiceStartDate
     */
    public Date getInvoiceStartDate() {
        return invoiceStartDate;
    }

    /**
     * @param invoiceStartDate the invoiceStartDate to set
     */
    public void setInvoiceStartDate(Date invoiceStartDate) {
        this.invoiceStartDate = invoiceStartDate;
    }

    /**
     * @return the invoiceEndDate
     */
    public Date getInvoiceEndDate() {
        return invoiceEndDate;
    }

    /**
     * @param invoiceEndDate the invoiceEndDate to set
     */
    public void setInvoiceEndDate(Date invoiceEndDate) {
        this.invoiceEndDate = invoiceEndDate;
    }

    /**
     * @return the invoiceUseSettlementDate
     */
    public boolean isInvoiceUseSettlementDate() {
        return invoiceUseSettlementDate;
    }

    /**
     * @param invoiceUseSettlementDate the invoiceUseSettlementDate to set
     */
    public void setInvoiceUseSettlementDate(boolean invoiceUseSettlementDate) {
        this.invoiceUseSettlementDate = invoiceUseSettlementDate;
    }

    /**
     * @return the invoiceShowSuccessful
     */
    public boolean isInvoiceShowSuccessful() {
        return invoiceShowSuccessful;
    }

    /**
     * @param invoiceShowSuccessful the invoiceShowSuccessful to set
     */
    public void setInvoiceShowSuccessful(boolean invoiceShowSuccessful) {
        this.invoiceShowSuccessful = invoiceShowSuccessful;
    }

    /**
     * @return the invoiceShowFailed
     */
    public boolean isInvoiceShowFailed() {
        return invoiceShowFailed;
    }

    /**
     * @param invoiceShowFailed the invoiceShowFailed to set
     */
    public void setInvoiceShowFailed(boolean invoiceShowFailed) {
        this.invoiceShowFailed = invoiceShowFailed;
    }

    /**
     * @return the invoiceShowPending
     */
    public boolean isInvoiceShowPending() {
        return invoiceShowPending;
    }

    /**
     * @param invoiceShowPending the invoiceShowPending to set
     */
    public void setInvoiceShowPending(boolean invoiceShowPending) {
        this.invoiceShowPending = invoiceShowPending;
    }

    /**
     * @return the invoiceShowScheduled
     */
    public boolean isInvoiceShowScheduled() {
        return invoiceShowScheduled;
    }

    /**
     * @param invoiceShowScheduled the invoiceShowScheduled to set
     */
    public void setInvoiceShowScheduled(boolean invoiceShowScheduled) {
        this.invoiceShowScheduled = invoiceShowScheduled;
    }

    /**
     * @return the invoiceFilteredItems
     */
    public List<Invoice> getInvoiceFilteredItems() {
        return invoiceFilteredItems;
    }

    /**
     * @param invoiceFilteredItems the invoiceFilteredItems to set
     */
    public void setInvoiceFilteredItems(List<Invoice> invoiceFilteredItems) {
        this.invoiceFilteredItems = invoiceFilteredItems;
    }

    /**
     * @return the lastGeneratedInvoice
     */
    public Invoice getLastGeneratedInvoice() {
        return lastGeneratedInvoice;
    }

    /**
     * @param lastGeneratedInvoice the lastGeneratedInvoice to set
     */
    public void setLastGeneratedInvoice(Invoice lastGeneratedInvoice) {
        this.lastGeneratedInvoice = lastGeneratedInvoice;
    }

    /**
     * @return the htmlInvoiceSource
     */
    public String getHtmlInvoiceSource() {
        if (htmlInvoiceSource == null || htmlInvoiceSource.isEmpty()) {
            htmlInvoiceSource = "Please click the generated Invoice Button";
            // htmlInvoiceSource = generateHtmlInvoice(lastGeneratedInvoice);
        }
        return htmlInvoiceSource;
    }

    /**
     * @param htmlInvoiceSource the htmlInvoiceSource to set
     */
    public void setHtmlInvoiceSource(String htmlInvoiceSource) {
        this.htmlInvoiceSource = htmlInvoiceSource;
    }

    /**
     * @return the invoiceGenerated
     */
    public boolean isInvoiceGenerated() {
        return invoiceGenerated;
    }

    /**
     * @param invoiceGenerated the invoiceGenerated to set
     */
    public void setInvoiceGenerated(boolean invoiceGenerated) {
        this.invoiceGenerated = invoiceGenerated;
    }

    /**
     * @return the htmlInvoiceEmailToAddress
     */
    public String getHtmlInvoiceEmailToAddress() {
        return htmlInvoiceEmailToAddress;
    }

    /**
     * @param htmlInvoiceEmailToAddress the htmlInvoiceEmailToAddress to set
     */
    public void setHtmlInvoiceEmailToAddress(String htmlInvoiceEmailToAddress) {
        this.htmlInvoiceEmailToAddress = htmlInvoiceEmailToAddress;
    }

    /**
     * @return the htmlInvoiceEmailToAddressEnabled
     */
    public boolean isHtmlInvoiceEmailToAddressEnabled() {
        return htmlInvoiceEmailToAddressEnabled;
    }

    /**
     * @param htmlInvoiceEmailToAddressEnabled the
     * htmlInvoiceEmailToAddressEnabled to set
     */
    public void setHtmlInvoiceEmailToAddressEnabled(boolean htmlInvoiceEmailToAddressEnabled) {
        this.htmlInvoiceEmailToAddressEnabled = htmlInvoiceEmailToAddressEnabled;
    }

    /**
     * @return the htmlInvoiceEmailEditSaveButtonText
     */
    public String getHtmlInvoiceEmailEditSaveButtonText() {
        return htmlInvoiceEmailEditSaveButtonText;
    }

    /**
     * @param htmlInvoiceEmailEditSaveButtonText the
     * htmlInvoiceEmailEditSaveButtonText to set
     */
    public void setHtmlInvoiceEmailEditSaveButtonText(String htmlInvoiceEmailEditSaveButtonText) {
        this.htmlInvoiceEmailEditSaveButtonText = htmlInvoiceEmailEditSaveButtonText;
    }

    @FacesConverter(value="invoiceControllerConverter")
    public static class InvoiceControllerConverter implements Converter {

        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            InvoiceController controller = (InvoiceController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "invoiceController");
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
            if (object instanceof Invoice) {
                Invoice o = (Invoice) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + InvoiceController.class.getName());
            }
        }

    }

    /**
     * @return the selectedInvoiceLineItem
     */
    public InvoiceLine getSelectedInvoiceLineItem() {
        return selectedInvoiceLineItem;
    }

    /**
     * @param selectedInvoiceLineItem the selectedInvoiceLineItem to set
     */
    public void setSelectedInvoiceLineItem(InvoiceLine selectedInvoiceLineItem) {
        this.selectedInvoiceLineItem = selectedInvoiceLineItem;
    }

    /**
     * @return the invoiceLineFilteredItems
     */
    public List<InvoiceLine> getInvoiceLineFilteredItems() {
        return invoiceLineFilteredItems;
    }

    /**
     * @param invoiceLineFilteredItems the invoiceLineFilteredItems to set
     */
    public void setInvoiceLineFilteredItems(List<InvoiceLine> invoiceLineFilteredItems) {
        this.invoiceLineFilteredItems = invoiceLineFilteredItems;
    }

    /**
     * @return the invoiceType
     */
    public int getInvoiceType() {
        return invoiceType;
    }

    /**
     * @param invoiceType the invoiceType to set
     */
    public void setInvoiceType(int invoiceType) {
        this.invoiceType = invoiceType;
    }

    /* public void createHtmlInvoice() {
     if (htmlInvoiceSource == null) {
     htmlInvoiceSource = generateHtmlInvoice(lastGeneratedInvoice);
     }
     //RequestContext.getCurrentInstance().update(":previewHtmlInvoiceDialogue");
     RequestContext.getCurrentInstance().execute("PF('previewHtmlInvoiceDialogueWidget').show()");
     }*/
    private String generateHtmlInvoice(Invoice inv) {

        Logger.getLogger(InvoiceController.class.getName()).log(Level.INFO, "generateHtmlInvoice: {0}", inv.getId().toString());
        String templateName = "InvoiceHtml2";
        String invoice = ejbEmailTemplatesFacade.findTemplateByName(templateName).getTemplate();
        if (inv == null || inv.getUserId().getFirstname() == null || inv.getUserId().getLastname() == null || inv.getCreateDatetime() == null || inv.getDueDate() == null || inv.getInvoiceLineCollection() == null) {
            logger.log(Level.SEVERE, "createExcelFromInvoice: invoice or invoice methods  are null");
            return null;

        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/YYYY");

        String custName = configMapFacade.getConfig("invoice.placeholder.CustomerName");
        String custStreetAddress = configMapFacade.getConfig("invoice.placeholder.CustomerStreetAddress");
        String custSuburb = configMapFacade.getConfig("invoice.placeholder.CustomerSuburbStatePostCode");
        String invoiceNo = configMapFacade.getConfig("invoice.placeholder.Invoice_No:");
        String issueDate = configMapFacade.getConfig("invoice.placeholder.Issue_Date:");
        String invoicePeriod = configMapFacade.getConfig("invoice.placeholder.Invoice_Period:");
        String accountNo = configMapFacade.getConfig("invoice.placeholder.Account_No:");
        String invoiceLineItems = configMapFacade.getConfig("invoice.placeholder.InvoiceLineItems");
        String endInvoiceLineItems = configMapFacade.getConfig("invoice.placeholder.EndInvoiceLineItems");
        String subTotal = configMapFacade.getConfig("invoice.placeholder.SubTotal");
        String gst = configMapFacade.getConfig("invoice.placeholder.GST");
        String total = configMapFacade.getConfig("invoice.placeholder.Total");
        String amountPaid = configMapFacade.getConfig("invoice.placeholder.Amount_Paid");
        String balanceDue = configMapFacade.getConfig("invoice.placeholder.Balance_Due");

        String CompanyLogoUrl = configMapFacade.getConfig("invoice.placeholder.CompanyLogoUrl");
        String CompanyLogoAlt = configMapFacade.getConfig("invoice.placeholder.CompanyLogoAlt");
        String CompanyAbn = configMapFacade.getConfig("invoice.placeholder.CompanyAbn");
        String InvoiceTypeHeading = configMapFacade.getConfig("invoice.placeholder.InvoiceTypeHeading");

        String productDescription = configMapFacade.getConfig("invoice.placeholder.lineitemHeader.productDescription");
        String productQuantity = configMapFacade.getConfig("invoice.placeholder.lineitemHeader.productQuantity");
        String productPrice = configMapFacade.getConfig("invoice.placeholder.lineitemHeader.productPrice");
        String productTax = configMapFacade.getConfig("invoice.placeholder.lineitemHeader.productTax");
        String productSubTotal = configMapFacade.getConfig("invoice.placeholder.lineitemHeader.productSubTotal");
        String productAmount = configMapFacade.getConfig("invoice.placeholder.lineitemHeader.productAmount");

        String summarySubTotal = configMapFacade.getConfig("invoice.placeholder.summaryHeader.subTotal");
        String summaryTaxTotal = configMapFacade.getConfig("invoice.placeholder.summaryHeader.taxTotal");
        String summaryTotalAmount = configMapFacade.getConfig("invoice.placeholder.summaryHeader.totalAmount");
        String summaryAmountPaid = configMapFacade.getConfig("invoice.placeholder.summaryHeader.amountPaid");
        String summaryBalanceDue = configMapFacade.getConfig("invoice.placeholder.summaryHeader.balanceDue");

        String registeredForGst = configMapFacade.getConfig("invoice.registeredForGst");
        boolean calculateGst = registeredForGst.toUpperCase().contains("TRUE");
        if (calculateGst) {
            invoice = invoice.replace(InvoiceTypeHeading, configMapFacade.getConfig("invoice.heading.registeredForGst"));
            invoice = invoice.replace(productTax, configMapFacade.getConfig("invoice.lineItemHeader.productTax"));
            invoice = invoice.replace(productSubTotal, configMapFacade.getConfig("invoice.lineItemHeader.productSubTotal"));
            invoice = invoice.replace(summarySubTotal, configMapFacade.getConfig("invoice.summaryHeader.subTotal"));
            invoice = invoice.replace(summaryTaxTotal, configMapFacade.getConfig("invoice.summaryHeader.taxTotal"));
            invoice = invoice.replace(summaryTotalAmount, configMapFacade.getConfig("invoice.summaryHeader.totalAmount"));
        } else {
            invoice = invoice.replace(InvoiceTypeHeading, configMapFacade.getConfig("invoice.heading.notRegisteredForGst"));
            invoice = invoice.replace(summaryTotalAmount, configMapFacade.getConfig("invoice.summaryHeaderNonGst.totalAmount"));
        }
        invoice = invoice.replace(CompanyLogoUrl, configMapFacade.getConfig("invoiceHtmlCompanyLogoUrl"));
        invoice = invoice.replace(CompanyLogoAlt, configMapFacade.getConfig("invoiceHtmlCompanyLogoAlt"));
        invoice = invoice.replace(CompanyAbn, configMapFacade.getConfig("invoiceHtmlCompanyAbn"));

        invoice = invoice.replace(productDescription, configMapFacade.getConfig("invoice.lineItemHeader.productDescription"));
        invoice = invoice.replace(productQuantity, configMapFacade.getConfig("invoice.lineItemHeader.productQuantity"));
        invoice = invoice.replace(productPrice, configMapFacade.getConfig("invoice.lineItemHeader.productPrice"));
        invoice = invoice.replace(productAmount, configMapFacade.getConfig("invoice.lineItemHeader.productAmount"));

        invoice = invoice.replace(summaryAmountPaid, configMapFacade.getConfig("invoice.summaryHeader.amountPaid"));
        invoice = invoice.replace(summaryBalanceDue, configMapFacade.getConfig("invoice.summaryHeader.balanceDue"));

        invoice = invoice.replace(custName, inv.getUserId().getFirstname() + " " + inv.getUserId().getLastname());

        invoice = invoice.replace(custStreetAddress, inv.getUserId().getStreetAddress());
        invoice = invoice.replace(custSuburb, inv.getUserId().getSuburb() + ", " + inv.getUserId().getAddrState() + " " + inv.getUserId().getPostcode());
        invoice = invoice.replace(invoiceNo, inv.getId().toString());
        invoice = invoice.replace(issueDate, sdf.format(inv.getCreateDatetime()));
        invoice = invoice.replace(invoicePeriod, sdf.format(invoiceStartDate) + " - " + sdf.format(invoiceEndDate));
        invoice = invoice.replace(accountNo, inv.getUserId().getId().toString());

        BigDecimal subTotalDecimal;
        BigDecimal gstDecimal;
        BigDecimal totalDecimal = null;
        BigDecimal amountPaidDecimal = inv.getBalance();
        if (amountPaidDecimal == null) {
            amountPaidDecimal = new BigDecimal(0);
        }
        BigDecimal amountDueDecimal;
        if (calculateGst) {
            try {
                totalDecimal = inv.getTotal();
                gstDecimal = totalDecimal.divide(new BigDecimal(11));
                subTotalDecimal = gstDecimal.multiply(new BigDecimal(10));
                invoice = invoice.replace(subTotal, currencyFormat(subTotalDecimal));
                invoice = invoice.replace(gst, currencyFormat(gstDecimal));

            } catch (Exception e) {
                Logger.getLogger(InvoiceController.class.getName()).log(Level.SEVERE, "Calculating GST on HTML Invoice ");
            }
        } else {

            subTotalDecimal = inv.getTotal();
            totalDecimal = subTotalDecimal;
        }
        amountDueDecimal = totalDecimal.add(amountPaidDecimal);

        invoice = invoice.replace(total, currencyFormat(totalDecimal));
        invoice = invoice.replace(amountPaid, currencyFormat(amountPaidDecimal));
        invoice = invoice.replace(balanceDue, currencyFormat(amountDueDecimal));

        // pattern1 and pattern2 are String objects
        int a = invoice.indexOf(invoiceLineItems) + invoiceLineItems.length();
        int b = invoice.indexOf(endInvoiceLineItems);
        String lineItemsFormattedRows;
        lineItemsFormattedRows = invoice.substring(a, b).trim();
        /*
         String regexString = Pattern.quote(invoiceLineItems) + "(.*?)" + Pattern.quote(endInvoiceLineItems);
         String regexPattern = "(?i)(" + invoiceLineItems +")(.+?)("+endInvoiceLineItems + ")";
         Pattern pattern = Pattern.compile(regexString);
         // text contains the full text that you want to extract data
         Matcher matcher = pattern.matcher(invoice);
       
         while (matcher.find()) {
         lineItemsFormattedRows = matcher.group(1); // Since (.*?) is capturing group 1
         // You can insert match into a List/Collection here
         }
         String lineItemsFormattedRows2 = invoice.replace(regexPattern, "TESTING");
         
         */

        String lineItems = getHtmlInvoiceLineItems(inv, lineItemsFormattedRows, calculateGst);
        invoice = invoice.replace(lineItemsFormattedRows, lineItems);

        return invoice;
    }

    private String getHtmlInvoiceLineItems(Invoice inv, String formattedRows, boolean calculateGst) {

        String lineItemsFormatted = "";
        int t = formattedRows.lastIndexOf("<tr");
        String evenRow = formattedRows.substring(0, t - 1);
        String oddRow = formattedRows.substring(t);

        String productDescription = configMapFacade.getConfig("invoice.placeholder.lineitem.productDescription");
        String productQuantity = configMapFacade.getConfig("invoice.placeholder.lineitem.ProductQuantity");
        String productPrice = configMapFacade.getConfig("invoice.placeholder.lineitem.productPrice");
        String productTax = configMapFacade.getConfig("invoice.placeholder.lineitem.productTax");
        String productSubTotal = configMapFacade.getConfig("invoice.placeholder.lineitem.productSubTotal");
        String productAmount = configMapFacade.getConfig("invoice.placeholder.lineitem.productAmount");

        //body - add formatting and formulas
        int rownum = 2;
        boolean useEvenRowStyle = true;
        List<InvoiceLine> invLineItemList = new ArrayList<>(inv.getInvoiceLineCollection());
        for (InvoiceLine li : invLineItemList) {
            useEvenRowStyle = !useEvenRowStyle;
            String rowTemplate;
            if (useEvenRowStyle) {
                rowTemplate = evenRow;
            } else {
                rowTemplate = oddRow;
            }
            rowTemplate = rowTemplate.replace(productDescription, li.getDescription());
            rowTemplate = rowTemplate.replace(productQuantity, li.getQuantity().toPlainString());
            rowTemplate = rowTemplate.replace(productPrice, currencyFormat(li.getPrice()));
            if (calculateGst) {
                BigDecimal subTotal = li.getPrice().multiply(li.getQuantity());
                BigDecimal gstAmount = subTotal.divide(new BigDecimal(10));
                //rowTemplate= rowTemplate.replace(productTax, li.getGstAmount()); TODO
                //rowTemplate= rowTemplate.replace(productSubTotal, li.getSubTotal()); TODO
                rowTemplate = rowTemplate.replace(productTax, currencyFormat(gstAmount));
                rowTemplate = rowTemplate.replace(productSubTotal, currencyFormat(subTotal));
            }
            rowTemplate = rowTemplate.replace(productAmount, currencyFormat(li.getAmount()));

            lineItemsFormatted += rowTemplate;

        }

        return lineItemsFormatted;
    }

    public static String currencyFormat(BigDecimal n) {
        return NumberFormat.getCurrencyInstance().format(n);
    }

    public StreamedContent getExportExcel() {
        StreamedContent file = null;
        try {
            Workbook wb = createExcelFromInvoice(lastGeneratedInvoice);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            InputStream stream = new ByteArrayInputStream(out.toByteArray());
            file = new DefaultStreamedContent(stream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", getInvoiceFileName() + ".xlsx");

            //writeExcelToResponse(FacesContext.getCurrentInstance().getExternalContext(), wb, getInvoiceFileName());
        } catch (Exception ex) {
            Logger.getLogger(InvoiceController.class.getName()).log(Level.SEVERE, "Export Excel spreadsheet from the invoices table failed", ex);
        }
        return file;
    }

    /*protected void writeExcelToResponse(ExternalContext externalContext, Workbook generatedExcel, String filename) throws IOException {
     externalContext.setResponseContentType("application/vnd.ms-excel");
     externalContext.setResponseHeader("Expires", "0");
     externalContext.setResponseHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
     externalContext.setResponseHeader("Pragma", "public");
     externalContext.setResponseHeader("Content-disposition", "attachment;filename=" + filename + ".xls");
     externalContext.addResponseCookie(Constants.DOWNLOAD_COOKIE, "true", Collections.<String, Object>emptyMap());

     OutputStream out = externalContext.getResponseOutputStream();
     generatedExcel.write(out);
     externalContext.responseFlushBuffer();
     }*/
    private Workbook createExcelFromInvoice(Invoice inv) {
        Workbook wb = new XSSFWorkbook();
        if (inv == null || inv.getUserId().getFirstname() == null || inv.getUserId().getLastname() == null || inv.getCreateDatetime() == null || inv.getDueDate() == null || inv.getInvoiceLineCollection() == null) {
            logger.log(Level.SEVERE, "createExcelFromInvoice: invoice or invoice methods  are null");
            return null;

        }

        Map<String, CellStyle> styles = createStyles(wb);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM YYYY");
        Sheet sheet = wb.createSheet("Invoice");
        PrintSetup printSetup = sheet.getPrintSetup();
        printSetup.setLandscape(false);
        sheet.setFitToPage(true);
        sheet.setHorizontallyCenter(true);

        //title row
        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(45);
        Cell titleCell = titleRow.createCell(0);
        String tit = inv.getUserId().getFirstname() + " " + inv.getUserId().getLastname() + " - " + sdf.format(inv.getCreateDatetime()) + " to " + sdf.format(inv.getDueDate());
        titleCell.setCellValue(tit);
        titleCell.setCellStyle(styles.get("title"));
        sheet.addMergedRegion(CellRangeAddress.valueOf("$A$1:$D$1"));

        //header row
        Row headerRow = sheet.createRow(1);
        headerRow.setHeightInPoints(40);
        Cell headerCell;
        String[] titles = {"Description", "Quantity", "Price", "Amount"};
        for (int i = 0; i < titles.length; i++) {
            headerCell = headerRow.createCell(i);
            headerCell.setCellValue(titles[i]);
            headerCell.setCellStyle(styles.get("header"));
        }

        //body - add formatting and formulas
        int rownum = 2;
        List<InvoiceLine> invLineItemList = new ArrayList<>(inv.getInvoiceLineCollection());
        for (InvoiceLine li : invLineItemList) {
            Row row = sheet.createRow(rownum++);
            for (int j = 0; j < titles.length; j++) {
                Cell cell = row.createCell(j);
                if (j == 3) {
                    //the 4th cell contains sum of quantity X price, e.g. SUM(B3*C3)
                    String ref = "B" + rownum + "*C" + rownum;
                    cell.setCellFormula("SUM(" + ref + ")");
                    cell.setCellStyle(styles.get("formula_2"));
                } else if (j == 2) {
                    cell.setCellStyle(styles.get("formula"));
                } else {
                    cell.setCellStyle(styles.get("cell"));
                }
            }
        }
//row with totals below
        Row sumRow = sheet.createRow(rownum++);
        sumRow.setHeightInPoints(35);
        int cellNum = 0;
        Cell cell;
        cell = sumRow.createCell(cellNum);
        cell.setCellStyle(styles.get("formula_2"));
        cell = sumRow.createCell(cellNum++);
        cell.setCellStyle(styles.get("formula_2"));
        cell = sumRow.createCell(cellNum++);
        cell.setCellStyle(styles.get("formula_2"));

        cell = sumRow.createCell(cellNum++);
        cell.setCellValue("Total Outstanding:");
        cell.setCellStyle(styles.get("formula_2"));

        cell = sumRow.createCell(cellNum++);
        String ref = "D3:D" + (2 + invLineItemList.size());
        cell.setCellFormula("SUM(" + ref + ")");
        cell.setCellStyle(styles.get("formula_3"));

        // add cell values
        for (int i = 0; i < invLineItemList.size(); i++) {
            InvoiceLine il = invLineItemList.get(i);
            Row row = sheet.getRow(2 + i);
            row.getCell(0).setCellValue(il.getDescription());
            row.getCell(1).setCellValue(il.getQuantity().intValue());
            row.getCell(2).setCellValue(il.getPrice().doubleValue());
            row.getCell(3).setCellValue(il.getAmount().doubleValue());

        }

        //finally set column widths, the width is measured in units of 1/256th of a character width
        sheet.setColumnWidth(0, 60 * 256); //30 characters wide
        for (int i = 1; i < 4; i++) {
            sheet.setColumnWidth(i, 12 * 256);  //6 characters wide
        }

        return wb;
    }

    /**
     * Create a library of cell styles
     */
    private static Map<String, CellStyle> createStyles(Workbook wb) {
        Map<String, CellStyle> styles = new HashMap<>();
        CellStyle style;
        Font titleFont = wb.createFont();
        titleFont.setFontHeightInPoints((short) 18);
        titleFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
        style = wb.createCellStyle();
        style.setAlignment(CellStyle.ALIGN_CENTER);
        style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        style.setFont(titleFont);
        styles.put("title", style);

        Font monthFont = wb.createFont();
        monthFont.setFontHeightInPoints((short) 11);
        monthFont.setColor(IndexedColors.WHITE.getIndex());
        style = wb.createCellStyle();
        style.setAlignment(CellStyle.ALIGN_CENTER);
        style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setFont(monthFont);
        style.setWrapText(true);
        styles.put("header", style);

        style = wb.createCellStyle();
        style.setAlignment(CellStyle.ALIGN_CENTER);
        style.setWrapText(true);
        style.setBorderRight(CellStyle.BORDER_THIN);
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderLeft(CellStyle.BORDER_THIN);
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderTop(CellStyle.BORDER_THIN);
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderBottom(CellStyle.BORDER_THIN);
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        styles.put("cell", style);

        style = wb.createCellStyle();
        style.setAlignment(CellStyle.ALIGN_RIGHT);
        style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setDataFormat(wb.createDataFormat().getFormat("$##,##0.00"));
        styles.put("formula", style);

        style = wb.createCellStyle();
        style.setAlignment(CellStyle.ALIGN_RIGHT);
        style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setDataFormat(wb.createDataFormat().getFormat("$##,##0.00"));
        styles.put("formula_2", style);

        Font totalFont = wb.createFont();
        totalFont.setFontHeightInPoints((short) 11);
        totalFont.setColor(IndexedColors.YELLOW.getIndex());
        totalFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
        style = wb.createCellStyle();
        style.setAlignment(CellStyle.ALIGN_RIGHT);
        style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);

        style.setDataFormat(wb.createDataFormat().getFormat("$##,##0.00"));
        style.setFont(totalFont);

        styles.put("formula_3", style);

        return styles;
    }

    public void emailInvoice() {

        try {
            Future<Boolean> emailSendResult = ejbPaymentBean.sendAsynchEmail(htmlInvoiceEmailToAddress, configMapFacade.getConfig("HtmlInvoiceCCEmailAddress"), configMapFacade.getConfig("HtmlInvoiceFromEmailAddress"), configMapFacade.getConfig("HtmlInvoiceEmailSubject"), htmlInvoiceSource, null, emailQueueFacade.getEmailServerProperties(), false);
            JsfUtil.addSuccessMessage("The Invoice has been Emailed to " + htmlInvoiceEmailToAddress);

        } catch (Exception e) {
            JsfUtil.addSuccessMessage("Error: The Invoice has NOT been Emailed to " + current.getUserId().getEmailAddress() + ", Reason:" + e.getMessage());
        }
    }

}
