package com.example.app.security;

import com.example.app.exception.AuthException;
import com.example.app.model.entity.UserEntity;
import com.example.app.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.expiration}")
    private Integer expirationInSeconds;
    @Value("${jwt.issuer}")
    private String issuer;

    public Mono<TokenDetails> authenticate(String username, String password) {
        return userRepository.findByUsername(username)
                .flatMap(user -> {
                    if (user.isDisabled()) {
                        return Mono.error(new AuthException(
                                "Account disabled", "JOYMUTLU_USER_ACCOUNT_DISABLED"));
                    }
                    if (!passwordEncoder.matches(password, user.getPassword())) {
                        return Mono.error(new AuthException(
                                "Invalid password", "JOYMUTLU_INVALID_PASSWORD"));
                    }
                    return Mono.just(generateToken(user)
                            .toBuilder()
                            .userId(user.getId())
                            .build());
                })
                // bad security approach
                .switchIfEmpty(Mono.error(new AuthException(
                        "Invalid username", "JOYMUTLU_INVALID_USERNAME")));
    }

    private TokenDetails generateToken(UserEntity user) {
        final HashMap<String, Object> claims = new HashMap<>(){{
           put("role", user.getRole());
        }};
        return generateToken(claims, user.getId().toString());
    }

    private TokenDetails generateToken(HashMap<String, Object> claims, String subject) {
        final Long expirationTimeInMillis = expirationInSeconds * 1000L;
        final Date expirationDate = new Date(new Date().getTime() + expirationTimeInMillis);

        return generateToken(expirationDate, claims, subject);
    }

    private TokenDetails generateToken(
            Date expirationDate,
            HashMap<String, Object> claims,
            String subject) {
        final Date createdDate = new Date();
        final String token = Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setIssuer(issuer)
                .setSubject(subject)
                .setIssuedAt(createdDate)
                .setExpiration(expirationDate)
                .setClaims(claims)
                .signWith(
                        SignatureAlgorithm.HS256,
                        Base64.getEncoder().encodeToString(secret.getBytes())
                )
                .compact();

        return TokenDetails.builder()
                .token(token)
                .issuedAt(createdDate)
                .expiresAt(expirationDate)
                .build();
    }
}
