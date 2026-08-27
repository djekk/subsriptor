#!/usr/bin/env sh

# Subscriptor environment variables for Linux/macOS
# Usage:
#   chmod +x env-linux.sh
#   source ./env-linux.sh

# App host and port
export SUBSCRIPTOR_HOST="http://localhost:8080"
export SERVER_PORT="8080"

# Database (Spring Boot overrides)
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/login_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
export SPRING_DATASOURCE_USERNAME="root"
export SPRING_DATASOURCE_PASSWORD="root"
export SPRING_DATASOURCE_DRIVER_CLASS_NAME="com.mysql.cj.jdbc.Driver"

# Mail
export MAIL_HOST=""
export MAIL_PORT="587"
export MAIL_USERNAME=""
export MAIL_PASSWORD=""
export MAIL_SMTP_AUTH="true"
export MAIL_SMTP_STARTTLS_ENABLE="true"

# Stripe
export STRIPE_SECRET_KEY=""
export STRIPE_WEBHOOK_SECRET=""

# Order confirmation email
export ORDER_CONFIRMATION_EMAIL_ENABLED="false"
export ORDER_CONFIRMATION_EMAIL_FROM="no-reply@subscriptor.local"

# Subscription-check AES keys
export SUBSCRIPTION_CHECK_REQUEST_AES_KEY="1CFA7B20EE3D9B48227DBAA6739AE044"
export SUBSCRIPTION_CHECK_RESPONSE_AES_KEY="DE0216CEF066E18B5AC877A43E982EF6"


export ORDER_CONFIRMATION_EMAIL_ADMIN_EMAILS="admin1@yourdomain.com;admin2@yourdomain.com"