# IMPERIAL EDUCATION: Setup and security guide

## Email verification and password resets
Set these on **Render → Environment**, not in source code:
- `MAIL_ENABLED=true`
- `MAIL_FROM` (verified sender address)
- `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`
- `SMTP_AUTH`, `SMTP_STARTTLS` according to your provider
- `APP_BASE_URL=https://imperial-education.onrender.com` (or your actual HTTPS domain)

Use a trusted transactional SMTP provider. Do not share SMTP credentials in chats. Verify that the provider accepts outbound connections from Render and that test messages arrive.

**Registration:** while email is not configured, new registrations return HTTP 503 rather than creating an account that cannot be verified. Existing accounts continue to log in. New verification links expire after 24 hours; students can resend via `/verify-email.html`. Password reset links expire after 15 minutes via `/forgot-password.html`. Deliverability and rate limits must be tested.

## Invoice configuration
Set `INVOICE_SELLER_NAME`, `INVOICE_SELLER_ADDRESS`, `INVOICE_SELLER_EMAIL` in Render. Only owners of APPROVED payments can open `/invoice.html?order=ID`. Print or **Save as PDF** from the browser. The output is a **payment invoice/record, not a GST tax invoice**. Confirm your business's legal invoice and tax requirements before issuing customer tax invoices. Payment approval remains manual, verified against the actual bank account.

## Admin and session security
Spring Security roles and CSRF checks remain on. Secure session cookies default to true; use `COOKIE_SECURE=false` only for local HTTP development, never production HTTPS. Session timeout defaults to 30 minutes, concurrent session limit is two. Keep `ADMIN_PASSWORD` long and unique. Enable MFA on GitHub, Render and Aiven accounts. In-app MFA, login brute-force throttling and WAF are **not** implemented by this change.

## Database backups
Database is on Aiven PostgreSQL; Render's local disk is ephemeral. Confirm Aiven automatic backup retention and test restore on an isolated database. No automatic backup schedule is created by this change.

For **optional manual encrypted backup**, install compatible `pg_dump`, `gzip` and `age` locally. Set `DB_URL` (PostgreSQL JDBC URL with `sslmode=require`), `DB_USERNAME`, `DB_PASSWORD`, `BACKUP_AGE_RECIPIENT` as local environment variables. Run `bash scripts/backup-db.sh`. Store the resulting `.sql.gz.age` securely offsite, keep decryption keys separate, and practice restoring to a non-production database. Never upload database credentials or backups to GitHub.

## Acceptance checks
1. Existing Student/Admin login, manual UPI checkout, Telegram group permissions and support chat.
2. With SMTP configured: register → verify email → login; resend expired verification; forgot/reset password.
3. My Account → Edit Profile name and My Payments → Print Invoice after approval.
4. Test backup restoration using a separate database.
