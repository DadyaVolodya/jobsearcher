package ru.fsp.jobsearcher.application.service;

import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fsp.jobsearcher.domain.entity.Industry;
import ru.fsp.jobsearcher.domain.entity.Specialization;
import ru.fsp.jobsearcher.domain.enums.Grade;
import ru.fsp.jobsearcher.domain.repository.IndustryRepository;
import ru.fsp.jobsearcher.domain.repository.SpecializationRepository;

@Service
@RequiredArgsConstructor
public class DictionaryService {

    private final IndustryRepository industryRepository;
    private final SpecializationRepository specializationRepository;

    @Transactional(readOnly = true)
    public List<Industry> industries() {
        return industryRepository.findAllByOrderBySortOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<Specialization> specializations(String industryCode) {
        if (industryCode == null || industryCode.isBlank()) {
            return specializationRepository.findAllByOrderBySortOrderAsc();
        }
        return specializationRepository.findByIndustryCodeOrderBySortOrderAsc(industryCode);
    }

    public List<String> grades() {
        return Arrays.stream(Grade.values()).map(Enum::name).toList();
    }
}
