#!/bin/bash
set -e

echo "Setting up Keycloak configuration..."

# Configure kcadm credentials
echo "Configuring kcadm credentials..."
/opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 \
  --realm master \
  --user admin \
  --password admin

echo "Creating realm 'booking'..."
# Create realm
/opt/keycloak/bin/kcadm.sh create realms \
  -s realm=realworld \
  -s enabled=true \
  -s displayName="Realworld Realm"

echo "Creating client 'booking_client'..."
# Create client
/opt/keycloak/bin/kcadm.sh create clients -r realworld \
  -s clientId=realword_client \
  -s enabled=true \
  -s secret=dQaAn9lXGECBnoDQRn929yydl5ZIWTeK \
  -s publicClient=false \
  -s serviceAccountsEnabled=true \
  -s directAccessGrantsEnabled=true \
  -s standardFlowEnabled=true \
  -s 'redirectUris=["http://localhost:8080/*"]' \
  -s 'webOrigins=["http://localhost:8080"]' \
  -s protocol=openid-connect

echo "Creating user 'hello_world'..."
# Create user
/opt/keycloak/bin/kcadm.sh create users -r realworld \
  -s username=realworld_user \
  -s enabled=true \
  -s firstName=real \
  -s lastName=word \
  -s email=real@world.com

echo "Setting password for user 'hello_world'..."
# Set password
/opt/keycloak/bin/kcadm.sh set-password -r realworld \
  --username realworld_user \
  --new-password realworld \
  --temporary false

echo "Creating group 'admin'..."
# Create admin group
/opt/keycloak/bin/kcadm.sh create groups -r realworld \
  -s name=admin \
  -s 'attributes.description=["Administrator group"]'

echo "Adding user to admin group..."
# Get user ID
USER_ID=$(/opt/keycloak/bin/kcadm.sh get users -r realworld -q username=realworld_user --format csv --noquotes | tail -n 1 | cut -d',' -f1)

# Get group ID
GROUP_ID=$(/opt/keycloak/bin/kcadm.sh get groups -r realworld -q search=admin --format csv --noquotes | tail -n 1 | cut -d',' -f1)

# Add user to group
/opt/keycloak/bin/kcadm.sh update users/$USER_ID/groups/$GROUP_ID -r realworld -s realm=realworld -n

echo "Assigning realm management roles to user..."
# Add realm management roles to user
/opt/keycloak/bin/kcadm.sh add-roles -r realworld \
  --uusername hello_world \
  --cclientid realm-management \
  --rolename manage-users \
  --rolename view-users \
  --rolename query-users \
  --rolename manage-realm \
  --rolename manage-clients

echo "Creating realm admin user 'dragon'..."

# Create realm admin user based on environment variables
/opt/keycloak/bin/kcadm.sh create users -r realworld \
  -s username=dragon \
  -s enabled=true \
  -s firstName=Dragon \
  -s lastName=Admin \
  -s email=dragon@realworld.com

echo "Setting password for realm admin 'dragon'..."
# Set password for dragon user
/opt/keycloak/bin/kcadm.sh set-password -r realworld \
  --username dragon \
  --new-password hello \
  --temporary false

echo "Assigning admin roles to 'dragon' user..."
# Add realm management roles to dragon user
/opt/keycloak/bin/kcadm.sh add-roles -r realworld \
  --uusername dragon \
  --cclientid realm-management \
  --rolename manage-users \
  --rolename view-users \
  --rolename query-users \
  --rolename manage-realm \
  --rolename manage-clients \
  --rolename realm-admin

# Add dragon user to admin group
DRAGON_USER_ID=$(/opt/keycloak/bin/kcadm.sh get users -r realworld -q username=dragon --format csv --noquotes | tail -n 1 | cut -d',' -f1)
/opt/keycloak/bin/kcadm.sh update users/$DRAGON_USER_ID/groups/$GROUP_ID -r realworld -s realm=realworld -n

echo "Keycloak setup completed successfully!"

# Test the configuration
echo "Testing client credentials..."
curl -s -X POST \
  http://localhost:8080/realms/booking/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=client_credentials' \
  -d 'client_id=booking_client' \
  -d 'client_secret=dQaAn9lXGECBnoDQRn929yydl5ZIWTeK' | jq .

echo "Testing user login..."
curl -s -X POST \
  http://localhost:8080/realms/realworld/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d 'client_id=realworld_client' \
  -d 'client_secret=dQaAn9lXGECBnoDQRn929yydl5ZIWTeK' \
  -d 'username=realworld_user' \
  -d 'password=realworld'