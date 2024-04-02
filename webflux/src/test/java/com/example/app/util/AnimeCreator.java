package com.example.app.util;

import com.example.app.model.entity.Anime;

public class AnimeCreator {

    public static Anime createAnimeToBeSaved() {
        return Anime.builder()
            .name("Tensei Shitara Slime Datta Ken")
            .build();
    }

    public static Anime createValidAnime() {
        return Anime.builder()
            .id(1L)
            .name("Tensei Shitara Slime Datta Ken")
            .build();
    }

    public static Anime createdValidUpdatedAnime() {
        return Anime.builder()
            .id(1L)
            .name("Tensei Shitara Slime Datta Ken 2")
            .build();
    }
}