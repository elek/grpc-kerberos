package net.anzix.grpc.kerberos.example;

import io.grpc.stub.StreamObserver;
import net.anzix.grpc.kerberos.example.HelloServiceGrpc.HelloServiceImplBase;
import net.anzix.grpc.kerberos.example.HelloServiceProtos.HelloRequest;
import net.anzix.grpc.kerberos.example.HelloServiceProtos.HelloResponse;
import net.anzix.grpc.kerberos.example.token.TokenService;

public class HelloService extends HelloServiceImplBase {

  private TokenService tokenService;

  public HelloService(TokenService tokenService) {
    this.tokenService = tokenService;
  }

  @Override
  public void hello(HelloRequest request,
      StreamObserver<HelloResponse> responseObserver) {
    System.out.println(Thread.currentThread());

    String authorizedName = "";
    if (request.getToken() != null) {
      authorizedName = tokenService.getAuthenticatedName(request.getToken().toByteArray());
    }
    responseObserver.onNext(
        HelloResponse.newBuilder().setResponse("Hello " + request.getName() + " (" + authorizedName + ")")
            .build());
    responseObserver.onCompleted();
  }
}