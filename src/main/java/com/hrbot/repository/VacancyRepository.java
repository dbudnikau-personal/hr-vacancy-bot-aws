package com.hrbot.repository;

import com.hrbot.model.Vacancy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VacancyRepository extends JpaRepository<Vacancy, Long> {
    Optional<Vacancy> findByUrl(String url);

    boolean existsByUrl(String url);

    Page<Vacancy> findByTitleContainingIgnoreCaseOrCompanyContainingIgnoreCase(
            String title, String company, Pageable pageable);

    // Filter by siteKey for now — later can be extended to filter by chatId/filterId
    Page<Vacancy> findBySiteKey(String siteKey, Pageable pageable);

    @Query("SELECT v FROM Vacancy v WHERE " +
            "(:keyword IS NULL OR LOWER(v.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(v.company) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Vacancy> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
