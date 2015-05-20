echo "Testing SLA REST API"
curl -i -X PUT -H "Content-Type: text/plain; charset=utf-8" -d @"overcommit" http://localhost:7777/v1/plug4green/test/cpuovercommit
