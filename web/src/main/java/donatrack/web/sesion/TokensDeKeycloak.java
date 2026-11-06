package donatrack.web.sesion;

public record TokensDeKeycloak(
    String accessToken,
    String idToken,
    long expiresIn
) {
}
