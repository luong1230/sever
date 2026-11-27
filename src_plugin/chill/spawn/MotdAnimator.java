package chill.spawn;

import java.io.File;
import java.nio.file.Files;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.Plugin;

public class MotdAnimator implements Listener, Runnable {
    private int step = 0;
    private String wssUrl = "wss://angies-irk-richness.ngrok-free.dev";

    public static void start(Plugin plugin) {
        MotdAnimator anim = new MotdAnimator();
        Bukkit.getPluginManager().registerEvents(anim, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, anim, 12L, 12L);
    }

    public void run() {
        step = (step + 1) % 2;
        try {
            File f = new File("wss_link.txt");
            if (!f.exists()) f = new File("../wss_link.txt");
            if (f.exists()) {
                String s = new String(Files.readAllBytes(f.toPath())).trim();
                if (s.startsWith("wss://")) wssUrl = s;
            }
        } catch (Exception ignored) {}
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPing(ServerListPingEvent e) {
        String c1 = (step == 0) ? "§c§l" : "§a§l";
        String c2 = (step == 0) ? "§a§l" : "§c§l";
        String line1 = "   §6§lCHILL MC §7- §aSinh Ton & Kinh Te §f[1.8 - 1.20]";
        String line2 = "  " + c1 + "§r  §b" + wssUrl + "  " + c2 + "§r";
        e.setMotd(line1 + "\n" + line2);
    }
}
