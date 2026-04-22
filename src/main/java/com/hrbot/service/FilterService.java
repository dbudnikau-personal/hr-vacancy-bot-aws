package com.hrbot.service;

import com.hrbot.model.VacancyFilter;
import com.hrbot.repository.FilterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FilterService {

    private final FilterRepository filterRepository;

    public VacancyFilter save(VacancyFilter filter) {
        return filterRepository.save(filter);
    }

    public VacancyFilter findById(Long id) {
        return filterRepository.findById(id).orElse(null);
    }

    public List<VacancyFilter> getActiveFiltersForChat(Long chatId) {
        return filterRepository.findByChatIdAndActiveTrue(chatId);
    }

    public List<VacancyFilter> getAllActiveFilters() {
        return filterRepository.findAllByActiveTrue();
    }

    public void deactivate(Long filterId) {
        filterRepository.findById(filterId).ifPresent(f -> {
            f.setActive(false);
            filterRepository.save(f);
        });
    }
}
