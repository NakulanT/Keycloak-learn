# app.py
from flask import Flask, redirect, request, session, url_for, render_template_string
from keycloak import KeycloakOpenID
import os, secrets

app = Flask(__name__)
app.secret_key = secrets.token_hex(32)

# ── Keycloak config — update these ───────────────────────────────────────────
# ── Keycloak config ───────────────────────────────────────────────────────────
KEYCLOAK_URL      = "http://localhost:8080/"
REALM_NAME        = "demo"
CLIENT_ID         = "sample"
CLIENT_SECRET     = "bQJKQWyfRuPUeMR3a3IZbVeFcmkWy7sR"
REDIRECT_URI      = "http://localhost:3000/callback"

keycloak_openid = KeycloakOpenID(
    server_url=KEYCLOAK_URL,
    client_id=CLIENT_ID,
    realm_name=REALM_NAME,
    client_secret_key=CLIENT_SECRET,
)

# ─────────────────────────────────────────────────────────────────────────────
# Templates
# ─────────────────────────────────────────────────────────────────────────────

HOME_HTML = """
<!DOCTYPE html>
<html>
<head>
  <title>NovaPulse</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: 'Segoe UI', sans-serif; background: #0f0f1a; color: #eee;
           display: flex; flex-direction: column; align-items: center;
           justify-content: center; min-height: 100vh; }
    .card { background: #1a1a2e; border-radius: 16px; padding: 48px 40px;
            width: 380px; text-align: center; box-shadow: 0 8px 32px rgba(0,0,0,0.4); }
    h1 { font-size: 28px; margin-bottom: 8px; color: #fff; }
    p  { color: #aaa; margin-bottom: 32px; font-size: 14px; }
    .btn { display: block; width: 100%; padding: 14px; border-radius: 8px;
           font-size: 15px; font-weight: 600; cursor: pointer;
           text-decoration: none; margin-bottom: 12px; border: none; }
    .btn-primary { background: #6c63ff; color: #fff; }
    .btn-primary:hover { background: #5a52d5; }
    .btn-outline  { background: transparent; color: #6c63ff;
                    border: 2px solid #6c63ff; }
    .btn-outline:hover { background: #6c63ff22; }
  </style>
</head>
<body>
  <div class="card">
    <h1>NovaPulse</h1>
    <p>Secure authentication powered by Keycloak</p>
    <a href="/login"   class="btn btn-primary">Sign In</a>
    <a href="/signup"  class="btn btn-outline">Create Account</a>
  </div>
</body>
</html>
"""

DASHBOARD_HTML = """
<!DOCTYPE html>
<html>
<head>
  <title>Dashboard — NovaPulse</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: 'Segoe UI', sans-serif; background: #0f0f1a; color: #eee;
           display: flex; flex-direction: column; align-items: center;
           justify-content: center; min-height: 100vh; }
    .card { background: #1a1a2e; border-radius: 16px; padding: 48px 40px;
            width: 440px; box-shadow: 0 8px 32px rgba(0,0,0,0.4); }
    h1  { font-size: 24px; margin-bottom: 4px; color: #fff; }
    .sub { color: #aaa; font-size: 13px; margin-bottom: 28px; }
    .info-row { display: flex; justify-content: space-between;
                padding: 10px 0; border-bottom: 1px solid #2a2a3e; font-size: 14px; }
    .info-row:last-of-type { border-bottom: none; }
    .label { color: #888; }
    .value { color: #ccc; font-weight: 500; max-width: 260px;
             overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .badge { background: #6c63ff22; color: #6c63ff; border-radius: 20px;
             padding: 2px 10px; font-size: 12px; }
    .btn-logout { margin-top: 28px; display: block; width: 100%; padding: 12px;
                  background: #e74c3c22; color: #e74c3c; border: 2px solid #e74c3c;
                  border-radius: 8px; font-size: 14px; font-weight: 600;
                  cursor: pointer; text-decoration: none; text-align: center; }
    .btn-logout:hover { background: #e74c3c44; }
  </style>
</head>
<body>
  <div class="card">
    <h1>Welcome back 👋</h1>
    <p class="sub">You are securely signed in</p>

    <div class="info-row">
      <span class="label">Username</span>
      <span class="value">{{ user.get('preferred_username', '—') }}</span>
    </div>
    <div class="info-row">
      <span class="label">Email</span>
      <span class="value">{{ user.get('email', '—') }}</span>
    </div>
    <div class="info-row">
      <span class="label">Name</span>
      <span class="value">{{ user.get('name', '—') }}</span>
    </div>
    <div class="info-row">
      <span class="label">Status</span>
      <span class="value"><span class="badge">✓ Authenticated</span></span>
    </div>

    <a href="/logout" class="btn-logout">Sign Out</a>
  </div>
</body>
</html>
"""

# ─────────────────────────────────────────────────────────────────────────────
# Routes
# ─────────────────────────────────────────────────────────────────────────────

@app.route("/")
def home():
    if "user" in session:
        return redirect(url_for("dashboard"))
    return render_template_string(HOME_HTML)


@app.route("/login")
def login():
    """Redirect to Keycloak login (uses your custom flow with OTP)."""
    auth_url = keycloak_openid.auth_url(
        redirect_uri=REDIRECT_URI,
        scope="openid email profile",
        state=secrets.token_urlsafe(16),
    )
    return redirect(auth_url)


@app.route("/signup")
def signup():
    """
    Redirect to Keycloak registration page.
    Keycloak appends ?tab=register to land directly on the sign-up form.
    """
    auth_url = keycloak_openid.auth_url(
        redirect_uri=REDIRECT_URI,
        scope="openid email profile",
        state=secrets.token_urlsafe(16),
    )
    # Replace /auth with /registrations to land on the signup tab
    registration_url = auth_url.replace("/auth?", "/registrations?")
    return redirect(registration_url)


@app.route("/callback")
def callback():
    """Keycloak redirects here after login with ?code=..."""
    code = request.args.get("code")
    if not code:
        return "Authentication failed — no code returned.", 400

    try:
        # Exchange code for tokens
        token = keycloak_openid.token(
            grant_type="authorization_code",
            code=code,
            redirect_uri=REDIRECT_URI,
        )
        # Fetch user info
        userinfo = keycloak_openid.userinfo(token["access_token"])
        session["user"]          = userinfo
        session["access_token"]  = token["access_token"]
        session["refresh_token"] = token.get("refresh_token", "")
        return redirect(url_for("dashboard"))

    except Exception as e:
        return f"Login failed: {e}", 400


@app.route("/dashboard")
def dashboard():
    if "user" not in session:
        return redirect(url_for("home"))
    return render_template_string(DASHBOARD_HTML, user=session["user"])


@app.route("/logout")
def logout():
    """End the local session and log out from Keycloak too."""
    refresh_token = session.pop("refresh_token", None)
    session.pop("access_token", None)
    session.clear()

    if refresh_token:
        try:
            keycloak_openid.logout(refresh_token)
        except Exception:
            pass  # local session is cleared regardless

    return redirect(url_for("home"))


# ─────────────────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    app.run(debug=True, port=3000)
