#!/bin/bash

BASE_URL=${1:-http://localhost:8080}

echo "🚨 Simulating RDBMS outage on $BASE_URL..."
for i in {1..50}; do
  curl -s -X POST $BASE_URL/api/signals \
    -H "Content-Type: application/json" \
    -d '{"componentId":"DB_PRIMARY","componentType":"RDBMS","errorCode":"CONNECTION_REFUSED","errorMessage":"Database primary node failure","severity":"CRITICAL"}' &
done 
wait
echo "✅ Sent 50 RDBMS signals"

sleep 2

echo "📢 Simulating Cache failure..."
for i in {1..30}; do
  curl -s -X POST $BASE_URL/api/signals \
    -H "Content-Type: application/json" \
    -d '{"componentId":"CACHE_CLUSTER_01","componentType":"CACHE","errorCode":"EVICTION_HIGH","errorMessage":"Cache eviction rate exceeded threshold","severity":"HIGH"}' &
done
wait
echo "✅ Sent 30 Cache signals"

sleep 2

echo "⚠️ Simulating API timeout..."
for i in {1..20}; do
  curl -s -X POST $BASE_URL/api/signals \
    -H "Content-Type: application/json" \
    -d '{"componentId":"API_GATEWAY","componentType":"API","errorCode":"TIMEOUT","errorMessage":"API gateway response timeout","severity":"HIGH"}' &
done
wait
echo "✅ Sent 20 API signals"

echo ""
echo "🎯 Done! Check dashboard — should show 3 incidents"
