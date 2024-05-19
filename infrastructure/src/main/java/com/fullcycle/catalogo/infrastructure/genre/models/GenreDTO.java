package com.fullcycle.catalogo.infrastructure.genre.models;

import com.fullcycle.catalogo.domain.genre.Genre;

import java.time.Instant;
import java.util.Set;

public record GenreDTO(
        String id,
        String name,
        Boolean isActive,
        Set<String> categoriesId,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {
    public static GenreDTO from(final Genre genre) {
        return new GenreDTO(
                genre.id(),
                genre.name(),
                genre.active(),
                genre.categories(),
                genre.createdAt(),
                genre.updatedAt(),
                genre.deletedAt()
        );
    }
}
