echo "Testing SLA REST API"
#set the CPU demand for <VMName> to 200% (two full cores)
curl -i -X PUT -H "Content-Type: text/plain" -d 200 http://localhost:7777/v1/plug4green/<VMname>/VMCPUDemand
