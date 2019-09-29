package net.anzix.grpc.kerberos.server;

import io.grpc.stub.StreamObserver;
import net.anzix.grpc.kerberos.AuthenticationListener;
import net.anzix.grpc.kerberos.LoginConfiguration.LoginParam;
import net.anzix.grpc.kerberos.LoginConfiguration.LoginParams;
import net.anzix.grpc.kerberos.Negotiate.NegotiationMessageRequest;
import net.anzix.grpc.kerberos.Negotiate.NegotiationMessageResponse;
import net.anzix.grpc.kerberos.NegotiationServiceGrpc.NegotiationServiceImplBase;
import net.anzix.grpc.kerberos.example.token.TokenService;

/**
 * Grpc service which implements the SASL negotiation.
 */
public class NegotiationService extends NegotiationServiceImplBase {

  private AuthenticationListener authenticationListener;

  private LoginParams loginParams;

  public NegotiationService(AuthenticationListener authenticationListener) {
    this.authenticationListener = authenticationListener;
    loginParams = LoginParams.getDefaults();
  }

  public NegotiationService(TokenService authorizationListener, String keytab,
      String principal) {
    this.authenticationListener = authorizationListener;
    loginParams = new LoginParams();
    loginParams.put(LoginParam.PRINCIPAL, principal);
    loginParams.put(LoginParam.KEYTAB, keytab);
  }

  @Override
  public StreamObserver<NegotiationMessageRequest> negotiate(
      StreamObserver<NegotiationMessageResponse> responseObserver) {
    return new NegotiationServerStream(responseObserver, loginParams,
        authenticationListener);
  }
}
