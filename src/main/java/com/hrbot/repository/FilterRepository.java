package com.hrbot.repository;

import com.hrbot.model.VacancyFilter;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FilterRepository extends JpaRepository<VacancyFilter, Long> {

    @EntityGraph(attributePaths = "sites")
    Optional<VacancyFilter> findById(Long id);

    @EntityGraph(attributePaths = "sites")
    List<VacancyFilter> findByChatIdAndActiveTrue(Long chatId);

    @EntityGraph(attributePaths = "sites")
    List<VacancyFilter> findAllByActiveTrue();
}
