/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import javax.faces.FacesException;
import javax.faces.context.ExternalContext;
import javax.faces.context.ExternalContextFactory;
import javax.faces.context.ExternalContextWrapper;

/**
 *
 * @author david
 */
public class CustomExternalContextSecureWebsocketWss extends ExternalContextWrapper {

    public CustomExternalContextSecureWebsocketWss(ExternalContext wrapped) {
        super(wrapped);
    }

    @Override
    public String encodeWebsocketURL(String url) {
        return super.encodeWebsocketURL(url).replaceFirst("ws://", "wss://");
    }

    public static class Factory extends ExternalContextFactory {
        public Factory(ExternalContextFactory wrapped) {
            super(wrapped);
        }

        @Override
        public ExternalContext getExternalContext(Object context, Object request, Object response) throws FacesException {
            return new CustomExternalContextSecureWebsocketWss(getWrapped().getExternalContext(context, request, response));
        }
    }
}
