package net.anzix.grpc.kerberos;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslClientFactory;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import javax.security.sasl.SaslServerFactory;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.HashMap;

import net.anzix.grpc.kerberos.LoginConfiguration.LoginParam;
import net.anzix.grpc.kerberos.LoginConfiguration.LoginParams;

public class TestBench {

  byte[] challenge = new byte[0];
  private SaslServer saslServer;
  private SaslClient saslClient;

  public static void main(String[] args) throws Exception {
    new TestBench().run();
  }

  private void run() throws Exception {
    System.setProperty("java.security.krb5.conf", "/etc/krb5.conf");

    LoginParams serverParam = new LoginParams();
    serverParam.put(LoginParam.PRINCIPAL, "om/om@ATHENA.MIT.EDU");
    serverParam.put(LoginParam.KEYTAB, "/tmp/om.keytab");

    LoginContext serverContext =
        new LoginContext(LoginConfiguration.KERBEROS_CONFIG_NAME,
            (Subject) null, null,
            new LoginConfiguration(serverParam));
    serverContext.login();
    saslServer = Subject.doAs(serverContext.getSubject(),
        (PrivilegedExceptionAction<SaslServer>) this::getSaslServer);

    LoginParams clientParams = new LoginParams();
    clientParams.put(LoginParam.PRINCIPAL, "admin/admin@ATHENA.MIT.EDU");
    clientParams.put(LoginParam.KEYTAB, "/tmp/admin.keytab");

    LoginContext clientContext =
        new LoginContext(LoginConfiguration.KERBEROS_CONFIG_NAME,
            (Subject) null, null,
            new LoginConfiguration(clientParams));
    clientContext.login();

    saslClient = Subject.doAs(clientContext.getSubject(),
        (PrivilegedExceptionAction<SaslClient>) () -> getSaslClient());

    Subject.doAs(clientContext.getSubject(),
        (PrivilegedExceptionAction<Void>) () -> {
          challenge =
              saslClient.evaluateChallenge(challenge);
          return null;
        });
    challenge = saslServer.evaluateResponse(challenge);
    System.out.println(saslServer.isComplete());
    challenge = saslClient.evaluateChallenge(challenge);
    challenge = saslServer.evaluateResponse(challenge);
    System.out.println(saslServer.isComplete());
    System.out.println(saslServer.getAuthorizationID());
    return;

  }

  private SaslClient getSaslClient() throws SaslException {
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

  private SaslServer getSaslServer() throws SaslException {
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
