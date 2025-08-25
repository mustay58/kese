package dev.mustay.kese.command;

import dev.lone.itemsadder.api.CustomStack;
import dev.mustay.kese.Kese;
import dev.mustay.kese.adminlistener.KeseAdminAltinAl;
import dev.mustay.kese.adminlistener.KeseAdminAltinGonder;
import dev.mustay.kese.adminlistener.KeseAdminAltinSifirla;
import io.th0rgal.oraxen.api.OraxenItems;
import me.arcaniax.hdb.api.HeadDatabaseAPI;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class KeseAdmin implements CommandExecutor, Listener {

    private final Kese plugin;

    public KeseAdmin(Kese plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Bu komut sadece oyuncular tarafından kullanılabilir.");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("keseadmin")) {
            if (player.hasPermission("m.kese.admin")) {
                if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                    plugin.reloadConfig();
                    try {
						plugin.reloadKeselerFile();
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                    try {
						plugin.reloadMesajlarFile();
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                    try {
						plugin.reloadAdminFile();
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                    player.sendMessage(ChatColor.GREEN + "Kese eklentisi yeniden yüklendi!");
                    return true;
                } else {
                    openAdminMenu(player);
                }
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getAdminConfig().getString("kese_genel.yetersiz_yetki")));
            }
            return true;
        }
        return false;
    }
    private void openAdminMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, plugin.getConfig().getInt("menuler.kese_admin_menu"), ChatColor.translateAlternateColorCodes('&', plugin.getAdminConfig().getString("admimenu_baslik.kese_admin_baslik")));

        ItemStack keseAltinGonder = createMenuItem("altin_gonder");
        List<Integer> altinGonderSlots = plugin.getAdminConfig().getIntegerList("adminmenu_items.altin_gonder.slots");
        for (int slot : altinGonderSlots) {
            menu.setItem(slot, keseAltinGonder);
        }

        ItemStack keseAltinAl = createMenuItem("altin_al");
        List<Integer> altinAlSlots = plugin.getAdminConfig().getIntegerList("adminmenu_items.altin_al.slots");
        for (int slot : altinAlSlots) {
            menu.setItem(slot, keseAltinAl);
        }

        ItemStack keseyiSifirla = createMenuItem("altin_sifirla");
        List<Integer> altinSifirlaSlots = plugin.getAdminConfig().getIntegerList("adminmenu_items.altin_sifirla.slots");
        for (int slot : altinSifirlaSlots) {
            menu.setItem(slot, keseyiSifirla);
        }
        
        player.openInventory(menu);
    }

    
    private ItemStack createMenuItem(String configKey) {
        String itemId = this.plugin.getAdminConfig().getString("adminmenu_items." + configKey + ".material");
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
                    this.plugin.getLogger().severe("Bir ItemsAdder veya Oraxen materyali kullanmaya çalışıyorsan config.yml dosyasından aktif et.");
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
        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', this.plugin.getAdminConfig().getString("adminmenu_items." + configKey + ".name")));
        List<String> lore = this.plugin.getAdminConfig().getStringList("adminmenu_items." + configKey + ".lore");
        List<String> loreCvr = new ArrayList<>();
        for (String loreMsg : lore) {
            loreCvr.add(ChatColor.translateAlternateColorCodes('&', loreMsg));
        }
        itemMeta.setLore(loreCvr);
        item.setItemMeta(itemMeta);
        return item;
     }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
       KeseAdminAltinSifirla keseAltinSifila = new KeseAdminAltinSifirla(this.plugin);
       KeseAdminAltinAl keseAdminAltinAl = new KeseAdminAltinAl(this.plugin);
       KeseAdminAltinGonder keseAdminAltinGonder = new KeseAdminAltinGonder(this.plugin);
       Player player = (Player)event.getWhoClicked();
       if (event.getView().getTitle().equals(ChatColor.translateAlternateColorCodes('&', this.plugin.getAdminConfig().getString("admimenu_baslik.kese_admin_baslik")))) {
          this.handleMenuItem(event, player, "altin_al", () -> {
             player.closeInventory();
             keseAdminAltinAl.openOyuncuListesiAdminMenu(player, 1);
          });
          this.handleMenuItem(event, player, "altin_gonder", () -> {
             player.closeInventory();
             keseAdminAltinGonder.openOyuncuListesiAdminMenu(player, 1);
          });
          this.handleMenuItem(event, player, "altin_sifirla", () -> {
             player.closeInventory();
             keseAltinSifila.openOyuncuListesiAdminMenu(player, 1);
          });
       }

       Player hedefOyuncu;
       if (event.getView().getTitle().equals(ChatColor.translateAlternateColorCodes('&', this.plugin.getAdminConfig().getString("al_admin_listesi.name"))) && event.getSlotType() == SlotType.CONTAINER && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
          event.setCancelled(true);
          hedefOyuncu = Bukkit.getPlayer(event.getCurrentItem().getItemMeta().getDisplayName());
          player.closeInventory();
          keseAdminAltinAl.startChatInputListenerAl(player, hedefOyuncu);
       }

       if (event.getView().getTitle().equals(ChatColor.translateAlternateColorCodes('&', this.plugin.getAdminConfig().getString("gonder_admin_listesi.name"))) && event.getSlotType() == SlotType.CONTAINER && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
          event.setCancelled(true);
          hedefOyuncu = Bukkit.getPlayer(event.getCurrentItem().getItemMeta().getDisplayName());
          player.closeInventory();
          keseAdminAltinGonder.startChatInputListenersGonderme(player, hedefOyuncu);
       }

    }
    
    
    private void handleMenuItem(InventoryClickEvent event, Player player, String configKey, Runnable action) {
        List<Integer> slots = plugin.getAdminConfig().getIntegerList("adminmenu_items." + configKey + ".slots");
        if (slots.contains(event.getSlot())) {
           event.setCancelled(true);
           action.run();
        }

     }
}