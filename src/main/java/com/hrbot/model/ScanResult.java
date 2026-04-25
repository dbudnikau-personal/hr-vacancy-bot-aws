package com.hrbot.model;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanResult {
    private int totalFound;
    private List<Vacancy> newVacancies;
    private List<Vacancy> updatedVacancies;
}
