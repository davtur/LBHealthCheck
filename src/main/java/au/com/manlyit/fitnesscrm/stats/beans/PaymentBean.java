package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.beans.util.PaymentSource;
import au.com.manlyit.fitnesscrm.stats.beans.util.PaymentStatus;
import au.com.manlyit.fitnesscrm.stats.classes.EziDebitPaymentGateway;
import au.com.manlyit.fitnesscrm.stats.classes.util.AsyncJob;
import au.com.manlyit.fitnesscrm.stats.classes.util.BatchOfPaymentJobs;
import au.com.manlyit.fitnesscrm.stats.classes.util.FutureMapEJB;
import au.com.manlyit.fitnesscrm.stats.classes.util.PaymentGatewayResponse;
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
import au.com.manlyit.fitnesscrm.stats.webservices.Payment;
import au.com.manlyit.fitnesscrm.stats.webservices.PaymentDetail;
import au.com.manlyit.fitnesscrm.stats.webservices.PaymentDetailPlusNextPaymentInfo;
import au.com.manlyit.fitnesscrm.stats.webservices.ScheduledPayment;

import java.io.Serializable;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import java.util.logging.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.WebServiceException;

/**
 *
 * @author david
 */
@Named
@Stateless
public class PaymentBean implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(PaymentBean.class.getName());
    private static final String PAYMENT_GATEWAY = "EZIDEBIT";
    private static final int PAYMENT_SCHEDULE_MONTHS_AHEAD = 11;

    private static final long serialVersionUID = 1L;

    @Inject
    private CustomersFacade customersFacade;
    @Inject
    private ConfigMapFacade configMapFacade;
    @Inject
    private AuditLogFacade auditLogFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.EmailTemplatesFacade ejbEmailTemplatesFacade;
    @Inject
    private PaymentsFacade paymentsFacade;

    @Inject
    private PaymentParametersFacade ejbPaymentParametersFacade;
    @Inject
    private FutureMapEJB futureMap;

    @Inject
    private EziDebitPaymentGateway eziDebit;

    private INonPCIService getWs() {
        /*URL url = null;
        WebServiceException e = null;
        try {
            url = new URL(configMapFacade.getConfig("payment.ezidebit.gateway.url"));
        } catch (MalformedURLException ex) {

            LOGGER.log(Level.SEVERE, "MalformedURLException - payment.ezidebit.gateway.url", ex);

        }
        return new NonPCIService(url).getBasicHttpBindingINonPCIService();*/

        return futureMap.getWs();
    }

    @TransactionAttribute(TransactionAttributeType.NEVER)// we don't want a transaction for this method as teh call within this method will invoke their own transactions
    @Asynchronous
    public Future<PaymentGatewayResponse> createCRMPaymentSchedule(Customers cust, Date scheduleStartDate, Date scheduleEndDate, char schedulePeriodType, int payDayOfWeek, int dayOfMonth, long amountInCents, int limitToNumberOfPayments, long paymentAmountLimitInCents, boolean keepManualPayments, boolean firstWeekOfMonth, boolean secondWeekOfMonth, boolean thirdWeekOfMonth, boolean fourthWeekOfMonth, String loggedInUser, String sessionId, String digitalKey, FutureMapEJB futureMap, PaymentBean payBean, boolean isScheduleUpdate) {

        PaymentGatewayResponse pgr = new PaymentGatewayResponse(false, cust, "", "-1", "An unhandled error occurred!");

        try {
            if (schedulePeriodType == 'W' || schedulePeriodType == 'F' || schedulePeriodType == 'N' || schedulePeriodType == '4') {
                if (payDayOfWeek < 1 || payDayOfWeek > 7) {
                    LOGGER.log(Level.WARNING, "createSchedule  FAILED: A value must be provided for dayOfWeek  when the SchedulePeriodType is  W,F,4,N");
                    pgr = new PaymentGatewayResponse(false, cust, "", "-1", "createSchedule  FAILED: A value must be provided for dayOfWeek  when the SchedulePeriodType is  W,F,4,N");
                    futureMap.processClearSchedule(sessionId, pgr);
                    return new AsyncResult<>(pgr);
                }
                if (payDayOfWeek < 2 || payDayOfWeek > 6) {
                    payDayOfWeek = 2;// can't debit on weekends only weekdays
                }
            }
            if (schedulePeriodType == 'M') {
                if (dayOfMonth < 1 || dayOfMonth > 31) {
                    LOGGER.log(Level.WARNING, "createSchedule FAILED: A value must be provided for dayOfMonth (1..31 )\n" + " when the\n" + "SchedulePeriodType is in\n" + "M");
                    pgr = new PaymentGatewayResponse(false, cust, "", "-1", "createSchedule FAILED: A value must be provided for dayOfMonth (1..31 )\n" + " when the\n" + "SchedulePeriodType is in\n" + "M");
                    futureMap.processClearSchedule(sessionId, pgr);
                    return new AsyncResult<>(pgr);
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
                LOGGER.log(Level.WARNING, "createSchedule FAILED: A value must be provided for week Of Month  when the SchedulePeriodType is N");
                pgr = new PaymentGatewayResponse(false, cust, "", "-1", "createSchedule FAILED: A value must be provided for week Of Month  when the SchedulePeriodType is N");
                futureMap.processClearSchedule(sessionId, pgr);
                return new AsyncResult<>(pgr);
            }

            //delete all existing scheduled payments
            List<Payments> crmPaymentList = paymentsFacade.findScheduledPaymentsByCustomer(cust, true);
            if (crmPaymentList != null) {
                LOGGER.log(Level.INFO, "createSchedule - Found {0} existing scheduled payments for {1}", new Object[]{crmPaymentList.size(), cust.getUsername()});

                ArrayList<Integer> jobIds = new ArrayList<>();
                BatchOfPaymentJobs bopj = new BatchOfPaymentJobs("DeletePaymentBatch", new ArrayList<>());
                futureMap.addBatchJobToList(sessionId, bopj);
                PaymentGatewayResponse delbatchResp = new PaymentGatewayResponse(true, cust, "", "0", "createCRMPaymentSchedule DeletePaymentBatch  Completed");
                if (isScheduleUpdate == false) {
                    for (int x = crmPaymentList.size() - 1; x > -1; x--) {
                        Payments p = crmPaymentList.get(x);
                        String ref = p.getId().toString();
                        boolean isManual = p.getManuallyAddedPayment();
                        if (keepManualPayments == true && isManual) {
                            LOGGER.log(Level.INFO, "createSchedule - keeping manual payment: Cust={0}, Ref={1}, Manaul Payment = {2}", new Object[]{cust.getUsername(), ref, isManual});
                        } else {
                            LOGGER.log(Level.INFO, "createSchedule - Deleting payment: Cust={0}, Ref={1}, Manaul Payment = {2}", new Object[]{cust.getUsername(), ref, isManual});
                            //paymentsFacade.remove(p); will be deleted once processed
                            p.setPaymentStatus(PaymentStatus.DELETE_REQUESTED.value());
                            paymentsFacade.editAndFlush(p);

                            bopj.getJobs().add(p.getId());
                            AsyncJob aj = new AsyncJob("DeletePayment", payBean.deletePayment(cust, null, null, p, loggedInUser, digitalKey, sessionId));
                            aj.setBatchId(bopj.getBatchId());
                            futureMap.put(sessionId, aj);
                            /*Future<PaymentGatewayResponse> deletePaymentResponseFutResp = deletePayment(cust, null, null, p, loggedInUser, digitalKey, sessionId);
                        PaymentGatewayResponse deletePaymentResponse = deletePaymentResponseFutResp.get();
                        if (deletePaymentResponse.isOperationSuccessful()) {
                            Logger.getLogger(PaymentBean.class.getName()).log(Level.INFO, "Payment Deleted Successfully - ID=", new Object[]{p.getId()});
                        } else {
                            Logger.getLogger(PaymentBean.class.getName()).log(Level.SEVERE, "Payment Deletetion FAILED - ID=", new Object[]{p.getId()});
                        }*/
                            try {
                                TimeUnit.MILLISECONDS.sleep(50);// the payment gateway has some concurrency throttling so we don't want to exceed our number of txns per second.
                            } catch (InterruptedException ex) {
                                Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                    futureMap.processDeletePaymentBatch(sessionId, delbatchResp);
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
            BatchOfPaymentJobs bopjAdd = new BatchOfPaymentJobs("AddPaymentBatch", new ArrayList<>());
            futureMap.addBatchJobToList(sessionId, bopjAdd);

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
                        if (arePaymentsWithinLimits(limitToNumberOfPayments, paymentAmountLimitInCents, cumulativeAmountInCents, amountInCents, numberOfpayments, newDebitDate)) {
                            bopjAdd.getJobs().add(addNewPayment(cust, newDebitDate, amountInCents, false, loggedInUser, sessionId, digitalKey, futureMap, payBean, bopjAdd.getBatchId()));
                            //addNewPayment(cust, newDebitDate, amountInCents, false, loggedInUser, sessionId, digitalKey, futureMap, payBean);
                            numberOfpayments++;
                        }

                    }
                    if (startCal.get(Calendar.DAY_OF_MONTH) >= 8 && startCal.get(Calendar.DAY_OF_MONTH) <= 14 && secondWeekOfMonth == true) {
                        if (arePaymentsWithinLimits(limitToNumberOfPayments, paymentAmountLimitInCents, cumulativeAmountInCents, amountInCents, numberOfpayments, newDebitDate)) {

                            bopjAdd.getJobs().add(addNewPayment(cust, newDebitDate, amountInCents, false, loggedInUser, sessionId, digitalKey, futureMap, payBean, bopjAdd.getBatchId()));

                            // addNewPayment(cust, newDebitDate, amountInCents, false, loggedInUser, sessionId, digitalKey, futureMap, payBean);
                            numberOfpayments++;
                        }

                    }
                    if (startCal.get(Calendar.DAY_OF_MONTH) >= 15 && startCal.get(Calendar.DAY_OF_MONTH) <= 21 && thirdWeekOfMonth == true) {
                        if (arePaymentsWithinLimits(limitToNumberOfPayments, paymentAmountLimitInCents, cumulativeAmountInCents, amountInCents, numberOfpayments, newDebitDate)) {
                            bopjAdd.getJobs().add(addNewPayment(cust, newDebitDate, amountInCents, false, loggedInUser, sessionId, digitalKey, futureMap, payBean, bopjAdd.getBatchId()));
                            //addNewPayment(cust, newDebitDate, amountInCents, false, loggedInUser, sessionId, digitalKey, futureMap, payBean);
                            numberOfpayments++;
                        }

                    }
                    if (startCal.get(Calendar.DAY_OF_MONTH) >= 22 && startCal.get(Calendar.DAY_OF_MONTH) <= 28 && fourthWeekOfMonth == true) {
                        if (arePaymentsWithinLimits(limitToNumberOfPayments, paymentAmountLimitInCents, cumulativeAmountInCents, amountInCents, numberOfpayments, newDebitDate)) {
                            bopjAdd.getJobs().add(addNewPayment(cust, newDebitDate, amountInCents, false, loggedInUser, sessionId, digitalKey, futureMap, payBean, bopjAdd.getBatchId()));
                            // addNewPayment(cust, newDebitDate, amountInCents, false, loggedInUser, sessionId, digitalKey, futureMap, payBean);
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
                    if (arePaymentsWithinLimits(limitToNumberOfPayments, paymentAmountLimitInCents, cumulativeAmountInCents, amountInCents, numberOfpayments, newDebitDate)) {
                        bopjAdd.getJobs().add(addNewPayment(cust, newDebitDate, amountInCents, false, loggedInUser, sessionId, digitalKey, futureMap, payBean, bopjAdd.getBatchId()));
                        //addNewPayment(cust, newDebitDate, amountInCents, false, loggedInUser, sessionId, digitalKey, futureMap, payBean);
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
                    TimeUnit.MILLISECONDS.sleep(200);
                } catch (InterruptedException ex) {
                    Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "createSchedule FAILED: Error", e);
            pgr.setErrorMessage(e.getMessage());
            futureMap.processClearSchedule(sessionId, pgr);
            return new AsyncResult<>(pgr);
        }
        PaymentGatewayResponse addbatchResp = new PaymentGatewayResponse(true, cust, "", "0", "createCRMPaymentSchedule AddPaymentBatch  Completed");
        futureMap.processAddPaymentBatch(sessionId, addbatchResp);
        pgr = new PaymentGatewayResponse(true, cust, "", "-1", "");
        futureMap.processClearSchedule(sessionId, pgr);
        return new AsyncResult<>(pgr);

    }

    private boolean arePaymentsWithinLimits(int limitToNumberOfPayments, long paymentAmountLimitInCents, long cumulativeAmountInCents, long amountInCents, int numberOfpayments, Date newDebitDate) {
        GregorianCalendar earliestDatePaymentGatewayWillAccept = new GregorianCalendar();
        GregorianCalendar debitDate = new GregorianCalendar();
        debitDate.setTime(newDebitDate);
        earliestDatePaymentGatewayWillAccept.add(Calendar.DAY_OF_MONTH, -29);
        if (earliestDatePaymentGatewayWillAccept.before(debitDate) == false) {
            return false;
        }

        if (limitToNumberOfPayments > 0 || paymentAmountLimitInCents > 0) {
            cumulativeAmountInCents += amountInCents;
            if (limitToNumberOfPayments > 0 && paymentAmountLimitInCents > 0) {
                if (limitToNumberOfPayments < numberOfpayments || paymentAmountLimitInCents < cumulativeAmountInCents) {
                    return false;
                }
            }
            if (limitToNumberOfPayments > 0 && paymentAmountLimitInCents == 0) {
                if (limitToNumberOfPayments < numberOfpayments) {
                    return false;
                }
            }
            if (limitToNumberOfPayments == 0 && paymentAmountLimitInCents > 0) {
                if (paymentAmountLimitInCents < cumulativeAmountInCents) {
                    return false;
                }
            }
            return true;

        } else {
            return true;
        }

    }

    public boolean retryDeletePayment(Payments pay, String user, String sessionId, String digitalKey, FutureMapEJB futureMap, PaymentBean payBean) {
        //String user = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        if (user != null) {

            try {
                Customers cust = pay.getCustomerName();
                Date debitDate = pay.getDebitDate();

                long amountInCents = pay.getScheduledAmount().movePointRight(2).longValue();// convert to cents
                String newPaymentID = pay.getId().toString();
                LOGGER.log(Level.INFO, "retryDeletePayment for customer {0} with paymentID: {1}", new Object[]{cust.getUsername(), newPaymentID});
                AsyncJob aj = new AsyncJob("DeletePayment", payBean.deletePayment(cust, debitDate, amountInCents, pay, user, digitalKey, sessionId));
                futureMap.put(sessionId, aj);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "retryDeletePayment failed due to exception:", e);
                return false;
            }
        } else {
            LOGGER.log(Level.WARNING, "Logged in user is null. retryDeletePayment aborted.");
            return false;
        }
        return true;
    }

    public boolean retryAddNewPayment(Payments pay, String user, String sessionId, String digitalKey, FutureMapEJB futureMap, PaymentBean payBean) {
        //String user = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        if (user != null) {

            try {
                Customers cust = pay.getCustomerName();
                Date debitDate = pay.getDebitDate();
                long amountInCents = pay.getScheduledAmount().movePointRight(2).longValue();// convert to cents
                String newPaymentID = pay.getId().toString();
                LOGGER.log(Level.INFO, "retryAddNewPayment for customer {0} with paymentID: {1}", new Object[]{cust.getUsername(), newPaymentID});
                AsyncJob aj = new AsyncJob("AddPayment", payBean.addPayment(cust, debitDate, amountInCents, pay, user, digitalKey, sessionId));
                futureMap.put(sessionId, aj);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "retryAddNewPayment failed due to exception:", e);
                return false;
            }
        } else {
            LOGGER.log(Level.WARNING, "Logged in user is null. retryAddNewPayment aborted.");
            return false;
        }
        return true;
    }

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public int addNewPayment(Customers cust, Date debitDate, long amountInCents, boolean manualPayment, String user, String sessionId, String digitalKey, FutureMapEJB futureMap, PaymentBean payBean, long batchId) {
        //String user = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        int paymentId = -1;
        if (user != null) {

            try {
                Payments newPayment = new Payments(-1);
                newPayment.setDebitDate(debitDate);
                newPayment.setEzidebitCustomerID(cust.getPaymentParametersId().getEzidebitCustomerID());
                newPayment.setPaymentSource(PaymentSource.DIRECT_DEBIT.value());
                newPayment.setCreateDatetime(new Date());
                newPayment.setLastUpdatedDatetime(new Date());
                newPayment.setYourSystemReference(cust.getId().toString());
                newPayment.setPaymentAmount(new BigDecimal(amountInCents / (long) 100));
                newPayment.setScheduledAmount(new BigDecimal(amountInCents / (long) 100));
                newPayment.setCustomerName(cust);
                newPayment.setPaymentStatus(PaymentStatus.SENT_TO_GATEWAY.value());
                newPayment.setManuallyAddedPayment(manualPayment);
                newPayment.setBankReturnCode("");
                newPayment.setBankFailedReason("");
                newPayment.setBankReceiptID("");
                newPayment.setPaymentDate(debitDate);// we can use this timestamp to reference bookings with booking date as they will be identical
                paymentsFacade.createAndFlushForGeneratedIdEntities(newPayment);// need to flush to ensure the id is generated as this onlyoccurs at flush time.
                paymentId = newPayment.getId();
                //int newId = newPayment.getId();
                // if (newId != -1) {
                //    String newPaymentID = Integer.toString(newId);
                //    newPayment.setPaymentReference(newPaymentID);
                // paymentsFacade.edit(newPayment);
                LOGGER.log(Level.INFO, "New Payment Created for customer {0} with paymentID: {1}, time: {2}", new Object[]{cust.getUsername(), newPayment.getId(), new SimpleDateFormat("dd/MM/yy HH:mm:ss.SSS").format(new Date())});
                AsyncJob aj = new AsyncJob("AddPayment", payBean.addPayment(cust, debitDate, amountInCents, newPayment, user, digitalKey, sessionId));
                aj.setBatchId(batchId);
                futureMap.put(sessionId, aj);
                //  } else {
                //     LOGGER.log(Level.SEVERE, "Could not get an id from a newly persisted payment. Possible JPA caching issues. {0} ", new Object[]{cust.getUsername(),});
                //  }

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Add Single Payment failed due to exception:", e);
            }
        } else {
            LOGGER.log(Level.WARNING, "Logged in user is null. Add Single Payment aborted.");
        }
        return paymentId;
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
        LOGGER.log(Level.INFO, "Getting Customer (EziId) Details {0}", eziDebitId);

        EziResponseOfCustomerDetailsTHgMB7OL customerdetails = getWs().getCustomerDetails(digitalKey, eziDebitId, "");
        if (customerdetails.getError() == 0) {// any errors will be a non zero value
            LOGGER.log(Level.INFO, "Get Customer (EziId) Details Response: Name - {0}", customerdetails.getData().getValue().getCustomerName().getValue());

            cd = customerdetails.getData().getValue();

        } else {
            LOGGER.log(Level.WARNING, "Get Customer (EziId) Details Response: Error - {0}", customerdetails.getErrorMessage().getValue());
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

     LOGGER.log(Level.INFO, "Running async task - Getting Customer Account Details {0}", cust.getUsername());
        
     EziResponseOfCustomerAccountDetails customerdetails = getWs().getCustomerAccountDetails(digitalKey, "", cust.getId().toString());
     if (customerdetails.getError() == 0) {// any errors will be a non zero value
     LOGGER.log(Level.INFO, "Get Customer Account Details Response: Name - {0}", customerdetails.getData().getValue().getCustomerName().getValue());

     cd = customerdetails.getData().getValue();

     } else {
     LOGGER.log(Level.WARNING, "Get Customer Account Details Response: Error - {0}", customerdetails.getErrorMessage().getValue());

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
     LOGGER.log(Level.INFO, "Getting Customer (EziId) Account Details {0}", eziDebitId);
        
     EziResponseOfCustomerDetailsTHgMB7OL customerdetails = getWs().getCustomerAccountDetails(digitalKey, eziDebitId, "");
     if (customerdetails.getError() == 0) {// any errors will be a non zero value
     LOGGER.log(Level.INFO, "Get Customer (EziId) Account Details Response: Name - {0}", customerdetails.getData().getValue().getCustomerName().getValue());

     cd = customerdetails.getData().getValue();

     } else {
     LOGGER.log(Level.WARNING, "Get Customer (EziId) Account Details Response: Error - {0}", customerdetails.getErrorMessage().getValue());
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
        LOGGER.log(Level.INFO, "Editing Customer  {0}", cust.getUsername());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        PaymentParameters payParams = cust.getPaymentParametersId();
        String addresssLine2 = ""; // not used
        String humanFriendlyReference = cust.getId() + " " + cust.getLastname().toUpperCase() + " " + cust.getFirstname().toUpperCase(); // existing customers use this type of reference by default

        if (payParams == null) {
            LOGGER.log(Level.WARNING, "Payment gateway EZIDEBIT parameters not found");
            return new AsyncResult<>(false);
        }

// note - NB - for Australian Customers the
//mobile phone number must be 10
//digits long and begin with '04'. For
//New Zealand Customers the mobile
//phone number must be 10 digits
//long and begin with '02'
        EziResponseOfstring editCustomerDetail = getWs().editCustomerDetails(digitalKey, eziDebitRef, ourSystemRef, cust.getId().toString(), humanFriendlyReference, cust.getLastname(), cust.getFirstname(), cust.getStreetAddress(), addresssLine2, cust.getSuburb(), cust.getPostcode(), cust.getAddrState(), cust.getEmailAddress(), cust.getTelephone(), payParams.getSmsPaymentReminder(), payParams.getSmsFailedNotification(), payParams.getSmsExpiredCard(), payParams.getCustomers().getUsername());
        LOGGER.log(Level.INFO, "editCustomerDetail Response: Error - {0}, Data - {1}", new Object[]{editCustomerDetail.getErrorMessage().getValue(), editCustomerDetail.getData().getValue()});
        if (editCustomerDetail.getError() == 0) {// any errors will be a non zero value
            result = true;
            String auditDetails = "Edit the customers details for  :" + cust.getUsername() + " Details:  " + humanFriendlyReference + " " + cust.getLastname() + " " + cust.getFirstname() + " " + cust.getStreetAddress() + " " + addresssLine2 + " " + cust.getSuburb() + " " + cust.getPostcode() + " " + cust.getAddrState() + " " + cust.getEmailAddress() + " " + cust.getTelephone() + " " + payParams.getSmsPaymentReminder() + " " + payParams.getSmsFailedNotification() + " " + payParams.getSmsExpiredCard() + " " + payParams.getCustomers().getUsername();
            String changedFrom = "Existing Customer Record";
            String changedTo = "Edited customer record";

            if (loggedInUser == null) {
                loggedInUser = cust;
                LOGGER.log(Level.WARNING, "Payment Bean, editCustomerDetail: The logged in user is NULL");
            }
            auditLogFacade.audit(loggedInUser, cust, "editCustomerDetails", auditDetails, changedFrom, changedTo);

        } else {
            LOGGER.log(Level.WARNING, "editCustomerDetail Response: Error - {0}, Data - {1}", new Object[]{editCustomerDetail.getErrorMessage().getValue(), editCustomerDetail.getData().getValue()});

        }
        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<PaymentGatewayResponse> clearSchedule(Customers cust, boolean keepManualPayments, String loggedInUser, String digitalKey, String sessionId) {
        // This method will remove payments that exist in the payment schedule for the given
        // customer. You can control whether all payments are deleted, or if you wish to preserve
        // any manually added payments, and delete an ongoing cyclic schedule.
        PaymentGatewayResponse pgr = new PaymentGatewayResponse(false, null, "The Customers Payment Schedule was not cleared in the payment gateway due to an error.", "Null", "Null");

        String eziDebitCustomerId = ""; // use our reference instead. THis must be an empty string.

        String ourSystemCustomerReference = cust.getId().toString();

        String keepManualPaymentsString = "NO";// update all specified payments for customer 
        if (keepManualPayments) {
            keepManualPaymentsString = "YES";// maintain any one off or ad-hoc payment amounts
        }
        if (loggedInUser.length() > 50) {
            loggedInUser = loggedInUser.substring(0, 50);
            LOGGER.log(Level.WARNING, "clearSchedule loggedInUser is greater than the allowed 50 characters. Truncating! to 50 chars");
        }
        EziResponseOfstring eziResponse = getWs().clearSchedule(digitalKey, eziDebitCustomerId, ourSystemCustomerReference, keepManualPaymentsString, loggedInUser);
        LOGGER.log(Level.INFO, "clearSchedule Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        boolean failed = true;
        if (eziResponse.getError() == 0) {// any errors will be a non zero value

            if (eziResponse.getData().getValue() != null) {
                if (eziResponse.getData().getValue().compareTo("S") == 0) {
                    String auditDetails = "Cleared scheduled for  :" + cust.getUsername() + ".  Keep Manual Payments: " + keepManualPayments;
                    String changedFrom = "From Date:" + cust.getPaymentParametersId().getPaymentPeriod();
                    String changedTo = "Cleared Schedule";
                    auditLogFacade.audit(customersFacade.findCustomerByUsername(loggedInUser), cust, "clearSchedule", auditDetails, changedFrom, changedTo);

                    pgr = new PaymentGatewayResponse(true, cust, "The Customers Payment Schedule was cleared successfully in the payment gateway.", "0", "");
                    futureMap.processClearSchedule(sessionId, pgr);
                    return new AsyncResult<>(pgr);
                }
            }

        }
        if (failed) {
            pgr = new PaymentGatewayResponse(false, null, "The Customers Payment Schedule was not cleared in the payment gateway due to an error.", eziResponse.getError().toString(), eziResponse.getErrorMessage().getValue());
            LOGGER.log(Level.WARNING, "The Customers Payment Schedule was not cleared in the payment gateway due to an error. : Error - {0}, Message - {1}", new Object[]{eziResponse.getError().toString(), eziResponse.getErrorMessage().getValue()});

        }

        return new AsyncResult<>(pgr);
    }

    @Asynchronous
    // @TransactionAttribute(REQUIRES_NEW) //see this for an explanation. we want a new transaction http://docs.oracle.com/javaee/6/tutorial/doc/bncij.html
    public Future<PaymentGatewayResponse> deletePayment(Customers cust, Date debitDate, Long paymentAmountInCents, Payments payment, String loggedInUser, String digitalKey, String sessionId) {
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
        String paymentReference = "";

        if (paymentAmountInCents == null) {
            paymentAmountInCents = (long) -1;
        }
        if (payment != null) {
            paymentReference = payment.getId().toString();
        }

        PaymentGatewayResponse pgr = new PaymentGatewayResponse(false, payment, paymentReference, "-1", "An unhandled error occurred!");

        if (paymentReference.isEmpty() && (debitDate == null || cust == null || paymentAmountInCents < 0)) {
            String eMessage = "deletePayment NULL parameter of Amount < 0. cust {0}, date {1}, Amount {2}";
            LOGGER.log(Level.WARNING, eMessage, new Object[]{cust, debitDate, paymentAmountInCents});
            pgr = new PaymentGatewayResponse(false, payment, paymentReference, "-1", eMessage);
            futureMap.processDeletePaymentAsync(sessionId, pgr);
            return new AsyncResult<>(pgr);
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
            LOGGER.log(Level.WARNING, "deletePayment paymentReference is greater than the allowed 50 characters. Truncating! to 50 chars");
        }
        if (loggedInUser.length() > 50) {
            loggedInUser = loggedInUser.substring(0, 50);
            LOGGER.log(Level.WARNING, "deletePayment loggedInUser is greater than the allowed 50 characters. Truncating! to 50 chars");
        }
        EziResponseOfstring eziResponse;
        try {
            if (paymentReference.isEmpty()) {
                eziResponse = getWs().deletePayment(digitalKey, eziDebitCustomerId, ourSystemCustomerReference, "", debitDateString, paymentAmountInCents, loggedInUser);
                LOGGER.log(Level.INFO, "deletePayment - the paymentReference is  NULL so date and amount will be used to identify the payment.If there are two then only one will be deleted.");
            } else {
                eziResponse = getWs().deletePayment(digitalKey, eziDebitCustomerId, ourSystemCustomerReference, paymentReference, debitDateString, new Long(0), loggedInUser);
                LOGGER.log(Level.INFO, "deletePayment - the paymentReference is being used to identify the payment");
            }
        } catch (Exception e) {
            if (e.getMessage().contains("Your update could not be processed at this time")) {
                LOGGER.log(Level.WARNING, "deletePayment Method Response: Gateway is busy and cannot process at this time. Will retry up tpo 10 times ");
            } else if (e.getMessage().contains("Payment selected for deletion could not be found")) {
                LOGGER.log(Level.WARNING, "The payment wasn't found in the payment gateway!");
            } else {
                LOGGER.log(Level.WARNING, "deletePayment Method Response: ", e);
            }

            String errorMessage = "ERROR:" + paymentReference + ":";
            if (e.getMessage() != null) {
                errorMessage += e.getMessage();
            }
            String eMessage = "changeCustomerStatus Response: Error - " + e.getMessage();
            pgr = new PaymentGatewayResponse(false, payment, paymentReference, "-1", eMessage);
            futureMap.processDeletePaymentAsync(sessionId, pgr);
            return new AsyncResult<>(pgr);

        }
        if (eziResponse != null) {
            if (eziResponse.getError() == 0) {// any errors will be a non zero value
                LOGGER.log(Level.INFO, "deletePayment Response: OK Data - {0}", new Object[]{eziResponse.getData().getValue()});
                if (eziResponse.getData().getValue().compareTo("S") == 0) {

                    String auditDetails = "Debit Date:" + debitDateString + ", Amount (cents): " + paymentAmountInCents.toString() + ", Payment Ref:" + paymentReference;
                    String changedFrom = "Ref:" + paymentReference;
                    String changedTo = "Deleted";
                    pgr = new PaymentGatewayResponse(true, payment, paymentReference, "-1", "");
                    try {
                        auditLogFacade.audit(customersFacade.findCustomerByUsername(loggedInUser), cust, "deletePayment", auditDetails, changedFrom, changedTo);

                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "deletePayment Audit logging failed : Customer - {0}, logged In User - {1}", new Object[]{cust.getUsername(), loggedInUser});
                    }
                    futureMap.processDeletePaymentAsync(sessionId, pgr);
                    return new AsyncResult<>(pgr);
                } else {
                    LOGGER.log(Level.WARNING, "deletePayment Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
                }
            } else {
                LOGGER.log(Level.WARNING, "deletePayment Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
                pgr = new PaymentGatewayResponse(false, payment, paymentReference, eziResponse.getError().toString(), eziResponse.getErrorMessage().getValue());
                futureMap.processDeletePaymentAsync(sessionId, pgr);
                return new AsyncResult<>(pgr);
            }
        } else {
            LOGGER.log(Level.WARNING, "deletePayment - the EziResponseOfstring is  NULL ");
        }
        futureMap.processDeletePaymentAsync(sessionId, pgr);
        return new AsyncResult<>(pgr);
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
     LOGGER.log(Level.WARNING, "deletePayment Customer NULL ");
     return new AsyncResult<>(result);
     }
     if (paymentReference == null) {
     LOGGER.log(Level.WARNING, "deletePayment paymentReference NULL ");
     return new AsyncResult<>(result);
     }
     if (paymentReference.trim().isEmpty()) {
     LOGGER.log(Level.WARNING, "deletePayment paymentReference is empty. ");
     return new AsyncResult<>(result);
     }
     if (loggedInUser == null) {
     LOGGER.log(Level.WARNING, "deletePayment loggedInUser NULL ");
     return new AsyncResult<>(result);
     }
     if (digitalKey == null) {
     LOGGER.log(Level.WARNING, "deletePayment digitalKey NULL ");
     return new AsyncResult<>(result);
     }

     String eziDebitCustomerId = ""; // use our reference instead. THis must be an empty string.

     String ourSystemCustomerReference = cust.getId().toString();

     paymentReference = paymentReference.trim();
     if (paymentReference.length() > 50) {
     paymentReference = paymentReference.substring(0, 50);
     LOGGER.log(Level.WARNING, "deletePayment paymentReference is greater than the allowed 50 characters. Truncating! to 50 chars");
     }
     if (loggedInUser.length() > 50) {
     loggedInUser = loggedInUser.substring(0, 50);
     LOGGER.log(Level.WARNING, "deletePayment loggedInUser is greater than the allowed 50 characters. Truncating! to 50 chars");
     }
     EziResponseOfstring eziResponse = null;

     try {
     eziResponse = getWs().deletePayment(digitalKey, eziDebitCustomerId, ourSystemCustomerReference, paymentReference, "", new Long(0), loggedInUser);
     } catch (Exception e) {
     if (e.getMessage().contains("Payment selected for deletion could not be found")) {
     LOGGER.log(Level.WARNING, "deletePayment:The Payment selected for deletion could not be found.Payment Ref: {2}, Customer {0}, logged In User - {1}", new Object[]{cust.getUsername(), loggedInUser, paymentReference});
     } else {
     LOGGER.log(Level.INFO, "deletePayment - FAILED", e);
     }
     return new AsyncResult<>(result);
     }
     LOGGER.log(Level.INFO, "deletePayment - the paymentReference is not NULL and is overriding date and amount to identify the payment");
     if (eziResponse != null) {
     // LOGGER.log(Level.INFO, "deletePayment Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
     if (eziResponse.getError() == 0) {// any errors will be a non zero value

     if (eziResponse.getData().getValue().compareTo("S") == 0) {
     result = paymentReference + ",OK";
     String auditDetails = "Payment Ref:" + paymentReference;
     String changedFrom = "Ref:" + paymentReference;
     String changedTo = "Deleted";
     try {
     auditLogFacade.audit(customersFacade.findCustomerByUsername(loggedInUser), cust, "deletePayment", auditDetails, changedFrom, changedTo);
     } catch (Exception e) {
     LOGGER.log(Level.WARNING, "deletePayment Auidt logging failed : Customer - {0}, logged In User - {1}", new Object[]{cust.getUsername(), loggedInUser});
     }
     } else {
     LOGGER.log(Level.WARNING, "deletePayment Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
     }
     } else {
     LOGGER.log(Level.WARNING, "deletePayment Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

     }
     } else {
     LOGGER.log(Level.WARNING, "deletePayment - the EziResponseOfstring is  NULL ");
     }

     return new AsyncResult<>(result);
     }*/
    @Asynchronous
    public Future<PaymentGatewayResponse> changeCustomerStatus(Customers cust, String newStatus, String loggedInUser, String digitalKey, String sessionId) {
        PaymentGatewayResponse pgr = new PaymentGatewayResponse(false, null, "", "-1", "An unhandled error occurred!");
        if (cust == null || newStatus == null || loggedInUser == null) {
            LOGGER.log(Level.WARNING, "changeCustomerStatus ABORTED because cust ==null || newStatus == null || loggedInUser == null");
            pgr = new PaymentGatewayResponse(false, null, "", "-1", "changeCustomerStatus ABORTED because cust ==null || newStatus == null || loggedInUser == null");
            futureMap.processChangeCustomerStatus(sessionId, pgr);
            return new AsyncResult<>(pgr);
        }
        try {
            // note: cancelled status cannot be changed with this method. i.e. cancelled is final like deleted.
            LOGGER.log(Level.INFO, "{2} changed customer ({0}) status to {1}", new Object[]{cust.getUsername(), newStatus, loggedInUser});
            String eziDebitCustomerId = ""; // use our reference instead. THis must be an empty string.
            String ourSystemCustomerReference = cust.getId().toString();
            String oldStatus = "Does not exist in payment gateway.";
            if (cust.getPaymentParametersId() != null) {
                oldStatus = cust.getPaymentParametersId().getStatusDescription();
            }
            if (newStatus.compareTo("A") == 0 || newStatus.compareTo("H") == 0 || newStatus.compareTo("C") == 0) {
                if (loggedInUser.length() > 50) {
                    loggedInUser = loggedInUser.substring(0, 50);
                    LOGGER.log(Level.WARNING, "changeCustomerStatus loggedInUser is greater than the allowed 50 characters. Truncating! to 50 chars");
                }
                EziResponseOfstring eziResponse;
                try {
                    eziResponse = getWs().changeCustomerStatus(digitalKey, eziDebitCustomerId, ourSystemCustomerReference, newStatus, loggedInUser);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "changeCustomerStatus Response: Error - {0}", e);
                    String eMessage = "changeCustomerStatus Response: Error - " + e.getMessage();
                    pgr = new PaymentGatewayResponse(false, null, "", "-1", eMessage);
                    futureMap.processChangeCustomerStatus(sessionId, pgr);
                    return new AsyncResult<>(pgr);
                }
                LOGGER.log(Level.INFO, "changeCustomerStatus Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
                if (eziResponse.getError() == 0) {// any errors will be a non zero value
                    if (eziResponse.getData().getValue().compareTo("S") == 0) {

                        String auditDetails = "Changed the status for :" + cust.getUsername() + " to  " + newStatus + " from " + oldStatus;
                        String changedFrom = "Old Status:" + oldStatus;
                        String changedTo = "New Status:" + newStatus;
                        auditLogFacade.audit(customersFacade.findCustomerByUsername(loggedInUser), cust, "changeCustomerStatus", auditDetails, changedFrom, changedTo);
                        pgr = new PaymentGatewayResponse(true, cust, auditDetails, "-1", "");
                        LOGGER.log(Level.INFO, "changeCustomerStatus  Successful  : ErrorCode - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

                    } else {
                        LOGGER.log(Level.WARNING, "changeCustomerStatus Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
                        pgr = new PaymentGatewayResponse(false, null, "Could not change the customer status in the payment gateway due to an error.", eziResponse.getError().toString(), eziResponse.getErrorMessage().getValue());

                    }

                } else {
                    LOGGER.log(Level.WARNING, "changeCustomerStatus Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
                    pgr = new PaymentGatewayResponse(false, null, "Could not change the customer status in the payment gateway due to an error.", eziResponse.getError().toString(), eziResponse.getErrorMessage().getValue());

                }
            } else {
                String eMessage = "changeCustomerStatus: newStatus should be A ( Active ), H ( Hold ) or C (Cancelled)  : Error - " + newStatus;
                LOGGER.log(Level.WARNING, eMessage);
                pgr = new PaymentGatewayResponse(false, null, "", "-1", eMessage);
            }
        } catch (Exception e) {
            String eMessage = "changeCustomerStatus Caught Unhandled Error - " + e.getMessage();
            pgr = new PaymentGatewayResponse(false, null, "", "-1", eMessage);
        }
        futureMap.processChangeCustomerStatus(sessionId, pgr);
        return new AsyncResult<>(pgr);
    }

    @Asynchronous
    @TransactionAttribute(REQUIRES_NEW)
    public Future<PaymentGatewayResponse> getScheduledPayments(Customers cust, Date fromDate, Date toDate, String digitalKey, String sessionId, boolean ignoreDatesAndGetAll) {

        ArrayOfScheduledPayment resultArrayOfScheduledPayments = new ArrayOfScheduledPayment();
        PaymentGatewayResponse pgr = new PaymentGatewayResponse(false, resultArrayOfScheduledPayments, "", "-1", "An unhandled error occurred!");

        boolean abort = false;
        boolean sendAlertEmail = false;
        String message = "<h3>The below payments exists in our database but not in the payment gateway so it won't be processed.This could be due to a fatal error with the customers credit card such as it being lost or stolen, a communication error when the payment was scheduled or other reason.Resubmit the payment and if it fails again check the customer credit card or bank account details.</h3><h1> </h1>";
        if (cust == null) {
            LOGGER.log(Level.WARNING, "getScheduledPayments: The customer object passed to this method is NULL.This parameter is required.Returning empty array!!");
            pgr = new PaymentGatewayResponse(false, resultArrayOfScheduledPayments, "", "-1", "The customer object passed to this method is NULL.This parameter is required.Returning empty array!!");
            futureMap.processGetScheduledPayments(sessionId, pgr);
            return new AsyncResult<>(pgr);
        }
        try {
            LOGGER.log(Level.INFO, "Running asychronous task getScheduledPayments Customer {0}, From Date {1}, to Date {2}", new Object[]{cust, fromDate, toDate});
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
            LOGGER.log(Level.INFO, "Payment Bean - Running async task - Getting Scheduled Payments for Customer {0}", cust.getUsername());
            EziResponseOfArrayOfScheduledPaymentTHgMB7OL eziResponse = null;
            if (ignoreDatesAndGetAll) {
                eziResponse = getWs().getScheduledPayments(digitalKey, "", "", eziDebitCustomerId, ourSystemCustomerReference);
            } else {
                eziResponse = getWs().getScheduledPayments(digitalKey, fromDateString, toDateString, eziDebitCustomerId, ourSystemCustomerReference);
            }
            if (eziResponse.getError() == 0) {// any errors will be a non zero value
                resultArrayOfScheduledPayments = eziResponse.getData().getValue();
                if (resultArrayOfScheduledPayments != null) {
                    LOGGER.log(Level.INFO, "Payment Bean - Get Customer Scheduled Payments Response Recieved from ezidebit for Customer  - {0}, Number of Payments : {1} ", new Object[]{cust.getUsername(), resultArrayOfScheduledPayments.getScheduledPayment().size()});
                    pgr = new PaymentGatewayResponse(true, resultArrayOfScheduledPayments, "Updated Scheduled Payment Information was recieved from the payment gateway.", "-1", "");

                    //process the payments
                    if (resultArrayOfScheduledPayments.getScheduledPayment() != null) {

                        List<ScheduledPayment> payList = resultArrayOfScheduledPayments.getScheduledPayment();
                        if (payList.isEmpty() == false) {
                            LOGGER.log(Level.INFO, "PaymentBean processGetScheduledPayments. Processing {0} payments for customer {1}.", new Object[]{payList.size(), cust.getUsername()});
                            if (abort == false) {
                                GregorianCalendar testCal = new GregorianCalendar();
                                testCal.add(Calendar.MINUTE, -5);
                                Date testDate = testCal.getTime();

                                //Collection<Payments> crmPayList = cust.getPaymentsCollection();
                                Collection<Payments> crmPayList = paymentsFacade.findPaymentsByCustomer(cust,true);
                                for (Payments crmPay : crmPayList) {
                                    boolean found = false;
                                    for (ScheduledPayment pay : payList) {

                                        if (pay.getPaymentReference().isNil() == false) {
                                            String ref = pay.getPaymentReference().getValue().trim();
                                            String ids = crmPay.getId().toString().trim();
                                            if (ids.equalsIgnoreCase(ref)) {
                                                found = true;
                                                int id = -1;
                                                try {
                                                    id = crmPay.getId();
                                                } catch (Exception e) {
                                                    LOGGER.log(Level.INFO, "PaymentBean processGetScheduledPayments  - found a payment without a valid reference", e);
                                                }
                                                LOGGER.log(Level.INFO, "PaymentBean processGetScheduledPayments  - Processing Payment CRM database id:{0}", id);
                                                if (compareScheduledPaymentXMLToEntity(crmPay, pay)) {
                                                    // they are the same - check status and update if necessary
                                                    LOGGER.log(Level.INFO, "PaymentBean processGetScheduledPayments  -- sync OK -- payment id:{0}", id);
                                                } else {
                                                    compareScheduledPaymentXMLToEntityAndUpdateAmountAndStatus(crmPay, pay);
                                                    LOGGER.log(Level.WARNING, "PaymentBean processGetScheduledPayments  - updating scheduled payment compareScheduledPaymentXMLToEntity FAILED id: {0}", id);
                                                }
                                            }
                                        }
                                    }
                                    if (found == false) {
                                        //String ref = p.getId().toString();
                                        if (crmPay.getCreateDatetime().before(testDate)) {// make sure we don't delate payments that have just been added and may still be being processed by the gateway. i.e they've been put into our DB but havn't been put into the payment gateway schedule yet
                                            crmPay.setPaymentStatus(PaymentStatus.MISSING_IN_PGW.value());
                                            if (cust.getPaymentParametersId().getStatusDescription().toUpperCase().contains("HOLD")) {
                                                crmPay.setPaymentStatus(PaymentStatus.REJECTED_CUST_ON_HOLD.value());
                                            } else {
                                                message += "<p>Customer " + cust.getUsername() + ", Payment ID:" + crmPay.getId().toString() + " for Amount:$" + crmPay.getPaymentAmount().toPlainString() + " on Date:" + crmPay.getDebitDate().toString() + " was rejected by the payment gateway and requires your action or revenue loss may occur!!.\r\n</p></BR>";

                                                sendAlertEmail = true;
                                            }
                                            paymentsFacade.edit(crmPay);
                                        }
                                    }
                                }
                                // remove any that 

                                // List<Payments> crmPaymentList = paymentsFacade.findScheduledPaymentsByCustomer(cust, false);
                                for (ScheduledPayment pay : payList) {

                                    boolean found = false;
                                    for (Payments p : crmPayList) {
                                        if (pay.getPaymentReference().isNil() == false) {
                                            String ref = pay.getPaymentReference().getValue().trim();
                                            String id = p.getId().toString().trim();
                                            if (id.equalsIgnoreCase(ref)) {
                                                found = true;
                                            }
                                        }
                                    }
                                    if (found == false) {

                                        int payGatewayReference = -1;
                                        try {
                                            payGatewayReference = Integer.parseInt(pay.getPaymentReference().getValue());
                                        } catch (NumberFormatException numberFormatException) {
                                            LOGGER.log(Level.SEVERE, "PaymentBean compareScheduledPaymentXMLToEntity method - The paymentGateway Reference is invalid -- no way to match this to an existing payment:payment gateway Ref={0}", new Object[]{pay.getPaymentReference().getValue()});
                                            found = true;
                                        }
                                        if (payGatewayReference <= 0) {
                                            LOGGER.log(Level.SEVERE, "PaymentBean compareScheduledPaymentXMLToEntity method - The paymentGateway Reference is invalid -- no way to match this to an existing payment:payment gateway Ref={0}", new Object[]{pay.getPaymentReference().getValue()});
                                            found = true;
                                        }
                                    }

                                    if (found == false) {

                                        Payments crmPay;
                                        //payment doesn't exist in crm so add it
                                        LOGGER.log(Level.WARNING, "PaymentBean processGetScheduledPayments - A payment exists in the PGW but not in CRM.(This can be ignored if a customer is onboarded with the online eddr form) EzidebitID={0}, CRM Ref:{1}, Amount={2}, Date={3}, Ref={4}", new Object[]{pay.getEzidebitCustomerID().getValue(), pay.getYourSystemReference().getValue(), pay.getPaymentAmount().floatValue(), pay.getPaymentDate().toGregorianCalendar().getTime(), pay.getPaymentReference().getValue()});
                                        crmPay = convertScheduledPaymentXMLToEntity(null, pay, cust);
                                        if (crmPay.getPaymentReference() != null) {
                                            if (Integer.parseInt(crmPay.getPaymentReference()) < 1) {
                                                crmPay.setPaymentReference(null);// this is a unique field and should be equal to the id field i.e the Payment gateway reference should match 
                                            }
                                        }
                                        paymentsFacade.createAndFlushForGeneratedIdEntities(crmPay);
                                        crmPay.setPaymentReference(crmPay.getId().toString());
                                        crmPay.setManuallyAddedPayment(false);
                                        paymentsFacade.edit(crmPay);
                                        Long amountLong = null;
                                        try {
                                            amountLong = crmPay.getPaymentAmount().movePointRight(2).longValue();
                                        } catch (Exception e) {
                                            LOGGER.log(Level.WARNING, "Arithemtic error.");
                                        }
                                        Future<PaymentGatewayResponse> deletePaymentResponseFutResp = deletePayment(cust, crmPay.getDebitDate(), amountLong, null, cust.getUsername(), digitalKey, sessionId);
                                        PaymentGatewayResponse deletePaymentResponse = deletePaymentResponseFutResp.get();
                                        if (deletePaymentResponse.isOperationSuccessful()) {
                                            Logger.getLogger(PaymentBean.class.getName()).log(Level.INFO, "Payment Deleted Successfully - ID= {0}", new Object[]{crmPay.getId()});
                                        } else {
                                            Logger.getLogger(PaymentBean.class.getName()).log(Level.SEVERE, "Payment Deletetion FAILED - ID= {0}", new Object[]{crmPay.getId()});
                                        }
                                        try {
                                            TimeUnit.MILLISECONDS.sleep(250);

                                        } catch (InterruptedException ex) {
                                            Logger.getLogger(FutureMapEJB.class
                                                    .getName()).log(Level.SEVERE, "Thread Sleep interrupted", ex);
                                        }
                                        Future<PaymentGatewayResponse> addPaymentResponseFutResp = addPayment(cust, crmPay.getDebitDate(), amountLong, crmPay, cust.getUsername(), digitalKey, sessionId);
                                        PaymentGatewayResponse addPaymentResponse = addPaymentResponseFutResp.get();
                                        if (addPaymentResponse.isOperationSuccessful()) {
                                            Logger.getLogger(PaymentBean.class.getName()).log(Level.INFO, "Payment Added Successfully - ID= {0}", new Object[]{crmPay.getId()});
                                        } else {
                                            Logger.getLogger(PaymentBean.class.getName()).log(Level.SEVERE, "Payment Addition FAILED - ID= {0}", new Object[]{crmPay.getId()});
                                        }

                                    }

                                }
                            }

                            LOGGER.log(Level.INFO, "updateNextScheduledPayment. Processing details for customer {0}.", new Object[]{cust.getUsername()});
                            PaymentParameters pp = cust.getPaymentParametersId();
                            if (pp != null) {
                                Payments p1 = null;
                                try {
                                    p1 = paymentsFacade.findLastSuccessfulScheduledPayment(cust);
                                } catch (Exception e) {
                                    LOGGER.log(Level.WARNING, "PaymentBean processGetCustomerDetails. findLastSuccessfulScheduledPayment for customer {0}. {1}", new Object[]{cust.getUsername(), e});
                                }
                                Payments p2 = null;
                                try {
                                    p2 = paymentsFacade.findNextScheduledPayment(cust);
                                } catch (Exception e) {
                                    LOGGER.log(Level.WARNING, "PaymentBean processGetCustomerDetails. findNextScheduledPayment for customer {0}. {1}", new Object[]{cust.getUsername(), e});
                                }
                                pp.setLastSuccessfulScheduledPayment(p1);
                                pp.setNextScheduledPayment(p2);
                                //paymentParametersFacade.edit(pp);
                                cust.setPaymentParametersId(pp);
                                customersFacade.editAndFlush(cust);
                            }

                        }

                    }
                    if (sendAlertEmail) {
                        sendAlertEmailToAdmin(message, sessionId);
                        LOGGER.log(Level.WARNING, "PaymentBean processGetScheduledPayments ALERT EMAIL SENT {0}", new Object[]{    message});
                    }
                    LOGGER.log(Level.INFO, "PaymentBean processGetScheduledPayments completed");

                } else {
                    LOGGER.log(Level.WARNING, "getScheduledPayments Response: Error - NULL Result ");
                    pgr = new PaymentGatewayResponse(false, null, "", "-1", "Scheduled Payment information was not recieved from the payment gateway due to an error.");
                }

            } else {
                if (eziResponse.getError() == 921) {// no scheduled payments found
                    // mark any found in our database for this customer as deleted in the payment gateway
                    // this really shouldnt happen
                    GregorianCalendar testCal = new GregorianCalendar();
                    testCal.add(Calendar.MINUTE, -5);
                    Date testDate = testCal.getTime();
                    List<Payments> crmPaymentList = paymentsFacade.findScheduledPaymentsByCustomer(cust, true);
                    for (Payments p : crmPaymentList) {
                        if (p.getCreateDatetime().before(testDate)) {// make sure we don't delate payments that have just been added and may still be being processed by the gateway. i.e they've been put into our DB but havn't been put into the payment gateway schedule yet
                            p.setPaymentStatus(PaymentStatus.MISSING_IN_PGW.value());
                            if (cust.getPaymentParametersId().getStatusDescription().toUpperCase().contains("HOLD")) {
                                p.setPaymentStatus(PaymentStatus.REJECTED_CUST_ON_HOLD.value());
                            } else {
                                message = "This payment exists in our database but not in the payment gateway so it won't be processed.Customer " + cust.getUsername() + ", Payment ID:" + p.getId().toString() + " for Amount:$" + p.getPaymentAmount().toPlainString() + " on Date:" + p.getDebitDate().toString() + " was rejected by the payment gateway and requires your action or revenue loss may occur!!.";

                                sendAlertEmailToAdmin(message, sessionId);
                            }
                            paymentsFacade.edit(p);
                        }

                    }
                    LOGGER.log(Level.INFO, "getScheduledPayments Response: None Found - {0}, ", eziResponse.getErrorMessage().getValue());
                } else {
                    LOGGER.log(Level.WARNING, "getScheduledPayments Response: Error - {0}, ", eziResponse.getErrorMessage().getValue());
                    pgr = new PaymentGatewayResponse(false, null, "Scheduled Payment information was not recieved from the payment gateway due to an error.", eziResponse.getError().toString(), eziResponse.getErrorMessage().getValue());

                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "getScheduledPayments Response: Error : ", e);
        }
        futureMap.processGetScheduledPayments(sessionId, pgr);
        return new AsyncResult<>(pgr);
    }

    @Asynchronous
    public Future<PaymentGatewayResponse> convertEzidebitScheduleToCrmSchedule(Customers cust, Date fromDate, Date toDate, String digitalKey, String sessionId) {

        ArrayOfScheduledPayment result = new ArrayOfScheduledPayment();
        PaymentGatewayResponse pgr = new PaymentGatewayResponse(false, result, "", "-1", "An unhandled error occurred!");
        ArrayOfScheduledPayment resultArrayOfScheduledPayments = null;
        boolean abort = false;

        if (cust == null) {
            LOGGER.log(Level.WARNING, "convertEzidebitScheduleToCrmSchedule: The customer object passed to this method is NULL.This parameter is required.Returning empty array!!");
            pgr = new PaymentGatewayResponse(false, result, "", "-1", "The customer object passed to this method is NULL.This parameter is required.Returning empty array!!");
            futureMap.processConvertSchedule(sessionId, pgr);
            return new AsyncResult<>(pgr);
        }
        try {
            LOGGER.log(Level.INFO, "Running asychronous task convertEzidebitScheduleToCrmSchedule Customer {0}, From Date {1}, to Date {2}", new Object[]{cust, fromDate, toDate});
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
            LOGGER.log(Level.INFO, "Payment Bean - Running async task - convertEzidebitScheduleToCrmSchedule for Customer {0}", cust.getUsername());
            EziResponseOfArrayOfScheduledPaymentTHgMB7OL eziResponse = getWs().getScheduledPayments(digitalKey, fromDateString, toDateString, eziDebitCustomerId, ourSystemCustomerReference);
            if (eziResponse.getError() == 0) {// any errors will be a non zero value
                result = eziResponse.getData().getValue();
                if (result != null) {
                    LOGGER.log(Level.INFO, "Payment Bean - Get convertEzidebitScheduleToCrmSchedule fo Convert Schedule Response Recieved from ezidebit for Customer  - {0}, Number of Payments : {1} ", new Object[]{cust.getUsername(), result.getScheduledPayment().size()});
                    pgr = new PaymentGatewayResponse(true, result, "Updated Scheduled Payment Information was recieved from the payment gateway.", "-1", "");

                    LOGGER.log(Level.INFO, "Payment Bean convertEzidebitScheduleToCrmSchedule completed");

                } else {
                    LOGGER.log(Level.WARNING, "getScheduledPayments Response: Error - NULL Result ");
                    pgr = new PaymentGatewayResponse(false, null, "", "-1", "Scheduled Payment information was not recieved from the payment gateway due to an error.");
                }

            } else {
                LOGGER.log(Level.WARNING, "getScheduledPayments Response: Error - {0}, ", eziResponse.getErrorMessage().getValue());
                pgr = new PaymentGatewayResponse(false, null, "Scheduled Payment information was not recieved from the payment gateway due to an error.", eziResponse.getError().toString(), eziResponse.getErrorMessage().getValue());

            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "getScheduledPayments Response: Error : ", e);
            pgr = new PaymentGatewayResponse(false, null, "Scheduled Payment information was not recieved from the payment gateway due to an error.", "-1", e.getMessage());
        }
        futureMap.processConvertSchedule(sessionId, pgr);
        return new AsyncResult<>(pgr);
    }

    private void sendAlertEmailToAdmin(String message, String sessionId) {

        try {
            LOGGER.log(Level.INFO, "Payment Bean sendAlertEmailToAdmin . Sending Administrator Alert Email");
            if (message == null) {
                LOGGER.log(Level.WARNING, "Payment Bean sendAlertEmailToAdmin . Message is NULL.Alert Email not sent!");
                return;
            }
            futureMap.sendAlertEmailToAdmin(message, sessionId);
        } catch (Exception ex) {
            Logger.getLogger(PaymentBean.class.getName()).log(Level.SEVERE, "sendAlertEmailToAdmin Failed", ex);
        }
        LOGGER.log(Level.INFO, "Payment Bean sendAlertEmailToAdmin . Completed sending Administrator Alert Email");
    }

    private Payments convertScheduledPaymentXMLToEntity(Payments payment, ScheduledPayment pay, Customers cust) {

        if (pay == null || cust == null) {
            String p1 = "NULL";
            String p2 = "NULL";
            String p3 = "NULL";
            if (payment != null) {
                p1 = payment.getYourSystemReference();
            }
            if (pay != null) {
                p2 = pay.getYourSystemReference().getValue();
            }
            if (cust != null) {
                p3 = cust.getUsername();
            }
            LOGGER.log(Level.WARNING, "Payment Bean convertScheduledPaymentXMLToEntity method failed due to a NULL value. Customer {2},payment pojo {0},payment XML {1}:", new Object[]{p1, p2, p3});
            return null;
        }
        if (payment == null) {
            payment = new Payments();
            payment.setCreateDatetime(new Date());
            payment.setPaymentSource(PaymentSource.DIRECT_DEBIT.value());
            payment.setId(-1);
            payment.setCustomerName(cust);
        }
        try {

            payment.setLastUpdatedDatetime(new Date());
            payment.setBankFailedReason("");
            payment.setBankReceiptID("");
            payment.setBankReturnCode("");
            payment.setDebitDate(pay.getPaymentDate().toGregorianCalendar().getTime());
            payment.setEzidebitCustomerID(pay.getEzidebitCustomerID().getValue());
            payment.setInvoiceID("");
            payment.setPaymentAmount(new BigDecimal(pay.getPaymentAmount().toString()));
            payment.setScheduledAmount(new BigDecimal(pay.getPaymentAmount().toString()));
            payment.setPaymentID(null);
            payment.setPaymentMethod("DR");
            if (pay.getPaymentReference().isNil() == false) {
                if (pay.getPaymentReference().getValue().trim().isEmpty()) {
                    payment.setPaymentReference(null);
                } else {
                    payment.setPaymentReference(pay.getPaymentReference().getValue());
                }
            } else {
                payment.setPaymentReference(null);
            }

            /* if (pay.isManuallyAddedPayment() != null) {

                 payment.setManuallyAddedPayment(pay.isManuallyAddedPayment());

                 }*/
            payment.setPaymentSource("SCHEDULED");
            payment.setScheduledAmount(new BigDecimal(pay.getPaymentAmount()));

            payment.setSettlementDate(null);
            payment.setPaymentStatus(PaymentStatus.SCHEDULED.value());
            payment.setTransactionFeeClient(new BigDecimal(0));
            payment.setTransactionFeeCustomer(new BigDecimal(0));

            payment.setTransactionTime(null); // only valid for real time and credit card payments

            payment.setYourGeneralReference(pay.getYourGeneralReference().getValue());
            payment.setYourSystemReference(pay.getYourSystemReference().getValue());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Payment Bean convertScheduledPaymentXMLToEntity method failed.:", e);
        }

        return payment;

    }

    private boolean compareScheduledPaymentXMLToEntity(Payments payment, ScheduledPayment pay) {

        if (payment == null || pay == null) {
            return payment == null && pay == null;
        }
        try {

            if (payment.getCustomerName() == null) {
                LOGGER.log(Level.INFO, "PaymentBean compareScheduledPaymentXMLToEntity failed  - payment.getCustomerName() == null Payment CRM database id:{0}", payment.getId().toString());
                return false;
            } else if (compareStringToXMLString(payment.getCustomerName().getId().toString(), (pay.getYourSystemReference())) == false) {
                LOGGER.log(Level.WARNING, "PaymentBean compareScheduledPaymentXMLToEntity method - customer ID did not match:Payments={0},ScheduledPayment={1}", new Object[]{payment.getCustomerName().getId().toString(), pay.getYourSystemReference()});
                return false;
            }
            if (compareDateToXMLGregCalendar(payment.getDebitDate(), pay.getPaymentDate()) == false) {
                LOGGER.log(Level.INFO, "PaymentBean compareScheduledPaymentXMLToEntity failed  - compareDateToXMLGregCalendar(payment.getDebitDate(), pay.getPaymentDate() ,Payment CRM database id:{0}", payment.getId().toString());
                return false;
            }
            if (compareStringToXMLString(payment.getEzidebitCustomerID(), pay.getEzidebitCustomerID()) == false) {
                LOGGER.log(Level.INFO, "PaymentBean compareScheduledPaymentXMLToEntity failed  - compareStringToXMLString(payment.getEzidebitCustomerID(), pay.getEzidebitCustomerID() ,Payment CRM database id:{0}", payment.getId().toString());

                return false;
            }
            if (payment.getPaymentStatus().contains(PaymentStatus.SCHEDULED.value()) == false) {
                if (payment.getPaymentStatus().contains(PaymentStatus.WAITING.value()) == false) {
                    LOGGER.log(Level.INFO, "PaymentBean compareScheduledPaymentXMLToEntity failed  - not status S or W : failed status is : {1} ,Payment CRM database id:{0}", new Object[]{payment.getId().toString(), payment.getPaymentStatus()});

                    return false;
                }
            }
            BigDecimal crmDatabaseRecord = payment.getScheduledAmount();
            BigDecimal paymentGatewayRecord = new BigDecimal(pay.getPaymentAmount());
            if (crmDatabaseRecord.compareTo(paymentGatewayRecord) != 0) {
                // if (compareBigDecimalToDouble(payment.getPaymentAmount(), pay.getPaymentAmount()) == false) {
                LOGGER.log(Level.WARNING, "Payment Bean compareScheduledPaymentXMLToEntity method failed. Payment BigDec Amounts dont match CRM DB {0} , payment gateway {1}", new Object[]{crmDatabaseRecord, paymentGatewayRecord});
                return false;
            }
            /* if (!Objects.equals(payment.getManuallyAddedPayment(), pay.isManuallyAddedPayment())) {
                 return false;
                 }*/

            int crmPaymentReference = payment.getId();
            int payGatewayReference = -1;
            try {
                payGatewayReference = Integer.parseInt(pay.getPaymentReference().getValue());
            } catch (NumberFormatException numberFormatException) {
                LOGGER.log(Level.WARNING, "PaymentBean compareScheduledPaymentXMLToEntity method - payment ID did not match paymentGateway Reference :CRM Ref={0},payment gateway Ref={1}", new Object[]{crmPaymentReference, pay.getPaymentReference().getValue()});
                return false;
            }

            if (compareStringToXMLString(payment.getYourGeneralReference(), pay.getYourGeneralReference()) == false) {
                LOGGER.log(Level.INFO, "PaymentBean compareScheduledPaymentXMLToEntity failed  - compareStringToXMLString(payment.getYourGeneralReference(), pay.getYourGeneralReference() ,Payment CRM database id:{0}", new Object[]{payment.getId().toString(), payment.getPaymentStatus()});

                return false;
            }
            if (compareStringToXMLString(payment.getYourSystemReference(), pay.getYourSystemReference()) == false) {
                LOGGER.log(Level.INFO, "PaymentBean compareScheduledPaymentXMLToEntity failed  - compareStringToXMLString(payment.getYourSystemReference(), pay.getYourSystemReference() ,Payment CRM database id:{0}", new Object[]{payment.getId().toString(), payment.getPaymentStatus()});

                return false;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Payment Bean compareScheduledPaymentXMLToEntity method failed.:", e.getMessage());
            return false;
        }

        return true;

    }

    private boolean compareScheduledPaymentXMLToEntityAndUpdateAmountAndStatus(Payments payment, ScheduledPayment pay) {

        if (payment == null || pay == null) {
            return false;
        }
        try {

            if (payment.getCustomerName() == null) {
                return false;
            } else if (compareStringToXMLString(payment.getCustomerName().getId().toString(), (pay.getYourSystemReference())) == false) {
                LOGGER.log(Level.WARNING, "PaymentBean compareScheduledPaymentXMLToEntity method - customer ID did not match:Payments={0},ScheduledPayment={1}", new Object[]{payment.getCustomerName().getId().toString(), pay.getYourSystemReference()});
                return false;
            }
            int crmPaymentReference = payment.getId();
            int payGatewayReference = -1;
            try {
                payGatewayReference = Integer.parseInt(pay.getPaymentReference().getValue());
            } catch (NumberFormatException numberFormatException) {
                LOGGER.log(Level.WARNING, "PaymentBean compareScheduledPaymentXMLToEntity method - payment ID did not match paymentGateway Reference :CRM Ref={0},payment gateway Ref={1}", new Object[]{crmPaymentReference, pay.getPaymentReference().getValue()});
                return false;
            }

            if (crmPaymentReference != payGatewayReference) {
                LOGGER.log(Level.WARNING, "PaymentBean compareScheduledPaymentXMLToEntity method - payment ID did not match paymentGateway Reference :CRM Ref={0},payment gateway Ref={1}", new Object[]{crmPaymentReference, payGatewayReference});

                return false;
            }
            if (compareStringToXMLString(payment.getYourSystemReference(), pay.getYourSystemReference()) == false) {
                return false;
            }
            if (compareStringToXMLString(payment.getEzidebitCustomerID(), pay.getEzidebitCustomerID()) == false) {
                return false;
            }
            if (compareDateToXMLGregCalendar(payment.getDebitDate(), pay.getPaymentDate()) == false) {
                payment.setDebitDate(pay.getPaymentDate().toGregorianCalendar().getTime());
            }

            if (payment.getPaymentStatus().contains(PaymentStatus.SCHEDULED.value()) == false) {
                if (payment.getPaymentStatus().contains(PaymentStatus.WAITING.value()) == false) {
                    payment.setPaymentStatus(PaymentStatus.SCHEDULED.value());
                }
            }

            if (compareBigDecimalToDouble(payment.getPaymentAmount(), pay.getPaymentAmount()) == false) {
                LOGGER.log(Level.WARNING, "Payment Bean compareScheduledPaymentXMLToEntity method failed. Payment BigDec Amounts dont match CRM DB {0} , payment gateway {1}", new Object[]{payment.getPaymentAmount(), pay.getPaymentAmount()});
                payment.setPaymentAmount(new BigDecimal(pay.getPaymentAmount()));
            }
            /* if (!Objects.equals(payment.getManuallyAddedPayment(), pay.isManuallyAddedPayment())) {
                 return false;
                 }*/
            paymentsFacade.edit(payment);
            LOGGER.log(Level.INFO, "PaymentBean compareScheduledPaymentXMLToEntity method - Updated and pushed to DB:Payments={0},ScheduledPayment={1}", new Object[]{payment.getCustomerName().getId().toString(), pay.getYourSystemReference()});

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Payment Bean compareScheduledPaymentXMLToEntity method failed.:", e.getMessage());
            return false;
        }

        return true;

    }

    private boolean compareStringToXMLString(String s, JAXBElement<String> xs) {
        boolean result = true;// return true if they are the same
        String s2 = null;
        if (s != null) {
            s = s.trim();
            if (s.isEmpty()) {
                s = null;
            }
        }
        if (xs != null) {
            if (xs.isNil() == false) {
                s2 = xs.getValue().trim();
                if (s2.isEmpty()) {
                    s2 = null;
                }
            } else {
                s2 = null;
            }
        }
        if (s == null && s2 == null) {
            return true;
        }
        if ((s == null && s2 != null) || (s != null && s2 == null)) {
            return false;
        }

        if (s.compareTo(s2) != 0) {
            return false;
        }
        return result;
    }

    private boolean compareDateToXMLGregCalendar(Date d, XMLGregorianCalendar xgc) {
        boolean result = true;// return true if they are the same
        GregorianCalendar gc = null;
        if (xgc != null) {
            if (xgc.toGregorianCalendar() != null) {
                gc = xgc.toGregorianCalendar();
            }
        }

        if (d == null && gc == null) {
            return true;
        }
        if ((d == null && gc != null) || (d != null && gc == null)) {
            return false;
        }
        Date d2 = gc.getTime();
        if (d.compareTo(d2) != 0) {
            return false;
        }
        return result;
    }

    private boolean compareDateToXMLGregCal(Date d, JAXBElement<XMLGregorianCalendar> jxgc) {
        boolean result = true;// return true if they are the same
        GregorianCalendar xgc = null;
        if (jxgc != null) {
            if (jxgc.isNil() == false) {
                xgc = jxgc.getValue().toGregorianCalendar();
            }
        }

        if (d == null && xgc == null) {
            return true;
        }
        if ((d == null && xgc != null) || (d != null && xgc == null)) {
            return false;
        }
        Date d2 = xgc.getTime();
        if (d.compareTo(d2) != 0) {
            return false;
        }
        return result;
    }

    private boolean compareBigDecimalToDouble(BigDecimal d, Double bd) {
        boolean result = false;// return true if they are the same
        BigDecimal d2 = null;
        if (bd != null) {
            d2 = new BigDecimal(bd);
        }

        if (d == null || d2 == null) {
            return false;
        }

        if (d.compareTo(d2) == 0) {
            return true;
        }
        return result;
    }

    private boolean comparePaymentXMLToEntity(Payments payment, Payment pay) {

        if (payment == null || pay == null) {
            return payment == null && pay == null;
        }
        try {

            if (compareStringToXMLString(payment.getBankFailedReason(), pay.getBankFailedReason()) == false) {
                return false;
            }

            if (compareStringToXMLString(payment.getBankReceiptID(), pay.getBankReceiptID()) == false) {
                return false;
            }
            if (compareStringToXMLString(payment.getBankReturnCode(), pay.getBankReturnCode()) == false) {
                return false;
            }
            if (payment.getCustomerName() == null) {
                return false;
            } else if (compareStringToXMLString(payment.getCustomerName().getId().toString(), (pay.getYourSystemReference())) == false) {
                return false;
            }
            if (compareDateToXMLGregCal(payment.getDebitDate(), pay.getDebitDate()) == false) {
                return false;
            }
            if (compareStringToXMLString(payment.getEzidebitCustomerID(), pay.getEzidebitCustomerID()) == false) {
                return false;
            }
            if (compareStringToXMLString(payment.getInvoiceID(), pay.getInvoiceID()) == false) {
                return false;
            }
            if (compareBigDecimalToDouble(payment.getPaymentAmount(), pay.getPaymentAmount()) == false) {
                return false;
            }
            if (compareStringToXMLString(payment.getPaymentID(), pay.getPaymentID()) == false) {
                return false;
            }
            if (compareStringToXMLString(payment.getPaymentMethod(), pay.getPaymentMethod()) == false) {
                return false;
            }
            if (compareStringToXMLString(payment.getPaymentReference(), pay.getPaymentReference()) == false) {
                return false;
            }
            if (compareStringToXMLString(payment.getPaymentSource(), pay.getPaymentSource()) == false) {
                return false;
            }
            if (compareStringToXMLString(payment.getPaymentStatus(), pay.getPaymentStatus()) == false) {
                return false;
            }
            if (compareBigDecimalToDouble(payment.getScheduledAmount(), pay.getScheduledAmount()) == false) {
                return false;
            }
            if (compareDateToXMLGregCal(payment.getSettlementDate(), pay.getSettlementDate()) == false) {
                return false;
            }
            if (compareBigDecimalToDouble(payment.getTransactionFeeClient(), pay.getTransactionFeeClient()) == false) {
                return false;
            }
            if (compareBigDecimalToDouble(payment.getTransactionFeeCustomer(), pay.getTransactionFeeCustomer()) == false) {
                return false;
            }
            if (compareDateToXMLGregCal(payment.getTransactionTime(), pay.getTransactionTime()) == false) {
                return false;
            }
            if (compareStringToXMLString(payment.getYourGeneralReference(), pay.getYourGeneralReference()) == false) {
                return false;
            }
            if (compareStringToXMLString(payment.getYourSystemReference(), pay.getYourSystemReference()) == false) {
                return false;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Payment Bean comparePaymentXMLToEntity method failed.:", e.getMessage());
            return false;
        }

        return true;

    }

    @Asynchronous
    public Future<PaymentGatewayResponse> addCustomer(Customers cust, String paymentGatewayName, String digitalKey, String authenticatedUser, String sessionId) {
        PaymentGatewayResponse pgr = new PaymentGatewayResponse(false, null, "", "-1", "An unhandled error occurred!");
        String paymentGatewayReference = "";
        try {
            if (authenticatedUser == null) {

                LOGGER.log(Level.INFO, "Authenticated User is NULL - Aborting add customer to ezidebit");
                futureMap.processAddCustomer(sessionId, pgr);
                return new AsyncResult<>(new PaymentGatewayResponse(false, null, "", "-1", "Authenticated User is NULL"));

            }

            if (cust == null || digitalKey == null || paymentGatewayName == null) {
                futureMap.processAddCustomer(sessionId, pgr);
                return new AsyncResult<>(new PaymentGatewayResponse(false, null, "", "-1", "The Customer Object is NULL, the Digital key is null or the paymentGateway name is null."));
            }
            if (cust.getId() == null || digitalKey.trim().isEmpty()) {
                futureMap.processAddCustomer(sessionId, pgr);
                return new AsyncResult<>(new PaymentGatewayResponse(false, null, "", "-1", "The Customer id is null or the digital key is Empty"));
            }
            // check if customer already exists in the gateway
            // if they are in cancelled status modify the old references so a new record can be created as cancelled customers in exidebit can't be reactivated.

            CustomerDetails cd = null;
            EziResponseOfCustomerDetailsTHgMB7OL customerdetails = getWs().getCustomerDetails(digitalKey, "", cust.getId().toString());
            if (customerdetails.getError() == 0) {// any errors will be a non zero value
                cd = customerdetails.getData().getValue();
                LOGGER.log(Level.INFO, "Add customer to payment gateway. The customer already exists: Payment gateway Name - {0}, Customers username - {1} ", new Object[]{cd.getCustomerName().getValue(), cust.getUsername()});

            } else {
                LOGGER.log(Level.INFO, "Add customer to payment gateway. Check if they already exist. Get Customer Details Response: Error - {0}", customerdetails.getErrorMessage().getValue());

            }
            if (cd != null) {

                if (cd.getStatusDescription().getValue().toUpperCase().contains("CANCELLED")) {
                    String ourSystemRef = cd.getYourSystemReference().getValue();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                    ourSystemRef += "-CANCELLED-" + sdf.format(new Date());
                    String ourGeneralReference = cust.getLastname() + " " + cust.getFirstname();

                    EziResponseOfstring editCustomerDetail = getWs().editCustomerDetails(digitalKey, "", cust.getId().toString(), ourSystemRef, ourGeneralReference, cd.getCustomerName().getValue(), cd.getCustomerFirstName().getValue(), cust.getStreetAddress(), cd.getAddressLine2().getValue(), cust.getSuburb(), cust.getPostcode(), cust.getAddrState(), cust.getEmailAddress(), cust.getTelephone().trim(), cd.getSmsPaymentReminder().getValue(), cd.getSmsFailedNotification().getValue(), cd.getSmsExpiredCard().getValue(), authenticatedUser);
                    LOGGER.log(Level.INFO, "editCustomerDetail Response: Error - {0}, Data - {1}", new Object[]{editCustomerDetail.getErrorMessage().getValue(), editCustomerDetail.getData().getValue()});
                    if (editCustomerDetail.getError() == 0) {// any errors will be a non zero value

                        LOGGER.log(Level.INFO, "Reuse old cancelled reference. Edit - Response:  Data - {1}", new Object[]{editCustomerDetail.getData().getValue()});
                        pgr = new PaymentGatewayResponse(true, cust, "The existing cancelled customer record was Added back to the payment gateway successfully.", "0", "EXISTING");
                    } else {
                        pgr = new PaymentGatewayResponse(false, null, "The new customer record could not be added to the payment gateway due to an error.", editCustomerDetail.getError().toString(), editCustomerDetail.getErrorMessage().getValue());

                        LOGGER.log(Level.WARNING, "editCustomerDetail Response: Error - {0}", new Object[]{editCustomerDetail.getErrorMessage().getValue(), editCustomerDetail.getError().toString()});

                    }

                } else {
                    String customeName = cd.getCustomerName().getValue();
                    String status = cd.getStatusDescription().getValue();
                    LOGGER.log(Level.INFO, "The customer {0} already exists in teh payment gateway - Response:  Payment Gateway Status - {1}", new Object[]{customeName, status});
                    pgr = new PaymentGatewayResponse(true, cust, "The customer  " + customeName + " already exists in the payment system with status " + status + ".", "0", "EXISTING");

                }
                cust.getPaymentParametersId().setStatusCode(cd.getStatusCode().getValue());
                cust.getPaymentParametersId().setStatusDescription(cd.getStatusDescription().getValue());
                customersFacade.editAndFlush(cust);

            } else {

                LOGGER.log(Level.INFO, "Adding Customer  {0}", cust.getUsername());

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

                PaymentParameters payParams = cust.getPaymentParametersId();
                String addresssLine2 = ""; // not used
                String humanFriendlyReference = cust.getId() + " " + cust.getLastname().toUpperCase() + " " + cust.getFirstname().toUpperCase(); // existing customers use this type of reference by default
                /* if (payParams == null && paymentGatewayName.toUpperCase().contains(PAYMENT_GATEWAY)) {

                    payParams = new PaymentParameters(0, new Date(), cust.getTelephone(), "NO", "NO", "NO", PAYMENT_GATEWAY);
                    //Customers loggedInUser = customersFacade.findCustomerByUsername(FacesContext.getCurrentInstance().getExternalContext().getRemoteUser());
                    payParams.setLoggedInUser(cust);

                    cust.setPaymentParameters(payParams);
                    customersFacade.editAndFlush(cust);
                }*/

                if (payParams == null) {
                    LOGGER.log(Level.WARNING, "Payment gateway EZIDEBIT parameters not found");
                    pgr = new PaymentGatewayResponse(false, null, "", "-1", "Payment gateway EZIDEBIT parameters not found!");
                    futureMap.processAddCustomer(sessionId, pgr);
                    return new AsyncResult<>(pgr);
                }

// note - NB - for Australian Customers the
//mobile phone number must be 10
//digits long and begin with '04'. For
//New Zealand Customers the mobile
//phone number must be 10 digits
//long and begin with '02'
                String phoneNumber = cust.getTelephone().trim();
                if (phoneNumber == null) {
                    phoneNumber = "";
                    LOGGER.log(Level.INFO, "Invalid Phone Number for Customer {0}. Setting it to empty string", cust.getUsername());
                }
                Pattern p = Pattern.compile("\\d{10}");
                Matcher m = p.matcher(phoneNumber);
                //ezidebit requires an australian mobile phone number that starts with 04
                if (m.matches() == false || phoneNumber.startsWith("04") == false) {
                    phoneNumber = "";
                    LOGGER.log(Level.WARNING, "Invalid Phone Number for Customer {0}. Setting it to empty string", cust.getUsername());
                }
                Date contractStartDate = new Date();

                EziResponseOfNewCustomerXcXH3LiW addCustomerResponse = getWs().addCustomer(digitalKey, cust.getId().toString(), humanFriendlyReference, cust.getLastname(), cust.getFirstname(), cust.getStreetAddress(), addresssLine2, cust.getSuburb(), cust.getAddrState(), cust.getPostcode(), cust.getEmailAddress(), phoneNumber, sdf.format(contractStartDate), payParams.getSmsPaymentReminder(), payParams.getSmsFailedNotification(), payParams.getSmsExpiredCard(), cust.getUsername());

                if (addCustomerResponse.getError() == 0) {// any errors will be a non zero value

                    paymentGatewayReference = addCustomerResponse.getData().getValue().getCustomerRef().toString();
                    cust.getPaymentParametersId().setEzidebitCustomerID(paymentGatewayReference);
                    customersFacade.editAndFlush(cust);
                    pgr = new PaymentGatewayResponse(true, cust, "The new customer record was added to the payment gateway successfully.", "0", "NEW");

                    LOGGER.log(Level.INFO, "Add Customer Response: New Customer payment gateway reference number =  {0}", new Object[]{addCustomerResponse.getData().getValue().getCustomerRef().toString()});

                } else {
                    LOGGER.log(Level.WARNING, "Add Customer Response: Error - {0},", addCustomerResponse.getErrorMessage().getValue());
                    pgr = new PaymentGatewayResponse(false, null, "The new customer record could not be added to the payment gateway due to an error.", addCustomerResponse.getError().toString(), addCustomerResponse.getErrorMessage().getValue());
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Add Customer - FATAL Error - ,", e);
            pgr = new PaymentGatewayResponse(false, e, "", "-1", e.getMessage());
            futureMap.processAddCustomer(sessionId, pgr);
            return new AsyncResult<>(pgr);
        }
        if (pgr.isOperationSuccessful() == true) {
            String auditDetails = "Customer Added :" + cust.getUsername() + " to  " + PAYMENT_GATEWAY;
            String changedFrom = "non-existant";
            String changedTo = "New Customer:";
            auditLogFacade.audit(customersFacade.findCustomerByUsername(authenticatedUser), cust, "addCustomer", auditDetails, changedFrom, changedTo);
        }
        futureMap.processAddCustomer(sessionId, pgr);
        return new AsyncResult<>(pgr);
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

            LOGGER.log(Level.WARNING, "getPaymentDetail paymentReference is required but it is NULL");
            return new AsyncResult<>(result);
        }

        if (paymentReference.length() > 50) {
            paymentReference = paymentReference.substring(0, 50);
            LOGGER.log(Level.WARNING, "getPaymentDetail paymentReference is greater than the allowed 50 characters. Truncating! to 50 chars");
        }

        EziResponseOfPaymentDetailTHgMB7OL eziResponse = getWs().getPaymentDetail(digitalKey, paymentReference);
        LOGGER.log(Level.INFO, "getPaymentDetail Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value
            result = eziResponse.getData().getValue();

        } else {
            LOGGER.log(Level.WARNING, "getPaymentDetail Response: Error - {0}", eziResponse.getErrorMessage().getValue());

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

            LOGGER.log(Level.WARNING, "getPaymentStatus paymentReference is required but it is NULL");
            return new AsyncResult<>(result);
        }

        if (paymentReference.length() > 50) {
            paymentReference = paymentReference.substring(0, 50);
            LOGGER.log(Level.WARNING, "getPaymentStatus paymentReference is greater than the allowed 50 characters. Truncating! to 50 chars");
        }

        EziResponseOfstring eziResponse = getWs().getPaymentStatus(digitalKey, paymentReference);
        LOGGER.log(Level.INFO, "getPaymentStatus Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value

            if (eziResponse.getData().getValue().compareTo("S") == 0) {
                result = true;
            } else {
                LOGGER.log(Level.WARNING, "getPaymentStatus Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
            }
        } else {
            LOGGER.log(Level.WARNING, "getPaymentStatus Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

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

            LOGGER.log(Level.WARNING, "getPaymentDetailPlusNextPaymentInfo paymentReference is required but it is NULL");
            return new AsyncResult<>(result);
        }

        if (paymentReference.length() > 50) {
            paymentReference = paymentReference.substring(0, 50);
            LOGGER.log(Level.WARNING, "getPaymentDetailPlusNextPaymentInfo paymentReference is greater than the allowed 50 characters. Truncating! to 50 chars");
        }

        EziResponseOfPaymentDetailPlusNextPaymentInfoTHgMB7OL eziResponse = getWs().getPaymentDetailPlusNextPaymentInfo(digitalKey, paymentReference);
        LOGGER.log(Level.INFO, "getPaymentDetailPlusNextPaymentInfo Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value
            result = eziResponse.getData().getValue();

        } else {
            LOGGER.log(Level.WARNING, "getPaymentDetailPlusNextPaymentInfo Response: Error - {0}, ", eziResponse.getErrorMessage().getValue());

        }

        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<PaymentGatewayResponse> getAllCustPaymentsAndDetails(Customers cust, String paymentType, String paymentMethod, String paymentSource, String paymentReference, Date fromDate, Date toDate, boolean useSettlementDate, String digitalKey, String sessionId) {
        String result = "Error";
        PaymentGatewayResponse pgr = new PaymentGatewayResponse(false, result, "", "-1", "An unhandled error occurred!");

        String batchSessionId = "Batch-Dont-Update";
        PaymentGatewayResponse pgr1 = null;
        PaymentGatewayResponse pgr2 = null;
        PaymentGatewayResponse pgr3 = null;
        try {

            Future<PaymentGatewayResponse> gpResponse = getPayments(cust, paymentType, paymentMethod, paymentSource, paymentReference, fromDate, toDate, useSettlementDate, digitalKey, batchSessionId);
            pgr1 = gpResponse.get();
            Future<PaymentGatewayResponse> gspResponse = getScheduledPayments(cust, fromDate, toDate, digitalKey, batchSessionId, true);
            Future<PaymentGatewayResponse> gcdResponse = getCustomerDetails(cust, digitalKey, batchSessionId);

            pgr2 = gspResponse.get();
            pgr3 = gcdResponse.get();
            result = "";
            if (pgr1.isOperationSuccessful()) {
                result = "Get Payments - OK, ";
            } else {
                result = "Get Payments - FAIL, ";
            }
            if (pgr2.isOperationSuccessful()) {
                result += "Get Scheduled - OK, ";
            } else {
                result += "Get Scheduled - FAIL, ";
            }
            if (pgr3.isOperationSuccessful()) {
                result += "Get Details - OK ";
            } else {
                result += "Get Details - FAIL ";
            }

        } catch (InterruptedException | ExecutionException e) {
            LOGGER.log(Level.WARNING, "getAllCustPaymentsAndDetails Response: Error : ", e);
            pgr = new PaymentGatewayResponse(false, null, "Customer details and Payment information was not recieved from the payment gateway due to an error.", "Server Fault", e.getMessage());
            futureMap.processGetAllCustPaymentsAndDetails(sessionId, pgr);
            return new AsyncResult<>(pgr);
        }
        if (pgr1.isOperationSuccessful() && pgr2.isOperationSuccessful() && pgr3.isOperationSuccessful()) {
            LOGGER.log(Level.INFO, "getAllCustPaymentsAndDetails: Details, Payments and Scheduled payments recieved OK");
            pgr = new PaymentGatewayResponse(true, result, "", "0", "Updated Customer Details and Payment Information was recieved from the payment gateway.");

        } else {
            LOGGER.log(Level.INFO, "getAllCustPaymentsAndDetails: The customer object passed to this method is NULL.This parameter is required.Returning empty array!!");
            pgr = new PaymentGatewayResponse(false, result, "", "-1", "The customer object passed to this method is NULL.This parameter is required.Returning empty array!!");

        }
        futureMap.processGetAllCustPaymentsAndDetails(sessionId, pgr);
        return new AsyncResult<>(pgr);

    }

    @Asynchronous
    public Future<PaymentGatewayResponse> getPayments(Customers cust, String paymentType, String paymentMethod, String paymentSource, String paymentReference, Date fromDate, Date toDate, boolean useSettlementDate, String digitalKey, String sessionId) {
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
        boolean result = false;
        PaymentGatewayResponse pgr = new PaymentGatewayResponse(false, result, "", "-1", "An unhandled error occurred!");

        if (cust == null) {
            LOGGER.log(Level.WARNING, "getPayments: The customer object passed to this method is NULL.This parameter is required.Returning empty array!!");
            pgr = new PaymentGatewayResponse(false, result, "", "-1", "The customer object passed to this method is NULL.This parameter is required.Returning empty array!!");
            futureMap.processGetPayments(sessionId, pgr);
            return new AsyncResult<>(pgr);

        }
        try {
            LOGGER.log(Level.INFO, "Running asychronous task getPayments Customer {0}, From Date {1}, to Date {2}", new Object[]{cust, fromDate, toDate});
            if (paymentType.compareTo("ALL") != 0 && paymentType.compareTo("PENDING") != 0 && paymentType.compareTo("FAILED") != 0 && paymentType.compareTo("SUCCESSFUL") != 0) {
                LOGGER.log(Level.WARNING, "getPayments: payment Type is required and should be either ALL,PENDING,FAILED,SUCCESSFUL.  Returning null as this parameter is required.");
                pgr = new PaymentGatewayResponse(false, result, "", "-1", "The Payment Type is required and should be either ALL,PENDING,FAILED,SUCCESSFUL.");
                futureMap.processGetPayments(sessionId, pgr);
                return new AsyncResult<>(pgr);
            }
            if (paymentMethod.compareTo("ALL") != 0 && paymentMethod.compareTo("CR") != 0 && paymentMethod.compareTo("DR") != 0) {
                LOGGER.log(Level.WARNING, "getPayments: payment Method is required and should be either ALL,CR,DR.  Returning null as this parameter is required.");
                pgr = new PaymentGatewayResponse(false, result, "", "-1", "The Payment Method is required and should be either ALL,CR,DR.");
                futureMap.processGetPayments(sessionId, pgr);
                return new AsyncResult<>(pgr);
            }
            if (paymentSource.compareTo("ALL") != 0 && paymentSource.compareTo("SCHEDULED") != 0 && paymentSource.compareTo("WEB") != 0 && paymentSource.compareTo("PHONE") != 0 && paymentSource.compareTo("BPAY") != 0) {
                LOGGER.log(Level.WARNING, "getPayments: paymentSource is required and should be either ALL,SCHEDULED,WEB,PHONE,BPAY.  Returning null as this parameter is required.");
                pgr = new PaymentGatewayResponse(false, result, "", "-1", "The PaymentSource is required and should be either ALL,SCHEDULED,WEB,PHONE,BPAY.");
                futureMap.processGetPayments(sessionId, pgr);
                return new AsyncResult<>(pgr);
            }
            if (paymentReference == null) {
                paymentReference = "";
            }
            if (paymentReference.length() > 50) {
                paymentReference = paymentReference.substring(0, 50);
                LOGGER.log(Level.WARNING, "getPayments paymentReference is greater than the allowed 50 characters. Truncating! to 50 chars");
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
            ArrayOfPayment resultPaymentArray;
            String ourSystemCustomerReference = cust.getId().toString();
            LOGGER.log(Level.INFO, "Payment Bean - Running async task - Getting Payments for Customer {0}", cust.getUsername());
            EziResponseOfArrayOfPaymentTHgMB7OL eziResponse = getWs().getPayments(digitalKey, paymentType, paymentMethod, paymentSource, paymentReference, fromDateString, toDateString, dateField, eziDebitCustomerId, ourSystemCustomerReference);
            // LOGGER.log(Level.INFO, "getPayments Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
            if (eziResponse.getError() == 0) {// any errors will be a non zero value
                result = true;
                resultPaymentArray = eziResponse.getData().getValue();
                boolean abort = false;
                if (resultPaymentArray != null && resultPaymentArray.getPayment() != null) {
                    List<Payment> payList = resultPaymentArray.getPayment();
                    if (payList.isEmpty() == false) {
                        //check the customer reference of teh first payment and verify they match
                        String customerRef = payList.get(0).getYourSystemReference().getValue();
                        int k = customerRef.indexOf('-');
                        if (k > 0) {
                            customerRef = customerRef.substring(0, k);
                        }
                        if (customerRef.trim().isEmpty() == false) {
                            int custId = 0;
                            try {
                                custId = Integer.parseInt(customerRef.trim());
                            } catch (NumberFormatException numberFormatException) {
                                LOGGER.log(Level.WARNING, "Payment Bean processGetPayments an ezidebit YourSystemReference string cannot be converted to a number.", numberFormatException);

                            }

                            Customers customerReferenceInPayment = customersFacade.findById(custId);
                            if (!Objects.equals(cust.getId(), customerReferenceInPayment.getId())) {
                                LOGGER.log(Level.WARNING, "Payment Bean processGetPayments . The customer ref of teh first payment in the array does not match the cust ref given to method. Aborting.");
                                abort = true;
                            }
                            if (cust != null) {
                                LOGGER.log(Level.INFO, "Payment Bean processGetPayments. Processing {0} payments for customer {1}.", new Object[]{payList.size(), cust.getUsername()});
                                for (Payment pay : payList) {
                                    if (customerRef.compareTo(pay.getYourSystemReference().getValue().trim()) != 0) {
                                        LOGGER.log(Level.WARNING, "Payment Bean processGetPayments . The list being processed contains multiple customers.It should only contain one for this method. Aborting.");
                                        abort = true;
                                    }

                                }
                                if (abort == false) {

                                    // sanity checks have passed -- process payments
                                    for (Payment pay : payList) {
                                        String paymentID = pay.getPaymentID().getValue();
                                        String paymentRef;
                                        Payments crmPay = null;
                                        int paymentRefInt = 0;
                                        boolean validReference = false;
                                        if (pay.getPaymentReference().isNil() == false) {
                                            paymentRef = pay.getPaymentReference().getValue().trim();
                                            if (paymentRef.contains("-") == false && paymentRef.length() > 0) {
                                                try {
                                                    paymentRefInt = Integer.parseInt(paymentRef);
                                                    crmPay = paymentsFacade.findPaymentById(paymentRefInt, false);
                                                    if (crmPay != null) {
                                                        validReference = true;
                                                    }
                                                } catch (NumberFormatException numberFormatException) {
                                                }
                                            }
                                        }
                                        if (validReference) {
                                            if (comparePaymentXMLToEntity(crmPay, pay) == false) {
                                                crmPay = convertPaymentXMLToEntity(crmPay, pay, cust);
                                                LOGGER.log(Level.INFO, "Payment Bean processGetPayments  - updating payment id:{0}.", paymentRefInt);
                                                paymentsFacade.edit(crmPay);
                                            }
                                        } else // old payment without a primary key reference
                                        {
                                            if (paymentID.toUpperCase(Locale.getDefault()).contains("SCHEDULED")) {
                                                // scheduled payment no paymentID
                                                LOGGER.log(Level.INFO, "Payment Bean processGetPayments scheduled payment .", pay.toString());
                                            } else {

                                                crmPay = paymentsFacade.findPaymentByPaymentId(paymentID);

                                                if (crmPay != null) { //' payment exists
                                                    if (comparePaymentXMLToEntity(crmPay, pay)) {
                                                        // they are the same so no update
                                                        LOGGER.log(Level.FINE, "Payment Bean processGetPayments paymenst are the same.");
                                                    } else {
                                                        crmPay = convertPaymentXMLToEntity(crmPay, pay, cust);
                                                        paymentsFacade.edit(crmPay);
                                                    }
                                                } else { //payment doesn't exist in crm so add it
                                                    LOGGER.log(Level.WARNING, "Payment Bean processGetPayments  - payment doesn't exist in crm (this should only happen for webddr form schedule) so adding it:{0}.", paymentRefInt);
                                                    crmPay = convertPaymentXMLToEntity(null, pay, cust);
                                                    paymentsFacade.createAndFlushForGeneratedIdEntities(crmPay);
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                LOGGER.log(Level.SEVERE, "Payment Bean processGetPayments couldn't find a customer with our system ref ({0}) from payment.", customerRef);
                                /*TODO email a report at the end of the process if there are any payments swithout a customer reference
                         as this means that a customer is in ezidebits system but not ours */

                            }
                        } else {
                            LOGGER.log(Level.WARNING, "Payment Bean processGetPayments our system ref in payment is null.");
                        }

                    }
                }

                if (resultPaymentArray != null) {
                    LOGGER.log(Level.INFO, "Payment Bean - Get Customer Payments Response Recieved from ezidebit for Customer  - {0}, Number of Payments : {1} ", new Object[]{cust.getUsername(), resultPaymentArray.getPayment().size()});
                    pgr = new PaymentGatewayResponse(true, result, "", "0", "Updated Payment Information was recieved from the payment gateway.");
                } else {
                    LOGGER.log(Level.WARNING, "getPayments Response: Error - NULL Result ");
                    pgr = new PaymentGatewayResponse(false, null, "", "0", "The payment information recieved from the payment gateway was empty");
                }
            } else {
                LOGGER.log(Level.WARNING, "getPayments Response: Error - {0}, ", eziResponse.getErrorMessage().getValue());
                pgr = new PaymentGatewayResponse(false, null, "Payment information was not recieved from the payment gateway due to an error.", eziResponse.getError().toString(), eziResponse.getErrorMessage().getValue());

            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "getPayments Response: Error : ", e);
            pgr = new PaymentGatewayResponse(false, null, "Payment information was not recieved from the payment gateway due to an error.", "Server Fault", e.getMessage());
        }
        futureMap.processGetPayments(sessionId, pgr);
        return new AsyncResult<>(pgr);
    }

    private Payments convertPaymentXMLToEntity(Payments payment, Payment pay, Customers cust) {

        if (pay == null || cust == null) {
            String p1 = "NULL";
            String p2 = "NULL";
            String p3 = "NULL";
            if (payment != null) {
                p1 = payment.getYourSystemReference();
            }
            if (cust != null) {
                p3 = cust.getUsername();
            }
            if (pay != null) {
                if (pay.getYourSystemReference().isNil() == false) {
                    p2 = pay.getYourSystemReference().getValue();
                } else {
                    LOGGER.log(Level.WARNING, "Payment Bean convertPaymentXMLToEntity method failed.Your system reference is NULL. Customer {2},payment pojo {0},payment XML {1}:", new Object[]{p1, p2, p3});

                }
            }

            LOGGER.log(Level.WARNING, "Payment Bean convertPaymentXMLToEntity method failed.Cant poceed due to a null value. Customer {2},payment pojo {0},payment XML {1}:", new Object[]{p1, p2, p3});
            return null;
        }
        if (payment == null) {
            payment = new Payments();
            payment.setPaymentSource(PaymentSource.DIRECT_DEBIT.value());
            payment.setCreateDatetime(new Date());
            payment.setManuallyAddedPayment(false);
            payment.setId(0);
            payment.setCustomerName(cust);
        }
        try {

            payment.setLastUpdatedDatetime(new Date());

            if (pay.getBankFailedReason().isNil() == false) {
                payment.setBankFailedReason(pay.getBankFailedReason().getValue());
            }
            if (pay.getBankReceiptID().isNil() == false) {
                payment.setBankReceiptID(pay.getBankReceiptID().getValue());
            }
            if (pay.getBankReturnCode().isNil() == false) {
                payment.setBankReturnCode(pay.getBankReturnCode().getValue());
            }
            //payment.setCustomerName(cust);

            try {
                payment.setDebitDate(pay.getDebitDate().getValue().toGregorianCalendar().getTime());
            } catch (NullPointerException e) {
                LOGGER.log(Level.WARNING, "Payment Bean convertPaymentXMLToEntity - DebitDate is NULL. Customer: {2},payment XML: {1}:", new Object[]{pay.toString(), cust.getUsername()});
            }

            if (pay.getEzidebitCustomerID().isNil() == false) {
                payment.setEzidebitCustomerID(pay.getEzidebitCustomerID().getValue());
            }
            if (pay.getInvoiceID().isNil() == false) {
                payment.setInvoiceID(pay.getInvoiceID().getValue());
            }
            if (pay.getPaymentAmount() != null) {
                payment.setPaymentAmount(new BigDecimal(pay.getPaymentAmount()));
            }
            if (pay.getPaymentID().isNil() == false) {
                payment.setPaymentID(pay.getPaymentID().getValue());
            }
            if (pay.getPaymentMethod().isNil() == false) {
                payment.setPaymentMethod(pay.getPaymentMethod().getValue());
            }
            if (pay.getPaymentReference().isNil() == false) {
                if (pay.getPaymentReference().getValue().trim().isEmpty()) {
                    payment.setPaymentReference(null);
                } else {
                    payment.setPaymentReference(pay.getPaymentReference().getValue());
                }
            } else {
                payment.setPaymentReference(null);
            }
            /* if (pay.getPaymentReference().isNil() == false) {
                 if (pay.getPaymentReference().getValue().trim().isEmpty() == false) {
                 payment.setManuallyAddedPayment(true);
                 } else {
                 payment.setManuallyAddedPayment(false);
                 }
                 }*/
            if (pay.getPaymentSource().isNil() == false) {
                payment.setPaymentSource(pay.getPaymentSource().getValue());
            }
            if (pay.getScheduledAmount() != null) {
                payment.setScheduledAmount(new BigDecimal(pay.getScheduledAmount()));
            }
            try {
                payment.setSettlementDate(pay.getSettlementDate().getValue().toGregorianCalendar().getTime());
            } catch (NullPointerException e) {
                LOGGER.log(Level.WARNING, "Payment Bean convertPaymentXMLToEntity - SettlementDate is NULL. Customer: {2},payment XML: {1}:", new Object[]{pay.toString(), cust.getUsername()});
            }
            if (pay.getPaymentStatus().isNil() == false) {
                payment.setPaymentStatus(pay.getPaymentStatus().getValue());
            }
            if (pay.getTransactionFeeClient() != null) {
                payment.setTransactionFeeClient(new BigDecimal(pay.getTransactionFeeClient()));
            }
            if (pay.getTransactionFeeCustomer() != null) {
                payment.setTransactionFeeCustomer(new BigDecimal(pay.getTransactionFeeCustomer()));
            }

            try {
                payment.setTransactionTime(pay.getTransactionTime().getValue().toGregorianCalendar().getTime()); // only valid for real time and credit card payments
            } catch (NullPointerException e) {
                LOGGER.log(Level.INFO, "Payment Bean convertPaymentXMLToEntity - TransactionTime is NULL. Customer: {2},payment XML: {1}:", new Object[]{pay.toString(), cust.getUsername()});
            }
            if (pay.getYourGeneralReference().isNil() == false) {
                payment.setYourGeneralReference(pay.getYourGeneralReference().getValue());
            }
            if (pay.getYourSystemReference().isNil() == false) {
                payment.setYourSystemReference(pay.getYourSystemReference().getValue());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Payment Bean convertPaymentXMLToEntity method failed. Customer: {2},Error Message: {0},payment XML: {1}:", new Object[]{e.getMessage(), pay.toString(), cust.getUsername()});
        }

        return payment;

    }

    @Asynchronous
    public Future<PaymentGatewayResponse> getCustomerDetails(Customers cust, String digitalKey, String sessionId) {

        PaymentGatewayResponse pgr = new PaymentGatewayResponse(false, null, "", "-1", "An unhandled error occurred!");
        CustomerDetails cd = null;
        if (cust == null || digitalKey == null) {
            pgr = new PaymentGatewayResponse(false, null, "", "-1", "The customer or digital key is NULL!");
            futureMap.processGetCustomerDetails(sessionId, pgr);
            return new AsyncResult<>(pgr);
        }
        if (cust.getId() == null || digitalKey.trim().isEmpty()) {
            pgr = new PaymentGatewayResponse(false, null, "", "-1", "The customer or digital key is empty!");
            futureMap.processGetCustomerDetails(sessionId, pgr);
            return new AsyncResult<>(pgr);
        }

        LOGGER.log(Level.INFO, "Payment Bean - Running async task - Getting Customer Details {0}", cust.getUsername());

        EziResponseOfCustomerDetailsTHgMB7OL customerdetails = getWs().getCustomerDetails(digitalKey, "", cust.getId().toString());
        if (customerdetails.getError() == 0) {// any errors will be a non zero value

            cd = customerdetails.getData().getValue();
            if (cd != null) {
                pgr = new PaymentGatewayResponse(true, cd, "The Customers details were retrieved from the payment gateway", "0", "");
                LOGGER.log(Level.INFO, "Payment Bean - Get Customer Details Response: Customer  - {0}, Ezidebit Name : {1} {2}", new Object[]{cust.getUsername(), cd.getCustomerFirstName().getValue(), cd.getCustomerName().getValue()});
                updatePaymentParameters(cust, cd, 0);
            }
        } else {
            pgr = new PaymentGatewayResponse(false, null, "The Customers details were not retrieved from the payment gateway due to an error.", customerdetails.getError().toString(), customerdetails.getErrorMessage().getValue());
            LOGGER.log(Level.WARNING, "Get Customer Details Response: Error - {0}", customerdetails.getErrorMessage().getValue());
            updatePaymentParameters(cust, null, customerdetails.getError());

        }
        futureMap.processGetCustomerDetails(sessionId, pgr);
        return new AsyncResult<>(pgr);

    }

    @Asynchronous
    public Future<PaymentGatewayResponse> updateCustomerPaymentSchedule(Customers cust, String digitalKey, String sessionId) {

        try {
            PaymentGatewayResponse pgr = new PaymentGatewayResponse(false, null, "", "-1", "An unhandled error occurred!");
            CustomerDetails cd;
            if (cust == null || digitalKey == null) {
                pgr = new PaymentGatewayResponse(false, null, "", "-1", "The customer or digital key is NULL!");
                //futureMap.processUpdateCustomerSchedule(sessionId, pgr);
                return new AsyncResult<>(pgr);
            }
            if (cust.getId() == null || digitalKey.trim().isEmpty()) {
                pgr = new PaymentGatewayResponse(false, null, "", "-1", "The customer or digital key is empty!");
                //futureMap.processUpdateCustomerSchedule(sessionId, pgr);
                return new AsyncResult<>(pgr);
            }

            LOGGER.log(Level.INFO, "Payment Bean - Running async task - Getting Customer Details {0}", cust.getUsername());

            EziResponseOfCustomerDetailsTHgMB7OL customerdetails = null;
            try {
                customerdetails = getWs().getCustomerDetails(digitalKey, "", cust.getId().toString());
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Payment Bean - Running async task - Getting Customer Details {0}.Error {1}", new Object[]{cust.getUsername(), e.getMessage()});
            }
            if (customerdetails != null) {
                if (customerdetails.getError() == 0) {// any errors will be a non zero value

                    cd = customerdetails.getData().getValue();
                    if (cd != null) {
                        pgr = new PaymentGatewayResponse(true, cd, "The Customers details were retrieved from the payment gateway", "0", "");
                        LOGGER.log(Level.INFO, "Payment Bean - Get Customer Details Response: Customer  - {0}, Ezidebit Name : {1} {2}", new Object[]{cust.getUsername(), cd.getCustomerFirstName().getValue(), cd.getCustomerName().getValue()});
                        updatePaymentParameters(cust, cd, 0);

                        // update schedule
                        GregorianCalendar paymentsStopDate = new GregorianCalendar();
                        GregorianCalendar paymentsStartDate = new GregorianCalendar();
                        paymentsStopDate.add(Calendar.MONTH, PAYMENT_SCHEDULE_MONTHS_AHEAD);
                        // get scheduled payments in the next 11 months 
                        Future<PaymentGatewayResponse> csp = getScheduledPayments(cust, paymentsStartDate.getTime(), paymentsStopDate.getTime(), digitalKey, sessionId, true);
                        PaymentGatewayResponse pgr2;
                        try {
                            pgr2 = csp.get();
                        } catch (InterruptedException | ExecutionException ex) {
                            Logger.getLogger(PaymentBean.class.getName()).log(Level.SEVERE, "updateCustomerPaymentSchedule", ex);
                            pgr = new PaymentGatewayResponse(false, null, "", "-1", ex.getMessage());
                            futureMap.processUpdateCustomerSchedule(sessionId, pgr);
                            return new AsyncResult<>(pgr);
                        }

                        if (pgr2.isOperationSuccessful()) {

                            PaymentParameters pp = cust.getPaymentParametersId();
                            //Collection<Payments> pl = cust.getPaymentsCollection();// cached values were stale 
                            Collection<Payments> pl = paymentsFacade.findPaymentsByCustomer(cust, true);
                            if (pl.isEmpty() == false) {
                                Optional<Payments> op = pl.stream().max(Comparator.comparing(Payments::getDebitDate));
                                Payments lastSchedPayment = op.get();
                                long paymentAmount = 0;
                                long totalPaymentAmount = pp.getPaymentRegularTotalPaymentsAmount().longValue();
                                String paymentDayOfWeek = pp.getPaymentPeriodDayOfWeek();
                                char payPeriod = pp.getPaymentPeriod().trim().charAt(0);
                                boolean firstWeekOfMonth = false;
                                boolean secondWeekOfMonth = false;
                                boolean thirdWeekOfMonth = false;
                                boolean fourthWeekOfMonth = false;
                                int dow = Calendar.MONDAY;

                                if (lastSchedPayment != null) {
                                    Date payDate = lastSchedPayment.getDebitDate();
                                    if (payDate == null) {
                                        payDate = lastSchedPayment.getPaymentDate();
                                    }
                                    if (payDate != null) {

                                        paymentsStartDate.setTime(payDate);
                                        paymentAmount = lastSchedPayment.getScheduledAmount().longValue() * 100;  // the value is submitted as cents 

                                        if (payPeriod == 'M') {
                                            paymentsStartDate.add(Calendar.MONTH, 1);
                                        }
                                        if (payPeriod == 'W') {
                                            paymentsStartDate.add(Calendar.WEEK_OF_YEAR, 1);
                                        }
                                        if (payPeriod == '4') {
                                            paymentsStartDate.add(Calendar.WEEK_OF_YEAR, 4);
                                        }
                                        if (payPeriod == 'F') {
                                            paymentsStartDate.add(Calendar.WEEK_OF_YEAR, 2);
                                        }
                                        if (payPeriod == 'Q') {
                                            paymentsStartDate.add(Calendar.MONTH, 3);
                                        }
                                        if (payPeriod == 'H') {
                                            paymentsStartDate.add(Calendar.MONTH, 6);
                                        }
                                        if (payPeriod == 'Y') {
                                            paymentsStartDate.add(Calendar.MONTH, 12);
                                        }
                                        if (payPeriod == 'N') {
                                            int weekOfMonth = paymentsStartDate.get(Calendar.WEEK_OF_MONTH);
                                            int dayOfWeek = paymentsStartDate.get(Calendar.DAY_OF_WEEK);

                                            paymentsStartDate.add(Calendar.MONTH, 1);
                                            paymentsStartDate.set(Calendar.WEEK_OF_MONTH, weekOfMonth);
                                            paymentsStartDate.set(Calendar.DAY_OF_WEEK, dayOfWeek);

                                            if (weekOfMonth == 1) {
                                                firstWeekOfMonth = true;
                                            }
                                            if (weekOfMonth == 2) {
                                                secondWeekOfMonth = true;
                                            }
                                            if (weekOfMonth == 3) {
                                                thirdWeekOfMonth = true;
                                            }
                                            if (weekOfMonth == 4) {
                                                fourthWeekOfMonth = true;
                                            }

                                        }

                                        if (paymentDayOfWeek.contains("TUE")) {
                                            dow = Calendar.TUESDAY;
                                        }
                                        if (paymentDayOfWeek.contains("WED")) {
                                            dow = Calendar.WEDNESDAY;
                                        }
                                        if (paymentDayOfWeek.contains("THU")) {
                                            dow = Calendar.THURSDAY;
                                        }
                                        if (paymentDayOfWeek.contains("FRI")) {
                                            dow = Calendar.FRIDAY;
                                        }
                                        if (paymentsStartDate.before(paymentsStopDate)) {
                                            Future<PaymentGatewayResponse> pgr3 = null;
                                            LOGGER.log(Level.INFO, "Payment Bean - Update Schedule: Customer  - {0}, Start {1}, Finish {2}, period {3}, day of week {4}, day of month {5}, Amount-cents {6}, num pay {7}, tot pay amount {8}, 1st wom {9},2nd wom {10},3rd wom {11},4th wom {12},", new Object[]{cust.getUsername(), paymentsStartDate.getTime(), paymentsStopDate.getTime(), payPeriod, dow,
                                                Integer.parseInt(pp.getPaymentPeriodDayOfMonth()), paymentAmount, pp.getPaymentsRegularTotalNumberOfPayments(), totalPaymentAmount, firstWeekOfMonth, secondWeekOfMonth, thirdWeekOfMonth, fourthWeekOfMonth});
                                            if (paymentAmount > 200 && paymentAmount < 1000000) {
                                                pgr3 = createCRMPaymentSchedule(cust, paymentsStartDate.getTime(), paymentsStopDate.getTime(), payPeriod, dow, Integer.parseInt(pp.getPaymentPeriodDayOfMonth()), paymentAmount, pp.getPaymentsRegularTotalNumberOfPayments(), totalPaymentAmount, true, firstWeekOfMonth, secondWeekOfMonth, thirdWeekOfMonth, fourthWeekOfMonth, "System - update payment schedule", sessionId, digitalKey, futureMap, this, true);
                                                try {
                                                    pgr = pgr3.get();
                                                } catch (InterruptedException | ExecutionException ex) {
                                                    Logger.getLogger(PaymentBean.class.getName()).log(Level.SEVERE, "updateCustomerPaymentSchedule", ex);
                                                    pgr = new PaymentGatewayResponse(false, null, "", "-1", "The customer or digital key is empty!");
                                                    //futureMap.processUpdateCustomerSchedule(sessionId, pgr);
                                                    return new AsyncResult<>(pgr);
                                                }
                                            } else {
                                                LOGGER.log(Level.SEVERE, "Payment Bean - Update Schedule Failed:Payment Amount outside < $2 or greater than $10000 Customer  - {0}, Start {1}, Finish {2}, period {3}, day of week {4}, day of month {5}, Amount-cents {6}, num pay {7}, tot pay amount {8}, 1st wom {9},2nd wom {10},3rd wom {11},4th wom {12},", new Object[]{cust.getUsername(), paymentsStartDate.getTime(), paymentsStopDate.getTime(), payPeriod, dow, Integer.parseInt(pp.getPaymentPeriodDayOfMonth()), paymentAmount, pp.getPaymentsRegularTotalNumberOfPayments(), totalPaymentAmount, firstWeekOfMonth, secondWeekOfMonth, thirdWeekOfMonth, fourthWeekOfMonth});
                                            }
                                        } else {

                                            LOGGER.log(Level.SEVERE, "Payment Bean - Update Schedule Failed:The last scheduled payment Date and debit date were NULL  - {0}, Start {1}, Finish {2}, period {3}, day of week {4}, day of month {5}, Amount-cents {6}, num pay {7}, tot pay amount {8}, 1st wom {9},2nd wom {10},3rd wom {11},4th wom {12},", new Object[]{cust.getUsername(), paymentsStartDate.getTime(), paymentsStopDate.getTime(), payPeriod, dow, Integer.parseInt(pp.getPaymentPeriodDayOfMonth()), paymentAmount, pp.getPaymentsRegularTotalNumberOfPayments(), totalPaymentAmount, firstWeekOfMonth, secondWeekOfMonth, thirdWeekOfMonth, fourthWeekOfMonth});

                                        }
                                    }
                                } else {

                                    LOGGER.log(Level.SEVERE, "Payment Bean - Update Schedule Failed:The last scheduled payment was NULL  - {0}, Start {1}, Finish {2}, period {3}, day of week {4}, day of month {5}, Amount-cents {6}, num pay {7}, tot pay amount {8}, 1st wom {9},2nd wom {10},3rd wom {11},4th wom {12},", new Object[]{cust.getUsername(), paymentsStartDate.getTime(), paymentsStopDate.getTime(), payPeriod, dow, Integer.parseInt(pp.getPaymentPeriodDayOfMonth()), paymentAmount, pp.getPaymentsRegularTotalNumberOfPayments(), totalPaymentAmount, firstWeekOfMonth, secondWeekOfMonth, thirdWeekOfMonth, fourthWeekOfMonth});

                                }
                            } else {
                                LOGGER.log(Level.INFO, "Payment Bean - Update Schedule Failed:The customer has no scheduled payments. Create a schedule for this customer manually   - {0}", new Object[]{cust.getUsername()});
                            }
                        }
                        //find the last scheduled payment from array

                    }

                } else {
                    pgr = new PaymentGatewayResponse(false, null, "The Customers details were not retrieved from the payment gateway due to an error.", customerdetails.getError().toString(), customerdetails.getErrorMessage().getValue());
                    LOGGER.log(Level.WARNING, "Get Customer Details Response: Error - {0}", customerdetails.getErrorMessage().getValue());
                    updatePaymentParameters(cust, null, customerdetails.getError());

                }
            } else {
                pgr = new PaymentGatewayResponse(false, null, "The Customers details were not retrieved from the payment gateway due to an error.", "NULL Pointer", "-1");
                LOGGER.log(Level.WARNING, "Get Customer Details Response: Error - NULL");
                updatePaymentParameters(cust, null, -1);

            }
            futureMap.processUpdateCustomerSchedule(sessionId, pgr);
            return new AsyncResult<>(pgr);
        } catch (Exception exception) {
            PaymentGatewayResponse pgr = new PaymentGatewayResponse(false, null, "", "-1", exception.getMessage());
            //futureMap.processUpdateCustomerSchedule(sessionId, pgr);
            return new AsyncResult<>(pgr);
        }
    }

    @TransactionAttribute(REQUIRES_NEW)
    private void updatePaymentParameters(Customers cust, CustomerDetails custDetails,
            int errorCode
    ) {
        PaymentParameters pp = cust.getPaymentParametersId();

        if (pp == null) {

            LOGGER.log(Level.SEVERE, "PaymentBean processGetCustomerDetails. Payment Parameters Object is NULL for customer {0}. Creating default parameters.", new Object[]{cust.getUsername()});
            return;
        }
        if (custDetails == null) {
            if (errorCode != 0) {
                if (errorCode == 201) { //customer could not be found
                    pp.setStatusCode("D");
                    pp.setStatusDescription("Inactive");
                    pp.setLastUpdatedFromPaymentGateway(new Date());
                }
            } else {
                LOGGER.log(Level.SEVERE, "PaymentBean processGetCustomerDetails. CustomerDetails Object is NULL but error code = 0 ( success) for customer {0}.This shouldn't ever happen.", new Object[]{cust.getUsername()});
            }

        } else {
            Payments p1 = null;
            try {
                p1 = paymentsFacade.findLastSuccessfulScheduledPayment(cust);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "PaymentBean processGetCustomerDetails. findLastSuccessfulScheduledPayment for customer {0}. {1}", new Object[]{cust.getUsername(), e});
            }
            Payments p2 = null;
            try {
                p2 = paymentsFacade.findNextScheduledPayment(cust);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "PaymentBean processGetCustomerDetails. findNextScheduledPayment for customer {0}. {1}", new Object[]{cust.getUsername(), e});
            }
            pp.setLastUpdatedFromPaymentGateway(new Date());
            pp.setLastSuccessfulScheduledPayment(p1);
            pp.setNextScheduledPayment(p2);
            pp.setAddressLine1(custDetails.getAddressLine1().getValue());
            pp.setAddressLine2(custDetails.getAddressLine2().getValue());
            pp.setAddressPostCode(custDetails.getAddressPostCode().getValue());
            pp.setAddressState(custDetails.getAddressState().getValue());
            pp.setAddressSuburb(custDetails.getAddressSuburb().getValue());
            pp.setContractStartDate(custDetails.getContractStartDate().getValue().toGregorianCalendar().getTime());
            pp.setCustomerFirstName(custDetails.getCustomerFirstName().getValue());
            pp.setCustomerName(custDetails.getCustomerName().getValue());
            pp.setEmail(custDetails.getEmail().getValue());
            pp.setEzidebitCustomerID(custDetails.getEzidebitCustomerID().getValue());

            pp.setMobilePhoneNumber(custDetails.getMobilePhone().getValue());
            pp.setPaymentGatewayName("EZIDEBIT");
            pp.setPaymentMethod(custDetails.getPaymentMethod().getValue());
            //pp.setPaymentPeriod(custDetails.getPaymentPeriod().getValue());
            //pp.setPaymentPeriodDayOfMonth(custDetails.getPaymentPeriodDayOfMonth().getValue());
            //pp.setPaymentPeriodDayOfWeek(custDetails.getPaymentPeriodDayOfWeek().getValue());

            pp.setSmsExpiredCard(custDetails.getSmsExpiredCard().getValue());
            pp.setSmsFailedNotification(custDetails.getSmsFailedNotification().getValue());
            pp.setSmsPaymentReminder(custDetails.getSmsPaymentReminder().getValue());
            pp.setStatusCode(custDetails.getStatusCode().getValue());
            pp.setStatusDescription(custDetails.getStatusDescription().getValue());
            pp.setTotalPaymentsFailed(custDetails.getTotalPaymentsFailed());
            pp.setTotalPaymentsFailedAmount(new BigDecimal(custDetails.getTotalPaymentsFailed()));
            pp.setTotalPaymentsSuccessful(custDetails.getTotalPaymentsSuccessful());
            pp.setTotalPaymentsSuccessfulAmount(new BigDecimal(custDetails.getTotalPaymentsSuccessfulAmount()));
            pp.setYourGeneralReference(custDetails.getYourGeneralReference().getValue());
            pp.setYourSystemReference(custDetails.getYourSystemReference().getValue());
        }
        ejbPaymentParametersFacade.edit(pp);
        //ejbPaymentParametersFacade.pushChangesToDBImmediatleyInsteadOfAtTxCommit();
        cust.setPaymentParametersId(pp);
        customersFacade.edit(cust);
        //customersFacade.pushChangesToDBImmediatleyInsteadOfAtTxCommit();
        LOGGER.log(Level.INFO, "Payment Bean processGetCustomerDetails. Payment Parameters have been updated for {0}.", new Object[]{cust.getUsername()});
    }

    @Asynchronous
    public Future<PaymentGatewayResponse> getAllPaymentsBySystemSinceDate(Date fromDate, Date endDate,
            boolean useSettlementDate, String digitalKey,
            String sessionId
    ) {
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
        ArrayOfPayment result;
        PaymentGatewayResponse pgr = new PaymentGatewayResponse(false, null, "", "-1", "An unhandled error occurred!");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String fromDateString = ""; // The exact date on which the payment that you wish to move is scheduled to be deducted from your Customer's bank account or credit card.
        try {
            if (fromDate != null) {
                fromDateString = sdf.format(fromDate);
            }
            String toDate = sdf.format(endDate);

            String dateField = "PAYMENT";
            if (useSettlementDate == true) {
                dateField = "SETTLEMENT";
            }
            LOGGER.log(Level.INFO, "getAllPaymentsBySystemSinceDate - Calling ezidebit WS, From Date {0}, To Date {1}, report Type {2}", new Object[]{fromDateString, toDate, dateField});

            EziResponseOfArrayOfPaymentTHgMB7OL eziResponse = getWs().getPayments(digitalKey, "ALL", "ALL", "ALL", "", fromDateString, toDate, dateField, "", "");
            if (eziResponse.getError() == 0) {// any errors will be a non zero value
                LOGGER.log(Level.INFO, "getAllPaymentsBySystemSinceDate Response:OK - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

                result = eziResponse.getData().getValue();
                if (result.getPayment() != null) {
                    pgr = new PaymentGatewayResponse(true, result, "The Customers details were retrieved from the payment gateway", "0", "");
                    LOGGER.log(Level.INFO, "getAllPaymentsBySystemSinceDate Response: OK {0}, No of Payments in List = {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue().getPayment().size()});

                }

            } else {
                LOGGER.log(Level.WARNING, "getAllPaymentsBySystemSinceDate Response: Error - {0}, ", eziResponse.getErrorMessage().getValue());
                pgr = new PaymentGatewayResponse(false, null, "getAllPaymentsBySystemSinceDate FAILED due to an error.", eziResponse.getError().toString(), eziResponse.getErrorMessage().getValue());

            }
        } catch (Exception e) {
            pgr = new PaymentGatewayResponse(false, null, "getAllPaymentsBySystemSinceDate FAILED due to an error.", "-1", e.getMessage());
        }
        if (useSettlementDate == true) {
            futureMap.processSettlementReport(sessionId, pgr);
        } else {
            futureMap.processPaymentReport(sessionId, pgr);
        }

        return new AsyncResult<>(pgr);
    }

    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Future<PaymentGatewayResponse> addPayment(Customers cust, Date debitDate,
            Long paymentAmountInCents, Payments payment,
            String loggedInUser, String digitalKey,
            String sessionId
    ) {
        if (payment == null) {
            PaymentGatewayResponse pgr = new PaymentGatewayResponse(false, payment, "UNKNOWN", "-1", "Payment Object is NULL");
            futureMap.processAddPaymentResult(sessionId, pgr);
            return new AsyncResult<>(pgr);
        }
         if (payment.getId() <=0) {
            PaymentGatewayResponse pgr = new PaymentGatewayResponse(false, payment, "PAYMENT ID not commited to DB", payment.getId().toString(), "Payment ID is <=0. This is not  valid primary key. The payment cant be added to payment gateway!");
            futureMap.processAddPaymentResult(sessionId, pgr);
            return new AsyncResult<>(pgr);
        }
        // paymentReference Max 50 chars. It can be search with with a wildcard in other methods. Use invoice number or other payment identifier
        LOGGER.log(Level.INFO, "running asychronous task addPayment Customer {0}, debitDate {1}, paymentAmountInCents {2}", new Object[]{cust, debitDate, paymentAmountInCents});
        String paymentReference = payment.getId().toString();
        payment.setPaymentReference(paymentReference);
        PaymentGatewayResponse pgr = new PaymentGatewayResponse(false, payment, paymentReference, "-1", "An unhandled error occurred!");

        String eziDebitCustomerId = ""; // use our reference instead. THis must be an empty string.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            String debitDateString = sdf.format(debitDate);
            String ourSystemCustomerReference = cust.getId().toString();

            if (paymentReference.length() > 50) {
                paymentReference = paymentReference.substring(0, 50);
                LOGGER.log(Level.WARNING, "addPayment paymentReference is greater than the allowed 50 characters. Truncating! to 50 chars");
            }
            if (loggedInUser.length() > 50) {
                loggedInUser = loggedInUser.substring(0, 50);
                LOGGER.log(Level.WARNING, "addPayment loggedInUser is greater than the allowed 50 characters. Truncating! to 50 chars");
            }
            EziResponseOfstring eziResponse;
            try {
                eziResponse = getWs().addPayment(digitalKey, eziDebitCustomerId, ourSystemCustomerReference, debitDateString, paymentAmountInCents, paymentReference, loggedInUser);
            } catch (Exception e) {
                String eMessage = "";
                if (e.getMessage().contains("Your update could not be processed at this time")) {
                    eMessage = "The Gateway is busy and cannot process your add payment request at this time.";
                    LOGGER.log(Level.WARNING, eMessage);
                } else if (e.getMessage().contains("This customer already has 2 payments on this date")) {
                    eMessage = "Add payment DENIED - This customer already has two payments on this date. This is an ezidebit payment gateway restriction";
                    LOGGER.log(Level.WARNING, eMessage);
                } else {
                    eMessage = "Exception:" + paymentReference + ":";
                    if (e.getMessage() != null) {
                        eMessage += e.getMessage();
                    }
                    LOGGER.log(Level.WARNING, "addPayment Method FAILED: ", eMessage);
                }
                pgr = new PaymentGatewayResponse(false, payment, paymentReference, "-1", eMessage);
                futureMap.processAddPaymentResult(sessionId, pgr);
                return new AsyncResult<>(pgr);
            }
            if (eziResponse.getError() == 0) {// any errors will be a non zero value
                LOGGER.log(Level.INFO, "addPayment Response: Successful. Reference:{0}, Customer: {2}, Return Value: - {1}", new Object[]{paymentReference, eziResponse.getData().getValue(), cust.getUsername()});

                if (eziResponse.getData().getValue().compareTo("S") == 0) {
                    pgr = new PaymentGatewayResponse(true, payment, paymentReference, "0", "The payment was added to the payment gateway successfully");
                    String auditDetails = "Payment Added - Debit Date:" + debitDateString + ", Amount (cents): " + paymentAmountInCents.toString() + ", Payment Ref:" + paymentReference;
                    String changedFrom = "non-existent";
                    String changedTo = "Ref:" + paymentReference;
                    auditLogFacade.audit(customersFacade.findCustomerByUsername(loggedInUser), cust, "addPayment", auditDetails, changedFrom, changedTo);

                } else {

                    LOGGER.log(Level.WARNING, "addPayment Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
                    String errorMessage = "ERROR:" + paymentReference + ":";
                    String errorCode = "-1";
                    if (eziResponse.getErrorMessage() != null) {
                        errorMessage += eziResponse.getErrorMessage();
                        errorCode = eziResponse.getError().toString();
                    }
                    pgr = new PaymentGatewayResponse(false, payment, paymentReference, errorCode, errorMessage);

                }
            } else {
                LOGGER.log(Level.WARNING, "addPayment Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
                String errorMessage = "ERROR:" + paymentReference + ":";
                String errorCode = "-1";
                if (eziResponse.getErrorMessage() != null) {
                    errorMessage += eziResponse.getErrorMessage();
                    errorCode = eziResponse.getError().toString();
                }
                pgr = new PaymentGatewayResponse(false, payment, paymentReference, errorCode, errorMessage);

            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "addPayment Response: Error:", e);
        }
        futureMap.processAddPaymentResult(sessionId, pgr);
        return new AsyncResult<>(pgr);
    }

    /*  @Asynchronous
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
                LOGGER.log(Level.WARNING, "createSchedule A value must be provided for dayOfWeek \n" + " when the\n" + "SchedulePeriodType is in\n" + "W,F,4,N");
                return new AsyncResult<>(false);
            }
        }
        if (schedulePeriodType == 'M') {
            if (dayOfMonth < 1 || dayOfMonth > 31) {
                LOGGER.log(Level.WARNING, "createSchedule: A value must be provided for dayOfMonth (1..31 )\n" + " when the\n" + "SchedulePeriodType is in\n" + "M");
                return new AsyncResult<>(false);
            }
        } else {
            dayOfMonth = 0;
        }
        String schedulePeriodTypeString = "";
        schedulePeriodTypeString = schedulePeriodTypeString + schedulePeriodType;

        if (loggedInUser.length() > 50) {
            loggedInUser = loggedInUser.substring(0, 50);
            LOGGER.log(Level.WARNING, "createSchedule loggedInUser is greater than the allowed 50 characters. Truncating! to 50 chars");
        }
        if (schedulePeriodType == 'W' || schedulePeriodType == 'F' || schedulePeriodType == 'M' || schedulePeriodType == '4' || schedulePeriodType == 'N' || schedulePeriodType == 'Q' || schedulePeriodType == 'H' || schedulePeriodType == 'Y') {

            EziResponseOfstring eziResponse = getWs().createSchedule(digitalKey, eziDebitCustomerId, ourSystemCustomerReference, scheduleStartDateString, schedulePeriodTypeString, dayOfWeek, dayOfMonth, firstWeekOfMonthString, secondWeekOfMonthString, thirdWeekOfMonthString, fourthWeekOfMonthString, paymentAmountInCents, limitToNumberOfPayments, limitToTotalAmountInCent, keepManualPaymentsString, loggedInUser);
            LOGGER.log(Level.INFO, "createSchedule Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
            if (eziResponse.getError() == 0) {// any errors will be a non zero value

                if (eziResponse.getData().getValue().compareTo("S") == 0) {
                    result = true;
                    String auditDetails = "Created scheduled for  :" + cust.getUsername() + ".  Keep Manual Payments: " + keepManualPayments + ", start:" + scheduleStartDateString + ",Period Type:" + schedulePeriodTypeString + ",Amount:" + paymentAmountInCents;
                    String changedFrom = "From Date:" + cust.getPaymentParametersId().getPaymentPeriod();
                    String changedTo = "New Schedule";
                    auditLogFacade.audit(customersFacade.findCustomerByUsername(loggedInUser), cust, "createSchedule", auditDetails, changedFrom, changedTo);

                } else {
                    LOGGER.log(Level.WARNING, "createSchedule Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
                }
            } else {
                LOGGER.log(Level.WARNING, "createSchedule Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

            }
        } else {
            LOGGER.log(Level.WARNING, "createSchedule : schedulePeriodType Possible values are:\\n'W' - Weekly\\n'F' - Fortnightly\\n'M' - Monthly\\n'4' - 4 Weekly\\n'N' - Weekday in month (e.g.\\nMonday in the third week of\\nevery month)\\n'Q' - Quarterly\\n'H' - Half Yearly (6 Monthly)\\n'Y' - Yearly\\nThe frequency is applied to the\\npayment scheduled beginning\\nfrom the date defined in\\nScheduledStartDate\\n Incorect value that was submitted = ", schedulePeriodType);

        }

        return new AsyncResult<>(result);
    }*/
    @Asynchronous
    public Future<PaymentGatewayResponse> changeScheduledAmount(Customers cust, Date changeFromDate,
            Long newPaymentAmountInCents, int changeFromPaymentNumber, boolean applyToAllFuturePayments, boolean keepManualPayments, String loggedInUser,
            String digitalKey, String sessionId
    ) {
        // Only scheduled payments with a status of 'W' can have their scheduled debit amounts changed;
        // This method will only change the debit amounts of scheduled payments - the debit dates will still remain the same.
        PaymentGatewayResponse pgr = new PaymentGatewayResponse(false, cust, "", "-1", "An unhandled error occurred!");

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
            LOGGER.log(Level.WARNING, "changeScheduledAmount loggedInUser is greater than the allowed 50 characters. Truncating! to 50 chars");
        }
        EziResponseOfstring eziResponse = getWs().changeScheduledAmount(digitalKey, eziDebitCustomerId, ourSystemCustomerReference, changeFromPaymentNumber, changeFromDateString, newPaymentAmountInCents, applyToAllFuturePaymentsString, keepManualPaymentsString, loggedInUser);
        LOGGER.log(Level.INFO, "changeScheduledAmount Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value

            if (eziResponse.getData().getValue().compareTo("S") == 0) {
                result = true;
                String auditDetails = "Changed the scheduled Amount for  :" + cust.getUsername() + " from  " + changeFromDateString + " to Amount in cents : " + newPaymentAmountInCents;
                String changedFrom = "From Date:" + changeFromDateString;
                String changedTo = "New Amount (cents):" + newPaymentAmountInCents;
                auditLogFacade.audit(customersFacade.findCustomerByUsername(loggedInUser), cust, "addCustomer", auditDetails, changedFrom, changedTo);
                pgr = new PaymentGatewayResponse(result, cust, auditDetails, "0", "");
            } else {
                LOGGER.log(Level.WARNING, "changeScheduledAmount Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
                pgr = new PaymentGatewayResponse(result, cust, "", eziResponse.getData().getValue(), eziResponse.getErrorMessage().getValue());
            }
        } else {
            LOGGER.log(Level.WARNING, "changeScheduledAmount Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
            pgr = new PaymentGatewayResponse(result, cust, "", eziResponse.getData().getValue(), eziResponse.getErrorMessage().getValue());
        }

        futureMap.processChangeScheduledAmount(sessionId, pgr);
        return new AsyncResult<>(pgr);
    }

    @Asynchronous
    public Future<PaymentGatewayResponse> changeScheduledDate(Customers cust, Date changeFromDate,
            Date changeToDate, String paymentReference,
            boolean keepManualPayments, String loggedInUser,
            String digitalKey, String sessionId
    ) {
        // PaymentReference - If you used a specific PaymentReference when adding a payment using the AddPayment Method, then you can use that value here to exactly identify that payment within the Customer's schedule.
        // NB - You must provide a value for either ChangeFromDate or PaymentReference to identify the payment.
        PaymentGatewayResponse pgr = new PaymentGatewayResponse(false, null, "", "-1", "An unhandled error occurred!");
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
            LOGGER.log(Level.WARNING, "changeScheduledDate loggedInUser is greater than the allowed 50 characters. Truncating! to 50 chars");
        }
        EziResponseOfstring eziResponse = getWs().changeScheduledDate(digitalKey, eziDebitCustomerId, ourSystemCustomerReference, changeFromDateString, paymentReference, changeToDateString, keepManualPaymentsString, loggedInUser);
        LOGGER.log(Level.INFO, "changeScheduledDate Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value

            if (eziResponse.getData().getValue().compareTo("S") == 0) {
                result = true;
                String auditDetails = "Changed the scheduled Date for  :" + cust.getUsername() + " from  " + changeFromDateString + " to Date: " + changeToDateString + ", Ref:" + paymentReference + ", Keep Manual Payments =" + keepManualPaymentsString;
                String changedFrom = "From Date:" + changeFromDateString;
                String changedTo = "New Date:" + changeToDateString;
                auditLogFacade.audit(customersFacade.findCustomerByUsername(loggedInUser), cust, "changeScheduledDate", auditDetails, changedFrom, changedTo);
                pgr = new PaymentGatewayResponse(result, cust, auditDetails, "0", "");
            } else {
                LOGGER.log(Level.WARNING, "changeScheduledDate Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
                pgr = new PaymentGatewayResponse(result, cust, "", eziResponse.getData().getValue(), eziResponse.getErrorMessage().getValue());
            }
        } else {
            LOGGER.log(Level.WARNING, "changeScheduledDate Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
            pgr = new PaymentGatewayResponse(result, cust, "", eziResponse.getData().getValue(), eziResponse.getErrorMessage().getValue());
        }
        futureMap.processChangeScheduledDate(sessionId, pgr);
        return new AsyncResult<>(pgr);
    }

    @Asynchronous
    public Future<PaymentGatewayResponse> isBsbValid(String bsb, String digitalKey,
            String sessionId
    ) {
        PaymentGatewayResponse pgr = new PaymentGatewayResponse(false, null, "", "-1", "An unhandled error occurred!");
        //  Description
        //  check that the BSB is valid
        boolean result = false;

        EziResponseOfstring eziResponse = getWs().isBsbValid(digitalKey, bsb);
        LOGGER.log(Level.INFO, "isBsbValid Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value
            String valid = eziResponse.getData().getValue();
            if (valid.compareTo("YES") == 0) {
                pgr = new PaymentGatewayResponse(true, null, "BSB is VALID!", "0", "");

            }

        } else {
            LOGGER.log(Level.WARNING, "isBsbValid Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
            pgr = new PaymentGatewayResponse(result, null, "", eziResponse.getData().getValue(), eziResponse.getErrorMessage().getValue());
        }
        futureMap.processIsBsbValid(sessionId, pgr);
        return new AsyncResult<>(pgr);
    }

    @Asynchronous
    public Future<PaymentGatewayResponse> isSystemLocked(String digitalKey, String sessionId
    ) {
        PaymentGatewayResponse pgr = new PaymentGatewayResponse(false, null, "", "-1", "An unhandled error occurred!");
        //  Description
        //  check that the BSB is valid
        boolean result = false;

        EziResponseOfstring eziResponse = getWs().isSystemLocked(digitalKey);
        LOGGER.log(Level.INFO, "isSystemLocked Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value
            String valid = eziResponse.getData().getValue();
            if (valid.compareTo("YES") == 0) {
                pgr = new PaymentGatewayResponse(true, null, "The System Is Locked!", "0", "");
            } else {
                pgr = new PaymentGatewayResponse(true, null, "The System Is Unlocked!", "0", "");
            }

        } else {
            LOGGER.log(Level.WARNING, "isSystemLocked Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
            pgr = new PaymentGatewayResponse(result, null, "", eziResponse.getData().getValue(), eziResponse.getErrorMessage().getValue());
        }
        futureMap.processIsSystemLocked(sessionId, pgr);
        return new AsyncResult<>(pgr);
    }

    @Asynchronous
    public Future<PaymentGatewayResponse> getPaymentExchangeVersion(String sessionId
    ) {
        PaymentGatewayResponse pgr = new PaymentGatewayResponse(false, null, "", "-1", "An unhandled error occurred!");
        //  Description
        //  
        //  This method returns the version of our web services and API that you are connecting to.
        //  This can be used as a check to ensure that you're connecting to the web services that
        //  you expect to, based on the API document that you have.

        String result = "ERROR Getting version";

        EziResponseOfstring eziResponse = getWs().paymentExchangeVersion();
        LOGGER.log(Level.INFO, "getPaymentExchangeVersion Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value
            result = eziResponse.getData().getValue();
            pgr = new PaymentGatewayResponse(true, result, result, "0", "");
        } else {
            LOGGER.log(Level.WARNING, "getPaymentExchangeVersion Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
            pgr = new PaymentGatewayResponse(false, result, "", eziResponse.getData().getValue(), eziResponse.getErrorMessage().getValue());
        }
        futureMap.processGetPaymentExchangeVersion(sessionId, pgr);
        return new AsyncResult<>(pgr);
    }

    @TransactionAttribute(TransactionAttributeType.NEVER)
    @Asynchronous
    public Future<PaymentGatewayResponse> sendAsynchEmailWithPGR(String to, String ccAddress,
            String from, String emailSubject,
            String message, String theAttachedfileName,
            Properties serverProperties, boolean debug, String sessionId
    ) {
        SendHTMLEmailWithFileAttached emailAgent = new SendHTMLEmailWithFileAttached();
        PaymentGatewayResponse pgr = new PaymentGatewayResponse(false, null, "", "-1", "An unhandled error occurred!");
        try {
            LOGGER.log(Level.INFO, "sending AsynchEmail TO: {0}, CC - {1}, From:{2}, Subject:{3}", new Object[]{to, ccAddress, from, emailSubject});
            emailAgent.send(to, ccAddress, from, emailSubject, message, theAttachedfileName, serverProperties, debug);
            LOGGER.log(Level.INFO, "sent AsynchEmail TO: {0}, CC - {1}, From:{2}, Subject:{3}", new Object[]{to, ccAddress, from, emailSubject});
            pgr = new PaymentGatewayResponse(true, null, "OK", "0", "Email sent successfully");
        } catch (Exception e) {
            String error = "Email Send Failed :" + e.getMessage();
            futureMap.processEmailAlert(sessionId, pgr);
            return new AsyncResult<>(new PaymentGatewayResponse(false, null, "", "-1", error));
        }
        futureMap.processEmailAlert(sessionId, pgr);
        return new AsyncResult<>(pgr);
    }

    @Asynchronous
    public Future<Boolean> sendAsynchEmail(String to, String ccAddress,
            String from, String emailSubject,
            String message, String theAttachedfileName,
            Properties serverProperties, boolean debug
    ) {
        SendHTMLEmailWithFileAttached emailAgent = new SendHTMLEmailWithFileAttached();
        try {
            emailAgent.send(to, ccAddress, from, emailSubject, message, theAttachedfileName, serverProperties, debug);
            LOGGER.log(Level.INFO, "sendAsynchEmail TO: {0}, CC - {1}, From:{2}, Subject:{3}", new Object[]{to, ccAddress, from, emailSubject});
        } catch (Exception e) {
            String error = "Email Send Failed :" + e.getMessage();
            return new AsyncResult<>(false);
        }
        return new AsyncResult<>(true);
    }

    public Properties emailServerProperties() {
        Properties props = new Properties();

        props.put("mail.smtp.host", configMapFacade.getConfig("mail.smtp.host"));
        props.put("mail.smtp.auth", configMapFacade.getConfig("mail.smtp.auth"));
        props.put("mail.debug", configMapFacade.getConfig("mail.debug"));
        props.put("mail.smtp.ssl.enable", configMapFacade.getConfig("mail.smtp.ssl.enable"));
        props.put("mail.smtp.port", configMapFacade.getConfig("mail.smtp.port"));
        props.put("mail.smtp.socketFactory.port", configMapFacade.getConfig("mail.smtp.socketFactory.port"));
        props.put("mail.smtp.socketFactory.class", configMapFacade.getConfig("mail.smtp.socketFactory.class"));
        props.put("mail.smtp.socketFactory.fallback", configMapFacade.getConfig("mail.smtp.socketFactory.fallback"));
        props.put("mail.smtp.ssluser", configMapFacade.getConfig("mail.smtp.ssluser"));
        props.put("mail.smtp.sslpass", configMapFacade.getConfig("mail.smtp.sslpass"));
        props.put("mail.smtp.headerimage.url", configMapFacade.getConfig("mail.smtp.headerimage.url"));
        props.put("mail.smtp.headerimage.cid", configMapFacade.getConfig("mail.smtp.headerimage.cid"));
        return props;

    }
}
