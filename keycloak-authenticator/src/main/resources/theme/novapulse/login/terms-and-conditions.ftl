<#-- NovaPulse — Terms & Conditions Acceptance Template                        -->
<!DOCTYPE html>
<html lang="${locale!'en'}">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="${realm.displayName!'NovaPulse'} — Terms and Conditions"/>
    <title>${realm.displayName!'NovaPulse'} — Terms &amp; Conditions</title>

    <!-- Google Fonts: Inter -->
    <link rel="preconnect" href="https://fonts.googleapis.com"/>
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin/>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap"
          rel="stylesheet"/>

    <!-- NovaPulse custom styles -->
    <link rel="stylesheet" href="${url.resourcesPath}/css/login.css"/>

    <style>
        /* ── T&C card layout ─────────────────────────────────────────────── */
        .np-tc-card {
            max-width: 640px;
            width: 100%;
        }

        .np-tc-body {
            background: rgba(255,255,255,0.04);
            border: 1px solid rgba(255,255,255,0.1);
            border-radius: 0.75rem;
            padding: 1.25rem 1.5rem;
            max-height: 340px;
            overflow-y: auto;
            font-size: 0.875rem;
            line-height: 1.7;
            color: rgba(255,255,255,0.75);
            margin-bottom: 1.75rem;
            text-align: left;
            scrollbar-width: thin;
            scrollbar-color: rgba(139,92,246,0.5) transparent;
        }
        .np-tc-body::-webkit-scrollbar { width: 6px; }
        .np-tc-body::-webkit-scrollbar-track { background: transparent; }
        .np-tc-body::-webkit-scrollbar-thumb {
            background-color: rgba(139,92,246,0.5);
            border-radius: 3px;
        }

        .np-tc-body h2 {
            font-size: 1rem;
            font-weight: 600;
            color: rgba(255,255,255,0.9);
            margin: 1rem 0 0.4rem;
        }
        .np-tc-body h2:first-child { margin-top: 0; }

        .np-tc-actions {
            display: flex;
            gap: 1rem;
        }

        .np-btn--danger {
            flex: 1;
            background: rgba(239,68,68,0.15);
            border: 1px solid rgba(239,68,68,0.4);
            color: #fca5a5;
            border-radius: 0.75rem;
            padding: 0.8rem 1rem;
            font-weight: 600;
            cursor: pointer;
            transition: background 0.2s, border-color 0.2s;
            font-family: inherit;
            font-size: 0.9375rem;
        }
        .np-btn--danger:hover {
            background: rgba(239,68,68,0.25);
            border-color: rgba(239,68,68,0.7);
        }

        .np-btn--accept {
            flex: 2;
        }

        .np-scroll-hint {
            text-align: center;
            font-size: 0.75rem;
            color: rgba(255,255,255,0.35);
            margin-bottom: 0.5rem;
        }
    </style>
</head>
<body>

<!-- Animated starfield background -->
<div class="np-bg" aria-hidden="true">
    <div class="np-stars np-stars--sm"></div>
    <div class="np-stars np-stars--md"></div>
    <div class="np-stars np-stars--lg"></div>
    <div class="np-aurora"></div>
</div>

<main class="np-main" role="main">
    <div class="np-card np-tc-card" id="kc-tc-wrapper">

        <!-- ── Header ─────────────────────────────────────────────────────── -->
        <div class="np-header">
            <div class="np-logo-wrap">
                <img src="${url.resourcesPath}/img/logo.svg"
                     alt="${realm.displayName!'NovaPulse'} logo"
                     class="np-logo" width="48" height="48"/>
            </div>
            <h1 class="np-brand">Terms &amp; Conditions</h1>
            <p class="np-tagline">Please read and accept to continue</p>
        </div>

        <!-- ── Error / Info message ────────────────────────────────────────── -->
        <#if message?has_content>
        <div class="np-alert np-alert--${message.type}" role="alert">
            <span class="np-alert__icon">
                <#if message.type == 'error'>&#10007;<#elseif message.type == 'warning'>&#9888;<#else>&#10003;</#if>
            </span>
            <span class="np-alert__text">${kcSanitize(message.summary)?no_esc}</span>
        </div>
        </#if>

        <!-- ── Scroll hint ─────────────────────────────────────────────────── -->
        <p class="np-scroll-hint">Scroll to read the full terms ↓</p>

        <!-- ── Terms Content ──────────────────────────────────────────────── -->
        <div class="np-tc-body" id="np-tc-content" tabindex="0" aria-label="Terms and Conditions">
            <h2>1. Acceptance of Terms</h2>
            <p>By accessing or using ${realm.displayName!'NovaPulse'} services, you agree to be
               bound by these Terms and Conditions and all applicable laws and regulations.
               If you do not agree, you must not use our services.</p>

            <h2>2. Use of Services</h2>
            <p>You agree to use our services only for lawful purposes and in a
               manner that does not infringe the rights of others or restrict their use
               and enjoyment of the service. Prohibited activities include, but are not
               limited to: hacking, spreading malware, engaging in fraudulent activities,
               or violating any applicable local, national, or international law.</p>

            <h2>3. Account Responsibility</h2>
            <p>You are responsible for maintaining the confidentiality of your account
               credentials and for all activities that occur under your account. You must
               notify us immediately of any unauthorised use of your account.</p>

            <h2>4. Privacy Policy</h2>
            <p>Your use of our services is also governed by our Privacy Policy, which
               describes how we collect, use, and protect your personal information. By
               accepting these terms you also accept our Privacy Policy.</p>

            <h2>5. Intellectual Property</h2>
            <p>All content, trademarks, and data on this platform, including but not
               limited to software, databases, text, graphics, icons, and hyperlinks,
               are the property of or licensed to ${realm.displayName!'NovaPulse'} and are
               protected by applicable intellectual property laws.</p>

            <h2>6. Limitation of Liability</h2>
            <p>To the maximum extent permitted by law, ${realm.displayName!'NovaPulse'}
               shall not be liable for any indirect, incidental, special, consequential,
               or punitive damages arising out of your use of the services.</p>

            <h2>7. Changes to Terms</h2>
            <p>We reserve the right to modify these Terms at any time. Continued use of
               the services after changes constitutes acceptance of the new Terms. We will
               notify you of significant changes via email or in-app notification.</p>

            <h2>8. Governing Law</h2>
            <p>These Terms shall be governed by and construed in accordance with
               applicable laws. Any disputes arising under these Terms shall be subject to
               the exclusive jurisdiction of the competent courts.</p>

            <h2>9. Contact</h2>
            <p>If you have questions about these Terms, please contact our support team
               through the platform or via the registered support email address.</p>
        </div>

        <!-- ── Form with Accept / Decline ─────────────────────────────────── -->
        <form id="kc-tc-form"
              class="np-form"
              action="${url.loginAction}"
              method="post">

            <div class="np-tc-actions">
                <button type="submit"
                        name="decision"
                        value="decline"
                        class="np-btn np-btn--danger"
                        id="kc-tc-decline">
                    Decline
                </button>
                <button type="submit"
                        name="decision"
                        value="accept"
                        class="np-btn np-btn--primary np-btn--accept"
                        id="kc-tc-accept">
                    <span class="np-btn__text">I Accept</span>
                    <span class="np-btn__arrow">&#8594;</span>
                </button>
            </div>

        </form>

    </div><!-- /np-card -->
</main>

<footer class="np-footer">
    <p>&copy; ${.now?string('yyyy')} ${realm.displayName!'NovaPulse'} &mdash; All rights reserved.</p>
</footer>

</body>
</html>
