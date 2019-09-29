package net.anzix.grpc.kerberos.client;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslClientFactory;
import javax.security.sasl.SaslException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import net.anzix.grpc.kerberos.LoginConfiguration;
import net.anzix.grpc.kerberos.LoginConfiguration.LoginParam;
import net.anzix.grpc.kerberos.LoginConfiguration.LoginParams;
import net.anzix.grpc.kerberos.Negotiate.NegotiationMessageRequest;
import net.anzix.grpc.kerberos.Negotiate.NegotiationMessageResponse;

public class NegotiationClientStream
    implements StreamObserver<NegotiationMessageResponse> {

  private final LoginContext clientContext;

  private StreamObserver<NegotiationMessageRequest> sender;

  private final SaslClient saslClient;

  private CompletableFuture<byte[]> responseToken;

  public NegotiationClientStream(
      LoginParams clientParams,
      CompletableFuture<byte[]> token)
      throws LoginException, PrivilegedActionException {
    responseToken = token;

    clientContext = new LoginContext(LoginConfiguration.KERBEROS_CONFIG_NAME,
        (Subject) null, null,
        new LoginConfiguration(clientParams));
    clientContext.login();

    saslClient = Subject.doAs(clientContext.getSubject(),
        (PrivilegedExceptionAction<SaslClient>) NegotiationClientStream::getSaslClient);
  }

  @Override
  public void onNext(NegotiationMessageResponse negotiationMessageResponse) {
    try {
      if (!negotiationMessageResponse.getComplete()) {
        byte[] challengeRespponse = getChallenge(
            negotiationMessageResponse.getChallenge().toByteArray());

        NegotiationMessageRequest request =
            NegotiationMessageRequest.newBuilder()
                .setChallenge(ByteString.copyFrom(challengeRespponse))
                .build();
        sender.onNext(request);
      } else {
        sender.onCompleted();
        responseToken
            .complete(negotiationMessageResponse.getToken().toByteArray());
      }
    } catch (Exception ex) {
      sender.onError(ex);
      sender.onCompleted();
      responseToken.completeExceptionally(ex);
    }
  }

  @Override
  public void onError(Throwable throwable) {

  }

  @Override
  public void onCompleted() {

  }

  private static SaslClient getSaslClient() throws SaslException {
    Enumeration<SaslClientFactory> saslClientFactories =
        Sasl.getSaslClientFactories();
    while (saslClientFactories.hasMoreElements()) {
      SaslClient saslClient = saslClientFactories.nextElement()
          .createSaslClient(new String[] {"GSSAPI"}, "",
              "om", "om",
              new HashMap<String, String>(), null);
      if (saslClient != null) {
        return saslClient;
      }
    }
    throw new IllegalArgumentException(
        "Can't found SaslClient implementation compatible with GSSAPI");
  }

  public byte[] getChallenge(byte[] challenge)
      throws SaslException, PrivilegedActionException {
    return Subject.doAs(clientContext.getSubject(),
        (PrivilegedExceptionAction<byte[]>) () -> saslClient
            .evaluateChallenge(challenge));
  }

  public void setSender(
      StreamObserver<NegotiationMessageRequest> sender) {
    this.sender = sender;
  }

  public void sendFirst() throws PrivilegedActionException, SaslException {
    NegotiationMessageRequest request = NegotiationMessageRequest.newBuilder()
        .setChallenge(ByteString.copyFrom(getChallenge(new byte[0])))
        .build();
    sender.onNext(request);
  }
}
