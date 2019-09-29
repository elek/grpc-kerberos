package net.anzix.grpc.kerberos;

/**
 * Interface to be notified about a successful authorization.
 */
public interface AuthenticationListener {

  /**
   * Callback about an successful authentication..
   *
   * @param principal kerberos principal name.
   * @return Optional payload/token which will be propagated back to the client.
   */
  byte[] authenticated(String principal);
}
