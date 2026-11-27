package chill.spawn;
import java.util.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.*;

public class SpawnZoneTask implements Listener, Runnable {
    private final Set step3Locked, unlogged;
    private final Random rng = new Random();
    private int tick = 0;

    public SpawnZoneTask(Set s, Set u) { this.step3Locked = s; this.unlogged = u; }
    public static void start(Plugin p, Set s, Set u) {
        SpawnZoneTask t = new SpawnZoneTask(s, u);
        Bukkit.getPluginManager().registerEvents(t, p);
        Bukkit.getScheduler().runTaskTimer(p, t, 5L, 5L);
    }
    private boolean inSp(Location l) {
        return l != null && l.getWorld() != null && l.getWorld().equals(Bukkit.getWorlds().get(0))
            && Math.abs(l.getBlockX() + 197) <= 60 && Math.abs(l.getBlockZ() - 34) <= 60;
    }
    private void dot(Player p, int x, double y, int z, int px, int pz, Particle.DustOptions d) {
        if (Math.abs(x - px) <= 32 && Math.abs(z - pz) <= 32)
            p.spawnParticle(Particle.REDSTONE, x + 0.5, y, z + 0.5, 2, 0.1, 0.4, 0.1, 0, d);
    }
    public void run() {
        tick++; Bukkit.setDefaultGameMode(GameMode.SURVIVAL);
        World w = Bukkit.getWorlds().get(0);
        Particle.DustOptions red = new Particle.DustOptions(Color.RED, 1.5f);
        for (Entity e : w.getEntities()) {
            if ((e instanceof Monster || e instanceof Slime || e instanceof Phantom || e instanceof Ghast || e instanceof Hoglin) && inSp(e.getLocation())) e.remove();
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().getName().equals("world_lobby") && !step3Locked.contains(p.getUniqueId())) {
                unlogged.remove(p.getUniqueId());
                if (!p.isOp() && p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SURVIVAL) p.setGameMode(GameMode.SURVIVAL);
            }
            if (tick % 4 == 0 && p.getWorld().equals(w)) {
                int px = p.getLocation().getBlockX(), pz = p.getLocation().getBlockZ(), r = 61;
                double py = p.getLocation().getY() + 0.5;
                for (int i = -r; i <= r; i += 2) {
                    dot(p, -197 + i, py, 34 - r, px, pz, red); dot(p, -197 + i, py, 34 + r, px, pz, red);
                    dot(p, -197 - r, py, 34 + i, px, pz, red); dot(p, -197 + r, py, 34 + i, px, pz, red);
                }
            }
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDmg(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && inSp(e.getEntity().getLocation()) && !UltimateChillSystems.isInActiveDuel(e.getEntity().getUniqueId())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.LOWEST)
    public void onRtp(PlayerCommandPreprocessEvent e) {
        String m = e.getMessage().trim().toLowerCase();
        if (!m.equals("/rtp") && !m.startsWith("/rtp ")) return;
        Player p = e.getPlayer();
        if (p.getWorld().getName().equals("world_lobby") || step3Locked.contains(p.getUniqueId())) return;
        e.setCancelled(true); World w = Bukkit.getWorlds().get(0);
        for (int k = 0; k < 25; k++) {
            int rx = rng.nextInt(5000) - 2500, rz = rng.nextInt(5000) - 2500;
            if (Math.abs(rx + 197) <= 120 && Math.abs(rz - 34) <= 120) continue;
            w.getChunkAt(rx >> 4, rz >> 4);
            Block top = w.getHighestBlockAt(rx, rz); Material mt = top.getType();
            if (top.getY() > w.getMinHeight() + 2 && mt.isSolid() && mt != Material.MAGMA_BLOCK && mt != Material.CACTUS && mt != Material.CAMPFIRE) {
                Block a1 = w.getBlockAt(rx, top.getY() + 1, rz), a2 = w.getBlockAt(rx, top.getY() + 2, rz);
                if (!a1.getType().isSolid() && !a2.getType().isSolid() && !a1.isLiquid()) {
                    p.teleport(new Location(w, rx + 0.5, top.getY() + 1.2, rz + 0.5));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 100, 4, false, false));
                    p.sendMessage("§a§l[Chill MC] §fĐã RTP an toàn tới §eX: " + rx + " Y: " + (top.getY() + 1) + " Z: " + rz + "§f!");
                    return;
                }
            }
        }
        p.sendMessage("§c§l[Chill MC] §fChưa tìm được đất trống, hãy gõ lại §e/rtp§f!");
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBrk(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (p.getWorld().getName().equals("world_lobby") || step3Locked.contains(p.getUniqueId())) { e.setCancelled(true); return; }
        if (inSp(e.getBlock().getLocation())) { if (!p.isOp()) e.setCancelled(true); }
        else { unlogged.remove(p.getUniqueId()); e.setCancelled(false); }
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlc(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (p.getWorld().getName().equals("world_lobby") || step3Locked.contains(p.getUniqueId())) { e.setCancelled(true); return; }
        if (inSp(e.getBlock().getLocation())) { if (!p.isOp()) e.setCancelled(true); }
        else { unlogged.remove(p.getUniqueId()); e.setCancelled(false); }
    }
}
