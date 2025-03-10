package io.github.cccm5;

import com.degitise.minevid.dtlTraders.Main;
import com.degitise.minevid.dtlTraders.guis.AGUI;
import com.degitise.minevid.dtlTraders.guis.gui.TradeGUI;
import com.degitise.minevid.dtlTraders.guis.gui.TradeGUIPage;
import com.degitise.minevid.dtlTraders.guis.items.AGUIItem;
import com.degitise.minevid.dtlTraders.guis.items.TradableGUIItem;
import com.degitise.minevid.dtlTraders.utils.citizens.TraderTrait;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.MovecraftLocation;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class CargoMain extends JavaPlugin implements Listener {
    public static final String ERROR_TAG = ChatColor.RED + "Error: " + ChatColor.DARK_RED;
    public static final String SUCCESS_TAG = ChatColor.DARK_AQUA + "Cargo: " + ChatColor.WHITE;
    public static Logger logger;
    private static Economy economy;
    private static ArrayList<Player> playersInQue;
    private static double unloadTax,loadTax;
    private static CargoMain instance;
    private static int delay;//ticks
    private static Material SIGN_POST = Material.getMaterial("SIGN_POST");
    private static Main dtlTradersPlugin;
    private CraftManager craftManager;
    private FileConfiguration config;
    private boolean cardinalDistance;
    private static boolean debug;
    private double scanRange;

//imports config and disables if missing required plugins
    public void onEnable() {
        logger = this.getLogger();
        this.getServer().getPluginManager().registerEvents(this, this);
        playersInQue = new ArrayList<Player>();
        instance = this;
        //************************
        //*       Configs        *
        //************************
        config = getConfig();
        config.addDefault("Scan range",100.0);
        config.addDefault("Transfer delay ticks",300);
        config.addDefault("Load tax percent", 0.01D);
        config.addDefault("Unload tax percent", 0.01D);
        config.addDefault("Cardinal distance",true);
        config.addDefault("Debug mode",false);
        config.options().copyDefaults(true);
        this.saveConfig();
        scanRange = config.getDouble("Scan range") >= 1.0 ? config.getDouble("Scan range") : 100.0;
        delay = config.getInt("Transfer delay ticks");
        loadTax = config.getDouble("Load tax percent")<=1.0 && config.getDouble("Load tax percent")>=0.0 ? config.getDouble("Load tax percent") : 0.01;
        unloadTax = config.getDouble("Unload tax percent")<=1.0 && config.getDouble("Unload tax percent")>=0.0 ? config.getDouble("Unload tax percent") : 0.01;
        cardinalDistance = config.getBoolean("Cardinal distance");
        debug = config.getBoolean("Debug mode");
        //************************
        //*    Load Movecraft    *
        //************************
        if(getServer().getPluginManager().getPlugin("Movecraft") == null || !getServer().getPluginManager().getPlugin("Movecraft").isEnabled()) {
            logger.log(Level.SEVERE, "Movecraft not found or not enabled");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        craftManager = CraftManager.getInstance();
        //************************
        //*    Load  Citizens    *
        //************************
        if(getServer().getPluginManager().getPlugin("Citizens") == null || !getServer().getPluginManager().getPlugin("Citizens").isEnabled()) {
            logger.log(Level.SEVERE, "Citizens 2.0 not found or not enabled");
            getServer().getPluginManager().disablePlugin(this);	
            return;
        }
        if(CitizensAPI.getTraitFactory().getTrait(CargoTrait.class)==null)
            CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(CargoTrait.class));
        //************************
        //*      Load Vault      *
        //************************
        if (getServer().getPluginManager().getPlugin("Vault") == null || !getServer().getPluginManager().getPlugin("Vault").isEnabled()) {
            logger.log(Level.SEVERE, "Vault not found or not enabled");
            getServer().getPluginManager().disablePlugin(this);	
            return;
        }
        Plugin traders = getServer().getPluginManager().getPlugin("dtlTradersPlus");
        if (traders == null  || !(traders instanceof Main)){
            getServer().getPluginManager().disablePlugin(this);
        }
        dtlTradersPlugin = (Main) traders;
        economy = getServer().getServicesManager().getRegistration(Economy.class).getProvider();
    }

    public void onDisable() {
        logger = null;
        economy = null;
        instance = null;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) { // Plugin


        //*****************************************
        //****         UNLOAD COMMAND          ****
        //*****************************************

        if (command.getName().equalsIgnoreCase("unload")) {
            if(!(sender instanceof Player)){
                sender.sendMessage(ERROR_TAG + "You need to be a player to execute that command!");
                return true;
			}

        // Default to unload all
        int moveAmount = -1;

        // If an argument is passed, try to parse it as a number
        if (args.length > 0) {
            try {
                moveAmount = Integer.parseInt(args[0]); // Number of items to unload
            } catch (NumberFormatException e) {
                if ("all".equalsIgnoreCase(args[0])) {
                    moveAmount = -1; // Unload all
                } else {
                    sender.sendMessage(ERROR_TAG + "Invalid argument! Use 'all' or a specific number.");
                    return true;
                }
            }
        }

        unload((Player) sender, moveAmount);
        return true;
    }

		
        //*****************************************
        //****         LOAD COMMAND            ****
        //*****************************************
		
        if (command.getName().equalsIgnoreCase("load")) {
            if(!(sender instanceof Player)){
                sender.sendMessage(ERROR_TAG + "You need to be a player to execute that command!");
                return true;
            }

        // Default to load all
        int moveAmount = -1;

        // If an argument is passed, try to parse it as a number
        if (args.length > 0) {
            try {
                moveAmount = Integer.parseInt(args[0]); // Number of items to Load
            } catch (NumberFormatException e) {
                if ("all".equalsIgnoreCase(args[0])) {
                    moveAmount = -1; // Load all
                } else {
                    sender.sendMessage(ERROR_TAG + "Invalid argument! Use 'all' or a specific number.");
                    return true;
                }
            }
        }

        load((Player) sender, moveAmount);
        return true;
    }

        //*****************************************
        //****         CARGO COMMAND           ****
        //*****************************************

        if (command.getName().equalsIgnoreCase("cargo")) {
            if(!sender.hasPermission("Cargo.cargo")){
                sender.sendMessage(ERROR_TAG + "You don't have permission to do that!");
                return true;
            }
            sender.sendMessage(ChatColor.WHITE + "--[ " + ChatColor.DARK_AQUA + "  Movecraft Cargo " + ChatColor.WHITE + " ]--");
            sender.sendMessage(ChatColor.DARK_AQUA + "Scan Range: " + ChatColor.WHITE + scanRange + " Blocks");
            sender.sendMessage(ChatColor.DARK_AQUA + "Transfer Delay: " + ChatColor.WHITE + delay + " ticks");
            sender.sendMessage(ChatColor.DARK_AQUA + "Unload Tax: " + ChatColor.WHITE + String.format("%.2f",100*unloadTax) + "%");
            sender.sendMessage(ChatColor.DARK_AQUA + "Load Tax: " + ChatColor.WHITE + String.format("%.2f",100*loadTax) + "%");
            sender.sendMessage(ChatColor.DARK_AQUA + "Distance Type: " + ChatColor.WHITE + (cardinalDistance ? "Cardinal" : "Direct"));
            return true;
        }
        return false;

    }

    @EventHandler
    public void onSignClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!e.getClickedBlock().getType().name().equals("SIGN_POST") && !e.getClickedBlock().getType().name().endsWith("SIGN") && !e.getClickedBlock().getType().name().endsWith("WALL_SIGN")) {
            return;
        }
        Sign sign = (Sign) e.getClickedBlock().getState();
        if (sign.getLine(0).equals(ChatColor.DARK_AQUA + "[UnLoad]")) {
            unload(e.getPlayer(), -1);
            return;
        }
        if (sign.getLine(0).equals(ChatColor.DARK_AQUA + "[Load]")) {
            load(e.getPlayer(), -1);
        }

    }

    @EventHandler
    public void onSignPlace(SignChangeEvent e){
        if(!e.getBlock().getType().name().equals("SIGN_POST") && !e.getBlock().getType().name().endsWith("SIGN") && !e.getBlock().getType().name().endsWith("WALL_SIGN")){
            return;
        }
        if(ChatColor.stripColor(e.getLine(0)).equalsIgnoreCase("[Load]") || ChatColor.stripColor(e.getLine(0)).equalsIgnoreCase("[UnLoad]")){
            e.setLine(0,ChatColor.DARK_AQUA + (ChatColor.stripColor(e.getLine(0))).replaceAll("u","U").replaceAll("l","L"));
        }

    }

    public static boolean isDebug(){
        return debug;
    }

    public static Economy getEconomy(){
        return economy;
    }

    public static List<Player> getQue(){
        return playersInQue;
    }

    public static double getLoadTax(){
        return loadTax;
    }

    public static double getUnloadTax(){
        return unloadTax;
    }

    public static int getDelay(){
        return delay;
    }

    public static CargoMain getInstance(){
        return instance;
    }

    private void unload(Player player, int moveAmount){
        if(!player.hasPermission("Cargo.unload")){
            player.sendMessage(ERROR_TAG + "You don't have permission to do that!");
            return;
        }
        Craft playerCraft = craftManager.getCraftByPlayer(player);
        if(playersInQue.contains(player)){
            player.sendMessage(ERROR_TAG + "You're already moving cargo!");
            return;
        }

        if(playerCraft == null){
            player.sendMessage(ERROR_TAG + "You need to be piloting a craft to do that!");
            return;
        }
		
        //NPC cargoMerchant=null;
        List<NPC> nearbyMerchants = new ArrayList<>();
        double distance;//, lastScan = scanRange;
        MovecraftLocation loc = playerCraft.getHitBox().getMidPoint();
        for(NPC npc :Utils.getNPCsWithTrait(CargoTrait.class)){
            if(!npc.isSpawned())
                continue;
            distance = cardinalDistance ? Math.abs(loc.getX()-npc.getEntity().getLocation().getX()) + Math.abs(loc.getZ()-npc.getEntity().getLocation().getZ()) : Math.sqrt(Math.pow(loc.getX()-npc.getEntity().getLocation().getX(),2) + Math.pow(loc.getZ()-npc.getEntity().getLocation().getZ(),2));
            if( distance <= scanRange){
                nearbyMerchants.add(npc);
            }
        }
        if(nearbyMerchants.size()==0){
            player.sendMessage(ERROR_TAG + "You need to be within " +  scanRange + " blocks of a merchant to use that command!");
            return;
        }

        if(player.getInventory().getItemInMainHand() == null || player.getInventory().getItemInMainHand().getType() == Material.AIR){
            player.sendMessage(ERROR_TAG + "You need to be holding a cargo item to do that!");
            return;
        }
        String guiName;
        TradableGUIItem finalItem = null;
		for (NPC cargoMerchant : nearbyMerchants) {
			if (finalItem != null) {
				break;
			}
    
			guiName = cargoMerchant.getTrait(TraderTrait.class).getGUIName();
			AGUI gui = dtlTradersPlugin.getGuiListService().getGUI(guiName);
			TradeGUI tradeGUI = (TradeGUI) gui;
    
			ItemStack compareItem = player.getInventory().getItemInMainHand().clone();
    
			// Make sure to log the number of pages
			List<TradeGUIPage> pages = tradeGUI.getPages();
			//player.sendMessage(ERROR_TAG + "Number of pages: " + pages.size()); // Debugging output
    
			// Loop through all pages
			for (TradeGUIPage page : pages) {
				if (page == null) continue;

				// Loop through the items on the current page
				for (AGUIItem tempItem : page.getItems("sell")) {
					if (!(tempItem instanceof TradableGUIItem)) continue;

					if (tempItem.getMainItem().isSimilar(compareItem)) {
						if (tempItem.getMainItem().getAmount() > 1) continue;
                
						finalItem = (TradableGUIItem) tempItem;  // Assign the matching item
						break;  // Break out of the inner loop
					}
				}
				// Check if a valid item was found, break the outer loop if so
				if (finalItem != null) {
				break;
			}
		}

		// If no valid item found, notify the player
		if (finalItem == null || finalItem.getTradePrice() == 0.0) {
			player.sendMessage(ERROR_TAG + "You need to be holding a cargo item to do that!");
			return;
		}
	}
        assert finalItem!=null;
        String itemName = finalItem.getMainItem().getItemMeta().getDisplayName() != null && finalItem.getMainItem().getItemMeta().getDisplayName().length() > 0 ? finalItem.getMainItem().getItemMeta().getDisplayName() : finalItem.getMainItem().getType().name().toLowerCase();

        List<Inventory> invs = Utils.getInventories(playerCraft, finalItem.getMainItem(), Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL);
        int size = invs.size();
        if(size <=0 ){
            player.sendMessage(CargoMain.ERROR_TAG + "You have no " + itemName + " on this craft!");
            return;
        }

        playersInQue.add(player);
		//modified unload task to receive a moveAmount- allows players to specify how much to unload
		new UnloadTask(craftManager.getCraftByPlayer(player), finalItem, moveAmount).runTaskTimer(this, delay, delay);
        new ProcessingTask(player, finalItem,size).runTaskTimer(this,0,20);
		player.sendMessage(SUCCESS_TAG + "Started unloading cargo");
    }

    private void load(Player player, int moveAmount){
        if(!player.hasPermission("Cargo.load")){
            player.sendMessage(ERROR_TAG + "You don't have permission to do that!");
            return;
        }
        Craft playerCraft = craftManager.getCraftByPlayer(player);
        if(playersInQue.contains(player)){
            player.sendMessage(ERROR_TAG + "You're already moving cargo!");
            return;
        }

        if(playerCraft == null){
            player.sendMessage(ERROR_TAG + "You need to be piloting a craft to do that!");
            return;
        }
        //NPC cargoMerchant=null;
        List<NPC> nearbyMerchants = new ArrayList<>();
        double distance;//, lastScan = scanRange;
        MovecraftLocation loc = playerCraft.getHitBox().getMidPoint();
        for(NPC npc :Utils.getNPCsWithTrait(CargoTrait.class)){
            if(!npc.isSpawned())
                continue;
            distance = cardinalDistance ? Math.abs(loc.getX()-npc.getEntity().getLocation().getX()) + Math.abs(loc.getZ()-npc.getEntity().getLocation().getZ()) : Math.sqrt(Math.pow(loc.getX()-npc.getEntity().getLocation().getX(),2) + Math.pow(loc.getZ()-npc.getEntity().getLocation().getZ(),2));
            if( distance <= scanRange){
                nearbyMerchants.add(npc);
            }
        }
        if(nearbyMerchants.size()==0){
            player.sendMessage(ERROR_TAG + "You need to be within " +  scanRange + " blocks of a merchant to use that command!");
            return;
        }

        if(player.getInventory().getItemInMainHand() == null || player.getInventory().getItemInMainHand().getType() == Material.AIR){
            logger.info(player.getInventory().getItemInMainHand().getType().name());
            player.sendMessage(ERROR_TAG + "You need to be holding a cargo item to do that!");
            return;
        }
        String guiName;
        TradableGUIItem finalItem = null;
		for (NPC cargoMerchant : nearbyMerchants) {
			if (finalItem != null) {
				break;
			}
    
			guiName = cargoMerchant.getTrait(TraderTrait.class).getGUIName();
			AGUI gui = dtlTradersPlugin.getGuiListService().getGUI(guiName);
			TradeGUI tradeGUI = (TradeGUI) gui;
    
			ItemStack compareItem = player.getInventory().getItemInMainHand().clone();
    
			// Make sure to log the number of pages
			List<TradeGUIPage> pages = tradeGUI.getPages();
			//player.sendMessage(ERROR_TAG + "Number of pages: " + pages.size()); // Debugging output
    
			// Loop through all pages
			for (TradeGUIPage page : pages) {
				if (page == null) continue;

				// Loop through the items on the current page
				for (AGUIItem tempItem : page.getItems("buy")) {
					if (!(tempItem instanceof TradableGUIItem)) continue;

					if (tempItem.getMainItem().isSimilar(compareItem)) {
						if (tempItem.getMainItem().getAmount() > 1) continue;
                
						finalItem = (TradableGUIItem) tempItem;  // Assign the matching item
						break;  // Break out of the inner loop
					}
				}
				// Check if a valid item was found, break the outer loop if so
				if (finalItem != null) {
				break;
			}
		}

		// If no valid item found, notify the player
		if (finalItem == null || finalItem.getTradePrice() == 0.0) {
			player.sendMessage(ERROR_TAG + "You need to be holding a cargo item to do that!");
			return;
		}
	}

        final ItemMeta meta = finalItem.getMainItem().getItemMeta();
        String itemName = meta.getDisplayName() != null && meta.getDisplayName().length() > 0 ? meta.getDisplayName() : finalItem.getMainItem().getType().name().toLowerCase();
        if(!economy.has(player,finalItem.getTradePrice()*(1+loadTax))){
            player.sendMessage(ERROR_TAG + "You don't have enough money to buy any " + itemName + "!");
            return;
        }

        List<Inventory> invs = Utils.getInventoriesWithSpace(playerCraft, finalItem.getMainItem(), Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL);
        int size = invs.size();
        if(size <=0 ){
            player.sendMessage(CargoMain.ERROR_TAG + "You don't have any space for " + itemName + " on this craft!");
            return;
        }

        playersInQue.add(player);
        new LoadTask(craftManager.getCraftByPlayer(player),finalItem, moveAmount).runTaskTimer(this,delay,delay);
        new ProcessingTask(player, finalItem,size).runTaskTimer(this,0,20);
        player.sendMessage(SUCCESS_TAG + "Started loading cargo");
    }
}
