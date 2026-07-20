#!/bin/sh
set -e

MASTER_HOST="${REDIS_MASTER_HOST:-redis}"
MASTER_PORT="${REDIS_MASTER_PORT:-6379}"
MASTER_SET="${REDIS_MASTER_SET:-scos_master}"

echo "Aguardando Redis master em ${MASTER_HOST}:${MASTER_PORT}..."
until redis-cli -h "${MASTER_HOST}" -p "${MASTER_PORT}" -a "${REDIS_PASSWORD}" ping 2>/dev/null | grep -q PONG; do
    sleep 2
done

# Redis Sentinel 8.x resolve hostname durante parsing do sentinel.conf,
# independente do shell. Usar IP evita o erro "Failed to resolve hostname".
MASTER_IP=$(getent hosts "${MASTER_HOST}" | awk '{print $1}')
echo "Redis master disponivel em ${MASTER_IP}."

cat > /tmp/sentinel.conf <<EOF
port 26379
sentinel monitor ${MASTER_SET} ${MASTER_IP} ${MASTER_PORT} 1
sentinel auth-pass ${MASTER_SET} ${REDIS_PASSWORD}
sentinel down-after-milliseconds ${MASTER_SET} ${REDIS_DOWN_AFTER_MS:-5000}
sentinel failover-timeout ${MASTER_SET} ${REDIS_FAILOVER_TIMEOUT:-30000}
sentinel parallel-syncs ${MASTER_SET} 1
EOF

exec redis-sentinel /tmp/sentinel.conf
