package io.github.cccm5;

import com.degitise.minevid.dtlTraders.guis.items.TradableGUIItem;
import net.countercraft.movecraft.craft.PlayerCraft;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class UnloadTask extends CargoTask {
	private int remainingAmount;
	private int moveAmount;
	
    public UnloadTask(PlayerCraft craft, TradableGUIItem item, int moveAmount) {
        super(craft, item);
		this.remainingAmount = moveAmount;
		this.moveAmount = moveAmount;
    }

    public void execute() {
        List<Inventory> invs = Utils.getInventories(craft, item.getMainItem(), Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL);
       
        // If there are no inventories, cancel the task
        if (invs.isEmpty()) {
            cancelUnload();
            return;
        }
		
	   Inventory inv = invs.get(0);	   
        int count = 0;
		
        for (int i = 0; i < inv.getSize(); i++) {
			
			if (moveAmount == -1) {
				if (inv.getItem(i) != null && inv.getItem(i).isSimilar(item.getMainItem())) {
                count += inv.getItem(i).getAmount();
                inv.setItem(i, null);
				}	
			} else {	
				if (inv.getItem(i) != null && inv.getItem(i).isSimilar(item.getMainItem())) {
					int itemAmount = inv.getItem(i).getAmount();
				
					if (remainingAmount > 0) {
						if (itemAmount <= remainingAmount) {
							count += itemAmount;
							remainingAmount -= itemAmount;
							inv.setItem(i, null);
						} 	else {
								inv.getItem(i).setAmount(itemAmount - remainingAmount);
								count += remainingAmount;
								remainingAmount = 0;
							}
					}		
				}
			}
		}
        originalPilot.sendMessage(CargoMain.SUCCESS_TAG + "Unloaded " + count + " worth $"
                + String.format("%.2f", count * item.getTradePrice()) + " took a tax of "
                + String.format("%.2f", CargoMain.getUnloadTax() * count * item.getTradePrice()));
        CargoMain.getEconomy().depositPlayer(originalPilot,
                count * item.getTradePrice() * (1 - CargoMain.getUnloadTax()));

        if (moveAmount == -1 && invs.size() <= 1) {
            cancelUnload();
			return;
        }
		if (remainingAmount == 0) {
			cancelUnload();
            return;
		}
        new ProcessingTask(originalPilot, item, invs.size()).runTaskTimer(CargoMain.getInstance(), 0, 20);
    }
    private void cancelUnload() {
        // This method cancels the unloading task and sends the final success message
        CargoMain.getQue().remove(originalPilot);
        originalPilot.sendMessage(CargoMain.SUCCESS_TAG + "All cargo unloaded");
		if (remainingAmount >=1) {
			originalPilot.sendMessage(CargoMain.ERROR_TAG + remainingAmount + " Items Could Not Be Unloaded"
                + String.format(" Do You Have These Items Onboard?"));
		}
        this.cancel();  // Cancel the task
    }
}
