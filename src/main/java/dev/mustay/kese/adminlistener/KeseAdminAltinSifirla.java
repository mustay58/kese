package dev.mustay.kese.adminlistener;

import dev.lone.itemsadder.api.CustomStack;
import dev.mustay.kese.Kese;
import io.th0rgal.oraxen.api.OraxenItems;
import me.arcaniax.hdb.api.HeadDatabaseAPI;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KeseAdminAltinSifirla implements Listener {

    private final Kese plugin;
    private Player seciliOyuncu;
    private String seciliIslem;
    
    public KeseAdminAltinSifirla(Kese plugin) {
        this.plugin = plugin;
    }
    
    
    public void openOyuncuListesiAdminMenu(Player player, int page) {
        List<Player> oyuncular = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.sort(oyuncular, (p1, p2) -> {
           return p1.getName().compareToIgnoreCase(p2.getName());
        });
        int itemsPerPage = 45;
        int totalPages = (int)Math.ceil((double)oyuncular.size() / (double)itemsPerPage);
        Inventory menu = Bukkit.createInventory(null, 54, ChatColor.translateAlternateColorCodes('&', this.plugin.getAdminConfig().getString("sifirla_admin_listesi.name")));
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, oyuncular.size());

        for(int i = startIndex; i < endIndex; ++i) {
           Player oyuncu = (Player)oyuncular.get(i);
           ItemStack oyuncuKafasi = new ItemStack(Material.PLAYER_HEAD);
           int keseAltın = this.getKesedekiAltin(oyuncu);
           SkullMeta meta = (SkullMeta)oyuncuKafasi.getItemMeta();
           meta.setOwningPlayer(oyuncu);
           meta.setDisplayName(oyuncu.getName());
           List<String> oyuncu_lore = this.plugin.getAdminConfig().getStringList("sifirla_admin_listesi.lore");
           List<String> loreCvr = new ArrayList<>();
           int limit = this.plugin.getConfig().getInt("kese_limiti");
           for (String loreMsg : oyuncu_lore) {
               loreCvr.add(ChatColor.translateAlternateColorCodes('&', loreMsg.replace("{altin}", String.valueOf(keseAltın)).replace("{limit}", String.valueOf(limit))));
           }

           meta.setLore(loreCvr);
           oyuncuKafasi.setItemMeta(meta);
           menu.setItem(i - startIndex, oyuncuKafasi);
        }

        ItemStack sonrakiSayfa;
        ItemMeta sonrakiMeta;
        if (page > 1) {
           sonrakiSayfa = new ItemStack(Material.ARROW);
           sonrakiMeta = sonrakiSayfa.getItemMeta();
           sonrakiMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', this.plugin.getAdminConfig().getString("sifirla_admin_listesi.onceki_sayfa")));
           sonrakiSayfa.setItemMeta(sonrakiMeta);
           menu.setItem(45, sonrakiSayfa);
        }

        if (page < totalPages) {
           sonrakiSayfa = new ItemStack(Material.ARROW);
           sonrakiMeta = sonrakiSayfa.getItemMeta();
           sonrakiMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', this.plugin.getAdminConfig().getString("sifirla_admin_listesi.sonraki_sayfa")));
           sonrakiSayfa.setItemMeta(sonrakiMeta);
           menu.setItem(53, sonrakiSayfa);
        }

        player.openInventory(menu);
     }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
       Player player = (Player)event.getWhoClicked();
       if (event.getView().getTitle().contains(ChatColor.translateAlternateColorCodes('&', this.plugin.getAdminConfig().getString("sifirla_admin_listesi.name").replace("{sayfa}", "")))) {
          event.setCancelled(true);
          ItemStack clickedItem = event.getCurrentItem();
          if (clickedItem != null && clickedItem.hasItemMeta()) {
             ItemMeta meta = clickedItem.getItemMeta();
             String displayName = meta.getDisplayName();
             int currentPage;
             if (displayName.equals(ChatColor.translateAlternateColorCodes('&', this.plugin.getAdminConfig().getString("sifirla_admin_listesi.onceki_sayfa")))) {
                currentPage = this.getCurrentPage(event.getView().getTitle());
                if (currentPage > 1) {
                   openOyuncuListesiAdminMenu(player, currentPage - 1);
                }
             } else if (displayName.equals(ChatColor.translateAlternateColorCodes('&', this.plugin.getAdminConfig().getString("sifirla_admin_listesi.sonraki_sayfa")))) {
                currentPage = this.getCurrentPage(event.getView().getTitle());
                int totalPages = this.getTotalPages(event.getView().getTitle());
                if (currentPage < totalPages) {
                   openOyuncuListesiAdminMenu(player, currentPage + 1);
                }
             } else {
                Player hedefOyuncu = Bukkit.getPlayer(displayName);
                if (hedefOyuncu != null) {
                   player.closeInventory();
                   this.openOnaylaIptalMenu(player, hedefOyuncu, this.seciliIslem);
                }
             }
          }
       }

       if (event.getView().getTitle().equals(ChatColor.translateAlternateColorCodes('&', this.plugin.getAdminConfig().getString("sifirla_admin_listesi.name"))) && event.getSlotType() == SlotType.CONTAINER && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
          String hedefOyuncuAdi = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
          Player hedefOyuncu = Bukkit.getPlayerExact(hedefOyuncuAdi);
          event.setCancelled(true);
          player.closeInventory();
          this.openOnaylaIptalMenu(player, hedefOyuncu, this.seciliIslem);
       }

       if (event.getView().getTitle().equals(ChatColor.translateAlternateColorCodes('&', this.plugin.getAdminConfig().getString("karar_baslik.name")))) {
          Player hedefOyuncu = this.seciliOyuncu;
          this.handleMenuItemKarar(event, player, "onayla", () -> {
             player.closeInventory();
             this.keseyiSifirla(player, hedefOyuncu);
          });
          this.handleMenuItemKarar(event, player, "iptal_et", () -> {
             player.closeInventory();
          });
       }

    }
    
    private int getCurrentPage(String title) {
        String[] parts = title.split("/");

        try {
           return Integer.parseInt(parts[0].replaceAll("[^0-9]", "").trim());
        } catch (NumberFormatException var4) {
           return 1;
        }
     }

     private int getTotalPages(String title) {
        String[] parts = title.split("/");

        try {
           return Integer.parseInt(parts[1].replaceAll("[^0-9]", "").trim());
        } catch (NumberFormatException var4) {
           return 1;
        }
     }
    
    private int getKesedekiAltin(Player oyuncu) {
        int altinMiktari = 0;
        String sql;
        if (this.plugin.getConfig().getBoolean("mysql-enabled")) {
           try {
              Connection connection = this.plugin.getConnection();
              if (connection != null) {
                 sql = "SELECT kesedeki_altin FROM keseler WHERE oyuncu_ismi = ?";
                 PreparedStatement statement = connection.prepareStatement(sql);
                 statement.setString(1, oyuncu.getName());
                 ResultSet result = statement.executeQuery();
                 if (result.next()) {
                    altinMiktari = result.getInt("kesedeki_altin");
                 }
              } else {
                 this.plugin.getLogger().severe("Veritabanı bağlantısı kurulamadı.");
              }
           } catch (SQLException e) {
              this.plugin.getLogger().severe("MySQL'den veri alınırken bir hata oluştu: " + e.getMessage());
           }
        } else {
           FileConfiguration keselerConfig = this.plugin.getKeselerConfig();
           sql = oyuncu.getName();
           altinMiktari = keselerConfig.getInt("keseler." + sql + ".kesedeki_altin", 0);
        }

        return altinMiktari;
     }
 
    public void openOnaylaIptalMenu(Player admin, Player hedefOyuncu, String islem) {
        Inventory menu = Bukkit.createInventory(null, 9, ChatColor.translateAlternateColorCodes('&', this.plugin.getAdminConfig().getString("karar_baslik.name")));
        ItemStack onayla = this.createMenuItem("onayla");
        List<Integer> onaylaslot = this.plugin.getAdminConfig().getIntegerList("karar_baslik.onayla.slots");
        for (int slot : onaylaslot) {
            menu.setItem(slot, onayla);
        }

        ItemStack iptalet = this.createMenuItem("iptal_et");
        List<Integer> iptaletslot = this.plugin.getAdminConfig().getIntegerList("karar_baslik.iptal_et.slots");
        for (int slot : iptaletslot) {
            menu.setItem(slot, iptalet);
            this.seciliOyuncu = hedefOyuncu;
            this.seciliIslem = islem;

            admin.openInventory(menu);
        }

     }
    
    private ItemStack createMenuItem(String configKey) {
        String itemId = this.plugin.getAdminConfig().getString("karar_baslik." + configKey + ".material");
        ItemStack item = null;

        try {
           if (itemId != null) {
              if (itemId.startsWith("hdb:")) {
                 if (this.plugin.getServer().getPluginManager().isPluginEnabled("HeadDatabase")) {
                    HeadDatabaseAPI api = new HeadDatabaseAPI();
                    String headId = itemId.substring(4);
                    item = api.getItemHead(headId);
                 }
              } else if (this.plugin.getConfig().getBoolean("oraxen-enabled") && this.plugin.getServer().getPluginManager().isPluginEnabled("Oraxen")) {
                 if (itemId.startsWith("oraxen:")) {
                    String oraxenId = itemId.substring(7);
                    item = OraxenItems.getItemById(oraxenId).build();
                 }
              } else if (this.plugin.getConfig().getBoolean("itemsadder-enabled") && this.plugin.getServer().getPluginManager().isPluginEnabled("ItemsAdder")) {
                 CustomStack customStack = CustomStack.getInstance(itemId);
                 if (customStack != null) {
                    item = customStack.getItemStack();
                 }
              } else {
                 try {
                    item = new ItemStack(Material.valueOf(itemId.toUpperCase()));
                 } catch (IllegalArgumentException var9) {
                    this.plugin.getLogger().severe("Geçersiz Material ID: " + itemId);
                 }
              }
           }
        } catch (Exception var10) {
           this.plugin.getLogger().severe("Item oluşturulurken bir hata oluştu: " + var10.getMessage());
           item = new ItemStack(Material.BARRIER);
        }

        if (item == null) {
           item = new ItemStack(Material.BARRIER);
        }

        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', this.plugin.getAdminConfig().getString("karar_baslik." + configKey + ".name")));
        List<String> lore = this.plugin.getAdminConfig().getStringList("karar_baslik." + configKey + ".lore");
        List<String> loreCvr = new ArrayList<>();
        for (String loreMsg : lore) {
            loreCvr.add(ChatColor.translateAlternateColorCodes('&', loreMsg));
        }

        itemMeta.setLore(loreCvr);
        item.setItemMeta(itemMeta);
        return item;
     }
    
    public void keseyiSifirla(Player admin, Player hedefOyuncu) {
        if (this.plugin.getConfig().getBoolean("mysql-enabled")) {
           try {
              String sql = "UPDATE keseler SET kesedeki_altin = 0 WHERE oyuncu_ismi = ?";
              PreparedStatement statement = this.plugin.getConnection().prepareStatement(sql);
              statement.setString(1, hedefOyuncu.getName());
              statement.executeUpdate();
           } catch (SQLException e) {
              this.plugin.getLogger().severe("MySQL'e kayıt yapılırken bir hata oluştu: " + e.getMessage());
              return;
           }
        } else {
           FileConfiguration keselerConfig = this.plugin.getKeselerConfig();
           keselerConfig.set("keseler." + hedefOyuncu.getName() + ".kesedeki_altin", 0);
           this.plugin.saveKeselerFile();
        }

        admin.sendTitle(ChatColor.translateAlternateColorCodes('&', this.plugin.getAdminConfig().getString("adminmenu_genel.islem_basarili")), ChatColor.translateAlternateColorCodes('&', this.plugin.getAdminConfig().getString("sifirla_mesajlari.altin_sifirladi_admin").replace("{oyuncu}", admin.getName())), 5, 100, 20);
        admin.playSound(admin.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
        hedefOyuncu.sendTitle(ChatColor.translateAlternateColorCodes('&', this.plugin.getAdminConfig().getString("adminmenu_genel.islem_basarili")), ChatColor.translateAlternateColorCodes('&', this.plugin.getAdminConfig().getString("sifirla_mesajlari.altin_sifirlandi_oyuncu").replace("{oyuncu}", hedefOyuncu.getName())), 5, 100, 20);
        hedefOyuncu.playSound(hedefOyuncu.getLocation(), Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 1.0F, 1.0F);
     }
    
    private void handleMenuItemKarar(InventoryClickEvent event, Player player, String configKey, Runnable action) {
        List<Integer> slots = plugin.getAdminConfig().getIntegerList("karar_baslik." + configKey + ".slots");
        if (slots.contains(event.getSlot())) {
           event.setCancelled(true);
           action.run();
        }

     }
    
    public Player getSeciliOyuncu() {
        return this.seciliOyuncu;
     }

     public void setSeciliOyuncu(Player seciliOyuncu) {
        this.seciliOyuncu = seciliOyuncu;
     }

     public String getSeciliIslem() {
        return this.seciliIslem;
     }

     public void setSeciliIslem(String seciliIslem) {
        this.seciliIslem = seciliIslem;
     }
}