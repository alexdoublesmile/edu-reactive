package com.example.app.model.entity;

import lombok.*;
import org.springframework.data.annotation.Id;

@Getter
@ToString
@EqualsAndHashCode
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
public class Anime {
    @Id
    private Long id;
    private String name;
}
