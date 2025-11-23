# Terraform configuration for Ajasta Application Deployment
# This module deploys the Ajasta application using Docker containers on the created VMs

locals {
  # Application configuration
  app_network_name = "ajasta-app-net"

  # Container configurations
  postgres_container = {
    name  = "ajasta-postgres"
    image = "postgres:16-alpine"
    env_vars = {
      POSTGRES_DB     = var.postgres_db
      POSTGRES_USER   = var.postgres_user
      POSTGRES_PASSWORD = var.postgres_password
    }
    ports = {
      internal = 5432
      external = var.postgres_port
    }
    volumes = {
      postgres_data = "/var/lib/postgresql/data"
    }
  }

  backend_container = {
    name  = "ajasta-backend"
    image = var.backend_image
    env_vars = {
      # Database configuration
      DB_URL      = "jdbc:postgresql://${local.postgres_container.name}:5432/${var.postgres_db}"
      DB_USERNAME = var.postgres_user
      DB_PASSWORD = var.postgres_password

      # Application configuration
      JWT_SECRET = var.jwt_secret

      # Optional configurations
      MAIL_USERNAME               = var.mail_username
      MAIL_PASSWORD               = var.mail_password
      AWS_ACCESS_KEY_ID           = var.aws_access_key_id
      AWS_SECRET_ACCESS_KEY       = var.aws_secret_access_key
      AWS_REGION                  = var.aws_region
      AWS_S3_BUCKET               = var.aws_s3_bucket
      STRIPE_PUBLIC_KEY           = var.stripe_public_key
      STRIPE_SECRET_KEY           = var.stripe_secret_key

      # JVM configuration
      JAVA_OPTS = var.java_opts
    }
    ports = {
      internal = 8090
      external = var.backend_port
    }
    depends_on = [local.postgres_container.name]
  }

  frontend_container = {
    name  = "ajasta-frontend"
    image = var.frontend_image
    ports = {
      internal = 80
      external = var.frontend_port
    }
    depends_on = [local.backend_container.name]
  }
}

# Cloud-init script for Docker and application setup
data "template_file" "app_setup" {
  template = file("${path.module}/../scripts/app-setup-cloudinit.yaml")

  vars = {
    # Docker registry credentials (if needed)
    docker_registry     = var.docker_registry
    docker_username     = var.docker_username
    docker_password     = var.docker_password

    # Application configurations
    postgres_db         = var.postgres_db
    postgres_user       = var.postgres_user
    postgres_password   = var.postgres_password
    postgres_port       = var.postgres_port

    backend_image       = var.backend_image
    frontend_image      = var.frontend_image
    backend_port        = var.backend_port
    frontend_port       = var.frontend_port

    # Application secrets
    jwt_secret          = var.jwt_secret
    mail_username       = var.mail_username
    mail_password       = var.mail_password
    aws_access_key_id   = var.aws_access_key_id
    aws_secret_access_key = var.aws_secret_access_key
    aws_region          = var.aws_region
    aws_s3_bucket       = var.aws_s3_bucket
    stripe_public_key   = var.stripe_public_key
    stripe_secret_key   = var.stripe_secret_key
    java_opts           = var.java_opts

    # Network configuration
    app_network_name    = local.app_network_name
  }
}

# Deploy application on master node
resource "null_resource" "deploy_app_master" {
  depends_on = [
    yandex_compute_instance.master,
    yandex_compute_instance.workers
  ]

  # Use the master node for application deployment
  connection {
    type        = "ssh"
    host        = yandex_vpc_address.master.external_ipv4_address[0].address
    user        = var.ssh_username
    private_key = file(var.ssh_private_key_file)
    timeout     = "5m"
  }

  # Upload cloud-init script
  provisioner "file" {
    content     = data.template_file.app_setup.rendered
    destination = "/tmp/app-setup.yaml"
  }

  # Execute application setup
  provisioner "remote-exec" {
    inline = [
      "echo '🚀 Starting Ajasta application deployment...'",
      "sudo cloud-init clean",
      "sudo cloud-init init",
      "sudo cloud-init modules -m final",
      "sudo cloud-init single --file /tmp/app-setup.yaml",
      "echo '⏳ Waiting for application containers to start...'",
      "sleep 30",
      "echo '🔍 Checking application status...'",
      "sudo docker ps -a",
      "echo '✅ Ajasta application deployment completed!'"
    ]
  }
}

# Health check for the deployed application
resource "null_resource" "health_check" {
  depends_on = [null_resource.deploy_app_master]

  connection {
    type        = "ssh"
    host        = yandex_vpc_address.master.external_ipv4_address[0].address
    user        = var.ssh_username
    private_key = file(var.ssh_private_key_file)
    timeout     = "5m"
  }

  provisioner "remote-exec" {
    inline = [
      "echo '🏥 Performing application health check...'",
      "",
      "# Check PostgreSQL",
      "if sudo docker exec ${local.postgres_container.name} pg_isready -U ${var.postgres_user} -d ${var.postgres_db}; then",
      "  echo '✅ PostgreSQL database is healthy'",
      "else",
      "  echo '❌ PostgreSQL database is not healthy'",
      "fi",
      "",
      "# Check Backend API",
      "for i in {1..10}; do",
      "  if curl -f http://localhost:${var.backend_port}/api/health 2>/dev/null || curl -f http://localhost:${var.backend_port}/ 2>/dev/null; then",
      "    echo '✅ Backend API is healthy'",
      "    break",
      "  else",
      "    echo '⏳ Backend API not ready yet, retrying in 30s...'",
      "    sleep 30",
      "  fi",
      "done",
      "",
      "# Check Frontend",
      "if curl -f http://localhost:${var.frontend_port}/ 2>/dev/null; then",
      "  echo '✅ Frontend is healthy'",
      "else",
      "  echo '❌ Frontend is not healthy'",
      "fi",
      "",
      "# Show container logs for debugging",
      "echo '📋 Container logs:'",
      "sudo docker logs ${local.postgres_container.name} --tail 10",
      "sudo docker logs ${local.backend_container.name} --tail 10",
      "sudo docker logs ${local.frontend_container.name} --tail 10"
    ]
  }
}

# Create application startup script for manual use
resource "local_file" "app_startup_script" {
  filename = "${path.module}/../scripts/start-ajasta-app.sh"
  content  = <<-EOT
#!/bin/bash

# Ajasta Application Startup Script
# This script starts the Ajasta application containers

set -e

echo "🚀 Starting Ajasta Application..."

# Variables
POSTGRES_CONTAINER="${local.postgres_container.name}"
BACKEND_CONTAINER="${local.backend_container.name}"
FRONTEND_CONTAINER="${local.frontend_container.name}"
APP_NETWORK="${local.app_network_name}"

# Create Docker network if it doesn't exist
if ! sudo docker network inspect "$APP_NETWORK" &>/dev/null; then
    echo "📡 Creating Docker network: $APP_NETWORK"
    sudo docker network create "$APP_NETWORK"
fi

# Start PostgreSQL container
echo "🗄️ Starting PostgreSQL container..."
sudo docker run -d \
    --name "$POSTGRES_CONTAINER" \
    --network "$APP_NETWORK" \
    -e POSTGRES_DB=${var.postgres_db} \
    -e POSTGRES_USER=${var.postgres_user} \
    -e POSTGRES_PASSWORD=${var.postgres_password} \
    -p ${var.postgres_port}:5432 \
    -v postgres_data:/var/lib/postgresql/data \
    postgres:16-alpine

# Wait for PostgreSQL to be ready
echo "⏳ Waiting for PostgreSQL to be ready..."
until sudo docker exec "$POSTGRES_CONTAINER" pg_isready -U ${var.postgres_user} -d ${var.postgres_db}; do
    echo "   PostgreSQL not ready yet..."
    sleep 5
done

echo "✅ PostgreSQL is ready!"

# Start Backend container
echo "⚙️ Starting Backend container..."
sudo docker run -d \
    --name "$BACKEND_CONTAINER" \
    --network "$APP_NETWORK" \
    -e DB_URL="jdbc://postgresql://$POSTGRES_CONTAINER:5432/${var.postgres_db}" \
    -e DB_USERNAME=${var.postgres_user} \
    -e DB_PASSWORD=${var.postgres_password} \
    -e JWT_SECRET=${var.jwt_secret} \
    -e MAIL_USERNAME=${var.mail_username} \
    -e MAIL_PASSWORD=${var.mail_password} \
    -e AWS_ACCESS_KEY_ID=${var.aws_access_key_id} \
    -e AWS_SECRET_ACCESS_KEY=${var.aws_secret_access_key} \
    -e AWS_REGION=${var.aws_region} \
    -e AWS_S3_BUCKET=${var.aws_s3_bucket} \
    -e STRIPE_PUBLIC_KEY=${var.stripe_public_key} \
    -e STRIPE_SECRET_KEY=${var.stripe_secret_key} \
    -e JAVA_OPTS="${var.java_opts}" \
    -p ${var.backend_port}:8090 \
    ${var.backend_image}

# Wait for Backend to be ready
echo "⏳ Waiting for Backend to be ready..."
for i in {1..20}; do
    if curl -f http://localhost:${var.backend_port}/api/health 2>/dev/null || curl -f http://localhost:${var.backend_port}/ 2>/dev/null; then
        echo "✅ Backend is ready!"
        break
    else
        echo "   Backend not ready yet... (attempt $i/20)"
        sleep 10
    fi
done

# Start Frontend container
echo "🎨 Starting Frontend container..."
sudo docker run -d \
    --name "$FRONTEND_CONTAINER" \
    --network "$APP_NETWORK" \
    -p ${var.frontend_port}:80 \
    ${var.frontend_image}

# Wait for Frontend to be ready
echo "⏳ Waiting for Frontend to be ready..."
for i in {1..10}; do
    if curl -f http://localhost:${var.frontend_port}/ 2>/dev/null; then
        echo "✅ Frontend is ready!"
        break
    else
        echo "   Frontend not ready yet... (attempt $i/10)"
        sleep 5
    fi
done

echo ""
echo "🎉 Ajasta Application is now running!"
echo "📊 Application Status:"
sudo docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""
echo "🌐 Access URLs:"
echo "   Frontend: http://$(curl -s ifconfig.me):${var.frontend_port}/"
echo "   Backend API: http://$(curl -s ifconfig.me):${var.backend_port}/api"
echo ""
echo "📋 To view logs:"
echo "   sudo docker logs $BACKEND_CONTAINER -f"
echo "   sudo docker logs $FRONTEND_CONTAINER -f"
echo ""
echo "🛑 To stop the application:"
echo "   sudo docker stop $FRONTEND_CONTAINER $BACKEND_CONTAINER $POSTGRES_CONTAINER"
echo "   sudo docker rm $FRONTEND_CONTAINER $BACKEND_CONTAINER $POSTGRES_CONTAINER"
EOT
}