package com.example.app.repository;

import com.example.app.model.entity.Anime;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface AnimeRepository extends ReactiveCrudRepository<Anime, Long> {
}
