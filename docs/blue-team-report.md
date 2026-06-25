# Blue Team Report - Hotel Manager

## 1. Scope

Application: Hotel Manager PMS.

Stack:

- Backend: Spring Boot 3, Java 21, Spring Security, PostgreSQL.
- Frontend: React 18, TypeScript, Tailwind, nginx.
- Exposure: ngrok over frontend port `5173`; backend is consumed through `/api` proxy.

## 2. Hardening Implemented

### HTTP headers

Configured in `frontend/nginx.conf` and backend Spring Security:

- `Content-Security-Policy`
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Permissions-Policy`
- `Cross-Origin-Opener-Policy`
- `Cross-Origin-Resource-Policy`
- `Strict-Transport-Security`

### Authentication and sessions

- Passwords are stored with BCrypt.
- Access token JWT expiration: 15 minutes.
- Refresh token is stored in an HttpOnly cookie.
- Refresh tokens are rotated on every use.
- Reuse of a revoked refresh token revokes active tokens for that user.
- Login and refresh endpoints have rate limiting.

### Authorization

- Role-based access control with `@PreAuthorize`.
- Sensitive guest data has a dedicated `PRIVACY_OFFICER` role.
- Reads of full personal data are audited.

### Input validation and SQL injection mitigation

- DTO validation with Bean Validation.
- Repository queries use JPA parameters, not string-concatenated SQL.
- Database constraints defend reservation overlaps and invalid states.

### Error handling

- Generic 500 responses no longer expose internal exception messages.
- Detailed stack traces remain only in server logs.

## 3. Monitoring and Detection

Backend logs include:

```text
HTTP_REQUEST method=GET path=/api/... ip=... status=200 durationMs=...
INJECTION_ATTEMPT method=GET path=/api/guests ip=... status=400 signals=[SQLI]
UNAUTHORIZED_ACCESS method=GET path=/api/... ip=... status=401
CRITICAL_UNAUTHORIZED_ACCESS method=GET path=/api/users ip=... status=403
AUTH_RATE_LIMIT method=POST path=/api/auth/login ip=... attempts=30 windowSeconds=60
```

Evidence command:

```powershell
docker logs -f hotel-backend
```

## 4. SAST / Dependency Audit

Run:

```powershell
.\security\snyk-scan.ps1
.\security\npm-audit.ps1
```

Attach generated files from:

```text
security/reports/
```

Document each finding:

| Tool | Severity | Finding | Evidence | Mitigation |
| --- | --- | --- | --- | --- |
| Snyk | TBD | TBD | `security/reports/snyk.json` | TBD |
| npm audit | 5 moderate, 1 high, 1 critical | Dependency vulnerabilities in frontend dependency tree | `security/reports/npm-audit.json` | Review affected packages and apply non-breaking upgrades first; avoid `npm audit fix --force` until regression tests are run. |

## 5. DAST with OWASP ZAP

Run locally:

```powershell
docker compose up --build
.\security\zap-baseline.ps1 -Target http://host.docker.internal:5173
```

Run against ngrok:

```powershell
ngrok http 5173
.\security\zap-baseline.ps1 -Target https://YOUR-SUBDOMAIN.ngrok-free.app
```

Attach:

- `security/reports/zap-baseline.html`
- `security/reports/zap-baseline.json`
- `security/reports/zap-baseline.md`

## 6. Exposure Checklist

Before publishing with ngrok:

- Change `POSTGRES_PASSWORD`.
- Change `JWT_SECRET`.
- Set `REFRESH_TOKEN_SECURE=true` when using HTTPS/ngrok.
- Set `APP_SECURITY_EXPOSE_DOCS=false`.
- Confirm frontend uses `/api`, not `localhost:8080`.
- Expose only port `5173`: `ngrok http 5173`.
- Keep backend logs open: `docker logs -f hotel-backend`.

## 7. Known Residual Risks

- Access token is currently stored in `localStorage`; this is acceptable for the demo but would be weaker than a backend-for-frontend or HttpOnly access-cookie design.
- The CSP allows `style-src 'unsafe-inline'` because the current UI stack may emit inline styles; this should be tightened further in a production hardening pass.
- The Docker Compose file is for classroom/demo use, not production.
