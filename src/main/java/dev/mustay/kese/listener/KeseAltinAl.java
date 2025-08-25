package dev.mustay.kese.listener;

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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.lone.itemsadder.api.CustomStack;
import dev.mustay.kese.Kese;
import io.th0rgal.oraxen.api.OraxenItems;
import me.arcaniax.hdb.api.HeadDatabaseAPI;

public class KeseAltinAl implements Listener {
	
	private final Kese plugin;
                                      
    public KeseAltinAl(Kese plugin) {
        this.plugin = plugin;         
    }
    public void openKeseAltinAlMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, plugin.getConfig().getInt("menuler.al_menu"), ChatColor.translateAlternateColorCodes('&', plugin.getMesajlarConfig().getString("menu_baslik.kese_altin_al")));

        ItemStack miktarGirerekAl = createMenuItem("miktar_girerek");
        List<Integer> miktarGirerekAlSlots = plugin.getMesajlarConfig().getIntegerList("menu_altin_al.miktar_girerek.slots");
        for (int slot : miktarGirerekAlSlots) {
            menu.setItem(slot, miktarGirerekAl);
        }

        ItemStack tamamınıAl = createMenuItem("tamamini");
        List<Integer> tamaminiAlSlots = plugin.getMesajlarConfig().getIntegerList("menu_altin_al.tamamini.slots");
        for (int slot : tamaminiAlSlots) {
            menu.setItem(slot, tamamınıAl);
        }
        
        player.openInventory(menu);
    }

    private ItemStack createMenuItem(String configKey) {
        String itemId = this.plugin.getMesajlarConfig().getString("menu_altin_al." + configKey + ".material");
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
        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("menu_altin_al." + configKey + ".name")));
        List<String> lore = this.plugin.getMesajlarConfig().getStringList("menu_altin_al." + configKey + ".lore");
        List<String> loreCvr = new ArrayList<>();
        for (String loreMsg : lore) {
            loreCvr.add(ChatColor.translateAlternateColorCodes('&', loreMsg));
        }

        itemMeta.setLore(loreCvr);
        item.setItemMeta(itemMeta);
        return item;
     }
    
    public void startChatInputListener(final Player player, boolean isAlis) {
        player.sendTitle(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("al_title_mesaj.miktar_gir")), ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("al_title_mesaj.miktar_gir_lore")), 10, 100, 20);
        Bukkit.getPluginManager().registerEvents(new Listener() {
           FileConfiguration keselerConfig;
           String oyuncuIsmi;

           {
              this.keselerConfig = KeseAltinAl.this.plugin.getKeselerConfig();
              this.oyuncuIsmi = player.getName();
           }

           @EventHandler
           public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
              if (event.getPlayer().equals(player)) {
                 event.setCancelled(true);
                 player.sendTitle(ChatColor.translateAlternateColorCodes('&', KeseAltinAl.this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', KeseAltinAl.this.plugin.getMesajlarConfig().getString("kese_genel.islem_sonlandirildi")), 5, 100, 20);
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
                 if (mesaj.matches("-?\\d+")) {
                    try {
                       int miktar = Integer.parseInt(mesaj);
                       if (mesaj.startsWith("/")) {
                          player.sendTitle(ChatColor.translateAlternateColorCodes('&', KeseAltinAl.this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', KeseAltinAl.this.plugin.getMesajlarConfig().getString("kese_genel.islem_sonlandirildi")), 5, 100, 20);
                          player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
                          AsyncPlayerChatEvent.getHandlerList().unregister(this);
                          return;
                       }

                       if (miktar <= 0) {
                          player.sendTitle(ChatColor.translateAlternateColorCodes('&', KeseAltinAl.this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', KeseAltinAl.this.plugin.getMesajlarConfig().getString("kese_genel.pozitif_sayi")), 5, 100, 20);
                          player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
                       } else {
                          int mevcutKeseAltini = this.keselerConfig.getInt("keseler." + this.oyuncuIsmi + ".kesedeki_altin");
                          if (KeseAltinAl.this.plugin.getConfig().getBoolean("mysql-enabled")) {
                             try {
                                String sql = "SELECT kesedeki_altin FROM keseler WHERE oyuncu_ismi = ?";
                                PreparedStatement statement = KeseAltinAl.this.plugin.getConnection().prepareStatement(sql);
                                statement.setString(1, player.getName());
                                ResultSet result = statement.executeQuery();
                                if (result.next()) {
                                   mevcutKeseAltini = result.getInt("kesedeki_altin");
                                }
                             } catch (SQLException e) {
                                KeseAltinAl.this.plugin.getLogger().severe("MySQL'e kayıt yapılırken bir hata oluştu: " + e.getMessage());
                                return;
                             }
                          } else {
                             mevcutKeseAltini = this.keselerConfig.getInt("keseler." + this.oyuncuIsmi + ".kesedeki_altin");
                          }

                          if (mevcutKeseAltini < miktar) {
                             player.sendTitle(ChatColor.translateAlternateColorCodes('&', KeseAltinAl.this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', KeseAltinAl.this.plugin.getMesajlarConfig().getString("al_mesajlari.miktar_yetersiz_altin")), 5, 100, 20);
                             player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
                          } else {
                             int kalanMiktar = miktar;
                             int toplamBosYer = 0;
                             int i = 0;

                             while(true) {
                                ItemStack item;
                                if (i >= 36) {
                                   if (toplamBosYer < miktar) {
                                      player.sendTitle(ChatColor.translateAlternateColorCodes('&', KeseAltinAl.this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', KeseAltinAl.this.plugin.getMesajlarConfig().getString("al_mesajlari.envanter_dolu")), 5, 100, 20);
                                      player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
                                      break;
                                   }

                                   for(i = 0; i < 36; ++i) {
                                      item = player.getInventory().getItem(i);
                                      int eklemeMiktari;
                                      if (item != null && item.getType() != Material.AIR) {
                                         if (item.getType() == Material.GOLD_INGOT) {
                                            eklemeMiktari = Math.min(kalanMiktar, 64 - item.getAmount());
                                            item.setAmount(item.getAmount() + eklemeMiktari);
                                            kalanMiktar -= eklemeMiktari;
                                            if (kalanMiktar <= 0) {
                                               break;
                                            }
                                         }
                                      } else {
                                         eklemeMiktari = Math.min(kalanMiktar, 64);
                                         ItemStack altin = new ItemStack(Material.GOLD_INGOT, eklemeMiktari);
                                         player.getInventory().setItem(i, altin);
                                         kalanMiktar -= eklemeMiktari;
                                         if (kalanMiktar <= 0) {
                                            break;
                                         }
                                      }
                                   }

                                   if (KeseAltinAl.this.plugin.getConfig().getBoolean("mysql-enabled")) {
                                      try {
                                         String sqlx = "UPDATE keseler SET kesedeki_altin = kesedeki_altin - ? WHERE oyuncu_ismi = ?";
                                         PreparedStatement statementx = KeseAltinAl.this.plugin.getConnection().prepareStatement(sqlx);
                                         statementx.setInt(1, miktar);
                                         statementx.setString(2, player.getName());
                                         statementx.executeUpdate();
                                      } catch (SQLException e) {
                                         KeseAltinAl.this.plugin.getLogger().severe("MySQL'e kayıt yapılırken bir hata oluştu: " + e.getMessage());
                                      }
                                   } else {
                                      this.keselerConfig.set("keseler." + this.oyuncuIsmi + ".kesedeki_altin", mevcutKeseAltini - miktar);
                                      KeseAltinAl.this.plugin.saveKeselerFile();
                                   }

                                   player.sendTitle(ChatColor.translateAlternateColorCodes('&', KeseAltinAl.this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarili")), ChatColor.translateAlternateColorCodes('&', KeseAltinAl.this.plugin.getMesajlarConfig().getString("al_mesajlari.altin_alindi_basarili")), 5, 100, 20);
                                   player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                                   break;
                                }

                                item = player.getInventory().getItem(i);
                                if (item != null && item.getType() != Material.AIR) {
                                   if (item.getType() == Material.GOLD_INGOT) {
                                      toplamBosYer += 64 - item.getAmount();
                                   }
                                } else {
                                   toplamBosYer += 64;
                                }

                                ++i;
                             }
                          }
                       }
                    } catch (NumberFormatException e) {
                       player.sendTitle(ChatColor.translateAlternateColorCodes('&', KeseAltinAl.this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', KeseAltinAl.this.plugin.getMesajlarConfig().getString("kese_genel.islem_sonlandirildi")), 5, 100, 20);
                       player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
                    }

                    AsyncPlayerChatEvent.getHandlerList().unregister(this);
                    PlayerCommandPreprocessEvent.getHandlerList().unregister(this);
                 } else {
                    player.sendTitle(ChatColor.translateAlternateColorCodes('&', KeseAltinAl.this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', KeseAltinAl.this.plugin.getMesajlarConfig().getString("kese_genel.sayisal_deger_hata")), 5, 100, 20);
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
                 }
              }

           }
        }, this.plugin);
     }

    public void altinAl(Player player) {
        FileConfiguration keselerConfig = this.plugin.getKeselerConfig();
        String oyuncuIsmi = player.getName();
        int mevcutKeseAltini = keselerConfig.getInt("keseler." + oyuncuIsmi + ".kesedeki_altin", 0);
        if (this.plugin.getConfig().getBoolean("mysql-enabled")) {
           try {
              String sql = "SELECT kesedeki_altin FROM keseler WHERE oyuncu_ismi = ?";
              PreparedStatement statement = this.plugin.getConnection().prepareStatement(sql);
              statement.setString(1, player.getName());
              ResultSet result = statement.executeQuery();
              if (result.next()) {
                 mevcutKeseAltini = result.getInt("kesedeki_altin");
              }
           } catch (SQLException var12) {
              this.plugin.getLogger().severe("MySQL'den veri alınırken bir hata oluştu: " + var12.getMessage());
              return;
           }
        }

        if (mevcutKeseAltini > 0) {
           int kalanMiktar = mevcutKeseAltini;
           boolean envanterDolu = true;

           for(int i = 0; i < 36; ++i) {
              ItemStack item = player.getInventory().getItem(i);
              if (item == null || item.getType() == Material.AIR) {
                 int eklemeMiktari = Math.min(kalanMiktar, 64);
                 ItemStack altin = new ItemStack(Material.GOLD_INGOT, eklemeMiktari);
                 player.getInventory().setItem(i, altin);
                 kalanMiktar -= eklemeMiktari;
                 envanterDolu = false;
                 if (kalanMiktar <= 0) {
                    break;
                 }
              }
           }

           if (envanterDolu) {
              player.sendTitle(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("al_mesajlari.envanter_dolu")), 5, 100, 20);
              player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
              return;
           }

           if (this.plugin.getConfig().getBoolean("mysql-enabled")) {
              try {
                 String sql = "UPDATE keseler SET kesedeki_altin = ? WHERE oyuncu_ismi = ?";
                 PreparedStatement statement = this.plugin.getConnection().prepareStatement(sql);
                 statement.setInt(1, kalanMiktar);
                 statement.setString(2, player.getName());
                 statement.executeUpdate();
              } catch (SQLException e) {
                 this.plugin.getLogger().severe("MySQL'den veri silinirken bir hata oluştu: " + e.getMessage());
                 return;
              }
           } else {
              keselerConfig.set("keseler." + oyuncuIsmi + ".kesedeki_altin", kalanMiktar);
              this.plugin.saveKeselerFile();
           }

           if (kalanMiktar > 0) {
              player.sendMessage(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("al_mesajlari.kalan_altin").replace("{kalan}", String.valueOf(kalanMiktar))));
           } else {
              player.sendTitle(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarili")), ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("al_mesajlari.altin_alindi_basarili")), 5, 100, 20);
              player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
           }
        } else {
           player.sendTitle(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("al_mesajlari.tamamini_yetersiz_altin")), 5, 100, 20);
           player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
        }

     }
}