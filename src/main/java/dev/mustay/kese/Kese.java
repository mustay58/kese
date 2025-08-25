package dev.mustay.kese;

import dev.mustay.kese.adminlistener.KeseAdminAltinAl;
import dev.mustay.kese.adminlistener.KeseAdminAltinGonder;
import dev.mustay.kese.adminlistener.KeseAdminAltinSifirla;
import dev.mustay.kese.command.KeseAdmin;
import dev.mustay.kese.command.KeseKomut;
import dev.mustay.kese.listener.KeseAltinAl;
import dev.mustay.kese.listener.KeseAltinGonder;
import dev.mustay.kese.listener.KeseAltinKoy;
import dev.mustay.kese.listener.KeseAltinSat;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Kese extends JavaPlugin {
	
    private File mesajlarFile, keselerFile, adminFile;
    private FileConfiguration mesajlarConfig, keselerConfig, adminConfig;
    private Connection connection;

    @Override
    public void onEnable() {
    	getCommand("kese").setExecutor(new KeseKomut(this));
    	getCommand("keseadmin").setExecutor(new KeseAdmin(this));
    	
    	getServer().getPluginManager().registerEvents(new KeseKomut(this), this);
    	getServer().getPluginManager().registerEvents(new KeseAltinKoy(this), this);
    	getServer().getPluginManager().registerEvents(new KeseAltinAl(this), this);
    	getServer().getPluginManager().registerEvents(new KeseAltinGonder(this), this);
    	getServer().getPluginManager().registerEvents(new KeseAltinSat(this), this);
    	getServer().getPluginManager().registerEvents(new KeseAdmin(this), this);
    	getServer().getPluginManager().registerEvents(new KeseAdminAltinGonder(this), this);
    	getServer().getPluginManager().registerEvents(new KeseAdminAltinAl(this), this);
    	getServer().getPluginManager().registerEvents(new KeseAdminAltinSifirla(this), this);

    	createKeseFile();
    	createAdminFile();
        saveDefaultConfig();
        createMesajlarFile();
        if (getConfig().getBoolean("mysql-enabled")) {
        	database();
        }
        
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new KesePlaceholders(this).register();
        }
    }
    
    @Override
    public void onDisable() {
        saveConfig();
        saveAdminFile();
        saveKeselerFile();
        saveMesajlarFile();
        if (getConfig().getBoolean("mysql-enabled")) {
        baglantiKapat();
        }
    }

    private void createKeseFile() {
    	keselerFile = new File(getDataFolder(), "keseler.yml");
        if (!keselerFile.exists()) {
            saveResource("keseler.yml", false);
        }
        keselerConfig = YamlConfiguration.loadConfiguration(keselerFile);
    }
    
    private void createAdminFile() {
    	adminFile = new File(getDataFolder(), "adminmenu.yml");
        if (!adminFile.exists()) {
            saveResource("adminmenu.yml", false);
        }
        adminConfig = YamlConfiguration.loadConfiguration(adminFile);
    }
    
    private void createMesajlarFile() {
    	mesajlarFile = new File(getDataFolder(), "mesajlar.yml");
        if (!mesajlarFile.exists()) {
            saveResource("mesajlar.yml", false);
        }
        mesajlarConfig = YamlConfiguration.loadConfiguration(mesajlarFile);
    }
    
    public void saveKeselerFile() {
        try {
        	keselerConfig.save(keselerFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void saveAdminFile() {
        try {
        	adminConfig.save(adminFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void saveMesajlarFile() {
        try {
        	mesajlarConfig.save(mesajlarFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getMesajlarConfig() {
        return mesajlarConfig;
    }

    public FileConfiguration getKeselerConfig() {
        return keselerConfig;
    }
    
    public FileConfiguration getAdminConfig() {
        return adminConfig;
    }
    
    public File getKeselerFile() {
        return keselerFile;
    }

    public void reloadKeselerFile() throws UnsupportedEncodingException {
    	if (keselerFile == null) {
    		keselerFile = new File(getDataFolder(), "keseler.yml");
    	    }
    	keselerConfig = YamlConfiguration.loadConfiguration(keselerFile);

    	    Reader defConfigStream = new InputStreamReader(this.getResource("keseler.yml"), "UTF8");
    	    if (defConfigStream != null) {
    	        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
    	        keselerConfig.setDefaults(defConfig);
    	    }
	}
    
    public void reloadAdminFile() throws UnsupportedEncodingException {
    	if (adminFile == null) {
    		adminFile = new File(getDataFolder(), "adminmenu.yml");
    	    }
    	adminConfig = YamlConfiguration.loadConfiguration(adminFile);

    	    Reader defConfigStream = new InputStreamReader(this.getResource("adminmenu.yml"), "UTF8");
    	    if (defConfigStream != null) {
    	        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
    	        adminConfig.setDefaults(defConfig);
    	    }
	}
    
	public void reloadMesajlarFile() throws UnsupportedEncodingException {
		if (mesajlarFile == null) {
			mesajlarFile = new File(getDataFolder(), "mesajlar.yml");
		    }
		    mesajlarConfig = YamlConfiguration.loadConfiguration(mesajlarFile);

		    Reader defConfigStream = new InputStreamReader(this.getResource("mesajlar.yml"), "UTF8");
		    if (defConfigStream != null) {
		        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
		        mesajlarConfig.setDefaults(defConfig);
		    }
	}

	
	private void database() {
        String host = getConfig().getString("mysql.host");
        int port = getConfig().getInt("mysql.port");
        String database = getConfig().getString("mysql.database");
        String username = getConfig().getString("mysql.username");
        String password = getConfig().getString("mysql.password");

        try {
            connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true", username, password);
            getLogger().info("MySQL bağlantısı başarıyla kuruldu!");

            try (Statement statement = connection.createStatement()) {
                String sql = "CREATE TABLE IF NOT EXISTS keseler (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "oyuncu_ismi VARCHAR(255) NOT NULL UNIQUE," +
                        "kesedeki_altin INT DEFAULT 0" +
                        ")";
                statement.executeUpdate(sql);
                getLogger().info("Tablo başarıyla oluşturuldu.");
            } catch (SQLException e) {
                getLogger().severe("Tablo oluşturulamadı: " + e.getMessage());
            }
        } catch (SQLException e) {
            getLogger().severe("MySQL bağlantısı kurulamadı: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }


    private void baglantiKapat() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                getLogger().info("MySQL bağlantısı kapatıldı.");
            }
        } catch (SQLException e) {
            getLogger().severe("MySQL bağlantısı kapatılırken bir hata oluştu: " + e.getMessage());
        }
    }
}
