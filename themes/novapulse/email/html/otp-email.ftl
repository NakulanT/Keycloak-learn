<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title>${realmName} — Your sign-in code</title>
    <style>
        body {
            margin: 0;
            padding: 0;
            background-color: #0f0c1a;
            font-family: 'Inter', Arial, Helvetica, sans-serif;
            color: #e2e8f0;
            -webkit-font-smoothing: antialiased;
        }
        .wrapper {
            width: 100%;
            max-width: 600px;
            margin: 0 auto;
            padding: 40px 20px;
        }
        .card {
            background: linear-gradient(135deg, rgba(30,20,60,0.95), rgba(15,12,26,0.98));
            border: 1px solid rgba(139,92,246,0.2);
            border-radius: 16px;
            padding: 40px 48px;
            box-shadow: 0 8px 32px rgba(0,0,0,0.5);
        }
        .logo-row {
            text-align: center;
            margin-bottom: 8px;
        }
        .brand-name {
            font-size: 22px;
            font-weight: 700;
            background: linear-gradient(90deg, #8b5cf6, #a78bfa);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
            margin: 0;
        }
        .divider {
            height: 1px;
            background: linear-gradient(90deg, transparent, rgba(139,92,246,0.4), transparent);
            margin: 24px 0;
        }
        h1 {
            font-size: 20px;
            font-weight: 600;
            color: #f1f5f9;
            text-align: center;
            margin: 0 0 8px;
        }
        .subtitle {
            font-size: 14px;
            color: rgba(255,255,255,0.55);
            text-align: center;
            margin: 0 0 32px;
            line-height: 1.5;
        }
        .otp-box {
            background: rgba(139,92,246,0.12);
            border: 1.5px solid rgba(139,92,246,0.35);
            border-radius: 12px;
            text-align: center;
            padding: 20px 24px;
            margin: 0 auto 28px;
            max-width: 280px;
        }
        .otp-label {
            font-size: 11px;
            font-weight: 600;
            letter-spacing: 0.12em;
            text-transform: uppercase;
            color: rgba(255,255,255,0.4);
            margin: 0 0 6px;
        }
        .otp-code {
            font-size: 40px;
            font-weight: 700;
            letter-spacing: 0.22em;
            color: #c4b5fd;
            margin: 0;
            font-feature-settings: "tnum";
        }
        .expiry-note {
            font-size: 13px;
            color: rgba(255,255,255,0.45);
            text-align: center;
            margin: 0 0 28px;
        }
        .expiry-note strong {
            color: rgba(255,255,255,0.7);
        }
        .warning-box {
            background: rgba(251,191,36,0.07);
            border: 1px solid rgba(251,191,36,0.2);
            border-radius: 8px;
            padding: 12px 16px;
            font-size: 12.5px;
            color: rgba(255,255,255,0.5);
            line-height: 1.5;
        }
        .footer {
            text-align: center;
            margin-top: 32px;
            font-size: 11px;
            color: rgba(255,255,255,0.25);
            line-height: 1.6;
        }
    </style>
</head>
<body>
<div class="wrapper">
    <div class="card">

        <!-- Brand header -->
        <div class="logo-row">
            <p class="brand-name">${realmName}</p>
        </div>

        <div class="divider"></div>

        <!-- Title -->
        <h1>Your sign-in verification code</h1>
        <p class="subtitle">
            Use the code below to complete your sign-in.<br/>
            Do not share this code with anyone.
        </p>

        <!-- OTP code block -->
        <div class="otp-box">
            <p class="otp-label">One-Time Code</p>
            <p class="otp-code">${otp}</p>
        </div>

        <!-- Expiry -->
        <p class="expiry-note">
            This code expires in <strong>${otpTtlMinutes} minutes</strong>.
        </p>

        <!-- Security notice -->
        <div class="warning-box">
            &#9888;&nbsp; <strong style="color:rgba(251,191,36,0.75)">Security notice:</strong>
            If you did not request this code, someone may be attempting to access your account.
            You can safely ignore this email — your account remains secure.
        </div>

    </div>

    <!-- Footer -->
    <div class="footer">
        <p>&copy; ${.now?string('yyyy')} ${realmName} &mdash; All rights reserved.</p>
        <p>This is an automated message, please do not reply.</p>
    </div>
</div>
</body>
</html>
