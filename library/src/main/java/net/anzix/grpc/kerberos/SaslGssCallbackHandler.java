package net.anzix.grpc.kerberos;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Callback handler for GSSAPI.
 *
 * This class is imported from Apache Hadoop.
 */
public class SaslGssCallbackHandler implements CallbackHandler {
  private static final Logger LOG =
      LoggerFactory.getLogger(SaslGssCallbackHandler.class);

  @Override
  public void handle(Callback[] callbacks) throws
      UnsupportedCallbackException {
    AuthorizeCallback ac = null;
    for (Callback callback : callbacks) {
      if (callback instanceof AuthorizeCallback) {
        ac = (AuthorizeCallback) callback;
      } else {
        throw new UnsupportedCallbackException(callback,
            "Unrecognized SASL GSSAPI Callback");
      }
    }
    if (ac != null) {
      String authid = ac.getAuthenticationID();
      String authzid = ac.getAuthorizationID();
      if (authid.equals(authzid)) {
        ac.setAuthorized(true);
      } else {
        ac.setAuthorized(false);
      }
      if (ac.isAuthorized()) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("SASL server GSSAPI callback: setting "
              + "canonicalized client ID: " + authzid);
        }
        ac.setAuthorizedID(authzid);
      }
    }
  }
}

