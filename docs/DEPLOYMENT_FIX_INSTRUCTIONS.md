# Fix for ERR_NAME_NOT_RESOLVED Issue

## Problem
Browser console showed:
```
GET http://${external_ip}:8090/api/categories/all net::ERR_NAME_NOT_RESOLVED
```

The literal placeholder `${external_ip}` was appearing in the browser instead of a real IP address or service name.

## Root Cause
The `scripts/build-and-push-images.zsh` script was building the frontend Docker image with:
```bash
docker build --build-arg API_BASE_URL="http://\${EXTERNAL_IP}:8090/api" ...
```

This intentionally injected the literal string `${EXTERNAL_IP}` into the built JavaScript files. While this might work for simple VM deployments where a post-processing step replaces it, it doesn't work properly for Kubernetes deployments where:
1. The frontend should use relative paths (`/api`) routed through Kubernetes Ingress
2. Or use Kubernetes service DNS names (`http://ajasta-backend:8090/api`)

## Solution Applied

### 1. Fixed Build Script
**File: `scripts/build-and-push-images.zsh`**
- Changed line 86 from: `API_BASE_URL="http://\${EXTERNAL_IP}:8090/api"`
- To: `API_BASE_URL="/api"`

Now the Docker image is built with a relative API path that works with Kubernetes Ingress.

### 2. Existing Safeguards (Already in Place)
The following components were already correctly implemented:

**File: `ajasta-react/src/services/ApiService.js`**
- Uses runtime detection to determine API base URL
- Prefers relative paths (`/api`) that work with Ingress
- Falls back to `window.location.origin + '/api'`

**File: `k8s/manifests/09-frontend-deployment.yml`**
- Has initContainer that rewrites any remaining `${external_ip}` placeholders
- Uses sed patterns to replace with Kubernetes service DNS names
- Sets FRONTEND_API_BASE environment variable

**File: `k8s/deploy-ajasta.yml`**
- Forces rollout restart of frontend (line 815-819)
- Has guard check to detect leftover `${external_ip}` references (line 830-848)
- Fails deployment if placeholders are still present

## Deployment Instructions

### Step 1: Rebuild and Push Docker Image
```bash
cd /Users/rurik/IdeaProjects/petrelevich/ajasta-app

# Build and push the corrected frontend image
./scripts/build-and-push-images.zsh
```

This will:
- Build the frontend image with `API_BASE_URL="/api"`
- Push to `vladimirryrik/ajasta-frontend:alpine`

### Step 2: Deploy to Kubernetes
If using Ansible deployment:
```bash
cd /Users/rurik/IdeaProjects/petrelevich/ajasta-app/k8s
ansible-playbook -i ../ansible-k8s/inventory.ini deploy-ajasta.yml
```

Or manually restart the frontend pods to pull the new image:
```bash
kubectl rollout restart deployment ajasta-frontend -n ajasta
kubectl rollout status deployment ajasta-frontend -n ajasta
```

### Step 3: Verify the Fix
Check that no `${external_ip}` remains in served files:
```bash
kubectl get pods -n ajasta -l component=frontend
POD=$(kubectl get pods -n ajasta -l component=frontend -o name | head -n1)
kubectl exec -n ajasta $POD -- grep -r "external_ip" /usr/share/nginx/html || echo "✓ No external_ip found"
```

### Step 4: Test in Browser
1. Clear browser cache (hard refresh: Ctrl+Shift+R or Cmd+Shift+R)
2. Open browser console
3. Navigate to your application
4. Verify API calls now use proper URLs (relative `/api` or service DNS)

## Expected Behavior After Fix
- Frontend makes API calls to `/api/*` (relative paths)
- Kubernetes Ingress routes `/api/*` to backend service
- No literal `${external_ip}` placeholders in JavaScript files
- No ERR_NAME_NOT_RESOLVED errors

## Technical Details

### How the Fix Works
1. **Build time**: Frontend is built with `API_BASE_URL="/api"` (relative path)
2. **Runtime detection**: ApiService.js uses `window.location.origin + '/api'`
3. **Ingress routing**: Kubernetes Ingress routes `/api` → `ajasta-backend:8090`
4. **InitContainer safety**: Rewrites any remaining placeholders in deployed assets
5. **Guard check**: Deployment fails if `${external_ip}` is detected in served files

### Architecture
```
Browser → https://example.com/api/categories/all
         ↓
Kubernetes Ingress (nginx)
         ↓
ajasta-backend Service (ClusterIP)
         ↓
Backend Pod (port 8090)
```

The frontend never needs to know the backend's external IP or service name - all routing happens through Ingress at the same origin.
