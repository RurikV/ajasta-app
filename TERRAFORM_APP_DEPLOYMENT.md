# Terraform Ajasta Application Deployment Guide

##  **Overview**

This guide shows how to deploy the **complete Ajasta application** using Terraform, including:

- ✅ **Kubernetes cluster infrastructure** (VMs, networking)
- ✅ **Application deployment** (Docker containers)
- ✅ **Database setup** (PostgreSQL)
- ✅ **Monitoring and management tools**

##  **Architecture**

### **What Terraform Deploys:**

1. **Infrastructure Layer:**
   - Kubernetes cluster VMs (1 master + 3 workers)
   - VPC networks and subnets
   - Static IP addresses
   - Security and access configuration

2. **Application Layer:**
   - **PostgreSQL database** (Docker container)
   - **Spring Boot backend** (Docker container)
   - **React frontend** (Docker container)
   - **Docker networking** and orchestration

3. **Management Layer:**
   - Health checks and monitoring
   - Log management
   - Backup and recovery scripts
   - SSH access and management tools

##  **Quick Deployment**

### **Option 1: GitLab CI/CD (Recommended)**

1. **Push to `develop` branch:**
   ```bash
   git add .
   git commit -m "Deploy Ajasta app via Terraform"
   git push origin develop
   ```

2. **Monitor the pipeline:**
   - Go to GitLab → CI/CD → Pipelines
   - Infrastructure deploys automatically
   - Application deploys on the created VMs

3. **Access the application:**
   - Frontend: `http://<MASTER_IP>:3000`
   - Backend API: `http://<MASTER_IP>:8090/api`

### **Option 2: Manual Terraform Deployment**

```bash
# 1. Navigate to terraform directory
cd terraform

# 2. Generate terraform.tfvars
./scripts/terraform-setup.sh

# 3. Initialize Terraform
terraform init

# 4. Plan the deployment
terraform plan

# 5. Deploy everything (infrastructure + application)
terraform apply
```

## ⚙️ **Configuration**

### **Application Variables**

Edit `terraform/terraform.tfvars` or set environment variables:

```hcl
# Enable application deployment
deploy_app = true

# Docker Images
backend_image  = "vladimirryrik/ajasta-backend:alpine"
frontend_image = "vladimirryrik/ajasta-frontend:alpine"

# Application Ports
frontend_port = 3000
backend_port  = 8090
postgres_port = 5432

# Database Configuration
postgres_db     = "ajastadb"
postgres_user   = "admin"
postgres_password = "secure-password-here"

# Application Secrets
jwt_secret = "your-jwt-secret-key-here"

# Optional Services
mail_username     = "your-email@example.com"
mail_password     = "your-email-password"
aws_access_key_id = "your-aws-access-key"
aws_secret_access_key = "your-aws-secret-key"
stripe_public_key = "your-stripe-public-key"
stripe_secret_key = "your-stripe-secret-key"
```

### **Custom Docker Registry**

If using a private Docker registry:

```hcl
docker_registry = "registry.gitlab.com"
docker_username = "your-gitlab-username"
docker_password = "your-gitlab-token"
```

## 🌐 **Accessing the Application**

### **After Deployment**

Once Terraform completes, you'll get outputs like:

```bash
# Application URLs
Frontend:  http://51.250.100.218:3000
Backend API: http://51.250.100.218:8090/api

# Management Commands
ssh ajasta@51.250.100.218 'cd /opt/ajasta-app && ./status-app.sh'
```

### **SSH Access**

```bash
# Connect to the application server
ssh ajasta@<MASTER_IP>

# View application status
cd /opt/ajasta-app
./status-app.sh

# View logs
docker-compose logs -f

# Restart application
docker-compose restart
```

## 🔧 **Application Management**

### **Useful Scripts**

The deployment creates these management scripts on the VM:

```bash
# Start the application
/opt/ajasta-app/start-app.sh

# Stop the application
/opt/ajasta-app/stop-app.sh

# Check application status
/opt/ajasta-app/status-app.sh
```

### **Docker Commands**

```bash
# View running containers
docker ps

# View container logs
docker logs ajasta-backend
docker logs ajasta-frontend
docker logs ajasta-postgres

# Access database
docker exec -it ajasta-postgres psql -U admin -d ajastadb

# Restart specific service
docker-compose restart backend
```

### **Health Monitoring**

```bash
# Check application health
curl http://localhost:3000/          # Frontend
curl http://localhost:8090/api        # Backend API

# Check database connection
docker exec ajasta-postgres pg_isready -U admin -d ajastadb
```

## 🔄 **Updates and Maintenance**

### **Updating the Application**

```bash
# Connect to the server
ssh ajasta@<MASTER_IP>

# Navigate to app directory
cd /opt/ajasta-app

# Pull latest images and restart
docker-compose pull
docker-compose up -d

# Check status
./status-app.sh
```

### **Updating Terraform Configuration**

```bash
# Make changes to terraform files
# Then apply updates
cd terraform
terraform plan
terraform apply
```

### **Backup and Recovery**

```bash
# Backup database
docker exec ajasta-postgres pg_dump -U admin ajastadb > backup.sql

# Restore database
docker exec -i ajasta-postgres psql -U admin ajastadb < backup.sql

# Backup application data
tar -czf app-data-backup.tar.gz /opt/ajasta-app/data/
```

## 🐛 **Troubleshooting**

### **Common Issues**

1. **Application won't start:**
   ```bash
   # Check logs
   docker-compose logs

   # Check container status
   docker ps -a

   # Restart services
   cd /opt/ajasta-app && docker-compose restart
   ```

2. **Database connection issues:**
   ```bash
   # Check PostgreSQL status
   docker exec ajasta-postgres pg_isready -U admin -d ajastadb

   # Check network connectivity
   docker network ls
   docker network inspect ajasta-app-net
   ```

3. **Cannot access application from external IP:**
   ```bash
   # Check firewall rules
   sudo ufw status

   # Check if ports are open
   sudo netstat -tlnp | grep -E ':(3000|8090|5432)'
   ```

### **Debug Mode**

Enable debug logging by setting environment variable:

```bash
# Add to terraform.tfvars
app_environment = "development"

# Or set directly on server
export LOG_LEVEL=DEBUG
```

## 📊 **Monitoring**

### **Resource Usage**

```bash
# View container resource usage
docker stats

# View system resources
htop
df -h
free -h
```

### **Application Logs**

```bash
# View all logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f postgres

# View application-specific logs
ls -la /opt/ajasta-app/logs/
tail -f /opt/ajasta-app/logs/backend/application.log
```

## 🔐 **Security**

### **Best Practices**

1. **Change default passwords:**
   - Database password
   - JWT secret
   - SSH keys

2. **Use HTTPS in production:**
   - Set `enable_ssl = true`
   - Provide SSL certificates

3. **Regular backups:**
   - Database backups
   - Configuration files
   - Application data

### **Firewall Rules**

```bash
# Allow only necessary ports
sudo ufw allow 22    # SSH
sudo ufw allow 3000  # Frontend
sudo ufw allow 8090  # Backend API
sudo ufw enable
```

