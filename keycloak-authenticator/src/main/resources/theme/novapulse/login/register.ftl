<#-- NovaPulse — Smart Registration Template                                    -->
<#-- Detects which FormAction sub-step is active and renders accordingly:       -->
<#--   1. Default            → standard register form                           -->
<#--   2. regOtpStep=VERIFY  → email OTP verification                           -->
<#--   3. tacStep=SHOW       → Terms & Conditions acceptance                    -->
<!DOCTYPE html>
<html lang="${locale!'en'}">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="${realm.displayName!'NovaPulse'} — Create account"/>
    <title>${realm.displayName!'NovaPulse'} — Register</title>

    <link rel="preconnect" href="https://fonts.googleapis.com"/>
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin/>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap"
          rel="stylesheet"/>
    <link rel="stylesheet" href="${url.resourcesPath}/css/login.css"/>

    <style>
        /* ── OTP input ───────────────────────────────────────────────────── */
        .np-otp-input {
            width: 100%; text-align: center;
            font-size: 1.75rem; font-weight: 600; letter-spacing: 0.4em;
            padding: 0.75rem 1rem; border-radius: 0.75rem;
            border: 1.5px solid rgba(255,255,255,0.2);
            background: rgba(255,255,255,0.06); color: #fff;
            outline: none; transition: border-color .2s, box-shadow .2s;
            caret-color: #8b5cf6;
        }
        .np-otp-input:focus { border-color: #8b5cf6; box-shadow: 0 0 0 3px rgba(139,92,246,.25); }
        .np-otp-input::-webkit-outer-spin-button,
        .np-otp-input::-webkit-inner-spin-button { -webkit-appearance: none; }
        .np-otp-input[type=number] { -moz-appearance: textfield; }

        /* ── Hint / sub-text ─────────────────────────────────────────────── */
        .np-otp-hint {
            font-size: .875rem; color: rgba(255,255,255,.65);
            text-align: center; margin: 0 0 1.5rem; line-height: 1.5;
        }
        .np-otp-hint strong { color: rgba(255,255,255,.9); font-weight: 600; }

        /* ── Timer badge ─────────────────────────────────────────────────── */
        .np-timer {
            display: inline-flex; align-items: center; gap: .3rem;
            font-size: .75rem; color: rgba(255,255,255,.45); margin-top: .5rem;
        }

        /* ── T&C scroll box ──────────────────────────────────────────────── */
        .np-tc-body {
            background: rgba(255,255,255,.04);
            border: 1px solid rgba(255,255,255,.1);
            border-radius: .75rem; padding: 1.25rem 1.5rem;
            max-height: 300px; overflow-y: auto;
            font-size: .875rem; line-height: 1.7;
            color: rgba(255,255,255,.75); margin-bottom: 1.75rem;
            text-align: left; scrollbar-width: thin;
            scrollbar-color: rgba(139,92,246,.5) transparent;
        }
        .np-tc-body::-webkit-scrollbar { width: 6px; }
        .np-tc-body::-webkit-scrollbar-thumb {
            background-color: rgba(139,92,246,.5); border-radius: 3px;
        }
        .np-tc-body h2 {
            font-size: 1rem; font-weight: 600; color: rgba(255,255,255,.9);
            margin: 1rem 0 .4rem;
        }
        .np-tc-body h2:first-child { margin-top: 0; }
        .np-tc-actions { display: flex; gap: 1rem; }
        .np-btn--danger {
            flex: 1; background: rgba(239,68,68,.15);
            border: 1px solid rgba(239,68,68,.4); color: #fca5a5;
            border-radius: .75rem; padding: .8rem 1rem;
            font-weight: 600; cursor: pointer;
            transition: background .2s, border-color .2s;
            font-family: inherit; font-size: .9375rem;
        }
        .np-btn--danger:hover { background: rgba(239,68,68,.25); border-color: rgba(239,68,68,.7); }
        .np-btn--accept { flex: 2; }
        .np-scroll-hint {
            text-align: center; font-size: .75rem;
            color: rgba(255,255,255,.35); margin-bottom: .5rem;
        }

        /* ── Resend row ──────────────────────────────────────────────────── */
        .np-resend-row {
            text-align: center; font-size: .8125rem;
            color: rgba(255,255,255,.5); margin-top: 1rem;
        }
        .np-resend-row a { color: #a78bfa; text-decoration: none; font-weight: 500; }
        .np-resend-row a:hover { text-decoration: underline; }
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
    <div class="np-card" id="kc-form-wrapper">

        <!-- ── Header ─────────────────────────────────────────────────────── -->
        <div class="np-header">
            <div class="np-logo-wrap">
                <img src="${url.resourcesPath}/img/logo.svg"
                     alt="${realm.displayName!'NovaPulse'} logo"
                     class="np-logo" width="48" height="48"/>
            </div>
            <#if tacStep?? && tacStep == "SHOW">
                <h1 class="np-brand">Terms &amp; Conditions</h1>
                <p class="np-tagline">Please read and accept to continue</p>
            <#elseif regOtpStep?? && regOtpStep == "VERIFY">
                <h1 class="np-brand">Verify your email</h1>
                <p class="np-tagline">Enter the code we sent to your inbox</p>
            <#else>
                <h1 class="np-brand">Join ${realm.displayName!'NovaPulse'}</h1>
                <p class="np-tagline">Create your secure account</p>
            </#if>
        </div>

        <!-- ── Messages ────────────────────────────────────────────────────── -->
        <#if message?has_content>
        <div class="np-alert np-alert--${message.type}" role="alert">
            <span class="np-alert__icon">
                <#if message.type == 'error'>&#10007;<#elseif message.type == 'warning'>&#9888;<#else>&#10003;</#if>
            </span>
            <span class="np-alert__text">${kcSanitize(message.summary)?no_esc}</span>
        </div>
        </#if>

        <!-- ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ -->
        <!-- STEP A: T&C Acceptance                                            -->
        <!-- ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ -->
        <#if tacStep?? && tacStep == "SHOW">
        <p class="np-scroll-hint">Scroll to read the full terms ↓</p>
        <div class="np-tc-body" tabindex="0">
            <h2>1. Acceptance of Terms</h2>
            <p>By creating an account on ${realm.displayName!'NovaPulse'} you agree to be
               bound by these Terms and Conditions and all applicable laws. If you do not
               agree, please close this page without registering.</p>

            <h2>2. Use of Services</h2>
            <p>You agree to use our services only for lawful purposes and in a manner
               that does not infringe the rights of others or restrict their use and
               enjoyment of the service. Prohibited activities include: hacking, spreading
               malware, engaging in fraudulent activities, or violating any applicable law.</p>

            <h2>3. Account Responsibility</h2>
            <p>You are responsible for maintaining the confidentiality of your account
               credentials and for all activities that occur under your account. Notify us
               immediately of any unauthorised use.</p>

            <h2>4. Privacy Policy</h2>
            <p>Your use of our services is governed by our Privacy Policy, which describes
               how we collect, use, and protect your personal data. By accepting these
               terms you also accept our Privacy Policy.</p>

            <h2>5. Intellectual Property</h2>
            <p>All content, trademarks, and data on this platform are the property of or
               licensed to ${realm.displayName!'NovaPulse'} and are protected by applicable
               intellectual property laws.</p>

            <h2>6. Limitation of Liability</h2>
            <p>To the maximum extent permitted by law, ${realm.displayName!'NovaPulse'}
               shall not be liable for indirect, incidental, or consequential damages
               arising from your use of the services.</p>

            <h2>7. Changes to Terms</h2>
            <p>We reserve the right to modify these Terms at any time. Continued use
               constitutes acceptance. Significant changes will be notified by email.</p>

            <h2>8. Governing Law</h2>
            <p>These Terms shall be governed by applicable laws. Disputes shall be
               subject to the exclusive jurisdiction of competent courts.</p>
        </div>

        <form id="kc-tac-form" class="np-form"
              action="${url.registrationAction}" method="post">
            <div class="np-tc-actions">
                <button type="submit" name="tacDecision" value="decline"
                        class="np-btn np-btn--danger" id="kc-tac-decline">
                    Decline
                </button>
                <button type="submit" name="tacDecision" value="accept"
                        class="np-btn np-btn--primary np-btn--accept" id="kc-tac-accept">
                    <span class="np-btn__text">I Accept &amp; Register</span>
                    <span class="np-btn__arrow">&#8594;</span>
                </button>
            </div>
        </form>

        <!-- ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ -->
        <!-- STEP B: Email OTP Verification                                     -->
        <!-- ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ -->
        <#elseif regOtpStep?? && regOtpStep == "VERIFY">
        <#if maskedEmail?has_content>
        <p class="np-otp-hint">A 6-digit code was sent to<br/>
            <strong>${maskedEmail}</strong></p>
        </#if>

        <form id="kc-reg-otp-form" class="np-form"
              action="${url.registrationAction}" method="post" novalidate>
            <div style="margin-bottom:1.75rem">
                <input type="text" id="otp" name="otp" class="np-otp-input"
                       inputmode="numeric" pattern="[0-9]{6}" maxlength="6"
                       autocomplete="one-time-code" autofocus placeholder="000000"
                       aria-label="Email verification code" aria-required="true"/>
            </div>
            <div style="text-align:center">
                <span class="np-timer" id="np-countdown" aria-live="polite">
                    &#128337; Expires in <span id="np-timer-val">5:00</span>
                </span>
            </div>
            <button type="submit" class="np-btn np-btn--primary"
                    id="kc-reg-otp-submit" style="margin-top:1.5rem">
                <span class="np-btn__text">Verify Email</span>
                <span class="np-btn__arrow">&#8594;</span>
            </button>
        </form>

        <p class="np-resend-row">Wrong email?
            <a href="${url.loginUrl}">Start over</a></p>

        <!-- ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ -->
        <!-- STEP C: Registration Form (default)                                -->
        <!-- ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ -->
        <#else>
        <form id="kc-register-form" class="np-form"
              action="${url.registrationAction}" method="post">

            <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px">
                <!-- First Name -->
                <div class="np-field">
                    <label for="firstName" class="np-label">First name</label>
                    <div class="np-input-wrap">
                        <input type="text" id="firstName" name="firstName" class="np-input"
                               style="padding-left:14px"
                               value="${(register.formData.firstName)!''}"
                               placeholder="e.g. Alex" aria-required="true"/>
                    </div>
                </div>
                <!-- Last Name -->
                <div class="np-field">
                    <label for="lastName" class="np-label">Last name</label>
                    <div class="np-input-wrap">
                        <input type="text" id="lastName" name="lastName" class="np-input"
                               style="padding-left:14px"
                               value="${(register.formData.lastName)!''}"
                               placeholder="e.g. Smith" aria-required="true"/>
                    </div>
                </div>
            </div>

            <!-- Email -->
            <div class="np-field">
                <label for="email" class="np-label">Email address</label>
                <div class="np-input-wrap">
                    <span class="np-input-icon" aria-hidden="true">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
                             stroke="currentColor" stroke-width="2">
                            <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                            <polyline points="22,6 12,13 2,6"/>
                        </svg>
                    </span>
                    <input type="email" id="email" name="email" class="np-input"
                           autocomplete="email"
                           value="${(register.formData.email)!''}"
                           placeholder="alex@example.com" aria-required="true"/>
                </div>
            </div>

            <#if !realm.registrationEmailAsUsername>
            <!-- Username -->
            <div class="np-field">
                <label for="username" class="np-label">Username</label>
                <div class="np-input-wrap">
                    <span class="np-input-icon" aria-hidden="true">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
                             stroke="currentColor" stroke-width="2">
                            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                            <circle cx="12" cy="7" r="4"/>
                        </svg>
                    </span>
                    <input type="text" id="username" name="username" class="np-input"
                           autocomplete="username"
                           value="${(register.formData.username)!''}"
                           placeholder="alex_smith" aria-required="true"/>
                </div>
            </div>
            </#if>

            <!-- Password -->
            <div class="np-field">
                <label for="password" class="np-label">Password</label>
                <div class="np-input-wrap">
                    <span class="np-input-icon" aria-hidden="true">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
                             stroke="currentColor" stroke-width="2">
                            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                            <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                        </svg>
                    </span>
                    <input type="password" id="password" name="password" class="np-input"
                           autocomplete="new-password"
                           placeholder="Create a password" aria-required="true"/>
                </div>
            </div>

            <!-- Confirm Password -->
            <div class="np-field">
                <label for="password-confirm" class="np-label">Confirm password</label>
                <div class="np-input-wrap">
                    <span class="np-input-icon" aria-hidden="true">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
                             stroke="currentColor" stroke-width="2">
                            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                            <polyline points="22 4 12 14.01 9 11.01"/>
                        </svg>
                    </span>
                    <input type="password" id="password-confirm" name="password-confirm"
                           class="np-input" autocomplete="new-password"
                           placeholder="Repeat your password" aria-required="true"/>
                </div>
            </div>

            <button type="submit" class="np-btn np-btn--primary">
                <span class="np-btn__text">Continue</span>
                <span class="np-btn__arrow">&#8594;</span>
            </button>
        </form>

        <p class="np-register-text">
            Already have an account?
            <a href="${url.loginUrl}" class="np-link">Sign in</a>
        </p>
        </#if>

    </div>
</main>

<footer class="np-footer">
    <p>&copy; ${.now?string('yyyy')} ${realm.displayName!'NovaPulse'} &mdash; All rights reserved.</p>
</footer>

<script>
<#if regOtpStep?? && regOtpStep == "VERIFY">
    /* OTP countdown */
    (function () {
        var remaining = 300, el = document.getElementById('np-timer-val');
        if (!el) return;
        var timer = setInterval(function () {
            if (--remaining <= 0) {
                clearInterval(timer);
                el.textContent = 'Expired';
                document.getElementById('np-countdown').style.color = '#f87171';
                return;
            }
            var m = Math.floor(remaining / 60), s = remaining % 60;
            el.textContent = m + ':' + (s < 10 ? '0' : '') + s;
        }, 1000);
    })();
    /* Auto-submit on 6 digits */
    document.getElementById('otp').addEventListener('input', function () {
        this.value = this.value.replace(/\D/g, '').slice(0, 6);
        if (this.value.length === 6) document.getElementById('kc-reg-otp-form').submit();
    });
</#if>
</script>

</body>
</html>
