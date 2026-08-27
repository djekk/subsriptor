@echo off
rem Copy this file, replace placeholder values, then run it with: call env.windows.example.bat

set SUBSCRIPTOR_HOST=http://localhost:8080

rem Stripe
set STRIPE_SECRET_KEY=your-stripe-secret-key
set STRIPE_WEBHOOK_SECRET=your-stripe-webhook-secret

rem Order confirmation emails
set ORDER_CONFIRMATION_EMAIL_ENABLED=true
set ORDER_CONFIRMATION_EMAIL_FROM=no-reply@yourdomain.com
set ORDER_CONFIRMATION_EMAIL_ADMIN_EMAILS=admin1@yourdomain.com;admin2@yourdomain.com

rem SMTP
set MAIL_HOST=smtp.yourprovider.com
set MAIL_PORT=587
set MAIL_USERNAME=your-smtp-user
set MAIL_PASSWORD=your-smtp-password
set MAIL_SMTP_AUTH=true
set MAIL_SMTP_STARTTLS_ENABLE=true

rem Optional: override only if you do not want the defaults from application.yml
rem set SUBSCRIPTION_CHECK_REQUEST_AES_KEY=1CFA7B20EE3D9B48227DBAA6739AE044
rem set SUBSCRIPTION_CHECK_RESPONSE_AES_KEY=DE0216CEF066E18B5AC877A43E982EF6
