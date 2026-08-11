package com.example.expensetracker.legal

import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
class PrivacyController {

    @GetMapping("/privacy", produces = [MediaType.TEXT_HTML_VALUE])
    @ResponseBody
    fun privacyPolicy(): String = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Privacy Policy - Expense Tracker</title>
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 800px; margin: 0 auto; padding: 40px 20px; color: #333; line-height: 1.6; }
                h1 { color: #1a1a1a; border-bottom: 2px solid #eee; padding-bottom: 16px; }
                h2 { color: #2a2a2a; margin-top: 32px; }
                p, li { color: #555; }
                ul { padding-left: 24px; }
                .last-updated { color: #888; font-size: 0.9em; margin-bottom: 32px; }
            </style>
        </head>
        <body>
            <h1>Privacy Policy</h1>
            <p class="last-updated">Last updated: August 10, 2026</p>

            <p>This Privacy Policy explains how Expense Tracker ("we", "us", or "our") collects, uses, and protects your personal information when you use our application.</p>

            <h2>1. Information We Collect</h2>
            <p>We collect the following types of information:</p>
            <ul>
                <li><strong>Account information:</strong> email address and password (stored as a secure hash) when you register.</li>
                <li><strong>Financial data:</strong> expense records, amounts, categories, merchants, and budgets that you enter into the application.</li>
                <li><strong>Usage data:</strong> standard server logs including IP address, request timestamps, and endpoint accessed.</li>
                <li><strong>Authentication tokens:</strong> JWT access tokens and refresh tokens used to maintain your session.</li>
            </ul>

            <h2>2. How We Use Your Information</h2>
            <ul>
                <li>To provide and operate the expense tracking service.</li>
                <li>To authenticate your identity and secure your account.</li>
                <li>To generate analytics and reports based on your own expense data.</li>
                <li>To maintain the security and integrity of the platform.</li>
            </ul>

            <h2>3. Data Storage and Security</h2>
            <p>Your data is stored in a secured database. Passwords are never stored in plain text — only bcrypt hashes are retained. We use HTTPS to encrypt data in transit. Access tokens are short-lived and refresh tokens are invalidated on logout.</p>

            <h2>4. Data Sharing</h2>
            <p>We do not sell, trade, or share your personal data with third parties. Your expense data is private to your account and is never shared with other users.</p>

            <h2>5. Third-Party Authentication</h2>
            <p>If you choose to sign in with Google, we receive your Google account email and profile information as provided by Google. We use this solely to create or identify your account. Please review <a href="https://policies.google.com/privacy">Google's Privacy Policy</a> for details on how Google handles your data.</p>

            <h2>6. Data Retention</h2>
            <p>We retain your account and expense data for as long as your account is active. If you request account deletion, your data will be permanently removed within 30 days.</p>

            <h2>7. Your Rights</h2>
            <p>You have the right to:</p>
            <ul>
                <li>Access all data we hold about you.</li>
                <li>Request correction of inaccurate data.</li>
                <li>Request deletion of your account and all associated data.</li>
                <li>Export your expense data.</li>
            </ul>

            <h2>8. Cookies</h2>
            <p>This application does not use cookies. Authentication is handled via HTTP Authorization headers using JWT tokens.</p>

            <h2>9. Changes to This Policy</h2>
            <p>We may update this Privacy Policy from time to time. Any changes will be reflected on this page with an updated "last updated" date.</p>

            <h2>10. Contact</h2>
            <p>If you have any questions about this Privacy Policy or your data, please contact us at <a href="mailto:support@helvio.app">support@helvio.app</a>.</p>
        </body>
        </html>
    """.trimIndent()
}
