# 🐾 Pet Adoption Platform

## Project Overview
The Pet Adoption Platform is a web-based application that connects pet adopters with animal shelters. Users can browse available pets, submit adoption applications, and track their application status. Shelters can manage pet listings and adoption requests, while administrators manage users and platform activities.

## Features
- **Admin:** Manage users, pets, and platform settings.
- **Shelter:** Add and manage pets and review adoption applications.
- **Adopter:** Browse pets, apply for adoption, and track application status.
- **Database:** Store user, pet, and application information.

## Technologies Used
- Java
- Java Servlets
- JDBC
- MySQL
- Apache Tomcat 10
- HTML, CSS, and JavaScript
- Maven

## Requirements
- JDK 17 or compatible Java version
- Apache Maven
- Apache Tomcat 10.1
- MySQL Server
- Git (optional)

## Setup Instructions

1. Clone the repository:

   ```bash
   git clone https://github.com/sharadsingh330/shrd_Pet_New_project.git
   ```

2. Open the project folder:

   ```bash
   cd shrd_Pet_New_project
   ```

3. Configure MySQL and create the project database using the SQL schema provided in the repository.

4. Update the database connection settings according to your local MySQL username and password.

5. Build the project using Maven:

   ```bash
   mvn clean package
   ```

6. Deploy the generated WAR file from the `target` folder to Apache Tomcat's `webapps` directory.

7. Start Tomcat and open the application in your browser using the appropriate local URL and application context path.

## Project Structure

- `src/` — Java source code and web resources
- `java-backend/` — Java backend, if included in the current repository
- `public/` — Public frontend assets
- `README.md` — Project documentation

## Author
Sharad Singh

## License
This project is developed for educational purposes.
