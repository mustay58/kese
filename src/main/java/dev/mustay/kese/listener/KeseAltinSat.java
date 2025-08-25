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
import net.milkbowl.vault.economy.Economy;

public class KeseAltinSat implements Listener {
	
    private final Kese plugin;
    private Economy econ;
    
    public KeseAltinSat(Kese plugin) {
        this.plugin = plugin;
        this.econ = Bukkit.getServer().getServicesManager().getRegistration(Economy.class).getProvider();
    }
    
    public void openKeseAltinSatMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, this.plugin.getConfig().getInt("menuler.sat_menu"), ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("menu_baslik.kese_altin_sat")));
        ItemStack miktarGirerekSat = this.createMenuItem("miktar_girerek");
        List<Integer> miktarGirerekSatSlots = this.plugin.getMesajlarConfig().getIntegerList("menu_altin_sat.miktar_girerek.slots");
        for (int slot : miktarGirerekSatSlots) {
            menu.setItem(slot, miktarGirerekSat);
        }
        
        ItemStack tamaminiSat = this.createMenuItem("tamamini");
        List<Integer> tamaminiSatSlots = this.plugin.getMesajlarConfig().getIntegerList("menu_altin_sat.tamamini.slots");
        for (int slot : tamaminiSatSlots) {
            menu.setItem(slot, tamaminiSat);
        }

        player.openInventory(menu);
     }

    
    private ItemStack createMenuItem(String configKey) {
        String itemId = this.plugin.getMesajlarConfig().getString("menu_altin_sat." + configKey + ".material");
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
        } catch (Exception e) {
           this.plugin.getLogger().severe("Item oluşturulurken bir hata oluştu: " + e.getMessage());
           item = new ItemStack(Material.BARRIER);
        }

        if (item == null) {
           item = new ItemStack(Material.BARRIER);
        }

        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("menu_altin_sat." + configKey + ".name")));
        List<String> lore = this.plugin.getMesajlarConfig().getStringList("menu_altin_sat." + configKey + ".lore");
        List<String> loreCvr = new ArrayList<>();
        for (String loreMsg : lore) {
            loreCvr.add(ChatColor.translateAlternateColorCodes('&', loreMsg));
        }

        itemMeta.setLore(loreCvr);
        item.setItemMeta(itemMeta);
        return item;
     }


    public void startChatInputListenerSatma(final Player player) {
        player.sendTitle(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("sat_title_mesaj.miktar_gir")), ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("sat_title_mesaj.miktar_gir_lore")), 10, 100, 20);
        Bukkit.getPluginManager().registerEvents(new Listener() {
           @EventHandler
           public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
              if (event.getPlayer().equals(player)) {
                 event.setCancelled(true);
                 player.sendTitle(ChatColor.translateAlternateColorCodes('&', KeseAltinSat.this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', KeseAltinSat.this.plugin.getMesajlarConfig().getString("kese_genel.islem_sonlandirildi")), 5, 100, 20);
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
                          KeseAltinSat.this.satinAltin(player, miktar);
                       } else {
                          player.sendTitle(ChatColor.translateAlternateColorCodes('&', KeseAltinSat.this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', KeseAltinSat.this.plugin.getMesajlarConfig().getString("genel.sayisal_deger_hata")), 5, 100, 20);
                          player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
                       }
                    } else {
                       player.sendTitle(ChatColor.translateAlternateColorCodes('&', KeseAltinSat.this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', KeseAltinSat.this.plugin.getMesajlarConfig().getString("kese_genel.pozitif_sayi")), 5, 100, 20);
                       player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
                    }
                 } catch (NumberFormatException e) {
                    player.sendTitle(ChatColor.translateAlternateColorCodes('&', KeseAltinSat.this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', KeseAltinSat.this.plugin.getMesajlarConfig().getString("kese_genel.islem_sonlandirildi")), 5, 100, 20);
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
                 } finally {
                    AsyncPlayerChatEvent.getHandlerList().unregister(this);
                    PlayerCommandPreprocessEvent.getHandlerList().unregister(this);
                 }
              }

           }
        }, this.plugin);
     }
    
    private void satinAltin(Player player, int miktar) {
        int mevcutKeseAltini = 0;
        String sql;
        PreparedStatement statement;
        FileConfiguration keselerConfig;
        if (this.plugin.getConfig().getBoolean("mysql-enabled")) {
           try {
              sql = "SELECT kesedeki_altin FROM keseler WHERE oyuncu_ismi = ?";
              statement = this.plugin.getConnection().prepareStatement(sql);
              statement.setString(1, player.getName());
              ResultSet result = statement.executeQuery();
              if (result.next()) {
                 mevcutKeseAltini = result.getInt("kesedeki_altin");
              }
           } catch (SQLException e) {
              this.plugin.getLogger().severe("MySQL'den veri alınırken bir hata oluştu: " + e.getMessage());
              return;
           }
        } else {
           keselerConfig = this.plugin.getKeselerConfig();
           mevcutKeseAltini = keselerConfig.getInt("keseler." + player.getName() + ".kesedeki_altin", 0);
        }

        if (mevcutKeseAltini >= miktar) {
           if (this.plugin.getConfig().getBoolean("mysql-enabled")) {
              try {
                 sql = "UPDATE keseler SET kesedeki_altin = kesedeki_altin - ? WHERE oyuncu_ismi = ?";
                 statement = this.plugin.getConnection().prepareStatement(sql);
                 statement.setInt(1, miktar);
                 statement.setString(2, player.getName());
                 statement.executeUpdate();
              } catch (SQLException e) {
                 this.plugin.getLogger().severe("MySQL'e kayıt yapılırken bir hata oluştu: " + e.getMessage());
                 return;
              }
           } else {
              keselerConfig = this.plugin.getKeselerConfig();
              keselerConfig.set("keseler." + player.getName() + ".kesedeki_altin", mevcutKeseAltini - miktar);
              this.plugin.saveKeselerFile();
           }

           int kulceFiyati = this.plugin.getConfig().getInt("kulce_fiyati");
           this.econ.depositPlayer(player, (double)(miktar * kulceFiyati));
           player.sendTitle(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarili")), 
        		   ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("sat_mesajlari.miktar_altin_sat").replace("{miktar}", String.valueOf(miktar)).replace("{fiyat}", String.valueOf(miktar * kulceFiyati))), 5, 100, 20);
           player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
        } else {
           player.sendTitle(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), 
        		   ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("sat_mesajlari.yetersiz_altin")), 5, 100, 20);
           player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
        }

     }
    
    public void tamaminiSat(Player player) {
        int mevcutKeseAltini = 0;
        String sql;
        PreparedStatement statement;
        FileConfiguration keselerConfig;
        if (this.plugin.getConfig().getBoolean("mysql-enabled")) {
           try {
              sql = "SELECT kesedeki_altin FROM keseler WHERE oyuncu_ismi = ?";
              statement = this.plugin.getConnection().prepareStatement(sql);
              statement.setString(1, player.getName());
              ResultSet result = statement.executeQuery();
              if (result.next()) {
                 mevcutKeseAltini = result.getInt("kesedeki_altin");
              }
           } catch (SQLException e) {
              this.plugin.getLogger().severe("MySQL'den veri alınırken bir hata oluştu: " + e.getMessage());
              return;
           }
        } else {
           keselerConfig = this.plugin.getKeselerConfig();
           mevcutKeseAltini = keselerConfig.getInt("keseler." + player.getName() + ".kesedeki_altin", 0);
        }

        if (mevcutKeseAltini > 0) {
           if (this.plugin.getConfig().getBoolean("mysql-enabled")) {
              try {
                 sql = "UPDATE keseler SET kesedeki_altin = 0 WHERE oyuncu_ismi = ?";
                 statement = this.plugin.getConnection().prepareStatement(sql);
                 statement.setString(1, player.getName());
                 statement.executeUpdate();
              } catch (SQLException e) {
                 this.plugin.getLogger().severe("MySQL'e kayıt yapılırken bir hata oluştu: " + e.getMessage());
                 return;
              }
           } else {
              keselerConfig = this.plugin.getKeselerConfig();
              keselerConfig.set("keseler." + player.getName() + ".kesedeki_altin", 0);
              this.plugin.saveKeselerFile();
           }

           int kulceFiyati = this.plugin.getConfig().getInt("kulce_fiyati");
           this.econ.depositPlayer(player, (double)(mevcutKeseAltini * kulceFiyati));
           player.sendTitle(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarili")), ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("sat_mesajlari.tamamini_sat").replace("{miktar}", String.valueOf(mevcutKeseAltini)).replace("{fiyat}", String.valueOf(mevcutKeseAltini * kulceFiyati))), 5, 100, 20);
           player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
        } else {
           player.sendTitle(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("kese_genel.islem_basarisiz")), ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("sat_mesajlari.yetersiz_altin")), 5, 100, 20);
           player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
        }

     }
}
