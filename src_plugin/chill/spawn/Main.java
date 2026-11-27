package chill.spawn;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;
import java.io.File;
import java.util.*;

public class Main extends JavaPlugin implements Listener {
    private final Map<UUID, Material> buyMat = new HashMap<>();
    private final Map<UUID, Integer> buyAmt = new HashMap<>();
    private final Map<UUID, String> lastPage = new HashMap<>();
    private final List<String> ahSellers = new ArrayList<>();
    private final List<Integer> ahPrices = new ArrayList<>();
    private final List<ItemStack> ahItems = new ArrayList<>();
    private File ahFile;
    private final Set<UUID> unlogged = new HashSet<>();
    private final Map<UUID, UUID> lastMsg = new HashMap<>();

    private final Map<UUID, UUID> tpaReq = new HashMap<>();
    private final Map<UUID, Long> tpaTime = new HashMap<>();
    private final Map<UUID, Long> rtpCd = new HashMap<>();


    @Override
    public void onEnable() {
        getOrCreateLobbyWorld();
        startClearLag();
        LanguageTranslateFeature.init(this);
        UltimateChillSystems.init(this);
        MotdAnimator.start(this);
        ChatShowEcFeature.init(this);
        SpawnZoneTask.start(this, step3Locked, unlogged);
        
        
        Bukkit.getPluginManager().registerEvents(this, this);
        loadAH(); File sf = new File(getDataFolder(), "spawn.yml"); org.bukkit.Location sl = sf.exists() ? YamlConfiguration.loadConfiguration(sf).getLocation("spawn") : null; for (Player op : Bukkit.getOnlinePlayers()) { unlogged.add(op.getUniqueId()); if (sl != null && sl.getWorld() != null) op.teleport(sl); sendCompactMsg(op, "§e§l[Bảo Mật] §fSever vừa tải lại! Gõ §a/login <mật_khẩu> §fđể mở khóa di chuyển."); }
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) updateBoard(p);
        }, 20L, 20L);
    }

    private void loadAH() {
        ahFile = new File(getDataFolder(), "ah_data.yml");
        if (!ahFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(ahFile);
        int count = cfg.getInt("count", 0);
        for (int i = 0; i < count; i++) {
            ItemStack it = cfg.getItemStack("item." + i);
            if (it != null) {
                ahSellers.add(cfg.getString("seller." + i, "Unknown"));
                ahPrices.add(cfg.getInt("price." + i, 100));
                ahItems.add(it);
            }
        }
    }

    private void saveAH() {
        try {
            if (!getDataFolder().exists()) getDataFolder().mkdirs();
            YamlConfiguration cfg = new YamlConfiguration();
            cfg.set("count", ahItems.size());
            for (int i = 0; i < ahItems.size(); i++) {
                cfg.set("seller." + i, ahSellers.get(i));
                cfg.set("price." + i, ahPrices.get(i));
                cfg.set("item." + i, ahItems.get(i));
            }
            cfg.save(ahFile);
        } catch (Exception ignored) {}
    }

    private void updateBoard(Player p) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("chill", "dummy", "§6§lCHILL MC");
        obj.getScore(UltimateChillSystems.getRankLine(p)).setScore(99);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.getScore("§7§m--------------------").setScore(6);
        obj.getScore("§fName : §a" + p.getName()).setScore(5);
        String cl = getPlayerClan(p.getName()); obj.getScore("§fClan : " + (cl.isEmpty() ? "§7Chưa có" : "§b[" + cl + "]")).setScore(4);
        obj.getScore("§fTiền : §e" + getMoney(p) + "$").setScore(3);
        obj.getScore("§fReward : " + getRewardTimer(p.getName())).setScore(2);
        obj.getScore("§7§m-------------------- ").setScore(1);
        p.setScoreboard(board);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equalsIgnoreCase("givephieu") && sender.isOp()) {
            if (args.length == 0) { sendCompactMsg(sender, "§cDung: /givephieu [ten] <so_tien> [so_luong]"); return true; }
            Player target = Bukkit.getPlayerExact(java.util.Arrays.asList(args).get(0));
            int idx = (target != null) ? 1 : 0;
            if (target == null) {
                if (!(sender instanceof Player)) { sendCompactMsg(sender, "§cNguoi choi khong online!"); return true; }
                target = (Player) sender;
            }
            if (args.length == idx) { sendCompactMsg(sender, "§cVui long nhap so tien!"); return true; }
            long val = Long.parseLong(java.util.Arrays.asList(args).get(idx).replaceAll("[^0-9]", ""));
            int amt = (args.length != idx + 1) ? Integer.parseInt(java.util.Arrays.asList(args).get(idx + 1).replaceAll("[^0-9]", "")) : 1;
            org.bukkit.inventory.ItemStack paper = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PAPER, amt);
            org.bukkit.inventory.meta.ItemMeta pm = paper.getItemMeta();
            pm.setDisplayName("§e§lPhiếu Tiền: §a§l" + val + "$");
            pm.setLore(java.util.Arrays.asList("§eBấm Chuột Phải để nhận ngay tiền!"));
            paper.setItemMeta(pm);
            target.getInventory().addItem(paper);
            sendCompactMsg(sender, "§a§l[Chill MC] §fĐã gửi §ex" + amt + " §fPhiếu Tiền §a" + val + "$ §fcho §e" + target.getName() + "§f!");
            return true;
        }
        if (cmd.getName().equalsIgnoreCase("thoatsinhton") && sender instanceof Player) {
            Player tp = (Player) sender;
            org.bukkit.World lw = getOrCreateLobbyWorld();
            if (lw != null) {
                saveSurvivalInvIfNeeded(tp);
                tp.teleport(new org.bukkit.Location(lw, 0.5, 65.0, 0.5));
                tp.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
                giveLobbyCompass(tp);
                openModeSelectorGui(tp);
                sendCompactMsg(tp, "§a§l[Sảnh Chờ] §fBạn đã thoát Sinh Tồn và quay về Sảnh Chờ!");
            }
            return true;
        }


        if (cmd.getName().equalsIgnoreCase("viewclan") && sender instanceof Player && args.length != 0) { openClanHierarchyGui((Player) sender, java.util.Arrays.asList(args).get(0)); return true; }
        if (cmd.getName().equalsIgnoreCase("clan") && sender instanceof Player && args.length != 0) {
            Player cp = (Player) sender; String sub = java.util.Arrays.asList(args).get(0).toLowerCase();
            if (sub.equals("upgrade") || sub.equals("promote") || (sub.equals("invite") && args.length != 1)) {
                File cf = new File(getDataFolder(), "clans.yml"); YamlConfiguration cc = YamlConfiguration.loadConfiguration(cf);
                String pfx = ""; if (pfx.isEmpty() && cc.isConfigurationSection("clans")) pfx = "clans.";
                String myC = null; org.bukkit.configuration.ConfigurationSection sec = pfx.isEmpty() ? cc : cc.getConfigurationSection("clans");
                if (sec != null) for (String k : sec.getKeys(false)) if (cc.getString(pfx+k+".leader", cc.getString(pfx+k+".owner", "")).equalsIgnoreCase(cp.getName()) || cc.getStringList(pfx+k+".members").contains(cp.getName())) { myC = k; break; }
                if (sub.equals("upgrade")) {
                    if (myC == null) { sendCompactMsg(cp, "§cBan chua co Clan!"); return true; }
                    if (!cc.getString(pfx+myC+".leader", cc.getString(pfx+myC+".owner", "")).equalsIgnoreCase(cp.getName())) { sendCompactMsg(cp, "§cChi Chu Clan moi duoc nang cap!"); return true; }
                    int lvl = cc.getInt(pfx+myC+".level", 1); if (lvl == 3) { sendCompactMsg(cp, "§bClan da dat Cap toi da (3/3)!"); return true; }
                    double cost = (lvl == 1) ? 10000.0 : 50000.0;
                    if (!(getMoney(cp) >= cost)) { sendCompactMsg(cp, "§cCan §e" + (int)cost + "$ §cde nang len Cap " + (lvl+1)); return true; }
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eco take " + cp.getName() + " " + (int)cost);
                    cc.set(pfx+myC+".level", lvl+1); try { cc.save(cf); } catch (Exception ex) {}
                    sendCompactMsg(cp, "§6§l[Clan] §aNang cap §e" + myC + " §alen §bCap " + (lvl+1) + "/3!"); return true;
                }
                if (sub.equals("promote") && args.length != 1) {
                    if (myC == null || !cc.getString(pfx+myC+".leader", cc.getString(pfx+myC+".owner", "")).equalsIgnoreCase(cp.getName())) { sendCompactMsg(cp, "§cChi Chu Clan moi dung duoc!"); return true; }
                    String tp = java.util.Arrays.asList(args).get(1); java.util.List off = cc.getStringList(pfx+myC+".officers");
                    if (off.contains(tp)) { off.remove(tp); sendCompactMsg(cp, "§eDa ha chuc §f"+tp); } else { off.add(tp); sendCompactMsg(cp, "§aDa phong §e"+tp+" §alam §6Pho Clan!"); }
                    cc.set(pfx+myC+".officers", off); try { cc.save(cf); } catch (Exception ex) {} return true;
                }
                if (sub.equals("invite") && args.length != 1 && myC != null && cc.getStringList(pfx+myC+".officers").contains(cp.getName())) {
                    Player tp = Bukkit.getPlayerExact(java.util.Arrays.asList(args).get(1));
                    if (tp == null) { sendCompactMsg(cp, "§cNguoi choi khong online!"); return true; }
                    cc.set("invites."+tp.getName().toLowerCase(), myC); try { cc.save(cf); } catch (Exception ex) {}
                    sendCompactMsg(cp, "§a[Pho Clan] Da moi §f"+tp.getName()); sendCompactMsg(tp, "§6§l[Clan] §e"+cp.getName()+" §fmoi ban vao §b"+myC+"§f. Go §a/clan join "+myC); return true;
                }
            }
        }

        
        if (cmd.getName().equalsIgnoreCase("deltien")) {
            if (!sender.isOp()) { sendCompactMsg(sender, "§cBạn không có quyền!"); return true; }
            if (args.length < 2) { sendCompactMsg(sender, "§cCách dùng: /deltien <tên> <số_tiền | all>"); return true; }
            if (args[1].equalsIgnoreCase("all")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "scoreboard players set " + args[0] + " tien 0");
                sendCompactMsg(sender, "§a§l[Chill MC] §fĐã xóa toàn bộ tiền của §a" + args[0] + " §fvề §e0$§f!"); return true;
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "scoreboard players remove " + args[0] + " tien " + args[1]);
            sendCompactMsg(sender, "§a§l[Chill MC] §fĐã trừ §e" + args[1] + "$ §fcủa §a" + args[0] + "§f!"); return true;
        }

        if (cmd.getName().equalsIgnoreCase("addtien")) {
            if (!sender.isOp()) { sendCompactMsg(sender, "§cBạn không có quyền!"); return true; }
            if (args.length < 2) { sendCompactMsg(sender, "§cCách dùng: /addtien <tên> <số_tiền>"); return true; }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "scoreboard players add " + args[0] + " tien " + args[1]);
            sendCompactMsg(sender, "§a§l[Chill MC] §fĐã cộng §e" + args[1] + "$ §fcho §a" + args[0]);
            return true;
        }
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("ah")) {
            if (args.length >= 2 && args[0].equalsIgnoreCase("sell")) {
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand == null || hand.getType() == Material.AIR) { sendCompactMsg(p, "§c§l[Chợ Đen] §fHãy cầm vật phẩm trên tay để treo bán!"); return true; }
                if (ahItems.size() >= 45) { sendCompactMsg(p, "§c§l[Chợ Đen] §fChợ đã đầy (tối đa 45 món)!"); return true; }
                int price;
                try { price = Integer.parseInt(args[1]); } catch (Exception e) { sendCompactMsg(p, "§c§l[Chợ Đen] §fGiá bán phải là số nguyên!"); return true; }
                if (price <= 0) { sendCompactMsg(p, "§c§l[Chợ Đen] §fGiá bán phải lớn hơn 0$!"); return true; }
                ahSellers.add(p.getName());
                ahPrices.add(price);
                ahItems.add(hand.clone());
                p.getInventory().setItemInMainHand(null);
                saveAH();
                sendCompactMsg(p, "§a§l[Chợ Đen] §fĐã treo bán vật phẩm lên §e/ah §fvới giá §e" + price + "$§f!");
                return true;
            }
            openAH(p);
            return true;
        }
        if (cmd.getName().equalsIgnoreCase("sellgui")) {
            p.openInventory(Bukkit.createInventory(null, 27, "§6§lBán Đồ Lấy Tiền"));
            return true;
        }
        if (cmd.getName().equalsIgnoreCase("tien")) {
            sendCompactMsg(p, "§a§l[Chill MC] §fSố tiền hiện tại của bạn là: §e" + getMoney(p) + "$");
            return true;
        }
        if (cmd.getName().equalsIgnoreCase("pay")) {
            if (args.length < 2) { sendCompactMsg(p, "§c§l[Chill MC] §fCách dùng: §e/pay <tên> <tiền>"); return true; }
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null || !target.isOnline() || target.equals(p)) { sendCompactMsg(p, "§c§l[Chill MC] §fNgười chơi không hợp lệ!"); return true; }
            int amount;
            try { amount = Integer.parseInt(args[1]); } catch (Exception e) { sendCompactMsg(p, "§c§l[Chill MC] §fSố tiền sai!"); return true; }
            if (amount <= 0 || getMoney(p) < amount) { sendCompactMsg(p, "§c§l[Chill MC] §fKhông đủ tiền!"); return true; }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "scoreboard players remove " + p.getName() + " tien " + amount);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "scoreboard players add " + target.getName() + " tien " + amount);
            sendCompactMsg(p, "§a§l[Chill MC] §fĐã chuyển §e" + amount + "$ §fcho §a" + target.getName());
            sendCompactMsg(target, "§a§l[Chill MC] §fNhận được §e+" + amount + "$ §ftừ §a" + p.getName());
            return true;
        }
        
        if (cmd.getName().equalsIgnoreCase("code")) {
            if (args.length < 1) { sendCompactMsg(p, "§c§l[Giftcode] §fCách dùng: §e/code <mã_code>"); return true; }
            if (args[0].equalsIgnoreCase("chillMC")) {
                if (hasUsedCode(p.getName(), "chillmc")) { sendCompactMsg(p, "§c§l[Giftcode] §fBạn đã dùng code §eChillMC §frồi!"); return true; }
                markUsedCode(p.getName(), "chillmc");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "scoreboard players add " + p.getName() + " tien 1000");
                giveItems(p, new ItemStack(Material.DIAMOND, 5), new ItemStack(Material.GOLDEN_APPLE, 2), new ItemStack(Material.COOKED_BEEF, 16));
                sendCompactMsg(p, "§a§l[Giftcode] §fNhập code §eChillMC §fthành công! Nhận §e+1.000$§f, §b5x Kim Cương§f, §62x Táo Vàng§f!"); return true;
            }
            sendCompactMsg(p, "§c§l[Giftcode] §fMã code không tồn tại!"); return true;
        }

        
        if (cmd.getName().equalsIgnoreCase("rtp")) {
            long now = System.currentTimeMillis();
            if (rtpCd.containsKey(p.getUniqueId()) && now - rtpCd.get(p.getUniqueId()) < 300000L) {
                long rem = (300000L - (now - rtpCd.get(p.getUniqueId()))) / 1000L;
                sendCompactMsg(p, "§c§l[RTP] §fVui lòng đợi §e" + (rem / 60) + " phút " + (rem % 60) + " giây §fnữa!"); return true;
            }
            Random r = new Random();
            org.bukkit.Location target = null;
            for (int i = 0; i < 15; i++) {
                int rx = p.getLocation().getBlockX() + r.nextInt(5000) - 2500;
                int rz = p.getLocation().getBlockZ() + r.nextInt(5000) - 2500;
                int ry = p.getWorld().getHighestBlockYAt(rx, rz);
                Material g = p.getWorld().getBlockAt(rx, ry, rz).getType();
                if (g.isSolid() && g != Material.LAVA && g != Material.MAGMA_BLOCK) {
                    target = new org.bukkit.Location(p.getWorld(), rx + 0.5, ry + 1.0, rz + 0.5);
                    target.getChunk().load(true);
                    break;
                }
            }
            if (target == null) { sendCompactMsg(p, "§c§l[RTP] §fChưa tìm thấy bãi đất an toàn, thử lại!"); return true; }
            final org.bukkit.Location dest = target;
            final org.bukkit.Location start = p.getLocation().clone();
            sendCompactMsg(p, "§e§l[RTP] §fĐang tải vùng đất mới... Đứng yên §a10 giây §fđể dịch chuyển!");
            new org.bukkit.scheduler.BukkitRunnable() {
                int sec = 10;
                @Override public void run() {
                    if (!p.isOnline()) { cancel(); return; }
                    if (p.getLocation().distanceSquared(start) > 0.5) {
                        sendCompactMsg(p, "§c§l[RTP] §fĐã hủy dịch chuyển vì bạn di chuyển!"); cancel(); return;
                    }
                    if (sec <= 0) {
                        rtpCd.put(p.getUniqueId(), System.currentTimeMillis());
                        p.teleport(dest);
                        sendCompactMsg(p, "§a§l[RTP] §fDịch chuyển thành công tới §eX:" + dest.getBlockX() + " Y:" + dest.getBlockY() + " Z:" + dest.getBlockZ() + "§f!");
                        cancel(); return;
                    }
                    sendCompactMsg(p, "§7[RTP] §fDịch chuyển sau §e" + sec + "s§f...");
                    sec--;
                }
            }.runTaskTimer(this, 0L, 20L);
            return true;
        }
        if (cmd.getName().equalsIgnoreCase("sethome")) {
            if (args.length < 1) { sendCompactMsg(p, "§c§l[Home] §fCách dùng: §e/sethome <tên_nhà>"); return true; }
            String h = args[0].toLowerCase(); YamlConfiguration cfg = getHomeCfg(); String b = p.getUniqueId().toString();
            Set<String> ks = cfg.getConfigurationSection(b) != null ? cfg.getConfigurationSection(b).getKeys(false) : new HashSet<>();
            int maxH = p.isOp() ? 10 : 4; if (!ks.contains(h) && ks.size() >= maxH) { sendCompactMsg(p, "§c§l[Home] §fTối đa §e" + maxH + " nhà§f! Dùng §e/delhome <tên> §fđể xóa bớt."); return true; }
            cfg.set(b + "." + h, p.getLocation()); saveHomeCfg(cfg);
            sendCompactMsg(p, "§a§l[Home] §fĐã đặt nhà §e" + h + " §f(" + (ks.contains(h) ? ks.size() : ks.size() + 1) + "/" + maxH + ")!"); return true;
        }
        if (cmd.getName().equalsIgnoreCase("home")) {
            YamlConfiguration cfg = getHomeCfg(); String b = p.getUniqueId().toString();
            Set<String> ks = cfg.getConfigurationSection(b) != null ? cfg.getConfigurationSection(b).getKeys(false) : new HashSet<>();
            if (args.length < 1) { sendCompactMsg(p, "§a§l[Home] §fDanh sách nhà (" + ks.size() + "/4): §e" + (ks.isEmpty() ? "Chưa có" : String.join(", ", ks))); return true; }
            org.bukkit.Location loc = cfg.getLocation(b + "." + args[0].toLowerCase());
            if (loc == null) { sendCompactMsg(p, "§c§l[Home] §fKhông tìm thấy nhà §e" + args[0] + "§f!"); return true; }
            safeTp(p, loc); sendCompactMsg(p, "§a§l[Home] §fĐã về nhà §e" + args[0].toLowerCase() + "§f!"); return true;
        }
        if (cmd.getName().equalsIgnoreCase("delhome")) {
            if (args.length < 1) { sendCompactMsg(p, "§c§l[Home] §fCách dùng: §e/delhome <tên_nhà>"); return true; }
            YamlConfiguration cfg = getHomeCfg(); String path = p.getUniqueId().toString() + "." + args[0].toLowerCase();
            if (!cfg.contains(path)) { sendCompactMsg(p, "§c§l[Home] §fKhông có nhà §e" + args[0] + "§f!"); return true; }
            cfg.set(path, null); saveHomeCfg(cfg); sendCompactMsg(p, "§a§l[Home] §fĐã xóa nhà §e" + args[0].toLowerCase() + "§f!"); return true;
        }

        if (cmd.getName().equalsIgnoreCase("tpa")) {
            if (args.length < 1) { sendCompactMsg(p, "§c§l[TPA] §fCách dùng: §e/tpa <tên>"); return true; }
            Player tg = Bukkit.getPlayerExact(args[0]);
            if (tg == null || !tg.isOnline() || tg.equals(p)) { sendCompactMsg(p, "§c§l[TPA] §fNgười chơi không online!"); return true; }
            tpaReq.put(tg.getUniqueId(), p.getUniqueId()); tpaTime.put(tg.getUniqueId(), System.currentTimeMillis());
            sendCompactMsg(p, "§a§l[TPA] §fĐã gửi lời mời tới §e" + tg.getName() + "§f!");
            sendCompactMsg(tg, "§e§l[TPA] §a" + p.getName() + " §fmuốn dịch chuyển tới bạn! Gõ §a/tpaccept §fđể đồng ý (60s)."); return true;
        }
        if (cmd.getName().equalsIgnoreCase("tpaccept")) {
            UUID sid = tpaReq.get(p.getUniqueId());
            if (sid == null || System.currentTimeMillis() - tpaTime.getOrDefault(p.getUniqueId(), 0L) > 60000L) { sendCompactMsg(p, "§c§l[TPA] §fKhông có lời mời hoặc đã hết hạn!"); return true; }
            tpaReq.remove(p.getUniqueId()); Player rp = Bukkit.getPlayer(sid);
            if (rp == null || !rp.isOnline()) { sendCompactMsg(p, "§c§l[TPA] §fNgười gửi đã thoát!"); return true; }
            rp.teleport(p.getLocation()); sendCompactMsg(rp, "§a§l[TPA] §e" + p.getName() + " §fđã đồng ý!"); sendCompactMsg(p, "§a§l[TPA] §fĐã chấp nhận §e" + rp.getName() + "§f!"); return true;
        }

        
        if (cmd.getName().equalsIgnoreCase("setspawn")) { if (!p.isOp()) { sendCompactMsg(p, "§cBạn không có quyền!"); return true; } YamlConfiguration cfg = new YamlConfiguration(); org.bukkit.Location sl = p.getLocation().clone(); sl.setX(sl.getBlockX() + 0.5); sl.setZ(sl.getBlockZ() + 0.5); sl.setYaw(Math.round(sl.getYaw() / 90f) * 90f); sl.setPitch(0f); cfg.set("spawn", sl); try { if (!getDataFolder().exists()) getDataFolder().mkdirs(); cfg.save(new File(getDataFolder(), "spawn.yml")); } catch (Exception ignored) {} sendCompactMsg(p, "§a§l[Spawn] §fĐã đặt điểm Spawn tại vị trí hiện tại!"); return true; }
        if (cmd.getName().equalsIgnoreCase("spawn")) {
            File f = new File(getDataFolder(), "spawn.yml");
            org.bukkit.Location loc = f.exists() ? YamlConfiguration.loadConfiguration(f).getLocation("spawn") : null;
            if (loc == null || loc.getWorld() == null) loc = p.getWorld().getSpawnLocation();
            int cx = loc.getBlockX() >> 4, cz = loc.getBlockZ() >> 4;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) loc.getWorld().getChunkAt(cx + dx, cz + dz).load(true);
            p.teleport(loc);
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.DAMAGE_RESISTANCE, 60, 255));
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOW_FALLING, 60, 0));
            sendCompactMsg(p, "§a§l[Spawn] §fĐã dịch chuyển về khu vực §eSpawn§f!");
            return true;
        }
        
                if (cmd.getName().equalsIgnoreCase("resetmk")) { if (!sender.isOp()) { sendCompactMsg(sender, "§cBạn không có quyền!"); return true; } if (args.length < 1) { sendCompactMsg(sender, "§cCách dùng: /resetmk <tên>"); return true; } YamlConfiguration cfg = getPassCfg(); cfg.set(args[0].toLowerCase(), null); savePassCfg(cfg); sendCompactMsg(sender, "§a§l[Bảo Mật] §fĐã xóa mật khẩu của §e" + args[0] + "§f!"); return true; }
        if (cmd.getName().equalsIgnoreCase("setmk")) {
            if (!sender.isOp()) { sendCompactMsg(sender, "§cBạn không có quyền!"); return true; }
            if (args.length < 2) { sendCompactMsg(sender, "§cCách dùng: /setmk <tên> <mật_khẩu_mới>"); return true; }
            YamlConfiguration cfg = getPassCfg(); cfg.set(args[0].toLowerCase(), args[1]); savePassCfg(cfg);
            sendCompactMsg(sender, "§a§l[Bảo Mật] §fĐã đặt mật khẩu của §e" + args[0] + " §fthành: §a" + args[1]); return true;
        }
        if (cmd.getName().equalsIgnoreCase("msg")) {
            if (args.length < 2) { sendCompactMsg(p, "§c§l[Tin Nhắn] §fCách dùng: §e/msg <tên> <nội_dung>"); return true; }
            Player tg = Bukkit.getPlayerExact(args[0]);
            if (tg == null || !tg.isOnline() || tg.equals(p)) { sendCompactMsg(p, "§c§l[Tin Nhắn] §fNgười chơi không online!"); return true; }
            String text = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            lastMsg.put(p.getUniqueId(), tg.getUniqueId()); lastMsg.put(tg.getUniqueId(), p.getUniqueId());
            sendCompactMsg(p, "§d§l[Bạn -> " + tg.getName() + "] §f" + text); sendCompactMsg(tg, "§d§l[" + p.getName() + " -> Bạn] §f" + text); return true;
        }
        if (cmd.getName().equalsIgnoreCase("r")) {
            UUID tid = lastMsg.get(p.getUniqueId());
            if (tid == null) { sendCompactMsg(p, "§c§l[Tin Nhắn] §fChưa có ai nhắn tin cho bạn!"); return true; }
            Player tg = Bukkit.getPlayer(tid);
            if (tg == null || !tg.isOnline() || args.length < 1) { sendCompactMsg(p, "§c§l[Tin Nhắn] §fKhông thể trả lời!"); return true; }
            String text = String.join(" ", args); lastMsg.put(tg.getUniqueId(), p.getUniqueId());
            sendCompactMsg(p, "§d§l[Bạn -> " + tg.getName() + "] §f" + text); sendCompactMsg(tg, "§d§l[" + p.getName() + " -> Bạn] §f" + text); return true;
        }

        if (cmd.getName().equalsIgnoreCase("create")) {
            YamlConfiguration cfg = getClanCfg(); String me = p.getName().toLowerCase();
            if (!cfg.getString("player." + me, "").isEmpty()) { sendCompactMsg(p, "§c§l[Clan] §fBạn đã ở trong Clan rồi!"); return true; }
            if (args.length < 1 || args[0].length() > 8) { sendCompactMsg(p, "§c§l[Clan] §fCách dùng: §e/create <tên_clan> §7(1-8 ký tự)"); return true; }
            String cname = args[0].toUpperCase();
            if (cfg.contains("clan." + cname)) { sendCompactMsg(p, "§c§l[Clan] §fTên Clan đã tồn tại!"); return true; }
            if (getMoney(p) < 500) { sendCompactMsg(p, "§c§l[Clan] §fCần §e500$ §fđể lập Clan!"); return true; }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "scoreboard players remove " + p.getName() + " tien 500");
            cfg.set("clan." + cname + ".owner", me); cfg.set("clan." + cname + ".members", Arrays.asList(p.getName())); cfg.set("player." + me, cname); saveClanCfg(cfg);
            sendCompactMsg(p, "§a§l[Clan] §fTạo thành công Clan §b[" + cname + "] §f(-§e500$§f)!"); return true;
        }
        if (cmd.getName().equalsIgnoreCase("invite")) {
            YamlConfiguration cfg = getClanCfg(); String me = p.getName().toLowerCase(); String cur = cfg.getString("player." + me, "");
            if (cur.isEmpty() || !cfg.getString("clan." + cur + ".owner", "").equals(me)) { sendCompactMsg(p, "§c§l[Clan] §fChỉ Chủ Clan mới được mời!"); return true; }
            if (args.length < 1) { sendCompactMsg(p, "§c§l[Clan] §fCách dùng: §e/invite <tên_người_chơi>"); return true; }
            Player tg = Bukkit.getPlayerExact(args[0]);
            if (tg == null || !tg.isOnline()) { sendCompactMsg(p, "§c§l[Clan] §fNgười chơi không online!"); return true; }
            cfg.set("invite." + tg.getName().toLowerCase(), cur); saveClanCfg(cfg);
            sendCompactMsg(p, "§a§l[Clan] §fĐã gửi lời mời vào Clan cho §e" + tg.getName());
            net.md_5.bungee.api.chat.TextComponent m1 = new net.md_5.bungee.api.chat.TextComponent("§b§l[Clan] §fBạn được mời vào Clan §b[" + cur + "]§f! Bấm vào đây: ");
            net.md_5.bungee.api.chat.TextComponent btn = new net.md_5.bungee.api.chat.TextComponent("§a§l[JOIN]");
            btn.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/join " + cur));
            m1.addExtra(btn); tg.spigot().sendMessage(m1); return true;
        }
        if (cmd.getName().equalsIgnoreCase("join")) {
            YamlConfiguration cfg = getClanCfg(); String me = p.getName().toLowerCase();
            if (!cfg.getString("player." + me, "").isEmpty()) { sendCompactMsg(p, "§c§l[Clan] §fHãy rời Clan cũ trước bằng §e/clan leave§f!"); return true; }
            String cname = args.length >= 1 ? args[0].toUpperCase() : cfg.getString("invite." + me, "");
            if (cname.isEmpty() || !cfg.getString("invite." + me, "").equals(cname)) { sendCompactMsg(p, "§c§l[Clan] §fBạn chưa nhận được lời mời vào Clan này!"); return true; }
            List<String> mems = new ArrayList<>(cfg.getStringList("clan." + cname + ".members")); mems.add(p.getName());
            cfg.set("clan." + cname + ".members", mems); cfg.set("player." + me, cname); cfg.set("invite." + me, null); saveClanCfg(cfg);
            sendCompactMsg(p, "§a§l[Clan] §fChào mừng bạn gia nhập Clan §b[" + cname + "]§f!"); return true;
        }
        if (cmd.getName().equalsIgnoreCase("cc")) {
            String cl = getPlayerClan(p.getName());
            if (cl.isEmpty() || args.length < 1) { sendCompactMsg(p, "§c§l[Clan] §fCách dùng: §e/cc <tin_nhắn>"); return true; }
            String msg = "§b§l[Clan " + cl + "] §f" + p.getName() + ": §e" + String.join(" ", args);
            for (Player op : Bukkit.getOnlinePlayers()) if (getPlayerClan(op.getName()).equals(cl)) sendCompactMsg(op, msg); return true;
        }
        if (cmd.getName().equalsIgnoreCase("clan")) {
            YamlConfiguration cfg = getClanCfg(); String me = p.getName().toLowerCase(); String cur = cfg.getString("player." + me, "");
            if (cur.isEmpty()) { sendCompactMsg(p, "§6§l[Clan] §fBạn chưa có Clan! Dùng §e/create <tên> §fđể lập Clan."); return true; }
            
            if (args.length >= 1 && args[0].equalsIgnoreCase("kick")) {
                if (!cfg.getString("clan." + cur + ".owner", "").equals(me)) { sendCompactMsg(p, "§c§l[Clan] §fChỉ Chủ Clan mới có quyền đuổi thành viên!"); return true; }
                if (args.length < 2) { sendCompactMsg(p, "§c§l[Clan] §fCách dùng: §e/clan kick <tên_thành_viên>"); return true; }
                if (args[1].equalsIgnoreCase(p.getName())) { sendCompactMsg(p, "§c§l[Clan] §fKhông thể tự đuổi chính mình! Dùng §e/clan leave §fđể giải tán."); return true; }
                List<String> mems = new ArrayList<>(cfg.getStringList("clan." + cur + ".members"));
                String found = null;
                for (String m : mems) if (m.equalsIgnoreCase(args[1])) found = m;
                if (found == null) { sendCompactMsg(p, "§c§l[Clan] §fNgười chơi §e" + args[1] + " §fkhông có trong Clan của bạn!"); return true; }
                mems.remove(found); cfg.set("clan." + cur + ".members", mems); cfg.set("player." + found.toLowerCase(), null); saveClanCfg(cfg);
                sendCompactMsg(p, "§a§l[Clan] §fĐã đuổi §e" + found + " §fkhỏi Clan §b[" + cur + "]§f!");
                Player tg = Bukkit.getPlayerExact(found);
                if (tg != null && tg.isOnline()) sendCompactMsg(tg, "§c§l[Clan] §fBạn đã bị đuổi khỏi Clan §b[" + cur + "]§f!");
                return true;
            }

            if (args.length >= 1 && args[0].equalsIgnoreCase("leave")) {
                if (cfg.getString("clan." + cur + ".owner", "").equals(me)) {
                    for (String m : cfg.getStringList("clan." + cur + ".members")) cfg.set("player." + m.toLowerCase(), null);
                    cfg.set("clan." + cur, null); saveClanCfg(cfg); sendCompactMsg(p, "§c§l[Clan] §fĐã giải tán Clan §b[" + cur + "]§f!"); return true;
                }
                List<String> mems = new ArrayList<>(cfg.getStringList("clan." + cur + ".members")); mems.remove(p.getName());
                cfg.set("clan." + cur + ".members", mems); cfg.set("player." + me, null); saveClanCfg(cfg); sendCompactMsg(p, "§e§l[Clan] §fĐã rời khỏi Clan §b[" + cur + "]§f!"); return true;
            }
            sendCompactMsg(p, "§b§l[Clan " + cur + "] §fChủ: §e" + cfg.getString("clan." + cur + ".owner") + " §f| Thành viên: §a" + String.join(", ", cfg.getStringList("clan." + cur + ".members")) + " §7(/clan leave để rời)"); return true;
        }

                if (cmd.getName().equalsIgnoreCase("register")) { String k = p.getName().toLowerCase(); YamlConfiguration cfg = getPassCfg(); if (cfg.contains(k)) { sendCompactMsg(p, "§c§l[Bảo Mật] §fĐã đăng ký rồi! Gõ §a/login <mật_khẩu>"); return true; } if (args.length < 2 || !args[0].equals(args[1])) { sendCompactMsg(p, "§c§l[Bảo Mật] §fGõ: §e/register <mk> <nhập_lại>"); return true; } cfg.set(k, args[0]); savePassCfg(cfg); unlogged.remove(p.getUniqueId()); p.sendTitle("§a§lTHÀNH CÔNG!", "§fChào mừng bạn đến với §6§lChill MC", 5, 40, 10); p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f); sendCompactMsg(p, "§a§l[Bảo Mật] §fĐăng ký thành công! Đã mở khóa di chuyển."); return true; }
        if (cmd.getName().equalsIgnoreCase("login")) { String k = p.getName().toLowerCase(); YamlConfiguration cfg = getPassCfg(); String sp = cfg.getString("server_global_pass", ""); if (args.length < 1 || (!cfg.getString(k, "").equals(args[0]) && (sp.isEmpty() || !sp.equals(args[0])))) { sendCompactMsg(p, "§c§l[Bảo Mật] §fSai mật khẩu hoặc chưa đăng ký!"); return true; } unlogged.remove(p.getUniqueId()); p.sendTitle("§a§lTHÀNH CÔNG!", "§fChào mừng bạn đến với §6§lChill MC", 5, 40, 10); p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f); sendCompactMsg(p, "§a§l[Bảo Mật] §fĐăng nhập thành công! Đã mở khóa di chuyển."); return true; }
        
        if (cmd.getName().equalsIgnoreCase("viewclan")) { if (sender instanceof Player && args.length != 0) { openClanHierarchyGui((Player) sender, java.util.Arrays.asList(args).get(0)); return true; }
            YamlConfiguration cfg = getClanCfg(); String me = p.getName().toLowerCase(); String cur = cfg.getString("player." + me, "");
            if (cur.isEmpty()) { sendCompactMsg(p, "§c§l[Clan] §fBạn chưa tham gia Clan nào!"); return true; }
            String owner = cfg.getString("clan." + cur + ".owner", "");
            List<String> coList = new ArrayList<>(); for (String s : cfg.getStringList("clan." + cur + ".coleaders")) coList.add(s.toLowerCase());
            int myRank = owner.equalsIgnoreCase(me) ? 3 : (coList.contains(me) ? 2 : 1);
            List<String> mems = cfg.getStringList("clan." + cur + ".members");
            Inventory inv = Bukkit.createInventory(null, 54, "§3§lThành Viên Clan: " + cur);
            Objective obj = Bukkit.getScoreboardManager().getMainScoreboard().getObjective("tien");
            for (int i = 0; i < mems.size() && i < 54; i++) {
                String mName = mems.get(i);
                int tRank = owner.equalsIgnoreCase(mName) ? 3 : (coList.contains(mName.toLowerCase()) ? 2 : 1);
                String cRole = (tRank == 3) ? "§6§lChủ Clan" : (tRank == 2 ? "§b§lPhó Clan" : "§eThành viên");
                ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
                org.bukkit.inventory.meta.SkullMeta sm = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
                if (sm != null) {
                    org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(mName);
                    sm.setOwningPlayer(op); sm.setDisplayName("§e§l" + mName);
                    if (myRank > tRank) {
                        String gRole = op.isOp() ? "§c§lADMIN (OP)" : "§aNgười chơi";
                        int money = (obj == null) ? 0 : obj.getScore(mName).getScore();
                        sm.setLore(Arrays.asList("§8§m--------------------", "§7Chức vụ Game: " + gRole, "§7Chức vụ Clan: " + cRole, "§7Số tiền: §6" + money + "$"));
                    } else {
                        sm.setLore(Arrays.asList("§8§m--------------------", "§7Chức vụ Clan: " + cRole, "§c§oChỉ xem được thông tin cấp thấp hơn!"));
                    }
                    head.setItemMeta(sm);
                }
                inv.setItem(i, head);
            }
            p.openInventory(inv); return true;
        }
        
        if (cmd.getName().equalsIgnoreCase("claimkeyreward")) {
            try {
                if (!getDataFolder().exists()) getDataFolder().mkdirs();
                File f = new File(getDataFolder(), "rewards.yml");
                YamlConfiguration cfg = f.exists() ? YamlConfiguration.loadConfiguration(f) : new YamlConfiguration();
                String path = "claim_time." + p.getName().toLowerCase();
                long now = System.currentTimeMillis(), last = cfg.getLong(path, 0L);
                if (now - last < 86400000L) {
                    long rem = (86400000L - (now - last)) / 1000L;
                    sendCompactMsg(p, "§c§l[Reward] §fBạn đã nhận hôm nay rồi! Quay lại sau §e" + (rem / 3600) + " giờ " + ((rem % 3600) / 60) + " phút§f.");
                    return true;
                }
                cfg.set(path, now); cfg.save(f);
            } catch (Exception ignored) {}
            giveItems(p, makeKey(1));
            sendCompactMsg(p, "§a§l[Reward] §fBạn đã nhận thành công §6§l1x Key Reward§f! Hãy ra Rương ở Spawn để quay.");
            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
            return true;
        }

        
        if (cmd.getName().equalsIgnoreCase("setcrate")) {
            if (!p.isOp()) { sendCompactMsg(p, "§cBạn không có quyền!"); return true; }
            org.bukkit.block.Block b = p.getTargetBlockExact(5);
            if (b == null || b.getType() != Material.CHEST) { sendCompactMsg(p, "§c§l[Rương Thưởng] §fHãy nhìn thẳng vào 1 cái Rương (Chest)!"); return true; }
            try { if (!getDataFolder().exists()) getDataFolder().mkdirs(); YamlConfiguration cfg = new YamlConfiguration(); cfg.set("crate", b.getLocation()); cfg.save(new File(getDataFolder(), "crate.yml")); } catch (Exception ignored) {}
            sendCompactMsg(p, "§a§l[Rương Thưởng] §fĐã đặt Rương Quay Thưởng tại vị trí này!"); return true;
        }
        if (cmd.getName().equalsIgnoreCase("givekey")) {
            if (!sender.isOp()) { sendCompactMsg(sender, "§cBạn không có quyền!"); return true; }
            if (args.length < 2) { sendCompactMsg(sender, "§cCách dùng: /givekey <tên> <số_lượng>"); return true; }
            Player tg = Bukkit.getPlayerExact(args[0]);
            if (tg == null || !tg.isOnline()) { sendCompactMsg(sender, "§cNgười chơi không online!"); return true; }
            int amt = Integer.parseInt(args[1]); giveItems(tg, makeKey(amt));
            sendCompactMsg(sender, "§a§l[Rương Thưởng] §fĐã trao §e" + amt + "x Key Reward §fcho §a" + tg.getName() + "§f!"); return true;
        }

        
        if (cmd.getName().equalsIgnoreCase("helpchillmc")) {
            if (sender instanceof Player) openHelpGui((Player) sender);
            return true;
        }

        
        if (cmd.getName().equalsIgnoreCase("helpclan")) {
            sendCompactMsg(sender, "§8§m----------------------------------------");
            sendCompactMsg(sender, "§6§l      HUONG DAN HE THONG CLAN CHILL MC");
            sendCompactMsg(sender, "§e/clan create <ten> §f- Tao Clan moi");
            sendCompactMsg(sender, "§e/clan invite <ten> §f- Moi nguoi choi vao Clan");
            sendCompactMsg(sender, "§e/clan join <ten> §f- Chap nhan loi moi vao Clan");
            sendCompactMsg(sender, "§e/clan promote <ten> §f- Phong / Ha chuc Pho Clan");
            sendCompactMsg(sender, "§e/clan upgrade §f- Nang cap Clan (Cap 2: 10k$, Cap 3 Max: 50k$)");
            sendCompactMsg(sender, "§e/viewclan <ten> §f- Xem giao dien & thu bac thanh vien Clan");
            sendCompactMsg(sender, "§e/clan kick <ten> §f- Duoi thanh vien khoi Clan");
            sendCompactMsg(sender, "§e/clan leave §f- Roi khoi Clan hien tai");
            sendCompactMsg(sender, "§8§m----------------------------------------");
            return true;
        }

        
        if (cmd.getName().equalsIgnoreCase("shop")) { openMainShop(p); return true; }
        return false;
    }

    private void openAH(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "§4§lChợ Đen Chill MC (/ah)");
        for (int i = 0; i < ahItems.size() && i < 45; i++) {
            ItemStack display = ahItems.get(i).clone();
            ItemMeta meta = display.getItemMeta();
            List<String> lore = (meta != null && meta.hasLore()) ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            String seller = ahSellers.get(i);
            int price = ahPrices.get(i);
            lore.add("§8§m--------------------");
            lore.add("§7Người bán: §a" + seller);
            lore.add("§7Giá bán: §e" + price + "$");
            lore.add(seller.equals(p.getName()) ? "§c▶ Bấm để THU HỒI về túi!" : "§a▶ Bấm để MUA NGAY!");
            if (meta != null) { meta.setLore(lore); display.setItemMeta(meta); }
            inv.setItem(i, display);
        }
        inv.setItem(49, menuIcon(Material.BOOK, "§e§lHướng Dẫn Treo Bán", "§7Cầm đồ trên tay và gõ: §a/ah sell <giá>"));
        p.openInventory(inv);
    }
    private void openMainShop(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, "§2§lCửa Hàng Chill MC");
        inv.setItem(10, menuIcon(Material.GRASS_BLOCK, "§a§lKhối Xây Dựng (Nhiều Trang)", "§7Đầy đủ tất cả các loại block xây dựng"));
        inv.setItem(12, menuIcon(Material.ENDER_PEARL, "§d§lĐá Đỏ & Chế Thuốc", "§7Piston, đá đỏ, pha thuốc & Ender"));
        inv.setItem(14, menuIcon(Material.DIAMOND_SWORD, "§b§lTrang Bị & Bộ Giáp", "§7Trọn bộ giáp Sắt/KC & vũ khí"));
        inv.setItem(16, menuIcon(Material.DIAMOND_PICKAXE, "§e§lDụng Cụ (Cúp, Rìu, Xẻng)", "§7Đủ bộ dụng cụ Đá, Sắt, KC"));
        p.openInventory(inv);
    }

    private void openBlocksPage(Player p, int page) {
        Material[] allBlocks = {
            Material.GRASS_BLOCK, Material.DIRT, Material.COARSE_DIRT, Material.PODZOL, Material.MYCELIUM, Material.SAND, Material.RED_SAND, Material.GRAVEL, Material.CLAY,
            Material.COBBLESTONE, Material.MOSSY_COBBLESTONE, Material.STONE, Material.SMOOTH_STONE, Material.STONE_BRICKS, Material.MOSSY_STONE_BRICKS, Material.CRACKED_STONE_BRICKS, Material.CHISELED_STONE_BRICKS, Material.GRANITE,
            Material.POLISHED_GRANITE, Material.DIORITE, Material.POLISHED_DIORITE, Material.ANDESITE, Material.POLISHED_ANDESITE, Material.DEEPSLATE, Material.COBBLED_DEEPSLATE, Material.POLISHED_DEEPSLATE, Material.DEEPSLATE_BRICKS,
            Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG, Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.CHERRY_LOG, Material.MANGROVE_LOG, Material.BAMBOO_BLOCK,
            Material.OAK_PLANKS, Material.BIRCH_PLANKS, Material.SPRUCE_PLANKS, Material.DARK_OAK_PLANKS, Material.GLASS, Material.WHITE_STAINED_GLASS, Material.BLACK_STAINED_GLASS, Material.TINTED_GLASS, Material.BOOKSHELF,
            Material.BRICKS, Material.TERRACOTTA, Material.WHITE_TERRACOTTA, Material.QUARTZ_BLOCK, Material.SMOOTH_QUARTZ, Material.QUARTZ_PILLAR, Material.PRISMARINE, Material.PRISMARINE_BRICKS, Material.DARK_PRISMARINE,
            Material.WHITE_WOOL, Material.RED_WOOL, Material.BLUE_WOOL, Material.YELLOW_WOOL, Material.GREEN_WOOL, Material.BLACK_WOOL, Material.WHITE_CONCRETE, Material.BLACK_CONCRETE, Material.RED_CONCRETE,
            Material.BLUE_CONCRETE, Material.YELLOW_CONCRETE, Material.LIME_CONCRETE, Material.CYAN_CONCRETE, Material.PURPLE_CONCRETE, Material.PINK_CONCRETE, Material.ORANGE_CONCRETE, Material.LIGHT_BLUE_CONCRETE, Material.GRAY_CONCRETE,
            Material.GLOWSTONE, Material.SEA_LANTERN, Material.SHROOMLIGHT, Material.JACK_O_LANTERN, Material.SNOW_BLOCK, Material.ICE, Material.PACKED_ICE, Material.BLUE_ICE, Material.MAGMA_BLOCK,
            Material.NETHERRACK, Material.NETHER_BRICKS, Material.RED_NETHER_BRICKS, Material.SOUL_SAND, Material.SOUL_SOIL, Material.BASALT, Material.POLISHED_BASALT, Material.BLACKSTONE, Material.POLISHED_BLACKSTONE_BRICKS,
            Material.END_STONE, Material.END_STONE_BRICKS, Material.PURPUR_BLOCK, Material.PURPUR_PILLAR, Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.HAY_BLOCK, Material.BONE_BLOCK, Material.HONEYCOMB_BLOCK
        };
        int perPage = 45, maxPage = (int) Math.ceil((double) allBlocks.length / perPage);
        page = Math.max(1, Math.min(page, maxPage));
        Inventory inv = Bukkit.createInventory(null, 54, "§2§lShop Khối - Trang " + page + "/" + maxPage);
        int start = (page - 1) * perPage;
        for (int i = 0; i < perPage && (start + i) < allBlocks.length; i++) inv.setItem(i, shopItem(allBlocks[start + i], 1, "§f" + allBlocks[start + i].name(), getUnitPrice(allBlocks[start + i])));
        if (page > 1) inv.setItem(45, menuIcon(Material.PAPER, "§e◀ Trang Trước (" + (page - 1) + ")", "§7Quay lại trang trước"));
        inv.setItem(49, menuIcon(Material.ARROW, "§c◀ Quay Lại Menu Chính", "§7Trở về danh mục"));
        if (page < maxPage) inv.setItem(53, menuIcon(Material.PAPER, "§aTrang Sau (" + (page + 1) + ") ▶", "§7Xem thêm khối mới"));
        p.openInventory(inv);
    }

    private void openRedstoneBrewShop(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "§5§lShop: Đá Đỏ & Chế Thuốc");
        Material[] mats = {
            Material.REDSTONE, Material.REDSTONE_BLOCK, Material.REDSTONE_TORCH, Material.REPEATER, Material.COMPARATOR, Material.PISTON, Material.STICKY_PISTON, Material.OBSERVER, Material.HOPPER,
            Material.DISPENSER, Material.DROPPER, Material.SLIME_BLOCK, Material.HONEY_BLOCK, Material.TNT, Material.LEVER, Material.STONE_BUTTON, Material.TRIPWIRE_HOOK, Material.DAYLIGHT_DETECTOR,
            Material.BREWING_STAND, Material.CAULDRON, Material.GLASS_BOTTLE, Material.NETHER_WART, Material.BLAZE_ROD, Material.BLAZE_POWDER, Material.GHAST_TEAR, Material.GLOWSTONE_DUST, Material.GUNPOWDER,
            Material.FERMENTED_SPIDER_EYE, Material.SPIDER_EYE, Material.MAGMA_CREAM, Material.GLISTERING_MELON_SLICE, Material.GOLDEN_CARROT, Material.RABBIT_FOOT, Material.PUFFERFISH, Material.PHANTOM_MEMBRANE, Material.ENDER_PEARL
        };
        for (int i = 0; i < mats.length; i++) inv.setItem(i, shopItem(mats[i], 1, "§f" + mats[i].name(), getUnitPrice(mats[i])));
        inv.setItem(49, menuIcon(Material.ARROW, "§c◀ Quay Lại Menu Chính", "§7Trở về danh mục"));
        p.openInventory(inv);
    }

    private void openGearShop(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, "§b§lShop: Trang Bị & Bộ Giáp");
        inv.setItem(10, shopItem(Material.IRON_CHESTPLATE, 1, "§f§lTrọn Bộ Giáp Sắt (4 Món)", 400));
        inv.setItem(11, shopItem(Material.DIAMOND_CHESTPLATE, 1, "§b§lTrọn Bộ Giáp Kim Cương (4 Món)", 1500));
        inv.setItem(12, shopItem(Material.IRON_SWORD, 1, "§fKiếm Sắt", 120));
        inv.setItem(13, shopItem(Material.DIAMOND_SWORD, 1, "§bKiếm Kim Cương", 450));
        inv.setItem(14, shopItem(Material.IRON_INGOT, 1, "§fPhôi Sắt", 20));
        inv.setItem(15, shopItem(Material.DIAMOND, 1, "§bKim Cương", 130));
        inv.setItem(16, shopItem(Material.GOLDEN_APPLE, 1, "§6Táo Vàng", 200));
        inv.setItem(19, shopItem(Material.TOTEM_OF_UNDYING, 1, "§6§l§r §6§lPhật Tổ §6§l", 500));
        inv.setItem(22, menuIcon(Material.ARROW, "§c◀ Quay Lại Menu Chính", "§7Trở về danh mục"));
        p.openInventory(inv);
    }

    private void openToolsShop(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, "§e§lShop: Dụng Cụ");
        Material[] tools = {Material.STONE_PICKAXE, Material.STONE_AXE, Material.STONE_SHOVEL, Material.IRON_PICKAXE, Material.IRON_AXE, Material.IRON_SHOVEL, Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE, Material.DIAMOND_SHOVEL};
        for (int i = 0; i < tools.length; i++) inv.setItem(i, shopItem(tools[i], 1, "§f" + tools[i].name(), getUnitPrice(tools[i])));
        inv.setItem(22, menuIcon(Material.ARROW, "§c◀ Quay Lại Menu Chính", "§7Trở về danh mục"));
        p.openInventory(inv);
    }

    private void openBuyAmountGUI(Player p, Material mat, int amount, String prevTitle) {
        amount = Math.max(1, Math.min(amount, 2304));
        buyMat.put(p.getUniqueId(), mat); buyAmt.put(p.getUniqueId(), amount); lastPage.put(p.getUniqueId(), prevTitle);
        int unit = getUnitPrice(mat), total = unit * amount;
        Inventory inv = Bukkit.createInventory(null, 27, "§1§lChọn Số Lượng Mua");
        inv.setItem(10, amtBtn(Material.RED_STAINED_GLASS_PANE, "§c§l-32", 32));
        inv.setItem(11, amtBtn(Material.RED_STAINED_GLASS_PANE, "§c§l-16", 16));
        inv.setItem(12, amtBtn(Material.RED_STAINED_GLASS_PANE, "§c§l-1", 1));
        ItemStack preview = new ItemStack(mat, Math.min(amount, 64)); ItemMeta pm = preview.getItemMeta();
        if (pm != null) { pm.setDisplayName("§e§l" + mat.name() + " §f(x" + amount + ")"); pm.setLore(Arrays.asList("§7Đơn giá: §e" + unit + "$", "§7Số lượng: §a" + amount, "§7Tổng tiền: §6§l" + total + "$")); preview.setItemMeta(pm); }
        inv.setItem(13, preview);
        inv.setItem(14, amtBtn(Material.LIME_STAINED_GLASS_PANE, "§a§l+1", 1));
        inv.setItem(15, amtBtn(Material.LIME_STAINED_GLASS_PANE, "§a§l+16", 16));
        inv.setItem(16, amtBtn(Material.LIME_STAINED_GLASS_PANE, "§a§l+32", 32));
        inv.setItem(21, menuIcon(Material.ARROW, "§c◀ Quay Lại Trang Cũ", "§7Trở về danh sách trước"));
        inv.setItem(23, menuIcon(Material.EMERALD_BLOCK, "§a§lXÁC NHẬN MUA (" + total + "$)", "§7Mua §e" + amount + "x " + mat.name()));
        p.openInventory(inv);
    }

    private ItemStack amtBtn(Material mat, String label, int amt) {
        ItemStack it = new ItemStack(mat, amt); ItemMeta m = it.getItemMeta();
        if (m != null) { m.setDisplayName(label); m.setLore(Arrays.asList("§7Bấm để đổi số lượng")); it.setItemMeta(m); }
        return it;
    }

    private ItemStack menuIcon(Material mat, String name, String desc) {
        ItemStack it = new ItemStack(mat, 1); ItemMeta meta = it.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); meta.setLore(Arrays.asList(desc, "§e▶ Bấm để chọn!")); it.setItemMeta(meta); }
        return it;
    }

    private ItemStack shopItem(Material mat, int amt, String name, int price) {
        ItemStack it = new ItemStack(mat, amt); ItemMeta meta = it.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); meta.setLore(Arrays.asList("§7Đơn giá: §e" + price + "$", "§a▶ Bấm chọn số lượng!")); it.setItemMeta(meta); }
        return it;
    }

    @EventHandler
    public void onShopClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (!title.startsWith("§2§lCửa Hàng") && !title.startsWith("§2§lShop Khối") && !title.startsWith("§5§lShop:") && !title.startsWith("§b§lShop:") && !title.startsWith("§e§lShop:") && !title.equals("§1§lChọn Số Lượng Mua") && !title.equals("§4§lChợ Đen Chill MC (/ah)") && !title.startsWith("§3§lThành Viên Clan:") && !title.equals("§5§lPhần Thưởng & Tỷ Lệ Rương")) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();
                if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) return;
        if (title.startsWith("§3§lThành Viên Clan:") || title.equals("§5§lPhần Thưởng & Tỷ Lệ Rương")) return;

        if (title.equals("§4§lChợ Đen Chill MC (/ah)")) {
            int slot = e.getRawSlot();
            if (slot < 0 || slot >= ahItems.size()) return;
            String seller = ahSellers.get(slot);
            int price = ahPrices.get(slot);
            ItemStack item = ahItems.get(slot);
            if (seller.equals(p.getName())) {
                ahSellers.remove(slot); ahPrices.remove(slot); ahItems.remove(slot); saveAH();
                giveItems(p, item); sendCompactMsg(p, "§a§l[Chợ Đen] §fĐã thu hồi vật phẩm về túi đồ!"); openAH(p); return;
            }
            if (getMoney(p) < price) { sendCompactMsg(p, "§c§l[Chợ Đen] §fBạn không đủ §e" + price + "$§f!"); return; }
            ahSellers.remove(slot); ahPrices.remove(slot); ahItems.remove(slot); saveAH();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "scoreboard players remove " + p.getName() + " tien " + price);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "scoreboard players add " + seller + " tien " + price);
            giveItems(p, item);
            sendCompactMsg(p, "§a§l[Chợ Đen] §fMua thành công vật phẩm của §a" + seller + " §f(-§e" + price + "$§f)!");
            Player sp = Bukkit.getPlayerExact(seller);
            if (sp != null && sp.isOnline()) sendCompactMsg(sp, "§a§l[Chợ Đen] §a" + p.getName() + " §fđã mua đồ của bạn trên /ah (§e+" + price + "$§f)!");
            openAH(p); return;
        }

        if (title.equals("§2§lCửa Hàng Chill MC")) {
            if (clicked.getType() == Material.GRASS_BLOCK) openBlocksPage(p, 1);
            else if (clicked.getType() == Material.ENDER_PEARL) openRedstoneBrewShop(p);
            else if (clicked.getType() == Material.DIAMOND_SWORD) openGearShop(p);
            else if (clicked.getType() == Material.DIAMOND_PICKAXE) openToolsShop(p);
            return;
        }
        if (title.equals("§1§lChọn Số Lượng Mua")) {
            UUID id = p.getUniqueId(); Material mat = buyMat.getOrDefault(id, Material.STONE); int cur = buyAmt.getOrDefault(id, 1); String prev = lastPage.getOrDefault(id, "§2§lCửa Hàng Chill MC");
            if (clicked.getType() == Material.ARROW) { reopenPrevPage(p, prev); return; }
            if (clicked.getType() == Material.RED_STAINED_GLASS_PANE || clicked.getType() == Material.LIME_STAINED_GLASS_PANE) {
                int delta = Integer.parseInt(clicked.getItemMeta().getDisplayName().replace("§c§l", "").replace("§a§l", "").replace("+", ""));
                openBuyAmountGUI(p, mat, cur + delta, prev); return;
            }
            if (clicked.getType() == Material.EMERALD_BLOCK) {
                int total = getUnitPrice(mat) * cur;
                if (getMoney(p) < total) { sendCompactMsg(p, "§c§l[Chill MC] §fKhông đủ tiền! Cần §e" + total + "$"); return; }
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "scoreboard players remove " + p.getName() + " tien " + total);
                giveItems(p, new ItemStack(mat, cur)); sendCompactMsg(p, "§a§l[Chill MC] §fĐã mua §e" + cur + "x " + mat.name() + " §f(-§e" + total + "$§f)!"); return;
            }
            return;
        }
        if (clicked.getType() == Material.ARROW) { openMainShop(p); return; }
        if (clicked.getType() == Material.PAPER && title.startsWith("§2§lShop Khối - Trang ")) {
            int curPage = Integer.parseInt(title.replace("§2§lShop Khối - Trang ", "").split("/")[0]);
            if (e.getSlot() == 45) openBlocksPage(p, curPage - 1); else if (e.getSlot() == 53) openBlocksPage(p, curPage + 1); return;
        }
        if (title.equals("§b§lShop: Trang Bị & Bộ Giáp") && (clicked.getType() == Material.IRON_CHESTPLATE || clicked.getType() == Material.DIAMOND_CHESTPLATE)) {
            int price = clicked.getType() == Material.IRON_CHESTPLATE ? 400 : 1500;
            if (getMoney(p) < price) { sendCompactMsg(p, "§c§l[Chill MC] §fKhông đủ tiền!"); return; }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "scoreboard players remove " + p.getName() + " tien " + price);
            if (clicked.getType() == Material.IRON_CHESTPLATE) giveItems(p, new ItemStack(Material.IRON_HELMET), new ItemStack(Material.IRON_CHESTPLATE), new ItemStack(Material.IRON_LEGGINGS), new ItemStack(Material.IRON_BOOTS));
            else giveItems(p, new ItemStack(Material.DIAMOND_HELMET), new ItemStack(Material.DIAMOND_CHESTPLATE), new ItemStack(Material.DIAMOND_LEGGINGS), new ItemStack(Material.DIAMOND_BOOTS));
            sendCompactMsg(p, "§a§l[Chill MC] §fMua trọn bộ giáp thành công (-§e" + price + "$§f)!"); return;
        }
        openBuyAmountGUI(p, clicked.getType(), 1, title);
    }

    private void reopenPrevPage(Player p, String prev) {
        if (prev.startsWith("§2§lShop Khối - Trang ")) openBlocksPage(p, Integer.parseInt(prev.replace("§2§lShop Khối - Trang ", "").split("/")[0]));
        else if (prev.equals("§5§lShop: Đá Đỏ & Chế Thuốc")) openRedstoneBrewShop(p);
        else if (prev.equals("§b§lShop: Trang Bị & Bộ Giáp")) openGearShop(p);
        else if (prev.equals("§e§lShop: Dụng Cụ")) openToolsShop(p);
        else openMainShop(p);
    }

    private void giveItems(Player p, ItemStack... items) { for (ItemStack it : items) p.getInventory().addItem(it).values().forEach(d -> p.getWorld().dropItem(p.getLocation(), d)); }

    private int getUnitPrice(Material m) {
        String n = m.name();
        if (n.endsWith("_PLANKS") || n.equals("DIRT") || n.equals("COBBLESTONE") || n.equals("SAND") || n.equals("GRAVEL") || n.equals("NETHERRACK") || n.equals("GLASS_BOTTLE")) return 2;
        if (n.endsWith("_LOG") || (n.endsWith("_BLOCK") && !n.equals("REDSTONE_BLOCK")) || n.endsWith("_CONCRETE") || n.endsWith("_WOOL") || n.endsWith("_GLASS") || n.endsWith("_BRICKS")) return 4;
        return switch (m) {
            case TOTEM_OF_UNDYING -> 500;
            case GRASS_BLOCK, STONE, SMOOTH_STONE, TERRACOTTA, WHITE_TERRACOTTA, SOUL_SAND, SOUL_SOIL, BASALT, POLISHED_BASALT, BLACKSTONE -> 3;
            case GLOWSTONE, SEA_LANTERN, SHROOMLIGHT, MAGMA_BLOCK, END_STONE, OBSIDIAN, CRYING_OBSIDIAN -> 10;
            case REDSTONE, REDSTONE_TORCH, LEVER, STONE_BUTTON, GLOWSTONE_DUST, GUNPOWDER -> 5;
            case REPEATER, COMPARATOR, PISTON, DISPENSER, DROPPER, OBSERVER, ENDER_PEARL -> 30;
            case STICKY_PISTON, HOPPER, TNT -> 45; case REDSTONE_BLOCK -> 40; case BREWING_STAND, CAULDRON -> 120;
            case NETHER_WART, BLAZE_POWDER, MAGMA_CREAM, FERMENTED_SPIDER_EYE, SPIDER_EYE, GLISTERING_MELON_SLICE, GOLDEN_CARROT -> 15;
            case BLAZE_ROD, GHAST_TEAR, RABBIT_FOOT, PHANTOM_MEMBRANE -> 60;
            case IRON_INGOT -> 20; case DIAMOND -> 130; case GOLDEN_APPLE -> 200; case IRON_SWORD -> 120; case DIAMOND_SWORD -> 450;
            case STONE_PICKAXE, STONE_AXE -> 30; case STONE_SHOVEL -> 20; case IRON_PICKAXE, IRON_AXE -> 120; case IRON_SHOVEL -> 80;
            case DIAMOND_PICKAXE, DIAMOND_AXE -> 500; case DIAMOND_SHOVEL -> 300; default -> 5;
        };
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!e.getView().getTitle().equals("§6§lBán Đồ Lấy Tiền")) return;
        if (!(e.getPlayer() instanceof Player)) return;
        Player p = (Player) e.getPlayer(); int total = 0;
        for (ItemStack item : e.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            int price = getSellPrice(item.getType());
            if (price > 0) total += price * item.getAmount(); else p.getInventory().addItem(item).values().forEach(d -> p.getWorld().dropItem(p.getLocation(), d));
        }
        if (total > 0) { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "scoreboard players add " + p.getName() + " tien " + total); sendCompactMsg(p, "§a§l[Chill MC] §fBạn đã bán đồ và nhận được §e+" + total + "$§f!"); }
    }


    private boolean hasUsedCode(String u, String c) { File f = new File(getDataFolder(), "used_codes.yml"); return f.exists() && YamlConfiguration.loadConfiguration(f).getBoolean(c.toLowerCase() + "." + u.toLowerCase(), false); }
    private void markUsedCode(String u, String c) { try { if (!getDataFolder().exists()) getDataFolder().mkdirs(); File f = new File(getDataFolder(), "used_codes.yml"); YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f); cfg.set(c.toLowerCase() + "." + u.toLowerCase(), true); cfg.save(f); } catch (Exception ignored) {} }

    private YamlConfiguration getHomeCfg() { File f = new File(getDataFolder(), "homes.yml"); return f.exists() ? YamlConfiguration.loadConfiguration(f) : new YamlConfiguration(); }
    private void saveHomeCfg(YamlConfiguration cfg) { try { if (!getDataFolder().exists()) getDataFolder().mkdirs(); cfg.save(new File(getDataFolder(), "homes.yml")); } catch (Exception ignored) {} }


    private void safeTp(Player p, org.bukkit.Location loc) {
        int cx = loc.getBlockX() >> 4, cz = loc.getBlockZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) loc.getWorld().getChunkAt(cx + dx, cz + dz).load(true);
        safeTp(p, loc);
        p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.DAMAGE_RESISTANCE, 60, 255));
        p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOW_FALLING, 60, 0));
    }


    private YamlConfiguration getClanCfg() { File f = new File(getDataFolder(), "clans.yml"); return f.exists() ? YamlConfiguration.loadConfiguration(f) : new YamlConfiguration(); }
    private void saveClanCfg(YamlConfiguration cfg) { try { if (!getDataFolder().exists()) getDataFolder().mkdirs(); cfg.save(new File(getDataFolder(), "clans.yml")); } catch (Exception ignored) {} }
    private String getPlayerClan(String name) { return getClanCfg().getString("player." + name.toLowerCase(), ""); }

    private int getMoney(Player p) { Objective obj = Bukkit.getScoreboardManager().getMainScoreboard().getObjective("tien"); return obj == null ? 0 : obj.getScore(p.getName()).getScore(); }

    private int getSellPrice(Material m) {
        String name = m.name();
        if (name.endsWith("_LOG") || name.endsWith("_WOOD")) return 4;
        if (name.endsWith("_PLANKS")) return 1;
        return switch (m) {
            case TOTEM_OF_UNDYING -> 500;
            case COBBLESTONE, COBBLED_DEEPSLATE, DIRT, SAND, GRAVEL, NETHERRACK -> 1; case STONE, DEEPSLATE, ANDESITE, DIORITE, GRANITE -> 2;
            case WHEAT, CARROT, POTATO, BEETROOT, SUGAR_CANE, MELON_SLICE, PUMPKIN -> 3; case ROTTEN_FLESH, STRING, SPIDER_EYE -> 2;
            case BONE, ARROW, FEATHER, LEATHER -> 4; case COAL, CHARCOAL -> 5; case GUNPOWDER, SLIME_BALL, ENDER_PEARL -> 8;
            case RAW_COPPER, COPPER_INGOT -> 6; case RAW_IRON -> 12; case IRON_INGOT -> 15; case RAW_GOLD -> 20; case GOLD_INGOT -> 25;
            case LAPIS_LAZULI, REDSTONE -> 5; case QUARTZ, AMETHYST_SHARD -> 8; case DIAMOND -> 100; case EMERALD -> 150;
            case ANCIENT_DEBRIS -> 300; case NETHERITE_INGOT -> 1200; default -> 0;
        };
    }

    private YamlConfiguration getPassCfg() { File f = new File(getDataFolder(), "passwords.yml"); return f.exists() ? YamlConfiguration.loadConfiguration(f) : new YamlConfiguration(); }
    private void savePassCfg(YamlConfiguration cfg) { try { if (!getDataFolder().exists()) getDataFolder().mkdirs(); cfg.save(new File(getDataFolder(), "passwords.yml")); } catch (Exception ignored) {} }

    @EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent e) {
        Player p = e.getPlayer(); unlogged.add(p.getUniqueId()); File sf = new File(getDataFolder(), "spawn.yml"); org.bukkit.Location sl = sf.exists() ? YamlConfiguration.loadConfiguration(sf).getLocation("spawn") : null; if (sl == null || sl.getWorld() == null) sl = p.getWorld().getSpawnLocation(); p.teleport(sl);
        String key = p.getName().toLowerCase(); String sp = getPassCfg().getString("server_global_pass", "");
        if (!sp.isEmpty()) sendCompactMsg(p, "§c§l[Bảo Mật Sever] §fSever đang khóa! Gõ §a/login <mật_khẩu> §fđể vào chơi & mở Menu Chọn Chế Độ.");
        else if (getPassCfg().contains(key)) sendCompactMsg(p, "§e§l[Bảo Mật] §fTài khoản đã đăng ký! Gõ §a/login <mật_khẩu> §fđể vào chơi & mở Menu Chọn Chế Độ.");
        else sendCompactMsg(p, "§c§l[Bảo Mật] §fTài khoản mới! Gõ §a/register <mật_khẩu> <nhập_lại> §fđể bảo vệ nick & mở Menu Chọn Chế Độ.");
    }
    @EventHandler
    public void onMove(org.bukkit.event.player.PlayerMoveEvent e) {
        if (unlogged.contains(e.getPlayer().getUniqueId()) && (e.getFrom().getBlockX() != e.getTo().getBlockX() || e.getFrom().getBlockZ() != e.getTo().getBlockZ())) e.setTo(e.getFrom());
    }
    @EventHandler
    public void onPreCmd(org.bukkit.event.player.PlayerCommandPreprocessEvent e) {
        if (!unlogged.contains(e.getPlayer().getUniqueId())) return;
        String m = e.getMessage().toLowerCase();
        if (!m.startsWith("/login ") && !m.startsWith("/register ")) { e.setCancelled(true); e.getPlayer().sendMessage("§c§l[Bảo Mật] §fVui lòng §a/login §fhoặc §a/register §ftrước!"); }
    }
    @EventHandler
    public void onChat(org.bukkit.event.player.AsyncPlayerChatEvent e) { if (unlogged.contains(e.getPlayer().getUniqueId())) { e.setCancelled(true); e.getPlayer().sendMessage("§c§l[Bảo Mật] §fVui lòng đăng nhập trước khi chat!"); } }
    @EventHandler
    public void onBreak(org.bukkit.event.block.BlockBreakEvent e) { if (unlogged.contains(e.getPlayer().getUniqueId())) e.setCancelled(true); }
    @EventHandler
    public void onPlace(org.bukkit.event.block.BlockPlaceEvent e) { if (unlogged.contains(e.getPlayer().getUniqueId())) e.setCancelled(true); }

    private int getRewardCount(String name) {
        File f = new File(getDataFolder(), "rewards.yml");
        return f.exists() ? YamlConfiguration.loadConfiguration(f).getInt(name.toLowerCase(), 0) : 0;
    }
    private void addRewardCount(String name) {
        try {
            if (!getDataFolder().exists()) getDataFolder().mkdirs();
            File f = new File(getDataFolder(), "rewards.yml");
            YamlConfiguration cfg = f.exists() ? YamlConfiguration.loadConfiguration(f) : new YamlConfiguration();
            cfg.set(name.toLowerCase(), cfg.getInt(name.toLowerCase(), 0) + 1);
            cfg.save(f);
        } catch (Exception ignored) {}
    }

    private final int[] PRIZE_VAL = {250, 500, 1000, 2000, 3500, 5000, 7500, 10000, 15000, 20000, 30000, 50000, 100000, 250000, 500000};
    private final int[] PRIZE_W = {200, 180, 150, 120, 90, 70, 50, 40, 30, 22, 16, 12, 10, 6, 4};

    private ItemStack makeKey(int amt) {
        ItemStack it = new ItemStack(Material.SUNFLOWER, amt);
        ItemMeta m = it.getItemMeta(); m.setDisplayName("§6§lKey Reward");
        m.addEnchant(org.bukkit.enchantments.Enchantment.MENDING, 1, true);
        m.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        m.setLore(Arrays.asList("§7Bấm §eChuột Phải §7vào Rương ở Spawn để quay!", "§7Bấm §aChuột Trái §7vào Rương để xem tỷ lệ."));
        it.setItemMeta(m); return it;
    }

    private ItemStack makeVoucher(int idx) {
        ItemStack it = new ItemStack(Material.PAPER, 1);
        ItemMeta m = it.getItemMeta(); m.setDisplayName("§e§lPhiếu Tiền: §a§l" + PRIZE_VAL[idx] + "$");
        m.setLore(Arrays.asList("§7Tỷ lệ trúng: §b" + (PRIZE_W[idx] / 10.0) + "%", "§eBấm Chuột Phải để nhận ngay tiền!"));
        it.setItemMeta(m); return it;
    }

    private void openCratePreview(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, "§5§lPhần Thưởng & Tỷ Lệ Rương");
        for (int i = 0; i < 15; i++) inv.setItem(i + 1, makeVoucher(i));
        p.openInventory(inv);
    }

    @EventHandler
    public void onCrateInteract(org.bukkit.event.player.PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (e.getItem() != null && e.getItem().getType() == Material.PAPER && e.getItem().hasItemMeta()) {
            String dn = e.getItem().getItemMeta().getDisplayName();
            if (dn.startsWith("§e§lPhiếu Tiền: §a§l") && (e.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR || e.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)) {
                e.setCancelled(true);
                int val = Integer.parseInt(dn.replace("§e§lPhiếu Tiền: §a§l", "").replace("$", ""));
                e.getItem().setAmount(e.getItem().getAmount() - 1);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "scoreboard players add " + p.getName() + " tien " + val);
                sendCompactMsg(p, "§a§l[Chill MC] §fBạn đã sử dụng phiếu và nhận được §e+" + val + "$§f!");
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f); return;
            }
        }
        if (e.getClickedBlock() == null || e.getClickedBlock().getType() != Material.CHEST) return;
        File f = new File(getDataFolder(), "crate.yml"); if (!f.exists()) return;
        org.bukkit.Location cloc = YamlConfiguration.loadConfiguration(f).getLocation("crate");
        if (cloc == null || !cloc.getWorld().equals(e.getClickedBlock().getWorld()) || cloc.distanceSquared(e.getClickedBlock().getLocation()) > 0.5) return;
        e.setCancelled(true);
        if (e.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) { openCratePreview(p); return; }
        if (e.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() != Material.SUNFLOWER || !hand.hasItemMeta() || !hand.getItemMeta().getDisplayName().equals("§6§lKey Reward")) {
                org.bukkit.util.Vector push = p.getLocation().toVector().subtract(e.getClickedBlock().getLocation().add(0.5, 0.5, 0.5).toVector()); push.setY(0); if (push.lengthSquared() < 0.01) push = p.getLocation().getDirection().multiply(-1).setY(0); p.setVelocity(push.normalize().multiply(1.15).setY(0.35)); p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.8f); sendCompactMsg(p, "§c§l[Rương Thưởng] §fBạn cần cầm §6Key Reward §fđể mở!"); return;
            }
            hand.setAmount(hand.getAmount() - 1);
            int roll = new Random().nextInt(1000), sum = 0, win = 0;
            for (int i = 0; i < 15; i++) { sum += PRIZE_W[i]; if (roll < sum) { win = i; break; } }
            giveItems(p, makeVoucher(win)); addRewardCount(p.getName());
            sendCompactMsg(p, "§6§l[Rương Thưởng] §fChúc mừng! Bạn quay trúng §ePhiếu Tiền " + PRIZE_VAL[win] + "$ §7(Tỷ lệ " + (PRIZE_W[win] / 10.0) + "%)!");
            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        }
    }

    private String getRewardTimer(String name) {
        File f = new File(getDataFolder(), "rewards.yml");
        if (!f.exists()) return "§aSẵn sàng!";
        long last = YamlConfiguration.loadConfiguration(f).getLong("claim_time." + name.toLowerCase(), 0L);
        long diff = 86400000L - (System.currentTimeMillis() - last);
        if (diff <= 0L) return "§aSẵn sàng!";
        long s = diff / 1000L;
        return String.format("§e%02d:%02d:%02d", s / 3600L, (s % 3600L) / 60L, s % 60L);
    }

    
    private boolean isInSpawn(org.bukkit.Location loc) {
        if (loc == null || loc.getWorld() == null || !loc.getWorld().getName().equalsIgnoreCase("world")) return false;
        org.bukkit.Location sp = loc.getWorld().getSpawnLocation();
        return Math.abs(loc.getBlockX() - (-197)) <= 60 && Math.abs(loc.getBlockZ() - 34) <= 60;
    }

    @EventHandler
    public void onSpawnBreak(org.bukkit.event.block.BlockBreakEvent e) {
        if (!e.getPlayer().isOp() && isInSpawn(e.getBlock().getLocation())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§c§l[Bảo Vệ] §fBạn không thể đập block tại khu vực Spawn!");
        }
    }

    @EventHandler
    public void onSpawnPlace(org.bukkit.event.block.BlockPlaceEvent e) {
        if (!e.getPlayer().isOp() && isInSpawn(e.getBlock().getLocation())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§c§l[Bảo Vệ] §fBạn không thể đặt block tại khu vực Spawn!");
        }
    }

    @EventHandler
    public void onSpawnExplode(org.bukkit.event.entity.EntityExplodeEvent e) {
        java.util.Iterator<org.bukkit.block.Block> it = e.blockList().iterator();
        while (it.hasNext()) {
            if (isInSpawn(it.next().getLocation())) it.remove();
        }
    }

    @EventHandler
    public void onDamageVillager(org.bukkit.event.entity.EntityDamageEvent e) {
        if (e.getEntity() instanceof org.bukkit.entity.Villager) {
            if (e instanceof org.bukkit.event.entity.EntityDamageByEntityEvent) {
                org.bukkit.entity.Entity dmg = ((org.bukkit.event.entity.EntityDamageByEntityEvent) e).getDamager();
                if (dmg instanceof Player && ((Player) dmg).isOp()) return;
            }
            e.setCancelled(true);
        }
    }

    private void openHelpGui(Player p) {
        org.bukkit.inventory.Inventory inv = Bukkit.createInventory(null, 27, "§b§lCHILL MC - Huong Dan");
        org.bukkit.inventory.ItemStack g = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GRAY_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta gm = g.getItemMeta(); gm.setDisplayName("§7 "); g.setItemMeta(gm);
        for (int i : java.util.stream.IntStream.range(0, 27).toArray()) inv.setItem(i, g);
        inv.setItem(4, mkHelp(org.bukkit.Material.BEACON, "§b§lCHAO MUNG DEN CHILL MC", "§7Sinh Ton & Kinh Te [1.8 - 1.20]", "§dGõ /helpchillmc hoac /help chill mc"));
        inv.setItem(10, mkHelp(org.bukkit.Material.EMERALD, "§a§lKINH TE & SHOP", "§e/shop §f- Mo cua hang", "§e/sellgui §f- Ban do lay tien", "§e/ah §f- Cho den treo ban", "§e/balance & /pay §f- Xem & chuyen tien"));
        inv.setItem(12, mkHelp(org.bukkit.Material.COMPASS, "§e§lDI CHUYEN & SINH TON", "§e/rtp §f- Dich chuyen ngau nhien", "§e/spawn §f- Quay ve sanh Spawn", "§e/sethome & /home §f- Luu nha"));
        inv.setItem(14, mkHelp(org.bukkit.Material.ENDER_CHEST, "§d§lTIEN ICH & KHOE DO", "§e/ec §f- Mo Ruong Ender ca nhan", "§e[i] §f- Khoe vat pham dang cam", "§e[ec] §f- Khoe Ruong Ender len chat", "§e/tieng (/lang) §f- Doi ngon ngu Viet/Anh"));
        inv.setItem(16, mkHelp(org.bukkit.Material.DIAMOND_SWORD, "§c§lDAU TRUONG PVP & CLAN", "§e/pvp §f- Den ngay Dau Truong PvP 1v1", "§7Hoac ra ngoai vach do Spawn de PvP", "§e/clan §f- Tao & quan ly Bang Hoi"));
        inv.setItem(22, mkHelp(org.bukkit.Material.TRIPWIRE_HOOK, "§6§lVONG QUAY & NHIEM VU", "§e/ruttiennhankey <tien> §7(/getkey)", "§7Chuot trai ruong xem ti le, chuot phai quay", "§e/nhiemvu §7(/quest) §f- Nhiem vu cay cuoc", "§e/diemdanh §7(/daily) §f- Nhan 500$/ngay"));
        p.openInventory(inv);
    }
    private org.bukkit.inventory.ItemStack mkHelp(org.bukkit.Material m, String n, String... l) {
        org.bukkit.inventory.ItemStack it = new org.bukkit.inventory.ItemStack(m);
        org.bukkit.inventory.meta.ItemMeta mt = it.getItemMeta(); mt.setDisplayName(n);
        mt.setLore(java.util.Arrays.asList(l)); it.setItemMeta(mt); return it;
    }
    @EventHandler
    public void onHelpClick(org.bukkit.event.inventory.InventoryClickEvent e) {
        if (e.getView().getTitle().contains("CHILL MC - Huong Dan")) e.setCancelled(true);
    }

    private java.util.Map authIp = new java.util.HashMap();
    private java.util.Map authTime = new java.util.HashMap();

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onWardenSessionJoin(org.bukkit.event.player.PlayerJoinEvent e) {
        final Player p = e.getPlayer();
        final String key = p.getName().toLowerCase();
        final String ip = (p.getAddress() != null && p.getAddress().getAddress() != null) ? p.getAddress().getAddress().getHostAddress() : "";
        long now = System.currentTimeMillis();
        long lastT = authTime.containsKey(key) ? ((Long) authTime.get(key)).longValue() : 0L;

        if (!ip.isEmpty() && ip.equals(authIp.get(key)) && !(now - lastT >= 86400000L)) {
            try {
                java.lang.reflect.Field f = getClass().getDeclaredField("unlogged");
                f.setAccessible(true);
                Object setObj = f.get(this);
                if (setObj instanceof java.util.Set) ((java.util.Set) setObj).add(p.getUniqueId());
            } catch (Exception ignored) {}
            p.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
            
        } else {
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 999999, 2, false, false));
        }
    }

    @EventHandler
    public void onClearWardenAuth(org.bukkit.event.player.PlayerCommandPreprocessEvent e) {
        String m = e.getMessage().toLowerCase();
        if (m.startsWith("/login ") || m.startsWith("/l ") || m.startsWith("/register ") || m.startsWith("/reg ")) {
            final Player p = e.getPlayer();
            Bukkit.getScheduler().runTaskLater(this, new Runnable() {
                public void run() {
                    if (!p.isOnline()) return;
                    p.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
                    String ip = (p.getAddress() != null && p.getAddress().getAddress() != null) ? p.getAddress().getAddress().getHostAddress() : "";
                    if (!ip.isEmpty()) {
                        authIp.put(p.getName().toLowerCase(), ip);
                        authTime.put(p.getName().toLowerCase(), Long.valueOf(System.currentTimeMillis()));
                    }
                }
            }, 5L);
        }
    }

    
    private void openClanHierarchyGui(Player v, String q) {
        File cf = new File(getDataFolder(), "clans.yml"); YamlConfiguration cc = YamlConfiguration.loadConfiguration(cf);
        String pfx = ""; if (pfx.isEmpty() && cc.isConfigurationSection("clans")) pfx = "clans.";
        String cName = null; org.bukkit.configuration.ConfigurationSection sec = pfx.isEmpty() ? cc : cc.getConfigurationSection("clans");
        if (sec != null) for (String k : sec.getKeys(false)) if (k.equalsIgnoreCase(q)) { cName = k; break; }
        if (cName == null) { sendCompactMsg(v, "§cKhong tim thay Clan: §e" + q); return; }
        org.bukkit.inventory.Inventory inv = Bukkit.createInventory(null, 27, "§6§lClan: " + cName);
        int lvl = cc.getInt(pfx+cName+".level", 1), maxM = (lvl == 1) ? 5 : ((lvl == 2) ? 10 : 20);
        String ld = cc.getString(pfx+cName+".leader", cc.getString(pfx+cName+".owner", "Unknown"));
        java.util.List off = cc.getStringList(pfx+cName+".officers"), mbs = cc.getStringList(pfx+cName+".members");
        if (!mbs.contains(ld)) mbs.add(0, ld);
        org.bukkit.inventory.ItemStack pot = new org.bukkit.inventory.ItemStack(org.bukkit.Material.POTION);
        org.bukkit.inventory.meta.ItemMeta pm = pot.getItemMeta(); pm.setDisplayName("§b§lCAP DO CLAN: §e" + lvl + " §7/ §c3 (MAX)");
        pm.setLore(java.util.Arrays.asList("§fClan: §6" + cName, "§fThanh vien: §a" + mbs.size() + " / " + maxM, "§7Go §e/helpclan §7de xem lenh!")); pot.setItemMeta(pm); inv.setItem(4, pot);
        org.bukkit.inventory.ItemStack bar = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BLACK_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta bm = bar.getItemMeta(); bm.setDisplayName("§8§m----------------"); bar.setItemMeta(bm);
        for (int i : java.util.stream.IntStream.range(9, 18).toArray()) inv.setItem(i, bar);
        java.util.List ord = new java.util.ArrayList(), rol = new java.util.ArrayList();
        ord.add(ld); rol.add("§c§l[CHU CLAN]");
        for (Object o : off) if (!String.valueOf(o).equalsIgnoreCase(ld) && !ord.contains(String.valueOf(o))) { ord.add(String.valueOf(o)); rol.add("§6§l[PHO CLAN]"); }
        for (Object m : mbs) if (!String.valueOf(m).equalsIgnoreCase(ld) && !ord.contains(String.valueOf(m))) { ord.add(String.valueOf(m)); rol.add("§a[THANH VIEN]"); }
        int s = 18;
        for (int i : java.util.stream.IntStream.range(0, ord.size()).toArray()) {
            if (s == 27) break;
            org.bukkit.inventory.ItemStack h = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PLAYER_HEAD);
            org.bukkit.inventory.meta.SkullMeta sm = (org.bukkit.inventory.meta.SkullMeta) h.getItemMeta(); sm.setOwner(String.valueOf(ord.get(i)));
            sm.setDisplayName(rol.get(i) + " §f" + ord.get(i)); h.setItemMeta(sm); inv.setItem(s++, h);
        }
        v.openInventory(inv);
    }
    @EventHandler public void onClanViewClick(org.bukkit.event.inventory.InventoryClickEvent e) { if (e.getView().getTitle().startsWith("§6§lClan: ")) e.setCancelled(true); }

    private java.util.Set lobbyAuthed = new java.util.HashSet();
    private java.util.Set step3Locked = new java.util.HashSet();
    private org.bukkit.World getOrCreateLobbyWorld() {
        org.bukkit.World lw = Bukkit.getWorld("world_lobby");
        if (lw == null) {
            org.bukkit.WorldCreator wc = new org.bukkit.WorldCreator("world_lobby");
            wc.type(org.bukkit.WorldType.FLAT); wc.generateStructures(false); lw = wc.createWorld();
            if (lw != null) {
                lw.setGameRuleValue("doMobSpawning", "false"); lw.setGameRuleValue("doDaylightCycle", "false"); lw.setTime(6000L);

                lw.setSpawnLocation(0, 65, 0);
            }
        }
        return lw;
    }
    private org.bukkit.configuration.file.FileConfiguration getLobbyInvCfg() {
        java.io.File f = new java.io.File(getDataFolder(), "lobby_inv_backup.yml");
        if (!f.exists()) { try { getDataFolder().mkdirs(); f.createNewFile(); } catch (Exception ignored) {} }
        return org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
    }
    private void saveSurvivalInvIfNeeded(Player p) {
        if (p.getWorld().getName().equals("world_lobby")) return;
        org.bukkit.configuration.file.FileConfiguration cfg = getLobbyInvCfg();
        String uid = p.getUniqueId().toString();
        cfg.set(uid + ".saved", true); cfg.set(uid + ".inv", p.getInventory().getContents()); cfg.set(uid + ".armor", p.getInventory().getArmorContents());
        try { cfg.save(new java.io.File(getDataFolder(), "lobby_inv_backup.yml")); } catch (Exception ignored) {}
    }
    private void giveLobbyCompass(Player p) {
        saveSurvivalInvIfNeeded(p); p.getInventory().clear(); p.getInventory().setArmorContents(null);
        org.bukkit.inventory.ItemStack comp = new org.bukkit.inventory.ItemStack(org.bukkit.Material.COMPASS);
        org.bukkit.inventory.meta.ItemMeta cm = comp.getItemMeta();
        cm.setDisplayName("§a§lCHỌN CHẾ ĐỘ CHƠI §7(Bấm Chuột Phải)");
        comp.setItemMeta(cm); p.getInventory().setItem(4, comp);
    }
    private void restoreLobbySlot4(Player p) {
        p.getInventory().clear(); p.getInventory().setArmorContents(null);
        org.bukkit.configuration.file.FileConfiguration cfg = getLobbyInvCfg(); String uid = p.getUniqueId().toString();
        if (cfg.contains(uid + ".saved")) {
            java.util.List invL = cfg.getList(uid + ".inv");
            if (invL != null) for (int i : java.util.stream.IntStream.range(0, invL.size()).toArray()) if (invL.get(i) instanceof org.bukkit.inventory.ItemStack) p.getInventory().setItem(i, (org.bukkit.inventory.ItemStack) invL.get(i));
            java.util.List armL = cfg.getList(uid + ".armor");
            if (armL != null) {
                org.bukkit.inventory.ItemStack[] a = p.getInventory().getArmorContents();
                for (int i : java.util.stream.IntStream.range(0, java.lang.Math.min(armL.size(), a.length)).toArray()) if (armL.get(i) instanceof org.bukkit.inventory.ItemStack) java.lang.reflect.Array.set(a, i, armL.get(i));
                p.getInventory().setArmorContents(a);
            }
            cfg.set(uid, null); try { cfg.save(new java.io.File(getDataFolder(), "lobby_inv_backup.yml")); } catch (Exception ignored) {}
        }
    }

    @EventHandler
    public void onQuitRestoreSlot4(org.bukkit.event.player.PlayerQuitEvent e) {
        if (e.getPlayer().getWorld().getName().equals("world_lobby")) restoreLobbySlot4(e.getPlayer());
    }
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onAntiKickDuplicate(org.bukkit.event.player.AsyncPlayerPreLoginEvent e) {
        String inc = e.getName();
        for (Player on : Bukkit.getOnlinePlayers()) {
            if (on.getName().equalsIgnoreCase(inc)) {
                e.disallow(org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "§c§l[CHILL MC] §fTài khoản §e" + inc + " §fđang trực tuyến!");
                return;
            }
        }
    }
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onTwoMapJoin(org.bukkit.event.player.PlayerJoinEvent e) {
        Player jp = e.getPlayer(); lobbyAuthed.remove(jp.getUniqueId()); step3Locked.remove(jp.getUniqueId()); unlogged.add(jp.getUniqueId()); if (!jp.getWorld().getName().equals("world_lobby") && !jp.getInventory().contains(org.bukkit.Material.COMPASS)) saveSurvivalInvIfNeeded(jp);
        org.bukkit.World lw = getOrCreateLobbyWorld();
        if (lw != null) {

            org.bukkit.Location sp = lw.getSpawnLocation(); jp.teleport((sp.getY() > 0) ? sp : new org.bukkit.Location(lw, 0.5, 65.0, 0.5));
        }
        giveLobbyCompass(jp);
        jp.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 999999, 2, false, false));
    }
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onTwoMapLoginIntercept(org.bukkit.event.player.PlayerCommandPreprocessEvent e) {
        if (e.isCancelled()) return;
        String low = e.getMessage().trim().toLowerCase();
        if (low.startsWith("/login ") || low.startsWith("/l ") || low.startsWith("/register ") || low.startsWith("/reg ")) {
            final Player lp = e.getPlayer();
            Bukkit.getScheduler().runTaskLater(this, new Runnable() {
                public void run() {
                    if (!lp.isOnline()) return;
                    if (step3Locked.contains(lp.getUniqueId()) && !lp.getWorld().getName().equals("world_lobby")) { step3Locked.remove(lp.getUniqueId()); try { java.lang.reflect.Field f2 = getClass().getDeclaredField("unlogged"); f2.setAccessible(true); ((java.util.Set) f2.get(this)).remove(lp.getUniqueId()); } catch (Exception ignored) {} if (!lp.isOp() && lp.getGameMode() != org.bukkit.GameMode.CREATIVE) { Bukkit.getScheduler().runTaskLater(Main.this, () -> { if (lp.isOnline() && !lp.getWorld().getName().equals("world_lobby") && !lp.isOp() && lp.getGameMode() != org.bukkit.GameMode.CREATIVE) lp.setGameMode(org.bukkit.GameMode.SURVIVAL); }, 2L); } if (unlogged.contains(lp.getUniqueId()) || lp.hasPotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS)) return; sendCompactMsg(lp, "§a§l[Bảo Mật] §fXác thực Bước 3 thành công! Chúc bạn chơi vui vẻ."); return; }
                    if (!lobbyAuthed.contains(lp.getUniqueId()) && lp.getWorld().getName().equals("world_lobby")) {
                        if (!lp.hasPotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS)) {
                            lobbyAuthed.add(lp.getUniqueId());
                            giveLobbyCompass(lp);
                            openModeSelectorGui(lp);
                        }
                    }
                }
            }, 6L);
        }
    }

    private void openModeSelectorGui(Player p) {
        org.bukkit.inventory.Inventory inv = Bukkit.createInventory(null, 27, "§a§lCHỌN CHẾ ĐỘ CHƠI");
        org.bukkit.inventory.ItemStack grass = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GRASS_BLOCK);
        org.bukkit.inventory.meta.ItemMeta gm = grass.getItemMeta();
        gm.setDisplayName("§a§lCHILL MC - SINH TỒN");
        grass.setItemMeta(gm); inv.setItem(13, grass); p.openInventory(inv);
    }
    @EventHandler
    public void onModeSelectClick(org.bukkit.event.inventory.InventoryClickEvent e) {
        if (!e.getView().getTitle().equals("§a§lCHỌN CHẾ ĐỘ CHƠI")) return;
        e.setCancelled(true);
        if (e.getWhoClicked() instanceof Player && !lobbyAuthed.contains(e.getWhoClicked().getUniqueId())) { e.getWhoClicked().closeInventory(); return; }
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        if (e.getRawSlot() == 13) {
            try { java.lang.reflect.Field f = getClass().getDeclaredField("unlogged"); f.setAccessible(true); ((java.util.Set) f.get(this)).add(p.getUniqueId()); } catch (Exception ignored) {}
            for (int s = 0; s < 36; s++) {
                    org.bukkit.inventory.ItemStack it = p.getInventory().getItem(s);
                    if (it != null && it.getType() == org.bukkit.Material.COMPASS) p.getInventory().setItem(s, null);
                }
                p.closeInventory(); step3Locked.add(p.getUniqueId());
                try {
                    java.io.File invF = new java.io.File(getDataFolder(), "lobby_inv_" + p.getUniqueId() + ".yml");
                    if (invF.exists()) {
                        org.bukkit.configuration.file.YamlConfiguration icfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(invF);
                        p.getInventory().clear();
                        for (int s = 0; s < 41; s++) {
                            if (icfg.contains("s." + s)) p.getInventory().setItem(s, icfg.getItemStack("s." + s));
                        }
                        invF.delete();
                    } else {
                        restoreLobbySlot4(p);
                    }
                } catch (Exception ex) { restoreLobbySlot4(p); }
            org.bukkit.World mainW = Bukkit.getWorlds().get(0);
            p.teleport(new org.bukkit.Location(mainW, -196.5, 64.5, 34.5)); Bukkit.getScheduler().runTaskLater(this, () -> { if (p.isOnline()) p.teleport(new org.bukkit.Location(mainW, -196.5, 64.5, 34.5)); }, 2L);
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 999999, 2, false, false));
            sendCompactMsg(p, "§6§l[Bảo Mật Bước 3] §fĐã về Spawn Sinh Tồn! Gõ §e/login <mật_khẩu> §fđể mở khóa di chuyển.");
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onLobbyCompassUse(org.bukkit.event.player.PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (p.getWorld().getName().equals("world_lobby") && !lobbyAuthed.contains(p.getUniqueId())) { e.setCancelled(true); sendCompactMsg(p, "§c§l[Bảo Mật] §fVui lòng §e/register §fhoặc §e/login §ftrước!"); return; }
        if (!p.getWorld().getName().equals("world_lobby") || !lobbyAuthed.contains(p.getUniqueId())) return;
        org.bukkit.inventory.ItemStack it = e.getItem();
        if (it != null && it.getType() == org.bukkit.Material.COMPASS && it.hasItemMeta() && it.getItemMeta().hasDisplayName() && it.getItemMeta().getDisplayName().contains("CHỌN CHẾ ĐỘ CHƠI")) {
            e.setCancelled(true); openModeSelectorGui(p);
        }
    }
    @EventHandler
    public void onLobbyCompassDrop(org.bukkit.event.player.PlayerDropItemEvent e) {
        if (e.getPlayer().getWorld().getName().equals("world_lobby") && !e.getPlayer().isOp()) e.setCancelled(true);
    }
    @EventHandler
    public void onLobbyProtect(org.bukkit.event.block.BlockBreakEvent e) {
        if (e.getPlayer().getWorld().getName().equals("world_lobby") && !e.getPlayer().isOp()) e.setCancelled(true);
    }
    @EventHandler
    public void onLobbyPlace(org.bukkit.event.block.BlockPlaceEvent e) {
        if (e.getPlayer().getWorld().getName().equals("world_lobby") && !e.getPlayer().isOp()) e.setCancelled(true);
    }
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onSafeRespawn(org.bukkit.event.player.PlayerRespawnEvent e) {
        if (!e.isBedSpawn()) {
            org.bukkit.World mainW = Bukkit.getWorlds().get(0);
            e.setRespawnLocation(new org.bukkit.Location(mainW, -196.5, 64.5, 34.5));
        }
    }
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onSafeZonePvP(org.bukkit.event.entity.EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player victim = (Player) e.getEntity();
        if (victim.getWorld().getName().equals("world_lobby")) { e.setCancelled(true); return; }
        double dx = victim.getLocation().getX() + 196.5, dz = victim.getLocation().getZ() - 34.5;
        if (!(dx * dx + dz * dz >= 2500.0)) e.setCancelled(true);
    }
    private int clearTimer = 2000;
    private boolean clearStarted = false;
    private void startClearLag() {
        if (clearStarted) return; clearStarted = true;
        Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            public void run() {
                if (clearTimer == 500 || clearTimer == 250) Bukkit.broadcastMessage("§b§l[CHILLMC] §e" + clearTimer + " GIAY NUA XOA VAT PHAM TREN DAT");
                else if (clearTimer != 0 && !(clearTimer >= 11)) Bukkit.broadcastMessage("§b§l[CHILLMC] §c" + clearTimer + " §eGIAY NUA XOA VAT PHAM TREN DAT");
                else if (clearTimer == 0) {
                    int cnt = 0;
                    for (org.bukkit.World w : Bukkit.getWorlds()) for (org.bukkit.entity.Entity ent : w.getEntities()) if (ent instanceof org.bukkit.entity.Item) { ent.remove(); cnt++; }
                    Bukkit.broadcastMessage("§b§l[CHILLMC] §aVAT PHAM TREN DAT BI XOA (§e" + cnt + " §avat pham)!");
                    clearTimer = 2001;
                }
                clearTimer--;
            }
        }, 20L, 20L);
    }
    private java.util.Map lastMsgText = new java.util.HashMap(), lastMsgCount = new java.util.HashMap(), lastMsgHistory = new java.util.HashMap();
    private void sendCompactMsg(org.bukkit.command.CommandSender target, String msg) {
        if (!(target instanceof Player)) { target.sendMessage(msg); return; }
        Player pl = (Player) target; java.util.UUID id = pl.getUniqueId();
        String prev = lastMsgText.containsKey(id) ? String.valueOf(lastMsgText.get(id)) : "";
        java.util.List hist = lastMsgHistory.containsKey(id) ? (java.util.List) lastMsgHistory.get(id) : new java.util.ArrayList();
        if (msg.equals(prev)) {
            int cnt = ((Integer) lastMsgCount.get(id)).intValue() + 1;
            if (cnt == 5001) cnt = 1;
            lastMsgCount.put(id, Integer.valueOf(cnt));
            for (int i : java.util.stream.IntStream.range(0, 100).toArray()) pl.sendMessage(" ");
            for (int i : java.util.stream.IntStream.range(0, hist.size() - 1).toArray()) pl.sendMessage(String.valueOf(hist.get(i)));
            pl.sendMessage(msg + " §7§l(x" + cnt + ")");
        } else {
            lastMsgText.put(id, msg); lastMsgCount.put(id, Integer.valueOf(1)); hist.add(msg);
            if (hist.size() == 21) hist.remove(0);
            lastMsgHistory.put(id, hist); pl.sendMessage(msg);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onStep3MoveLock(org.bukkit.event.player.PlayerMoveEvent e) {
        Player mp = e.getPlayer();
        if (mp.getWorld().getName().equals("world_lobby") && !lobbyAuthed.contains(mp.getUniqueId())) {
            if (e.getFrom().getX() != e.getTo().getX() || e.getFrom().getZ() != e.getTo().getZ()) e.setTo(e.getFrom());
            return;
        }
        if (step3Locked.contains(mp.getUniqueId())) {
            org.bukkit.World mainW = Bukkit.getWorlds().get(0);
            org.bukkit.Location spLoc = new org.bukkit.Location(mainW, -196.5, 64.5, 34.5);
            if (!mp.getWorld().equals(mainW) || mp.getLocation().distanceSquared(spLoc) > 0.25) {
                mp.teleport(spLoc);
            }
        }
    }

    
    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onStrictRegisterCheck(org.bukkit.event.player.PlayerCommandPreprocessEvent e) {
        String msg = e.getMessage().trim();
        String[] parts = msg.split("\\s+");
        java.util.List<String> list = java.util.Arrays.asList(parts);
        String cmd = list.get(0).toLowerCase();
        if (cmd.equals("/register") || cmd.equals("/reg")) {
            if (parts.length != 3 || !list.get(1).equals(list.get(2))) {
                e.setCancelled(true);
                sendCompactMsg(e.getPlayer(), "§c§l[Bảo Mật] §fSai cú pháp! Nhập 2 lần giống nhau: §e/register <mật_khẩu> <mật_khẩu>");
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onBlockUnauthedModeGui(org.bukkit.event.inventory.InventoryOpenEvent e) {
        if (e.getView().getTitle().contains("CHỌN CHẾ ĐỘ CHƠI")) {
            java.util.UUID id = e.getPlayer().getUniqueId();
            if (!lobbyAuthed.contains(id) || unlogged.contains(id) || e.getPlayer().hasPotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS)) {
                e.setCancelled(true);
            }
        }
    }

    public static void addParkourMoney(org.bukkit.entity.Player p, long amt) {
        try {
            Main inst = (Main) org.bukkit.Bukkit.getPluginManager().getPlugin("ChillCore");
            if (inst == null) return;
            long cur = inst.getMoney(p);
            for (java.lang.reflect.Method m : Main.class.getDeclaredMethods()) {
                Class<?>[] pt = m.getParameterTypes();
                java.util.List<Class<?>> ptl = java.util.Arrays.asList(pt);
                if (pt.length == 2 && ptl.get(0) == org.bukkit.entity.Player.class && (ptl.get(1) == long.class || ptl.get(1) == int.class)) {
                    String mn = m.getName();
                    if (mn.equals("setMoney") || mn.equals("addMoney")) {
                        m.setAccessible(true);
                        long val = mn.equals("setMoney") ? (cur + amt) : amt;
                        if (ptl.get(1) == long.class) m.invoke(inst, p, val);
                        else m.invoke(inst, p, (int) val);
                        break;
                    }
                }
            }
            for (java.lang.reflect.Method m : Main.class.getDeclaredMethods()) {
                if (m.getParameterCount() == 1 && java.util.Arrays.asList(m.getParameterTypes()).get(0) == org.bukkit.entity.Player.class) {
                    if (m.getName().toLowerCase().contains("board") || m.getName().toLowerCase().contains("score")) {
                        m.setAccessible(true);
                        m.invoke(inst, p);
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}
