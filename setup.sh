#!/bin/bash

################################################################################
# Buy-01 E-Commerce Platform - Complete Setup & Start Script
# 
# This script:
# 1. Removes all Docker containers and images (optional)
# 2. Starts the core project (docker-compose.yml)
# 3. Optionally starts Jenkins (docker-compose.jenkins.yml)
#
# Usage:
#   ./setup.sh                    # Start core project (keeps existing data)
#   ./setup.sh --clean            # Clean Docker + start core project
#   ./setup.sh --jenkins          # Start core + Jenkins
#   ./setup.sh --ngrok            # Start core + ngrok tunnels
#   ./setup.sh --clean --jenkins  # Clean Docker + start core + Jenkins
#   ./setup.sh --jenkins --ngrok  # Start core + Jenkins + ngrok
#   ./setup.sh --help             # Show help
#
################################################################################

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Flags
CLEAN_DOCKER=false
START_JENKINS=false
START_NGROK=false

# Functions
print_header() {
    echo -e "\n${BLUE}╔════════════════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║${NC} $1"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════════════════════╝${NC}\n"
}

print_section() {
    echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

show_help() {
    cat << 'EOF'
Buy-01 E-Commerce Platform - Setup Script

USAGE:
    ./setup.sh [OPTIONS]

OPTIONS:
    --clean         Remove all Docker containers, images, and volumes before starting
    --jenkins       Start Jenkins (docker-compose.jenkins.yml) in addition to core project
    --ngrok         Start ngrok tunnels for external access to services
    --help          Show this help message

EXAMPLES:
    Start core project only (keep existing data):
        ./setup.sh

    Clean Docker and start core project:
        ./setup.sh --clean

    Start core project and Jenkins (for CI/CD):
        ./setup.sh --jenkins

    Start with ngrok for external access:
        ./setup.sh --ngrok

    Complete setup with Jenkins and ngrok:
        ./setup.sh --jenkins --ngrok

    Complete clean setup with Jenkins:
        ./setup.sh --clean --jenkins

WHAT THIS SCRIPT DOES:
    1. Validates Docker is installed and running
    2. Changes to project directory
    3. (Optional) Removes all Docker containers, images, and volumes
    4. Builds and starts core project services
    5. (Optional) Starts Jenkins for CI/CD pipeline testing
    6. Displays access URLs and health status

NOTES:
    - Core project (docker-compose.yml): Contains all microservices, database, frontend
    - Jenkins (docker-compose.jenkins.yml): Optional CI/CD pipeline environment
    - SonarQube: Runs with core project but is optional for code quality analysis
    - Data cleanup: Use --clean flag to remove all Docker data (WARNING: permanent!)

EOF
}

# Parse command line arguments
for arg in "$@"; do
    case $arg in
        --clean)
            CLEAN_DOCKER=true
            shift
            ;;
        --jenkins)
            START_JENKINS=true
            shift
            ;;
        --ngrok)
            START_NGROK=true
            shift
            ;;
        --help)
            show_help
            exit 0
            ;;
        *)
            print_error "Unknown option: $arg"
            echo ""
            show_help
            exit 1
            ;;
    esac
done

# Main script
main() {
    print_header "Buy-01 E-Commerce Platform Setup"

    # Check if Docker is installed
    print_section "1️⃣  Checking Docker Installation"
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed"
        echo "Please install Docker Desktop from https://www.docker.com/products/docker-desktop"
        exit 1
    fi
    print_success "Docker is installed: $(docker --version)"

    # Check if Docker daemon is running
    if ! docker info &> /dev/null; then
        print_error "Docker daemon is not running"
        echo "Please start Docker Desktop"
        exit 1
    fi
    print_success "Docker daemon is running"

    # Check if docker-compose is available
    if ! command -v docker-compose &> /dev/null; then
        print_error "docker-compose is not found"
        echo "It should be included with Docker Desktop"
        exit 1
    fi
    print_success "docker-compose is available: $(docker-compose --version)"

    # Change to project directory
    print_section "2️⃣  Changing to Project Directory"
    cd "$SCRIPT_DIR"
    print_success "Current directory: $(pwd)"

    # Verify docker-compose files exist
    print_section "3️⃣  Verifying Project Files"
    if [ ! -f "docker-compose.yml" ]; then
        print_error "docker-compose.yml not found in $SCRIPT_DIR"
        exit 1
    fi
    print_success "Found docker-compose.yml"

    if [ "$START_JENKINS" = true ] && [ ! -f "docker-compose.jenkins.yml" ]; then
        print_error "docker-compose.jenkins.yml not found in $SCRIPT_DIR"
        exit 1
    fi
    if [ "$START_JENKINS" = true ]; then
        print_success "Found docker-compose.jenkins.yml"
    fi

    # Clean Docker if requested
    if [ "$CLEAN_DOCKER" = true ]; then
        print_section "4️⃣  Cleaning Docker (This will remove ALL containers, images, and volumes)"
        print_warning "This is a destructive operation - all Docker data will be removed"
        
        read -p "Are you sure? Type 'yes' to continue: " -r
        echo
        if [[ $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
            print_info "Stopping all running containers..."
            docker-compose down 2>/dev/null || true
            if [ "$START_JENKINS" = true ]; then
                docker-compose -f docker-compose.jenkins.yml down 2>/dev/null || true
            fi
            
            print_info "Removing all Docker containers..."
            docker container prune -f --filter "label!=keep-me" 2>/dev/null || docker rm -f $(docker ps -aq) 2>/dev/null || true
            
            print_info "Removing all Docker images..."
            docker image prune -af 2>/dev/null || docker rmi -f $(docker images -q) 2>/dev/null || true
            
            print_info "Removing all Docker volumes..."
            docker volume prune -f 2>/dev/null || true
            
            print_success "Docker cleanup completed"
        else
            print_warning "Docker cleanup skipped"
        fi
    else
        print_section "4️⃣  Skipping Docker Cleanup (use --clean to clean)"
        print_info "Existing containers and data will be preserved"
    fi

    # Start core project
    print_section "5️⃣  Starting Core Project (docker-compose.yml)"
    print_info "This starts all microservices, database, frontend, and optional SonarQube..."
    
    docker-compose down 2>/dev/null || true
    docker-compose up -d
    
    if [ $? -eq 0 ]; then
        print_success "Core project started successfully"
    else
        print_error "Failed to start core project"
        exit 1
    fi

    # Start Jenkins if requested
    if [ "$START_JENKINS" = true ]; then
        print_section "6️⃣  Starting Jenkins (docker-compose.jenkins.yml)"
        print_info "This starts Jenkins for CI/CD pipeline testing..."
        
        docker-compose -f docker-compose.jenkins.yml up -d
        
        if [ $? -eq 0 ]; then
            print_success "Jenkins started successfully"
        else
            print_error "Failed to start Jenkins"
            exit 1
        fi
    else
        print_section "6️⃣  Skipping Jenkins (use --jenkins to start)"
    fi

    # Start ngrok if requested
    if [ "$START_NGROK" = true ]; then
        print_section "7️⃣  Starting ngrok Tunnels"
        
        # Check if ngrok is installed
        if ! command -v ngrok &> /dev/null; then
            print_error "ngrok is not installed"
            echo ""
            echo "Install ngrok:"
            echo "  macOS:   brew install ngrok/ngrok/ngrok"
            echo "  Linux:   snap install ngrok"
            echo "  Manual:  https://ngrok.com/download"
            echo ""
            echo "Then authenticate: ngrok config add-authtoken <your-token>"
            echo "Get token from: https://dashboard.ngrok.com/get-started/your-authtoken"
            print_warning "Continuing without ngrok..."
            START_NGROK=false
        else
            print_info "Starting ngrok tunnel for frontend (port 4200)..."
            ngrok http 4200 --log=stdout > /dev/null 2>&1 &
            NGROK_PID_FRONTEND=$!
            
            if [ "$START_JENKINS" = true ]; then
                print_info "Starting ngrok tunnel for Jenkins (port 8080)..."
                ngrok http 8080 --log=stdout > /dev/null 2>&1 &
                NGROK_PID_JENKINS=$!
            fi
            
            print_success "ngrok tunnels started in background"
            print_info "ngrok PIDs: Frontend=$NGROK_PID_FRONTEND$([ \"$START_JENKINS\" = true ] && echo \", Jenkins=$NGROK_PID_JENKINS\")"
            
            # Save PIDs for cleanup
            echo $NGROK_PID_FRONTEND > .ngrok_pids
            [ "$START_JENKINS" = true ] && echo $NGROK_PID_JENKINS >> .ngrok_pids
        fi
    else
        print_section "7️⃣  Skipping ngrok (use --ngrok for external access)"
    fi

    # Wait for services to be healthy
    print_section "8️⃣  Waiting for Services to Start (this may take 1-2 minutes)"
    
    print_info "Building images and starting containers..."
    sleep 10
    
    # Check services
    print_section "9️⃣  Service Status"
    
    # Count running containers
    CONTAINER_COUNT=$(docker ps --format '{{.Names}}' | wc -l)
    print_info "Running containers: $CONTAINER_COUNT"
    
    # List running services
    print_info "Running services:"
    docker ps --format "table {{.Names}}\t{{.Status}}" | tail -n +2 | sed 's/^/  /'

    # Display access URLs
    print_section "🔟 📲 Application URLs"
    
    cat << 'URLS'
Core Project Services:
  🌐 Frontend          https://localhost:4200
  🔧 API Gateway       https://localhost:8443/actuator/health
  🎯 Eureka Discovery  http://localhost:8761
  📊 SonarQube         http://localhost:9000 (admin/admin)

Services & Ports:
  👤 User Service      http://localhost:8081
  📦 Product Service   http://localhost:8082
  🎨 Media Service     http://localhost:8083
  📨 Kafka Broker      localhost:9092
  🗂️  MongoDB           localhost:27017 (admin/password)

URLS

    if [ "$START_JENKINS" = true ]; then
        echo ""
        echo "Jenkins (CI/CD):"
        echo "  🔄 Jenkins            http://localhost:8080"
        echo ""
        print_info "Getting Jenkins initial password..."
        JENKINS_PASSWORD=$(docker exec jenkins-cicd cat /var/jenkins_home/secrets/initialAdminPassword 2>/dev/null || echo "See logs for password")
        echo "  Initial password: $JENKINS_PASSWORD"
    fi

    if [ "$START_NGROK" = true ]; then
        echo ""
        echo "🌍 External Access (ngrok):"
        echo ""
        print_info "Ngrok tunnels are running in background..."
        sleep 3
        
        # Get ngrok URLs from API
        NGROK_TUNNELS=$(curl -s http://localhost:4040/api/tunnels 2>/dev/null)
        
        if [ -n "$NGROK_TUNNELS" ]; then
            FRONTEND_URL=$(echo "$NGROK_TUNNELS" | grep -o 'https://[a-zA-Z0-9-]*\.ngrok-free\.app' | head -1)
            JENKINS_URL=$(echo "$NGROK_TUNNELS" | grep -o 'https://[a-zA-Z0-9-]*\.ngrok-free\.app' | tail -1)
            
            echo "  🌐 Frontend (External): $FRONTEND_URL"
            if [ "$START_JENKINS" = true ]; then
                echo "  🔄 Jenkins (External):  $JENKINS_URL"
                echo ""
                echo "  📡 GitHub Webhook URL:"
                echo "     $JENKINS_URL/github-webhook/"
                echo ""
                print_info "Copy the webhook URL above to GitHub repository settings:"
                echo "     Repo → Settings → Webhooks → Add webhook"
                echo "     Payload URL: [paste webhook URL]"
                echo "     Content type: application/json"
                echo "     Events: Just the push event"
            fi
            echo ""
            print_info "Ngrok dashboard: http://localhost:4040"
        else
            print_warning "Ngrok tunnels starting... Check http://localhost:4040 for URLs"
        fi
    fi

    # Display next steps
    print_section "🎯 Next Steps"
    
    cat << 'NEXTSTEPS'
1. Wait a moment for all services to fully start (watch Docker logs)
2. Open Frontend: https://localhost:4200
3. Click "Sign Up" to create an account
4. Choose role: SELLER (to test all features)
5. Create products and upload images
6. (Optional) Check SonarQube at http://localhost:9000
7. (Optional) Check Jenkins at http://localhost:8080 (if started with --jenkins)

NEXTSTEPS

    if [ "$START_JENKINS" = true ] && [ "$START_NGROK" = true ]; then
        echo ""
        echo "📡 Jenkins + GitHub Webhook Setup:"
        echo "1. Get the Jenkins webhook URL from the ngrok output above"
        echo "2. Go to GitHub: Your-Repo → Settings → Webhooks → Add webhook"
        echo "3. Paste the webhook URL (example: https://xxx.ngrok-free.app/github-webhook/)"
        echo "4. Content type: application/json"
        echo "5. Select: Just the push event"
        echo "6. Click 'Add webhook'"
        echo "7. Push code to test automatic build trigger"
        echo ""
    fi

    # Display useful commands
    print_section "📝 Useful Commands"
    
    cat << 'COMMANDS'
View logs:
  docker-compose logs -f                 # All services
  docker-compose logs -f api-gateway     # Specific service

Stop services:
  docker-compose down                    # Stop core project
  docker-compose -f docker-compose.jenkins.yml down    # Stop Jenkins

Stop everything:
  docker-compose down
  docker-compose -f docker-compose.jenkins.yml down

Check container status:
  docker ps                              # Running containers
  docker stats                           # Resource usage

Access database:
  docker exec -it buy-01 mongosh --username admin --password password

Stop ngrok tunnels (if running):
  kill $(cat .ngrok_pids) 2>/dev/null && rm .ngrok_pids

Remove all Docker data (WARNING: permanent):
  docker system prune -a --volumes --force

COMMANDS

    print_section "✅ Setup Complete!"
    print_success "Your Buy-01 E-Commerce platform is ready to use!"
    echo ""
    print_info "If services aren't responding yet, wait a moment and try again"
    print_info "View logs with: docker-compose logs -f"
}

# Run main function
main
