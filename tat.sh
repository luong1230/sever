#!/bin/bash
echo "=== Dang luu map va tat toan bo server... ==="
screen -S paper -X stuff "stop^M" 2>/dev/null
screen -S bungee -X stuff "end^M" 2>/dev/null
sleep 6
pkill -9 -f bungee.jar 2>/dev/null
pkill -9 -f server.jar 2>/dev/null
pkill -f ngrok
pkill -9 -f pinggy.io 2>/dev/null 2>/dev/null
echo "=== DA TAT SACH SE VA LUU MAP AN TOAN! ==="
