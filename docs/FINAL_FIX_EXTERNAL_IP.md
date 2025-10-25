# FINAL FIX: ${external_ip} ERR_NAME_NOT_RESOLVED Issue

## Root Cause Identified

The problem was **incorrect shell escaping** in the Kubernetes initContainer sed patterns.

### The Bug
In `k8s/manifests/09-frontend-deployment.yml` (lines 52-60), the sed patterns used:
```bash
sed -i -e "s#http://\\${external_ip}:8090/api#${BASE}#gI"
```

The double backslash `\\$` in a double-quoted shell string means "literal backslash + dollar sign", so this pattern was looking for `\${external_ip}` (with backslash) instead of `${external_ip}` (without backslash).

### The Fix
Changed to single backslash:
```bash
sed -i -e "s#http://\${external_ip}:8090/api#${BASE}#gI"
```

In double-quoted strings, `\$` escapes the dollar sign so sed receives `${external_ip}` as the search pattern.

## Why This Matters

1. **Docker Image Build**: The frontend image was built with `API_BASE_URL="/api"` (correct)
2. **Runtime Detection**: ApiService.js has correct runtime detection logic
3. **InitContainer Rewrite**: Was supposed to remove any remaining `${external_ip}` placeholders
4. **The Problem**: InitContainer sed patterns NEVER MATCHED because of incorrect escaping
5. **Result**: `${external_ip}` placeholders remained in JavaScript files → ERR_NAME_NOT_RESOLVED

## Deployment Instructions

### Step 1: Apply the Fixed Manifest
```bash
cd /Users/rurik/IdeaProjects/petrelevich/ajasta-app/k8s

# Apply the corrected frontend deployment manifest
kubectl apply -f manifests/09-frontend-deployment.yml
```

### Step 2: Force Pod Restart
```bash
# Delete existing frontend pods to force recreation with new initContainer
kubectl delete pods -n ajasta -l component=frontend

# Wait for new pods to start
kubectl rollout status deployment ajasta-frontend -n ajasta
```

### Step 3: Verify the Fix
```bash
# Check that ${external_ip} has been removed from served files
POD=$(kubectl get pods -n ajasta -l component=frontend -o name | head -n1)
kubectl exec -n ajasta $POD -- grep -r "external_ip" /usr/share/nginx/html

# Expected output: (nothing - command should find no matches)
# If it still shows matches, the initContainer didn't run or failed
```

### Step 4: Check InitContainer Logs (if needed)
```bash
# View initContainer logs to verify it ran successfully
kubectl logs -n ajasta $POD -c fix-api-base-url
```

### Step 5: Test in Browser
1. **Hard refresh**: Press Ctrl+Shift+R (Windows/Linux) or Cmd+Shift+R (Mac)
2. Open browser console (F12)
3. Navigate to http://51.250.21.26/resources
4. Verify API calls now use proper URLs:
   - Should see: `GET http://51.250.21.26/api/resources?active=true`
   - NOT: `GET http://${external_ip}:8090/api/resources?active=true`

## What the InitContainer Does Now

```bash
# Copies image files to /work volume
cp -r /usr/share/nginx/html/* /work/

# Replaces ${external_ip} with ajasta-backend (K8s service DNS)
sed -i "s#http://\${external_ip}:8090/api#http://ajasta-backend:8090/api#gI" *.js

# Frontend container mounts /work as webroot
# Browser receives cleaned JavaScript without placeholders
```

## Expected Behavior After Fix

1. ✅ No `${external_ip}` literal strings in served JavaScript
2. ✅ API calls use relative paths or proper service names
3. ✅ No ERR_NAME_NOT_RESOLVED errors
4. ✅ Application works correctly at http://51.250.21.26/

## Troubleshooting

### If ${external_ip} Still Appears

1. **Check pod restart**:
   ```bash
   kubectl get pods -n ajasta -l component=frontend
   # Verify AGE is recent (less than 5 minutes)
   ```

2. **Check initContainer ran**:
   ```bash
   kubectl logs -n ajasta <pod-name> -c fix-api-base-url
   # Should show: files being processed by find/sed
   ```

3. **Verify manifest applied**:
   ```bash
   kubectl get deployment ajasta-frontend -n ajasta -o yaml | grep -A5 "fix-api-base-url"
   # Should show single backslash: \${external_ip}
   ```

### If Browser Still Shows Error

- **Clear browser cache**: Close all tabs, clear cache, restart browser
- **Check browser console**: Network tab should show actual URLs being called
- **Bypass cache**: Use incognito/private mode

## Technical Details

### Shell Escaping Rules (for reference)
In double-quoted shell strings (`"..."`):
- `$var` → expands variable
- `\$var` → literal `$var` (one backslash escapes the $)
- `\\$var` → literal `\` + expanded variable (two backslashes = one literal backslash)
- `\\\$var` → literal `\$var` (three backslashes = backslash + escaped $)

For sed to receive `${external_ip}` as search pattern in a double-quoted shell string:
- Correct: `"s#\${external_ip}#replacement#g"`
- Wrong: `"s#\\${external_ip}#replacement#g"` (looks for `\${external_ip}`)

### Why Previous Fixes Didn't Work

1. **build-and-push-images.zsh**: Fixed to use `API_BASE_URL="/api"` ✅
2. **ApiService.js**: Has correct runtime detection ✅
3. **Dockerfile sed**: Doesn't match new code structure (but not needed) ⚠️
4. **InitContainer sed**: Had incorrect escaping → **THIS WAS THE ACTUAL BUG** ❌ → **NOW FIXED** ✅

## Success Criteria

After applying this fix:
- ✅ `kubectl exec` grep finds NO "external_ip" in served files
- ✅ Browser console shows clean API URLs
- ✅ Application loads and functions correctly
- ✅ No ERR_NAME_NOT_RESOLVED errors

---

**This is the definitive fix. The root cause was a shell escaping bug in the initContainer sed patterns.**
