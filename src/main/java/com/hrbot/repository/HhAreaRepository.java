package com.hrbot.repository;

import com.hrbot.model.HhArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HhAreaRepository extends JpaRepository<HhArea, String> {

    Optional<HhArea> findByNameLower(String nameLower);

    boolean existsBy();
}
