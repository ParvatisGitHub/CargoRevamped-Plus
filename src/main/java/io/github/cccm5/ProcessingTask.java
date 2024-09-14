package io.github.cccm5;

import com.degitise.minevid.dtlTraders.guis.items.TradableGUIItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
/**
 * Write a description of class ProcessingTask here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class ProcessingTask extends BukkitRunnable implements Listener
{
    private static final int DELAY_BETWEEN_DISPLAY = 1;
    private int remainingTime,remainingChests;
    private final Player player;
    private final TradableGUIItem item;
    private final String itemDisplayName;
    private Scoreboard board;
    private Objective objective;
    public ProcessingTask(Player player, TradableGUIItem item, int remainingChests){//, int remainingChests){
        if (item == null) 
            throw new IllegalArgumentException("item must not be null");
        if (player == null) 
            throw new IllegalArgumentException("player must not be null");
        this.player = player;
        this.item = item;
        this.remainingTime = CargoMain.getDelay()/20;
        this.remainingChests = remainingChests;
        board = Bukkit.getScoreboardManager().getNewScoreboard();
        itemDisplayName = item.getMainItem().getItemMeta().getDisplayName() != null && item.getMainItem().getItemMeta().getDisplayName().length() > 0  ? item.getMainItem().getItemMeta().getDisplayName() : item.getMainItem().getType().name().toLowerCase();
        objective = itemDisplayName.length() <=14 ? board.registerNewObjective(ChatColor.DARK_AQUA + itemDisplayName, "dummy") : board.registerNewObjective(ChatColor.DARK_AQUA + "Cargo", "dummy","Cargo");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(board);
    }

    @Override
    public void run() {
        if(remainingTime > DELAY_BETWEEN_DISPLAY)
            remainingTime-=DELAY_BETWEEN_DISPLAY;
        else{
            this.cancel();
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            return;
        }
        //Score score = objective.getScore(ChatColor.GREEN + "Time:"); //Get a fake offline player
        objective.getScore(ChatColor.GREEN + "Remaining Chests:").setScore(remainingChests);
        objective.getScore(ChatColor.GREEN + "Time:").setScore(remainingTime);
        //Bukkit.broadcastMessage(ChatColor.GREEN + "Time:" + remainingTime);
        //player.setScoreboard(board);
    }
}
