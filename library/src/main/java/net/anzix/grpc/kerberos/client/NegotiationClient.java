package net.anzix.grpc.kerberos.client;

import javax.security.auth.login.LoginException;
import javax.security.sasl.SaslException;
import java.security.PrivilegedActionException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import net.anzix.grpc.kerberos.LoginConfiguration.LoginParam;
import net.anzix.grpc.kerberos.LoginConfiguration.LoginParams;
import net.anzix.grpc.kerberos.Negotiate.NegotiationMessageRequest;
import net.anzix.grpc.kerberos.NegotiationServiceGrpc;
import net.anzix.grpc.kerberos.NegotiationServiceGrpc.NegotiationServiceStub;

/**
 * Client to do the Kerberos authorization.
 */
public class NegotiationClient {

  private final NegotiationServiceStub stub;

  private LoginParams clientParams;

  public NegotiationClient(
      ManagedChannel channel, String keytab, String principal) {
    this.stub = NegotiationServiceGrpc.newStub(channel);
    clientParams = new LoginParams();
    clientParams.put(LoginParam.PRINCIPAL, principal);
    clientParams.put(LoginParam.KEYTAB, keytab);
  }

  public NegotiationClient(ManagedChannel channel) {
    this.stub = NegotiationServiceGrpc.newStub(channel);
    clientParams = LoginParams.getDefaults();
  }

  public byte[] negotiate()
      throws PrivilegedActionException, SaslException, LoginException,
      InterruptedException, ExecutionException {

    CompletableFuture<byte[]> token = new CompletableFuture<>();
    NegotiationClientStream handler =
        new NegotiationClientStream(clientParams, token);
    StreamObserver<NegotiationMessageRequest> sender = stub.negotiate(handler);
    handler.setSender(sender);

    handler.sendFirst();
    return token.get();
  }
}
