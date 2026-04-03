<#-- NovaPulse — New-IP OTP Verification Template                             -->
<!DOCTYPE html>
<html lang="${locale!'en'}">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="${realm.displayName!'NovaPulse'} — New location detected"/>
    <title>${realm.displayName!'NovaPulse'} — New Location Verification</title>

    <!-- Google Fonts: Inter -->
    <link rel="preconnect" href="https://fonts.googleapis.com"/>
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin/>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap"
          rel="stylesheet"/>

    <!-- NovaPulse custom styles -->
    <link rel="stylesheet" href="${url.resourcesPath}/css/login.css"/>

    <style>
        .np-location-badge {
            display: flex;
            align-items: center;
            gap: 0.6rem;
            background: rgba(251,191,36,0.1);
            border: 1px solid rgba(251,191,36,0.3);
            border-radius: 0.75rem;
            padding: 0.75rem 1rem;
            margin-bottom: 1.5rem;
            font-size: 0.875rem;
            color: rgba(255,255,255,0.85);
        }
        .np-location-badge__icon { font-size: 1.1rem; }

        .np-otp-hint {
            font-size: 0.875rem;
            color: rgba(255,255,255,0.65);
            text-align: center;
            margin: 0 0 1.5rem;
            line-height: 1.5;
        }
        .np-otp-hint strong { color: rgba(255,255,255,0.9); font-weight: 600; }

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
            caret-color: #f59e0b;
        }
        .np-otp-input:focus {
            border-color: #f59e0b;
            box-shadow: 0 0 0 3px rgba(245,158,11,0.25);
        }
        .np-otp-input::-webkit-outer-spin-button,
        .np-otp-input::-webkit-inner-spin-button { -webkit-appearance: none; }
        .np-otp-input[type=number] { -moz-appearance: textfield; }

        .np-resend-row {
            text-align: center;
            font-size: 0.8125rem;
            color: rgba(255,255,255,0.5);
            margin-top: 1rem;
        }
        .np-resend-row a { color: #fbbf24; text-decoration: none; font-weight: 500; }
        .np-resend-row a:hover { text-decoration: underline; }

        .np-timer {
            display: inline-flex;
            align-items: center;
            gap: 0.3rem;
            font-size: 0.75rem;
            color: rgba(255,255,255,0.45);
            margin-top: 0.5rem;
        }

        /* Override primary button colour to amber for new-IP context */
        .np-btn--primary { background: linear-gradient(135deg, #f59e0b, #d97706); }
        .np-btn--primary:hover { background: linear-gradient(135deg, #fbbf24, #f59e0b); }
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
    <div class="np-card" id="kc-ip-otp-wrapper">

        <!-- ── Header ─────────────────────────────────────────────────────── -->
        <div class="np-header">
            <div class="np-logo-wrap">
                <img src="${url.resourcesPath}/img/logo.svg"
                     alt="${realm.displayName!'NovaPulse'} logo"
                     class="np-logo" width="48" height="48"/>
            </div>
            <h1 class="np-brand">New location detected</h1>
            <p class="np-tagline">We need to verify it's you</p>
        </div>

        <!-- ── Warning badge ───────────────────────────────────────────────── -->
        <div class="np-location-badge" role="status">
            <span class="np-location-badge__icon">&#9888;&#65039;</span>
            <span>We noticed you're signing in from a location we don't recognise.
                  A verification code has been sent to your email.</span>
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

        <!-- ── Masked-email hint ───────────────────────────────────────────── -->
        <#if maskedEmail?has_content>
        <p class="np-otp-hint">
            Code sent to <strong>${maskedEmail}</strong>
        </p>
        </#if>

        <!-- ── OTP Form ────────────────────────────────────────────────────── -->
        <form id="kc-ip-otp-form"
              class="np-form"
              action="${url.loginAction}"
              method="post"
              novalidate>

            <div style="margin-bottom:1.75rem">
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
                       aria-label="Security verification code"
                       aria-required="true"/>
            </div>

            <!-- Countdown timer -->
            <div style="text-align:center">
                <span class="np-timer" id="np-countdown" aria-live="polite">
                    &#128337; Code expires in <span id="np-timer-val">5:00</span>
                </span>
            </div>

            <button type="submit"
                    class="np-btn np-btn--primary"
                    id="kc-ip-otp-submit"
                    style="margin-top:1.5rem">
                <span class="np-btn__text">Verify &amp; Continue</span>
                <span class="np-btn__arrow">&#8594;</span>
            </button>

        </form>

        <!-- ── Links ──────────────────────────────────────────────────────── -->
        <p class="np-resend-row">
            Didn't receive it?
            <a href="${url.loginUrl}">Resend code</a>
        </p>

    </div><!-- /np-card -->
</main>

<footer class="np-footer">
    <p>&copy; ${.now?string('yyyy')} ${realm.displayName!'NovaPulse'} &mdash; All rights reserved.</p>
</footer>

<script>
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

    document.getElementById('otp').addEventListener('input', function () {
        this.value = this.value.replace(/\D/g, '').slice(0, 6);
        if (this.value.length === 6) {
            document.getElementById('kc-ip-otp-form').submit();
        }
    });
</script>

</body>
</html>
