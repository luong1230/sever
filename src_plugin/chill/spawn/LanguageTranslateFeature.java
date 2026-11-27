package chill.spawn;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.*;

public class LanguageTranslateFeature implements Listener, CommandExecutor {
    private final JavaPlugin plugin;
    private final Map<UUID, String> playerLang = new ConcurrentHashMap<>();
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private final File langFile;
    private static final String USA_TEX = "http://minecraft.net";
    private static final String VN_TEX = "http://minecraft.net";

    public LanguageTranslateFeature(JavaPlugin p) {
        this.plugin = p;
        this.langFile = new File(p.getDataFolder(), "player_lang.yml");
        if (langFile.exists()) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(langFile);
            for (String k : cfg.getKeys(false)) try { playerLang.put(UUID.fromString(k), cfg.getString(k, "vi")); } catch (Exception ignored) {}
        }
    }
    public static void init(JavaPlugin p) {
        LanguageTranslateFeature f = new LanguageTranslateFeature(p);
        Bukkit.getPluginManager().registerEvents(f, p);
        if (p.getCommand("tieng") != null) p.getCommand("tieng").setExecutor(f);
        if (p.getCommand("lang") != null) p.getCommand("lang").setExecutor(f);
    }
    private void saveLang(UUID id, String lang) {
        playerLang.put(id, lang);
        YamlConfiguration cfg = langFile.exists() ? YamlConfiguration.loadConfiguration(langFile) : new YamlConfiguration();
        cfg.set(id.toString(), lang);
        try { cfg.save(langFile); } catch (Exception ignored) {}
    }
    private ItemStack makeLangButton(Player p) {
        ItemStack it = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName("§e§lTiếng §7(Language)");
        m.setLore(Arrays.asList("§7Bấm chọn ngôn ngữ dịch chat", "§7Click to select chat language"));
        it.setItemMeta(m); return it;
    }
    private boolean isLangButton(ItemStack it) {
        return it != null && it.hasItemMeta() && it.getItemMeta().hasDisplayName() && it.getItemMeta().getDisplayName().startsWith("§e§lTiếng");
    }
    private ItemStack makeFlagHead(String texUrl, String title, String l1, String l2) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm = (SkullMeta) head.getItemMeta();
        try {
            PlayerProfile prof = Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures tex = prof.getTextures(); tex.setSkin(new URL(texUrl));
            prof.setTextures(tex); sm.setOwnerProfile(prof);
        } catch (Exception ignored) {}
        sm.setDisplayName(title); sm.setLore(Arrays.asList(l1, l2));
        head.setItemMeta(sm); return head;
    }
    public void openLangGui(Player p) {
        Inventory gui = Bukkit.createInventory(null, 27, "§8Chọn Ngôn Ngữ / Language");
        ItemStack gl = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gm = gl.getItemMeta(); gm.setDisplayName(" "); gl.setItemMeta(gm);
        for (int i = 0; i < 27; i++) gui.setItem(i, gl);
        gui.setItem(11, makeFlagHead(USA_TEX, "§b§lEnglish (USA)", "§7Auto-translate chat to §fEnglish", "§a▶ Click to select English"));
        gui.setItem(15, makeFlagHead(VN_TEX, "§c§lTiếng Việt (VN)", "§7Tự động dịch chat sang §fTiếng Việt", "§a▶ Bấm để chọn Tiếng Việt"));
        p.openInventory(gui);
    }
    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (s instanceof Player) openLangGui((Player) s);
        return true;
    }
    @EventHandler
    public void onJoinCleanOldBook(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        for (int i = 0; i < 36; i++) {
            if (isLangButton(p.getInventory().getItem(i))) p.getInventory().setItem(i, null);
        }
    }
    @EventHandler(priority = EventPriority.HIGH)
    public void onShopOpenInject(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player p = (Player) e.getPlayer();
        String title = e.getView().getTitle();
        Inventory top = e.getView().getTopInventory();
        if (title.contains("Huong Dan") || title.contains("CHỌN CHẾ ĐỘ CHƠI") || title.contains("CHILL MC") || title.contains("CỬA HÀNG") || title.contains("Shop")) {
            if (top.getSize() >= 9 && (top.getItem(8) == null || top.getItem(8).getType() == Material.AIR || top.getItem(8).getType().name().contains("GLASS_PANE"))) {
                top.setItem(8, makeLangButton(p));
            }
        }
        if ("en".equals(playerLang.getOrDefault(p.getUniqueId(), "vi"))) {
            String enTitle = toEnGui(title);
            if (!enTitle.equals(title)) {
                try { e.getView().setTitle(enTitle); } catch (Exception ignored) {}
            }
            for (int i = 0; i < top.getSize(); i++) {
                ItemStack it = top.getItem(i);
                if (it == null || !it.hasItemMeta() || isLangButton(it)) continue;
                ItemMeta m = it.getItemMeta();
                if (m.hasDisplayName()) m.setDisplayName(toEnGui(m.getDisplayName()));
                if (m.hasLore()) {
                    java.util.List<String> nl = new java.util.ArrayList<>();
                    for (String line : m.getLore()) nl.add(toEnGui(line));
                    m.setLore(nl);
                }
                it.setItemMeta(m);
            }
        }
    }

    private String toEnGui(String s) {
        return s.replace("Bán Đồ Lấy Tiền", "Sell Items for Money")
                .replace("Cửa Hàng Chill MC", "Chill MC Server Shop")
                .replace("Shop Khối - Trang", "Block Shop - Page")
                .replace("Shop: Đá Đỏ & Chế Thuốc", "Shop: Redstone & Brewing")
                .replace("Shop: Trang Bị & Bộ Giáp", "Shop: Armor & Weapons")
                .replace("Shop: Dụng Cụ", "Shop: Tools")
                .replace("Chọn Số Lượng", "Select Amount")
                .replace("Mua Số Lượng", "Buy Quantity")
                .replace("Xác Nhận Mua", "Confirm Purchase")
                .replace("Hủy Bỏ", "Cancel")
                .replace("Thêm 1", "Add 1")
                .replace("Thêm 16", "Add 16")
                .replace("Thêm 64", "Add 64")
                .replace("Giảm 1", "Remove 1")
                .replace("Giảm 16", "Remove 16")
                .replace("Chợ Đen Chill MC", "Chill MC Auction House")
                .replace("Hướng Dẫn Treo Bán", "How to Sell Items")
                .replace("Cầm đồ trên tay và gõ:", "Hold item and type:")
                .replace("Thành Viên Clan:", "Clan Members:")
                .replace("Phần Thưởng & Tỷ Lệ Rương", "Crate Rewards & Rates")
                .replace("CHỌN CHẾ ĐỘ CHƠI", "SELECT GAME MODE")
                .replace("Gõ /helpchillmc hoac /help chill mc", "Type /helpchillmc or /help chill mc")
                .replace("Ban do lay tien", "Sell items for money")
                .replace("Xem & chuyen tien", "Check & send money")
                .replace("DAU TRUONG PVP & CLAN", "PVP ARENA & CLANS")
                .replace("Dung 2 tam ap luc o Dau Truong de PvP 1v1", "Teleport to 1v1 PvP Arena")
                .replace("Hoac ra ngoai vach do Spawn de PvP", "Or walk outside red Spawn border to PvP")
                .replace("Chuot trai ruong xem ti le, chuot phai quay", "Left-click chest for rates, Right-click to spin")
                .replace("Nhiem vu cay cuoc", "Daily mining & woodcutting quests")
                .replace("CHILL MC - Huong Dan", "CHILL MC - Server Guide")
                .replace("CHAO MUNG DEN CHILL MC", "WELCOME TO CHILL MC")
                .replace("Sinh Ton & Kinh Te", "Survival & Economy")
                .replace("KINH TE & SHOP", "ECONOMY & SHOP")
                .replace("Xem tien", "Check balance")
                .replace("Mo cua hang", "Open server shop")
                .replace("Chuyen tien", "Send money")
                .replace("Cho den treo ban", "Auction house")
                .replace("DI CHUYEN & SINH TON", "TELEPORT & SURVIVAL")
                .replace("Dich chuyen ngau nhien", "Random safe teleport")
                .replace("Quay ve sanh Spawn", "Return to Spawn")
                .replace("Luu nha", "Set & go home")
                .replace("TIEN ICH & KHOE DO", "UTILITIES & SHOWCASE")
                .replace("Mo Ruong Ender ca nhan", "Open personal Ender Chest")
                .replace("Khoe vat pham dang cam", "Show held item in chat")
                .replace("Khoe Ruong Ender len chat", "Show Ender Chest in chat")
                .replace("VONG QUAY & NHIEM VU", "CRATES & QUESTS").replace("Nhiem vu chat go, dao da", "Wood & Stone mining quests").replace("Nhan 500$/ngay", "Claim 500$ daily").replace("BANG HOI & PVP", "CLANS & PVP")
                .replace("Tao & quan ly Bang Hoi", "Create & manage Clans")
                .replace("Ra ngoai vach do Spawn de PvP", "Walk outside red Spawn border to PvP")
                .replace("Doi ngon ngu Viet/Anh", "Switch English / Vietnamese")
                .replace("Khối Xây Dựng (Nhiều Trang)", "Building Blocks (Multi-Page)")
                .replace("Đầy đủ tất cả các loại block xây dựng", "All building blocks available")
                .replace("Đá Đỏ & Chế Thuốc", "Redstone & Brewing")
                .replace("Piston, đá đỏ, pha thuốc & Ender", "Pistons, redstone, potions & Ender")
                .replace("Trang Bị & Bộ Giáp", "Armor & Weapons")
                .replace("Trọn bộ giáp Sắt/KC & vũ khí", "Full Iron/Diamond armor & weapons")
                .replace("Dụng Cụ (Cúp, Rìu, Xẻng)", "Tools (Pickaxe, Axe, Shovel)")
                .replace("Đủ bộ dụng cụ Đá, Sắt, KC", "Stone, Iron & Diamond tools")
                .replace("Phật Tổ", "Buddha Blessing")
                .replace("Táo Vàng", "Golden Apple")
                .replace("Bấm để chọn!", "Click to select!").replace("Bấm để chọn", "Click to select").replace("Quay lại Menu Chính", "Back to Main Menu")
                .replace("Trở về danh mục", "Return to categories")
                .replace("Trang Trước", "Previous Page")
                .replace("Trang Sau", "Next Page")
                .replace("Đơn giá:", "Unit price:")
                .replace("Số lượng:", "Amount:")
                .replace("Tổng tiền:", "Total:")
                .replace("CHILL MC - SINH TỒN", "CHILL MC - SURVIVAL");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLockAllServerGuis(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (title.contains("Cửa Hàng") || title.contains("Shop") || title.contains("CHILL MC") || title.contains("Huong Dan") || title.contains("Server Guide") || title.contains("Chọn Ngôn Ngữ") || title.contains("Select Language") || title.contains("Số Lượng") || title.contains("Amount") || title.contains("Chợ Đen") || title.contains("Auction")) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGuiAndSlotClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        if (isLangButton(e.getCurrentItem())) { e.setCancelled(true); openLangGui(p); return; }
        if (e.getView().getTitle().equals("§8Chọn Ngôn Ngữ / Language")) {
            e.setCancelled(true);
            if (e.getRawSlot() == 11) {
                saveLang(p.getUniqueId(), "en"); p.closeInventory();
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                p.sendMessage("§b§l[Language] §fSelected §bEnglish (USA)§f! Chat will auto-translate to English.");
            } else if (e.getRawSlot() == 15) {
                saveLang(p.getUniqueId(), "vi"); p.closeInventory();
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                p.sendMessage("§c§l[Ngôn Ngữ] §fĐã chọn §cTiếng Việt (VN)§f! Tin nhắn sẽ tự động dịch sang Tiếng Việt.");
            }
        }
    }
    @EventHandler
    public void onDropLangBtn(PlayerDropItemEvent e) {
        if (isLangButton(e.getItemDrop().getItemStack())) e.setCancelled(true);
    }
    private String translateText(String text, String targetLang) {
        String key = targetLang + ":" + text.toLowerCase();
        if (cache.containsKey(key)) return cache.get(key);
        try {
            String u = "https://googleapis.com" + targetLang + "&dt=t&q=" + URLEncoder.encode(text, StandardCharsets.UTF_8);
            HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
            c.setRequestProperty("User-Agent", "Mozilla/5.0"); c.setConnectTimeout(2000); c.setReadTimeout(2000);
            BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            String json = sb.toString();
            int q1 = json.indexOf('"'), q2 = json.indexOf('"', q1 + 1);
            if (q1 != -1 && q2 > q1) {
                String res = json.substring(q1 + 1, q2);
                if (!res.isEmpty()) { cache.put(key, res); return res; }
            }
        } catch (Exception ignored) {}
        return text;
    }
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAutoTranslateChat(AsyncPlayerChatEvent e) {
        String raw = e.getMessage(), low = raw.toLowerCase();
        if (raw.contains("[i]") || low.contains("[ec]") || low.contains("[enderchest]")) return;
        e.setCancelled(true);
        Player sender = e.getPlayer();
        boolean needEn = false, needVi = false;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if ("en".equals(playerLang.getOrDefault(p.getUniqueId(), "vi"))) needEn = true; else needVi = true;
        }
        String enText = needEn ? translateText(raw, "en") : raw;
        String viText = needVi ? translateText(raw, "vi") : raw;
        for (Player p : Bukkit.getOnlinePlayers()) {
            String chosen = "en".equals(playerLang.getOrDefault(p.getUniqueId(), "vi")) ? enText : viText;
            if (!chosen.equalsIgnoreCase(raw)) p.sendMessage("§7" + sender.getName() + "§f: " + chosen + " §8(" + raw + ")");
            else p.sendMessage("§7" + sender.getName() + "§f: " + raw);
        }
    }

    public static String translateServerMsg(org.bukkit.entity.Player p, String msg) {
        if (p == null || msg == null) return msg;
        try {
            org.bukkit.plugin.Plugin pl = org.bukkit.Bukkit.getPluginManager().getPlugin("ChillCore");
            java.io.File f = new java.io.File(pl.getDataFolder(), "player_lang.yml");
            if (!f.exists()) return msg;
            org.bukkit.configuration.file.YamlConfiguration cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
            if (!"en".equals(cfg.getString(p.getUniqueId().toString(), "vi"))) return msg;
            return msg.replace("Đã mua", "Purchased")
                      .replace("Đã bán", "Sold")
                      .replace("Không đủ tiền", "Not enough money")
                      .replace("Túi đồ đã đầy", "Inventory is full")
                      .replace("Số tiền hiện tại của bạn là:", "Your current balance is:")
                      .replace("Chúc bạn chơi vui vẻ", "Have fun playing")
                      .replace("Sai mật khẩu", "Wrong password")
                      .replace("Vui lòng nhập", "Please enter")
                      .replace("Đã dịch chuyển", "Teleported")
                      .replace("Khu vực an toàn", "Safe Zone")
                      .replace("Bạn không có quyền", "You do not have permission")
                      .replace("Thành công", "Success")
                      .replace("Thất bại", "Failed");
        } catch (Exception ignored) {}
        return msg;
    }
}
