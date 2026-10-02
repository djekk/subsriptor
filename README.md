# Java Login & Registration Application

A Spring Boot web application with a login and registration system using MySQL database.

## Features

- ✅ User Registration with validation
- ✅ User Login with session management
- ✅ Password encryption using BCrypt
- ✅ Responsive HTML/CSS/JavaScript UI
- ✅ RESTful API endpoints
- ✅ Session-based authentication
- ✅ User profile management

## Prerequisites

- Java 8 or higher
- Maven 3.6+
- MySQL 5.7+ or MariaDB 10.1+
- Apache 2.4+ (if deploying to Apache)

## Project Structure

```
noi2/
├── src/
│   ├── main/
│   │   ├── java/com/example/loginapp/
│   │   │   ├── model/
│   │   │   │   └── User.java
│   │   │   ├── repository/
│   │   │   │   └── UserRepository.java
│   │   │   ├── service/
│   │   │   │   └── UserService.java
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java
│   │   │   │   └── PageController.java
│   │   │   ├── dto/
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── RegisterRequest.java
│   │   │   │   └── ApiResponse.java
│   │   │   ├── config/
│   │   │   │   └── SecurityConfig.java
│   │   │   └── LoginAppApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── static/
│   │           ├── index.html
│   │           ├── login.html
│   │           ├── register.html
│   │           ├── dashboard.html
│   │           ├── css/
│   │           │   └── style.css
│   │           └── js/
│   │               └── app.js
├── pom.xml
└── database.sql
```

## Setup Instructions

### 1. Database Setup

First, create the database and tables by running the SQL script:

```bash
mysql -u root -p < database.sql
```

Or manually in MySQL:
```sql
CREATE DATABASE login_db;
USE login_db;

-- Run the contents of database.sql
```

### 2. Configure Database Connection

The app reads database settings from environment variables so credentials do not need to be committed into `application.yml`:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/login_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true}
    username: ${DB_USERNAME:}
    password: ${DB_PASSWORD:}
    driver-class-name: com.mysql.cj.jdbc.Driver
```

Examples:

```bat
set DB_URL=jdbc:mysql://localhost:3306/login_db?useSSL=false^&serverTimezone=UTC^&allowPublicKeyRetrieval=true
set DB_USERNAME=app_user
set DB_PASSWORD=your-password
```

```sh
export DB_URL='jdbc:mysql://localhost:3306/login_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
export DB_USERNAME='app_user'
export DB_PASSWORD='your-password'
```

For production, create a dedicated database user instead of `root`, use a strong password, and prefer an SSL-enabled JDBC URL.

### 3. Build the Project

```bash
mvn clean package
```

Or, if you prefer to run without packaging:

```bash
mvn clean install
```

### Email verification and order confirmation emails

New accounts must verify their email before login. Configure the same SMTP settings below and set the public application URL used in verification links:

```bash
set SUBSCRIPTOR_HOST=http://localhost:8080
```

Verification links expire after 24 hours. Users can request another link from the registration flow by calling `POST /api/auth/resend-verification` with `{"email":"user@example.com"}`.

To send an email receipt after successful payment, configure SMTP and enable confirmation emails:

To send an email receipt after successful payment, configure SMTP and enable confirmation emails:

```bash
set ORDER_CONFIRMATION_EMAIL_ENABLED=true
set ORDER_CONFIRMATION_EMAIL_FROM=no-reply@yourdomain.com
set ORDER_CONFIRMATION_EMAIL_ADMIN_EMAILS=admin1@yourdomain.com;admin2@yourdomain.com
set MAIL_HOST=smtp.yourprovider.com
set MAIL_PORT=587
set MAIL_USERNAME=your-smtp-user
set MAIL_PASSWORD=your-smtp-password
```

`ORDER_CONFIRMATION_EMAIL_ADMIN_EMAILS` is optional. When set, those addresses are added as hidden recipients (BCC), so customers only see their own address. Use commas or semicolons as separators.

On Windows, you can also start from the included example file:

```bat
copy env.windows.example.bat env.windows.bat
notepad env.windows.bat
env.windows.bat
rem close this Command Prompt and open a new one
java -jar target\subscriptor-1.0.0.war
```

### 4. Run the Application

Using Maven:
```bash
mvn spring-boot:run
```

Or run the JAR file:
```bash
java -jar target/subscriptor-1.0.0.jar
```

The application will start at `http://localhost:8080`

### 5. Access the Application

- **Home**: http://localhost:8080/
- **Login**: http://localhost:8080/login
- **Register**: http://localhost:8080/register
- **Dashboard**: http://localhost:8080/dashboard (requires login)

## API Endpoints

### Authentication Endpoints

#### Register User
```
POST /api/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "password123",
  "confirmPassword": "password123",
  "firstName": "John",
  "lastName": "Doe"
}
```

Response:
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe"
  }
}
```

#### Login User
```
POST /api/auth/login
Content-Type: application/json

{
  "username": "john_doe",
  "password": "password123"
}
```

Response:
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe"
  }
}
```

#### Logout
```
POST /api/auth/logout
```

#### Check Session
```
GET /api/auth/check-session
```

## Database Schema

### Users Table
```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    is_active TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### Login History Table (Optional)
```sql
CREATE TABLE login_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    logout_time TIMESTAMP NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

## Deployment on Apache

To deploy on Apache with mod_proxy:

1. Build the project: `mvn clean package`
2. Copy the JAR to a deployment directory
3. Run the JAR: `java -jar subscriptor-1.0.0.jar`
4. Configure Apache virtual host:

```apache
<VirtualHost *:80>
    ServerName your-domain.com
    
    ProxyPreserveHost On
    ProxyPass / http://localhost:8080/
    ProxyPassReverse / http://localhost:8080/
    
    ErrorLog ${APACHE_LOG_DIR}/subscriptor-error.log
    CustomLog ${APACHE_LOG_DIR}/subscriptor-access.log combined
</VirtualHost>
```

## Technologies Used

- **Backend**: Spring Boot 2.7.14, Spring Security, Spring Data JPA
- **Database**: MySQL/MariaDB
- **Frontend**: HTML5, CSS3, JavaScript (Vanilla)
- **Security**: BCrypt Password Encoding
- **Build**: Maven

## Security Features

- ✅ Password encryption using BCrypt
- ✅ CSRF protection via Spring Security
- ✅ Session management
- ✅ Input validation
- ✅ Unique username and email constraints
- ✅ Prepared statements (via JPA/Hibernate)

## Troubleshooting

### MySQL Connection Issues
- Ensure MySQL is running
- Check credentials in `application.yml`
- Verify database exists: `CREATE DATABASE login_db;`

### Port Already in Use
- Change port in `application.yml`: `server.port: 8081`

### Maven Build Fails
- Clear Maven cache: `mvn clean`
- Update Maven: `mvn -v`
- Check Java version: `java -version`

## Future Enhancements

- Email verification
- Password reset functionality
- Two-factor authentication (2FA)
- OAuth2 integration (Google, GitHub)
- Role-based access control (RBAC)
- User profile picture upload
- Activity logging

## License

MIT License - Feel free to use and modify!

## Support

For issues or questions, please check the logs or contact the development team.


ll