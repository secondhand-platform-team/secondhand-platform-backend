package com.secondhand.coreservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.secondhand.coreservice.model.Location;

@Repository
public interface LocationRepository extends JpaRepository<Location, String> {
}
