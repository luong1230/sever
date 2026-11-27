package chill.spawn;
import java.util.*;
import net.md_5.bungee.api.chat.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class ChatShowEcFeature implements Listener, CommandExecutor {
    private final JavaPlugin plugin;
    private final Map<String, ItemStack> items = new HashMap<>();
    private final Map<String, ItemStack[]> ecs = new HashMap<>();
    private final Map<String, String> owners = new HashMap<>();
    public ChatShowEcFeature(JavaPlugin p) { this.plugin = p; }
    public static void init(JavaPlugin p) {
        ChatShowEcFeature f = new ChatShowEcFeature(p);
        Bukkit.getPluginManager().registerEvents(f, p);
        if (p.getCommand("ec") != null) p.getCommand("ec").setExecutor(f);
        if (p.getCommand("chillview") != null) p.getCommand("chillview").setExecutor(f);
    }
    private String pretty(Material m) {
        String[] a = m.name().toLowerCase().split("_");
        StringBuilder b = new StringBuilder();
        for (String s : a) if (!s.isEmpty()) b.append(Character.toUpperCase(s.charAt(0))).append(s.substring(1)).append(" ");
        return b.toString().trim();
    }
    private String rarity(ItemStack it) {
        int ec = it.getEnchantments().size();
        boolean cn = it.hasItemMeta() && it.getItemMeta().hasDisplayName();
        String n = it.getType().name();
        boolean hi = n.contains("NETHERITE") || n.contains("DIAMOND") || n.contains("TOTEM") || n.contains("BEACON") || n.contains("ELYTRA");
        if (n.contains("NETHERITE") || (hi && ec >= 3) || (cn && ec >= 2)) return "§6§l";
        if (hi || ec >= 2 || cn) return "§d";
        if (ec == 1 || n.contains("IRON") || n.contains("GOLDEN")) return "§b";
        if (it.getMaxStackSize() == 1 || n.contains("STONE") || n.contains("BOW")) return "§a";
        return "§f";
    }
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player)) return true;
        Player p = (Player) s;
        if (c.getName().equalsIgnoreCase("ec")) {
            if (p.getWorld().getName().equals("world_lobby")) { p.sendMessage("§c§l[Chill MC] §fVui lòng vào Sinh Tồn trước khi mở Rương Ender!"); return true; }
            p.openInventory(p.getEnderChest()); return true;
        }
        if (c.getName().equalsIgnoreCase("chillview") && a.length >= 2) {
            String id = a[1], ow = owners.getOrDefault(id, "Người chơi");
            if (a[0].equalsIgnoreCase("item") && items.containsKey(id)) {
                Inventory g = Bukkit.createInventory(null, 27, "§8Xem đồ của §e" + ow);
                ItemStack gl = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
                ItemMeta gm = gl.getItemMeta(); gm.setDisplayName("§7Chỉ xem — Không thể lấy"); gl.setItemMeta(gm);
                for (int i = 0; i < 27; i++) g.setItem(i, gl);
                g.setItem(13, items.get(id).clone()); p.openInventory(g);
            } else if (a[0].equalsIgnoreCase("ec") && ecs.containsKey(id)) {
                Inventory g = Bukkit.createInventory(null, 27, "§5EnderChest của §d" + ow);
                ItemStack[] cs = ecs.get(id);
                for (int i = 0; i < Math.min(27, cs.length); i++) if (cs[i] != null && cs[i].getType() != Material.AIR) g.setItem(i, cs[i].clone());
                p.openInventory(g);
            } else p.sendMessage("§c§l[Chill MC] §fVật phẩm hoặc Rương Ender này đã hết hạn xem!");
        }
        return true;
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        String m = e.getMessage();
        boolean hi = m.contains("[i]"), he = m.toLowerCase().contains("[enderchest]") || m.toLowerCase().contains("[ec]");
        if (!hi && !he) return;
        Player p = e.getPlayer(); e.setCancelled(true);
        String id = UUID.randomUUID().toString().substring(0, 8);
        owners.put(id, p.getName());
        if (hi) {
            ItemStack h = p.getInventory().getItemInMainHand();
            if (h == null || h.getType() == Material.AIR) { p.sendMessage("§c§l[Chill MC] §fPhải cầm vật phẩm trên tay mới dùng được §e[i]§f!"); return; }
            ItemStack cl = h.clone(); items.put(id, cl);
            String rn = (cl.hasItemMeta() && cl.getItemMeta().hasDisplayName()) ? cl.getItemMeta().getDisplayName() : pretty(cl.getType());
            String col = rarity(cl), amt = cl.getAmount() > 1 ? " §7x" + cl.getAmount() : "";
            String[] sp = m.split("\\[i\\]", 2);
            TextComponent full = new TextComponent("§7" + p.getName() + "§f: " + sp[0]);
            TextComponent ic = new TextComponent(col + "[" + p.getName() + "'" + ChatColor.stripColor(rn) + amt + col + "]");
            ic.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/chillview item " + id));
            ic.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§eBấm để soi chi tiết vật phẩm của §a" + p.getName()).create()));
            full.addExtra(ic); if (sp.length > 1) full.addExtra(new TextComponent("§f" + sp[1]));
            Bukkit.getScheduler().runTask(plugin, () -> { for (Player o : Bukkit.getOnlinePlayers()) o.spigot().sendMessage(full); });
        } else {
            ItemStack[] raw = p.getEnderChest().getContents(), snap = new ItemStack[raw.length];
            for (int i = 0; i < raw.length; i++) snap[i] = raw[i] != null ? raw[i].clone() : null;
            ecs.put(id, snap);
            String[] sp = m.split("(?i)\\[(enderchest|ec)\\]", 2);
            TextComponent full = new TextComponent("§7" + p.getName() + "§f: " + sp[0]);
            TextComponent ec = new TextComponent("§d§l[" + p.getName() + "'enderchest]");
            ec.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/chillview ec " + id));
            ec.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§dBấm để xem toàn bộ Rương Ender của §e" + p.getName()).create()));
            full.addExtra(ec); if (sp.length > 1) full.addExtra(new TextComponent("§f" + sp[1]));
            Bukkit.getScheduler().runTask(plugin, () -> { for (Player o : Bukkit.getOnlinePlayers()) o.spigot().sendMessage(full); });
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) { String t = e.getView().getTitle(); if (t.startsWith("§8Xem đồ của ") || t.startsWith("§5EnderChest của ")) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent e) { String t = e.getView().getTitle(); if (t.startsWith("§8Xem đồ của ") || t.startsWith("§5EnderChest của ")) e.setCancelled(true); }
}
