# IMPERIAL EDUCATION — Offers, coupon prices and buyer notifications

## Admin: publish an offer
1. Sign in to the Admin Panel.
2. Open **Coupon & Discount Manager**.
3. Create a **unique coupon code**, choose a discount percentage from 1% to 90%, and optionally set maximum uses (0 = unlimited).
4. Publishing a new active coupon makes it appear as a course offer and as a new item in existing buyers' **Notification Center**. An old coupon created before this feature might be shown on course cards but does not get a retroactive "new offer" alert.
5. **Disable** the coupon from the manager to remove it from the public offer list and buyer notifications; exhausted limited-use coupons also disappear.

## Buyer view
- Course listings and Course Details show the regular price, conditional discounted price **"With coupon"**, percentage OFF and coupon code beneath the price.
- The website currently highlights the **highest available discount code** for all courses, because existing coupons are platform-wide, not course-specific.
- A buyer can click Buy Now and the code is prefilled and verified at checkout. A coupon does **not** discount the actual price until the server validates it.
- Login as a student → **Notification Center** to see newly published offer notices, mark read and follow **View Offers** to the catalog.
- Notification Center refreshes periodically; this is **not** email/SMS/browser push.

## Price and payment safety
- Coupon discount is calculated and validated again by the backend on checkout; the server determines the UPI amount and usage limits.
- If the user modifies or removes the code, the ordinary price returns.
- When coupon changes the payable amount, the original static UPI QR is hidden; the buyer should use the generated UPI deep link and verify the exact displayed amount.
- Admin must independently verify actual receipt before approving any UPI payment.
- Offers may expire or run out between browsing and checkout; only the server-validated checkout quote is authoritative.

## External configuration
No new environment variables, email provider, or third-party service are needed. Existing Aiven PostgreSQL is used to store coupon records and Notification Center read states.

## Acceptance tests
1. Create a new coupon in Admin; verify it appears in the buyer Notification Center and the public catalog.
2. View a course priced at e.g. ₹1000 with a 20% coupon: regular price ₹1000, "With coupon" price ₹800, code shown.
3. Click Buy Now; verify the coupon pre-fills, is server-validated, and UPI amount is discounted.
4. Disable coupon; reload home page and Student Notification Center, confirm offer disappears. A previously opened checkout must reject expired code.
5. Test a limited-use coupon reaching its maximum; verify it vanishes from active offers.
6. Verify unrelated Student Login, Support Chat, Admin Panel and Telegram course access still function.
7. Confirm Render deploy completes and browser hard-refreshes, then test on the live URL.
