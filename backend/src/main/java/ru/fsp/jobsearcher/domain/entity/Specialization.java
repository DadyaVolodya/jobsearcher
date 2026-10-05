package ru.fsp.jobsearcher.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "specialization")
public class Specialization {

    @Id
    private String code;

    @Column(name = "industry_code", nullable = false)
    private String industryCode;

    @Column(nullable = false)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
