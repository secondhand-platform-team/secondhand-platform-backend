package com.secondhand.authservice.repository;

import com.secondhand.authservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUserId(String userId);

    Optional<User> findByEmailOrPhoneNumber(String email, String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u LEFT JOIN u.userProfile p WHERE " +
            "(:keyword IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.phoneNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:role IS NULL OR u.role = :role)")
    org.springframework.data.domain.Page<User> searchUsers(
            @org.springframework.data.repository.query.Param("keyword") String keyword,
            @org.springframework.data.repository.query.Param("role") com.secondhand.authservice.model.enums.Role role,
            org.springframework.data.domain.Pageable pageable);
}
