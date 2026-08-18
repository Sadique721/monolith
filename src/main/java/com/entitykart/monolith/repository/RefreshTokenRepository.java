package com.entitykart.monolith.repository;

import com.entitykart.monolith.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    List<RefreshTokenEntity> findByUserIdAndRevoked(Long userId, boolean revoked);

    void deleteByUserId(Long userId);
}
