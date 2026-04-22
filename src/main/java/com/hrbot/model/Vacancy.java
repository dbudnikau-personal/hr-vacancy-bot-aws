package com.hrbot.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vacancies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vacancy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false, unique = true)
    private String url;

    @Column(columnDefinition = "text")
    private String description;

    private String location;
    private String salary;
    private String siteKey;

    @Column(nullable = false)
    private LocalDateTime foundAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private String contentHash;
}
