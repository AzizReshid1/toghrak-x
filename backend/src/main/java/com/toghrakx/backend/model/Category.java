package com.toghrakx.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // e.g. "Events", "Problems", etc.

    @Column(unique = true, length = 150)
    private String slug;

    // Optional description for the category
    private String description;

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (Hibernate.getClass(this) != Hibernate.getClass(o))
            return false;
        Category category = (Category) o;
        return id != null && id.equals(category.id);
    }

    @Override
    public final int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
