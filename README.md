# LibraryAPI
A CRUD application for library management.
Technologies: Java, Spring Boot, Hibernate, PostgreSQL.
Main functionality: adding and editing books, adding and assigning readers, reservation of books and tracking return deadlines.

HOW TO RUN THE PROJECT:
To run the project, you have two options for configuring the database connection:

1. Use environment variables or .env file
The application no longer has default values for database connection settings. You must provide your own configuration.
You can either create an .env file in the project root or set the following environment variables:
    DB_URL=jdbc:postgresql://your_host:your_port/your_database
    DB_USERNAME=your_username
    DB_PASSWORD=your_password
    RABBITMQ_HOST=rabbitmq  # Replace with your RabbitMQ host if necessary
    RABBITMQ_PORT=5672
    RABBITMQ_USERNAME=guest  # Replace with your RabbitMQ username
    RABBITMQ_PASSWORD=your_password
    MAIL_USERNAME=your_email@gmail.com  # Replace with your email address
    MAIL_PASSWORD=your_password
2. Run the project using Docker
You can set up the project with Docker by running the following commands:
   docker-compose up --build
3. Run the project with Maven
To run the project with Maven, use this command:
   mvn spring-boot:run
4. Once the application is running, you can visit:
   http://localhost:8080/people to check the project.




