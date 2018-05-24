<?php

/*
Plugin Name: Fitness CRM Lead Plugin
Plugin URI: https://services.purefitnessmanly.com.au
Description: A simple hook to push contact form 7 form information to a CRM webservice
Version: 1.3
Author: David Turner
Author URI: http://www.purefitnessmanly.com.au
*/

//add_action( 'wpcf7_before_send_mail', 'create_new_lead_in_crm', 10, 1 ); 
//add_action( 'wpcf7_mail_sent', 'create_new_lead_in_crm_function' );
//
add_action( 'wp_footer', 'mycustom_wp_footer' );
 
//add_action( 'wpcf7_before_send_mail', 'mycustom_wp_footer'); 


function mycustom_wp_footer() {
?>
<script type="text/javascript">
document.addEventListener( 'wpcf7mailsent', function( event ) {
    ga( 'send', 'event', 'Contact Form', 'submit' );
	//alert("Fired plugin");
	console.log(event);
	var inputs = event.detail.inputs;
	var id = "0";
	var name ="";
	var authToken ="Green21Blue22";
	var emailAddress ="";
	var phoneNumber ="";
	var message ="";
	for ( var i = 0; i < inputs.length; i++ ) {
        if ( 'lead-name' == inputs[i].name ) {
           name = inputs[i].value
        }
		if ( 'lead-email' == inputs[i].name ) {
           emailAddress = inputs[i].value
        }       
		if ( 'lead-phone' == inputs[i].name ) {
           phoneNumber = inputs[i].value
        }
		if ( 'lead-message' == inputs[i].name ) {
           message = inputs[i].value
        }       

    }
	var newLead = {"id": id,"name": name, "emailAddress": emailAddress, "phoneNumber": phoneNumber,"message": message,"authToken": authToken};
    if ( undefined !== window.jQuery ) {
        jQuery.ajax({
			type: "POST",
			contentType: "application/json",
    		url: "https://test-services.purefitnessmanly.com.au/FitnessStats/api/customers",
	 		//dataType:'json',
			data: JSON.stringify(newLead),
  			xhrFields: {
    			// The 'xhrFields' property sets additional fields on the XMLHttpRequest.
   	 			// This can be used to set the 'withCredentials' property.
    			// Set the value to 'true' if you'd like to pass cookies to the server.
    			// If this is enabled, your server must respond with the header
    			// 'Access-Control-Allow-Credentials: true'.
    			withCredentials: false
  			},

  			headers: {
    			// Set any custom headers here.
    			// If you set any non-simple headers, your server must include these
    			// headers in the 'Access-Control-Allow-Headers' response header.
  			},   
	
    		success: function (data, textStatus, jqXHR) {
        			console.log(textStatus);
    		},
    		error: function( jqXHR,  textStatus,  errorThrown){
        		console.log("Something went wrong", textStatus);
    		}
		});
	}else{
		 console.log("Something went wrong", "undefined !== window.jQuery");
	}	
	
}, false );


</script>
<?php
}

function create_new_lead_in_crm( $contact_form ) {
	$title = $contact_form->title();
        $submission = WPCF7_Submission::get_instance();

	if ( $submission ) {
		$posted_data = $submission->get_posted_data();


	    if ( 'Contact Form' == $title ) {

		$name = $posted_data['lead-name'];
		$email = $posted_data['lead-email'];
		$phone = $posted_data['lead-phone'];// if you have a field with name "phone"
		$message = $posted_data['lead-message']; 
		$splitName = explode(" ", $name,2);

		$url = "https://services.purefitnessmanly.com.au/FitnessStats/WordpressInterfaceWebService?wsdl";
                $client = new SoapClient($url);
               // $fcs = $client->__getFunctions();
                $res = $client->addNewLead(array('firstname'=> $splitName[0], 'lastname' => $splitName[1], 'email' => $email, 'mobile' => $phone, 'message' => $message));


	     }
        }else{
          echo "ERROR - Submitted form is empty ";
        }
}



?>
