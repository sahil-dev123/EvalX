package com.evalx.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "shifts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_year_id", nullable = false)
    private ExamYear examYear;

    @Column(nullable = false)
    private String name; // e.g., "Shift 1", "Morning Shift"

    @Column(name = "shift_date")
    private LocalDate shiftDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @OneToMany(mappedBy = "shift", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Section> sections;
}
