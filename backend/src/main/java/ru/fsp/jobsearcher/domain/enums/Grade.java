package ru.fsp.jobsearcher.domain.enums;

import java.util.Locale;
import java.util.Optional;

public enum Grade {
    INTERN(0),
    JUNIOR(1),
    MIDDLE(2),
    SENIOR(3);

    private final int level;

    Grade(int level) {
        this.level = level;
    }

    public int level() {
        return level;
    }

    public Optional<Grade> lower() {
        return switch (this) {
            case INTERN -> Optional.empty();
            case JUNIOR -> Optional.of(INTERN);
            case MIDDLE -> Optional.of(JUNIOR);
            case SENIOR -> Optional.of(MIDDLE);
        };
    }

    public Optional<Grade> higher() {
        return switch (this) {
            case INTERN -> Optional.of(JUNIOR);
            case JUNIOR -> Optional.of(MIDDLE);
            case MIDDLE -> Optional.of(SENIOR);
            case SENIOR -> Optional.empty();
        };
    }

    public static Grade from(String value) {
        return Grade.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
