# Ingress 404 Runbook (Ajasta)

This runbook explains how to resolve `404 Not Found` from NGINX when opening the public IP (e.g., `http://89.169.183.199/`) and how to make the fix resilient for future IP rotations.

## TL;DR — Quick Fix

1) Ensure the ingress controller exposes the current public IP:

```bash
kubectl -n ingress-nginx patch svc ingress-nginx-controller \
  --type merge \
  -p '{"spec":{"externalIPs":["<PUBLIC_IP>"]}}'

kubectl -n ingress-nginx get svc ingress-nginx-controller -o wide
```

2) Use a hostless Ingress rule (catch‑all) during deploys:

- The CI deploy jobs now do this automatically if `K8S_*_INGRESS_HOST` is empty.
- To force hostless rule manually: add Helm flag `--set-string ingress.hosts[0].host=""`.

3) Re‑deploy via GitLab:

- Pipelines → latest → `deploy:k8s:production` (Play)
- The job patches the controller `Service` and deploys a host or hostless Ingress based on variables.

---

## How It Works Now (Automation in CI)

The deploy jobs in `.gitlab-ci.yml` were updated to:

- Patch `ingress-nginx-controller` `Service` with the current public IP (`externalIPs: [ $YC_VM_EXTERNAL_IP ]`) on every deploy
- Render Ingress with either:
  - Explicit host (from `K8S_*_INGRESS_HOST`), or
  - Hostless catch‑all rule if the host variable is empty
- Remove risky NGINX rewrite annotations in the chart, preventing accidental path breaks
- Run a quick post‑deploy curl to root URL to confirm reachability

This makes the deployment robust when the VM/master IP changes.

---

## End‑to‑End Checklist

Run these commands to verify the stack if you see 404:

```bash
# 1) Verify ingress class
kubectl get ingressclass

# 2) Inspect Ingress
kubectl -n ajasta get ingress ajasta-ingress -o yaml

# 3) Confirm controller exposure
kubectl -n ingress-nginx get svc ingress-nginx-controller -o wide

# 4) Ensure Services exist
kubectl -n ajasta get svc ajasta-frontend ajasta-backend -o wide

# 5) Patch controller Service with current public IP (if needed)
MASTER_IP=<public-ip>
kubectl -n ingress-nginx patch svc ingress-nginx-controller \
  --type merge \
  -p '{"spec":{"externalIPs":["'"${MASTER_IP}"'"]}}'

# 6) Re‑deploy via GitLab CI (manual)
# Pipelines → latest → deploy:k8s:production → Play
```

If you use a DNS name, set it in CI variables:
- `K8S_PRODUCTION_INGRESS_HOST=my.domain.tld`

If you prefer catch‑all by path, leave the variable empty and the CI job will force hostless rules.

---

## Common Root Causes of 404

- Host mismatch: Ingress was configured for `ajasta.local` while you browsed the IP
- Ingress controller Service not exposing the new public IP after VM/master IP change
- Path rewrite annotations breaking path forwarding

---

## How to Switch Between IP and DNS Name

- Use DNS name: set `K8S_PRODUCTION_INGRESS_HOST` in GitLab → Settings → CI/CD → Variables; add an A record to your current IP
- Use IP (host rule): set `K8S_PRODUCTION_INGRESS_HOST` to the public IP
- Use hostless (catch‑all): leave `K8S_PRODUCTION_INGRESS_HOST` empty; CI will pass `--set-string ingress.hosts[0].host=""`

---

## Verification (HTTP)

From your machine:

```bash
curl -I http://<PUBLIC_IP>/
curl -I http://<PUBLIC_IP>/api/resources?active=true
```

Expected: 200 OK for both.

From inside cluster (optional):

```bash
kubectl -n ajasta run curl --rm -it --image=alpine/curl -- /bin/sh
# Inside the pod
curl -I http://ajasta-frontend
curl -I http://ajasta-backend:8090/api/resources?active=true
exit
```

---

## Notes

- The chart’s `values.yaml` no longer includes `nginx.ingress.kubernetes.io/rewrite-target: /` to avoid path complications. If you need rewrites, enable `use-regex: "true"` and adjust rules accordingly.
- The CI job includes a quick `curl` probe; add more diagnostics if desired (e.g., dump `describe ingress`, controller logs).
- For stability, consider switching to a DNS name long‑term and issuing a valid TLS cert; this avoids constant IP edits.
