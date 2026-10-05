package ru.fsp.jobsearcher.api.dictionary;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.fsp.jobsearcher.application.service.DictionaryService;
import ru.fsp.jobsearcher.domain.entity.Industry;
import ru.fsp.jobsearcher.domain.entity.Specialization;

@RestController
@RequestMapping("/api/v1/dictionaries")
@RequiredArgsConstructor
@Tag(name = "Dictionaries")
public class DictionaryController {

    private final DictionaryService dictionaryService;

    @GetMapping("/industries")
    public List<Industry> industries() {
        return dictionaryService.industries();
    }

    @GetMapping("/specializations")
    public List<Specialization> specializations(@RequestParam(required = false) String industryCode) {
        return dictionaryService.specializations(industryCode);
    }

    @GetMapping("/grades")
    public Map<String, List<String>> grades() {
        return Map.of("grades", dictionaryService.grades());
    }
}
