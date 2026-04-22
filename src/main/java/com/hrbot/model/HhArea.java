package com.hrbot.model;

import jakarta.persistence.*;
import lombok.*;

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
