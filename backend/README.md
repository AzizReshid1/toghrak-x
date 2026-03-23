# Spring Boot Backend Documentation

## Overview
This document provides comprehensive information about the Spring Boot backend for the Toghrak-X project.

## Table of Contents
1. [Getting Started](#getting-started)
2. [Project Structure](#project-structure)
3. [Configuration](#configuration)
4. [Running the Application](#running-the-application)
5. [Endpoints](#endpoints)

## Getting Started
To get started with the Spring Boot backend, follow the steps below:

1. **Clone the repository**:
   ```bash
   git clone https://github.com/AzizTuprak/toghrak-x.git
   cd toghrak-x/backend
   ```

2. **Install dependencies**:
   ```bash
   ./mvnw install
   ```

3. **Run the application**:
   ```bash
   ./mvnw spring-boot:run
   ```

## Project Structure
- **src/main/java**  - Contains the Java source files.
- **src/main/resources** - Contains application properties and static resources.

## Configuration
The application can be configured using the `application.properties` file located in the `src/main/resources` directory.

## Running the Application
To run the application, use the following command:
```bash
./mvnw spring-boot:run
```

## Endpoints
Here is a list of available endpoints:
- `GET /api/items` - Retrieves all items.
- `POST /api/items` - Creates a new item.
- `GET /api/items/{id}` - Retrieves an item by ID.
- `PUT /api/items/{id}` - Updates an existing item.
- `DELETE /api/items/{id}` - Deletes an item by ID.

## Conclusion
This documentation serves as a guide to understanding and working with the Spring Boot backend of Toghrak-X. For any questions, please refer to the project's GitHub repository or contact the maintainers.