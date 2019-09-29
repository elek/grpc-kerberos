package net.anzix.grpc.kerberos.server;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import javax.security.sasl.SaslServerFactory;
import java.security.PrivilegedExceptionAction;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import net.anzix.grpc.kerberos.AuthenticationListener;
import net.anzix.grpc.kerberos.LoginConfiguration;
import net.anzix.grpc.kerberos.LoginConfiguration.LoginParams;
import net.anzix.grpc.kerberos.Negotiate.NegotiationMessageRequest;
import net.anzix.grpc.kerberos.Negotiate.NegotiationMessageResponse;
import net.anzix.grpc.kerberos.Negotiate.NegotiationMessageResponse.Builder;
import net.anzix.grpc.kerberos.SaslGssCallbackHandler;

public class NegotiationServerStream
    implements StreamObserver<NegotiationMessageRequest> {

  private final StreamObserver<NegotiationMessageResponse> responseObserver;

  private AuthenticationListener authenticationListener;

  private SaslServer saslServer;

  public NegotiationServerStream(
      StreamObserver<NegotiationMessageResponse> responseObserver,
      LoginParams loginParams,
      AuthenticationListener authenticationListener) {
    this.responseObserver = responseObserver;
    this.authenticationListener = authenticationListener;
    try {

      LoginContext serverContext =
          new LoginContext(LoginConfiguration.KRB5_LOGIN_MODULE,
              (Subject) null, null,
              new LoginConfiguration(loginParams));
      serverContext.login();
      saslServer = Subject.doAs(serverContext.getSubject(),
          (PrivilegedExceptionAction<SaslServer>) () -> getSaslServer());
    } catch (Exception e) {
      e.printStackTrace();
      responseObserver.onError(e);
    }
  }

  @Override
  public void onNext(NegotiationMessageRequest request) {
    if (!saslServer.isComplete()) {
      try {
        System.out.println(Base64.getEncoder()
            .encodeToString(request.getChallenge().toByteArray()));

        byte[] challenge = saslServer
            .evaluateResponse(request.getChallenge().toByteArray());

        Builder responseBuilder = NegotiationMessageResponse.newBuilder()
            .setChallenge(saslServer.isComplete() ?
                ByteString.copyFromUtf8("") :
                ByteString.copyFrom(challenge))
            .setComplete(saslServer.isComplete());

        if (saslServer.isComplete()) {
          byte[] token =
              authenticationListener.authenticated(saslServer.getAuthorizationID());
          responseBuilder.setToken(ByteString.copyFrom(token));
        }

        responseObserver.onNext(responseBuilder.build());
        if (saslServer.isComplete()) {
          responseObserver.onCompleted();
        }
      } catch (SaslException e) {
        e.printStackTrace();
        responseObserver.onError(e);
      }
    }
  }

  @Override
  public void onError(Throwable throwable) {

  }

  @Override
  public void onCompleted() {

  }

  private static SaslServer getSaslServer() throws SaslException {
    Enumeration<SaslServerFactory> saslServerFactories =
        Sasl.getSaslServerFactories();
    while (saslServerFactories.hasMoreElements()) {
      SaslServer saslServer = saslServerFactories.nextElement()
          .createSaslServer("GSSAPI", "om", "om",
              new HashMap<String, String>(),
              new SaslGssCallbackHandler());
      if (saslServer != null) {
        return saslServer;
      }
    }
    throw new IllegalArgumentException(
        "Can't found SaslServer implementation compatible with GSSAPI");
  }

}
