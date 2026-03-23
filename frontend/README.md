# Toghrak X - Frontend

A modern Angular-based frontend for **Toghrak X**, a full-stack publishing platform. This application provides a user-friendly interface for managing and publishing content with secure JWT authentication.

## Tech Stack

- **Angular** 14.2.3
- **TypeScript**
- **RESTful API Integration** (Spring Boot backend)
- **JWT Authentication**

## Prerequisites

Before you begin, ensure you have the following installed:

- **Node.js** (v14 or higher)
- **npm** (v6 or higher) or **yarn**
- **Angular CLI** (v14 or higher)

To install Angular CLI globally:
```bash
npm install -g @angular/cli@14
```

## Getting Started

### 1. Installation

Clone the repository and install dependencies:

```bash
git clone https://github.com/AzizTuprak/toghrak-x.git
cd toghrak-x/frontend
npm install
```

### 2. Development Server

Start the development server:

```bash
ng serve
```

Navigate to `http://localhost:4200/`. The application will automatically reload when you make changes to source files.

## Project Structure

```
frontend/
├── src/
│   ├── app/
│   │   ├── components/      # Reusable UI components
│   │   ├── services/        # API and business logic services
│   │   ├── guards/          # Route guards and authentication
│   │   ├── models/          # TypeScript interfaces and models
│   │   └── app.module.ts    # Main app module
│   ├── assets/              # Static assets (images, icons, etc.)
│   └── environments/        # Environment configurations
├── angular.json             # Angular CLI configuration
└── package.json             # Project dependencies
```

## Development Commands

### Code Scaffolding

Generate new Angular components and other structures:

```bash
# Generate a new component
ng generate component component-name

# Generate other structures
ng generate directive|pipe|service|class|guard|interface|enum|module
```

### Build

Build the project for production:

```bash
ng build
```

Build artifacts will be stored in the `dist/toghrak-x/browser` directory.

### Running Tests

#### Unit Tests

Run unit tests via [Karma](https://karma-runner.github.io):

```bash
ng test
```

#### End-to-End Tests

Run end-to-end tests:

```bash
ng e2e
```

> **Note:** You may need to add a package that implements end-to-end testing capabilities first.

## Backend Integration

This frontend communicates with the Toghrak X REST API (Spring Boot backend). Ensure the backend server is running on `http://localhost:8080` (or update the API URL in your environment configuration).

## Authentication

The application uses JWT (JSON Web Token) for secure authentication. Tokens are automatically handled by HTTP interceptors and stored in local storage.

## Troubleshooting

### Port 4200 Already in Use

If port 4200 is in use, specify a different port:

```bash
ng serve --port 4300
```

### Build Errors

Clear node_modules and reinstall:

```bash
rm -rf node_modules
npm install
```

### API Connection Issues

Verify the backend server is running and update the API endpoint in `src/environments/environment.ts`.

## Further Help

For more information about Angular CLI, run:

```bash
ng help
```

Or visit the [Angular CLI Overview and Command Reference](https://angular.io/cli).

## Contributing

For contribution guidelines, please see the main project repository documentation.

## License

Part of the Toghrak X project. See the main repository for license details.