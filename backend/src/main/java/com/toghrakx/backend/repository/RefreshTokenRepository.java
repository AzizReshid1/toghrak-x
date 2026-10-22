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

    // Bulk delete bypasses Hibernate's session: flush pending changes first, then clear the
    // session so no stale RefreshToken objects still point at a user that is about to be deleted.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from RefreshToken rt where rt.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("delete from RefreshToken rt where rt.revoked = true or rt.expiresAt < :now")
    int deleteExpiredOrRevoked(@Param("now") java.time.LocalDateTime now);
}
