# Java Login Application - Setup & Run Guide

## ✅ Build Status
The application has been successfully built! All Java compilation errors have been resolved by removing Lombok compatibility issues.

## 🚀 Quick Start (3 Steps)

### Step 1: Set Up MySQL Database

**Windows (if you have MySQL/MariaDB installed):**

```batch
:: Open Command Prompt as Administrator
cd "C:\Program Files\MySQL\MySQL Server 8.0\bin"
mysql -u root -p < "C:\Users\ezaporozhets\Documents\noi2\database.sql"
```

**Or use MariaDB (you mentioned 10.1.29):**

```batch
cd "C:\Program Files\MariaDB 10.1\bin"
mysql -u root -p < "C:\Users\ezaporozhets\Documents\noi2\database.sql"
```

When prompted, enter your MySQL root password.

**Verify database was created:**
```sql
mysql -u root -p
> SHOW DATABASES;
> USE login_db;
> SHOW TABLES;
```

### Step 2: Update Database Credentials (if needed)

Edit: `c:\Users\ezaporozhets\Documents\noi2\src\main\resources\application.yml`

Change these lines if your MySQL credentials are different:
```yaml
datasource:
  username: root          # Change this to your MySQL username
  password: root          # Change this to your MySQL password
```

### Step 3: Run the Application

**Using the pre-built JAR:**

```batch
cd c:\Users\ezaporozhets\Documents\noi2
java -jar target/login-app-1.0.0.jar
```

**Or use Maven:**

```batch
cd c:\Users\ezaporozhets\Documents\noi2
mvn spring-boot:run
```

You should see:
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::               (v2.7.14)
```

Then navigate to: **http://localhost:8080**

## 📝 Default Credentials

After creating the database, you can register new users via the web interface:

- **Registration Page**: http://localhost:8080/register
- **Login Page**: http://localhost:8080/login
- **Dashboard**: http://localhost:8080/dashboard (after login)

## 🔧 Troubleshooting

### Error: "Connection refused to host: localhost"
**Solution**: MySQL is not running. Start MySQL/MariaDB service:
```batch
:: Windows
net start MySQL80
:: or
net start MariaDB101
```

### Error: "Access denied for user 'root'"
**Solution**: Update the password in `application.yml` to match your MySQL root password.

### Error: "Database 'login_db' doesn't exist"
**Solution**: Run the SQL script again:
```batch
mysql -u root -p < "C:\Users\ezaporozhets\Documents\noi2\database.sql"
```

### Port 8080 already in use
**Solution**: Either stop the other service or change the port in `application.yml`:
```yaml
server:
  port: 8081   # Change to a different port
```

## 📂 Project Files Location

- **Application JAR**: `c:\Users\ezaporozhets\Documents\noi2\target\login-app-1.0.0.jar`
- **Source Code**: `c:\Users\ezaporozhets\Documents\noi2\src\main\java\com\example\loginapp\`
- **Frontend**: `c:\Users\ezaporozhets\Documents\noi2\src\main\resources\static\`
- **Database Schema**: `c:\Users\ezaporozhets\Documents\noi2\database.sql`
- **Config**: `c:\Users\ezaporozhets\Documents\noi2\src\main\resources\application.yml`

## 📊 Database Tables

### users table
Stores user account information:
- username (unique)
- email (unique)
- password (hashed with BCrypt)
- firstName, lastName
- isActive (default: true)
- createdAt, updatedAt (timestamps)

### login_history table (optional)
Tracks login activity for audit purposes.

## 🌐 API Endpoints

### Register
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

### Login
```
POST /api/auth/login
Content-Type: application/json

{
  "username": "john_doe",
  "password": "password123"
}
```

### Logout
```
POST /api/auth/logout
```

### Check Session
```
GET /api/auth/check-session
```

## 🔐 Security Features

✅ Password encryption with BCrypt
✅ Session-based authentication
✅ CSRF protection
✅ Input validation
✅ Unique constraints on username and email
✅ SQL injection prevention (via JPA)

## 📞 Support

If you encounter issues:
1. Check the console output for error messages
2. Verify MySQL is running: `netstat -an | findstr :3306`
3. Check MySQL credentials in `application.yml`
4. Ensure the database was created successfully

---

**Application successfully compiled and packaged!** 🎉
All you need to do is set up MySQL and run the JAR file.
