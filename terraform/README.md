Terraform for Yandex Cloud (Ajasta infra)

This directory contains Terraform configuration for Yandex Cloud that mirrors the Ansible playbook `k8s/yc-create.yml` and the script `scripts/create-vm-static-ip.zsh`:
- VPC networks: external and internal
- Subnets: external and internal (single zone)
- Reserved static public IPs: 1 for the master and 1 per worker
- Compute instances: 1 master and N workers — attached to the internal subnet, with static public IP via NAT
- Optional: YC service account and static key (disabled by default)

Parities with scripts:
- Preemptible instances with `core_fraction = 20`
- Boot disk type `network-hdd`
- Image `centos-stream-9-oslogin` from `standard-images`
- Cloud-init `user-data` from `../scripts/metadata.yaml`
- Ability to inject SSH public key via variable

Requirements
- Terraform >= 1.5
- Access to Yandex Cloud via `YC_TOKEN` or a service account key JSON

Authentication
- Recommended: authenticate via `YC_TOKEN` provided by the `yc` CLI or Instance Metadata Service (if running in YC).
  - `yc iam create-token | pbcopy` and export `YC_TOKEN` in your shell, or just run `yc init` and the provider will pick up the active profile.
- Alternatively, you may pass an IAM service account key JSON via `yc_service_account_key_file`.
  - Important: this must be an IAM key JSON (fields like `id`, `service_account_id`, `private_key`).
  - Do NOT use Object Storage static access keys JSON (which contains `access_key`/`secret_key`); the provider will fail to parse it with an error like: `key unmarshal fail: unknown field "access_key"`.
  - To create a proper IAM key JSON:
    - `yc iam service-account create --name <sa-name>`
    - `yc iam service-account get --name <sa-name>` to note its `id`
    - `yc iam key create --service-account-id <sa-id> --output sa-iam-key.json`
    - set `yc_service_account_key_file = "../sa-iam-key.json"` in `terraform.tfvars`

Grant required roles to the Service Account
- The service account used by Terraform must have permissions on the target folder to create networks, addresses, and instances.
- Minimal roles you need to grant on the folder:
  - `vpc.admin` — to create VPC networks and subnets
  - `vpc.publicAdmin` — to allocate reserved external IPv4 addresses (static public IPs)
  - `compute.editor` (or `compute.admin`) — to create compute instances and disks

Helper script (auto mode recommended):
```bash
# Make sure you are authenticated with a human account that can manage access bindings
yc init

# Auto-detect folder_id and service account from terraform/terraform.tfvars and sa-iam-key.json
chmod +x scripts/grant-sa-folder-roles.zsh
scripts/grant-sa-folder-roles.zsh

# Dry-run to see what it will do
scripts/grant-sa-folder-roles.zsh --dry-run

# Override anything explicitly (examples):
scripts/grant-sa-folder-roles.zsh --folder-id b1gndaq08b52358dleke --sa-name ajasta-tf
scripts/grant-sa-folder-roles.zsh --folder-id b1gndaq08b52358dleke --sa-id sa-xxxxxxxxxxxxxxxxxxxx
scripts/grant-sa-folder-roles.zsh --roles vpc.admin,compute.admin  # custom roles
```

If you cannot obtain `vpc.publicAdmin` but still want to proceed, you may switch to ephemeral NAT IPs instead of reserved static addresses. Ask the maintainer to enable this option in Terraform.

 Helper script: create IAM key JSON automatically
 This repository includes a helper script that automates the IAM key creation flow above and writes a proper key JSON for Terraform.

 Usage:
 ```bash
 # 1) Ensure YC CLI is initialized
 yc init

 # 2) Run the helper (service account name is optional; default: ajasta-tf)
 chmod +x scripts/create-iam-terraform-key.zsh
 scripts/create-iam-terraform-key.zsh ajasta-tf  # or choose another name

 # The script will write a key file to the repo root: sa-iam-key.json
 # (It refuses to overwrite an existing file; pass a custom path as the 2nd argument if needed)
 ```

 Then point Terraform to that file in your `terraform.tfvars`.

 Notes:
 - The script reuses an existing service account if it already exists.
 - Ensure the service account has sufficient roles on the folder (e.g., `editor` during bootstrap, or granular roles like `compute.editor`, `vpc.admin`).

Files
- `versions.tf` — Terraform and provider versions
- `providers.tf` — Yandex provider and authentication parameters
- `variables.tf` — input variables (names, CIDRs, VM specs, SSH, etc.)
- `network.tf` — networks and subnets (external + internal)
- `addresses.tf` — static public addresses for master and workers
- `compute.tf` — compute instances: master and workers
- `service_account.tf` — optional service account and its key
- `outputs.tf` — outputs (IPs and instance IDs)
- `terraform.tfvars.example` — example values
- `terraform.tfstate.example` — sanitized/example state file (no real resources)

Quick start
1) Copy example variables and edit:
   cd terraform
   cp terraform.tfvars.example terraform.tfvars
   set yc_cloud_id, yc_folder_id, ssh_public_key, etc.
   - By default, the config expects `YC_TOKEN` or IMDS. If you want to use an IAM key file, see Authentication above and update `yc_service_account_key_file`.

2) Initialize and review plan:
   terraform init
   terraform plan

3) Apply:
   terraform apply

Outputs:
- `master_public_ip`
- `worker_public_ips` (map: worker name => public IP)

Troubleshooting: who am I?
- To verify which cloud/folder/zone your provider is using, check the output `current_context` (visible in plan/apply output). This is sourced from the `yandex_client_config` data source and helps detect mismatched auth/folder configs.

You can then generate an Ansible inventory from these IPs, similar to what `k8s/yc-create.yml` does.

Ansible parity (defaults from `k8s/group_vars/all.yml`):
- External network: `external-ajasta-network`, subnet `ajasta-external-segment` (`172.16.17.0/28`)
- Internal network: `internal-ajasta-network`, subnet `ajasta-internal-segment` (`10.10.0.0/24`)
- Master VM: `k8s-master`
- Workers: `k8s-worker-1..3`
- Static addresses: `ajasta-k8s-master-ip`, `ajasta-k8s-worker1-ip`, ...
- Resources: master 2 vCPU / 6 GB / 30 GB; workers 2 vCPU / 6 GB / 30 GB
- SSH user: `ajasta`
- cloud-init: `../scripts/metadata.yaml`

Notes
- No explicit security groups are defined; default YC rules are used. Add `yandex_vpc_security_group` if you need strict rules (SSH/ICMP/HTTP, etc.).
- Subnets are created in a single zone (`yc_zone`, default `ru-central1-b`).
- Instances are preemptible with `core_fraction = 20`, matching the scripts.
- Public addresses are created as separate resources and attached to instances via `nat_ip_address`.

About tfstate
- By default, local backend is used and Terraform writes `terraform.tfstate` in this directory.
- To satisfy the “attach tfstate” requirement without exposing sensitive data, I include `terraform.tfstate.example` — a sanitized example with no real resources.
- If you prefer to keep the real state in the repo, you can run `terraform apply` and commit your `terraform.tfstate` at your own risk.

Destroy
To remove all created resources:
terraform destroy

Warning: this will remove networks, addresses, and instances created by this stack.
