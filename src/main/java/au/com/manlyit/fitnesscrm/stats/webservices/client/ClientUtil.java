package au.com.manlyit.fitnesscrm.stats.webservices.client;


import au.com.manlyit.fitnesscrm.stats.webservices.NewLead;








public class ClientUtil {

    public static String createCustomerInJSON(String name, String emailAddress, String phoneNumber, String message, String authToken) {
        return String.format("{\"name\":\"%s\",\"emailAddress\":\"%s\",\"phoneNumber\":\"%s\",\"message\":\"%s\",\"authToken\":\"%s\"}",
                             name, emailAddress, phoneNumber,message,authToken);

    }

    public static NewLead createNewCustomer(String name, String emailAddress, String phoneNumber, String message, String authToken) {
        NewLead newCustomer = new NewLead();
        newCustomer.setName(name);
        newCustomer.setEmailAddress(emailAddress);
        newCustomer.setPhoneNumber(phoneNumber);
        newCustomer.setMessage(message);
        newCustomer.setAuthToken(authToken);
        return newCustomer;
    }

}
