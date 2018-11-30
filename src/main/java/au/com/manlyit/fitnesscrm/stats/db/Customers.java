/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.db;

import au.com.manlyit.fitnesscrm.stats.classes.util.BaseEntity;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.eclipse.persistence.annotations.Cache;
import org.eclipse.persistence.annotations.CacheCoordinationType;
import org.eclipse.persistence.annotations.CacheType;
import org.eclipse.persistence.annotations.DatabaseChangeNotificationType;
import org.eclipse.persistence.config.CacheIsolationType;

/**
 *
 * @author david
 */
@Cache(
        type = CacheType.SOFT_WEAK,// Cache everything in memory as this object is mostly read only and will be called hundreds of time on each page.
        size = 64000, // Use 64,000 as the initial cache size.
        //expiry = 36000000, // 10 minutes // by default it never expires which is what we want for this table
        coordinationType = CacheCoordinationType.INVALIDATE_CHANGED_OBJECTS,
        databaseChangeNotificationType = DatabaseChangeNotificationType.INVALIDATE,
        isolation = CacheIsolationType.SHARED
)
@Entity
@Table(name = "customers")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Customers.findAll", query = "SELECT c FROM Customers c"),
    @NamedQuery(name = "Customers.findById", query = "SELECT c FROM Customers c WHERE c.id = :id"),
    @NamedQuery(name = "Customers.findByGender", query = "SELECT c FROM Customers c WHERE c.gender = :gender"),
    @NamedQuery(name = "Customers.findByFirstname", query = "SELECT c FROM Customers c WHERE c.firstname = :firstname"),
    @NamedQuery(name = "Customers.findByLastname", query = "SELECT c FROM Customers c WHERE c.lastname = :lastname"),
    @NamedQuery(name = "Customers.findByDob", query = "SELECT c FROM Customers c WHERE c.dob = :dob"),
    @NamedQuery(name = "Customers.findByEmailAddress", query = "SELECT c FROM Customers c WHERE c.emailAddress = :emailAddress"),
    @NamedQuery(name = "Customers.findByUsername", query = "SELECT c FROM Customers c WHERE c.username = :username"),
    @NamedQuery(name = "Customers.findByStreetAddress", query = "SELECT c FROM Customers c WHERE c.streetAddress = :streetAddress"),
    @NamedQuery(name = "Customers.findBySuburb", query = "SELECT c FROM Customers c WHERE c.suburb = :suburb"),
    @NamedQuery(name = "Customers.findByPostcode", query = "SELECT c FROM Customers c WHERE c.postcode = :postcode"),
    @NamedQuery(name = "Customers.findByCity", query = "SELECT c FROM Customers c WHERE c.city = :city"),
    @NamedQuery(name = "Customers.findByAddrState", query = "SELECT c FROM Customers c WHERE c.addrState = :addrState"),
    @NamedQuery(name = "Customers.findByCountryId", query = "SELECT c FROM Customers c WHERE c.countryId = :countryId"),
    @NamedQuery(name = "Customers.findByTelephone", query = "SELECT c FROM Customers c WHERE c.telephone = :telephone"),
    @NamedQuery(name = "Customers.findByFax", query = "SELECT c FROM Customers c WHERE c.fax = :fax"),
    @NamedQuery(name = "Customers.findByPassword", query = "SELECT c FROM Customers c WHERE c.password = :password"),
    @NamedQuery(name = "Customers.findByNewsletter", query = "SELECT c FROM Customers c WHERE c.newsletter = :newsletter"),
    @NamedQuery(name = "Customers.findByReferredby", query = "SELECT c FROM Customers c WHERE c.referredby = :referredby"),
    @NamedQuery(name = "Customers.findByFacebookId", query = "SELECT c FROM Customers c WHERE c.facebookId = :facebookId"),
    @NamedQuery(name = "Customers.findByGoogleId", query = "SELECT c FROM Customers c WHERE c.googleId = :googleId")})
public class Customers implements BaseEntity, Serializable {

    @Basic(optional = false)
    @NotNull
    @Column(name = "gender")
    private Character gender;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 127)
    @Column(name = "firstname")
    private String firstname;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 127)
    @Column(name = "lastname")
    private String lastname;
    @Basic(optional = false)
    @NotNull
    @Column(name = "dob")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dob;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "email_address")
    private String emailAddress;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 96)
    @Column(name = "username")
    private String username;
    @Size(max = 127)
    @Column(name = "street_address")
    private String streetAddress;
    @Size(max = 127)
    @Column(name = "suburb")
    private String suburb;
    @Size(max = 10)
    @Column(name = "postcode")
    private String postcode;
    @Size(max = 127)
    @Column(name = "city")
    private String city;
    @Size(max = 32)
    @Column(name = "addr_state")
    private String addrState;
    @Size(max = 63)
    @Column(name = "telephone")
    private String telephone;
    // @Pattern(regexp="^\\(?(\\d{3})\\)?[- ]?(\\d{3})[- ]?(\\d{4})$", message="Invalid phone/fax format, should be as xxx-xxx-xxxx")//if the field contains phone or fax number consider using this annotation to enforce field validation
    @Size(max = 63)
    @Column(name = "fax")
    private String fax;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "password")
    private String password;
    @Size(max = 45)
    @Column(name = "facebookId")
    private String facebookId;
    @Size(max = 45)
    @Column(name = "googleId")
    private String googleId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "emergency_contact_name")
    private String emergencyContactName;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 45)
    @Column(name = "emergency_contact_phone")
    private String emergencyContactPhone;
    @OneToMany(mappedBy = "customer")
    private Collection<NotificationsLog> notificationsLogCollection;

    @OneToMany(mappedBy = "internalContractorId")
    private Collection<Suppliers> suppliersCollection;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "customerId")
    private Collection<QuestionnaireMap> questionnaireMapCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "customerId")
    private Collection<SessionBookings> sessionBookingsCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "customer")
    private Collection<Tickets> ticketsCollection;
    @Column(name = "terms_conditions_accepted")
    private Boolean termsConditionsAccepted;
    @Column(name = "last_login_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastLoginTime;
    @Column(name = "login_attempts")
    private Integer loginAttempts;
    @Column(name = "must_reset_password")
    private Boolean mustResetPassword;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "trainerId")
    private Collection<SessionTimetable> sessionTimetableCollection;
   
    
    
    @OneToMany(mappedBy = "changedBy")
    private Collection<AuditLog> auditLogCollection;
    @OneToMany(mappedBy = "customer")
    private Collection<AuditLog> auditLogCollection1;
    @JoinColumn(name = "profile_image", referencedColumnName = "id")
    @OneToOne
    private CustomerImages profileImage;
    
    // JoinColumn indicates this tabl;e will have the foreign key and be the owner of the relationship.THis entity should be persisted to invoke the cascade persist on its payment parameters. 
    @JoinColumn(name = "payment_parameters_id", referencedColumnName = "id")
    
    @OneToOne(cascade = CascadeType.ALL, optional = false)
    private PaymentParameters paymentParametersId;
    
    @OneToMany(mappedBy = "customerName")
    private Collection<Payments> paymentsCollection;
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Column(name = "country_id")
    private Integer countryId;
    @Column(name = "newsletter")
    private Boolean newsletter;
    @Column(name = "referredby")
    private Integer referredby;
    @OneToMany(mappedBy = "userId")
    private Collection<Invoice> invoiceCollection;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "customerId")
    private Collection<SessionTrainers> sessionTrainersCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "customerId")
    private Collection<CustomerGoals> customerGoalsCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "customerId")
    private Collection<Participants> participantsCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "customerId")
    private Collection<CustomerImages> customerImagesCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "customer")
    private Collection<Activation> activationCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "username")
    private Collection<Groups> groupsCollection;
     @OneToMany(cascade = CascadeType.ALL, mappedBy = "userId")
    private Collection<SurveyAnswers> surveyAnswersCollection;

    @JoinColumn(name = "active", referencedColumnName = "id")
    @ManyToOne
    private CustomerState active;
    @JoinColumn(name = "group_pricing", referencedColumnName = "id")
    @ManyToOne
    private Plan groupPricing;
    @JoinColumn(name = "email_format", referencedColumnName = "id")
    @ManyToOne
    private EmailFormat emailFormat;
    @JoinColumn(name = "auth", referencedColumnName = "id")
    @ManyToOne
    private CustomerAuth auth;
    @JoinColumn(name = "demographic", referencedColumnName = "id")
    @ManyToOne
    private DemographicTypes demographic;
    @JoinColumn(name = "preferred_contact", referencedColumnName = "id")
    @ManyToOne
    private PreferedContact preferredContact;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "customerId")
    private Collection<StatsTaken> statsTakenCollection;
    @OneToMany(mappedBy = "userId")
    private Collection<Notes> notesCollection;

    

    public Customers() {
    }

    public Customers(Integer id) {
        this.id = id;
    }

    public Customers(Integer id, char gender, String firstname, String lastname, Date dob, String emailAddress, String username, String password) {
        this.id = id;
        this.gender = gender;
        this.firstname = firstname;
        this.lastname = lastname;
        this.dob = new Date(dob.getTime());
        this.emailAddress = emailAddress;
        this.username = username;
        this.password = password;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }


    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }


    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }


    public String getAddrState() {
        return addrState;
    }

    public void setAddrState(String addrState) {
        this.addrState = addrState;
    }

    public Integer getCountryId() {
        return countryId;
    }

    public void setCountryId(Integer countryId) {
        this.countryId = countryId;
    }


    public Boolean getNewsletter() {
        return newsletter;
    }

    public void setNewsletter(Boolean newsletter) {
        this.newsletter = newsletter;
    }

    public Integer getReferredby() {
        return referredby;
    }

    public void setReferredby(Integer referredby) {
        this.referredby = referredby;
    }

    @XmlTransient
    public Collection<Invoice> getInvoiceCollection() {
        return invoiceCollection;
    }

    public void setInvoiceCollection(Collection<Invoice> invoiceCollection) {
        this.invoiceCollection = invoiceCollection;
    }

    @XmlTransient
    public Collection<CustomerGoals> getCustomerGoalsCollection() {
        return customerGoalsCollection;
    }

    public void setCustomerGoalsCollection(Collection<CustomerGoals> customerGoalsCollection) {
        this.customerGoalsCollection = customerGoalsCollection;
    }

    @XmlTransient
    public Collection<Participants> getParticipantsCollection() {
        return participantsCollection;
    }

    public void setParticipantsCollection(Collection<Participants> participantsCollection) {
        this.participantsCollection = participantsCollection;
    }

    @XmlTransient
    public Collection<CustomerImages> getCustomerImagesCollection() {
        return customerImagesCollection;
    }

    public void setCustomerImagesCollection(Collection<CustomerImages> customerImagesCollection) {
        this.customerImagesCollection = customerImagesCollection;
    }

    @XmlTransient
    public Collection<Activation> getActivationCollection() {
        return activationCollection;
    }

    public void setActivationCollection(Collection<Activation> activationCollection) {
        this.activationCollection = activationCollection;
    }

    @XmlTransient
    public Collection<Groups> getGroupsCollection() {
        return groupsCollection;
    }
    public String getGroupsCollectionAsString(){
        String response = "";
        for(Groups g : getGroupsCollection()){
            response += g.getGroupname() + " ";
        }
        
        return response;
        
    }

    public void setGroupsCollection(Collection<Groups> groupsCollection) {
        this.groupsCollection = groupsCollection;
    }

    public CustomerState getActive() {
        return active;
    }

    public void setActive(CustomerState active) {
        this.active = active;
    }

    public Plan getGroupPricing() {
        return groupPricing;
    }

    public void setGroupPricing(Plan groupPricing) {
        this.groupPricing = groupPricing;
    }

    public EmailFormat getEmailFormat() {
        return emailFormat;
    }

    public void setEmailFormat(EmailFormat emailFormat) {
        this.emailFormat = emailFormat;
    }

    public CustomerAuth getAuth() {
        return auth;
    }

    public void setAuth(CustomerAuth auth) {
        this.auth = auth;
    }

    public DemographicTypes getDemographic() {
        return demographic;
    }

    public void setDemographic(DemographicTypes demographic) {
        this.demographic = demographic;
    }

    public PreferedContact getPreferredContact() {
        return preferredContact;
    }

    public void setPreferredContact(PreferedContact preferredContact) {
        this.preferredContact = preferredContact;
    }

    @XmlTransient
    public Collection<StatsTaken> getStatsTakenCollection() {
        return statsTakenCollection;
    }

    public void setStatsTakenCollection(Collection<StatsTaken> statsTakenCollection) {
        this.statsTakenCollection = statsTakenCollection;
    }

    @XmlTransient
    public Collection<Notes> getNotesCollection() {
        return notesCollection;
    }

    public void setNotesCollection(Collection<Notes> notesCollection) {
        this.notesCollection = notesCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Customers)) {
            return false;
        }
        Customers other = (Customers) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return firstname + " " + lastname;
    }


    @XmlTransient
    public Collection<SessionTrainers> getSessionTrainersCollection() {
        return sessionTrainersCollection;
    }

    public void setSessionTrainersCollection(Collection<SessionTrainers> sessionTrainersCollection) {
        this.sessionTrainersCollection = sessionTrainersCollection;
    }


    @XmlTransient
    public Collection<Payments> getPaymentsCollection() {
        return paymentsCollection;
    }

    public void setPaymentsCollection(Collection<Payments> paymentsCollection) {
        this.paymentsCollection = paymentsCollection;
    }

    public PaymentParameters getPaymentParametersId() {
        
        return paymentParametersId;
    }

    public void setPaymentParametersId(PaymentParameters paymentParameters) {
        this.paymentParametersId = paymentParameters;
    }

    public CustomerImages getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(CustomerImages profileImage) {
        this.profileImage = profileImage;
    }

    @XmlTransient
    public Collection<AuditLog> getAuditLogCollection() {
        return auditLogCollection;
    }

    public void setAuditLogCollection(Collection<AuditLog> auditLogCollection) {
        this.auditLogCollection = auditLogCollection;
    }

    @XmlTransient
    public Collection<AuditLog> getAuditLogCollection1() {
        return auditLogCollection1;
    }

    public void setAuditLogCollection1(Collection<AuditLog> auditLogCollection1) {
        this.auditLogCollection1 = auditLogCollection1;
    }


  

    @XmlTransient
    public Collection<SurveyAnswers> getSurveyAnswersCollection() {
        return surveyAnswersCollection;
    }

    public void setSurveyAnswersCollection(Collection<SurveyAnswers> surveyAnswersCollection) {
        this.surveyAnswersCollection = surveyAnswersCollection;
    }

    @XmlTransient
    public Collection<SessionTimetable> getSessionTimetableCollection() {
        return sessionTimetableCollection;
    }

    public void setSessionTimetableCollection(Collection<SessionTimetable> sessionTimetableCollection) {
        this.sessionTimetableCollection = sessionTimetableCollection;
    }

    public Date getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(Date lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public Integer getLoginAttempts() {
        return loginAttempts;
    }

    public void setLoginAttempts(Integer loginAttempts) {
        this.loginAttempts = loginAttempts;
    }

    public Boolean getMustResetPassword() {
        return mustResetPassword;
    }

    public void setMustResetPassword(Boolean mustResetPassword) {
        this.mustResetPassword = mustResetPassword;
    }

    public Boolean getTermsConditionsAccepted() {
        return termsConditionsAccepted;
    }

    public void setTermsConditionsAccepted(Boolean termsConditionsAccepted) {
        this.termsConditionsAccepted = termsConditionsAccepted;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public void setEmergencyContactName(String emergencyContactName) {
        this.emergencyContactName = emergencyContactName;
    }

    public String getEmergencyContactPhone() {
        return emergencyContactPhone;
    }

    public void setEmergencyContactPhone(String emergencyContactPhone) {
        this.emergencyContactPhone = emergencyContactPhone;
    }

    @XmlTransient
    public Collection<SessionBookings> getSessionBookingsCollection() {
        return sessionBookingsCollection;
    }

    public void setSessionBookingsCollection(Collection<SessionBookings> sessionBookingsCollection) {
        this.sessionBookingsCollection = sessionBookingsCollection;
    }

    @XmlTransient
    public Collection<QuestionnaireMap> getQuestionnaireMapCollection() {
        return questionnaireMapCollection;
    }

    public void setQuestionnaireMapCollection(Collection<QuestionnaireMap> questionnaireMapCollection) {
        this.questionnaireMapCollection = questionnaireMapCollection;
    }

    @XmlTransient
    public Collection<Suppliers> getSuppliersCollection() {
        return suppliersCollection;
    }

    public void setSuppliersCollection(Collection<Suppliers> suppliersCollection) {
        this.suppliersCollection = suppliersCollection;
    }

    public Character getGender() {
        return gender;
    }

    public void setGender(Character gender) {
        this.gender = gender;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public Date getDob() {
        return dob;
    }

    public void setDob(Date dob) {
        this.dob = dob;
    }

  

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

   
    public String getSuburb() {
        return suburb;
    }

    public void setSuburb(String suburb) {
        this.suburb = suburb;
    }

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

  

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getFax() {
        return fax;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFacebookId() {
        return facebookId;
    }

    public void setFacebookId(String facebookId) {
        this.facebookId = facebookId;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

   

    @XmlTransient
    public Collection<NotificationsLog> getNotificationsLogCollection() {
        return notificationsLogCollection;
    }

    public void setNotificationsLogCollection(Collection<NotificationsLog> notificationsLogCollection) {
        this.notificationsLogCollection = notificationsLogCollection;
    }

    
    @XmlTransient
    public Collection<Tickets> getTicketsCollection() {
        return ticketsCollection;
    }

   
    public void setTicketsCollection(Collection<Tickets> ticketsCollection) {
        this.ticketsCollection = ticketsCollection;
    }

    

}
