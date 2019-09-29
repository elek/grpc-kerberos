package net.anzix.grpc.kerberos.example.token;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

/**
 * Client interceptor to send a token in the GRPC header field.
 */
public class TokenClientInterceptor implements ClientInterceptor {

  private byte[] token;

  public TokenClientInterceptor(byte[] token) {
    this.token = token;
  }

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> method, CallOptions callOptions,
      Channel next) {
    return new SimpleForwardingClientCall<ReqT, RespT>(
        next.newCall(method, callOptions)) {
      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        headers.put(TokenFilterInterceptor.TOKEN_KEY, token);
        super.start(responseListener, headers);
      }
    };
  }
}
