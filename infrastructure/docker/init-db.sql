-- Create all service databases
CREATE DATABASE auth_db;
CREATE DATABASE product_db;
CREATE DATABASE order_db;
CREATE DATABASE keycloak_db;

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE auth_db TO smartcommerce;
GRANT ALL PRIVILEGES ON DATABASE product_db TO smartcommerce;
GRANT ALL PRIVILEGES ON DATABASE order_db TO smartcommerce;
GRANT ALL PRIVILEGES ON DATABASE keycloak_db TO smartcommerce;
