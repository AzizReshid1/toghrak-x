// Spring Data's stable paged JSON shape (pageSerializationMode = VIA_DTO on the backend).
export interface Page<T> {
  content: T[];
  page: PageMetadata;
}

export interface PageMetadata {
  number: number; // current page index (0-based)
  size: number; // size requested
  totalElements: number; // total items across all pages
  totalPages: number; // total pages
}
