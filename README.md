# Toghrak X

A full-stack publishing platform with categories, posts, pages, footer links, and admin workflows.

**Stack:** Angular 21 (frontend) + Spring Boot 3.5.9 (backend) + PostgreSQL + JWT
**Live Features:** Post browsing, global search, admin content management, role-based access control, gallery support, and API documentation.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
  - [Backend Setup](#backend-setup)
  - [Frontend Setup](#frontend-setup)
- [Running Locally](#running-locally)
- [Deployment](#deployment)
- [Scripts & Testing](#scripts--testing)
- [Troubleshooting](#troubleshooting)

---

## Overview

Toghrak X is a modern, full-stack publishing platform designed for content creators and site administrators. It provides a responsive public interface for readers to discover content while offering a powerful admin panel for managing posts, pages, categories, and user roles.

Whether you're building a blog, news site, or content hub, Toghrak X gives you the tools to publish, organize, and moderate content at scale with built-in security and performance considerations.

---

## Features

### Public Features
- **Browse posts** by recency, popularity, or category
- **Global search** across posts and content
- **View post details** with embedded images and metadata
- **Static pages** (About, Contact, FAQ, Terms, Privacy) with image galleries
- **Footer links & social media integration**
- **Fully responsive UI** with professional design, optimized for all devices

### Authentication & Authorization
- **JWT-based login** for secure user authentication
- **Refresh token support** for seamless session continuity
- **Role-based access control** (Admin/Editor/Viewer roles)
- **Route guards** for client-side access protection
- **Secure session management** with configurable token expiration

### Admin Features
- **Post management**: Create, edit, delete posts with images and category assignments
- **Category management**: Full CRUD operations with navbar integration
- **Page management**: Manage static pages with gallery images
- **User & role management**: Assign roles and control permissions
- **Site settings & branding**: Customize site-wide configuration and branding
- **Footer & social links**: Customize footer branding and social profiles
- **API documentation**: Interactive OpenAPI/Swagger documentation

### Architecture & Security
- **REST API design** with clear separation of concerns
- **HTML sanitization** using jsoup to prevent XSS attacks
- **Rate limiting** on public endpoints to prevent abuse
- **Database connection pooling** via HikariCP for performance
- **Batch fetching** for optimized database queries
- **Image upload endpoint** with gallery support

---

## Tech Stack

### Frontend
- **Angular 21** with modern best practices
- **TypeScript 5.9**
- **RxJS 7.8** for reactive programming
- **Angular Router** for client-side navigation with lazy loading
- **Bootstrap** for styling framework
- **Custom CSS/SCSS** for unique design elements

### Backend
- **Spring Boot 3.5.9**
- **Java 21** (LTS)
- **Spring Security** with JWT token support (JJWT 0.11.5)
- **Spring Data JPA** with Hibernate ORM
- **PostgreSQL 14+** as primary data store
- **jsoup 1.18** for server-side HTML sanitization
- **Lombok** for reduced boilerplate

### Extras
- **HikariCP** connection pooling (5–15 connections)
- **Rate limiter** for public API protection
- **Multipart upload** (10 MB max file size)
- **OpenAPI/Swagger** (Springdoc 2.8) for API documentation

---

## Prerequisites

Before you begin, ensure you have the following installed on your system:

- **Node.js** 18+ and npm
- **Angular CLI** 21+
- **Java** 21 (JDK)
- **Maven** 3.9+
- **PostgreSQL** 14+ (or compatible)

---

## Installation

### Backend Setup

1. **Create a PostgreSQL database and user:**
   ```bash
   createdb blogdb
   createuser YOUR_DB_USER -P  # You'll be prompted for password
   ```

2. **Configure the application:**
   Edit `backend/src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/blogdb
   spring.datasource.username=YOUR_DB_USER
   spring.datasource.password=YOUR_DB_PASS
   app.jwt.secret=CHANGE_ME_TO_STRONG_SECRET_KEY
   app.jwt.expiration=3600000
   ```

   > **Security Note:** In production, use strong, randomly generated values for `app.jwt.secret` and manage secrets via environment variables or a secrets manager.

3. **Build and run the backend:**
   ```bash
   cd backend
   mvn clean package -DskipTests
   mvn spring-boot:run
   ```

4. **Verify the backend:**
   - API running at `http://localhost:8081`
   - Swagger UI: `http://localhost:8081/swagger-ui/index.html`

### Frontend Setup

1. **Install dependencies:**
   ```bash
   cd frontend
   npm install
   ```

2. **Start development server:**
   ```bash
   ng serve --open
   ```
   - App will open at `http://localhost:4200`
   - Backend is expected at `http://localhost:8081`
   - To change backend URL, edit `src/environments/environment.ts`

3. **Build for production:**
   ```bash
   ng build
   ```
   - Output: `frontend/dist/toghrak-x/browser`

---

## Running Locally

Once both backend and frontend are set up, run them in separate terminal sessions:

**Terminal 1 - Backend:**
```bash
cd backend
mvn spring-boot:run
```
Running on: `http://localhost:8081`

**Terminal 2 - Frontend:**
```bash
cd frontend
ng serve
```
Running on: `http://localhost:4200`

Both services automatically reload on file changes during development.

---

## Deployment

### Backend Deployment
- **Set `app.jwt.secret`** to a strong, randomly-generated value (use a secrets manager in production)
- **Configure CORS** in `SecurityConfig` if frontend is hosted on a different domain
- **Set up image storage**: Configure cloud storage (AWS S3, Azure Blob, etc.) or persistent file system
- **Database migrations**: Consider using Flyway or Liquibase for schema versioning
- **Environment variables**: Use Spring profiles or environment variables for sensitive config

### Frontend Deployment
- Build output directory: `frontend/dist/toghrak-x/browser`
- Deploy this folder to your web server (Nginx, Apache) or CDN (Vercel, Netlify, AWS S3)
- Update `src/environments/environment.prod.ts` with production API endpoint
- Ensure CORS policy allows requests from your frontend domain

### Example Deployment Stack
- **Frontend**: Nginx reverse proxy + CDN (Cloudflare, AWS CloudFront)
- **Backend**: Docker container on Kubernetes or traditional app server
- **Database**: Managed PostgreSQL (AWS RDS, Azure Database, DigitalOcean)
- **Storage**: Cloud storage for images (AWS S3, Azure Blob Storage)

---

## Scripts & Testing

### Backend
```bash
# Run tests
mvn test

# Run with specific profile (dev, staging, prod)
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### Frontend
```bash
# Lint code
ng lint

# Run unit tests
ng test

# Run e2e tests (requires setup)
ng e2e

# Build for production
ng build --configuration production
```

---

## Troubleshooting

### "Unused file" warnings in Angular
- This is cosmetic and doesn't affect functionality
- To silence warnings, exclude in `tsconfig.json`:
  ```json
  "exclude": ["src/**/*.spec.ts"]
  ```

### Database schema errors
- **Issue**: JPA entity mismatch with database schema
- **Solution**:
  - Auto-generate schema: Set `spring.jpa.hibernate.ddl-auto=update` (dev only)
  - For production: Use Flyway/Liquibase migrations
  - Manual: Run SQL migrations in `backend/src/main/resources/db/migration/`

### Backend connection issues
- Verify PostgreSQL is running: `psql -U YOUR_DB_USER -d blogdb`
- Check port 8081 is available: `lsof -i :8081`
- Review logs for connection errors

### Frontend can't reach backend
- Verify backend is running on `http://localhost:8081`
- Check `src/environments/environment.ts` has correct API URL
- Check browser console for CORS errors
- Verify `SecurityConfig` CORS settings match frontend origin

### Build failures
- Clear Maven cache: `mvn clean`
- Clear npm cache: `npm cache clean --force && rm -rf node_modules`
- Check Java version: `java -version` (should be 21+)
- Check Node version: `node -v` (should be 18+)

---

## License & Contributing

For more information on contributing or licensing, please see the repository root directory.

---

**Happy Publishing!**
