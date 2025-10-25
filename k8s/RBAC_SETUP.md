# Kubernetes RBAC Configuration

This directory contains RBAC (Role-Based Access Control) configuration for the ajasta Kubernetes cluster, providing three levels of access: **read-only**, **read-write**, and **admin**.

## Overview

RBAC configuration creates three types of users with different permission levels:

1. **Reader** - Read-only access to resources in the `ajasta` namespace
2. **Writer** - Read-write access to resources in the `ajasta` namespace
3. **Admin** - Full cluster-wide administrative access

## Files

### RBAC Manifests (in `manifests/` directory)

- `12-rbac-serviceaccounts.yml` - ServiceAccount definitions for three user types
- `13-rbac-roles.yml` - Role and ClusterRole definitions with specific permissions
- `14-rbac-bindings.yml` - RoleBindings and ClusterRoleBinding connecting accounts to roles

### Scripts

- `generate-kubeconfigs.sh` - Script to generate kubeconfig files for each service account

## Permission Levels

### 1. Reader (Read-Only Access)

**ServiceAccount:** `reader-user`  
**Role:** `reader-role` (namespace-scoped)  
**Permissions:**
- View (get, list, watch) resources in the `ajasta` namespace:
  - Pods, Services, Deployments, StatefulSets, DaemonSets
  - ConfigMaps, Secrets, PersistentVolumeClaims
  - Jobs, CronJobs, Ingresses
  - Pod logs and status

**Use cases:**
- Monitoring and observability
- Developers viewing application status
- CI/CD read-only access for verification

### 2. Writer (Read-Write Access)

**ServiceAccount:** `writer-user`  
**Role:** `writer-role` (namespace-scoped)  
**Permissions:**
- All Reader permissions PLUS:
- Create, update, patch, delete resources in the `ajasta` namespace:
  - Pods, Services, Deployments, StatefulSets, DaemonSets
  - ConfigMaps, PersistentVolumeClaims
  - Jobs, CronJobs, Ingresses
  - Execute commands in pods (pods/exec)
- Read-only access to ServiceAccounts

**Use cases:**
- Application developers deploying updates
- DevOps engineers managing applications
- CI/CD pipelines for deployment

### 3. Admin (Full Cluster Access)

**ServiceAccount:** `admin-user`  
**ClusterRole:** `admin-role` (cluster-scoped)  
**Permissions:**
- Full access to all resources across all namespaces
- Access to cluster-level resources
- Access to non-resource URLs (metrics, health checks, etc.)

**Use cases:**
- Cluster administrators
- Infrastructure management
- Emergency troubleshooting and recovery

## Installation

### Step 1: Apply RBAC Manifests

Apply the RBAC configuration to your Kubernetes cluster (requires cluster-admin access):

```bash
# From the k8s directory
kubectl apply -f manifests/12-rbac-serviceaccounts.yml
kubectl apply -f manifests/13-rbac-roles.yml
kubectl apply -f manifests/14-rbac-bindings.yml
```

Or apply all at once:

```bash
kubectl apply -f manifests/12-rbac-serviceaccounts.yml \
              -f manifests/13-rbac-roles.yml \
              -f manifests/14-rbac-bindings.yml
```

### Step 2: Verify RBAC Resources

Check that all resources were created successfully:

```bash
# Check ServiceAccounts
kubectl get serviceaccounts -n ajasta | grep -E "reader-user|writer-user|admin-user"

# Check Roles
kubectl get roles -n ajasta | grep -E "reader-role|writer-role"

# Check ClusterRole
kubectl get clusterroles | grep admin-role

# Check RoleBindings
kubectl get rolebindings -n ajasta | grep -E "reader-binding|writer-binding"

# Check ClusterRoleBinding
kubectl get clusterrolebindings | grep admin-binding
```

### Step 3: Generate Kubeconfig Files

**IMPORTANT:** Before running the script, ensure your kubectl context has cluster-admin access:

```bash
# Check your current context
kubectl config current-context

# If KUBECONFIG is set to a service account config, unset it first
unset KUBECONFIG

# Verify you have admin access
kubectl auth can-i get secrets -n ajasta
# Should return "yes"
```

Run the generation script to create kubeconfig files for each service account:

```bash
# From the k8s directory
./generate-kubeconfigs.sh
```

The script will automatically check for proper permissions and warn you if `KUBECONFIG` is set to a non-admin configuration.

This will create three kubeconfig files in the `k8s/kubeconfigs/` directory:
- `kubeconfig-read.yaml` - For reader-user
- `kubeconfig-write.yaml` - For writer-user
- `kubeconfig-admin.yaml` - For admin-user

**Note:** The `kubeconfigs/` directory is excluded from git to prevent accidentally committing credentials.

## Usage

### Using Kubeconfig Files

There are two ways to use the generated kubeconfig files:

#### Option 1: Set KUBECONFIG Environment Variable

```bash
# Use reader kubeconfig
export KUBECONFIG=/path/to/ajasta-app/k8s/kubeconfigs/kubeconfig-read.yaml
kubectl get pods -n ajasta

# Use writer kubeconfig
export KUBECONFIG=/path/to/ajasta-app/k8s/kubeconfigs/kubeconfig-write.yaml
kubectl get deployments -n ajasta

# Use admin kubeconfig
export KUBECONFIG=/path/to/ajasta-app/k8s/kubeconfigs/kubeconfig-admin.yaml
kubectl get nodes
```

#### Option 2: Use --kubeconfig Flag

```bash
# Reader access
kubectl --kubeconfig=./kubeconfigs/kubeconfig-read.yaml get pods -n ajasta

# Writer access
kubectl --kubeconfig=./kubeconfigs/kubeconfig-write.yaml scale deployment/backend --replicas=3 -n ajasta

# Admin access
kubectl --kubeconfig=./kubeconfigs/kubeconfig-admin.yaml get nodes
```

### Testing Permissions

Test each user's permissions to verify RBAC is working correctly:

#### Test Reader (should succeed)
```bash
export KUBECONFIG=./kubeconfigs/kubeconfig-read.yaml
kubectl get pods -n ajasta           # ✓ Should work
kubectl get deployments -n ajasta    # ✓ Should work
kubectl logs <pod-name> -n ajasta    # ✓ Should work
```

#### Test Reader (should fail)
```bash
export KUBECONFIG=./kubeconfigs/kubeconfig-read.yaml
kubectl delete pod <pod-name> -n ajasta     # ✗ Should fail (forbidden)
kubectl create deployment test -n ajasta --image=nginx  # ✗ Should fail (forbidden)
kubectl get nodes                            # ✗ Should fail (forbidden)
```

#### Test Writer (should succeed)
```bash
export KUBECONFIG=./kubeconfigs/kubeconfig-write.yaml
kubectl get pods -n ajasta                   # ✓ Should work
kubectl scale deployment/backend --replicas=2 -n ajasta  # ✓ Should work
kubectl exec -it <pod-name> -n ajasta -- /bin/sh  # ✓ Should work
```

#### Test Writer (should fail)
```bash
export KUBECONFIG=./kubeconfigs/kubeconfig-write.yaml
kubectl get nodes                            # ✗ Should fail (forbidden)
kubectl create namespace test                # ✗ Should fail (forbidden)
kubectl get pods -n kube-system              # ✗ Should fail (forbidden)
```

#### Test Admin (should succeed)
```bash
export KUBECONFIG=./kubeconfigs/kubeconfig-admin.yaml
kubectl get pods -n ajasta                   # ✓ Should work
kubectl get nodes                            # ✓ Should work
kubectl get pods --all-namespaces            # ✓ Should work
kubectl create namespace test                # ✓ Should work
kubectl delete namespace test                # ✓ Should work
```

## Security Considerations

1. **Token Security**
   - Kubeconfig files contain sensitive tokens that grant access to the cluster
   - Store kubeconfig files securely and never commit them to version control
   - The `kubeconfigs/` directory is already in `.gitignore`

2. **Token Rotation**
   - Tokens created by the script are long-lived
   - For production, consider implementing token rotation policies
   - To revoke access, delete the corresponding Secret: `kubectl delete secret <user>-token -n ajasta`

3. **Principle of Least Privilege**
   - Distribute kubeconfigs based on actual needs
   - Use reader access for monitoring and observability
   - Use writer access for application deployment
   - Reserve admin access for cluster administrators only

4. **Audit Logging**
   - Enable Kubernetes audit logging to track actions performed by each user
   - Monitor for suspicious activity or permission violations

## Regenerating Kubeconfig Files

If you need to regenerate kubeconfig files (e.g., after token expiration or compromise):

```bash
# Delete existing token secrets
kubectl delete secret reader-user-token -n ajasta
kubectl delete secret writer-user-token -n ajasta
kubectl delete secret admin-user-token -n ajasta

# Regenerate kubeconfigs
./generate-kubeconfigs.sh
```

## Troubleshooting

### "Token creation timeout" or "Forbidden: cannot get resource secrets"

**Error message:**
```
ERROR: Token creation timeout for reader-user
Error from server (Forbidden): secrets "reader-user-token" is forbidden: 
User "system:serviceaccount:ajasta:reader-user" cannot get resource "secrets" in API group "" in the namespace "ajasta"
```

**Cause:** The `generate-kubeconfigs.sh` script is being run with a service account kubeconfig instead of cluster-admin credentials.

**Solution:**
1. Unset the KUBECONFIG environment variable:
   ```bash
   unset KUBECONFIG
   ```
2. Verify your default kubectl context has admin access:
   ```bash
   kubectl config current-context
   kubectl auth can-i get secrets -n ajasta
   ```
3. Run the script again:
   ```bash
   ./generate-kubeconfigs.sh
   ```

The script now includes automatic validation to detect this issue and provide guidance.

### "Error from server (Forbidden)" Messages

This usually means the RBAC configuration is working correctly and denying access as expected. Verify:
1. You're using the correct kubeconfig file
2. The operation is within the user's permissions
3. You're accessing the correct namespace

### Token Not Found or Expired

Regenerate the token secrets:
```bash
kubectl delete secret <user>-token -n ajasta
./generate-kubeconfigs.sh
```

### Script Fails to Generate Kubeconfigs

Ensure:
1. You have cluster-admin access with your current kubectl context
2. The RBAC manifests have been applied
3. The `ajasta` namespace exists
4. kubectl is properly configured

## Additional Resources

- [Kubernetes RBAC Documentation](https://kubernetes.io/docs/reference/access-authn-authz/rbac/)
- [Using RBAC Authorization](https://kubernetes.io/docs/reference/access-authn-authz/rbac/)
- [Service Account Tokens](https://kubernetes.io/docs/reference/access-authn-authz/service-accounts-admin/)

## Maintenance

### Adding New Users

To add a new user with custom permissions:

1. Create a new ServiceAccount in `12-rbac-serviceaccounts.yml`
2. Define a new Role or ClusterRole with specific permissions in `13-rbac-roles.yml`
3. Create a RoleBinding or ClusterRoleBinding in `14-rbac-bindings.yml`
4. Apply the updated manifests
5. Run `generate-kubeconfigs.sh` or create a custom token generation

### Modifying Permissions

To change permissions for existing roles:

1. Edit the appropriate Role or ClusterRole in `13-rbac-roles.yml`
2. Apply the updated manifest: `kubectl apply -f manifests/13-rbac-roles.yml`
3. Changes take effect immediately (no need to regenerate kubeconfigs)

### Removing Users

To revoke access:

```bash
# Delete the token secret
kubectl delete secret <user>-token -n ajasta

# Optionally delete the entire RBAC configuration
kubectl delete rolebinding <binding-name> -n ajasta
kubectl delete serviceaccount <user-name> -n ajasta
```
