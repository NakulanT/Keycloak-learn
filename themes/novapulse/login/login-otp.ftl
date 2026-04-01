<#-- NovaPulse OTP Verification Template                                    -->
<#-- Rendered by CustomUsernamePasswordAuthenticator when NOTE_STAGE = "otp" -->
<!DOCTYPE html>
<html lang="${locale!'en'}">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="${realm.displayName!'NovaPulse'} — Verify your identity"/>
    <title>${realm.displayName!'NovaPulse'} — Verify Code</title>

    <!-- Google Fonts: Inter -->
    <link rel="preconnect" href="https://fonts.googleapis.com"/>
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin/>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap"
          rel="stylesheet"/>

    <!-- NovaPulse custom styles (shared with login.ftl) -->
    <link rel="stylesheet" href="${url.resourcesPath}/css/login.css"/>

    <style>
        /* ── OTP-specific overrides ──────────────────────────────────────── */
        .np-otp-hint {
            font-size: 0.875rem;
            color: rgba(255,255,255,0.65);
            text-align: center;
            margin: 0 0 1.5rem;
            line-height: 1.5;
        }
        .np-otp-hint strong {
            color: rgba(255,255,255,0.9);
            font-weight: 600;
        }

        .np-otp-wrap {
            display: flex;
            justify-content: center;
            gap: 0.5rem;
            margin-bottom: 1.75rem;
        }

        /* Single wide input — easier on mobile than split boxes */
        .np-otp-input {
            width: 100%;
            text-align: center;
            font-size: 1.75rem;
            font-weight: 600;
            letter-spacing: 0.4em;
            padding: 0.75rem 1rem;
            border-radius: 0.75rem;
            border: 1.5px solid rgba(255,255,255,0.2);
            background: rgba(255,255,255,0.06);
            color: #fff;
            outline: none;
            transition: border-color 0.2s, box-shadow 0.2s;
            caret-color: #8b5cf6;
        }
        .np-otp-input:focus {
            border-color: #8b5cf6;
            box-shadow: 0 0 0 3px rgba(139,92,246,0.25);
        }
        /* Remove number spinners */
        .np-otp-input::-webkit-outer-spin-button,
        .np-otp-input::-webkit-inner-spin-button { -webkit-appearance: none; }
        .np-otp-input[type=number] { -moz-appearance: textfield; }

        .np-resend-row {
            text-align: center;
            font-size: 0.8125rem;
            color: rgba(255,255,255,0.5);
            margin-top: 1rem;
        }
        .np-resend-row a {
            color: #a78bfa;
            text-decoration: none;
            font-weight: 500;
        }
        .np-resend-row a:hover { text-decoration: underline; }

        .np-back-btn {
            display: block;
            text-align: center;
            margin-top: 1.25rem;
            font-size: 0.8125rem;
            color: rgba(255,255,255,0.45);
            text-decoration: none;
        }
        .np-back-btn:hover { color: rgba(255,255,255,0.75); }

        /* Timer badge */
        .np-timer {
            display: inline-flex;
            align-items: center;
            gap: 0.3rem;
            font-size: 0.75rem;
            color: rgba(255,255,255,0.45);
            margin-top: 0.5rem;
        }
    </style>
</head>
<body>

<!-- Animated starfield background (same as login.ftl) -->
<div class="np-bg" aria-hidden="true">
    <div class="np-stars np-stars--sm"></div>
    <div class="np-stars np-stars--md"></div>
    <div class="np-stars np-stars--lg"></div>
    <div class="np-aurora"></div>
</div>

<!-- OTP card -->
<main class="np-main" role="main">
    <div class="np-card" id="kc-otp-wrapper">

        <!-- ── Logo & Brand ────────────────────────────────────────────── -->
        <div class="np-header">
            <div class="np-logo-wrap">
                <img src="${url.resourcesPath}/img/logo.svg"
                     alt="${realm.displayName!'NovaPulse'} logo"
                     class="np-logo"
                     width="48" height="48"/>
            </div>
            <h1 class="np-brand">Check your email</h1>
            <p class="np-tagline">We sent a 6-digit code to your inbox</p>
        </div>

        <!-- ── Error / Info message ─────────────────────────────────────── -->
        <#if message?has_content>
        <div class="np-alert np-alert--${message.type}" role="alert" id="kc-alert-msg">
            <span class="np-alert__icon">
                <#if message.type == 'error'>&#10007;<#elseif message.type == 'warning'>&#9888;<#else>&#10003;</#if>
            </span>
            <span class="np-alert__text">${kcSanitize(message.summary)?no_esc}</span>
        </div>
        </#if>

        <!-- ── Masked-email hint ────────────────────────────────────────── -->
        <#if maskedEmail?has_content>
        <p class="np-otp-hint">
            A verification code was sent to<br/>
            <strong>${maskedEmail}</strong>
        </p>
        </#if>

        <!-- ── OTP Form ─────────────────────────────────────────────────── -->
        <form id="kc-otp-form"
              class="np-form"
              action="${url.loginAction}"
              method="post"
              novalidate>

            <div class="np-otp-wrap">
                <input type="text"
                       id="otp"
                       name="otp"
                       class="np-otp-input"
                       inputmode="numeric"
                       pattern="[0-9]{6}"
                       maxlength="6"
                       autocomplete="one-time-code"
                       autofocus
                       placeholder="000000"
                       aria-label="One-time passcode"
                       aria-required="true"/>
            </div>

            <!-- Countdown timer (purely presentational) -->
            <div style="text-align:center">
                <span class="np-timer" id="np-countdown" aria-live="polite">
                    &#128337; Code expires in <span id="np-timer-val">5:00</span>
                </span>
            </div>

            <!-- Submit -->
            <button type="submit"
                    class="np-btn np-btn--primary"
                    id="kc-otp-submit"
                    style="margin-top:1.5rem">
                <span class="np-btn__text">Verify Code</span>
                <span class="np-btn__arrow">&#8594;</span>
            </button>

        </form>

        <!-- ── Resend / Back links ──────────────────────────────────────── -->
        <p class="np-resend-row">
            Didn't receive it?
            <a href="${url.loginUrl}" id="kc-resend-link">Resend code</a>
        </p>

        <a href="${url.loginUrl}" class="np-back-btn" id="kc-back-login">
            &#8592; Back to sign in
        </a>

    </div><!-- /np-card -->
</main>

<!-- Footer -->
<footer class="np-footer" role="contentinfo">
    <p>&copy; ${.now?string('yyyy')} ${realm.displayName!'NovaPulse'} &mdash; All rights reserved.</p>
</footer>

<script>
    /* 5-minute countdown matching OTP_TTL_SECONDS = 300 in EmailOtpService */
    (function () {
        var remaining = 300;
        var el = document.getElementById('np-timer-val');
        if (!el) return;
        var timer = setInterval(function () {
            remaining--;
            if (remaining <= 0) {
                clearInterval(timer);
                el.textContent = 'Expired';
                document.getElementById('np-countdown').style.color = '#f87171';
                return;
            }
            var m = Math.floor(remaining / 60);
            var s = remaining % 60;
            el.textContent = m + ':' + (s < 10 ? '0' : '') + s;
        }, 1000);
    })();

    /* Auto-submit when 6 digits entered */
    document.getElementById('otp').addEventListener('input', function () {
        this.value = this.value.replace(/\D/g, '').slice(0, 6);
        if (this.value.length === 6) {
            document.getElementById('kc-otp-form').submit();
        }
    });
</script>

</body>
</html>
