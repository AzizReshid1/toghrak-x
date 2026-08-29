package com.toghrakx.backend.repository;

import com.toghrakx.backend.model.RefreshToken;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("update RefreshToken rt set rt.revoked = true where rt.user.id = :userId")
    void revokeAllByUserId(Long userId);

    @Modifying
    @Query("delete from RefreshToken rt where rt.revoked = true or rt.expiresAt < :now")
    int deleteExpiredOrRevoked(@Param("now") java.time.LocalDateTime now);
}
