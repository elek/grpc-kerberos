package net.anzix.grpc.kerberos;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.sun.security.auth.module.Krb5LoginModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Login configuration which can help to cofigure kerberos login module.
 * <p>
 * This class is a simplified version of the same class from Apache Hadoop.
 */
public class LoginConfiguration
    extends javax.security.auth.login.Configuration {

  public static final String KRB5_LOGIN_MODULE =
      getKrb5LoginModuleName();

  private static final boolean windows =
      System.getProperty("os.name").startsWith("Windows");

  private static final boolean is64Bit =
      System.getProperty("os.arch").contains("64") ||
          System.getProperty("os.arch").contains("s390x");

  private static final boolean aix =
      System.getProperty("os.name").equals("AIX");

  private static final Logger LOG =
      LoggerFactory.getLogger(LoginConfiguration.class);

  static final String JAVA_VENDOR_NAME =
      System.getProperty("java.vendor");
  static final boolean IBM_JAVA = JAVA_VENDOR_NAME.contains("IBM");

  private static String getKrb5LoginModuleName() {
    return (IBM_JAVA)
        ? "com.ibm.security.auth.module.Krb5LoginModule"
        : "com.sun.security.auth.module.Krb5LoginModule";

  }

  public static final String KERBEROS_CONFIG_NAME = "grpc-kerberos";

  private static final Map<String, String> BASIC_JAAS_OPTIONS =
      new HashMap<>();

  static {
    if ("true".equalsIgnoreCase(System.getenv("GRPC_JAAS_DEBUG"))) {
      BASIC_JAAS_OPTIONS.put("debug", "true");
    }
  }

  private final LoginParams params;

  public LoginConfiguration(LoginParams params) {
    this.params = params;
  }

  @Override
  public LoginParams getParameters() {
    return params;
  }

  @Override
  public AppConfigurationEntry[] getAppConfigurationEntry(String appName) {
    ArrayList<AppConfigurationEntry> entries = new ArrayList<>();
    entries.add(getKerberosEntry());
    return entries.toArray(new AppConfigurationEntry[0]);
  }

  private AppConfigurationEntry getKerberosEntry() {
    final Map<String, String> options = new HashMap<>(BASIC_JAAS_OPTIONS);
    LoginModuleControlFlag controlFlag = LoginModuleControlFlag.OPTIONAL;
    // kerberos login is mandatory if principal is specified.  principal
    // will not be set for initial default login, but will always be set
    // for relogins.
    final String principal = params.get(LoginParam.PRINCIPAL);
    if (principal != null) {
      options.put("principal", principal);
      controlFlag = LoginModuleControlFlag.REQUIRED;
    }

    // use keytab if given else fallback to ticket cache.
    if (IBM_JAVA) {
      if (params.containsKey(LoginParam.KEYTAB)) {
        final String keytab = params.get(LoginParam.KEYTAB);
        if (keytab != null) {
          options.put("useKeytab", prependFileAuthority(keytab));
        } else {
          options.put("useDefaultKeytab", "true");
        }
        options.put("credsType", "both");
      } else {
        String ticketCache = params.get(LoginParam.CCACHE);
        if (ticketCache != null) {
          options.put("useCcache", prependFileAuthority(ticketCache));
        } else {
          options.put("useDefaultCcache", "true");
        }
        options.put("renewTGT", "true");
      }
    } else {
      if (params.containsKey(LoginParam.KEYTAB)) {
        options.put("useKeyTab", "true");
        final String keytab = params.get(LoginParam.KEYTAB);
        if (keytab != null) {
          options.put("keyTab", keytab);
        }
        options.put("storeKey", "true");
      } else {
        options.put("useTicketCache", "true");
        String ticketCache = params.get(LoginParam.CCACHE);
        if (ticketCache != null) {
          options.put("ticketCache", ticketCache);
        }
        options.put("renewTGT", "true");
      }
      options.put("doNotPrompt", "true");
    }
    options.put("refreshKrb5Config", "true");

    return new AppConfigurationEntry(
        KRB5_LOGIN_MODULE, controlFlag, options);
  }

  private static String prependFileAuthority(String keytabPath) {
    return keytabPath.startsWith("file://")
        ? keytabPath
        : "file://" + keytabPath;
  }

  public enum LoginParam {
    PRINCIPAL,
    KEYTAB,
    CCACHE,
  }

  public static class LoginParams extends EnumMap<LoginParam, String>
      implements Parameters {
    public LoginParams() {
      super(LoginParam.class);
    }

    // do not add null values, nor allow existing values to be overriden.
    @Override
    public String put(LoginParam param, String val) {
      boolean add = val != null && !containsKey(param);
      return add ? super.put(param, val) : null;
    }

    public static LoginParams getDefaults() {
      LoginParams params = new LoginParams();
      params.put(LoginParam.PRINCIPAL, System.getenv("KRB5PRINCIPAL"));
      params.put(LoginParam.KEYTAB, System.getenv("KRB5KEYTAB"));
      params.put(LoginParam.CCACHE, System.getenv("KRB5CCNAME"));
      return params;
    }
  }
}