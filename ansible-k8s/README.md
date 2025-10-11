Ansible playbooks for provisioning a minimal Kubernetes-ready set of VMs in Yandex Cloud and destroying them to save costs.

Overview
- Creates one master VM and two worker VMs in Yandex Cloud
- Master gets a static public IP; workers have no public IPs and live on a private subnet shared with master
- Uses existing project scripts in ./scripts for idempotent provisioning
- Bootstraps a Kubernetes cluster (kubeadm) automatically: initializes control plane and joins both workers
- Provides a destroy playbook to remove all created resources

Prerequisites
- Ansible installed on your local machine
- Yandex Cloud CLI (yc) installed and configured (yc init) with access to your cloud/folder
- jq installed
- zsh available (scripts are written in zsh)
- SSH public key file available if you want to inject it into the instances (optional; auto-detected)

Files
- inventory.ini — local inventory (localhost) to run scripts from your machine
- group_vars/all.yml — configuration variables (cloud/folder IDs, names, etc.)
- k8s-provision.yml — creates/reuses network, static IP, service account, and 3 VMs (1 master + 2 workers)
- k8s-destroy.yml — removes VMs and related resources to stop incurring costs

Quick start
1) Configure variables
- Copy or edit ansible-k8s/group_vars/all.yml as needed. At a minimum set:
  - yc_cloud_id (optional if you have yc CLI configured or set env YC_CLOUD_ID)
  - yc_folder_id (optional if you have yc CLI configured or set env YC_FOLDER_ID)
  - ssh_pubkey_file (path to your public key)

Tip: If yc_cloud_id/yc_folder_id are empty, the playbooks will try to auto-detect them in this order:
  1) Environment variables YC_CLOUD_ID / YC_FOLDER_ID
  2) Yandex CLI config (yc config get cloud-id / folder-id)
If still missing, the playbooks will fail with a clear message.

SSH key auto-detection:
- You may set ansible-k8s/group_vars/all.yml: ssh_pubkey_file to a path.
- If not set or the file does not exist, the playbook will look for the first existing key in this order:
  1) $HOME/.ssh/id_ed25519.pub
  2) $HOME/.ssh/id_rsa.pub
  3) ./scripts/ajasta_ed25519.pub
- If none is found, the VM will be created without injecting an SSH key. In that case, use scripts/add-ssh-key.zsh later to add one, e.g.:
  SSH_USERNAME=ajasta SSH_PUBKEY_FILE=~/.ssh/id_ed25519.pub ./scripts/add-ssh-key.zsh k8s-master

2) Provision
ansible-playbook -i ansible-k8s/inventory.ini ansible-k8s/k8s-provision.yml

This will:
- Ensure external and internal VPC networks/subnets exist
- Ensure static IP for master exists
- Ensure service account exists
- Create master VM on the private subnet with the static IP and two worker VMs without public IPs on the same private subnet
- Bootstrap a Kubernetes cluster: install container runtime and kubeadm on all nodes, init control-plane, install Flannel CNI, and join workers

Kubernetes access
- SSH into master: ssh ajasta@<MASTER_PUBLIC_IP>
- kubeconfig on master: /home/ajasta/.kube/config
- Use kubectl on master, e.g.: kubectl get nodes -o wide
- From your local machine, you can ProxyJump via master or copy kubeconfig:
  scp -o ProxyJump=ajasta@<MASTER_PUBLIC_IP> ajasta@<MASTER_PUBLIC_IP>:/home/ajasta/.kube/config ./kubeconfig
  export KUBECONFIG=./kubeconfig

Rancher Dashboard
By default, cluster bootstrap is disabled to avoid long waits and unnecessary spend during provisioning. If you enable bootstrap (see below), you can optionally install Rancher for easier cluster management.

Quick toggle (no more long wait):
- In ansible-k8s/group_vars/all.yml set:
  bootstrap_k8s: false  # default; only infra + VMs created
  install_rancher: false # recommended to avoid extra time until cluster is ready

Enable cluster bootstrap later (faster and controlled):
1) Provision infra and VMs first (default behavior):
   ansible-playbook -i ansible-k8s/inventory.ini ansible-k8s/k8s-provision.yml
2) When ready, enable bootstrap and (optionally) Rancher:
   - Set bootstrap_k8s: true
   - Optionally set install_rancher: true
   - Optionally tune retries:
       ssh_retry_attempts: 8
       ssh_retry_delay: 3
   - Run the provision playbook again; it will only run the bootstrap and Rancher steps.

Why was a bastion used?
- Workers have no public IPs by design (to save costs and increase security), so they are reachable only from the master over the private subnet. The master’s public IP acts as a bastion for SSH ProxyCommand to reach workers without exposing them to the Internet.
- If you prefer no bastion logic at all, assign temporary public IPs to workers during bootstrap, or move the kubeadm logic into cloud-init; both options raise complexity/cost and are not the default here.

Access Rancher (when installed):
1. The playbook output will show the Rancher URL and NodePort (e.g., https://<MASTER_PUBLIC_IP>:30443)
2. Open the URL in your browser
3. Accept the self-signed certificate warning
4. Login with the bootstrap password: admin
5. You'll be prompted to set a new password on first login

Configuration:
- To skip Rancher installation, set install_rancher: false in group_vars/all.yml
- To change the hostname, set rancher_hostname in group_vars/all.yml (default: rancher.local)

Manual Rancher installation:
If you skipped installation or want to install it later:
  SSH_USERNAME=ajasta ./scripts/install-rancher.zsh <MASTER_PUBLIC_IP>

3) Destroy
ansible-playbook -i ansible-k8s/inventory.ini ansible-k8s/k8s-destroy.yml

Notes
- The playbooks operate on localhost and call the ./scripts/*.zsh helpers, preserving their idempotency.
- You can change VM names, network names, etc. in group_vars/all.yml.
- Default image and VM sizes are defined inside the scripts. Adjust scripts if you need different sizes/images.