package net.anzix.grpc.kerberos.example.token;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import net.anzix.grpc.kerberos.AuthenticationListener;

/**
 * A custom authorization listener implements a token service.
 * <p>
 * This is a very simple token service (for example without expiry)
 * demonstrates how kerberos based authentication can be used together with
 * any custom authorization.
 */
public class TokenService implements AuthenticationListener {

  private Random random = new Random();

  private Map<Token, String> tokens = new HashMap<>();

  public byte[] authenticated(String principal) {
    byte[] token = new byte[32];
    random.nextBytes(token);
    tokens.put(new Token(token), principal);
    return token;
  }

  public boolean isValidToken(byte[] token) {
    return tokens.containsKey(new Token(token));
  }

  public String getAuthenticatedName(byte[] token) {
    return tokens.get(new Token(token));
  }

  private static final class Token {
    private final byte[] token;

    public Token(byte[] token) {
      this.token = token;
    }

    public byte[] getToken() {
      return token;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Token token1 = (Token) o;
      return Arrays.equals(token, token1.token);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(token);
    }
  }
}
