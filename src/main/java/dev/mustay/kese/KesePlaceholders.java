package dev.mustay.kese;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class KesePlaceholders extends PlaceholderExpansion {
	
    private Kese plugin;
    public KesePlaceholders(Kese plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "kese";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) {
            return "";
        }

        if (params.equalsIgnoreCase("altin")) {
            int altinMiktari = 0;
            if (plugin.getConfig().getBoolean("mysql-enabled")) {
                try {
                    String sql = "SELECT kesedeki_altin FROM keseler WHERE oyuncu_ismi = ?";
                    PreparedStatement statement = plugin.getConnection().prepareStatement(sql);
                    statement.setString(1, player.getName());
                    ResultSet result = statement.executeQuery();
                    if (result.next()) {
                        altinMiktari = result.getInt("kesedeki_altin");
                    }
                } catch (SQLException e) {
                    plugin.getLogger().severe("MySQL'den veri alınırken bir hata oluştu: " + e.getMessage());
                }
            } else {
                FileConfiguration keselerConfig = plugin.getKeselerConfig();
                altinMiktari = keselerConfig.getInt("keseler." + player.getName() + ".kesedeki_altin", 0);
            }
            return String.valueOf(altinMiktari);
        }

        if (params.equalsIgnoreCase("limit")) {
            return String.valueOf(plugin.getConfig().getInt("kese_limiti"));
        }

        return null;
    }
}

