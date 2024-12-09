# LibraryAPI 
A CRUD application for library management. 
Technologies: Java, Spring Boot, Hibernate, PostgreSQL. 
Main functionality: adding and editing books, adding and assigning readers, reservation of books and tracking return deadlines.

HOW TO RUN THE PROJECT:
To run the project, you have two options for configuring the database connection:

1: Use default values (no setup required)
If you don't create an .env file or set environment variables, the application will use the following default values:
Database URL: jdbc:postgresql://localhost:5432/project1
Username: postgres
Password: password
These values are pre-configured in the application.properties file, so you can simply run the application without any changes.

2: Customize via environment variables or .env file
If you prefer to use your own database configuration, 
you can do so by creating an .env file in the project root or setting the following environment variables:
DB_URL=jdbc:postgresql://your_host:your_port/your_database
DB_USERNAME=your_username
DB_PASSWORD=your_password
The application will automatically detect these values and override the defaults.

3)To run the project using Docker, put in terminal:
docker-compose up --build

4)If you see this : "ERROR: Windows named pipe error: Channel is closed. (code: 109)" - 
use this command to run:
mvn spring-boot:run

And after you can go to : http://localhost:8080/people to check my project :)