export interface PostResponse {
  id: number;
  slug: string;
  title: string;
  content: string;
  coverImage?: string | null;
  categoryId?: number;
  categoryName: string;
  authorUsername: string;
  createdAt: string;
  updatedAt?: string | null;
  viewCount?: number;
  imageUrls: string[];
}

// request payloads expected by backend DTOs
export interface CreatePostRequest {
  title: string;
  content: string;
  categoryId: number;
  coverImage?: string | null;
  imageUrls?: string[];
}

export interface UpdatePostRequest {
  title?: string;
  content?: string;
  categoryId?: number;
  coverImage?: string | null;
  imageUrls?: string[];
}
