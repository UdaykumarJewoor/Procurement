package com.pradeepit.pit_auth_service.component.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

@Component
public class JwtUtil {

    private final Logger log = Logger.getLogger(JwtUtil.class.getName());

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration-time}")
    private long jwtExpiration;

    @Value("${security.jwt.refreshExpirationMs}")
    private long refreshTokenValidity;


    /**
     * Extracts the username (subject) from the JWT token.
     *
     * @param token the JWT token
     * @return the username (subject) extracted from the token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts a specific claim from the JWT token.
     *
     * @param token          the JWT token
     * @param claimsResolver the function to extract the claim
     * @param <T>            the type of the claim
     * @return the extracted claim
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generates a JWT token for the provided user details.
     *
     * @param userDetails the user details to include in the token
     * @return the generated JWT token
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(Map.of(), userDetails);
    }

    /**
     * Generates a JWT token with additional claims for the provided user details.
     *
     * @param extraClaims additional claims to include in the token
     * @param userDetails the user details to include in the token
     * @return the generated JWT token
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        log.info("Generated JWT token for user: {}" + userDetails.getUsername());
        log.info("JWT token expiration time: {}" + jwtExpiration);
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    /**
     * Generates a refresh JWT token for the provided user details.
     *
     * @param userDetails the user details to include in the refresh token
     * @return the generated refresh JWT token
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return generateRefreshToken(Map.of(), userDetails);
    }

    /**
     * Generates a refresh JWT token with additional claims for the provided user details.
     *
     * @param extraClaims additional claims to include in the refresh token
     * @param userDetails the user details to include in the refresh token
     * @return the generated refresh JWT token
     */
    public String generateRefreshToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        log.info("Generated refresh JWT token for user: {}" + userDetails.getUsername());
        return buildToken(extraClaims, userDetails, refreshTokenValidity);
    }

    /**
     * Builds the JWT token with the given claims, user details, and expiration time.
     *
     * @param extraClaims additional claims to include in the token
     * @param userDetails the user details to include in the token
     * @param expiration  the expiration time of the token
     * @return the JWT token
     */
    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Checks whether the JWT token is valid based on the username and expiration.
     *
     * @param token       the JWT token
     * @param userDetails the user details to compare with
     * @return true if the token is valid, false otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && isTokenExpired(token);
    }

    /**
     * Validates the JWT token by checking if the username matches and the token is not expired.
     *
     * @param token    the JWT token
     * @param username the username to compare with
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String token, String username) {
        String extractedUsername = extractUsername(token);
        return extractedUsername.equals(username) && isTokenExpired(token);
    }

    /**
     * Checks whether the JWT token has expired.
     *
     * @param token the JWT token
     * @return true if the token is expired, false otherwise
     */
    private boolean isTokenExpired(String token) {
        return !extractExpiration(token).before(new Date());
    }

    /**
     * Extracts the expiration date from the JWT token.
     *
     * @param token the JWT token
     * @return the expiration date extracted from the token
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts all claims from the JWT token.
     *
     * @param token the JWT token
     * @return the claims extracted from the token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Retrieves the signing key used to sign the JWT token.
     *
     * @return the signing key
     */
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
