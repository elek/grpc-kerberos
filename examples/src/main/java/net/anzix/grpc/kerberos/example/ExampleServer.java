package net.anzix.grpc.kerberos.example;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import net.anzix.grpc.kerberos.server.NegotiationService;
import net.anzix.grpc.kerberos.example.token.TokenFilterInterceptor;
import net.anzix.grpc.kerberos.example.token.TokenService;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "server", mixinStandardHelpOptions = true,
    description = "Starts an example GRPC server.")
public class ExampleServer implements Runnable {

  @Option(names = {"-k", "--keytab"}, description = "Pat of of the keytab to be used. (Optional, the current session will be used if not set.")
  private String keytab;

  @Option(names = {"-p", "--principal"}, description = "kerberos principal to be used from the keytab. Eg. foo/bar@EXAMPLE.COM")
  private String principal;

  public static void main(String... args) {
    int exitCode = new CommandLine(new ExampleServer()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public void run() {
    try {
      TokenService tokenService = new TokenService();

      NegotiationService negotiationService;
      if (keytab != null) {
        negotiationService = new NegotiationService(tokenService, keytab, principal);
      } else {
        negotiationService = new NegotiationService(tokenService);
      }
      Server server = ServerBuilder
          .forPort(1234)
          .addService(negotiationService)
          .addService(ServerInterceptors.intercept(new HelloService(tokenService),
              new TokenFilterInterceptor(tokenService)))
          .build();
      server.start();
      server.awaitTermination();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
