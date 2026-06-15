package com.rohit.ChatApplication.service;

import com.rohit.ChatApplication.data.UserDetail;
import com.rohit.ChatApplication.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${JWT_SECRET}")
    private String secretKey;

    private static final  long ACCESS_TOKEN_VALIDITY = 1000 * 60 * 60 * 10;
    private static final long REFRESH_TOKEN_VALIDITY = 1000 * 60 * 60 * 24 * 7;

    public String extractUserName(String jwtToken) {
        return extractClaim(jwtToken, Claims::getSubject);
    }

    public String extractUserId(String jwtToken) {
        return extractClaim(jwtToken, claims -> claims.get("userId", String.class));
    }

    public <T> T extractClaim(String jwtToken, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(jwtToken);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String jwtToken) {
        return Jwts.parser().verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(jwtToken)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte [] bytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(bytes);
    }

    public boolean validateTokenSignatureOnly(String jwtToken) {
        try {
            // This automatically checks the signature against your secret key
            // AND checks if the 'exp' claim is in the past.
            extractAllClaims(jwtToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Catches SignatureException, ExpiredJwtException, MalformedJwtException, etc.
            // Log the exception message as a debug trace if needed, but return false to reject
            return false;
        }
    }

    public boolean validateToken(String jwtToken, UserDetails userDetails) {
        final String userName = extractUserName(jwtToken);
        return userName.equals(userDetails.getUsername()) && !isTokenExpired(jwtToken);
    }

    private boolean isTokenExpired(String jwtToken) {
        return extractExpiration(jwtToken).before(new Date());
    }

    private Date extractExpiration(String jwtToken) {
        return extractClaim(jwtToken, Claims::getExpiration);
    }

    public String generateToken(UserDetail userDetails) {
        return createToken(userDetails.getUsername() ,userDetails.getId(), ACCESS_TOKEN_VALIDITY);
    }

    public String generateRefreshToken(UserDetail userDetails ){
        return createToken(userDetails.getUsername() ,userDetails.getId(), REFRESH_TOKEN_VALIDITY);
    }

    private String createToken(String userName, String userId , long validity) {
        return Jwts.builder()
                .claim("userId", userId)
                .subject(userName)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() +validity))
                .signWith(getSigningKey())
                .compact();
    }
}
