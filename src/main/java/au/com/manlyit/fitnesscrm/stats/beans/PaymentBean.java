package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.beans.util.PaymentStatus;
import au.com.manlyit.fitnesscrm.stats.classes.EziDebitPaymentGateway;
import au.com.manlyit.fitnesscrm.stats.classes.util.AsyncJob;
import au.com.manlyit.fitnesscrm.stats.classes.util.FutureMapEJB;
import au.com.manlyit.fitnesscrm.stats.classes.util.SendHTMLEmailWithFileAttached;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.PaymentParameters;
import au.com.manlyit.fitnesscrm.stats.db.Payments;
import au.com.manlyit.fitnesscrm.stats.webservices.ArrayOfPayment;
import au.com.manlyit.fitnesscrm.stats.webservices.ArrayOfScheduledPayment;
import au.com.manlyit.fitnesscrm.stats.webservices.CustomerDetails;
import au.com.manlyit.fitnesscrm.stats.webservices.EziResponseOfArrayOfPaymentTHgMB7OL;
import au.com.manlyit.fitnesscrm.stats.webservices.EziResponseOfArrayOfScheduledPaymentTHgMB7OL;
import au.com.manlyit.fitnesscrm.stats.webservices.EziResponseOfCustomerDetailsTHgMB7OL;
import au.com.manlyit.fitnesscrm.stats.webservices.EziResponseOfNewCustomerXcXH3LiW;
import au.com.manlyit.fitnesscrm.stats.webservices.EziResponseOfPaymentDetailPlusNextPaymentInfoTHgMB7OL;
import au.com.manlyit.fitnesscrm.stats.webservices.EziResponseOfPaymentDetailTHgMB7OL;
import au.com.manlyit.fitnesscrm.stats.webservices.EziResponseOfstring;
import au.com.manlyit.fitnesscrm.stats.webservices.INonPCIService;
import au.com.manlyit.fitnesscrm.stats.webservices.NonPCIService;
import au.com.manlyit.fitnesscrm.stats.webservices.PaymentDetail;
import au.com.manlyit.fitnesscrm.stats.webservices.PaymentDetailPlusNextPaymentInfo;
import au.com.manlyit.fitnesscrm.stats.webservices.ScheduledPayment;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.ws.WebServiceException;

/**
 *
 * @author david
 */
@Named
@Stateless
public class PaymentBean implements Serializable {

    private static final Logger logger = Logger.getLogger(PaymentBean.class.getName());
    private static final String paymentGateway = "EZIDEBIT";

    @Inject
    private CustomersFacade customersFacade;
    @Inject
    private ConfigMapFacade configMapFacade;
    @Inject
    private AuditLogFacade auditLogFacade;

    @Inject
    private PaymentsFacade paymentsFacade;

    private INonPCIService getWs() {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL(configMapFacade.getConfig("payment.ezidebit.gateway.url"));
        } catch (MalformedURLException ex) {

            logger.log(Level.SEVERE, "MalformedURLException - payment.ezidebit.gateway.url", ex);

        }
        return new NonPCIService(url).getBasicHttpBindingINonPCIService();
    }

    @Asynchronous
    public Future<CustomerDetails> getCustomerDetails(Customers cust, String digitalKey) {

        CustomerDetails cd = null;
        if (cust == null || digitalKey == null) {
            return new AsyncResult<>(cd);
        }
        if (cust.getId() == null || digitalKey.trim().isEmpty()) {
            return new AsyncResult<>(cd);
        }

        logger.log(Level.INFO, "Payment Bean - Running async task - Getting Customer Details {0}", cust.getUsername());

        EziResponseOfCustomerDetailsTHgMB7OL customerdetails = getWs().getCustomerDetails(digitalKey, "", cust.getId().toString());
        if (customerdetails.getError() == 0) {// any errors will be a non zero value

            cd = customerdetails.getData().getValue();
            if (cd != null) {
                logger.log(Level.INFO, "Payment Bean - Get Customer Details Response: Customer  - {0}, Ezidebit Name : {1} {2}", new Object[]{cust.getUsername(), cd.getCustomerFirstName().getValue(), cd.getCustomerName().getValue()});
            }
        } else {
            logger.log(Level.WARNING, "Get Customer Details Response: Error - {0}", customerdetails.getErrorMessage().getValue());

        }
        return new AsyncResult<>(cd);

    }

    public synchronized void createCRMPaymentSchedule(Customers cust, Date scheduleStartDate, Date scheduleEndDate, char schedulePeriodType, int payDayOfWeek, int dayOfMonth, long amountInCents, int limitToNumberOfPayments, long paymentAmountLimitInCents, boolean keepManualPayments, boolean firstWeekOfMonth, boolean secondWeekOfMonth, boolean thirdWeekOfMonth, boolean fourthWeekOfMonth, String loggedInUser, String sessionId, String digitalKey, FutureMapEJB futureMap, PaymentBean payBean) {

        if (schedulePeriodType == 'W' || schedulePeriodType == 'F' || schedulePeriodType == 'N' || schedulePeriodType == '4') {
            if (payDayOfWeek < 1 || payDayOfWeek > 7) {
                logger.log(Level.WARNING, "createSchedule  FAILED: A value must be provided for dayOfWeek  when the SchedulePeriodType is  W,F,4,N");
                return;
            }
            if (payDayOfWeek < 2 || payDayOfWeek > 6) {
                payDayOfWeek = 2;// can't debit on weekends only weekdays
            }
        }
        if (schedulePeriodType == 'M') {
            if (dayOfMonth < 1 || dayOfMonth > 31) {
                logger.log(Level.WARNING, "createSchedule FAILED: A value must be provided for dayOfMonth (1..31 )\n" + " when the\n" + "SchedulePeriodType is in\n" + "M");
                return;
            }
        }

        int check = 0;
        if (firstWeekOfMonth) {
            check++;
        }
        if (secondWeekOfMonth) {
            check++;
        }
        if (thirdWeekOfMonth) {
            check++;
        }
        if (fourthWeekOfMonth) {
            check++;
        }
        if (schedulePeriodType == 'N' && check == 0) {
            logger.log(Level.WARNING, "createSchedule FAILED: A value must be provided for week Of Month  when the SchedulePeriodType is N");
            return;
        }

        //delete all existing scheduled payments
        List<Payments> crmPaymentList = paymentsFacade.findScheduledPaymentsByCustomer(cust);
        if (crmPaymentList != null) {
            logger.log(Level.INFO, "createSchedule - Found {0} existing scheduled payments for {1}", new Object[]{crmPaymentList.size(), cust.getUsername()});
            for (int x = crmPaymentList.size() - 1; x > -1; x--) {
                Payments p = crmPaymentList.get(x);
                String ref = p.getId().toString();
                boolean isManual = p.getManuallyAddedPayment();
                if (keepManualPayments == true && isManual) {
                    logger.log(Level.INFO, "createSchedule - keeping manual payment: Cust={0}, Ref={1}, Manaul Payment = {2}", new Object[]{cust.getUsername(), ref, isManual});
                } else {
                    logger.log(Level.INFO, "createSchedule - Deleting payment: Cust={0}, Ref={1}, Manaul Payment = {2}", new Object[]{cust.getUsername(), ref, isManual});
                    //paymentsFacade.remove(p); will be deleted once processed
                    p.setPaymentStatus(PaymentStatus.DELETE_REQUESTED.value());
                    paymentsFacade.edit(p);

                    AsyncJob aj = new AsyncJob("DeletePayment", payBean.deletePayment(cust, null, null, ref, loggedInUser, digitalKey));
                    futureMap.put(sessionId, aj);
                    try {
                        Thread.sleep(100);// the payment gateway has some concurrency throttling so we don't want to exceed our number of txns per second.
                    } catch (InterruptedException ex) {
                        Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        // startAsynchJob("ClearSchedule", paymentBean.clearSchedule(cust, false, loggedInUser, getDigitalKey()));// work around failsafe until migration complete. THere are styill scheduled payments without a payment reference from DB
        // try {
        //     Thread.sleep(200);// work around to ensure clearschedule workaround doesn't impact new payments being added 
        // } catch (InterruptedException interruptedException) {
        //  }
        GregorianCalendar startCal = new GregorianCalendar();
        GregorianCalendar endCal = new GregorianCalendar();
        startCal.setTime(scheduleStartDate);
        endCal.setTime(scheduleEndDate);
        int calendarField = 0;
        int calendarAmount = 0;
        int currentDay = startCal.get(Calendar.DAY_OF_MONTH);
        int calendarDow = startCal.get(Calendar.DAY_OF_WEEK);
        if (schedulePeriodType != 'M') {
            dayOfMonth = currentDay;
        }

        switch (schedulePeriodType) {
            case 'W'://weekly
                calendarField = Calendar.DAY_OF_YEAR;
                calendarAmount = 7;
                if (calendarDow > payDayOfWeek) {
                    int d = (payDayOfWeek + 7) - calendarDow;
                    startCal.add(Calendar.DAY_OF_YEAR, d);
                } else {
                    int d = payDayOfWeek - calendarDow;
                    startCal.add(Calendar.DAY_OF_YEAR, d);
                }//sunday = 1 , monday = 2 ...saturday = 7
                break;
            case 'F'://fortnightly
                calendarField = Calendar.DAY_OF_YEAR;
                calendarAmount = 14;
                if (calendarDow > payDayOfWeek) {
                    int d = (payDayOfWeek + 7) - calendarDow;
                    startCal.add(Calendar.DAY_OF_YEAR, d);
                } else {
                    int d = payDayOfWeek - calendarDow;
                    startCal.add(Calendar.DAY_OF_YEAR, d);
                }
                break;
            case 'M':// monthly
                calendarField = Calendar.MONTH;
                calendarAmount = 1;

                if (currentDay > dayOfMonth) {
                    startCal.add(calendarField, 1);
                }
                startCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                break;
            case '4': // 4 weekly
                calendarField = Calendar.DAY_OF_YEAR;
                calendarAmount = 28;
                //startCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                //if (currentDay > dayOfMonth) {
                //    startCal.add(Calendar.MONTH, 1);
                //}
                calendarDow = startCal.get(Calendar.DAY_OF_WEEK);
                if (calendarDow > payDayOfWeek) {
                    int d = (payDayOfWeek + 7) - calendarDow;
                    startCal.add(Calendar.DAY_OF_YEAR, d);
                } else {
                    int d = payDayOfWeek - calendarDow;
                    startCal.add(Calendar.DAY_OF_YEAR, d);
                }

                break;
            case 'N': //Weekday in month (e.g. Monday in the third week of every month)
                calendarField = Calendar.DAY_OF_YEAR;
                calendarAmount = 7;
                calendarDow = startCal.get(Calendar.DAY_OF_WEEK);
                if (calendarDow > payDayOfWeek) {
                    int d = (payDayOfWeek + 7) - calendarDow;
                    startCal.add(Calendar.DAY_OF_YEAR, d);
                } else {
                    int d = payDayOfWeek - calendarDow;
                    startCal.add(Calendar.DAY_OF_YEAR, d);
                }
                break;
            case 'Q': // quarterly
                calendarField = Calendar.MONTH;
                calendarAmount = 3;
                //if (currentDay > dayOfMonth) {
                //     startCal.add(Calendar.MONTH, 1);
                // }
                // startCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                break;
            case 'H': // 6 monthly
                calendarField = Calendar.MONTH;
                calendarAmount = 6;
                // if (currentDay > dayOfMonth) {
                //     startCal.add(Calendar.MONTH, 1);
                // }
                //  startCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                break;
            case 'Y'://yearly
                calendarField = Calendar.YEAR;
                calendarAmount = 1;
                // if (currentDay > dayOfMonth) {
                //      startCal.add(Calendar.MONTH, 1);
                //  }
                //  startCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                break;
        }
        int numberOfpayments = 0;
        long cumulativeAmountInCents = 0;
        Date newDebitDate = startCal.getTime();

        while (startCal.compareTo(endCal) < 0) {

            if (schedulePeriodType == 'N') {
                //TODO fix this 
                //if (startCal.get(Calendar.WEEK_OF_MONTH) == 1 && firstWeekOfMonth == true) {
                if (startCal.get(Calendar.DAY_OF_MONTH) >= 1 && startCal.get(Calendar.DAY_OF_MONTH) <= 7 && firstWeekOfMonth == true) {
                    if (arePaymentsWithinLimits(limitToNumberOfPayments, paymentAmountLimitInCents, cumulativeAmountInCents, amountInCents, numberOfpayments)) {
                        addNewPayment(cust, newDebitDate, amountInCents, false, loggedInUser, sessionId, digitalKey, futureMap, payBean);
                        numberOfpayments++;
                    }

                }
                if (startCal.get(Calendar.DAY_OF_MONTH) >= 8 && startCal.get(Calendar.DAY_OF_MONTH) <= 14 && secondWeekOfMonth == true) {
                    if (arePaymentsWithinLimits(limitToNumberOfPayments, paymentAmountLimitInCents, cumulativeAmountInCents, amountInCents, numberOfpayments)) {
                        addNewPayment(cust, newDebitDate, amountInCents, false, loggedInUser, sessionId, digitalKey, futureMap, payBean);
                        numberOfpayments++;
                    }

                }
                if (startCal.get(Calendar.DAY_OF_MONTH) >= 15 && startCal.get(Calendar.DAY_OF_MONTH) <= 21 && thirdWeekOfMonth == true) {
                    if (arePaymentsWithinLimits(limitToNumberOfPayments, paymentAmountLimitInCents, cumulativeAmountInCents, amountInCents, numberOfpayments)) {
                        addNewPayment(cust, newDebitDate, amountInCents, false, loggedInUser, sessionId, digitalKey, futureMap, payBean);
                        numberOfpayments++;
                    }

                }
                if (startCal.get(Calendar.DAY_OF_MONTH) >= 22 && startCal.get(Calendar.DAY_OF_MONTH) <= 28 && fourthWeekOfMonth == true) {
                    if (arePaymentsWithinLimits(limitToNumberOfPayments, paymentAmountLimitInCents, cumulativeAmountInCents, amountInCents, numberOfpayments)) {
                        addNewPayment(cust, newDebitDate, amountInCents, false, loggedInUser, sessionId, digitalKey, futureMap, payBean);
                        numberOfpayments++;
                    }

                }
                startCal.add(calendarField, calendarAmount);
                Date placeholder = startCal.getTime();

                calendarDow = startCal.get(Calendar.DAY_OF_WEEK);
                if (calendarDow > payDayOfWeek) {
                    int d = calendarDow - (payDayOfWeek + 7);
                    startCal.add(Calendar.DAY_OF_YEAR, d);
                } else {
                    int d = payDayOfWeek - calendarDow;
                    startCal.add(Calendar.DAY_OF_YEAR, d);
                }
                newDebitDate = startCal.getTime();
                startCal.setTime(placeholder);// set it back to correct day of month as we may have changed the day of the week.
            } else {
                if (arePaymentsWithinLimits(limitToNumberOfPayments, paymentAmountLimitInCents, cumulativeAmountInCents, amountInCents, numberOfpayments)) {
                    addNewPayment(cust, newDebitDate, amountInCents, false, loggedInUser, sessionId, digitalKey, futureMap, payBean);
                    numberOfpayments++;
                }
                startCal.add(calendarField, calendarAmount);
                Date placeholder = startCal.getTime();
                // we can only schedule payments for business days
                if (startCal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                    startCal.add(Calendar.DAY_OF_YEAR, 2);
                }
                if (startCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    startCal.add(Calendar.DAY_OF_YEAR, 1);
                }
                newDebitDate = startCal.getTime();
                startCal.setTime(placeholder);// set it back to correct day of month as we may have changed the day of the week.
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

   

    private synchronized boolean arePaymentsWithinLimits(int limitToNumberOfPayments, long paymentAmountLimitInCents, long cumulativeAmountInCents, long amountInCents, int numberOfpayments) {
        if (limitToNumberOfPayments > 0 || paymentAmountLimitInCents > 0) {
            cumulativeAmountInCents += amountInCents;
            if (limitToNumberOfPayments > 0 && paymentAmountLimitInCents > 0) {
                if (limitToNumberOfPayments > numberOfpayments && paymentAmountLimitInCents > cumulativeAmountInCents) {
                    return true;
                }
            }
            if (limitToNumberOfPayments > 0 && paymentAmountLimitInCents == 0) {
                if (limitToNumberOfPayments > numberOfpayments) {
                    return true;
                }
            }
            if (limitToNumberOfPayments == 0 && paymentAmountLimitInCents > 0) {
                if (paymentAmountLimitInCents > cumulativeAmountInCents) {
                    return true;
                }
            }
            return false;

        } else {
            return true;
        }

    }

    public synchronized boolean retryDeletePayment(Payments pay, String user, String sessionId, String digitalKey, FutureMapEJB futureMap, PaymentBean payBean) {
        //String user = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        if (user != null) {

            try {
                Customers cust = pay.getCustomerName();
                Date debitDate = pay.getDebitDate();

                long amountInCents = pay.getPaymentAmount().movePointRight(2).longValue();// convert to cents
                String newPaymentID = pay.getId().toString();
                logger.log(Level.INFO, "retryDeletePayment for customer {0} with paymentID: {1}", new Object[]{cust.getUsername(), newPaymentID});
                AsyncJob aj = new AsyncJob("DeletePayment", payBean.deletePayment(cust, debitDate, amountInCents, newPaymentID, user, digitalKey));
                futureMap.put(sessionId, aj);
            } catch (Exception e) {
                logger.log(Level.WARNING, "retryDeletePayment failed due to exception:", e);
                return false;
            }
        } else {
            logger.log(Level.WARNING, "Logged in user is null. retryDeletePayment aborted.");
            return false;
        }
        return true;
    }

    public synchronized boolean retryAddNewPayment(Payments pay, String user, String sessionId, String digitalKey, FutureMapEJB futureMap, PaymentBean payBean) {
        //String user = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        if (user != null) {

            try {
                Customers cust = pay.getCustomerName();
                Date debitDate = pay.getDebitDate();
                long amountInCents = pay.getPaymentAmount().movePointRight(2).longValue();// convert to cents
                String newPaymentID = pay.getId().toString();
                logger.log(Level.INFO, "retryAddNewPayment for customer {0} with paymentID: {1}", new Object[]{cust.getUsername(), newPaymentID});
                AsyncJob aj = new AsyncJob("AddPayment", payBean.addPayment(cust, debitDate, amountInCents, newPaymentID, user, digitalKey));
                futureMap.put(sessionId, aj);
            } catch (Exception e) {
                logger.log(Level.WARNING, "retryAddNewPayment failed due to exception:", e);
                return false;
            }
        } else {
            logger.log(Level.WARNING, "Logged in user is null. retryAddNewPayment aborted.");
            return false;
        }
        return true;
    }

    public synchronized void addNewPayment(Customers cust, Date debitDate, long amountInCents, boolean manualPayment, String user, String sessionId, String digitalKey, FutureMapEJB futureMap, PaymentBean payBean) {
        //String user = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        if (user != null) {

            try {
                Payments newPayment = new Payments(0);
                newPayment.setDebitDate(debitDate);
                newPayment.setCreateDatetime(new Date());
                newPayment.setLastUpdatedDatetime(new Date());
                newPayment.setYourSystemReference(cust.getId().toString());
                newPayment.setPaymentAmount(new BigDecimal(amountInCents / (long) 100));
                newPayment.setCustomerName(cust);
                newPayment.setPaymentStatus(PaymentStatus.SENT_TO_GATEWAY.value());
                newPayment.setManuallyAddedPayment(manualPayment);
                newPayment.setBankReturnCode("");
                newPayment.setBankFailedReason("");
                newPayment.setBankReceiptID("");
                newPayment.setPaymentDate(debitDate);// we can use this timestamp to reference bookings with booking date as they will be identical
                paymentsFacade.createAndFlush(newPayment);

                String newPaymentID = newPayment.getId().toString();
                newPayment.setPaymentReference(newPaymentID);
                paymentsFacade.edit(newPayment);
                logger.log(Level.INFO, "New Payment Created for customer {0} with paymentID: {1}", new Object[]{cust.getUsername(), newPaymentID});
                AsyncJob aj = new AsyncJob("AddPayment", payBean.addPayment(cust, debitDate, amountInCents, newPaymentID, user, digitalKey));
                futureMap.put(sessionId, aj);

            } catch (Exception e) {
                logger.log(Level.WARNING, "Add Single Payment failed due to exception:", e);
            }
        } else {
            logger.log(Level.WARNING, "Logged in user is null. Add Single Payment aborted.");
        }

    }

    @Asynchronous
    public Future<CustomerDetails> getCustomerDetailsFromEziDebitId(String eziDebitId, String digitalKey) {

        CustomerDetails cd = null;
        if (eziDebitId == null || digitalKey == null) {
            return new AsyncResult<>(cd);
        }
        if (eziDebitId.trim().isEmpty() || digitalKey.trim().isEmpty()) {
            return new AsyncResult<>(cd);
        }
        logger.log(Level.INFO, "Getting Customer (EziId) Details {0}", eziDebitId);

        EziResponseOfCustomerDetailsTHgMB7OL customerdetails = getWs().getCustomerDetails(digitalKey, eziDebitId, "");
        if (customerdetails.getError() == 0) {// any errors will be a non zero value
            logger.log(Level.INFO, "Get Customer (EziId) Details Response: Name - {0}", customerdetails.getData().getValue().getCustomerName().getValue());

            cd = customerdetails.getData().getValue();

        } else {
            logger.log(Level.WARNING, "Get Customer (EziId) Details Response: Error - {0}", customerdetails.getErrorMessage().getValue());
        }
        return new AsyncResult<>(cd);
    }

    /*  
     pci compliant version only
    
     @Asynchronous
     public Future<CustomerDetails> getCustomerAccountDetails(Customers cust, String digitalKey) {

     CustomerDetails cd = null;
     if (cust == null || digitalKey == null) {
     return new AsyncResult<>(cd);
     }
     if (cust.getId() == null || digitalKey.trim().isEmpty()) {
     return new AsyncResult<>(cd);
     }

     logger.log(Level.INFO, "Running async task - Getting Customer Account Details {0}", cust.getUsername());
        
     EziResponseOfCustomerAccountDetails customerdetails = getWs().getCustomerAccountDetails(digitalKey, "", cust.getId().toString());
     if (customerdetails.getError() == 0) {// any errors will be a non zero value
     logger.log(Level.INFO, "Get Customer Account Details Response: Name - {0}", customerdetails.getData().getValue().getCustomerName().getValue());

     cd = customerdetails.getData().getValue();

     } else {
     logger.log(Level.WARNING, "Get Customer Account Details Response: Error - {0}", customerdetails.getErrorMessage().getValue());

     }
     return new AsyncResult<>(cd);

     }

     @Asynchronous
     public Future<CustomerDetails> getCustomerAccountDetailsFromEziDebitId(String eziDebitId, String digitalKey) {

     CustomerDetails cd = null;
     if (eziDebitId == null || digitalKey == null) {
     return new AsyncResult<>(cd);
     }
     if (eziDebitId.trim().isEmpty() || digitalKey.trim().isEmpty()) {
     return new AsyncResult<>(cd);
     }
     logger.log(Level.INFO, "Getting Customer (EziId) Account Details {0}", eziDebitId);
        
     EziResponseOfCustomerDetailsTHgMB7OL customerdetails = getWs().getCustomerAccountDetails(digitalKey, eziDebitId, "");
     if (customerdetails.getError() == 0) {// any errors will be a non zero value
     logger.log(Level.INFO, "Get Customer (EziId) Account Details Response: Name - {0}", customerdetails.getData().getValue().getCustomerName().getValue());

     cd = customerdetails.getData().getValue();

     } else {
     logger.log(Level.WARNING, "Get Customer (EziId) Account Details Response: Error - {0}", customerdetails.getErrorMessage().getValue());
     }
     return new AsyncResult<>(cd);
     }*/
    @Asynchronous
    public Future<Boolean> editCustomerDetails(Customers cust, String eziDebitRef, Customers loggedInUser, String digitalKey) {
        String ourSystemRef = "";

        if (eziDebitRef == null) {
            eziDebitRef = "";
            ourSystemRef = cust.getId().toString();
        }
        boolean result = false;
        logger.log(Level.INFO, "Editing Customer  {0}", cust.getUsername());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        PaymentParameters payParams = cust.getPaymentParameters();
        String addresssLine2 = ""; // not used
        String humanFriendlyReference = cust.getId() + " " + cust.getLastname().toUpperCase() + " " + cust.getFirstname().toUpperCase(); // existing customers use this type of reference by default

        if (payParams == null) {
            logger.log(Level.WARNING, "Payment gateway EZIDEBIT parameters not found");
            return new AsyncResult<>(false);
        }

// note - NB - for Australian Customers the
//mobile phone number must be 10
//digits long and begin with '04'. For
//New Zealand Customers the mobile
//phone number must be 10 digits
//long and begin with '02'
        EziResponseOfstring editCustomerDetail = getWs().editCustomerDetails(digitalKey, eziDebitRef, ourSystemRef, cust.getId().toString(), humanFriendlyReference, cust.getLastname(), cust.getFirstname(), cust.getStreetAddress(), addresssLine2, cust.getSuburb(), cust.getPostcode(), cust.getAddrState(), cust.getEmailAddress(), cust.getTelephone(), payParams.getSmsPaymentReminder(), payParams.getSmsFailedNotification(), payParams.getSmsExpiredCard(), payParams.getLoggedInUser().getUsername());
        logger.log(Level.INFO, "editCustomerDetail Response: Error - {0}, Data - {1}", new Object[]{editCustomerDetail.getErrorMessage().getValue(), editCustomerDetail.getData().getValue()});
        if (editCustomerDetail.getError() == 0) {// any errors will be a non zero value
            result = true;
            String auditDetails = "Edit the customers details for  :" + cust.getUsername() + " Details:  " + humanFriendlyReference + " " + cust.getLastname() + " " + cust.getFirstname() + " " + cust.getStreetAddress() + " " + addresssLine2 + " " + cust.getSuburb() + " " + cust.getPostcode() + " " + cust.getAddrState() + " " + cust.getEmailAddress() + " " + cust.getTelephone() + " " + payParams.getSmsPaymentReminder() + " " + payParams.getSmsFailedNotification() + " " + payParams.getSmsExpiredCard() + " " + payParams.getLoggedInUser().getUsername();
            String changedFrom = "Existing Customer Record";
            String changedTo = "Edited customer record";

            if (loggedInUser == null) {
                loggedInUser = cust;
                logger.log(Level.WARNING, "Payment Bean, editCustomerDetail: The logged in user is NULL");
            }
            auditLogFacade.audit(loggedInUser, cust, "editCustomerDetails", auditDetails, changedFrom, changedTo);

        } else {
            logger.log(Level.WARNING, "editCustomerDetail Response: Error - {0}, Data - {1}", new Object[]{editCustomerDetail.getErrorMessage().getValue(), editCustomerDetail.getData().getValue()});

        }
        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<String> clearSchedule(Customers cust, boolean keepManualPayments, String loggedInUser, String digitalKey) {
        // This method will remove payments that exist in the payment schedule for the given
        // customer. You can control whether all payments are deleted, or if you wish to preserve
        // any manually added payments, and delete an ongoing cyclic schedule.

        String result = cust.getId().toString() + ",FAILED";
        String eziDebitCustomerId = ""; // use our reference instead. THis must be an empty string.

        String ourSystemCustomerReference = cust.getId().toString();

        String keepManualPaymentsString = "NO";// update all specified payments for customer 
        if (keepManualPayments) {
            keepManualPaymentsString = "YES";// maintain any one off or ad-hoc payment amounts
        }
        if (loggedInUser.length() > 50) {
            loggedInUser = loggedInUser.substring(0, 50);
            logger.log(Level.WARNING, "clearSchedule loggedInUser is greater than the allowed 50 characters. Truncating! to 50 chars");
        }
        EziResponseOfstring eziResponse = getWs().clearSchedule(digitalKey, eziDebitCustomerId, ourSystemCustomerReference, keepManualPaymentsString, loggedInUser);
        logger.log(Level.INFO, "clearSchedule Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value

            if (eziResponse.getData().getValue().compareTo("S") == 0) {
                String auditDetails = "Cleared scheduled for  :" + cust.getUsername() + ".  Keep Manual Payments: " + keepManualPayments;
                String changedFrom = "From Date:" + cust.getPaymentParameters().getPaymentPeriod();
                String changedTo = "Cleared Schedule";
                auditLogFacade.audit(customersFacade.findCustomerByUsername(loggedInUser), cust, "clearSchedule", auditDetails, changedFrom, changedTo);
                result = cust.getId().toString() + ",OK";
                return new AsyncResult<>(result);
            } else {
                logger.log(Level.WARNING, "clearSchedule Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
            }
        } else {
            logger.log(Level.WARNING, "clearSchedule Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

        }

        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<String> deletePayment(Customers cust, Date debitDate, Long paymentAmountInCents, String paymentReference, String loggedInUser, String digitalKey) {
        	//  This method will delete a single payment from the Customer's payment schedule.
        //  It is important to note the following when deleting a payment:

        //  Only scheduled payments with a PaymentStatus of 'W' can be deleted;
        //  A specified PaymentReference value will override the debit date and payment
        //  amount when identifying the payment to be deleted;
        //  Values for both DebitDate and PaymentAmountInCents must be provided in order
        //  to identify a payment;
        //  If you provide values for DebitDate and PaymentAmountInCents and there is more
        //  than one payment for PaymentAmountInCents scheduled on DebitDate, then only
        //  one of the payments will be deleted.
        if (paymentReference == null) {
            paymentReference = "";
        }
        if (paymentAmountInCents == null) {
            paymentAmountInCents = (long) -1;
        }

        String result = "ERROR:" + paymentReference + ":";
        if (paymentReference.isEmpty() && (debitDate == null || cust == null || paymentAmountInCents < 0)) {
            logger.log(Level.WARNING, "deletePayment NULL parameter of Amount < 0. cust {0}, date {1}, Amount {2}", new Object[]{cust, debitDate, paymentAmountInCents});
            result += "NULL parameter";
            return new AsyncResult<>(result);
        }

        String eziDebitCustomerId = ""; // use our reference instead. THis must be an empty string.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String debitDateString = "";
        if (debitDate != null) {
            debitDateString = sdf.format(debitDate);
        }
        String ourSystemCustomerReference = cust.getId().toString();

        paymentReference = paymentReference.trim();
        if (paymentReference.length() > 50) {
            paymentReference = paymentReference.substring(0, 50);
            logger.log(Level.WARNING, "deletePayment paymentReference is greater than the allowed 50 characters. Truncating! to 50 chars");
        }
        if (loggedInUser.length() > 50) {
            loggedInUser = loggedInUser.substring(0, 50);
            logger.log(Level.WARNING, "deletePayment loggedInUser is greater than the allowed 50 characters. Truncating! to 50 chars");
        }
        EziResponseOfstring eziResponse;
        try {
            if (paymentReference.isEmpty()) {
                eziResponse = getWs().deletePayment(digitalKey, eziDebitCustomerId, ourSystemCustomerReference, "", debitDateString, paymentAmountInCents, loggedInUser);
                logger.log(Level.INFO, "deletePayment - the paymentReference is  NULL so date and amount will be used to identify the payment.If there are two then only one will be deleted.");
            } else {
                eziResponse = getWs().deletePayment(digitalKey, eziDebitCustomerId, ourSystemCustomerReference, paymentReference, debitDateString, new Long(0), loggedInUser);
                logger.log(Level.INFO, "deletePayment - the paymentReference is being used to identify the payment");
            }
        } catch (Exception e) {
            if (e.getMessage().contains("Your update could not be processed at this time")) {
                logger.log(Level.WARNING, "deletePayment Method Response: Gateway is busy and cannot process at this time. Will retry up tpo 10 times ");
            } else if (e.getMessage().contains("Payment selected for deletion could not be found")) {
                logger.log(Level.WARNING, "The payment wasn't found in the payment gateway!");
            } else {
                logger.log(Level.WARNING, "deletePayment Method Response: ", e);
            }

            String errorMessage = "ERROR:" + paymentReference + ":";
            if (e.getMessage() != null) {
                errorMessage += e.getMessage();
            }
            return new AsyncResult<>(errorMessage);
        }
        if (eziResponse != null) {
            if (eziResponse.getError() == 0) {// any errors will be a non zero value
                logger.log(Level.INFO, "deletePayment Response: OK Data - {0}", new Object[]{eziResponse.getData().getValue()});
                if (eziResponse.getData().getValue().compareTo("S") == 0) {
                    result = paymentReference;
                    String auditDetails = "Debit Date:" + debitDateString + ", Amount (cents): " + paymentAmountInCents.toString() + ", Payment Ref:" + paymentReference;
                    String changedFrom = "Ref:" + paymentReference;
                    String changedTo = "Deleted";
                    try {
                        auditLogFacade.audit(customersFacade.findCustomerByUsername(loggedInUser), cust, "deletePayment", auditDetails, changedFrom, changedTo);

                    } catch (Exception e) {
                        logger.log(Level.WARNING, "deletePayment Audit logging failed : Customer - {0}, logged In User - {1}", new Object[]{cust.getUsername(), loggedInUser});
                    }
                    return new AsyncResult<>(result);
                } else {
                    logger.log(Level.WARNING, "deletePayment Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
                }
            } else {
                logger.log(Level.WARNING, "deletePayment Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
                String errorMessage = "ERROR:" + paymentReference + ":";
                if (eziResponse.getErrorMessage() != null) {
                    errorMessage += eziResponse.getErrorMessage();
                }
                return new AsyncResult<>(errorMessage);
            }
        } else {
            logger.log(Level.WARNING, "deletePayment - the EziResponseOfstring is  NULL ");
        }

        return new AsyncResult<>(result);
    }

    /*   @Asynchronous
     public Future<String> deletePaymentByRef(Customers cust, String paymentReference, String loggedInUser, String digitalKey) {
     //  This method will delete a single payment from the Customer's payment schedule.
     //  It is important to note the following when deleting a payment:

     //  Only scheduled payments with a PaymentStatus of 'W' can be deleted;
     //  A specified PaymentReference value will override the debit date and payment
     //  amount when identifying the payment to be deleted;
     //  Values for both DebitDate and PaymentAmountInCents must be provided in order
     //  to identify a payment;
     //  If you provide values for DebitDate and PaymentAmountInCents and there is more
     //  than one payment for PaymentAmountInCents scheduled on DebitDate, then only
     //  one of the payments will be deleted.
     String result = paymentReference + ",FAILED";
     if (cust == null) {
     logger.log(Level.WARNING, "deletePayment Customer NULL ");
     return new AsyncResult<>(result);
     }
     if (paymentReference == null) {
     logger.log(Level.WARNING, "deletePayment paymentReference NULL ");
     return new AsyncResult<>(result);
     }
     if (paymentReference.trim().isEmpty()) {
     logger.log(Level.WARNING, "deletePayment paymentReference is empty. ");
     return new AsyncResult<>(result);
     }
     if (loggedInUser == null) {
     logger.log(Level.WARNING, "deletePayment loggedInUser NULL ");
     return new AsyncResult<>(result);
     }
     if (digitalKey == null) {
     logger.log(Level.WARNING, "deletePayment digitalKey NULL ");
     return new AsyncResult<>(result);
     }

     String eziDebitCustomerId = ""; // use our reference instead. THis must be an empty string.

     String ourSystemCustomerReference = cust.getId().toString();

     paymentReference = paymentReference.trim();
     if (paymentReference.length() > 50) {
     paymentReference = paymentReference.substring(0, 50);
     logger.log(Level.WARNING, "deletePayment paymentReference is greater than the allowed 50 characters. Truncating! to 50 chars");
     }
     if (loggedInUser.length() > 50) {
     loggedInUser = loggedInUser.substring(0, 50);
     logger.log(Level.WARNING, "deletePayment loggedInUser is greater than the allowed 50 characters. Truncating! to 50 chars");
     }
     EziResponseOfstring eziResponse = null;

     try {
     eziResponse = getWs().deletePayment(digitalKey, eziDebitCustomerId, ourSystemCustomerReference, paymentReference, "", new Long(0), loggedInUser);
     } catch (Exception e) {
     if (e.getMessage().contains("Payment selected for deletion could not be found")) {
     logger.log(Level.WARNING, "deletePayment:The Payment selected for deletion could not be found.Payment Ref: {2}, Customer {0}, logged In User - {1}", new Object[]{cust.getUsername(), loggedInUser, paymentReference});
     } else {
     logger.log(Level.INFO, "deletePayment - FAILED", e);
     }
     return new AsyncResult<>(result);
     }
     logger.log(Level.INFO, "deletePayment - the paymentReference is not NULL and is overriding date and amount to identify the payment");
     if (eziResponse != null) {
     // logger.log(Level.INFO, "deletePayment Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
     if (eziResponse.getError() == 0) {// any errors will be a non zero value

     if (eziResponse.getData().getValue().compareTo("S") == 0) {
     result = paymentReference + ",OK";
     String auditDetails = "Payment Ref:" + paymentReference;
     String changedFrom = "Ref:" + paymentReference;
     String changedTo = "Deleted";
     try {
     auditLogFacade.audit(customersFacade.findCustomerByUsername(loggedInUser), cust, "deletePayment", auditDetails, changedFrom, changedTo);
     } catch (Exception e) {
     logger.log(Level.WARNING, "deletePayment Auidt logging failed : Customer - {0}, logged In User - {1}", new Object[]{cust.getUsername(), loggedInUser});
     }
     } else {
     logger.log(Level.WARNING, "deletePayment Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
     }
     } else {
     logger.log(Level.WARNING, "deletePayment Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

     }
     } else {
     logger.log(Level.WARNING, "deletePayment - the EziResponseOfstring is  NULL ");
     }

     return new AsyncResult<>(result);
     }*/
    @Asynchronous
    public Future<Boolean> changeCustomerStatus(Customers cust, String newStatus, String loggedInUser, String digitalKey) {

        if (cust == null || newStatus == null || loggedInUser == null) {
            logger.log(Level.WARNING, "changeCustomerStatus ABORTED because cust ==null || newStatus == null || loggedInUser == null");
            return new AsyncResult<>(false);
        }

        // note: cancelled status cannot be changed with this method. i.e. cancelled is final like deleted.
        logger.log(Level.INFO, "{2} changed customer ({0}) status to {1}", new Object[]{cust.getUsername(), newStatus, loggedInUser});

        boolean result = false;
        String eziDebitCustomerId = ""; // use our reference instead. THis must be an empty string.
        String ourSystemCustomerReference = cust.getId().toString();
        String oldStatus = "Does not exist in payment gateway.";
        if (cust.getPaymentParameters() != null) {
            oldStatus = cust.getPaymentParameters().getStatusDescription();
        }
        if (newStatus.compareTo("A") == 0 || newStatus.compareTo("H") == 0 || newStatus.compareTo("C") == 0) {
            if (loggedInUser.length() > 50) {
                loggedInUser = loggedInUser.substring(0, 50);
                logger.log(Level.WARNING, "changeCustomerStatus loggedInUser is greater than the allowed 50 characters. Truncating! to 50 chars");
            }
            EziResponseOfstring eziResponse = null;
            try {
                eziResponse = getWs().changeCustomerStatus(digitalKey, eziDebitCustomerId, ourSystemCustomerReference, newStatus, loggedInUser);
            } catch (Exception e) {
                logger.log(Level.INFO, "changeCustomerStatus Response: Error - {0}, Data - {1}", e);
                return new AsyncResult<>(result);
            }
            logger.log(Level.INFO, "changeCustomerStatus Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
            if (eziResponse.getError() == 0) {// any errors will be a non zero value
                if (eziResponse.getData().getValue().compareTo("S") == 0) {
                    result = true;
                    String auditDetails = "Changed the status for :" + cust.getUsername() + " to  " + newStatus + " from " + oldStatus;
                    String changedFrom = "Old Status:" + oldStatus;
                    String changedTo = "New Status:" + newStatus;
                    auditLogFacade.audit(customersFacade.findCustomerByUsername(loggedInUser), cust, "changeCustomerStatus", auditDetails, changedFrom, changedTo);

                    logger.log(Level.INFO, "changeCustomerStatus  Successful  : ErrorCode - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

                } else {
                    logger.log(Level.WARNING, "changeCustomerStatus Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
                }

            } else {
                logger.log(Level.WARNING, "changeCustomerStatus Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
            }
        } else {
            logger.log(Level.WARNING, "changeCustomerStatus: newStatus should be A ( Active ), H ( Hold ) or C (Cancelled)  : Error - {0}", newStatus);
        }
        return new AsyncResult<>(result);
    }

    @Asynchronous
    public synchronized Future<ArrayOfScheduledPayment> getScheduledPayments(Customers cust, Date fromDate, Date toDate, String digitalKey) {

        ArrayOfScheduledPayment result = new ArrayOfScheduledPayment();
        if (cust == null) {
            logger.log(Level.WARNING, "getScheduledPayments: The customer object passed to this method is NULL.This parameter is required.Returning empty array!!");
            return new AsyncResult<>(result);
        }
        logger.log(Level.INFO, "Running asychronous task getScheduledPayments Customer {0}, From Date {1}, to Date {2}", new Object[]{cust, fromDate, toDate});
        String eziDebitCustomerId = ""; // use our reference instead. THis must be an empty string.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String fromDateString = ""; // The exact date on which the payment that you wish to move is scheduled to be deducted from your Customer's bank account or credit card.
        if (fromDate != null) {
            fromDateString = sdf.format(fromDate);
        }
        String toDateString = "";
        if (toDate != null) {
            toDateString = sdf.format(toDate);
        }

        String ourSystemCustomerReference = cust.getId().toString();
        logger.log(Level.INFO, "Payment Bean - Running async task - Getting Scheduled Payments for Customer {0}", cust.getUsername());
        EziResponseOfArrayOfScheduledPaymentTHgMB7OL eziResponse = getWs().getScheduledPayments(digitalKey, fromDateString, toDateString, eziDebitCustomerId, ourSystemCustomerReference);
        if (eziResponse.getError() == 0) {// any errors will be a non zero value
            result = eziResponse.getData().getValue();
            if (result != null) {
                logger.log(Level.INFO, "Payment Bean - Get Customer Scheduled Payments Response Recieved from ezidebit for Customer  - {0}, Number of Payments : {1} ", new Object[]{cust.getUsername(), result.getScheduledPayment().size()});
            } else {
                logger.log(Level.WARNING, "getScheduledPayments Response: Error - NULL Result ");
            }

        } else {
            logger.log(Level.WARNING, "getScheduledPayments Response: Error - {0}, ", eziResponse.getErrorMessage().getValue());

        }

        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<Boolean> addCustomer(Customers cust, String paymentGatewayName, String digitalKey, String authenticatedUser) {

        if (authenticatedUser == null) {

            logger.log(Level.INFO, "Authenticated User is NULL - Aborting add customer to ezidebit");
            return new AsyncResult<>(false);

        }

        if (cust == null || digitalKey == null || paymentGatewayName == null) {
            return new AsyncResult<>(false);
        }
        if (cust.getId() == null || digitalKey.trim().isEmpty()) {
            return new AsyncResult<>(false);
        }
        // check if customer already exists in the gateway
        // if they are in cancelled status modify the old references so a new record can be created as cancelled customers in exidebit can't be reactivated.

        CustomerDetails cd = null;
        EziResponseOfCustomerDetailsTHgMB7OL customerdetails = getWs().getCustomerDetails(digitalKey, "", cust.getId().toString());
        if (customerdetails.getError() == 0) {// any errors will be a non zero value
            logger.log(Level.INFO, "Add customer to payment gateway. Check if they already exist. Get Customer Details Response: Name - {0}", customerdetails.getData().getValue().getCustomerName().getValue());

            cd = customerdetails.getData().getValue();

        } else {
            logger.log(Level.WARNING, "Add customer to payment gateway. Check if they already exist. Get Customer Details Response: Error - {0}", customerdetails.getErrorMessage().getValue());

        }
        if (cd != null) {
            if (cd.getStatusDescription().getValue().toUpperCase().contains("CANCELLED")) {
                String ourSystemRef = cd.getYourSystemReference().getValue();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                ourSystemRef += "-CANCELLED-" + sdf.format(new Date());
                String ourGeneralReference = cust.getLastname() + " " + cust.getFirstname();

                EziResponseOfstring editCustomerDetail = getWs().editCustomerDetails(digitalKey, "", cust.getId().toString(), ourSystemRef, ourGeneralReference, cd.getCustomerName().getValue(), cd.getCustomerFirstName().getValue(), cust.getStreetAddress(), cd.getAddressLine2().getValue(), cust.getSuburb(), cust.getPostcode(), cust.getAddrState(), cust.getEmailAddress(), cust.getTelephone(), cd.getSmsPaymentReminder().getValue(), cd.getSmsFailedNotification().getValue(), cd.getSmsExpiredCard().getValue(), authenticatedUser);
                logger.log(Level.INFO, "editCustomerDetail Response: Error - {0}, Data - {1}", new Object[]{editCustomerDetail.getErrorMessage().getValue(), editCustomerDetail.getData().getValue()});
                if (editCustomerDetail.getError() == 0) {// any errors will be a non zero value

                    logger.log(Level.INFO, "Add Customer old cancelled reference edited - Response: Error - {0}, Data - {1}", new Object[]{editCustomerDetail.getErrorMessage().getValue(), editCustomerDetail.getData().getValue()});

                } else {
                    logger.log(Level.WARNING, "editCustomerDetail Response: Error - {0}, Data - {1}", new Object[]{editCustomerDetail.getErrorMessage().getValue(), editCustomerDetail.getData().getValue()});

                }
            }

        }

        boolean result = false;
        logger.log(Level.INFO, "Adding Customer  {0}", cust.getUsername());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        PaymentParameters payParams = cust.getPaymentParameters();
        String addresssLine2 = ""; // not used
        String humanFriendlyReference = cust.getId() + " " + cust.getLastname().toUpperCase() + " " + cust.getFirstname().toUpperCase(); // existing customers use this type of reference by default
        if (payParams == null && paymentGatewayName.toUpperCase().contains(paymentGateway)) {

            payParams = new PaymentParameters(0, new Date(), cust.getTelephone(), "NO", "NO", "NO", paymentGateway);
            //Customers loggedInUser = customersFacade.findCustomerByUsername(FacesContext.getCurrentInstance().getExternalContext().getRemoteUser());
            payParams.setLoggedInUser(cust);

            cust.setPaymentParameters(payParams);
            customersFacade.editAndFlush(cust);
        }

        if (payParams == null) {
            logger.log(Level.WARNING, "Payment gateway EZIDEBIT parameters not found");
            return new AsyncResult<>(false);
        }

// note - NB - for Australian Customers the
//mobile phone number must be 10
//digits long and begin with '04'. For
//New Zealand Customers the mobile
//phone number must be 10 digits
//long and begin with '02'
        String phoneNumber = cust.getTelephone();
        if (phoneNumber == null) {
            phoneNumber = "";
            logger.log(Level.INFO, "Invalid Phone Number for Customer {0}. Setting it to empty string", cust.getUsername());
        }
        Pattern p = Pattern.compile("\\d{10}");
        Matcher m = p.matcher(phoneNumber);
        //ezidebit requires an australian mobile phone number that starts with 04
        if (m.matches() == false || phoneNumber.startsWith("04") == false) {
            phoneNumber = "";
            logger.log(Level.WARNING, "Invalid Phone Number for Customer {0}. Setting it to empty string", cust.getUsername());
        }

        EziResponseOfNewCustomerXcXH3LiW addCustomerResponse = getWs().addCustomer(digitalKey, cust.getId().toString(), humanFriendlyReference, cust.getLastname(), cust.getFirstname(), cust.getStreetAddress(), addresssLine2, cust.getSuburb(), cust.getAddrState(), cust.getPostcode(), cust.getEmailAddress(), phoneNumber, sdf.format(payParams.getContractStartDate()), payParams.getSmsPaymentReminder(), payParams.getSmsFailedNotification(), payParams.getSmsExpiredCard(), payParams.getLoggedInUser().getUsername());

        if (addCustomerResponse.getError() == 0) {// any errors will be a non zero value
            result = true;
            String auditDetails = "Customer Added :" + cust.getUsername() + " to  " + paymentGateway;
            String changedFrom = "non-existant";
            String changedTo = "New Customer:";
            auditLogFacade.audit(customersFacade.findCustomerByUsername(authenticatedUser), cust, "addCustomer", auditDetails, changedFrom, changedTo);

            logger.log(Level.INFO, "Add Customer Response: Error - {0}, Data - {1}", new Object[]{addCustomerResponse.getErrorMessage().getValue(), addCustomerResponse.getData().getValue()});

        } else {
            logger.log(Level.WARNING, "Add Customer Response: Error - {0},", addCustomerResponse.getErrorMessage().getValue());

        }
        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<PaymentDetail> getPaymentDetail(String paymentReference, String digitalKey) {
        //  This method retrieves details about the given payment. It can only be used to retrieve
        //  information about payments where Ezidebit was provided with a PaymentReference.
        //  It is important to note the following when querying payment details:
        //  
        //  This method can be used to retrieve information about payments that have been
        //  scheduled by you. It cannot access information about real-time payments. Other
        //  methods are provided for retrieving real-time payment information.
        PaymentDetail result = null;

        if (paymentReference == null) {

            logger.log(Level.WARNING, "getPaymentDetail paymentReference is required but it is NULL");
            return new AsyncResult<>(result);
        }

        if (paymentReference.length() > 50) {
            paymentReference = paymentReference.substring(0, 50);
            logger.log(Level.WARNING, "getPaymentDetail paymentReference is greater than the allowed 50 characters. Truncating! to 50 chars");
        }

        EziResponseOfPaymentDetailTHgMB7OL eziResponse = getWs().getPaymentDetail(digitalKey, paymentReference);
        logger.log(Level.INFO, "getPaymentDetail Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value
            result = eziResponse.getData().getValue();

        } else {
            logger.log(Level.WARNING, "getPaymentDetail Response: Error - {0}", eziResponse.getErrorMessage().getValue());

        }

        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<Boolean> getPaymentStatus(String paymentReference, String digitalKey) {
        //  Description
        //  This method allows you to retrieve the status of a particular payment from the direct
        //  debit system where a PaymentReference has been provided.
        //  It is important to note the following when querying Payment details:
        //  • This method cannot be used to retrieve the status of BPAY or real-time credit card
        //  payments;
        //  • This method will only return the status of one payment at a time;
        //  • To use this method, you must have provided a PaymentReference when adding
        //  the payment to the Customer's schedule.
        //  
        //  Response
        //  The <Data> field in the GetPaymentStatus response will contain either:
        //  • A status of which the possible values are:
        //  - 'W' (waiting) - Payment is scheduled waiting to be sent to the bank;
        //  - 'P' (pending) - Payment request has been sent to the bank for processing
        //  and Ezidebit is waiting on a success or fail response before completing
        //  settlement;
        //  - 'S' (successful) - Payment has been successfully debited from the
        //  Customer and deposited to the client's settlement bank account;
        //  - 'D' (dishonoured) - Payment has been dishonoured by the Customer's
        //  financial institution due to insufficient funds;
        //  - 'F' (fatal dishonour) - Payment has been dishonoured by the Customer's
        //  financial institution;
        boolean result = false;

        if (paymentReference == null) {

            logger.log(Level.WARNING, "getPaymentStatus paymentReference is required but it is NULL");
            return new AsyncResult<>(result);
        }

        if (paymentReference.length() > 50) {
            paymentReference = paymentReference.substring(0, 50);
            logger.log(Level.WARNING, "getPaymentStatus paymentReference is greater than the allowed 50 characters. Truncating! to 50 chars");
        }

        EziResponseOfstring eziResponse = getWs().getPaymentStatus(digitalKey, paymentReference);
        logger.log(Level.INFO, "getPaymentStatus Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value

            if (eziResponse.getData().getValue().compareTo("S") == 0) {
                result = true;
            } else {
                logger.log(Level.WARNING, "getPaymentStatus Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
            }
        } else {
            logger.log(Level.WARNING, "getPaymentStatus Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

        }

        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<PaymentDetailPlusNextPaymentInfo> getPaymentDetailPlusNextPaymentInfo(String paymentReference, String digitalKey) {
        //  This method retrieves details about the given payment. It can only be used to retrieve
        //  information about payments where Ezidebit was provided with a PaymentReference.
        //  It is important to note the following when querying payment details:
        //  
        //  This method can be used to retrieve information about payments that have been
        //  scheduled by you. It cannot access information about real-time payments. Other
        //  methods are provided for retrieving real-time payment information.
        PaymentDetailPlusNextPaymentInfo result = null;

        if (paymentReference == null) {

            logger.log(Level.WARNING, "getPaymentDetailPlusNextPaymentInfo paymentReference is required but it is NULL");
            return new AsyncResult<>(result);
        }

        if (paymentReference.length() > 50) {
            paymentReference = paymentReference.substring(0, 50);
            logger.log(Level.WARNING, "getPaymentDetailPlusNextPaymentInfo paymentReference is greater than the allowed 50 characters. Truncating! to 50 chars");
        }

        EziResponseOfPaymentDetailPlusNextPaymentInfoTHgMB7OL eziResponse = getWs().getPaymentDetailPlusNextPaymentInfo(digitalKey, paymentReference);
        logger.log(Level.INFO, "getPaymentDetailPlusNextPaymentInfo Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value
            result = eziResponse.getData().getValue();

        } else {
            logger.log(Level.WARNING, "getPaymentDetailPlusNextPaymentInfo Response: Error - {0}, ", eziResponse.getErrorMessage().getValue());

        }

        return new AsyncResult<>(result);
    }

    @Asynchronous
    public synchronized Future<ArrayOfPayment> getPayments(Customers cust, String paymentType, String paymentMethod, String paymentSource, String paymentReference, Date fromDate, Date toDate, boolean useSettlementDate, String digitalKey) {
        //  Description
        //  	  
        //  This method allows you to retrieve payment information from across Ezidebit's various
        //  payment systems. It provides you with access to scheduled, pending and completed
        //  payments made through all payment channels.
        //  It is important to note the following when querying Payment details:
        //  • This method can be used to retrieve information about payments that have been
        //  made by your Customer through any means;
        //  • This is the recommended method for retrieving a set of payment results in a
        //  single call as most other methods are designed to provide detail about a single
        //  transaction. This method will return a full set of transactions matching the
        //  supplied criteria;
        //  • The flexibility of using a wildcard in the PaymentReference search value means
        //  that if you are adding payments with the AddPayment method as they become
        //  due, you can provide structured PaymentReferences that will allow you to group
        //  or batch payments in a way that you see fit;
        //  • Ezidebit only processes settlement deposits to clients once a day. Since the
        //  "SUCCESS" criteria for a scheduled direct debit payment is set when the payment
        //  is deposited to the client, the combination of
        //  PaymentType=ALL
        //  DateField=SETTLEMENT
        //  DateFrom={LastSuccessfulPaymentDate + 1}
        //  DateTo={currentDate}
        //  will provide you with all payments that have been made to the client since the
        //  last time your system received payment information.

        ArrayOfPayment result = new ArrayOfPayment();
        if (cust == null) {
            logger.log(Level.WARNING, "getPayments: The customer object passed to this method is NULL.This parameter is required.Returning empty array!!");
            return new AsyncResult<>(result);
        }
        logger.log(Level.INFO, "Running asychronous task getPayments Customer {0}, From Date {1}, to Date {2}", new Object[]{cust, fromDate, toDate});
        if (paymentType.compareTo("ALL") != 0 && paymentType.compareTo("PENDING") != 0 && paymentType.compareTo("FAILED") != 0 && paymentType.compareTo("SUCCESSFUL") != 0) {
            logger.log(Level.WARNING, "getPayments: payment Type is required and should be either ALL,PENDING,FAILED,SUCCESSFUL.  Returning null as this parameter is required.");
            return new AsyncResult<>(result);
        }
        if (paymentMethod.compareTo("ALL") != 0 && paymentMethod.compareTo("CR") != 0 && paymentMethod.compareTo("DR") != 0) {
            logger.log(Level.WARNING, "getPayments: payment Method is required and should be either ALL,CR,DR.  Returning null as this parameter is required.");
            return new AsyncResult<>(result);
        }
        if (paymentSource.compareTo("ALL") != 0 && paymentSource.compareTo("SCHEDULED") != 0 && paymentSource.compareTo("WEB") != 0 && paymentSource.compareTo("PHONE") != 0 && paymentSource.compareTo("BPAY") != 0) {
            logger.log(Level.WARNING, "getPayments: paymentSource is required and should be either ALL,SCHEDULED,WEB,PHONE,BPAY.  Returning null as this parameter is required.");
            return new AsyncResult<>(result);
        }
        if (paymentReference == null) {
            paymentReference = "";
        }
        if (paymentReference.length() > 50) {
            paymentReference = paymentReference.substring(0, 50);
            logger.log(Level.WARNING, "getPayments paymentReference is greater than the allowed 50 characters. Truncating! to 50 chars");
        }

        String dateField = "PAYMENT";
        if (useSettlementDate == true) {
            dateField = "SETTLEMENT";
        }
        String eziDebitCustomerId = ""; // use our reference instead. THis must be an empty string.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String fromDateString = ""; // The exact date on which the payment that you wish to move is scheduled to be deducted from your Customer's bank account or credit card.
        if (fromDate != null) {
            fromDateString = sdf.format(fromDate);
        }
        String toDateString = "";
        if (toDate != null) {
            toDateString = sdf.format(toDate);
        }

        String ourSystemCustomerReference = cust.getId().toString();
        logger.log(Level.INFO, "Payment Bean - Running async task - Getting Payments for Customer {0}", cust.getUsername());
        EziResponseOfArrayOfPaymentTHgMB7OL eziResponse = getWs().getPayments(digitalKey, paymentType, paymentMethod, paymentSource, paymentReference, fromDateString, toDateString, dateField, eziDebitCustomerId, ourSystemCustomerReference);
        // logger.log(Level.INFO, "getPayments Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value
            result = eziResponse.getData().getValue();
            if (result != null) {
                logger.log(Level.INFO, "Payment Bean - Get Customer Payments Response Recieved from ezidebit for Customer  - {0}, Number of Payments : {1} ", new Object[]{cust.getUsername(), result.getPayment().size()});
            } else {
                logger.log(Level.WARNING, "getPayments Response: Error - NULL Result ");
            }
        } else {
            logger.log(Level.WARNING, "getPayments Response: Error - {0}, ", eziResponse.getErrorMessage().getValue());

        }

        return new AsyncResult<>(result);
    }

    @Asynchronous
    public synchronized Future<ArrayOfPayment> getAllPaymentsBySystemSinceDate(Date fromDate, Date endDate, boolean useSettlementDate, String digitalKey) {
        //  Description
        //  	  
        //  This method allows you to retrieve payment information from across Ezidebit's various
        //  payment systems. It provides you with access to scheduled, pending and completed
        //  payments made through all payment channels.
        //  It is important to note the following when querying Payment details:
        //  • This method can be used to retrieve information about payments that have been
        //  made by your Customer through any means;
        //  • This is the recommended method for retrieving a set of payment results in a
        //  single call as most other methods are designed to provide detail about a single
        //  transaction. This method will return a full set of transactions matching the
        //  supplied criteria;
        //  • The flexibility of using a wildcard in the PaymentReference search value means
        //  that if you are adding payments with the AddPayment method as they become
        //  due, you can provide structured PaymentReferences that will allow you to group
        //  or batch payments in a way that you see fit;
        //  • Ezidebit only processes settlement deposits to clients once a day. Since the
        //  "SUCCESS" criteria for a scheduled direct debit payment is set when the payment
        //  is deposited to the client, the combination of
        //  PaymentType=ALL
        //  DateField=SETTLEMENT
        //  DateFrom={LastSuccessfulPaymentDate + 1}
        //  DateTo={currentDate}
        //  will provide you with all payments that have been made to the client since the
        //  last time your system received payment information.
        ArrayOfPayment result = new ArrayOfPayment();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String fromDateString = ""; // The exact date on which the payment that you wish to move is scheduled to be deducted from your Customer's bank account or credit card.
        if (fromDate != null) {
            fromDateString = sdf.format(fromDate);
        }
        String toDate = sdf.format(endDate);

        String dateField = "PAYMENT";
        if (useSettlementDate == true) {
            dateField = "SETTLEMENT";
        }
        logger.log(Level.INFO, "getAllPaymentsBySystemSinceDate - Calling ezidebit WS, From Date {0}, To Date {1}, report Type {2}", new Object[]{fromDateString, toDate, dateField});

        EziResponseOfArrayOfPaymentTHgMB7OL eziResponse = getWs().getPayments(digitalKey, "ALL", "ALL", "ALL", "", fromDateString, toDate, dateField, "", "");
        if (eziResponse.getError() == 0) {// any errors will be a non zero value
            logger.log(Level.INFO, "getAllPaymentsBySystemSinceDate Response:OK - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

            result = eziResponse.getData().getValue();
            if (result.getPayment() != null) {
                logger.log(Level.INFO, "getAllPaymentsBySystemSinceDate Response: OK {0}, No of Payments in List = {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue().getPayment().size()});

            }

        } else {
            logger.log(Level.WARNING, "getAllPaymentsBySystemSinceDate Response: Error - {0}, ", eziResponse.getErrorMessage().getValue());

        }

        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<String> addPayment(Customers cust, Date debitDate, Long paymentAmountInCents, String paymentReference, String loggedInUser, String digitalKey) {
        // paymentReference Max 50 chars. It can be search with with a wildcard in other methods. Use invoice number or other payment identifier
        logger.log(Level.INFO, "running asychronous task addPayment Customer {0}, debitDate {1}, paymentAmountInCents {2}", new Object[]{cust, debitDate, paymentAmountInCents});
        String result = paymentReference;
        String eziDebitCustomerId = ""; // use our reference instead. THis must be an empty string.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String debitDateString = sdf.format(debitDate);
        String ourSystemCustomerReference = cust.getId().toString();

        if (paymentReference.length() > 50) {
            paymentReference = paymentReference.substring(0, 50);
            logger.log(Level.WARNING, "addPayment paymentReference is greater than the allowed 50 characters. Truncating! to 50 chars");
        }
        if (loggedInUser.length() > 50) {
            loggedInUser = loggedInUser.substring(0, 50);
            logger.log(Level.WARNING, "addPayment loggedInUser is greater than the allowed 50 characters. Truncating! to 50 chars");
        }
        EziResponseOfstring eziResponse;
        try {
            eziResponse = getWs().addPayment(digitalKey, eziDebitCustomerId, ourSystemCustomerReference, debitDateString, paymentAmountInCents, paymentReference, loggedInUser);
        } catch (Exception e) {
            if (e.getMessage().contains("Your update could not be processed at this time")) {
                logger.log(Level.WARNING, "addPayment Method Response: Gateway is busy and cannot process at this time. Will retry up tpo 10 times ");
            } else if (e.getMessage().contains("This customer already has two payments on this date")) {
                logger.log(Level.WARNING, "addPayment Method Response: Add payment DENIED - This customer already has two payments on this date. This is an ezidebit payment gateway restriction");
            } else {
                logger.log(Level.WARNING, "addPayment Method Response: ", e);
            }

            String errorMessage = "ERROR:" + paymentReference + ":";
            if (e.getMessage() != null) {
                errorMessage += e.getMessage();
            }
            return new AsyncResult<>(errorMessage);
        }
        if (eziResponse.getError() == 0) {// any errors will be a non zero value
            logger.log(Level.INFO, "addPayment Response: Successful. Reference:{0}, Customer: {2}, Return Value: - {1}", new Object[]{paymentReference, eziResponse.getData().getValue(), cust.getUsername()});

            if (eziResponse.getData().getValue().compareTo("S") == 0) {

                String auditDetails = "Payment Added - Debit Date:" + debitDateString + ", Amount (cents): " + paymentAmountInCents.toString() + ", Payment Ref:" + paymentReference;
                String changedFrom = "non-existent";
                String changedTo = "Ref:" + paymentReference;
                auditLogFacade.audit(customersFacade.findCustomerByUsername(loggedInUser), cust, "addPayment", auditDetails, changedFrom, changedTo);

            } else {

                logger.log(Level.WARNING, "addPayment Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
                String errorMessage = "ERROR:" + paymentReference + ":";
                if (eziResponse.getErrorMessage() != null) {
                    errorMessage += eziResponse.getErrorMessage();
                }
                return new AsyncResult<>(errorMessage);
            }
        } else {
            logger.log(Level.WARNING, "addPayment Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

        }

        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<Boolean> createSchedule(Customers cust, Date scheduleStartDate, char schedulePeriodType, String dayOfWeek, int dayOfMonth, boolean firstWeekOfMonth, boolean secondWeekOfMonth, boolean thirdWeekOfMonth, boolean fourthWeekOfMonth, long paymentAmountInCents, int limitToNumberOfPayments, long limitToTotalAmountInCent, boolean keepManualPayments, String loggedInUser, String digitalKey) {
        //This method allows you to create a new schedule for the given Customer. It will create a
        //schedule for on-going debits (up to one year’s worth of payments will exist at a point in
        //time), or will calculate a schedule to fulfil a required total payment amount, or number of
        //payments.
        //It is important to note the following when creating a payment schedule:
        //• This function will first remove an existing payment schedule and then create a
        //new payment schedule in accordance with your parameters;
        //• You can choose whether to maintain or delete any payments that were manually
        //added to the payment schedule with the AddPayment method by specifying "YES"
        //or "NO" respectively in the KeepManualPayments parameter;
        //• When creating a new schedule for a fixed amount or fixed number of payments,
        //the calculation will not consider any payments already made by the Customer;
        //• When a schedule is created, a series of individual payment records are created in
        //the Ezidebit system, which can be altered independently of each other;
        //• For on-going Customers, when a debit is processed (or removed from the
        //schedule for Customers in a non-processing status), a new debit is added to the
        //end of their existing schedules, at the frequency specified when the schedule was
        //created;
        //• For Customers on a fixed number of payments, or total amount owing, if a
        //payment is unsuccessful, it will cause a new debit to be scheduled at the end of
        //the existing schedule for the correct amount, at the frequency specified when the
        //schedule was created;
        //• Ezidebit will not schedule payments for weekend days, instead updating the
        //scheduled debit record to reflect the fact that it will be drawn from the
        //Customer's payment method on the next business-banking day.
        boolean result = false;
        String keepManualPaymentsString = "NO";// update all specified payments for customer 
        if (keepManualPayments) {
            keepManualPaymentsString = "YES";// maintain any one off or ad-hoc payment amounts
        }
        String firstWeekOfMonthString = "NO";
        if (firstWeekOfMonth) {
            firstWeekOfMonthString = "YES";
        }
        String secondWeekOfMonthString = "NO";
        if (secondWeekOfMonth) {
            secondWeekOfMonthString = "YES";
        }
        String thirdWeekOfMonthString = "NO";
        if (thirdWeekOfMonth) {
            thirdWeekOfMonthString = "YES";
        }
        String fourthWeekOfMonthString = "NO";
        if (fourthWeekOfMonth) {
            fourthWeekOfMonthString = "YES";
        }

        String eziDebitCustomerId = ""; // use our reference instead. THis must be an empty string.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String scheduleStartDateString = sdf.format(scheduleStartDate);
        String ourSystemCustomerReference = cust.getId().toString();
        if (schedulePeriodType == 'W' || schedulePeriodType == 'F' || schedulePeriodType == 'N' || schedulePeriodType == '4') {
            if (dayOfWeek == null || dayOfWeek.trim().isEmpty()) {
                logger.log(Level.WARNING, "createSchedule A value must be provided for dayOfWeek \n" + " when the\n" + "SchedulePeriodType is in\n" + "W,F,4,N");
                return new AsyncResult<>(false);
            }
        }
        if (schedulePeriodType == 'M') {
            if (dayOfMonth < 1 || dayOfMonth > 31) {
                logger.log(Level.WARNING, "createSchedule: A value must be provided for dayOfMonth (1..31 )\n" + " when the\n" + "SchedulePeriodType is in\n" + "M");
                return new AsyncResult<>(false);
            }
        } else {
            dayOfMonth = 0;
        }
        String schedulePeriodTypeString = "";
        schedulePeriodTypeString = schedulePeriodTypeString + schedulePeriodType;

        if (loggedInUser.length() > 50) {
            loggedInUser = loggedInUser.substring(0, 50);
            logger.log(Level.WARNING, "createSchedule loggedInUser is greater than the allowed 50 characters. Truncating! to 50 chars");
        }
        if (schedulePeriodType == 'W' || schedulePeriodType == 'F' || schedulePeriodType == 'M' || schedulePeriodType == '4' || schedulePeriodType == 'N' || schedulePeriodType == 'Q' || schedulePeriodType == 'H' || schedulePeriodType == 'Y') {

            EziResponseOfstring eziResponse = getWs().createSchedule(digitalKey, eziDebitCustomerId, ourSystemCustomerReference, scheduleStartDateString, schedulePeriodTypeString, dayOfWeek, dayOfMonth, firstWeekOfMonthString, secondWeekOfMonthString, thirdWeekOfMonthString, fourthWeekOfMonthString, paymentAmountInCents, limitToNumberOfPayments, limitToTotalAmountInCent, keepManualPaymentsString, loggedInUser);
            logger.log(Level.INFO, "createSchedule Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
            if (eziResponse.getError() == 0) {// any errors will be a non zero value

                if (eziResponse.getData().getValue().compareTo("S") == 0) {
                    result = true;
                    String auditDetails = "Created scheduled for  :" + cust.getUsername() + ".  Keep Manual Payments: " + keepManualPayments + ", start:" + scheduleStartDateString + ",Period Type:" + schedulePeriodTypeString + ",Amount:" + paymentAmountInCents;
                    String changedFrom = "From Date:" + cust.getPaymentParameters().getPaymentPeriod();
                    String changedTo = "New Schedule";
                    auditLogFacade.audit(customersFacade.findCustomerByUsername(loggedInUser), cust, "createSchedule", auditDetails, changedFrom, changedTo);

                } else {
                    logger.log(Level.WARNING, "createSchedule Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
                }
            } else {
                logger.log(Level.WARNING, "createSchedule Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

            }
        } else {
            logger.log(Level.WARNING, "createSchedule : schedulePeriodType Possible values are:\\n'W' - Weekly\\n'F' - Fortnightly\\n'M' - Monthly\\n'4' - 4 Weekly\\n'N' - Weekday in month (e.g.\\nMonday in the third week of\\nevery month)\\n'Q' - Quarterly\\n'H' - Half Yearly (6 Monthly)\\n'Y' - Yearly\\nThe frequency is applied to the\\npayment scheduled beginning\\nfrom the date defined in\\nScheduledStartDate\\n Incorect value that was submitted = ", schedulePeriodType);

        }

        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<Boolean> changeScheduledAmount(Customers cust, Date changeFromDate, Long newPaymentAmountInCents, int changeFromPaymentNumber, boolean applyToAllFuturePayments, boolean keepManualPayments, String loggedInUser, String digitalKey) {
        // Only scheduled payments with a status of 'W' can have their scheduled debit amounts changed;
        // This method will only change the debit amounts of scheduled payments - the debit dates will still remain the same.

        boolean result = false;
        String applyToAllFuturePaymentsString = "NO";// apply to earliest payment
        if (applyToAllFuturePayments) {
            applyToAllFuturePaymentsString = "YES"; //applied to all (YES) payments that occur on or after the position identified by the ChangeFromPaymentNumber or ChangeFromPaymentDate
        }
        String keepManualPaymentsString = "NO";// update all specified payments for customer 
        if (keepManualPayments) {
            keepManualPaymentsString = "YES";// maintain any one off or ad-hoc payment amounts
        }

        String eziDebitCustomerId = ""; // use our reference instead. THis must be an empty string.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String changeFromDateString = sdf.format(changeFromDate);
        String ourSystemCustomerReference = cust.getId().toString();

        if (loggedInUser.length() > 50) {
            loggedInUser = loggedInUser.substring(0, 50);
            logger.log(Level.WARNING, "changeScheduledAmount loggedInUser is greater than the allowed 50 characters. Truncating! to 50 chars");
        }
        EziResponseOfstring eziResponse = getWs().changeScheduledAmount(digitalKey, eziDebitCustomerId, ourSystemCustomerReference, changeFromPaymentNumber, changeFromDateString, newPaymentAmountInCents, applyToAllFuturePaymentsString, keepManualPaymentsString, loggedInUser);
        logger.log(Level.INFO, "changeScheduledAmount Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value

            if (eziResponse.getData().getValue().compareTo("S") == 0) {
                result = true;
                String auditDetails = "Changed the scheduled Amount for  :" + cust.getUsername() + " from  " + changeFromDateString + " to Amount in cents : " + newPaymentAmountInCents;
                String changedFrom = "From Date:" + changeFromDateString;
                String changedTo = "New Amount (cents):" + newPaymentAmountInCents;
                auditLogFacade.audit(customersFacade.findCustomerByUsername(loggedInUser), cust, "addCustomer", auditDetails, changedFrom, changedTo);

            } else {
                logger.log(Level.WARNING, "changeScheduledAmount Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
            }
        } else {
            logger.log(Level.WARNING, "changeScheduledAmount Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

        }

        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<Boolean> changeScheduledDate(Customers cust, Date changeFromDate, Date changeToDate, String paymentReference, boolean keepManualPayments, String loggedInUser, String digitalKey) {
        // PaymentReference - If you used a specific PaymentReference when adding a payment using the AddPayment Method, then you can use that value here to exactly identify that payment within the Customer's schedule.
        // NB - You must provide a value for either ChangeFromDate or PaymentReference to identify the payment.
        boolean result = false;
        if (paymentReference.trim().isEmpty()) {
            paymentReference = null;
        }
        String keepManualPaymentsString = "NO";// update all specified payments for customer 
        if (keepManualPayments) {
            keepManualPaymentsString = "YES";// maintain any one off or ad-hoc payment amounts
        }

        String eziDebitCustomerId = ""; // use our reference instead. THis must be an empty string.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String changeFromDateString = ""; // The exact date on which the payment that you wish to move is scheduled to be deducted from your Customer's bank account or credit card.
        String changeToDateString = sdf.format(changeToDate); // The new date that you wish for this payment to be deducted from your Customer's bank account or credit card.
        String ourSystemCustomerReference = cust.getId().toString();

        if (changeFromDate != null && paymentReference == null) {
            changeFromDateString = sdf.format(changeFromDate);
            paymentReference = "";
        }

        if (loggedInUser.length() > 50) {
            loggedInUser = loggedInUser.substring(0, 50);
            logger.log(Level.WARNING, "changeScheduledDate loggedInUser is greater than the allowed 50 characters. Truncating! to 50 chars");
        }
        EziResponseOfstring eziResponse = getWs().changeScheduledDate(digitalKey, eziDebitCustomerId, ourSystemCustomerReference, changeFromDateString, paymentReference, changeToDateString, keepManualPaymentsString, loggedInUser);
        logger.log(Level.INFO, "changeScheduledDate Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value

            if (eziResponse.getData().getValue().compareTo("S") == 0) {
                result = true;
                String auditDetails = "Changed the scheduled Date for  :" + cust.getUsername() + " from  " + changeFromDateString + " to Date: " + changeToDateString + ", Ref:" + paymentReference + ", Keep Manual Payments =" + keepManualPaymentsString;
                String changedFrom = "From Date:" + changeFromDateString;
                String changedTo = "New Date:" + changeToDateString;
                auditLogFacade.audit(customersFacade.findCustomerByUsername(loggedInUser), cust, "changeScheduledDate", auditDetails, changedFrom, changedTo);

            } else {
                logger.log(Level.WARNING, "changeScheduledDate Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
            }
        } else {
            logger.log(Level.WARNING, "changeScheduledDate Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

        }

        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<Boolean> isBsbValid(String bsb, String digitalKey) {
        //  Description
        //  check that the BSB is valid
        boolean result = false;

        EziResponseOfstring eziResponse = getWs().isBsbValid(digitalKey, bsb);
        logger.log(Level.INFO, "isBsbValid Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value
            String valid = eziResponse.getData().getValue();
            if (valid.compareTo("YES") == 0) {
                return new AsyncResult<>(true);
            }

        } else {
            logger.log(Level.WARNING, "isBsbValid Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

        }

        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<Boolean> isSystemLocked(String digitalKey) {
        //  Description
        //  check that the BSB is valid
        boolean result = false;

        EziResponseOfstring eziResponse = getWs().isSystemLocked(digitalKey);
        logger.log(Level.INFO, "isSystemLocked Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value
            String valid = eziResponse.getData().getValue();
            if (valid.compareTo("YES") == 0) {
                return new AsyncResult<>(true);
            }

        } else {
            logger.log(Level.WARNING, "isSystemLocked Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

        }

        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<String> getPaymentExchangeVersion() {
        //  Description
        //  
        //  This method returns the version of our web services and API that you are connecting to.
        //  This can be used as a check to ensure that you're connecting to the web services that
        //  you expect to, based on the API document that you have.

        String result = "ERROR Getting version";

        EziResponseOfstring eziResponse = getWs().paymentExchangeVersion();
        logger.log(Level.INFO, "getPaymentExchangeVersion Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value
            result = eziResponse.getData().getValue();
        } else {
            logger.log(Level.WARNING, "getPaymentExchangeVersion Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        }
        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<Boolean> sendAsynchEmail(String to, String ccAddress, String from, String emailSubject, String message, String theAttachedfileName, Properties serverProperties, boolean debug) {
        SendHTMLEmailWithFileAttached emailAgent = new SendHTMLEmailWithFileAttached();
        try {
            emailAgent.send(to, ccAddress, from, emailSubject, message, theAttachedfileName, serverProperties, debug);
            logger.log(Level.INFO, "sendAsynchEmail TO: {0}, CC - {1}, From:{2}, Subject:{3}", new Object[]{to, ccAddress,from,emailSubject});
        } catch (Exception e) {
            String error = "Email Send Failed :" + e.getMessage();
            return new AsyncResult<>(false);
        }
        return new AsyncResult<>(true);
    }

}
