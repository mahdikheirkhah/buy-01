#!/bin/bash
# Script to fix SonarQube project keys
# This will delete old projects and recreate them with correct keys

SONAR_URL="http://localhost:9000"
SONAR_TOKEN="${1:-admin}"  # Default to admin if no token provided

echo "🔍 Checking existing SonarQube projects..."
echo ""

# List all projects
curl -s -u "${SONAR_TOKEN}:" "${SONAR_URL}/api/projects/search" | jq '.components[] | {key: .key, name: .name}'

echo ""
echo "📋 Projects to fix:"
echo "  - buy01-backend → buy-01-backend"
echo "  - buy01-frontend → buy-01-frontend"
echo ""
read -p "Do you want to proceed with deletion and recreation? (y/n) " -n 1 -r
echo

if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo "🗑️  Deleting old projects..."
    
    # Delete buy01-backend if exists
    echo "Deleting buy01-backend..."
    curl -s -u "${SONAR_TOKEN}:" -X POST "${SONAR_URL}/api/projects/delete?project=buy01-backend"
    
    # Delete buy01-frontend if exists
    echo "Deleting buy01-frontend..."
    curl -s -u "${SONAR_TOKEN}:" -X POST "${SONAR_URL}/api/projects/delete?project=buy01-frontend"
    
    echo ""
    echo "✅ Old projects deleted"
    echo ""
    echo "📁 Creating new projects with correct keys..."
    
    # Create buy-01-backend
    echo "Creating buy-01-backend..."
    curl -s -u "${SONAR_TOKEN}:" -X POST \
        -F "project=buy-01-backend" \
        -F "name=Buy-01 Backend" \
        "${SONAR_URL}/api/projects/create"
    
    echo ""
    
    # Create buy-01-frontend
    echo "Creating buy-01-frontend..."
    curl -s -u "${SONAR_TOKEN}:" -X POST \
        -F "project=buy-01-frontend" \
        -F "name=Buy-01 Frontend" \
        "${SONAR_URL}/api/projects/create"
    
    echo ""
    echo ""
    echo "✅ Projects recreated successfully!"
    echo ""
    echo "🔍 Current projects:"
    curl -s -u "${SONAR_TOKEN}:" "${SONAR_URL}/api/projects/search" | jq '.components[] | {key: .key, name: .name}'
    
    echo ""
    echo "📊 Next steps:"
    echo "  1. Run a new SonarQube analysis from Jenkins"
    echo "  2. Verify the projects appear correctly in SonarQube UI"
    echo "  3. Configure quality gates if needed"
else
    echo "Aborted."
    exit 1
fi
