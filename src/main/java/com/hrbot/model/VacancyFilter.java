package com.hrbot.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vacancy_filters")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VacancyFilter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long chatId;

    @Column(nullable = false)
    private String name;

    private String keywords;
    private String location;
    private String salaryMin;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "filter_sites", joinColumns = @JoinColumn(name = "filter_id"))
    @Column(name = "site_key")
    private List<String> sites = new ArrayList<>();

    private boolean active;
}
