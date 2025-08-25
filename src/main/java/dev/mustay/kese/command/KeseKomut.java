package dev.mustay.kese.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;

import dev.lone.itemsadder.api.CustomStack;
import dev.mustay.kese.Kese;
import dev.mustay.kese.listener.KeseAltinAl;
import dev.mustay.kese.listener.KeseAltinGonder;
import dev.mustay.kese.listener.KeseAltinKoy;
import dev.mustay.kese.listener.KeseAltinSat;
import io.th0rgal.oraxen.api.OraxenItems;
import me.arcaniax.hdb.api.HeadDatabaseAPI;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class KeseKomut implements CommandExecutor, Listener {

    private final Kese plugin;
    
    public KeseKomut(Kese plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission("m.kese.oyuncu")) {
					openMenu(player);
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMesajlarConfig().getString("kese_genel.yetersiz_yetki")));
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F);
            }
        } else {
            sender.sendMessage("Bu komut sadece oyuncular tarafından kullanılabilir.");
        }
        return true;
    }

    private void openMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, plugin.getConfig().getInt("menuler.kese_menu"), ChatColor.translateAlternateColorCodes('&', plugin.getMesajlarConfig().getString("menu_baslik.kese_baslik")));
        
        ItemStack keseAltınKoy = createMenuItem("altin_koy");
        List<Integer> altinKoySlots = plugin.getMesajlarConfig().getIntegerList("menu_items.altin_koy.slots");
        for (int slot : altinKoySlots) {
            menu.setItem(slot, keseAltınKoy);
        }

        ItemStack keseAltinAl = createMenuItem("altin_al");
        List<Integer> altinAlSlots = plugin.getMesajlarConfig().getIntegerList("menu_items.altin_al.slots");
        for (int slot : altinAlSlots) {
            menu.setItem(slot, keseAltinAl);
        }

        ItemStack keseAltinGonder = createMenuItem("altin_gonder");
        List<Integer> altinGonderSlots = plugin.getMesajlarConfig().getIntegerList("menu_items.altin_gonder.slots");
        for (int slot : altinGonderSlots) {
            menu.setItem(slot, keseAltinGonder);
        }

        ItemStack keseAltınSat = createMenuItem("altin_sat");
        List<Integer> altinSatSlots = plugin.getMesajlarConfig().getIntegerList("menu_items.altin_sat.slots");
        for (int slot : altinSatSlots) {
            menu.setItem(slot, keseAltınSat);
        }
        
        ItemStack keseAltın = createMenuItem("kesedeki_altin", getKesedekiAltin(player));
        List<Integer> kesedekiAltinSlots = plugin.getMesajlarConfig().getIntegerList("menu_items.kesedeki_altin.slots");
        for (int slot : kesedekiAltinSlots) {
            menu.setItem(slot, keseAltın);
        }
        
        player.openInventory(menu);
    }
    

    private ItemStack createMenuItem(String configKey) {
        String itemId = this.plugin.getMesajlarConfig().getString("menu_items." + configKey + ".material");
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
        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("menu_items." + configKey + ".name")));
        List<String> lore = this.plugin.getMesajlarConfig().getStringList("menu_items." + configKey + ".lore");
        List<String> loreCvr = new ArrayList<>();
        for (String loreMsg : lore) {
            loreCvr.add(ChatColor.translateAlternateColorCodes('&', loreMsg));
        }

        itemMeta.setLore(loreCvr);
        item.setItemMeta(itemMeta);
        return item;
     }
    
    public <T> T getProvider(Class<T> classz) {
        RegisteredServiceProvider<T> provider = this.plugin.getServer().getServicesManager().getRegistration(classz);
        return provider == null ? null : provider.getProvider();
     }

    private ItemStack createMenuItem(String configKey, int altinMiktari) {
        String itemId = this.plugin.getMesajlarConfig().getString("menu_items." + configKey + ".material");
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
                    this.plugin.getLogger().severe("Bir ItemsAdder veya Oraxen materyali kullanmaya çalışıyorsan config.yml dosyasından aktif et.");
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
        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("menu_items." + configKey + ".name")));
        List<String> lore = this.plugin.getMesajlarConfig().getStringList("menu_items." + configKey + ".lore");
        List<String> loreCvr = new ArrayList<>();
        int limit = this.plugin.getConfig().getInt("kese_limiti");
        for (String loreMsg : lore) {
            loreCvr.add(ChatColor.translateAlternateColorCodes('&', loreMsg.replace("{altin}", String.valueOf(altinMiktari)).replace("{limit}", String.valueOf(limit))));
        }

        itemMeta.setLore(loreCvr);
        item.setItemMeta(itemMeta);
        return item;
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

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
       KeseAltinKoy keseAltinKoy = new KeseAltinKoy(this.plugin);
       KeseAltinAl keseAltinAl = new KeseAltinAl(this.plugin);
       KeseAltinGonder keseAltinGonder = new KeseAltinGonder(this.plugin);
       KeseAltinSat keseAltinSat = new KeseAltinSat(this.plugin);
       Player player = (Player)event.getWhoClicked();
       if (event.getView().getTitle().equals(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("menu_baslik.kese_baslik")))) {
          this.handleMenuItem(event, player, "altin_koy", () -> {
             keseAltinKoy.openKeseAltınKoyMenu(player);
          });
          this.handleMenuItem(event, player, "altin_al", () -> {
             keseAltinAl.openKeseAltinAlMenu(player);
          });
          this.handleMenuItem(event, player, "altin_gonder", () -> {
             player.closeInventory();
             keseAltinGonder.openOyuncuListesiMenu(player, 1);
          });
          this.handleMenuItem(event, player, "altin_sat", () -> {
             player.closeInventory();
             keseAltinSat.openKeseAltinSatMenu(player);
          });
          this.handleMenuItem(event, player, "kesedeki_altin", () -> {
             event.setCancelled(true);
          });
       }

       if (event.getView().getTitle().equals(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("menu_baslik.kese_altin_koy")))) {
          this.handleMenuItemKoy(event, player, "miktar_girerek", () -> {
             player.closeInventory();
             keseAltinKoy.startChatInputListener(player);
          });
          this.handleMenuItemKoy(event, player, "tamamini", () -> {
              player.closeInventory();
              int miktar = 0;
              for (ItemStack item : player.getInventory().getContents()) {
                  if (item != null && item.getType() == Material.GOLD_INGOT) {
                      miktar += item.getAmount();
                  }
              }
             keseAltinKoy.altinKoy(player, miktar);
          });
       }

       if (event.getView().getTitle().equals(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("menu_baslik.kese_altin_al")))) {
          this.handleMenuItemAl(event, player, "miktar_girerek", () -> {
             player.closeInventory();
             keseAltinAl.startChatInputListener(player, true);
          });
          this.handleMenuItemAl(event, player, "tamamini", () -> {
             player.closeInventory();
             keseAltinAl.altinAl(player);
          });
       }

       if (event.getView().getTitle().equals(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("gonder_oyuncu_listesi.name"))) && event.getSlotType() == SlotType.CONTAINER && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
          event.setCancelled(true);
          Player hedefOyuncu = Bukkit.getPlayer(event.getCurrentItem().getItemMeta().getDisplayName());
          player.closeInventory();
          keseAltinGonder.startChatInputListenerGonderme(player, hedefOyuncu);
       }

       if (event.getView().getTitle().equals(ChatColor.translateAlternateColorCodes('&', this.plugin.getMesajlarConfig().getString("menu_baslik.kese_altin_sat")))) {
          this.handleMenuItemSat(event, player, "miktar_girerek", () -> {
             player.closeInventory();
             keseAltinSat.startChatInputListenerSatma(player);
          });
          this.handleMenuItemSat(event, player, "tamamini", () -> {
             player.closeInventory();
             keseAltinSat.tamaminiSat(player);
          });
          if (event.getSlotType() == SlotType.CONTAINER) {
             event.setCancelled(true);
          }
       }

    }
    
    private void handleMenuItem(InventoryClickEvent event, Player player, String configKey, Runnable action) {
        List<Integer> slots = this.plugin.getMesajlarConfig().getIntegerList("menu_items." + configKey + ".slots");
        if (slots.contains(event.getSlot())) {
           event.setCancelled(true);
           action.run();
        }

     }

     private void handleMenuItemKoy(InventoryClickEvent event, Player player, String configKey, Runnable action) {
        List<Integer> slots = this.plugin.getMesajlarConfig().getIntegerList("menu_altin_koy." + configKey + ".slots");
        if (slots.contains(event.getSlot())) {
           event.setCancelled(true);
           action.run();
        }

     }

     private void handleMenuItemAl(InventoryClickEvent event, Player player, String configKey, Runnable action) {
        List<Integer> slots = this.plugin.getMesajlarConfig().getIntegerList("menu_altin_al." + configKey + ".slots");
        if (slots.contains(event.getSlot())) {
           event.setCancelled(true);
           action.run();
        }

     }

     private void handleMenuItemSat(InventoryClickEvent event, Player player, String configKey, Runnable action) {
        List<Integer> slots = this.plugin.getMesajlarConfig().getIntegerList("menu_altin_sat." + configKey + ".slots");
        if (slots.contains(event.getSlot())) {
           event.setCancelled(true);
           action.run();
        }

     }
}