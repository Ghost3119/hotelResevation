# Security Audit Scripts

Scripts for the cybersecurity activity.

## 1. Start the defended app

```powershell
docker compose up --build
```

Open the frontend at `http://localhost:5173`.

For ngrok exposure, expose only the frontend:

```powershell
ngrok http 5173
```

Do not expose `8080` directly. The frontend proxies `/api` to the backend.

## 2. SAST / dependency audit

Snyk requires an authenticated CLI:

```powershell
npm install -g snyk
snyk auth
.\security\snyk-scan.ps1
```

Local npm audit:

```powershell
.\security\npm-audit.ps1
```

Reports are written under `security/reports/`.

## 3. DAST with OWASP ZAP

With the stack running:

```powershell
.\security\zap-baseline.ps1 -Target http://host.docker.internal:5173
```

If scanning the ngrok URL:

```powershell
.\security\zap-baseline.ps1 -Target https://YOUR-SUBDOMAIN.ngrok-free.app
```

Reports are written under `security/reports/`.

## 4. Log evidence

During the attack phase, capture backend logs:

```powershell
docker logs -f hotel-backend
```

Useful event markers:

- `HTTP_REQUEST`
- `INJECTION_ATTEMPT`
- `UNAUTHORIZED_ACCESS`
- `CRITICAL_UNAUTHORIZED_ACCESS`
- `AUTH_RATE_LIMIT`
