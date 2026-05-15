package com.secondhand.authservice.repository;

import com.secondhand.authservice.model.Addresses;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Addresses, Long> {

    List<Addresses> findByUserUserId(String userId);

    Optional<Addresses> findByIdAndUserUserId(Long id, String userId);

    Optional<Addresses> findByUserUserIdAndMain(String userId, int main);

    boolean existsByUserUserIdAndMain(String userId, int main);
}
