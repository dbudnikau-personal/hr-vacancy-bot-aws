package com.hrbot.model;

import jakarta.persistence.*;
import lombok.*;
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

    @ElementCollection
    @CollectionTable(name = "filter_sites", joinColumns = @JoinColumn(name = "filter_id"))
    @Column(name = "site_key")
    private List<String> sites;

    private boolean active;
}
