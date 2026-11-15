mkdir -p mc/plugins && cd mc
echo "eula=true" > eula.txt
printf "online-mode=false\nserver-port=25565\n" > server.properties
U1="api.papermc.io/v2/projects/paper/versions/1.20.1/builds/196/downloads/paper-1.20.1-196.jar"
U2="://github.com"
curl -L -o server.jar "https://$U1"
curl -L -o plugins/EaglerXServer.jar "https://$U2"
java -Xmx2G -jar server.jar nogui
