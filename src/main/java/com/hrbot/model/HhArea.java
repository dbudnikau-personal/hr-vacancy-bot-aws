package com.hrbot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "hh_areas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HhArea {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    private String parentId;

    @Column(nullable = false)
    private String nameLower;
}
