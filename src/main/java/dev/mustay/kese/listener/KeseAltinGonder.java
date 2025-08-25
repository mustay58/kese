package dev.mustay.kese.listener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import dev.mustay.kese.Kese;

public class KeseAltinGonder implements Listener {
	
	private final Kese plugin;
    
    public KeseAltinGonder(Kese plugin) {
        this.plugin = plugin;
    }
    
    public void openOyuncuListesiMenu(Player player, int page) {
        List<Player> oyuncular = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.sort(oyuncular, (p1, p2) -> {
           return p1.getName().compareToIgnoreCase(p2.getName());
        });
        int itemsPerPage = 45;
        int totalPages = (int)Math.ceil((double)oyuncular.size() / (double)itemsPerPage);
        Inventory menu = Bukkit.createInventory(null, 54, ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("gonder_oyuncu_listesi.name").replace("{sayfa}", page + "/" + totalPages)));
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, oyuncular.size());

        for(int i = startIndex; i < endIndex; ++i) {
           Player oyuncu = (Player)oyuncular.get(i);
           ItemStack oyuncuKafasi = new ItemStack(Material.PLAYER_HEAD);
           int keseAltın = this.getKesedekiAltin(oyuncu);
           SkullMeta meta = (SkullMeta)oyuncuKafasi.getItemMeta();
           meta.setOwningPlayer(oyuncu);
           meta.setDisplayName(oyuncu.getName());
           List<String> oyuncu_lore = this.plugin.getMesajlarConfig().getStringList("gonder_oyuncu_listesi.lore");
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
           sonrakiMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("gonder_oyuncu_listesi.onceki_sayfa")));
           sonrakiSayfa.setItemMeta(sonrakiMeta);
           menu.setItem(45, sonrakiSayfa);
        }

        if (page < totalPages) {
           sonrakiSayfa = new ItemStack(Material.ARROW);
           sonrakiMeta = sonrakiSayfa.getItemMeta();
           sonrakiMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("gonder_oyuncu_listesi.sonraki_sayfa")));
           sonrakiSayfa.setItemMeta(sonrakiMeta);
           menu.setItem(53, sonrakiSayfa);
        }

        player.openInventory(menu);
     }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
       if (event.getView().getTitle().contains(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("gonder_oyuncu_listesi.name").replace("{sayfa}", "")))) {
          event.setCancelled(true);
          Player player = (Player)event.getWhoClicked();
          ItemStack clickedItem = event.getCurrentItem();
          if (clickedItem != null && clickedItem.hasItemMeta()) {
             ItemMeta meta = clickedItem.getItemMeta();
             String displayName = meta.getDisplayName();
             int currentPage;
             if (displayName.equals(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("gonder_oyuncu_listesi.onceki_sayfa")))) {
                currentPage = this.getCurrentPage(event.getView().getTitle());
                if (currentPage > 1) {
                   this.openOyuncuListesiMenu(player, currentPage - 1);
                }
             } else if (displayName.equals(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("gonder_oyuncu_listesi.sonraki_sayfa")))) {
                currentPage = this.getCurrentPage(event.getView().getTitle());
                int totalPages = this.getTotalPages(event.getView().getTitle());
                if (currentPage < totalPages) {
                   this.openOyuncuListesiMenu(player, currentPage + 1);
                }
             } else {
                Player hedefOyuncu = Bukkit.getPlayer(displayName);
                if (hedefOyuncu != null) {
                   player.closeInventory();
                   this.startChatInputListenerGonderme(player, hedefOyuncu);
                }
             }
          }
       }

    }
    
    private int getCurrentPage(String title) {
        String[] parts = title.split("/");

        try {
           return Integer.parseInt(parts[0].replaceAll("[^0-9]", "").trim());
        } catch (NumberFormatException e) {
           return 1;
        }
     }

     private int getTotalPages(String title) {
        String[] parts = title.split("/");

        try {
           return Integer.parseInt(parts[1].replaceAll("[^0-9]", "").trim());
        } catch (NumberFormatException e) {
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
    
    public void startChatInputListenerGonderme(final Player player, final Player hedefOyuncu) {
        if (hedefOyuncu.equals(player)) {
           player.sendTitle(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("gonder_mesajlari.kendine_gonderemez")), 10, 100, 20);
           player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
           player.closeInventory();
        } else {
           player.sendTitle(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("gonder_title_mesaj.miktar_gir")), ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("gonder_title_mesaj.miktar_gir_lore")), 10, 100, 20);
           Bukkit.getPluginManager().registerEvents(new Listener() {
              @EventHandler
              public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
                 if (event.getPlayer().equals(player)) {
                    event.setCancelled(true);
                    player.sendTitle(ChatColor.translateAlternateColorCodes('&', KeseAltinGonder.this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', KeseAltinGonder.this.plugin.getMesajlarConfig().getString("kese_genel.islem_sonlandirildi")), 5, 100, 20);
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
                    PlayerCommandPreprocessEvent.getHandlerList().unregister(this);
                    AsyncPlayerChatEvent.getHandlerList().unregister(this);
                 }

              }

              @EventHandler
              public void onPlayerChat(AsyncPlayerChatEvent event) {
                 if (event.getPlayer().equals(player)) {
                    event.setCancelled(true);
                    String mesaj = event.getMessage();

                    try {
                       int miktar = Integer.parseInt(mesaj);
                       if (miktar > 0) {
                          if (mesaj.matches("-?\\d+")) {
                             KeseAltinGonder.this.gonderAltin(player, hedefOyuncu, miktar);
                          } else {
                             player.sendTitle(ChatColor.translateAlternateColorCodes('&', KeseAltinGonder.this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', KeseAltinGonder.this.plugin.getMesajlarConfig().getString("kese_genel.sayisal_deger_hata")), 5, 100, 20);
                             player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
                          }
                       } else {
                          player.sendTitle(ChatColor.translateAlternateColorCodes('&', KeseAltinGonder.this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', KeseAltinGonder.this.plugin.getMesajlarConfig().getString("kese_genel.pozitif_sayi")), 5, 100, 20);
                          player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
                       }
                    } catch (NumberFormatException var7) {
                       player.sendTitle(ChatColor.translateAlternateColorCodes('&', KeseAltinGonder.this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', KeseAltinGonder.this.plugin.getMesajlarConfig().getString("kese_genel.islem_sonlandirildi")), 5, 100, 20);
                       player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
                    } finally {
                       AsyncPlayerChatEvent.getHandlerList().unregister(this);
                       PlayerCommandPreprocessEvent.getHandlerList().unregister(this);
                    }
                 }

              }
           }, this.plugin);
        }
     }
    
    private void gonderAltin(Player gonderen, Player alici, int miktar) {
        int gonderenAltini = 0;
        if (this.plugin.getConfig().getBoolean("mysql-enabled")) {
           try {
              String sql = "SELECT kesedeki_altin FROM keseler WHERE oyuncu_ismi = ?";
              PreparedStatement statement = this.plugin.getConnection().prepareStatement(sql);
              statement.setString(1, gonderen.getName());
              ResultSet result = statement.executeQuery();
              if (result.next()) {
                 gonderenAltini = result.getInt("kesedeki_altin");
              }
           } catch (SQLException e) {
              this.plugin.getLogger().severe("MySQL'den veri alınırken bir hata oluştu: " + e.getMessage());
              return;
           }
        } else {
           FileConfiguration keselerConfig = this.plugin.getKeselerConfig();
           gonderenAltini = keselerConfig.getInt("keseler." + gonderen.getName() + ".kesedeki_altin", 0);
        }

        if (gonderenAltini >= miktar) {
           int aliciAltini = 0;
           if (this.plugin.getConfig().getBoolean("mysql-enabled")) {
              try {
                 String sql = "SELECT kesedeki_altin FROM keseler WHERE oyuncu_ismi = ?";
                 PreparedStatement statement = this.plugin.getConnection().prepareStatement(sql);
                 statement.setString(1, alici.getName());
                 ResultSet result = statement.executeQuery();
                 if (result.next()) {
                    aliciAltini = result.getInt("kesedeki_altin");
                 }
              } catch (SQLException e) {
                 this.plugin.getLogger().severe("MySQL'den veri alınırken bir hata oluştu: " + e.getMessage());
                 return;
              }
           } else {
              FileConfiguration keselerConfig = this.plugin.getKeselerConfig();
              aliciAltini = keselerConfig.getInt("keseler." + alici.getName() + ".kesedeki_altin", 0);
           }

           int keseLimiti = this.plugin.getConfig().getInt("kese_limiti");
           if (aliciAltini + miktar <= keseLimiti) {
              if (this.plugin.getConfig().getBoolean("mysql-enabled")) {
                 try {
                    String sql = "UPDATE keseler SET kesedeki_altin = kesedeki_altin - ? WHERE oyuncu_ismi = ?";
                    PreparedStatement statement = this.plugin.getConnection().prepareStatement(sql);
                    statement.setInt(1, miktar);
                    statement.setString(2, gonderen.getName());
                    statement.executeUpdate();
                    sql = "UPDATE keseler SET kesedeki_altin = kesedeki_altin + ? WHERE oyuncu_ismi = ?";
                    statement = this.plugin.getConnection().prepareStatement(sql);
                    statement.setInt(1, miktar);
                    statement.setString(2, alici.getName());
                    statement.executeUpdate();
                 } catch (SQLException e) {
                    this.plugin.getLogger().severe("MySQL'e kayıt yapılırken bir hata oluştu: " + e.getMessage());
                    return;
                 }
              } else {
                 FileConfiguration keselerConfig = this.plugin.getKeselerConfig();
                 keselerConfig.set("keseler." + gonderen.getName() + ".kesedeki_altin", gonderenAltini - miktar);
                 keselerConfig.set("keseler." + alici.getName() + ".kesedeki_altin", aliciAltini + miktar);
                 this.plugin.saveKeselerFile();
              }

              gonderen.sendTitle(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarili")), ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("gonder_mesajlari.altin_gonderildi_basarili").replace("{oyuncu}", alici.getName()).replace("{miktar}", String.valueOf(miktar))), 5, 100, 20);
              gonderen.playSound(gonderen.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
              alici.sendTitle(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarili")), ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("gonder_mesajlari.altin_geldi_basarili").replace("{oyuncu}", gonderen.getName()).replace("{miktar}", String.valueOf(miktar))), 5, 100, 20);
              alici.playSound(alici.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
           } else {
              gonderen.sendTitle(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("gonder_mesajlari.limit_asildi")), 5, 100, 20);
              gonderen.playSound(gonderen.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
           }
        } else {
           gonderen.sendTitle(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("gonder_mesajlari.yetersiz_altin")), 5, 100, 20);
           gonderen.playSound(gonderen.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
        }

     }
}
