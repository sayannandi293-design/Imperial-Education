# IMPERIAL EDUCATION — buyer remember-me and Render start-up

## Persistent login
- Students can select **Keep me signed in for 30 days on this device** (checked by default). Admin login defaults to unchecked.
- Spring Security's rotating persistent remember-me tokens are backed by the existing PostgreSQL database, so they survive backend restarts.
- The existing HTTP session still expires according to SESSION_TIMEOUT (default 30m). With remember-me enabled and a valid cookie, subsequent visits can authenticate automatically.
- Logout revokes remember-me login. Browser cookie deletion, private browsing, token theft protection, changes to signing key, or token expiry can also require re-login.
- This is **30 days**, not unlimited login until Logout.

## Render configuration
- Recommended: set **REMEMBER_ME_KEY** to a stable, random 32+-character secret in Render Environment. Keep it unchanged between deployments. Do not share or commit it.
- For compatibility with existing setups, the value defaults to **DB_PASSWORD** if REMEMBER_ME_KEY is absent. A separate key is preferred.
- Existing PostgreSQL and `spring.jpa.hibernate.ddl-auto=update` create `persistent_login_tokens` at startup.
- Existing sessions, UPI verification, Telegram courses, and chat code remain unchanged.

## Free Render cold starts
Render Free web services spin down after ~15 minutes without traffic. The first visitor can see a waiting/loading page as the service starts again. Application-side changes cannot eliminate this for a sleeping Spring Boot server. Move the Render service to a paid instance for reliable always-on response.

Official Render documentation: https://render.com/docs/free

## Test
1. Verify deployment is Live and check startup logs.
2. Sign in as Student with Keep me signed in checked; close/reopen browser, revisit /account.html, and confirm that re-authentication works.
3. Wait beyond the short HTTP session timeout; verify remember-me restores the login on a subsequent request.
4. Log out, close/reopen browser, and verify private Account page requires sign-in.
5. Sign in again with Remember Me unchecked and confirm the login does not persist beyond session expiry.
6. Use a separate private window to test Admin login (remember-me unchecked by default).
7. Verify coupon, payment review, Telegram, and notifications as before.
