package ru.fsp.jobsearcher.application.policy;

import ru.fsp.jobsearcher.domain.enums.Grade;

public final class CategoryCodes {

    private CategoryCodes() {
    }

    public static String of(String industry, String specialization, Grade grade) {
        return industry + ":" + specialization + ":" + grade.name();
    }
}
