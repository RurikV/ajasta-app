# GitLab CI/CD kubeconfig Runbook (Unauthorized fixes and future-proofing)

This runbook explains how to fix `kubectl` auth errors in GitLab CI/CD ("You must be logged in" / `Unauthorized`) and how to prevent them in the future when the Kubernetes API public IP or certificates change.

## Symptoms in CI/CD

- `kubectl cluster-info` fails with `Unauthorized`
- Locally, `kubectl` works against the same cluster
- Master IP was rotated or cluster certificates were reissued

## Root causes

- The GitLab CI/CD variable `KUBECONFIG_CONTENT` is stale (old server IP or expired client certs)
- TLS mismatch: cluster certs are for internal IPs, CI connects via public IP
- CI jobs used user `~/.kube/config` on a shell runner and overwrote a working file

## What we changed in the pipeline

- Do not touch `~/.kube/config` on the runner anymore. CI writes kubeconfig to a project-local path and `export KUBECONFIG`.
- Normalize the kubeconfig during job startup:
  - Remove any `certificate-authority` and `certificate-authority-data`
  - Set `--insecure-skip-tls-verify=true` on the current cluster
  - Force a specific context
- Added a manual smoke test job `k8s:auth:check` (validate stage) so you can check CI auth before running a deploy.

## Quick fix when IP/certs change

1) Generate a clean, minimal kubeconfig on your local machine (the one that works):

```bash
# Option A: from master node admin.conf (replace with current master IP)
MASTER_IP=<public-master-ip>
ssh ajasta@"$MASTER_IP" 'sudo cat /etc/kubernetes/admin.conf' > /tmp/admin.conf

# Replace internal server with public and enable insecure skip; remove CA
awk '1' /tmp/admin.conf \
  | sed "s|server: https://[0-9.]*:6443|server: https://$MASTER_IP:6443|" \
  | awk 'BEGIN{rm=0} /certificate-authority-data:/{rm=1;next} rm&&/^[^ ]/{rm=0} !rm {print}' \
  > /tmp/kubeconfig.min.yaml

# Ensure current-context is set (adjust name if needed)
if ! grep -q '^current-context:' /tmp/kubeconfig.min.yaml; then
  echo 'current-context: kubernetes-admin@kubernetes' >> /tmp/kubeconfig.min.yaml
fi

# Sanity check
KUBECONFIG=/tmp/kubeconfig.min.yaml kubectl cluster-info
```

2) Base64-encode it to a single line:

```bash
base64 -w 0 < /tmp/kubeconfig.min.yaml 2>/dev/null || base64 < /tmp/kubeconfig.min.yaml | tr -d '\n'
```

3) Update GitLab variable:

- Project → Settings → CI/CD → Variables
- Name: `KUBECONFIG_CONTENT`
- Value: paste the base64 string (single line)
- Protected: ✓
- Masked: ✓ (may warn due to length; OK)
- Expand variable reference: ✗
- Save

4) Run the smoke test job:

- CI/CD → Pipelines → latest → `k8s:auth:check` → Play
- Should show control plane info and `get nodes` output

5) Deploy:

- Run `deploy:k8s:staging` or `deploy:k8s:production` (manual)

## If smoke test fails

Use these diagnostics (already printed by the job if `cluster-info` fails):

- `kubectl config view --minify`
- `kubectl get --raw=/version`
- `kubectl auth can-i --list`

Common remediation:

- Verify the `server:` points to the public master IP
- Ensure `certificate-authority*` fields are absent if using `insecure-skip-tls-verify`
- Ensure `current-context` exists and is correct (e.g. `kubernetes-admin@kubernetes`)

## Prevent future breakage (1–2 minute rotation)

Use the helper script to regenerate the minimal kubeconfig and print base64:

```bash
# From repo root
bash scripts/update-kubeconfig-after-ip-change.sh
# Follow prompts or pass the master IP; script outputs base64 ready to paste
```

Paste the output into `KUBECONFIG_CONTENT` and re-run `k8s:auth:check`.

## Optional hardening (recommended later)

- Switch CI auth from admin certificates to a dedicated ServiceAccount token with minimal RBAC
- Assign a stable DNS name to the API server and issue a certificate with proper SANs to remove `insecure-skip-tls-verify`

## Verification checklist

- [ ] `k8s:auth:check` shows cluster info and nodes
- [ ] `deploy:k8s:*` jobs pass the kubeconfig verification step
- [ ] Helm deploy proceeds without `Unauthorized`
