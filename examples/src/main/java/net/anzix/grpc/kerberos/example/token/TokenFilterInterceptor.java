package net.anzix.grpc.kerberos.example.token;

import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

/**
 * Server interceptor to check the validity of a token.
 */
public class TokenFilterInterceptor implements ServerInterceptor {

  static final Key<byte[]> TOKEN_KEY =
      Key.of("authorization-token-bin", Metadata.BINARY_BYTE_MARSHALLER);

  private TokenService tokenService;

  public TokenFilterInterceptor(TokenService tokenService) {
    this.tokenService = tokenService;
  }

  @Override
  public <ReqT, RespT> Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata headers,
      ServerCallHandler<ReqT, RespT> next) {
    if (headers.containsKey(TOKEN_KEY)) {
      byte[] token = headers.get(TOKEN_KEY);
      if (tokenService.isValidToken(token)) {
        System.out.println(Thread.currentThread());
        return next.startCall(call, headers);
      }
    }
    call.close(Status.UNAUTHENTICATED, new Metadata());
    return new Listener<ReqT>() {
      @Override
      public void onMessage(ReqT message) {
      }

      @Override
      public void onHalfClose() {

      }

      @Override
      public void onCancel() {

      }

      @Override
      public void onComplete() {

      }

      @Override
      public void onReady() {

      }
    };
  }
}
