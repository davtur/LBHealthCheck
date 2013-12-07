/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.db;

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
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author david
 */
@Entity
@Table(name = "customers")
@NamedQueries({
    @NamedQuery(name = "Customers.findAll", query = "SELECT c FROM Customers c"),
    @NamedQuery(name = "Customers.findById", query = "SELECT c FROM Customers c WHERE c.id = :id"),
    @NamedQuery(name = "Customers.findByGender", query = "SELECT c FROM Customers c WHERE c.gender = :gender"),
    @NamedQuery(name = "Customers.findByFirstname", query = "SELECT c FROM Customers c WHERE c.firstname = :firstname"),
    @NamedQuery(name = "Customers.findByLastname", query = "SELECT c FROM Customers c WHERE c.lastname = :lastname"),
    @NamedQuery(name = "Customers.findByDob", query = "SELECT c FROM Customers c WHERE c.dob = :dob"),
    @NamedQuery(name = "Customers.findByEmailAddress", query = "SELECT c FROM Customers c WHERE c.emailAddress = :emailAddress"),
    @NamedQuery(name = "Customers.findByPreferredContact", query = "SELECT c FROM Customers c WHERE c.preferredContact = :preferredContact"),
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
    @NamedQuery(name = "Customers.findByGroupPricing", query = "SELECT c FROM Customers c WHERE c.groupPricing = :groupPricing"),
    @NamedQuery(name = "Customers.findByEmailFormat", query = "SELECT c FROM Customers c WHERE c.emailFormat = :emailFormat"),
    @NamedQuery(name = "Customers.findByAuth", query = "SELECT c FROM Customers c WHERE c.auth = :auth"),
    @NamedQuery(name = "Customers.findByActive", query = "SELECT c FROM Customers c WHERE c.active = :active"),
    @NamedQuery(name = "Customers.findByReferredby", query = "SELECT c FROM Customers c WHERE c.referredby = :referredby")})
public class Customers implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @Column(name = "gender")
    private char gender;
    @Basic(optional = false)
    @Column(name = "firstname")
    private String firstname;
    @Basic(optional = false)
    @Column(name = "lastname")
    private String lastname;
    @Basic(optional = false)
    @Column(name = "dob")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dob;
    @Basic(optional = false)
    @Column(name = "email_address")
    private String emailAddress;
    @Column(name = "preferred_contact")
    private Integer preferredContact;
    @Basic(optional = false)
    @Column(name = "username")
    private String username;
    @Column(name = "street_address")
    private String streetAddress;
    @Column(name = "suburb")
    private String suburb;
    @Column(name = "postcode")
    private String postcode;
    @Column(name = "city")
    private String city;
    @Column(name = "addr_state")
    private String addrState;
    @Column(name = "country_id")
    private Integer countryId;
    @Column(name = "telephone")
    private String telephone;
    @Column(name = "fax")
    private String fax;
    @Basic(optional = false)
    @Column(name = "password")
    private String password;
    @Column(name = "newsletter")
    private Character newsletter;
    @Column(name = "group_pricing")
    private Integer groupPricing;
    @Column(name = "email_format")
    private String emailFormat;
    @Column(name = "auth")
    private Integer auth;
    @Column(name = "active")
    private Integer active;
    @Column(name = "referredby")
    private Integer referredby;
    @JoinColumn(name = "demographic", referencedColumnName = "id")
    @ManyToOne
    private DemograhicTypes demograhicTypes;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "customers")
    private Collection<Groups> groupsCollection;

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
        this.dob = dob;
        this.emailAddress = emailAddress;
        this.username = username;
        this.password = password;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public char getGender() {
        return gender;
    }

    public void setGender(char gender) {
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

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public Integer getPreferredContact() {
        return preferredContact;
    }

    public void setPreferredContact(Integer preferredContact) {
        this.preferredContact = preferredContact;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
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

    public Character getNewsletter() {
        return newsletter;
    }

    public void setNewsletter(Character newsletter) {
        this.newsletter = newsletter;
    }

    public Integer getGroupPricing() {
        return groupPricing;
    }

    public void setGroupPricing(Integer groupPricing) {
        this.groupPricing = groupPricing;
    }

    public String getEmailFormat() {
        return emailFormat;
    }

    public void setEmailFormat(String emailFormat) {
        this.emailFormat = emailFormat;
    }

    public Integer getAuth() {
        return auth;
    }

    public void setAuth(Integer auth) {
        this.auth = auth;
    }

    public Integer getActive() {
        return active;
    }

    public void setActive(Integer active) {
        this.active = active;
    }

    public Integer getReferredby() {
        return referredby;
    }

    public void setReferredby(Integer referredby) {
        this.referredby = referredby;
    }

    public DemograhicTypes getDemograhicTypes() {
        return demograhicTypes;
    }

    public void setDemograhicTypes(DemograhicTypes demograhicTypes) {
        this.demograhicTypes = demograhicTypes;
    }

    public Collection<Groups> getGroupsCollection() {
        return groupsCollection;
    }

    public void setGroupsCollection(Collection<Groups> groupsCollection) {
        this.groupsCollection = groupsCollection;
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
        return  firstname +" "+ lastname;
    }

}
