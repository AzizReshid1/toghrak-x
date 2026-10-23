export interface PostResponse {
  id: number;
  slug: string;
  title: string;
  /** HTML content — render with [innerHTML], never with {{ }}. */
  content: string;
  /** Plain-text excerpt (HTML stripped, ≤200 chars). Use in list/card views. */
  excerpt: string;
  /** Always "html". Tells the client how to render `content`. */
  contentType: string;
  coverImage?: string | null;
  categoryId?: number;
  categoryName: string;
  authorId: number;
  authorUsername: string;
  createdAt: string;
  updatedAt?: string | null;
  viewCount?: number;
  imageUrls: string[];
}

/** A post in lists and cards (GET /api/posts, /api/posts/popular): no HTML content or gallery. */
export interface PostSummary {
  id: number;
  slug: string;
  title: string;
  /** Plain-text excerpt (HTML stripped, ≤200 chars). */
  excerpt: string;
  coverImage?: string | null;
  categoryId?: number;
  categoryName: string;
  authorId: number;
  authorUsername: string;
  createdAt: string;
  updatedAt?: string | null;
  viewCount?: number;
}

// request payloads expected by backend DTOs
export interface CreatePostRequest {
  title: string;
  content: string;
  categoryId: number;
  coverImage?: string | null;
  imageUrls?: string[];
}

/** PUT replaces the whole post: send its complete new state. Null cover image / empty gallery removes them. */
export interface UpdatePostRequest {
  title: string;
  content: string;
  categoryId: number;
  coverImage?: string | null;
  imageUrls?: string[];
}
