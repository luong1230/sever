#!/bin/bash
echo "=== [1/3] Dang don tien trinh cu... ==="
pkill -9 -f bungee.jar 2>/dev/null
pkill -9 -f server.jar 2>/dev/null
pkill -f ngrok 2>/dev/null
pkill -9 -f pinggy.io 2>/dev/null
pkill -9 -f "java_link" 2>/dev/null
pkill -9 -f "pe_link" 2>/dev/null
rm -f /tmp/java.log /tmp/pe.log
sleep 2

echo "=== [2/3] Dang bat BungeeCord (8081) & Server Paper... ==="
cd /workspaces/sever/mc/bungee
screen -dmS bungee java -Xmx512M -jar bungee.jar
sleep 5
cd /workspaces/sever/mc
screen -dmS paper /usr/lib/jvm/java-17-openjdk-amd64/bin/java -Xmx4G -jar server.jar nogui
sleep 15

echo "=== [3/3] Dang mo cong Poja, Eaglercraft & MCPE... ==="
cd /workspaces/sever
nohup ngrok http --url=angles-irk-richness.ngrok-free.dev 8081 >/dev/null 2>&1 &
screen -dmS java_link bash -c "while true; do ssh -p 443 -o StrictHostKeyChecking=no -o ServerAliveInterval=30 -R0:localhost:25565 tcp@a.pinggy.io > /tmp/java.log 2>&1; sleep 2; done"
screen -dmS pe_link bash -c "while true; do ssh -p 443 -o StrictHostKeyChecking=no -o ServerAliveInterval=30 -R0:localhost:19132 udp@a.pinggy.io > /tmp/pe.log 2>&1; sleep 2; done"
sleep 6

JAVA_URL=$(grep -oE '[a-z0-9-]+\.run\.pinggy-free\.link:[0-9]+' /tmp/java.log | head -n 1)
PE_URL=$(grep -oE '[a-z0-9-]+\.run\.pinggy-free\.link:[0-9]+' /tmp/pe.log | head -n 1)

echo ""
echo "=================================================================================================================================="
printf "%-50s %-50s %s\n" "link poja" "link eaglecraft" "link mcpe"
printf "%-50s %-50s %s\n" "$JAVA_URL" "wss://angles-irk-richness.ngrok-free.dev" "$PE_URL"
echo ""
printf "%68s\n" "CHILL MC"
echo "=================================================================================================================================="
