package net.anzix.grpc.kerberos.example;

import com.google.protobuf.ByteString;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import net.anzix.grpc.kerberos.client.NegotiationClient;
import net.anzix.grpc.kerberos.example.token.TokenClientInterceptor;
import net.anzix.grpc.kerberos.example.HelloServiceGrpc.HelloServiceBlockingStub;
import net.anzix.grpc.kerberos.example.HelloServiceProtos.HelloRequest;
import net.anzix.grpc.kerberos.example.HelloServiceProtos.HelloResponse;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "server", mixinStandardHelpOptions = true,
    description = "Starts an example GRPC client.")
public class ExampleClient implements Runnable {

  @Option(names = {"-k", "--keytab"}, description = "Pat of of the keytab to be used. (Optional, the current session will be used if not set.")
  private String keytab;

  @Option(names = {"-p", "--principal"}, description = "kerberos principal to be used from the keytab. Eg. foo/bar@EXAMPLE.COM")
  private String principal;

  @Option(names = {"-s", "--skip-kerberos"}, description = "Skip kerberos based authentication.")
  private boolean skipKerberos;

  public static void main(String... args) {
    int exitCode = new CommandLine(new ExampleClient()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public void run() {
    try {
      ManagedChannelBuilder<?> builder =
          ManagedChannelBuilder.forAddress("localhost", 1234).usePlaintext();
      ManagedChannel channel = builder.build();

      byte[] token = new byte[0];
      HelloServiceBlockingStub helloService;

      if (!skipKerberos) {
        NegotiationClient negotiationClient;

        if (keytab != null) {
          negotiationClient = new NegotiationClient(channel, keytab, principal);
        } else {
          negotiationClient = new NegotiationClient(channel);
        }
        token = negotiationClient.negotiate();
        helloService =
            HelloServiceGrpc.newBlockingStub(ClientInterceptors
                .intercept(channel, new TokenClientInterceptor(token)));

      } else {
        helloService =
            HelloServiceGrpc.newBlockingStub(channel);
      }

      HelloResponse response =
          helloService.hello(HelloRequest.newBuilder().setName("Geza")
              .setToken(ByteString.copyFrom(token))
              .build());

      System.out.println(response.getResponse());
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
