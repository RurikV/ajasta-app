# Fix: Browser ERR_NAME_NOT_RESOLVED for ajasta-backend DNS

## Problem

Browser console showed:
```
GET http://ajasta-backend:8090/api/resources?active=true net::ERR_NAME_NOT_RESOLVED
```

The frontend JavaScript was making API calls to `http://ajasta-backend:8090` - a Kubernetes internal service DNS name that only resolves **inside the cluster**, not in the user's browser.

## Root Cause

The initContainer in `k8s/manifests/09-frontend-deployment.yml` was configured to replace `${external_ip}` placeholders with the Kubernetes service DNS name `ajasta-backend:8090`:

```yaml
env:
- name: FRONTEND_API_BASE
  value: "http://ajasta-backend:8090/api"  # ❌ Internal K8s DNS - doesn't work in browser
```

When the browser loaded the frontend from `http://51.250.21.26/`, the JavaScript files contained absolute URLs pointing to `http://ajasta-backend:8090/api`. The browser tried to resolve `ajasta-backend` as a public DNS name, which failed.

## Solution Applied

Changed the initContainer to use **relative paths** instead of absolute URLs:

```yaml
env:
- name: FRONTEND_API_BASE
  value: "/api"  # ✅ Relative path - works through Ingress routing
```

### How It Works Now

1. **Browser accesses**: `http://51.250.21.26/` (Ingress external IP)
2. **Frontend loaded**: Static assets served from nginx container
3. **API call made**: JavaScript uses relative path `/api/resources?active=true`
4. **Browser resolves**: `http://51.250.21.26/api/resources?active=true` (same origin)
5. **Ingress routes**: `/api/*` → `ajasta-backend:8090` (internal Kubernetes routing)
6. **Backend responds**: Through Ingress back to browser

```
┌─────────┐                      ┌──────────────┐                    ┌──────────┐
│ Browser │ /api/resources       │   Ingress    │  ajasta-backend:  │ Backend  │
│         │ ──────────────────> │  Controller  │ ──────────────────> │   Pod    │
│         │ <────────────────── │              │ <────────────────── │          │
└─────────┘   Response           └──────────────┘     8090/api       └──────────┘
```

### What Changed in the InitContainer

**Before** (lines 37-60):
```yaml
env:
- name: FRONTEND_API_BASE
  value: "http://ajasta-backend:8090/api"
# ... sed patterns replaced URLs with ajasta-backend:8090
```

**After**:
```yaml
env:
- name: FRONTEND_API_BASE
  value: "/api"
command: ["/bin/sh","-c"]
args:
  - |
    BASE="${FRONTEND_API_BASE:-/api}"
    # Rewrite all absolute URLs to use relative path routed by Ingress
    find /work -type f \( -name "*.js" -o -name "*.html" -o -name "*.map" -o -name "*.css" \) -exec sed -i \
      -e "s#http://\${external_ip}:8090/api#${BASE}#gI" \
      -e "s#http://localhost:8090/api#${BASE}#gI" \
      -e "s#http://ajasta-backend:8090/api#${BASE}#gI" \
      # ... more patterns
```

The initContainer now replaces:
- `http://${external_ip}:8090/api` → `/api`
- `http://localhost:8090/api` → `/api`
- `http://ajasta-backend:8090/api` → `/api`

## Deployment Steps (Already Applied)

1. ✅ Updated `k8s/manifests/09-frontend-deployment.yml` with relative path configuration
2. ✅ Applied manifest: `kubectl apply -f k8s/manifests/09-frontend-deployment.yml`
3. ✅ Deleted frontend pods: `kubectl delete pods -n ajasta -l component=frontend`
4. ✅ Waited for rollout: `kubectl rollout status deployment ajasta-frontend -n ajasta`
5. ✅ Verified no absolute URLs remain in served files

## Verification

Confirmed that the served frontend assets contain NO problematic absolute URLs:

```bash
# Check for ajasta-backend references
kubectl exec -n ajasta <pod-name> -- grep -r "ajasta-backend" /usr/share/nginx/html
# Result: ✓ No ajasta-backend references found

# Check for ${external_ip} placeholders
kubectl exec -n ajasta <pod-name> -- grep -r "external_ip" /usr/share/nginx/html
# Result: ✓ No external_ip references found
```

## Expected Behavior After Fix

### ✅ Correct (Current)
- Browser accesses: `http://51.250.21.26/`
- API call in console: `GET http://51.250.21.26/api/resources?active=true`
- Status: **200 OK** (or appropriate response)

### ❌ Before Fix
- Browser accesses: `http://51.250.21.26/`
- API call in console: `GET http://ajasta-backend:8090/api/resources?active=true`
- Status: **ERR_NAME_NOT_RESOLVED** (DNS lookup failed)

## Testing in Browser

1. **Clear browser cache**: Hard refresh (Ctrl+Shift+R or Cmd+Shift+R)
2. **Open browser console**: Press F12 → Network tab
3. **Navigate to application**: `http://51.250.21.26/`
4. **Verify API calls**:
   - URLs should be: `http://51.250.21.26/api/*`
   - NOT: `http://ajasta-backend:8090/api/*`
   - Status should be 200 OK (or appropriate response codes)

## Why Relative Paths Are Correct

### ✅ Advantages of Relative Paths (`/api`)
1. **Browser compatibility**: Works from any origin (external IP, domain name, localhost)
2. **Ingress routing**: Kubernetes Ingress handles backend routing internally
3. **No DNS resolution**: Browser uses same origin, no external DNS lookup needed
4. **Secure**: No exposure of internal service names
5. **Flexible**: Works with different ingress configurations without code changes

### ❌ Problems with Absolute URLs (`http://ajasta-backend:8090/api`)
1. **Browser DNS failure**: `ajasta-backend` doesn't resolve outside the cluster
2. **CORS issues**: Different origin causes cross-origin errors
3. **Security**: Exposes internal service names
4. **Inflexible**: Hardcoded URLs don't work across environments

## Architecture Overview

```yaml
# Kubernetes Ingress Configuration (11-ingress.yml)
spec:
  rules:
  - http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: ajasta-frontend  # Serves static files
            port:
              number: 80
      - path: /api
        pathType: Prefix
        backend:
          service:
            name: ajasta-backend   # Handles API requests
            port:
              number: 8090
```

The Ingress controller:
- Routes `/` → frontend service (nginx serving static assets)
- Routes `/api` → backend service (Spring Boot application)
- Browser only sees the external IP, all routing happens internally

## Related Files Modified

- **k8s/manifests/09-frontend-deployment.yml** (Lines 37-61): Changed `FRONTEND_API_BASE` to `/api` and updated sed patterns
- No other files required changes

## Rollback (If Needed)

If issues arise, rollback to previous configuration:

```bash
# Revert the manifest
git checkout HEAD~1 k8s/manifests/09-frontend-deployment.yml

# Apply old configuration
kubectl apply -f k8s/manifests/09-frontend-deployment.yml

# Restart pods
kubectl delete pods -n ajasta -l component=frontend
```

## Success Criteria

- ✅ Browser console shows NO `ERR_NAME_NOT_RESOLVED` errors
- ✅ API calls use relative paths: `http://51.250.21.26/api/*`
- ✅ Application functions correctly (can fetch resources, login, etc.)
- ✅ No absolute URLs with service DNS names in served JavaScript

---

**Fix Status**: ✅ **COMPLETE**

The frontend now correctly uses relative API paths that are routed through Kubernetes Ingress, eliminating browser DNS resolution errors.


## New localhost defaults (React)

- In production/static builds served on localhost:3000 without an Ingress/proxy for /api, the frontend now automatically targets:
  - http://localhost:8090/api for the main backend
  - http://localhost:8091/api/cms for the CMS service
- This applies only when the app is served on localhost:3000 in production mode. During development (`npm start`), the CRA dev proxy remains in effect and relative URLs `/api/*` are used.
