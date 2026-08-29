package com.toghrakx.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;

@Entity
@Table(name = "post_images", indexes = {
        @Index(name = "idx_post_images_post_id", columnList = "post_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The location/URL of the image
    @Column(nullable = false)
    private String imageUrl;

    // Many images can belong to one Post; using lazy fetching to avoid unnecessary
    // loading
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (Hibernate.getClass(this) != Hibernate.getClass(o))
            return false;
        PostImage postImage = (PostImage) o;
        return id != null && id.equals(postImage.id);
    }

    @Override
    public final int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
