package com.evalx.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "marking_policy")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarkingPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;
}
