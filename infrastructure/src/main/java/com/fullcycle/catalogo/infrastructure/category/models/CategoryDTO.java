package com.fullcycle.catalogo.infrastructure.category.models;

import com.fullcycle.catalogo.domain.category.Category;

import java.time.Instant;

public record CategoryDTO(
        String id,
        String name,
        String description,
        Boolean isActive,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {

    @Override
    public Boolean isActive() {
        return isActive != null ? isActive : true;
    }

    public Category toCategory() {
        return Category.with(id(), name(), description(), isActive(), createdAt(), updatedAt(), deletedAt());
    }
}
