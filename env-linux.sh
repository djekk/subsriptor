#!/usr/bin/env sh

# Subscriptor environment variables for Linux/macOS
# Usage:
#   chmod +x env-linux.sh
#   source ./env-linux.sh

# App host and port
export SUBSCRIPTOR_HOST="http://localhost:8080"
export SERVER_PORT="8080"

# Database
export DB_URL="jdbc:mysql://localhost:3306/login_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
export DB_USERNAME="root"
export DB_PASSWORD="root"

# Mail
export MAIL_HOST="smtp.gmail.com"
export MAIL_PORT="587"
export MAIL_USERNAME="djekk28@gmail.com"
export MAIL_PASSWORD="zbbn jqmo ixhu bflo"
export MAIL_SMTP_AUTH="true"
export MAIL_SMTP_STARTTLS_ENABLE="true"

# Stripe
export STRIPE_SECRET_KEY="YOUR_STRIPE_TEST_SECRET_KEY"
export STRIPE_WEBHOOK_SECRET="kaka"

# Order confirmation email
export ORDER_CONFIRMATION_EMAIL_ENABLED="true"
export ORDER_CONFIRMATION_EMAIL_FROM="no-reply@domain.com"
export ORDER_CONFIRMATION_EMAIL_ADMIN_EMAILS="djekk28@gmail.com"

# Subscription-check AES keys
export SUBSCRIPTION_CHECK_REQUEST_AES_KEY="1CFA7B20EE3D9B48227DBAA6739AE044"
export SUBSCRIPTION_CHECK_RESPONSE_AES_KEY="DE0216CEF066E18B5AC877A43E982EF6"