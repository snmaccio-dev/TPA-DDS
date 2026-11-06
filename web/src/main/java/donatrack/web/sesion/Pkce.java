package donatrack.web.sesion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public final class Pkce {

  private static final SecureRandom AZAR = new SecureRandom();
  private static final Base64.Encoder BASE64 = Base64.getUrlEncoder().withoutPadding();

  private Pkce() {
  }

  public static String nuevoVerificador() {
    byte[] bytes = new byte[32];
    AZAR.nextBytes(bytes);

    return BASE64.encodeToString(bytes);
  }

  public static String desafioDe(String verificador) {
    try {
      return BASE64.encodeToString(
          MessageDigest.getInstance("SHA-256")
              .digest(verificador.getBytes(StandardCharsets.US_ASCII))
      );
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("La JVM no soporta SHA-256.", e);
    }
  }
}
