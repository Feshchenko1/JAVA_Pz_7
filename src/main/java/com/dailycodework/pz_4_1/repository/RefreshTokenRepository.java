package com.dailycodework.pz_4_1.repository;

import com.dailycodework.pz_4_1.model.RefreshToken;
import com.dailycodework.pz_4_1.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByUser(User user);

    @Modifying
    void deleteByUser(User user);
}
