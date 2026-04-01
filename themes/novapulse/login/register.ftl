<#-- NovaPulse Custom Registration Template                                  -->
<#import "template.ftl" as layout>
<!DOCTYPE html>
<html lang="${locale!'en'}">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="${realm.displayName!'NovaPulse'} — Create your account"/>
    <title>${realm.displayName!'NovaPulse'} — Register</title>

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

<main class="np-main" role="main">
    <div class="np-card" id="kc-form-wrapper">

        <!-- ── Header ───────────────────────────────────────────────────── -->
        <div class="np-header">
            <div class="np-logo-wrap">
                <img src="${url.resourcesPath}/img/logo.svg"
                     alt="${realm.displayName!'NovaPulse'} logo"
                     class="np-logo"
                     width="48" height="48"/>
            </div>
            <h1 class="np-brand">Join ${realm.displayName!'NovaPulse'}</h1>
            <p class="np-tagline">Create your secure account</p>
        </div>

        <!-- ── Message ──────────────────────────────────────────────────── -->
        <#if message?has_content>
        <div class="np-alert np-alert--${message.type}" role="alert">
            <span class="np-alert__icon">
                <#if message.type == 'error'>&#10007;<#elseif message.type == 'warning'>&#9888;<#else>&#10003;</#if>
            </span>
            <span class="np-alert__text">${kcSanitize(message.summary)?no_esc}</span>
        </div>
        </#if>

        <!-- ── Register Form ────────────────────────────────────────────── -->
        <form id="kc-register-form" class="np-form" action="${url.registrationAction}" method="post">
            
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
                <!-- First Name -->
                <div class="np-field">
                    <label for="firstName" class="np-label">First name</label>
                    <div class="np-input-wrap">
                        <input type="text" id="firstName" name="firstName" class="np-input" 
                               style="padding-left: 14px;" value="${(register.formData.firstName)!''}" 
                               placeholder="e.g. Alex" aria-required="true" />
                    </div>
                </div>

                <!-- Last Name -->
                <div class="np-field">
                    <label for="lastName" class="np-label">Last name</label>
                    <div class="np-input-wrap">
                        <input type="text" id="lastName" name="lastName" class="np-input" 
                               style="padding-left: 14px;" value="${(register.formData.lastName)!''}" 
                               placeholder="e.g. Smith" aria-required="true" />
                    </div>
                </div>
            </div>

            <!-- Email -->
            <div class="np-field">
                <label for="email" class="np-label">Email address</label>
                <div class="np-input-wrap">
                    <span class="np-input-icon" aria-hidden="true">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                            <polyline points="22,6 12,13 2,6"/>
                        </svg>
                    </span>
                    <input type="email" id="email" name="email" class="np-input" 
                           autocomplete="email" value="${(register.formData.email)!''}" 
                           placeholder="alex@example.com" aria-required="true" />
                </div>
            </div>

            <#if !realm.registrationEmailAsUsername>
            <!-- Username -->
            <div class="np-field">
                <label for="username" class="np-label">Username</label>
                <div class="np-input-wrap">
                    <span class="np-input-icon" aria-hidden="true">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
                        </svg>
                    </span>
                    <input type="text" id="username" name="username" class="np-input" 
                           autocomplete="username" value="${(register.formData.username)!''}" 
                           placeholder="alex_smith" aria-required="true" />
                </div>
            </div>
            </#if>

            <!-- Password -->
            <div class="np-field">
                <label for="password" class="np-label">Password</label>
                <div class="np-input-wrap">
                    <span class="np-input-icon" aria-hidden="true">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                        </svg>
                    </span>
                    <input type="password" id="password" name="password" class="np-input" 
                           autocomplete="new-password" placeholder="Create a password" aria-required="true" />
                </div>
            </div>

            <!-- Password Confirm -->
            <div class="np-field">
                <label for="password-confirm" class="np-label">Confirm password</label>
                <div class="np-input-wrap">
                    <span class="np-input-icon" aria-hidden="true">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
                        </svg>
                    </span>
                    <input type="password" id="password-confirm" name="password-confirm" class="np-input" 
                           autocomplete="new-password" placeholder="Repeat your password" aria-required="true" />
                </div>
            </div>

            <button type="submit" class="np-btn np-btn--primary">
                Register account
            </button>
        </form>

        <p class="np-register-text">
            Already have an account?
            <a href="${url.loginUrl}" class="np-link">Sign in</a>
        </p>

    </div>
</main>

<footer class="np-footer">
    <p>&copy; ${.now?string('yyyy')} ${realm.displayName!'NovaPulse'} &mdash; All rights reserved.</p>
</footer>

</body>
</html>
