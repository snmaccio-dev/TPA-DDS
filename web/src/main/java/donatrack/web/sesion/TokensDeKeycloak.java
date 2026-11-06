package donatrack.web.sesion;

public record TokensDeKeycloak(
    String accessToken,
    String refreshToken,
    String idToken,
    long expiresIn
) {
}
