package com.entitykart.monolith.service;

import com.entitykart.monolith.event.UserCreatedEvent;
import com.entitykart.monolith.event.PasswordResetEvent;
import com.entitykart.monolith.util.HashUtils;
import com.entitykart.monolith.validation.PasswordValidator;
import com.entitykart.monolith.dto.UserDTO;
import com.entitykart.monolith.dto.LoginRequest;
import com.entitykart.monolith.dto.LoginResponse;
import com.entitykart.monolith.entity.UserEntity;
import com.entitykart.monolith.repository.UserRepository;
import com.entitykart.monolith.repository.AddressRepository;
import com.entitykart.monolith.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

    private final java.util.concurrent.ConcurrentHashMap<String, FailedLoginAttempts> failedAttempts = new java.util.concurrent.ConcurrentHashMap<>();

    private static class FailedLoginAttempts {
        int count;
        LocalDateTime lastAttempt;
        LocalDateTime lockedUntil;
    }

    private void checkBruteForceLock(String email) {
        FailedLoginAttempts attempts = failedAttempts.get(email.toLowerCase());
        if (attempts != null && attempts.lockedUntil != null && attempts.lockedUntil.isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Account temporarily locked due to multiple failed login attempts. Try again in " 
                + java.time.Duration.between(LocalDateTime.now(), attempts.lockedUntil).toMinutes() + " minutes.");
        }
    }

    private void recordLoginSuccess(String email) {
        failedAttempts.remove(email.toLowerCase());
    }

    private void recordLoginFailure(String email) {
        failedAttempts.compute(email.toLowerCase(), (k, v) -> {
            if (v == null) {
                v = new FailedLoginAttempts();
                v.count = 1;
                v.lastAttempt = LocalDateTime.now();
            } else {
                if (v.lastAttempt.plusMinutes(15).isBefore(LocalDateTime.now())) {
                    v.count = 1;
                } else {
                    v.count++;
                }
                v.lastAttempt = LocalDateTime.now();
            }

            if (v.count >= 5) {
                v.lockedUntil = LocalDateTime.now().plusMinutes(30);
                log.warn("Account {} locked due to 5 failed login attempts.", k);
            }
            return v;
        });
    }

    @Transactional
    public UserDTO register(UserDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("User already exists with email: " + dto.getEmail());
        }

        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            throw new RuntimeException("Password is required");
        }

        PasswordValidator.validate(dto.getPassword());

        UserEntity user = new UserEntity();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole("USER");
        user.setActive(true);
        user.setGender(dto.getGender());
        user.setContactNum(dto.getContactNum());
        user.setProfilePicURL(dto.getProfilePicURL());

        UserEntity saved = userRepository.save(user);

        UserCreatedEvent event = new UserCreatedEvent(
                saved.getId(),
                saved.getName(),
                saved.getEmail(),
                saved.getRole());
        try {
            eventPublisher.publishEvent(event);
            log.info("UserCreatedEvent published via ApplicationEventPublisher for userId={}", saved.getId());
        } catch (Exception e) {
            log.error("Failed to publish UserCreatedEvent for userId={}: {}", saved.getId(), e.getMessage());
        }

        return userMapper.toDTO(saved);
    }

    public LoginResponse login(LoginRequest request) {
        checkBruteForceLock(request.getEmail());

        UserEntity user;
        try {
            user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Invalid email or password"));

            if (!user.isActive()) {
                throw new RuntimeException("User account is inactive");
            }

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new RuntimeException("Invalid email or password");
            }
        } catch (Exception e) {
            recordLoginFailure(request.getEmail());
            throw e;
        }

        recordLoginSuccess(request.getEmail());
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());

        return new LoginResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getProfilePicURL(), jwtService.getExpiration());
    }

    @Transactional
    public void forgotPassword(String email) {
        var optUser = userRepository.findByEmail(email);
        if (optUser.isEmpty()) {
            log.warn("Forgot-password request for unregistered email: {}", email);
            return;
        }

        UserEntity user = optUser.get();
        String rawToken = UUID.randomUUID().toString();
        String hashedToken = HashUtils.sha256(rawToken);
        
        user.setResetToken(hashedToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        PasswordResetEvent event = new PasswordResetEvent(
                user.getId(),
                user.getName(),
                user.getEmail(),
                rawToken
        );

        try {
            eventPublisher.publishEvent(event);
            log.info("Password reset event published via ApplicationEventPublisher for userId={}, email={}", user.getId(), email);
        } catch (Exception e) {
            log.error("Failed to publish password-reset event for {}: {}", email, e.getMessage());
        }
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        String hashedToken = HashUtils.sha256(token);
        UserEntity user = userRepository.findByResetToken(hashedToken)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset token has expired");
        }

        PasswordValidator.validate(newPassword);

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return userMapper.toDTO(user);
    }

    @Transactional(readOnly = true)
    public UserEntity getUserEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Transactional
    public UserDTO updateUser(Long id, UserDTO dto) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        if (!user.getEmail().equalsIgnoreCase(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email already in use: " + dto.getEmail());
        }

        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            PasswordValidator.validate(dto.getPassword());
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }
        if (dto.getActive() != null) {
            user.setActive(dto.getActive());
        }
        user.setGender(dto.getGender());
        user.setContactNum(dto.getContactNum());
        user.setProfilePicURL(dto.getProfilePicURL());

        UserEntity saved = userRepository.save(user);
        return userMapper.toDTO(saved);
    }

    @Transactional
    public void deleteUser(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public UserDTO toggleUserStatus(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        user.setActive(!user.isActive());
        UserEntity saved = userRepository.save(user);
        return userMapper.toDTO(saved);
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getUserStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalAdmins", userRepository.countByRole("ADMIN"));
        stats.put("totalActive", userRepository.countByActive(true));
        stats.put("totalSellers", userRepository.countByRole("SELLER"));
        stats.put("totalCities", addressRepository.countDistinctCities());
        return stats;
    }
}
