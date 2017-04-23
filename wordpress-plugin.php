<?php

/*
Plugin Name: Fitness CRM Lead Plugin
Plugin URI: https://services.purefitnessmanly.com.au
Description: A simple hook to push contact form 7 form information to a CRM webservice
Version: 1.3
Author: David Turner
Author URI: http://www.purefitnessmanly.com.au
*/

add_action( 'wpcf7_before_send_mail', 'create_new_lead_in_crm', 10, 1 ); 
//add_action( 'wpcf7_mail_sent', 'create_new_lead_in_crm_function' );

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

		$url = "https://services.purefitnessmanly.com.au/FitnessStats/WordpressWS?wsdl";
                $client = new SoapClient($url);
               // $fcs = $client->__getFunctions();
                $res = $client->addNewLead(array('firstname'=> $splitName[0], 'lastname' => $splitName[1], 'email' => $email, 'mobile' => $phone, 'message' => $message));


	     }
        }else{
          echo "ERROR - Submitted form is empty ";
        }
}



?>
