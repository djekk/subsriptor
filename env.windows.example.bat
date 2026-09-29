@echo on
setlocal

rem Run this file from an Administrator Command Prompt to save values to the machine environment.
rem It writes persistent system-level environment variables using PowerShell,
rem because setx truncates values containing '&' in JDBC URLs.
rem Open a new Command Prompt after running it.

powershell -NoProfile -ExecutionPolicy Bypass -Command "[Environment]::SetEnvironmentVariable('DB_URL','jdbc:mysql://localhost:3306/login_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true','Machine'); [Environment]::SetEnvironmentVariable('DB_USERNAME','root','Machine'); [Environment]::SetEnvironmentVariable('DB_PASSWORD','root','Machine'); [Environment]::SetEnvironmentVariable('SUBSCRIPTOR_HOST','http://localhost:8080','Machine'); [Environment]::SetEnvironmentVariable('STRIPE_SECRET_KEY','YOUR_STRIPE_TEST_SECRET_KEY','Machine'); [Environment]::SetEnvironmentVariable('STRIPE_WEBHOOK_SECRET','kaka','Machine'); [Environment]::SetEnvironmentVariable('ORDER_CONFIRMATION_EMAIL_ENABLED','true','Machine'); [Environment]::SetEnvironmentVariable('ORDER_CONFIRMATION_EMAIL_FROM','no-reply@domain.com','Machine'); [Environment]::SetEnvironmentVariable('ORDER_CONFIRMATION_EMAIL_ADMIN_EMAILS','djekk28@gmail.com','Machine'); [Environment]::SetEnvironmentVariable('MAIL_HOST','smtp.gmail.com','Machine'); [Environment]::SetEnvironmentVariable('MAIL_PORT','587','Machine'); [Environment]::SetEnvironmentVariable('MAIL_USERNAME','djekk28@gmail.com','Machine'); [Environment]::SetEnvironmentVariable('MAIL_PASSWORD','zbbn jqmo ixhu bflo','Machine'); [Environment]::SetEnvironmentVariable('MAIL_SMTP_AUTH','true','Machine'); [Environment]::SetEnvironmentVariable('MAIL_SMTP_STARTTLS_ENABLE','true','Machine')"

echo.
echo Machine environment variables were saved.
echo Open a new Command Prompt before running the application.
pause