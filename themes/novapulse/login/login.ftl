<#-- NovaPulse Custom Login Template                                       -->
<#-- Extends Keycloak's FreeMarker context; renders a fully branded page.  -->
<#import "template.ftl" as layout>
<!DOCTYPE html>
<html lang="${locale!'en'}">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="${realm.displayName!'NovaPulse'} — Sign in to your account"/>
    <title>${realm.displayName!'NovaPulse'} — Sign In</title>

    <!-- Google Fonts: Inter -->
    <link rel="preconnect" href="https://fonts.googleapis.com"/>
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin/>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap"
          rel="stylesheet"/>

    <!-- NovaPulse custom styles -->
    <link rel="stylesheet" href="${url.resourcesPath}/css/login.css"/>
</head>
<body>

<!-- Animated starfield background -->
<div class="np-bg" aria-hidden="true">
    <div class="np-stars np-stars--sm"></div>
    <div class="np-stars np-stars--md"></div>
    <div class="np-stars np-stars--lg"></div>
    <div class="np-aurora"></div>
</div>

<!-- Login card -->
<main class="np-main" role="main">
    <div class="np-card" id="kc-form-wrapper">

        <!-- ── Logo & Brand ───────────────────────────────────────────── -->
        <div class="np-header">
            <div class="np-logo-wrap">
                <img src="${url.resourcesPath}/img/logo.svg"
                     alt="${realm.displayName!'NovaPulse'} logo"
                     class="np-logo"
                     width="48" height="48"/>
            </div>
            <h1 class="np-brand">${realm.displayName!'NovaPulse'}</h1>
            <p class="np-tagline">Sign in to continue</p>
        </div>

        <!-- ── Error / Info message ───────────────────────────────────── -->
        <#if message?has_content>
        <div class="np-alert np-alert--${message.type}" role="alert" id="kc-alert-msg">
            <span class="np-alert__icon">
                <#if message.type == 'error'>&#10007;<#elseif message.type == 'warning'>&#9888;<#else>&#10003;</#if>
            </span>
            <span class="np-alert__text">${kcSanitize(message.summary)?no_esc}</span>
        </div>
        </#if>

        <!-- ── Login Form ─────────────────────────────────────────────── -->
        <form id="kc-form-login"
              class="np-form"
              action="${url.loginAction}"
              method="post"
              novalidate>

            <!-- Username -->
            <div class="np-field" id="kc-username-field">
                <label for="username" class="np-label">
                    <#if !realm.loginWithEmailAllowed>Username
                    <#elseif !realm.registrationEmailAsUsername>Username or email
                    <#else>Email</#if>
                </label>
                <div class="np-input-wrap">
                    <span class="np-input-icon np-input-icon--user" aria-hidden="true">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
                             stroke="currentColor" stroke-width="2">
                            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                            <circle cx="12" cy="7" r="4"/>
                        </svg>
                    </span>
                    <input type="text"
                           id="username"
                           name="username"
                           class="np-input"
                           autocomplete="username"
                           autofocus
                           value="${(login.username)!''}"
                           placeholder="Enter your username or email"
                           aria-required="true"/>
                </div>
            </div>

            <!-- Password -->
            <div class="np-field" id="kc-password-field">
                <label for="password" class="np-label">Password</label>
                <div class="np-input-wrap">
                    <span class="np-input-icon np-input-icon--lock" aria-hidden="true">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
                             stroke="currentColor" stroke-width="2">
                            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                            <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                        </svg>
                    </span>
                    <input type="password"
                           id="password"
                           name="password"
                           class="np-input"
                           autocomplete="current-password"
                           placeholder="Enter your password"
                           aria-required="true"/>
                    <button type="button"
                            class="np-toggle-password"
                            aria-label="Toggle password visibility"
                            onclick="togglePassword()">
                        <svg id="eye-open" width="16" height="16" viewBox="0 0 24 24" fill="none"
                             stroke="currentColor" stroke-width="2">
                            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                            <circle cx="12" cy="12" r="3"/>
                        </svg>
                        <svg id="eye-closed" width="16" height="16" viewBox="0 0 24 24" fill="none"
                             stroke="currentColor" stroke-width="2" style="display:none">
                            <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8
                                     a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4
                                     c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07
                                     a3 3 0 1 1-4.24-4.24"/>
                            <line x1="1" y1="1" x2="23" y2="23"/>
                        </svg>
                    </button>
                </div>
            </div>

            <!-- Remember me + Forgot password row -->
            <div class="np-options-row">
                <#if realm.rememberMe && !usernameEditDisabled??>
                <label class="np-checkbox-label" for="rememberMe">
                    <input type="checkbox"
                           id="rememberMe"
                           name="rememberMe"
                           class="np-checkbox"
                           <#if login.rememberMe??>checked</#if>/>
                    <span class="np-checkmark"></span>
                    Remember me
                </label>
                <#else>
                <span></span>
                </#if>

                <#if realm.resetPasswordAllowed>
                <a href="${url.loginResetCredentialsUrl}"
                   class="np-link np-link--forgot"
                   id="kc-forgot-password-link">
                    Forgot password?
                </a>
                </#if>
            </div>

            <!-- Submit -->
            <button type="submit"
                    class="np-btn np-btn--primary"
                    id="kc-login"
                    name="login">
                <span class="np-btn__text">Sign In</span>
                <span class="np-btn__arrow">&#8594;</span>
            </button>

        </form>

        <!-- ── Register link ──────────────────────────────────────────── -->
        <#if realm.registrationAllowed && !registrationDisabled??>
        <p class="np-register-text">
            Don't have an account?
            <a href="${url.registrationUrl}" class="np-link" id="kc-register-link">
                Create one
            </a>
        </p>
        </#if>

        <!-- ── Social / Identity providers ───────────────────────────── -->
        <#if social.providers??>
        <div class="np-divider"><span>or continue with</span></div>
        <div class="np-social-list" id="kc-social-providers">
            <#list social.providers as p>
            <a href="${p.loginUrl}"
               class="np-social-btn"
               id="social-${p.alias}"
               title="${p.displayName!}">
                <img src="${url.resourcesPath}/img/idp/${p.providerId}.svg"
                     onerror="this.style.display='none'"
                     alt="" width="20" height="20" aria-hidden="true"/>
                <span>${p.displayName!}</span>
            </a>
            </#list>
        </div>
        </#if>

    </div><!-- /np-card -->
</main>

<!-- Footer -->
<footer class="np-footer" role="contentinfo">
    <p>&copy; ${.now?string('yyyy')} ${realm.displayName!'NovaPulse'} &mdash; All rights reserved.</p>
</footer>

<!-- Inline JS: password toggle (no external dependency needed) -->
<script>
    function togglePassword() {
        var input  = document.getElementById('password');
        var eyeO   = document.getElementById('eye-open');
        var eyeC   = document.getElementById('eye-closed');
        if (input.type === 'password') {
            input.type = 'text';
            eyeO.style.display = 'none';
            eyeC.style.display = 'inline';
        } else {
            input.type = 'password';
            eyeO.style.display = 'inline';
            eyeC.style.display = 'none';
        }
    }
</script>

</body>
</html>
