package org.inventivetalent.quarry;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class QuarryRunner {

	private Quarry plugin;
	private final Map<String, BukkitTask> taskMap = new HashMap<>();

	public QuarryRunner(Quarry plugin) {
		this.plugin = plugin;
	}

	public void start(final QuarryData data) {
		String hash = data.getHash();
		if (taskMap.containsKey(hash)) {
			throw new IllegalArgumentException("Quarry is already running");
		}

		if (data.digX == 0 && data.digY == 0 && data.digZ == 0) {// only reset if all is 0, so we don't loose progress on paused digs
			data.digY = data.y - 2;// start 2 blocks below
			data.digX = data.x - data.size;
			data.digZ = data.z - data.size;
		}

		data.blocksTotal = (data.y - 3) * (data.size * 2) * (data.size * 2);

		int speed = data.speed == 5 ? 1
				: data.speed == 4 ? 2
				: data.speed == 3 ? 5
				: data.speed == 2 ? 10
				: data.speed == 1 ? 20 : 40;
		//TODO: get enchantments to work
		ItemStack breakingItem = new ItemStack(Material.DIAMOND_PICKAXE);
		ItemMeta breakingMeta = breakingItem.getItemMeta();
		if (data.hasUpgrade(Upgrade.FORTUNE)) {
			breakingMeta.addEnchant(Enchantment.LOOT_BONUS_BLOCKS, 2, true);
		}
		if (data.hasUpgrade(Upgrade.SILK_TOUCH)) {
			breakingMeta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
		}
		breakingItem.setItemMeta(breakingMeta);

		World world = Bukkit.getWorld(data.world);
		Block chestBlock = world.getBlockAt(data.x, data.y, data.z);

		data.active = true;
		BukkitTask task = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
			// Actual block modification
			Block block = world.getBlockAt(data.digX, data.digY, data.digZ);
			data.blocksScanned++;
			if (block.getType() != Material.AIR && block.getType() != Material.BEDROCK) {
				boolean doNotMine = false;
				for (Material material : Quarry.DO_NOT_MINE) {
					if (material == block.getType()) {
						doNotMine = true;
						break;
					}
				}

				if (!doNotMine && data.shouldMine(block.getType())) {
					Collection<ItemStack> drops = block.getDrops(breakingItem);
					block.setType(Material.COBBLESTONE);
					world.playSound(block.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);

					data.blocksMined++;

					if (chestBlock.getType() != Material.CHEST) {
						System.err.println("Quarry block was destroyed while running. Stopping.");
						stop(hash);
						return;
					}
					Inventory inventory = ((Chest) chestBlock.getState()).getBlockInventory();
					for (ItemStack item : drops) {
						HashMap<Integer, ItemStack> overload = inventory.addItem(item);
						if (!overload.isEmpty()) {// Chest is full -> stop
							stop(hash);
							data.active = false;

							//Drop the items that didn't fit into the chest
							for (ItemStack overloadItem : overload.values()) {
								chestBlock.getWorld().dropItem(chestBlock.getLocation().add(0.5, 1.5, 0.5), overloadItem);
							}
							return;
						}
					}
				}
			}

			//			if (data.digY < 1) {// reached bedrock
			//				System.out.println("FINISHED");
			//				stop(hash);
			//				data.active = false;
			//				data.digY = data.y - 2;// start 2 blocks below
			//				return;
			//			} else if (data.digZ > data.z + data.size) {// finished a layer -> move down
			//				data.digY--;
			//				data.digZ = data.z - data.size;// reset to start
			//			} else if (data.digX > data.x + data.size) {// finished row -> go to next row
			//				data.digZ++;
			//				data.digX = data.x - data.size;// reset to start
			//			} else {
			//				// Otherwise just keep digging the current row
			//				data.digX++;
			//			}

			// move around
			if (data.digZ > data.z + data.size) {
				System.out.println("DONE");
				stop(hash);
				data.active = false;
			} else if (data.digX > data.x + data.size) {
				data.digZ++;
				data.digX = data.x - data.size;
			} else if (data.digY <= 1) {
				data.digX++;
				data.digY = data.y - 2;
			} else {
				data.digY--;
			}

			System.out.println(data.digX + "  " + data.digY + "  " + data.digZ);

		}, 20L, (long) speed);
		taskMap.put(hash, task);
	}

	public void stop(String hash) {
		if (!taskMap.containsKey(hash)) {
			return;
		}
		BukkitTask task = taskMap.get(hash);
		task.cancel();

		taskMap.remove(hash);
	}

	public void stopAll() {// Stop all tasks, but without updating the 'active' status, so we can continue after restart
		for (BukkitTask task : this.taskMap.values()) {
			task.cancel();
		}
	}

}
