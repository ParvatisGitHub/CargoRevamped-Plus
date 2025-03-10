package io.github.cccm5;

import com.degitise.minevid.dtlTraders.guis.items.TradableGUIItem;
import net.countercraft.movecraft.craft.PlayerCraft;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class LoadTask extends CargoTask {
    private int remainingAmount;
    private int moveAmount;

    public LoadTask(PlayerCraft craft, TradableGUIItem item, int moveAmount) {
        super(craft, item);
		//sets local moveAmount and remainingAmount to be equal to moveAmount being brought in on the loadTask. 
		//seperated for redundancey, i probably dont need to but here we are ig
        this.remainingAmount = moveAmount;
		this.moveAmount = moveAmount;
    }

    protected void execute() {
    // Re-fetch the inventories to ensure it's up to date in case any inventories were filled
    List<Inventory> invs = Utils.getInventoriesWithSpace(craft, item.getMainItem(), Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL);

    // If there are no inventories to process, cancel the task
    if (invs.isEmpty()) {
        cancelLoad();
        return;
    }
    Inventory inv = invs.get(0);
    int loaded = 0;

        for (int i = 0; i < inv.getSize(); i++) {
            // Load all function (if moveAmount is -1)
			if (moveAmount == -1) {
				if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR || inv.getItem(i).isSimilar(item.getMainItem())) {
					int maxCount = (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR)
							? item.getMainItem().getMaxStackSize()
							: inv.getItem(i).getMaxStackSize() - inv.getItem(i).getAmount();

					if (CargoMain.getEconomy().getBalance(originalPilot) > item.getTradePrice() * maxCount * (1 + CargoMain.getLoadTax())) {
						loaded += maxCount;
						ItemStack tempItem = item.getMainItem().clone();
						tempItem.setAmount(tempItem.getMaxStackSize());
						inv.setItem(i, tempItem);
					} else {
						maxCount = (int) (CargoMain.getEconomy().getBalance(originalPilot) / (item.getTradePrice() * (1 + CargoMain.getLoadTax())));
						this.cancel();
						CargoMain.getQue().remove(originalPilot);
						originalPilot.sendMessage(CargoMain.SUCCESS_TAG + "You ran out of money!");
                    
						if (maxCount <= 0) {
							originalPilot.sendMessage(CargoMain.SUCCESS_TAG + "Loaded " + loaded + " items worth $"
									+ String.format("%.2f", loaded * item.getTradePrice()) + " took a tax of "
									+ String.format("%.2f", CargoMain.getLoadTax() * loaded * item.getTradePrice()));
							return;
						}

						ItemStack tempItem = item.getMainItem().clone();
						if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR)
							tempItem.setAmount(maxCount);
						else
							tempItem.setAmount(inv.getItem(i).getAmount() + maxCount);
						inv.setItem(i, tempItem);
						loaded += maxCount;

						originalPilot.sendMessage(CargoMain.SUCCESS_TAG + "Loaded " + loaded + " items worth $"
								+ String.format("%.2f", loaded * item.getTradePrice()) + " took a tax of "
								+ String.format("%.2f", CargoMain.getLoadTax() * loaded * item.getTradePrice()));
						CargoMain.getEconomy().withdrawPlayer(originalPilot, loaded * item.getTradePrice() * (1 + CargoMain.getLoadTax()));
						return;
					}
					CargoMain.getEconomy().withdrawPlayer(originalPilot, maxCount * item.getTradePrice() * (1 + CargoMain.getLoadTax()));					
				}
            } else {
                // Load specific amount
                if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR || inv.getItem(i).isSimilar(item.getMainItem())) {
                    int maxCount = (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR)
                            ? item.getMainItem().getMaxStackSize()
                            : inv.getItem(i).getMaxStackSize() - inv.getItem(i).getAmount();

                    // Ensure that we don't load more than the remaining amount
                    if (remainingAmount > 0) {
						if (maxCount <= remainingAmount) {
							if (CargoMain.getEconomy().getBalance(originalPilot) > item.getTradePrice() * maxCount * (1 + CargoMain.getLoadTax())) {
						loaded += maxCount;
						ItemStack tempItem = item.getMainItem().clone();
						tempItem.setAmount(tempItem.getMaxStackSize());
						inv.setItem(i, tempItem);
					} else {
						maxCount = (int) (CargoMain.getEconomy().getBalance(originalPilot) / (item.getTradePrice() * (1 + CargoMain.getLoadTax())));
						this.cancel();
						CargoMain.getQue().remove(originalPilot);
						originalPilot.sendMessage(CargoMain.SUCCESS_TAG + "You ran out of money!");
                    
						if (maxCount <= 0) {
							originalPilot.sendMessage(CargoMain.SUCCESS_TAG + "Loaded " + loaded + " items worth $"
									+ String.format("%.2f", loaded * item.getTradePrice()) + " took a tax of "
									+ String.format("%.2f", CargoMain.getLoadTax() * loaded * item.getTradePrice()));
							return;
						}

						ItemStack tempItem = item.getMainItem().clone();
						if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR)
							tempItem.setAmount(maxCount);
						else
							tempItem.setAmount(inv.getItem(i).getAmount() + maxCount);
						inv.setItem(i, tempItem);
						loaded += maxCount;
						originalPilot.sendMessage(CargoMain.SUCCESS_TAG + "Loaded " + loaded + " items worth $"
								+ String.format("%.2f", loaded * item.getTradePrice()) + " took a tax of "
								+ String.format("%.2f", CargoMain.getLoadTax() * loaded * item.getTradePrice()));
						CargoMain.getEconomy().withdrawPlayer(originalPilot, loaded * item.getTradePrice() * (1 + CargoMain.getLoadTax()));
						return;
					}
					CargoMain.getEconomy().withdrawPlayer(originalPilot, maxCount * item.getTradePrice() * (1 + CargoMain.getLoadTax()));
						remainingAmount -= maxCount;
					
				} else {
                            // If the maxCount is more than remainingAmount, load the remaining amount and stop.
                            if (CargoMain.getEconomy().getBalance(originalPilot) > item.getTradePrice() * remainingAmount * (1 + CargoMain.getLoadTax())) {                               
								ItemStack tempItem = item.getMainItem().clone();
								if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR)
									tempItem.setAmount(remainingAmount);
								else
									tempItem.setAmount(inv.getItem(i).getAmount() + remainingAmount);
								inv.setItem(i, tempItem);
								loaded += remainingAmount;
                                remainingAmount = 0;
                            } else {
                                remainingAmount = (int) (CargoMain.getEconomy().getBalance(originalPilot) / (item.getTradePrice() * (1 + CargoMain.getLoadTax())));
                                this.cancel();
								CargoMain.getQue().remove(originalPilot);
								originalPilot.sendMessage(CargoMain.SUCCESS_TAG + "You ran out of money!");
                                return;
                            }
                            CargoMain.getEconomy().withdrawPlayer(originalPilot, remainingAmount * item.getTradePrice() * (1 + CargoMain.getLoadTax()));
                        }
					}
                }
			}
        
		


		

        // Stop if we've loaded everything
        if (remainingAmount == 0) {
            originalPilot.sendMessage(CargoMain.SUCCESS_TAG + "Loaded " + loaded + " items worth $"
						+ String.format("%.2f", loaded * item.getTradePrice()) + " took a tax of "
						+ String.format("%.2f", CargoMain.getLoadTax() * loaded * item.getTradePrice()));
			cancelLoad();
            return;
        }
    }
    // If there are no more inventories to process, cancel the task
    if (remainingAmount == 0) {
        cancelLoad();
        return;
    }

    originalPilot.sendMessage(CargoMain.SUCCESS_TAG + "Loaded " + loaded + " items worth $"
		+ String.format("%.2f", loaded * item.getTradePrice()) + " took a tax of "
		+ String.format("%.2f", CargoMain.getLoadTax() * loaded * item.getTradePrice()));
	// After processing all inventories, schedule the next task if there are remaining items
    new ProcessingTask(originalPilot, item, invs.size()).runTaskTimer(CargoMain.getInstance(), 0, 20);
}


    private void cancelLoad() {
        // This method cancels the Loading task and sends the final success message
        CargoMain.getQue().remove(originalPilot);
        originalPilot.sendMessage(CargoMain.SUCCESS_TAG + "All cargo Loaded");
        if (remainingAmount >=1) {
			originalPilot.sendMessage(CargoMain.ERROR_TAG + remainingAmount + " Items Could Not Be Loaded"
				+ String.format(" Please Ensure You Have Enough Storage Space To Load More Cargo"));
		}
		this.cancel();  // Cancel the task
    }
}
