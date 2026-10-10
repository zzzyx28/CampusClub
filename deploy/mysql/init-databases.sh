#!/bin/bash
set -euo pipefail
# Passwords are generated as hexadecimal by init-cloud-env.py.
for value in "$USER_DB_PASSWORD" "$TICKET_DB_PASSWORD" "$AI_DB_PASSWORD"; do
 [[ "$value" =~ ^[a-zA-Z0-9_-]+$ ]] || { echo "Use alphanumeric database passwords" >&2; exit 1; }
done
MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot <<SQL
CREATE DATABASE IF NOT EXISTS campus_user CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS campus_ticket CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS campus_ai CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS 'campus_user'@'%' IDENTIFIED BY '$USER_DB_PASSWORD';
CREATE USER IF NOT EXISTS 'campus_ticket'@'%' IDENTIFIED BY '$TICKET_DB_PASSWORD';
CREATE USER IF NOT EXISTS 'campus_ai'@'%' IDENTIFIED BY '$AI_DB_PASSWORD';
GRANT ALL ON campus_user.* TO 'campus_user'@'%';
GRANT ALL ON campus_ticket.* TO 'campus_ticket'@'%';
GRANT ALL ON campus_ai.* TO 'campus_ai'@'%';
SQL
