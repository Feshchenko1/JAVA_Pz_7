package com.dailycodework.pz_4_1.service;

import com.dailycodework.pz_4_1.exception.TokenRefreshException;
import com.dailycodework.pz_4_1.model.RefreshToken;
import com.dailycodework.pz_4_1.repository.RefreshTokenRepository;
import com.dailycodework.pz_4_1.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Value("${jwt.refreshExpirationMs}")
    private Long refreshTokenDurationMs;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        Optional<RefreshToken> existingToken = userRepository.findById(userId)
                .flatMap(refreshTokenRepository::findByUser);

        RefreshToken refreshToken;
        if (existingToken.isPresent()) {
            refreshToken = existingToken.get();
            refreshToken.setToken(UUID.randomUUID().toString());
            refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        } else {
            refreshToken = new RefreshToken();
            refreshToken.setUser(userRepository.findById(userId)
                    .orElseThrow(() -> new TokenRefreshException(null, "User not found with id: " + userId)));
            refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
            refreshToken.setToken(UUID.randomUUID().toString());
        }
        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    @Transactional
    public int deleteByUserId(Long userId) {
        return userRepository.findById(userId).map(user -> {
            refreshTokenRepository.deleteByUser(user);
            return 1; // Assuming one token per user
        }).orElse(0);
    }
}
