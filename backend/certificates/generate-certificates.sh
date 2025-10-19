#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

echo -e "${GREEN}Generating certificates for all microservices...${NC}"

SERVICES=("discovery-service" "api-gateway" "user-service" "product-service" "media-service")

for SERVICE in "${SERVICES[@]}"; do
    echo -e "${GREEN}Generating certificate for $SERVICE...${NC}"
    
    # Create service directory
    mkdir -p keystores/$SERVICE
    
    # Generate private key
    openssl genrsa -out keystores/$SERVICE/$SERVICE-key.pem 2048
    
    # Create certificate signing request
    openssl req -new -key keystores/$SERVICE/$SERVICE-key.pem -out keystores/$SERVICE/$SERVICE.csr -subj "/C=US/ST=State/L=City/O=Organization/CN=$SERVICE"
    
    # Sign the certificate with CA
    openssl x509 -req -in keystores/$SERVICE/$SERVICE.csr -CA ca/ca-cert.pem -CAkey ca/ca-key.pem -CAcreateserial -out keystores/$SERVICE/$SERVICE-cert.pem -days 365 -sha256
    
    # Create PKCS12 keystore for Java
    openssl pkcs12 -export -in keystores/$SERVICE/$SERVICE-cert.pem -inkey keystores/$SERVICE/$SERVICE-key.pem -out keystores/$SERVICE/$SERVICE-keystore.p12 -name $SERVICE -password pass:changeit
    
    # Import CA certificate into keystore (for trusting other services)
    keytool -import -trustcacerts -alias root-ca -file ca/ca-cert.pem -keystore keystores/$SERVICE/$SERVICE-keystore.p12 -storepass changeit -noprompt
    
    echo -e "${GREEN}Certificate for $SERVICE created successfully!${NC}"
done

echo -e "${GREEN}All certificates generated!${NC}"
