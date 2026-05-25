package com.hrbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
