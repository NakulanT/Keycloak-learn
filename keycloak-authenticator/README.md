# NovaPulse Custom Keycloak Authenticator

A custom **username/password authenticator SPI** and fully **branded login theme** for Keycloak 24.x.

![Dark space login page with gradient accent, animated starfield, and NovaPulse logo](./docs/preview.png)

---

## What's Inside

| Component | Path | Description |
|-----------|------|-------------|
| Authenticator | `src/main/java/.../CustomUsernamePasswordAuthenticator.java` | Core auth logic: validation, brute-force protection, audit logging |
| Factory | `src/main/java/.../CustomUsernamePasswordAuthenticatorFactory.java` | SPI registration under `novapulse-username-password` |
| Login template | `src/main/resources/theme/novapulse/login/login.ftl` | Branded FreeMarker page |
| CSS | `...resources/css/login.css` | Dark space theme, gradient accent, animations |
| Logo | `...resources/img/logo.svg` | SVG NovaPulse logo mark |
| Messages | `...messages/messages_en.properties` | Custom English UI copy |
| Docker Compose | `docker-compose.yml` | One-command local dev environment |

---

## Prerequisites

- **Java 17+**
- **Maven 3.9+**
- **Docker & Docker Compose** (for local testing)

---

## Build

```bash
cd /path/to/Custom-Keycloak
mvn clean package -DskipTests
# Output: target/novapulse-authenticator.jar
```

---

## Run Locally (Docker)

```bash
# 1. Build the JAR
mvn clean package -DskipTests

# 2. Start Keycloak (mounts JAR + theme automatically)
docker compose up -d

# 3. Tail logs
docker compose logs -f keycloak
```

Keycloak will be available at **http://localhost:8080**.  
Admin console: **http://localhost:8080/admin** (`admin` / `admin`).

---

## Deploy to Existing Keycloak

### 1 — Copy the JAR

```bash
cp target/novapulse-authenticator.jar $KEYCLOAK_HOME/providers/
```

### 2 — Copy the Theme

```bash
cp -r src/main/resources/theme/novapulse $KEYCLOAK_HOME/themes/
```

### 3 — Rebuild Keycloak

```bash
$KEYCLOAK_HOME/bin/kc.sh build
$KEYCLOAK_HOME/bin/kc.sh start   # or start-dev for development
```

---

## Configure in Admin Console

### Enable the Theme

1. Log into **Admin Console** → select your Realm
2. **Realm Settings → Themes** tab
3. Set **Login Theme** → `novapulse`
4. Click **Save**

### Wire the Authenticator

1. Go to **Authentication → Flows**
2. Click **Duplicate** next to the **Browser** flow → name it `NovaPulse Browser`
3. In the duplicated flow, find the **Username Password Form** step → click **⚙ → Delete**
4. Click **Add step** → search for **NovaPulse: Username Password Form** → Add
5. Set it to **Required**
6. Go to **Authentication → Bindings** → set **Browser Flow** to `NovaPulse Browser`
7. **Save**

---

## Authenticator Features

| Feature | Detail |
|---------|--------|
| Input validation | Blank username / password guard before any DB call |
| User lookup | By username first, then email (if `loginWithEmailAllowed`) |
| Brute-force awareness | Reads `isPermanentlyLockedOut` + `isTemporarilyDisabled`, increments / resets counter |
| Audit logging | JBoss Logger `[NovaPulse AUDIT]` entries for every attempt (success or failure) |
| Disabled accounts | Explicit check + user-facing error message |
| Custom error messages | Keycloak message keys mapped to friendly copy in `messages_en.properties` |

---

## Theme Features

| Feature | Detail |
|---------|--------|
| Animated starfield | Three CSS-animated star layers + aurora glow |
| Glassmorphism card | `backdrop-filter: blur` with gradient border |
| Inter font | Loaded from Google Fonts |
| Password toggle | Show/hide without any JS framework |
| Custom checkbox | Pure CSS remember-me checkbox with gradient fill |
| Social IdP support | Shows third-party login buttons when configured |
| Responsive | Mobile-first, tested down to 320 px wide |
| ARIA / Accessibility | `role`, `aria-label`, `aria-required` on all interactive elements |

---

## Development Tips

### Hot-reload themes (no restart needed)

Add these flags to `kc.sh start-dev`:

```
--spi-theme-static-max-age=-1 \
--spi-theme-cache-themes=false \
--spi-theme-cache-templates=false
```

Or set as environment variables:

```bash
KC_SPI_THEME_STATIC_MAX_AGE=-1
KC_SPI_THEME_CACHE_THEMES=false
KC_SPI_THEME_CACHE_TEMPLATES=false
```

### Logging

The authenticator logs under the logger name:

```
com.novapulse.keycloak.auth.CustomUsernamePasswordAuthenticator
```

Set level to `DEBUG` in Keycloak's `conf/quarkus.properties`:

```properties
quarkus.log.category."com.novapulse".level=DEBUG
```

---

## Project Structure

```
Custom-Keycloak/
├── pom.xml
├── docker-compose.yml
├── README.md
└── src/
    └── main/
        ├── java/
        │   └── com/novapulse/keycloak/auth/
        │       ├── CustomUsernamePasswordAuthenticator.java
        │       └── CustomUsernamePasswordAuthenticatorFactory.java
        └── resources/
            ├── META-INF/services/
            │   └── org.keycloak.authentication.AuthenticatorFactory
            └── theme/
                └── novapulse/
                    └── login/
                        ├── theme.properties
                        ├── login.ftl
                        ├── messages/
                        │   └── messages_en.properties
                        └── resources/
                            ├── css/
                            │   └── login.css
                            └── img/
                                └── logo.svg
```

---

