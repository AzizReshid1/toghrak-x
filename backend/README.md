# Spring Boot Documentation

## Quick Start
To get started with the Spring Boot application, follow the steps below:

1. **Database Setup:**
   - Install MySQL or PostgreSQL.
   - Create a database for your application.
   - Update `application.properties` with your database credentials:
     ```properties
     spring.datasource.url=jdbc:mysql://localhost:3306/yourDatabase
     spring.datasource.username=yourUsername
     spring.datasource.password=yourPassword
     ```
   
2. **Run the Application:**
   - Use the following command to run your application:
     ```bash
     ./mvnw spring-boot:run
     ```

## Project Structure
The project is structured as follows:
- `src/main/java`: Contains Java source files.
- `src/main/resources`: Contains configuration files (application.properties).
- `src/test/java`: Contains test files for unit and integration testing.

## Configuration
Spring Boot provides a variety of properties to customize your application. Here are some key configurations:
- **Server Port:**
  ```properties
  server.port=8080
  ```
- **Logging Level:**
  ```properties
  logging.level.root=INFO
  ```
- **JPA Configuration:**
  ```properties
  spring.jpa.hibernate.ddl-auto=update
  spring.jpa.show-sql=true
  ```

## API Documentation
You can access the API documentation through Swagger by navigating to:
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Development Workflows
To contribute to the project, follow these steps:
1. **Clone the Repository:**
   ```bash
   git clone https://github.com/AzizTuprak/toghrak-x.git
   ```
2. **Create a New Branch:**
   ```bash
   git checkout -b feature-branch
   ```
3. **Make Your Changes and Commit:**
   ```bash
   git add .
   git commit -m "Description of changes"
   ```
4. **Push your Branch:**
   ```bash
   git push origin feature-branch
   ```
5. **Open a Pull Request.**

## Testing
To run tests, use the following command:
```bash
./mvnw test
```

## Building and Deployment with Docker
1. **Build Docker Image:**
   ```bash
   docker build -t your-image-name .
   ```
2. **Run Docker Container:**
   ```bash
   docker run -p 8080:8080 your-image-name
   ```

## Troubleshooting
Common issues:
- **Database Connection Issue:** Ensure that your database is running and the credentials in `application.properties` are correct.
- **Port Already in Use:** Change the `server.port` in `application.properties` to avoid port conflicts.

## Additional Resources
- [Spring Boot Official Documentation](https://spring.io/projects/spring-boot)
- [Swagger Documentation](https://swagger.io/docs/)
- [Docker Documentation](https://docs.docker.com/)