package chill.spawn;
import java.io.File;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class UltimateChillSystems implements Listener, CommandExecutor, Runnable {
    private final JavaPlugin plugin;
    private final Random rng = new Random();
    private final Map<UUID, UUID> duelOpponent = new ConcurrentHashMap<>();
    private final Map<UUID, Location> duelCenter = new ConcurrentHashMap<>();
    private final Map<UUID, Double> duelRadius = new ConcurrentHashMap<>();
    private final Map<String, Integer> questProg = new ConcurrentHashMap<>();
    private final File questFile;

    public UltimateChillSystems(JavaPlugin p) {
        this.plugin = p;
        this.questFile = new File(p.getDataFolder(), "quests.yml");
    }
    public static void init(JavaPlugin p) {
        UltimateChillSystems u = new UltimateChillSystems(p);
        Bukkit.getPluginManager().registerEvents(u, p);
        for (String c : Arrays.asList("ruttiennhankey", "getkey", "moneykey", "crate", "quest", "nhiemvu", "diemdanh", "daily", "xaydautruong", "setkeytien", "pvp"))
            if (p.getCommand(c) != null) p.getCommand(c).setExecutor(u);
        Bukkit.getScheduler().runTaskTimer(p, u, 5L, 5L);
    }
    private boolean isEn(Player p) {
        if (p == null) return false;
        try {
            File f = new File(plugin.getDataFolder(), "player_lang.yml");
            if (!f.exists()) return false;
            return "en".equals(YamlConfiguration.loadConfiguration(f).getString(p.getUniqueId().toString(), "vi"));
        } catch (Exception e) { return false; }
    }
    private void msg(Player p, String vi, String en) {
        p.sendMessage(isEn(p) ? en : vi);
    }
    public static String getOverheadSuffix(Player target, boolean viewerEn) {
        int done = 0, days = 1;
        boolean isStaff = false;
        try {
            org.bukkit.plugin.Plugin pl = org.bukkit.Bukkit.getPluginManager().getPlugin("ChillCore");
            File qf = new File(pl.getDataFolder(), "quests.yml");
            if (qf.exists()) {
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(qf);
                for (int i = 1; i <= 12; i++) if (cfg.getBoolean("qdone." + target.getUniqueId() + "." + i, false)) done++;
                days = cfg.getInt("play_days." + target.getUniqueId(), 1);
                isStaff = cfg.getBoolean("staff." + target.getUniqueId(), false);
            }
        } catch (Exception ignored) {}
        if (target.isOp()) return " §8[§c§k|§r §c§lADMIN §c§k|§8]";
        if (isStaff) return viewerEn ? " §8[§b§lSTAFF§8]" : " §8[§b§lCẬN SỰ§8]";
        if (days >= 15) return viewerEn ? " §8[§6§lVETERAN§8]" : " §8[§6§lLÃO LÀNG§8]";
        if (done >= 12) return viewerEn ? " §8[§aWARRIOR§8]" : " §8[§aCHIẾN BINH§8]";
        return viewerEn ? " §8[§7Newbie§8]" : " §8[§7Người Mới§8]";
    }

    private void syncOverheadTags() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            org.bukkit.scoreboard.Scoreboard sb = viewer.getScoreboard();
            if (sb == null) continue;
            boolean vEn = isEn(viewer);
            for (Player target : Bukkit.getOnlinePlayers()) {
                String tName = "rk_" + target.getUniqueId().toString().substring(0, 12);
                org.bukkit.scoreboard.Team team = sb.getTeam(tName);
                if (team == null) team = sb.registerNewTeam(tName);
                String suf = getOverheadSuffix(target, vEn);
                if (!suf.equals(team.getSuffix())) team.setSuffix(suf);
                if (!team.hasEntry(target.getName())) team.addEntry(target.getName());
            }
            viewer.setPlayerListName(viewer.getName() + getOverheadSuffix(viewer, vEn)); viewer.setDisplayName(viewer.getName() + getOverheadSuffix(viewer, vEn));
        }
    }

    private final Map<UUID, Long> welcomeBannerUntil = new ConcurrentHashMap<>();

    @EventHandler
    public void onEnterSurvivalShowHelp20s(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        if (!p.getWorld().getName().equals("world_lobby")) {
            welcomeBannerUntil.put(p.getUniqueId(), System.currentTimeMillis() + 20000L);
        }
    }

    @EventHandler
    public void onJoinDirectSurvivalHelp20s(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline() && !p.getWorld().getName().equals("world_lobby")) {
                welcomeBannerUntil.put(p.getUniqueId(), System.currentTimeMillis() + 20000L);
            }
        }, 30L);
    }

    public static String getRankLine(Player p) {
        boolean en = false;
        int done = 0, days = 1;
        boolean isStaff = false;
        try {
            org.bukkit.plugin.Plugin pl = org.bukkit.Bukkit.getPluginManager().getPlugin("ChillCore");
            File lf = new File(pl.getDataFolder(), "player_lang.yml");
            if (lf.exists()) en = "en".equals(YamlConfiguration.loadConfiguration(lf).getString(p.getUniqueId().toString(), "vi"));
            File qf = new File(pl.getDataFolder(), "quests.yml");
            if (qf.exists()) {
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(qf);
                for (int i = 1; i <= 12; i++) if (cfg.getBoolean("qdone." + p.getUniqueId() + "." + i, false)) done++;
                days = cfg.getInt("play_days." + p.getUniqueId(), 1);
                isStaff = cfg.getBoolean("staff." + p.getUniqueId(), false);
            }
        } catch (Exception ignored) {}
        String rank;
        if (p.isOp()) rank = "§c§k|§r §c§lADMIN §c§k|";
        else if (isStaff) rank = en ? "§b§lStaff" : "§b§lCận Sự";
        else if (days >= 15) rank = en ? "§6§lVeteran" : "§6§lLão Làng";
        else if (done >= 12) rank = en ? "§a§lWarrior" : "§a§lChiến Binh";
        else rank = en ? "§7Newbie" : "§7Người Mới";
        return (en ? "§fRank : " : "§fDanh hiệu : ") + rank;
    }

    private long getBal(Player p) {
        try { java.lang.reflect.Method m = Main.class.getDeclaredMethod("getMoney", Player.class); m.setAccessible(true); return ((Number) m.invoke(plugin, p)).longValue(); } catch (Exception e) { return 0L; }
    }
    private void changeBal(Player p, long delta) {
        if (delta > 0) Main.addParkourMoney(p, delta);
        else {
            try {
                long cur = getBal(p);
                for (java.lang.reflect.Method m : Main.class.getDeclaredMethods()) {
                    if (m.getName().equals("setMoney") && m.getParameterCount() == 2) {
                        m.setAccessible(true); m.invoke(plugin, p, Math.max(0L, cur + delta)); break;
                    }
                }
            } catch (Exception ignored) {}
        }
    }
    private ItemStack createKey(long val) {
        ItemStack it = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta m = it.getItemMeta();
        boolean vip = val >= 5000L;
        m.setDisplayName((vip ? "§d§l✦ Chìa Khóa VIP ✦ " : "§e§lChìa Khóa May Mắn ") + "§a(" + val + "$)");
        m.setLore(Arrays.asList("§7Giá trị cược: §e" + val + "$", vip ? "§dChế độ VIP (>=5000$): Tỉ lệ cao trúng Sách Mending & Tăng Tiền x3!" : "§7Chế độ Thường: Quay nhận tiền x1.5 - x3 & vật phẩm", "§a▶ Bấm chuột phải để quay ngay!"));
        it.setItemMeta(m); return it;
    }
    private long parseKeyVal(ItemStack it) {
        if (it == null || it.getType() != Material.TRIPWIRE_HOOK || !it.hasItemMeta() || !it.getItemMeta().hasDisplayName()) return 0L;
        String n = ChatColor.stripColor(it.getItemMeta().getDisplayName());
        if (!n.contains("Chìa Khóa") || !n.contains("(") || !n.contains("$)")) return 0L;
        try { return Long.parseLong(n.substring(n.indexOf("(") + 1, n.indexOf("$)"))); } catch (Exception e) { return 0L; }
    }
    private void spinCrate(Player p, long val) {
        boolean vip = val >= 5000L;
        int roll = rng.nextInt(100);
        ItemStack mending = new ItemStack(Material.ENCHANTED_BOOK);
        org.bukkit.inventory.meta.EnchantmentStorageMeta em = (org.bukkit.inventory.meta.EnchantmentStorageMeta) mending.getItemMeta();
        em.addStoredEnchant(Enchantment.MENDING, 1, true);
        em.setDisplayName("§d§l✦ Sách Mending (Sửa Chữa) ✦");
        mending.setItemMeta(em);

        if (vip) {
            if (roll < 15) {
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
                p.getInventory().addItem(mending);
                Bukkit.broadcastMessage("§d§l[Vòng Quay VIP] §e" + p.getName() + " §fđã nổ hũ trúng §d§lSách Mending §ftừ Key §a" + val + "$§f!");
            } else if (roll < 35) {
                long win = val * 3L; changeBal(p, win);
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.3f);
                p.sendMessage("§a§l[Vòng Quay VIP] §fHên quá! Tiền tăng gấp 3: §e+" + win + "$§f!");
            } else if (roll < 65) {
                long win = (long)(val * 1.5); changeBal(p, win);
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                p.sendMessage("§a§l[Vòng Quay VIP] §fTrúng thưởng tăng tiền x1.5: §e+" + win + "$§f!");
            } else {
                long back = Math.max(1L, val / 5L); changeBal(p, back);
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.9f);
                p.sendMessage("§c§l[Vòng Quay VIP] §fXui rồi! Bạn bị hụt giải, chỉ vớt lại được §e" + back + "$ §7(mất " + (val - back) + "$)!");
            }
        } else {
            if (roll < 10) {
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
                p.getInventory().addItem(mending);
                Bukkit.broadcastMessage("§e§l[Vòng Quay] §b" + p.getName() + " §fquá đỏ! Quay Key thường mà trúng §d§lSách Mending§f!");
            } else if (roll < 55) {
                long win = (long)(val * 1.5); changeBal(p, win);
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                p.sendMessage("§a§l[Vòng Quay] §fMay mắn tăng tiền x1.5: §e+" + win + "$§f!");
            } else {
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.9f);
                p.getInventory().addItem(new ItemStack(Material.BAKED_POTATO, 1));
                p.sendMessage("§c§l[Vòng Quay] §fXui xẻo mất sạch §e" + val + "$ §frồi! Tặng bạn củ khoai nướng ăn đỡ buồn nhé!");
            }
        }
    }
    private void buildArena(Player p) {
        World w = Bukkit.getWorlds().get(0);
        Location c = new Location(w, -159, 63, 34);
        int r = 8;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = 0; y <= 4; y++) w.getBlockAt(c.getBlockX() + x, c.getBlockY() + y, c.getBlockZ() + z).setType(Material.AIR);
                boolean border = Math.abs(x) == r || Math.abs(z) == r;
                boolean gate = Math.abs(z) == r && Math.abs(x) <= 1;
                w.getBlockAt(c.getBlockX() + x, c.getBlockY() - 1, c.getBlockZ() + z).setType(gate ? Material.GOLD_BLOCK : (border ? Material.OBSIDIAN : ((x + z) % 2 == 0 ? Material.SMOOTH_STONE : Material.QUARTZ_BLOCK)));
                if (border && !gate) {
                    w.getBlockAt(c.getBlockX() + x, c.getBlockY(), c.getBlockZ() + z).setType(Material.STONE_BRICK_WALL);
                    w.getBlockAt(c.getBlockX() + x, c.getBlockY() + 1, c.getBlockZ() + z).setType(Material.GLASS);
                }
            }
        }
        w.getBlockAt(c.getBlockX() - 6, c.getBlockY() - 1, c.getBlockZ()).setType(Material.RED_CONCRETE);
        w.getBlockAt(c.getBlockX() - 6, c.getBlockY(), c.getBlockZ()).setType(Material.STONE_PRESSURE_PLATE);
        w.getBlockAt(c.getBlockX() + 6, c.getBlockY() - 1, c.getBlockZ()).setType(Material.BLUE_CONCRETE);
        w.getBlockAt(c.getBlockX() + 6, c.getBlockY(), c.getBlockZ()).setType(Material.STONE_PRESSURE_PLATE);
        p.teleport(c.clone().add(0.5, 1.0, 0.5));
        p.sendMessage("§a§l[Chill MC] §fĐã xây xong §eĐấu Trường PvP 1v1 (17x17) §fvới 2 tấm áp lực Đỏ & Xanh!");
    }
    private final Map<UUID, Integer> tpTask = new ConcurrentHashMap<>();
    private final Set<UUID> tpBypass = ConcurrentHashMap.newKeySet();

    private void startCountdownTp(Player p, Location target, String rawCmd) {
        if (p.getWorld().getName().equals("world_lobby")) return;
        if (isInActiveDuel(p.getUniqueId())) {
            msg(p, "§cKhông thể dịch chuyển khi đang trong trận PvP 1v1!", "§cCannot teleport during a 1v1 PvP duel!");
            return;
        }
        Integer old = tpTask.remove(p.getUniqueId());
        if (old != null) Bukkit.getScheduler().cancelTask(old);
        Location startLoc = p.getLocation().clone();
        final int[] sec = {0};
        int id = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!p.isOnline() || isInActiveDuel(p.getUniqueId()) || !p.getWorld().equals(startLoc.getWorld()) || p.getLocation().distanceSquared(startLoc) > 1.5) {
                Integer tid = tpTask.remove(p.getUniqueId());
                if (tid != null) Bukkit.getScheduler().cancelTask(tid);
                if (p.isOnline()) msg(p, "§c§l[Dịch Chuyển] §fĐã hủy vì bạn vừa di chuyển!", "§c§l[Teleport] §fCancelled because you moved!");
                return;
            }
            sec[0]++;
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            p.sendTitle("", isEn(p) ? "§e⏳ Teleporting: §a" + sec[0] + "/10s §7(Don't move!)" : "§e⏳ Đang dịch chuyển: §a" + sec[0] + "/10s §7(Đứng yên!)", 0, 22, 0);
            if (sec[0] >= 10) {
                Integer tid = tpTask.remove(p.getUniqueId());
                if (tid != null) Bukkit.getScheduler().cancelTask(tid);
                if (target != null) {
                    p.teleport(target);
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
                    msg(p, "§c§l[Đấu Trường PvP] §fĐã tới trước cổng §eĐấu Trường 1v1§f!", "§c§l[PvP Arena] §fArrived at the §e1v1 Arena§f!");
                } else if (rawCmd != null) {
                    tpBypass.add(p.getUniqueId());
                    try { p.performCommand(rawCmd); } finally { tpBypass.remove(p.getUniqueId()); }
                }
            }
        }, 0L, 20L);
        tpTask.put(p.getUniqueId(), id);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTpCommandCountdown(PlayerCommandPreprocessEvent e) {
        String raw = e.getMessage().substring(1).trim();
        if (raw.isEmpty()) return;
        String base = raw.split("\\s+")[0].toLowerCase();
        Player p = e.getPlayer();
        if (base.equals("clan")) {
            String[] parts = raw.split("\\s+");
            if (parts.length >= 3) {
                String sub = java.util.Arrays.asList(parts).get(1).toLowerCase();
                if (sub.equals("create") || sub.equals("tao") || sub.equals("rename") || sub.equals("doiten")) {
                    String cName = java.util.Arrays.asList(parts).get(2);
                    if (parts.length > 3 || cName.length() > 12 || !cName.matches("^[a-zA-Z0-9]+$")) {
                        e.setCancelled(true);
                        msg(p, "§c§l[Clan] §fTên Clan chỉ được tối đa §e12 ký tự §fvà §ckhông dùng ký tự đặc biệt / dấu cách §7(chỉ dùng A-Z, 0-9)!",
                               "§c§l[Clan] §fClan name must be max §e12 characters §fwith §cno special characters or spaces §7(A-Z, 0-9 only)!");
                        return;
                    }
                }
            }
        }
        if (base.equals("viewclan") && p.isOp()) {
            e.setCancelled(true);
            String[] parts = raw.split("\\s+");
            String query = parts.length > 1 ? java.util.Arrays.asList(parts).get(1).trim().toLowerCase() : "";
            File[] ymls = plugin.getDataFolder().listFiles((d, name) -> name.toLowerCase().contains("clan") && name.endsWith(".yml"));
            if (ymls == null || ymls.length == 0) ymls = plugin.getDataFolder().listFiles((d, name) -> name.endsWith(".yml"));
            java.util.Set<String> foundClans = new java.util.LinkedHashSet<>();
            boolean matched = false;
            if (ymls != null) {
                for (File f : ymls) {
                    String fn = f.getName().toLowerCase();
                    if (fn.contains("lang") || fn.contains("quest") || fn.contains("data") || fn.contains("money")) continue;
                    YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                    org.bukkit.configuration.ConfigurationSection sec = cfg.isConfigurationSection("clans") ? cfg.getConfigurationSection("clans") : (cfg.isConfigurationSection("clan") ? cfg.getConfigurationSection("clan") : cfg);
                    for (String k : sec.getKeys(false)) {
                        String kl = k.toLowerCase();
                        if (kl.equals("player") || kl.equals("invite") || kl.equals("claim_time") || kl.equals("clan") || k.length() >= 32 || !sec.isConfigurationSection(k)) continue;
                        String cName = sec.getString(k + ".name", k);
                        String cTag = sec.getString(k + ".tag", "");
                        String leader = sec.getString(k + ".leader", sec.getString(k + ".owner", "Admin"));
                        java.util.List<String> mems = sec.getStringList(k + ".members");
                        foundClans.add(cName + (cTag.isEmpty() ? "" : " [" + cTag + "]"));
                        if (!query.isEmpty() && (kl.contains(query) || cName.toLowerCase().contains(query) || cTag.toLowerCase().contains(query) || leader.toLowerCase().contains(query) || mems.toString().toLowerCase().contains(query))) {
                            matched = true;
                            if (isEn(p)) {
                                p.sendMessage("§6§l=== CLAN INFO: §e" + cName + " §6§l===");
                                p.sendMessage("§7• Tag: §b" + cTag + " §7| Leader: §a" + leader);
                                p.sendMessage("§7• Members (" + mems.size() + "): §f" + String.join(", ", mems));
                            } else {
                                p.sendMessage("§6§l=== THÔNG TIN BANG HỘI: §e" + cName + " §6§l===");
                                p.sendMessage("§7• Tag: §b" + cTag + " §7| Chủ bang: §a" + leader);
                                p.sendMessage("§7• Thành viên (" + mems.size() + "): §f" + String.join(", ", mems));
                            }
                        }
                    }
                }
            }
            if (query.isEmpty() || !matched) {
                if (foundClans.isEmpty()) msg(p, "§eHiện tại chưa có bang hội nào!", "§eNo clans found!");
                else msg(p, "§aDanh sách Clan hiện có: §e" + String.join("§7, §e", foundClans), "§aExisting Clans: §e" + String.join("§7, §e", foundClans));
            }
            return;
        }
        if (base.equals("pvp")) {
            e.setCancelled(true);
            startCountdownTp(p, new Location(Bukkit.getWorlds().get(0), -158.5, 64.0, 25.0, 0.0f, 0.0f), null);
        } else if ((base.equals("spawn") || base.equals("home") || base.equals("rtp")) && !tpBypass.contains(p.getUniqueId())) {
            e.setCancelled(true);
            startCountdownTp(p, null, raw);
        }
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String lbl, String[] args) {
        if (!(s instanceof Player)) return true;
        Player p = (Player) s;
        String c = cmd.getName().toLowerCase();
        if (c.equals("setkeytien")) {
            if (!p.isOp()) { p.sendMessage("§cBạn không có quyền OP!"); return true; }
            Block b = p.getTargetBlockExact(5);
            if (b == null || (b.getType() != Material.CHEST && b.getType() != Material.ENDER_CHEST && b.getType() != Material.TRAPPED_CHEST)) { p.sendMessage("§cHãy nhìn thẳng vào chiếc Rương (cách tối đa 5 ô) rồi gõ §e/setkeytien§c!"); return true; }
            YamlConfiguration cfg = questFile.exists() ? YamlConfiguration.loadConfiguration(questFile) : new YamlConfiguration();
            java.util.List<String> list = cfg.getStringList("crate_chests");
            String locKey = b.getWorld().getName() + ":" + b.getX() + ":" + b.getY() + ":" + b.getZ();
            if (!list.contains(locKey)) { list.add(locKey); cfg.set("crate_chests", list); try { cfg.save(questFile); } catch (Exception ignored) {} }
            p.playSound(b.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.4f);
            p.sendMessage("§a§l[Chill MC] §fĐã đặt chiếc rương tại §e" + b.getX() + ", " + b.getY() + ", " + b.getZ() + " §flàm §6Rương Quay Thưởng Key Tiền§f!");
            return true;
        }
        if (c.equals("xaydautruong")) {
            if (!p.isOp()) { p.sendMessage("§cBạn không có quyền OP!"); return true; }
            buildArena(p); return true;
        }
        if (c.equals("ruttiennhankey") || c.equals("moneykey") || c.equals("getkey")) {
            if (args.length != 1) { msg(p, "§cCú pháp: §e/ruttiennhankey <số_tiền> §7(VD: /ruttiennhankey 5000)", "§cUsage: §e/getkey <amount> §7(Ex: /getkey 5000)"); return true; }
            long amt;
            try { amt = Long.parseLong(java.util.Arrays.asList(args).get(0).replace("$", "").trim()); } catch (Exception e) { msg(p, "§cSố tiền không hợp lệ!", "§cInvalid amount!"); return true; }
            if (amt < 1L) { msg(p, "§cSố tiền tối thiểu là 1$!", "§cMinimum amount is 1$!"); return true; }
            if (getBal(p) < amt) { msg(p, "§cBạn không đủ §e" + amt + "$ §cđể rút!", "§cYou do not have enough §e" + amt + "$§c!"); return true; }
            changeBal(p, -amt);
            p.getInventory().addItem(createKey(amt));
            msg(p, "§a§l[Chill MC] §fĐã rút §e" + amt + "$ §fthành §6Chìa Khóa Quay Thưởng§f! Cầm chuột phải vào rương để quay.", "§a§l[Chill MC] §fWithdrew §e" + amt + "$ §finto a §6Crate Key§f! Right-click the crate chest to spin.");
            return true;
        }
        if (c.equals("crate")) {
            ItemStack hand = p.getInventory().getItemInMainHand();
            long val = parseKeyVal(hand);
            if (val > 0L) { hand.setAmount(hand.getAmount() - 1); spinCrate(p, val); }
            else p.sendMessage("§e§l[Vòng Quay IQ] §fGõ §a/ruttiennhankey <tiền> §fđể tạo Key (từ §d5000$ §ftrở lên quay VIP)!");
            return true;
        }
        if (c.equals("diemdanh") || c.equals("daily")) {
            YamlConfiguration cfg = questFile.exists() ? YamlConfiguration.loadConfiguration(questFile) : new YamlConfiguration();
            String today = LocalDate.now().toString(), k = "daily." + p.getUniqueId();
            if (today.equals(cfg.getString(k, ""))) { p.sendMessage("§cHôm nay bạn đã điểm danh rồi!"); return true; }
            cfg.set(k, today); try { cfg.save(questFile); } catch (Exception ignored) {}
            changeBal(p, 500L); p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            p.sendMessage("§a§l[Điểm Danh] §fĐiểm danh thành công! Nhận §e+500$§f!");
            return true;
        }
        if (c.equals("quest") || c.equals("nhiemvu")) {
            int wood = questProg.getOrDefault(p.getUniqueId() + ":wood", 0), stone = questProg.getOrDefault(p.getUniqueId() + ":stone", 0);
            if (isEn(p)) {
                p.sendMessage("§6§l=== DAILY QUESTS ===\n§e1. Chop 32 Logs: §a" + wood + "/32 §7(+300$)\n§e2. Mine 64 Stone: §a" + stone + "/64 §7(+300$)\n§e3. Daily Reward: §b/daily §7(+500$)");
            } else {
                p.sendMessage("§6§l=== NHIỆM VỤ HÔM NAY ===\n§e1. Chặt 32 gỗ: §a" + wood + "/32 §7(+300$)\n§e2. Đào 64 đá: §a" + stone + "/64 §7(+300$)\n§e3. Điểm danh: §b/diemdanh §7(+500$)");
            }
            return true;
        }
        return true;
    }
    private void openCratePreviewGui(Player p) {
        boolean en = isEn(p);
        Inventory gui = Bukkit.createInventory(null, 27, en ? "§6§lCrate Rewards & Guide" : "§6§lHướng Dẫn & Tỉ Lệ Quay");
        ItemStack gl = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gm = gl.getItemMeta(); gm.setDisplayName(" "); gl.setItemMeta(gm);
        for (int i = 0; i < 27; i++) gui.setItem(i, gl);

        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta pm = paper.getItemMeta();
        if (en) {
            pm.setDisplayName("§e§l✦ HOW TO SPIN & DROP RATES ✦");
            pm.setLore(Arrays.asList(
                "§71. Type §a/getkey <amount> §7to create a Key",
                "§72. Hold the Key & §eRight-Click §7this chest to spin",
                "§8--------------------------------",
                "§bNormal Key (< 5,000$):",
                "§f • §d10% §7Mending Book",
                "§f • §a45% §7Win x1.5 Money",
                "§f • §c45% §7Lose Key (Baked Potato)",
                "§8--------------------------------",
                "§dVIP Key (>= 5,000$):",
                "§f • §d15% §7Mending Book (Jackpot)",
                "§f • §a20% §7Win x3 Money | §a30% §7Win x1.5",
                "§f • §c35% §7Unlucky (Keep only 20%)"
            ));
        } else {
            pm.setDisplayName("§e§l✦ HƯỚNG DẪN & TỈ LỆ QUAY THƯỞNG ✦");
            pm.setLore(Arrays.asList(
                "§71. Gõ §a/ruttiennhankey <số_tiền> §7để tạo Key",
                "§72. Cầm Key & §eChuột Phải §7vào rương này để quay",
                "§8--------------------------------",
                "§bKey Thường (< 5.000$):",
                "§f • §d10% §7Trúng Sách Mending (Sửa Chữa)",
                "§f • §a45% §7Tăng tiền x1.5",
                "§f • §c45% §7Xui xẻo mất sạch tiền Key",
                "§8--------------------------------",
                "§dKey VIP (>= 5.000$):",
                "§f • §d15% §7Nổ hũ Sách Mending",
                "§f • §a20% §7Tăng tiền x3 | §a30% §7Tăng tiền x1.5",
                "§f • §c35% §7Xui xẻo (Chỉ vớt lại 20% tiền)"
            ));
        }
        paper.setItemMeta(pm);
        gui.setItem(13, paper);
        p.openInventory(gui);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onUseKey(PlayerInteractEvent e) {
        String act = e.getAction().name();
        if (!act.contains("CLICK_BLOCK") && !act.contains("RIGHT_CLICK")) return;
        if (e.getClickedBlock() != null) {
            Block b = e.getClickedBlock();
            String locKey = b.getWorld().getName() + ":" + b.getX() + ":" + b.getY() + ":" + b.getZ();
            YamlConfiguration cfg = questFile.exists() ? YamlConfiguration.loadConfiguration(questFile) : new YamlConfiguration();
            if (cfg.getStringList("crate_chests").contains(locKey)) {
                e.setCancelled(true);
                Player p = e.getPlayer();
                if (act.startsWith("LEFT_CLICK")) {
                    openCratePreviewGui(p);
                    return;
                }
                ItemStack it = e.getItem();
                long val = parseKeyVal(it);
                if (val > 0L) {
                    it.setAmount(it.getAmount() - 1);
                    spinCrate(p, val);
                } else {
                    org.bukkit.util.Vector kb = p.getLocation().toVector().subtract(b.getLocation().add(0.5, 0.5, 0.5).toVector()).normalize().multiply(0.75).setY(0.32);
                    p.setVelocity(kb);
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.9f);
                    msg(p, "§c§l[Rương Quay] §fBạn cần có §eKey §fđể mở! Gõ §a/ruttiennhankey <số_tiền> §f(Chuột trái vào rương để xem hướng dẫn).",
                           "§c§l[Crate] §fYou need a §eKey §fto open this! Type §a/getkey <amount> §f(Left-click chest for guide).");
                }
                return;
            }
        }
        ItemStack it = e.getItem(); long val = parseKeyVal(it);
        if (val > 0L) { e.setCancelled(true); it.setAmount(it.getAmount() - 1); spinCrate(e.getPlayer(), val); }
    }
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockCrateChestOpen(org.bukkit.event.inventory.InventoryOpenEvent e) {
        if (e.getInventory().getLocation() != null) {
            Block b = e.getInventory().getLocation().getBlock();
            String locKey = b.getWorld().getName() + ":" + b.getX() + ":" + b.getY() + ":" + b.getZ();
            YamlConfiguration cfg = questFile.exists() ? YamlConfiguration.loadConfiguration(questFile) : new YamlConfiguration();
            if (cfg.getStringList("crate_chests").contains(locKey)) e.setCancelled(true);
        }
    }
    @EventHandler(priority = EventPriority.LOWEST)
    public void onLockPreviewClick(InventoryClickEvent e) {
        String tt = e.getView().getTitle();
        if (tt.contains("Hướng Dẫn & Tỉ Lệ Quay") || tt.contains("Crate Rewards & Guide")) e.setCancelled(true);
        }
    @EventHandler
    public void onQuestMine(BlockBreakEvent e) {
        Player p = e.getPlayer(); if (p.getGameMode() == GameMode.CREATIVE) return;
        String m = e.getBlock().getType().name();
        if (m.contains("LOG") || m.contains("WOOD")) {
            String k = p.getUniqueId() + ":wood"; int v = questProg.getOrDefault(k, 0) + 1;
            if (v >= 32) { questProg.put(k, 0); changeBal(p, 300L); p.sendMessage("§a§l[Nhiệm Vụ] §fXong chặt 32 gỗ! Thưởng §e+300$§f!"); } else questProg.put(k, v);
        } else if (m.equals("STONE") || m.equals("COBBLESTONE") || m.equals("DEEPSLATE")) {
            String k = p.getUniqueId() + ":stone"; int v = questProg.getOrDefault(k, 0) + 1;
            if (v >= 64) { questProg.put(k, 0); changeBal(p, 300L); p.sendMessage("§a§l[Nhiệm Vụ] §fXong đào 64 đá! Thưởng §e+300$§f!"); } else questProg.put(k, v);
        }
    }
    public static boolean isInActiveDuel(UUID id) {
        try {
            for (org.bukkit.plugin.RegisteredListener rl : EntityDamageEvent.getHandlerList().getRegisteredListeners())
                if (rl.getListener() instanceof UltimateChillSystems) return ((UltimateChillSystems) rl.getListener()).duelOpponent.containsKey(id);
        } catch (Exception ignored) {}
        return false;
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDuelDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player v = (Player) e.getEntity(), a = e.getDamager() instanceof Player ? (Player) e.getDamager() : (e.getDamager() instanceof Projectile && ((Projectile) e.getDamager()).getShooter() instanceof Player ? (Player) ((Projectile) e.getDamager()).getShooter() : null);
        if (a == null) return;
        UUID vo = duelOpponent.get(v.getUniqueId()), ao = duelOpponent.get(a.getUniqueId());
        if (vo != null || ao != null) e.setCancelled(!(v.getUniqueId().equals(ao) && a.getUniqueId().equals(vo)));
    }
    @EventHandler
    public void onDuelDeath(PlayerDeathEvent e) {
        Player d = e.getEntity(); UUID o = duelOpponent.remove(d.getUniqueId()); duelCenter.remove(d.getUniqueId()); duelRadius.remove(d.getUniqueId());
        if (o != null) {
            duelOpponent.remove(o); duelCenter.remove(o); duelRadius.remove(o); Player w = Bukkit.getPlayer(o);
            if (w != null && w.isOnline()) { changeBal(w, 200L); Bukkit.broadcastMessage("§c§l[Đấu Trường 1v1] §e" + w.getName() + " §fđã hạ gục §c" + d.getName() + " §f(+200$)!"); }
        }
    }
    @EventHandler
    public void onDuelQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId(), o = duelOpponent.remove(id); duelCenter.remove(id); duelRadius.remove(id);
        if (o != null) { duelOpponent.remove(o); duelCenter.remove(o); duelRadius.remove(o); Player w = Bukkit.getPlayer(o); if (w != null) w.sendMessage("§eĐối thủ đã thoát trận!"); }
    }
    private int overheadTick = 0;
    public void run() {
        if (++overheadTick % 4 == 0) syncOverheadTags();
        long nowMs = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            Long until = welcomeBannerUntil.get(p.getUniqueId());
            if (until != null) {
                if (nowMs <= until && !p.getWorld().getName().equals("world_lobby")) {
                    if (overheadTick % 4 == 0) {
                        int secPassed = Math.min(20, Math.max(1, (int)((20000L - (until - nowMs)) / 1000L) + 1));
                        boolean en = isEn(p);
                        p.sendTitle(en ? "§b§lCHILL MC §7(" + secPassed + "/20s)" : "§b§lCHILL MC §7(" + secPassed + "/20s)",
                                    en ? "§eType §a/helpchillmc §efor assistance" : "§eGõ §a/helpchillmc §eđể được hỗ trợ", 0, 25, 0);
                        if (secPassed == 1) p.sendMessage(en ? "§6§l[CHILL MC] §eType §a/helpchillmc §efor assistance!" : "§6§l[CHILL MC] §eGõ §a/helpchillmc §eđể được hỗ trợ!");
                    }
                } else if (nowMs > until) {
                    welcomeBannerUntil.remove(p.getUniqueId());
                }
            }
        }
        List<Player> pls = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID id = p.getUniqueId();
            if (duelOpponent.containsKey(id)) {
                Player opp = Bukkit.getPlayer(duelOpponent.get(id)); Location cen = duelCenter.get(id); double rad = duelRadius.getOrDefault(id, 8.5);
                if (opp == null || !opp.isOnline() || cen == null || !p.getWorld().equals(cen.getWorld())) { duelOpponent.remove(id); duelCenter.remove(id); duelRadius.remove(id); continue; }
                if (Math.abs(p.getLocation().getX() - cen.getX()) > rad || Math.abs(p.getLocation().getZ() - cen.getZ()) > rad) { p.teleport(cen); p.sendMessage("§cKhông thể rời đấu trường khi đang PvP 1v1!"); }
                p.sendTitle("", "§c§l⚔ PvP 1v1: §e§l" + opp.getName() + " §c§l⚔", 0, 12, 0);
            } else if (!p.getWorld().getName().equals("world_lobby") && p.getLocation().getBlock().getType() == Material.STONE_PRESSURE_PLATE) pls.add(p);
        }
        for (int i = 0; i < pls.size(); i++) for (int j = i + 1; j < pls.size(); j++) {
            Player p1 = pls.get(i), p2 = pls.get(j); if (!p1.getWorld().equals(p2.getWorld())) continue;
            double d = p1.getLocation().distance(p2.getLocation());
            if (d >= 5.0 && d <= 25.0 && !duelOpponent.containsKey(p1.getUniqueId()) && !duelOpponent.containsKey(p2.getUniqueId())) {
                Location mid = p1.getLocation().clone().add(p2.getLocation()).multiply(0.5); double rad = Math.max(7.5, d / 2.0 + 2.0);
                duelOpponent.put(p1.getUniqueId(), p2.getUniqueId()); duelCenter.put(p1.getUniqueId(), mid); duelRadius.put(p1.getUniqueId(), rad);
                duelOpponent.put(p2.getUniqueId(), p1.getUniqueId()); duelCenter.put(p2.getUniqueId(), mid); duelRadius.put(p2.getUniqueId(), rad);
                p1.sendMessage("§c§l[Đấu Trường 1v1] §fBắt đầu PvP với §e" + p2.getName() + "§f!"); p2.sendMessage("§c§l[Đấu Trường 1v1] §fBắt đầu PvP với §e" + p1.getName() + "§f!");
            }
        }
    }
}
