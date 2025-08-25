package dev.mustay.kese.listener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.lone.itemsadder.api.CustomStack;
import dev.mustay.kese.Kese;
import io.th0rgal.oraxen.api.OraxenItems;
import me.arcaniax.hdb.api.HeadDatabaseAPI;

public class KeseAltinKoy implements Listener{
	
	private final Kese plugin;
    
    public KeseAltinKoy(Kese plugin) {
        this.plugin = plugin;
    }
	
    public void openKeseAltınKoyMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, this.plugin.getConfig().getInt("menuler.koy_menu"), ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("menu_baslik.kese_altin_koy")));
        ItemStack miktarGirerekKoy = this.createMenuItem("miktar_girerek");
        List<Integer> miktarGirerekKoySlots = this.plugin.getMesajlarConfig().getIntegerList("menu_altin_koy.miktar_girerek.slots");
        for (int slot : miktarGirerekKoySlots) {
            menu.setItem(slot, miktarGirerekKoy);
        }

        ItemStack tamamınıKoy = this.createMenuItem("tamamini");
        List<Integer> tamaminiKoySlots = this.plugin.getMesajlarConfig().getIntegerList("menu_altin_koy.tamamini.slots");
        for (int slot : tamaminiKoySlots) {
            menu.setItem(slot, tamamınıKoy);
        }

        player.openInventory(menu);
     }
    
    
    private ItemStack createMenuItem(String configKey) {
        String itemId = this.plugin.getMesajlarConfig().getString("menu_altin_koy." + configKey + ".material");
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
                 } catch (IllegalArgumentException e) {
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
        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("menu_altin_koy." + configKey + ".name")));
        List<String> lore = this.plugin.getMesajlarConfig().getStringList("menu_altin_koy." + configKey + ".lore");
        List<String> loreCvr = new ArrayList<>();
        for (String loreMsg : lore) {
            loreCvr.add(ChatColor.translateAlternateColorCodes('&', loreMsg));
        }

        itemMeta.setLore(loreCvr);
        item.setItemMeta(itemMeta);
        return item;
     }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
       String oyuncuIsmi = event.getPlayer().getName();
       if (this.plugin.getConfig().getBoolean("mysql-enabled")) {
          try {
             Connection connection = this.plugin.getConnection();
             if (connection != null) {
                String checkSql = "SELECT COUNT(*) FROM keseler WHERE oyuncu_ismi = ?";
                PreparedStatement checkStmt = connection.prepareStatement(checkSql);
                checkStmt.setString(1, oyuncuIsmi);
                ResultSet resultSet = checkStmt.executeQuery();
                resultSet.next();
                if (resultSet.getInt(1) == 0) {
                   String insertSql = "INSERT INTO keseler (oyuncu_ismi) VALUES (?)";
                   PreparedStatement insertStmt = connection.prepareStatement(insertSql);
                   insertStmt.setString(1, oyuncuIsmi);
                   insertStmt.executeUpdate();
                }
             } else {
                this.plugin.getLogger().severe("Veritabanı bağlantısı kurulamadı.");
             }
          } catch (SQLException e) {
             this.plugin.getLogger().severe("MySQL'e kayıt yapılırken bir hata oluştu: " + e.getMessage());
          }
       } else if (!this.plugin.getKeselerConfig().contains("keseler." + oyuncuIsmi)) {
          this.plugin.getKeselerConfig().set("keseler." + oyuncuIsmi + ".kesedeki_altin", 0);
          this.plugin.saveKeselerFile();
       }

    }
    
    public void startChatInputListener(final Player player) {
        player.sendTitle(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("koy_title_mesaj.miktar_gir")), ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("koy_title_mesaj.miktar_gir_lore")), 10, 100, 20);
        Bukkit.getPluginManager().registerEvents(new Listener() {
           @EventHandler
           public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
              if (event.getPlayer().equals(player)) {
                 event.setCancelled(true);
                 player.sendTitle(ChatColor.translateAlternateColorCodes('&', KeseAltinKoy.this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', KeseAltinKoy.this.plugin.getMesajlarConfig().getString("kese_genel.islem_sonlandirildi")), 5, 100, 20);
                 player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
                 PlayerCommandPreprocessEvent.getHandlerList().unregister(this);
                 AsyncPlayerChatEvent.getHandlerList().unregister(this);
              }

           }

           @EventHandler
           public void onPlayerChat(AsyncPlayerChatEvent event) {
              if (event.getPlayer().equals(player)) {
                 event.setCancelled(true);
                 int keseLimiti = KeseAltinKoy.this.plugin.getConfig().getInt("kese_limiti");
                 FileConfiguration keselerConfig = KeseAltinKoy.this.plugin.getKeselerConfig();
                 String oyuncuIsmi = player.getName();
                 String mesaj = event.getMessage();

                 try {
                    int miktar = Integer.parseInt(mesaj);
                    if (miktar > 0) {
                       if (player.getInventory().containsAtLeast(new ItemStack(Material.GOLD_INGOT), miktar)) {
                          int mevcutAltin = KeseAltinKoy.this.getKesedekiAltin(player);
                          if (mevcutAltin + miktar <= keseLimiti) {
                             if (mesaj.matches("-?\\d+")) {
                                if (KeseAltinKoy.this.plugin.getConfig().getBoolean("mysql-enabled")) {
                                   try {
                                      Connection connection = KeseAltinKoy.this.plugin.getConnection();
                                      if (connection != null) {
                                         String selectSql = "SELECT kesedeki_altin FROM keseler WHERE oyuncu_ismi = ?";
                                         PreparedStatement selectStatement = connection.prepareStatement(selectSql);
                                         selectStatement.setString(1, oyuncuIsmi);
                                         ResultSet resultSet = selectStatement.executeQuery();
                                         if (resultSet.next()) {
                                            int mevcutAltinDB = resultSet.getInt("kesedeki_altin");
                                            String updateSql = "UPDATE keseler SET kesedeki_altin = ? WHERE oyuncu_ismi = ?";
                                            PreparedStatement updateStatement = connection.prepareStatement(updateSql);
                                            updateStatement.setInt(1, mevcutAltinDB + miktar);
                                            updateStatement.setString(2, oyuncuIsmi);
                                            updateStatement.executeUpdate();
                                         } else {
                                            String insertSql = "INSERT INTO keseler (oyuncu_ismi, kesedeki_altin) VALUES (?, ?)";
                                            PreparedStatement insertStatement = connection.prepareStatement(insertSql);
                                            insertStatement.setString(1, oyuncuIsmi);
                                            insertStatement.setInt(2, miktar);
                                            insertStatement.executeUpdate();
                                         }

                                         player.getInventory().removeItem(new ItemStack[]{new ItemStack(Material.GOLD_INGOT, miktar)});
                                         player.sendTitle(ChatColor.translateAlternateColorCodes('&', KeseAltinKoy.this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarili")), ChatColor.translateAlternateColorCodes('&', KeseAltinKoy.this.plugin.getMesajlarConfig().getString("koy_mesajlari.altin_eklendi_basarili")), 5, 100, 20);
                                         player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                                      } else {
                                         KeseAltinKoy.this.plugin.getLogger().severe("Veritabanı bağlantısı kurulamadı.");
                                      }
                                   } catch (SQLException var15) {
                                      KeseAltinKoy.this.plugin.getLogger().severe("MySQL'e kayıt yapılırken bir hata oluştu: " + var15.getMessage());
                                   }
                                } else {
                                   keselerConfig.set("keseler." + oyuncuIsmi + ".kesedeki_altin", mevcutAltin + miktar);
                                   KeseAltinKoy.this.plugin.saveKeselerFile();
                                   player.getInventory().removeItem(new ItemStack[]{new ItemStack(Material.GOLD_INGOT, miktar)});
                                   player.resetTitle();
                                   player.sendTitle(ChatColor.translateAlternateColorCodes('&', KeseAltinKoy.this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarili")), ChatColor.translateAlternateColorCodes('&', KeseAltinKoy.this.plugin.getMesajlarConfig().getString("koy_mesajlari.altin_eklendi_basarili")), 5, 100, 20);
                                   player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                                }
                             } else {
                                player.sendTitle(ChatColor.translateAlternateColorCodes('&', KeseAltinKoy.this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', KeseAltinKoy.this.plugin.getMesajlarConfig().getString("genel.sayisal_deger_hata")), 5, 100, 20);
                                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
                             }
                          } else {
                             player.sendTitle(ChatColor.translateAlternateColorCodes('&', KeseAltinKoy.this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', KeseAltinKoy.this.plugin.getMesajlarConfig().getString("koy_mesajlari.limit_asildi")), 5, 100, 20);
                             player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
                          }
                       } else {
                          player.sendTitle(ChatColor.translateAlternateColorCodes('&', KeseAltinKoy.this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', KeseAltinKoy.this.plugin.getMesajlarConfig().getString("koy_mesajlari.yetersiz_altin")), 5, 100, 20);
                          player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
                       }
                    } else {
                       player.sendTitle(ChatColor.translateAlternateColorCodes('&', KeseAltinKoy.this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', KeseAltinKoy.this.plugin.getMesajlarConfig().getString("kese_genel.pozitif_sayi")), 5, 100, 20);
                       player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
                    }
                 } catch (NumberFormatException var16) {
                    player.sendTitle(ChatColor.translateAlternateColorCodes('&', KeseAltinKoy.this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', KeseAltinKoy.this.plugin.getMesajlarConfig().getString("kese_genel.islem_sonlandirildi")), 5, 100, 20);
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
                 }

                 AsyncPlayerChatEvent.getHandlerList().unregister(this);
                 PlayerCommandPreprocessEvent.getHandlerList().unregister(this);
              }

           }
        }, this.plugin);
     }
 
    
    public void altinKoy(Player player, int miktar) {
        FileConfiguration keselerConfig = this.plugin.getKeselerConfig();
        String oyuncuIsmi = player.getName();
        if (miktar > 0) {
           int mevcutAltin = this.getKesedekiAltin(player);
           int keseLimiti = this.plugin.getConfig().getInt("kese_limiti");
           int koyulacakMiktar = Math.min(miktar, keseLimiti - mevcutAltin);
           if (koyulacakMiktar > 0) {
              if (this.plugin.getConfig().getBoolean("mysql-enabled")) {
                 try {
                    Connection connection = this.plugin.getConnection();
                    if (connection != null) {
                       String selectSql = "SELECT kesedeki_altin FROM keseler WHERE oyuncu_ismi = ?";
                       PreparedStatement selectStatement = connection.prepareStatement(selectSql);
                       selectStatement.setString(1, oyuncuIsmi);
                       ResultSet resultSet = selectStatement.executeQuery();
                       if (resultSet.next()) {
                          int mevcutAltinDB = resultSet.getInt("kesedeki_altin");
                          if (mevcutAltinDB + miktar > keseLimiti) {
                             koyulacakMiktar = keseLimiti - mevcutAltinDB;
                          } else {
                             koyulacakMiktar = miktar;
                          }

                          String updateSql = "UPDATE keseler SET kesedeki_altin = ? WHERE oyuncu_ismi = ?";
                          PreparedStatement updateStatement = connection.prepareStatement(updateSql);
                          updateStatement.setInt(1, mevcutAltinDB + koyulacakMiktar);
                          updateStatement.setString(2, oyuncuIsmi);
                          updateStatement.executeUpdate();
                       } else {
                          koyulacakMiktar = Math.min(miktar, keseLimiti);
                          String insertSql = "INSERT INTO keseler (oyuncu_ismi, kesedeki_altin) VALUES (?, ?)";
                          PreparedStatement insertStatement = connection.prepareStatement(insertSql);
                          insertStatement.setString(1, oyuncuIsmi);
                          insertStatement.setInt(2, koyulacakMiktar);
                          insertStatement.executeUpdate();
                       }

                       player.getInventory().removeItem(new ItemStack[]{new ItemStack(Material.GOLD_INGOT, koyulacakMiktar)});
                       player.sendTitle(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarili")), ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("koy_mesajlari.altin_eklendi_basarili")), 5, 100, 20);
                       player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                    } else {
                       this.plugin.getLogger().severe("Veritabanı bağlantısı kurulamadı.");
                    }
                 } catch (SQLException var15) {
                    this.plugin.getLogger().severe("MySQL'e kayıt yapılırken bir hata oluştu: " + var15.getMessage());
                 }
              } else {
                 koyulacakMiktar = Math.min(miktar, keseLimiti - mevcutAltin);
                 keselerConfig.set("keseler." + oyuncuIsmi + ".kesedeki_altin", mevcutAltin + koyulacakMiktar);
                 this.plugin.saveKeselerFile();
                 player.getInventory().removeItem(new ItemStack[]{new ItemStack(Material.GOLD_INGOT, koyulacakMiktar)});
                 player.sendTitle(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarili")), ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("koy_mesajlari.altin_eklendi_basarili")), 5, 100, 20);
                 player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
              }
           } else {
              player.sendTitle(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("koy_mesajlari.limit_asildi")), 5, 100, 20);
              player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
           }
        } else {
           player.sendTitle(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("koy_mesajlari.yetersiz_altin")), 5, 100, 20);
           player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
        }

     }
    
    private int getKesedekiAltin(Player player) {
        int altinMiktari = 0;
        String sql;
        if (this.plugin.getConfig().getBoolean("mysql-enabled")) {
           try {
              Connection connection = this.plugin.getConnection();
              if (connection != null) {
                 sql = "SELECT kesedeki_altin FROM keseler WHERE oyuncu_ismi = ?";
                 PreparedStatement statement = connection.prepareStatement(sql);
                 statement.setString(1, player.getName());
                 ResultSet result = statement.executeQuery();
                 if (result.next()) {
                    altinMiktari = result.getInt("kesedeki_altin");
                 }
              } else {
                 this.plugin.getLogger().severe("Veritabanı bağlantısı kurulamadı.");
              }
           } catch (SQLException e) {
              this.plugin.getLogger().severe("MySQL'e kayıt yapılırken bir hata oluştu: " + e.getMessage());
           }
        } else {
           FileConfiguration keselerConfig = this.plugin.getKeselerConfig();
           sql = player.getName();
           altinMiktari = keselerConfig.getInt("keseler." + sql + ".kesedeki_altin", 0);
        }

        return altinMiktari;
     }
}
