# Outputs for Ajasta Application Deployment

output "app_deployment_status" {
  description = "Status of the application deployment"
  value = var.deploy_app ? "Application deployed successfully" : "Application deployment skipped"
}

output "frontend_url" {
  description = "URL for accessing the frontend application"
  value = var.deploy_app ? "http://${yandex_vpc_address.master.external_ipv4_address[0].address}:${var.frontend_port}" : null
}

output "backend_api_url" {
  description = "URL for accessing the backend API"
  value = var.deploy_app ? "http://${yandex_vpc_address.master.external_ipv4_address[0].address}:${var.backend_port}/api" : null
}

output "database_info" {
  description = "Database connection information"
  value = var.deploy_app ? {
    host     = "${yandex_vpc_address.master.external_ipv4_address[0].address}:${var.postgres_port}"
    database = var.postgres_db
    username = var.postgres_user
  } : null
  sensitive = true
}

output "application_management" {
  description = "Commands for managing the application"
  value = var.deploy_app ? {
    start   = "ssh ${var.ssh_username}@${yandex_vpc_address.master.external_ipv4_address[0].address} 'cd /opt/ajasta-app && ./start-app.sh'"
    stop    = "ssh ${var.ssh_username}@${yandex_vpc_address.master.external_ipv4_address[0].address} 'cd /opt/ajasta-app && ./stop-app.sh'"
    status  = "ssh ${var.ssh_username}@${yandex_vpc_address.master.external_ipv4_address[0].address} 'cd /opt/ajasta-app && ./status-app.sh'"
    logs    = "ssh ${var.ssh_username}@${yandex_vpc_address.master.external_ipv4_address[0].address} 'cd /opt/ajasta-app && docker-compose logs -f'"
  } : null
}

output "container_info" {
  description = "Information about deployed containers"
  value = var.deploy_app ? {
    postgres_container = "ajasta-postgres"
    backend_container  = "ajasta-backend"
    frontend_container = "ajasta-frontend"
    network_name       = "ajasta-app-net"
  } : null
}

output "access_credentials" {
  description = "Access credentials and configuration"
  value = var.deploy_app ? {
    ssh_host     = yandex_vpc_address.master.external_ipv4_address[0].address
    ssh_user     = var.ssh_username
    ssh_command  = "ssh ${var.ssh_username}@${yandex_vpc_address.master.external_ipv4_address[0].address}"
    environment  = var.app_environment
  } : null
  sensitive = true
}

output "health_check_urls" {
  description = "URLs for health checking the application"
  value = var.deploy_app ? {
    frontend_health = "http://${yandex_vpc_address.master.external_ipv4_address[0].address}:${var.frontend_port}/"
    backend_health  = "http://${yandex_vpc_address.master.external_ipv4_address[0].address}:${var.backend_port}/actuator/health"
    database_check  = "ssh ${var.ssh_username}@${yandex_vpc_address.master.external_ipv4_address[0].address} 'docker exec ajasta-postgres pg_isready -U ${var.postgres_user} -d ${var.postgres_db}'"
  } : null
}

output "deployment_commands" {
  description = "Useful commands for deployment management"
  value = var.deploy_app ? {
    connect_to_master = "ssh ${var.ssh_username}@${yandex_vpc_address.master.external_ipv4_address[0].address}"
    view_containers   = "ssh ${var.ssh_username}@${yandex_vpc_address.master.external_ipv4_address[0].address} 'docker ps -a'"
    view_logs         = "ssh ${var.ssh_username}@${yandex_vpc_address.master.external_ipv4_address[0].address} 'docker-compose logs -f'"
    restart_app       = "ssh ${var.ssh_username}@${yandex_vpc_address.master.external_ipv4_address[0].address} 'cd /opt/ajasta-app && docker-compose restart'"
    update_app        = "ssh ${var.ssh_username}@${yandex_vpc_address.master.external_ipv4_address[0].address} 'cd /opt/ajasta-app && docker-compose pull && docker-compose up -d'"
  } : null
}

output "configuration_files" {
  description = "Paths to important configuration files"
  value = var.deploy_app ? {
    app_env_file      = "/opt/ajasta-app/.env"
    docker_compose    = "/opt/ajasta-app/docker-compose.yml"
    app_scripts       = "/opt/ajasta-app/start-app.sh"
    logs_directory    = "/opt/ajasta-app/logs"
    data_directory    = "/opt/ajasta-app/data"
  } : null
}

output "monitoring_info" {
  description = "Monitoring and debugging information"
  value = var.deploy_app ? {
    resource_usage = "ssh ${var.ssh_username}@${yandex_vpc_address.master.external_ipv4_address[0].address} 'docker stats --no-stream'"
    system_info    = "ssh ${var.ssh_username}@${yandex_vpc_address.master.external_ipv4_address[0].address} 'htop'"
    disk_usage     = "ssh ${var.ssh_username}@${yandex_vpc_address.master.external_ipv4_address[0].address} 'df -h'"
    memory_usage   = "ssh ${var.ssh_username}@${yandex_vpc_address.master.external_ipv4_address[0].address} 'free -h'"
  } : null
}