### Kubernetes Monitoring (Prometheus, Grafana, Loki) — Quickstart via IP

This setup deploys:
- kube-prometheus-stack (Prometheus, Alertmanager, Grafana, exporters)
- Loki + Promtail for logs collection
- ServiceMonitor for your backend (Spring Boot Actuator `/actuator/prometheus`)

Everything can be accessed behind your existing Ingress controller using only the public IP (port 80), no domains required.

---

#### Prerequisites
- A running Kubernetes cluster with:
  - Ingress controller (e.g., ingress-nginx) with an External IP
  - `kubectl` and `helm` installed on your machine
- Your kube-context points to the target cluster
- Ansible installed if you run the playbook from your machine

---

#### Quickstart (IP mode — no domains)
Run the playbook in IP mode to expose UIs at path prefixes on the public Ingress IP:

```bash
ansible-playbook -i k8s/inventory.ini k8s/monitoring-install.yml \
  -e enable_ingress=true \
  -e ingress_use_ip=true \
  -e grafana_admin_password='ChangeMe_SuperSecret'
```

On completion, the playbook prints links like:
```
http://<INGRESS_IP>/grafana
http://<INGRESS_IP>/prometheus
http://<INGRESS_IP>/alertmanager
```

To discover the current Ingress IP manually:
```bash
kubectl -n ingress-nginx get svc ingress-nginx-controller -o jsonpath='{.status.loadBalancer.ingress[0].ip}'
```

Grafana login (defaults):
- user: `admin`
- pass: the value you set via `grafana_admin_password` (default `ChangeMe_SuperSecret`)

---

#### Verify
- Pods up:
  ```bash
  kubectl get pods -n monitoring
  kubectl get pods -n logging
  ```
- Grafana datasources show Prometheus and Loki as “green”
- Prometheus targets contain your backend ServiceMonitor (Status > Targets)
- Logs available in Grafana > Explore (Loki):
  - Example query: `{namespace="ajasta"}`

---

#### Port-forward fallback (if you don’t want to expose Ingress yet)
Run with `-e enable_ingress=false`, then locally:
```bash
kubectl -n monitoring port-forward svc/kps-grafana 3000:80
kubectl -n monitoring port-forward svc/kps-kube-prometheus-stack-prometheus 9090:9090
kubectl -n monitoring port-forward svc/kps-kube-prometheus-stack-alertmanager 9093:9093
kubectl -n logging    port-forward svc/loki 3100:3100
```
Open:
- Grafana: http://localhost:3000
- Prometheus: http://localhost:9090
- Alertmanager: http://localhost:9093

Loki notes:
- Loki has no standalone UI; use Grafana Explore to query logs against the Loki datasource.
- You can still access Loki’s HTTP API when port-forwarded: http://localhost:3100
  - Example endpoints: `/ready`, `/metrics`, `/loki/api/v1/labels`, `/loki/api/v1/query`

---

#### Switch later to DNS + TLS (optional)
1) Rerun with:
```bash
-e ingress_use_ip=false \
-e grafana_domain=grafana.example.com \
-e prometheus_domain=prometheus.example.com \
-e alertmanager_domain=alertmanager.example.com
```
2) Ensure cert-manager is installed and configure TLS for those hosts.
3) Update your DNS to point to the Ingress controller’s public IP (or use ExternalDNS).

---

#### Useful variables (defaults in playbook)
- `enable_ingress` (bool): expose UIs via Ingress (default: true)
- `ingress_use_ip` (bool): hostless/catch‑all Ingress, access by IP (default: true)
- `grafana_admin_password` (string): Grafana admin password (default: `ChangeMe_SuperSecret`)
- Path prefixes (IP mode):
  - `grafana_path`: `/grafana`
  - `prometheus_path`: `/prometheus`
  - `alertmanager_path`: `/alertmanager`

---

#### Uninstall
```bash
helm -n monitoring uninstall kps || true
helm -n logging uninstall loki || true
kubectl delete ns monitoring logging || true
```
