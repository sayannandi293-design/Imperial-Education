# Notification Center — IMPERIAL EDUCATION

This feature uses existing PostgreSQL (Aiven) tables for payment and support activity, plus a new `notification_reads` table to persist read/unread status. The Spring application generates notifications from **actual existing records**, so it does not change UPI verification or Telegram entitlement logic.

## Student dashboard
- Open **My Account → Notification Center**.
- See payment submitted/pending, approved/unlocked-course, rejected, and support replies.
- Open related **My Courses**, **Payment History**, or **Support Tickets**.
- Mark individual notices as read. Read status is shared between the same student’s sessions.

## Admin dashboard
- Open the collapsible **Notification Center** topic.
- See pending payment requests, new support tickets and student chat replies.
- Open related **Manual UPI Payment Requests** or **Support Tickets** sections.
- Mark individual notices as read. Admin notifications use one shared read status.

## Updating
The browser polls for updates every **30 seconds while the tab is visible**. This is *not* an email, SMS or browser push notification and does not need third-party credentials or a new service. A browser refresh or **Refresh** button retrieves notices immediately.

## Security
- Authenticated students see only activity associated with their student account; admins see platform notifications.
- All notifications and read actions are protected by existing Spring Security role checks and CSRF tokens.
- User-controlled messages are displayed as text, never injected as HTML.
- Deleting a support ticket removes its derived chat notifications.
- Notification marks are persisted in the existing database; no additional API keys are required.

## Acceptance tests
1. Deploy latest GitHub commit on Render and confirm service is Live.
2. Login as a student and submit a test UPI reference **without approving unless money is actually received**; confirm pending notification.
3. Admin receives pending payment notification; test marking it read. Check read status survives reload.
4. Test payment approval only with independent verification in the bank/UPI app; student gets approval/course-access notice.
5. Student creates a support ticket and sends a reply. Admin sees a new ticket/message notice.
6. Admin responds to that ticket; student receives a reply notice.
7. Open the relevant payment/support page from the notification, and verify it loads.
8. Log in as a separate student, verify no other student's payment or chat details are visible.
9. Ensure existing login, Telegram course access, invoices, checkout and chat deletion still work.

## Limitations
- No device-level push or notification delivery when browser is closed.
- Recent list is capped to 100 items; old records remain in the original Payment and Support sections.
- For large stores, use database-backed notification events or pagination instead of repeatedly scanning all orders and tickets.
- The initial rollout may display historical payment and support records as unread; use **Mark as read** to clear.
