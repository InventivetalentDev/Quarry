package org.inventivetalent.quarry;

import org.bstats.bukkit.MetricsLite;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;

public class Quarry extends JavaPlugin implements Listener {

	static final String PREFIX = "§8[§9Quarry§8]§r ";
	static final           ItemStack[] DISPENSER_LAYOUT     = new ItemStack[] {
			new ItemStack(Material.REDSTONE), new ItemStack(Material.IRON_INGOT), new ItemStack(Material.REDSTONE),
			new ItemStack(Material.DIAMOND), new ItemStack(Material.IRON_INGOT), new ItemStack(Material.DIAMOND),
			new ItemStack(Material.REDSTONE), new ItemStack(Material.DIAMOND), new ItemStack(Material.REDSTONE)
	};
	static final           String      DISPENSER_IDENTIFIER = "%%QUARRY%DISPENSER%%";
	static final           String      CONTROLS_TITLE       = "§5Quarry Controls§r";
	static final           String      FILTERS_TITLE        = "§bQuarry Filter§r";
	protected static final Material[]  DO_NOT_MINE          = new Material[] {
			Material.LONG_GRASS,
			Material.YELLOW_FLOWER,
			Material.RED_ROSE,
			Material.SAPLING,
			Material.BROWN_MUSHROOM,
			Material.RED_MUSHROOM,
			Material.WATER_LILY,
			Material.VINE,
			Material.WEB,
			Material.SNOW,
			Material.CARPET,
			Material.END_ROD,
			Material.DOUBLE_PLANT,
			Material.FLOWER_POT,
			Material.BANNER,
			Material.SKULL,
			Material.SIGN,
			Material.BED,
			Material.RAILS,
			Material.ACTIVATOR_RAIL,
			Material.DETECTOR_RAIL,
			Material.POWERED_RAIL,
			Material.REDSTONE,
			Material.REDSTONE_COMPARATOR,
			Material.TRIPWIRE_HOOK,
			Material.TRIPWIRE,
			Material.CAKE_BLOCK,

			Material.LAVA,
			Material.STATIONARY_LAVA,
			Material.WATER,
			Material.STATIONARY_WATER,
			Material.STONE

	};

	public QuarryRegistry registry;
	public QuarryRunner   runner;

	@Override
	public void onEnable() {
		this.registry = new QuarryRegistry(this, new File(getDataFolder(), "quarries"));
		this.runner = new QuarryRunner(this);
		Bukkit.getScheduler().runTaskLater(this, () -> {
			System.out.println("Loading quarries...");
			Quarry.this.registry.load();
		}, 40L);

		Bukkit.getPluginManager().registerEvents(this, this);

		new MetricsLite(this);
	}

	@Override
	public void onDisable() {
		this.runner.stopAll();
		this.registry.save();
	}

	@EventHandler
	public void on(BlockPlaceEvent event) {
		if (event.getBlock() == null) { return; }
		if (event.getBlock().getType() == Material.CHEST) {// only check if the chest is the last block placed (for now) TODO
			boolean isQuarryStructure = Structure.QUARRY.test(event.getBlock());
			if (isQuarryStructure) {
				/* TODO: get this working...
				// Rotate the dispenser (just aesthetic stuff, really)
				Block dispenserBlock = event.getBlock().getRelative(BlockFace.DOWN);
				Dispenser dispenser= (Dispenser) dispenserBlock.getState().getData();
				System.out.println(dispenser.getFacing());
				dispenser.setFacingDirection(BlockFace.DOWN);
				dispenserBlock.getState().setData(dispenser);
				dispenserBlock.getState().update();
				*/

				Block dispenserBlock = event.getBlock().getRelative(BlockFace.DOWN);
				Dispenser dispenser = (Dispenser) dispenserBlock.getState();
				// Test dispenser content
				if (!testDispenserContent(dispenser)) {
					event.getPlayer().sendMessage(PREFIX+"§cInvalid dispenser contents for Quarry");
					return;
				}

				dispenser.setCustomName(DISPENSER_IDENTIFIER);

				// Register
				this.registry.register(event.getBlock().getLocation());

				//TODO: permissions + proper message
				event.getPlayer().sendMessage(PREFIX+"§aQuarry created!");
				event.getPlayer().sendMessage(PREFIX+"§7Sneak + Right-Click to open the menu");
			}
		}
	}

	boolean testDispenserContent(Dispenser dispenser) {
		for (int i = 0; i < 9; i++) {
			ItemStack layout = DISPENSER_LAYOUT[i];
			ItemStack inventory = dispenser.getInventory().getItem(i);

			boolean match = true;
			if (inventory == null) {
				match = false;
			} else if (layout.getType() != inventory.getType()) {
				match = false;
			} else if (layout.getAmount() != inventory.getAmount()) {
				match = false;
			} else if (layout.getDurability() != inventory.getDurability()) {
				match = false;
			}
			if (!match) {

				return false;
			}
		}
		System.out.println("Dispenser contents match");
		return true;
	}

	@EventHandler
	public void on(BlockBreakEvent event) {
		if (event.getBlock() == null) { return; }
		if (event.getBlock().getType() == Material.CHEST || event.getBlock().getType() == Material.DISPENSER || event.getBlock().getType() == Material.FENCE) {
			Block chestBlock = null;
			if (event.getBlock().getType() == Material.CHEST) {
				//				chestBlock = event.getBlock();

				boolean isQuarryStructure = Structure.QUARRY.test(event.getBlock());// Test with the chest as the base block
				if (isQuarryStructure) {
					String hash = QuarryRegistry.makeLocationHash(event.getBlock().getLocation().getWorld().getName(), event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ());

					// Stop quarry
					this.runner.stop(hash);

					// Unregister
					this.registry.unregister(event.getBlock().getLocation());

					event.getPlayer().sendMessage(PREFIX+"§cQuarry destroyed");
				}
				return;
			} else if (event.getBlock().getType() == Material.DISPENSER) {
				chestBlock = event.getBlock().getRelative(BlockFace.UP);
			} else if (event.getBlock().getType() == Material.FENCE) {
				// Search for the center dispenser block
				BlockFace[] faces = new BlockFace[] {
						BlockFace.NORTH,
						BlockFace.EAST,
						BlockFace.SOUTH,
						BlockFace.WEST
				};

				Block dispenserBlock = null;
				Block lastFenceBlock = null;
				for (BlockFace face : faces) {
					Block adjacent = event.getBlock().getRelative(face);
					if (adjacent.getType() == Material.DISPENSER) {
						// Found dispenser directly
						dispenserBlock = adjacent;
						break;
					} else if (adjacent.getType() == Material.FENCE) {
						lastFenceBlock = adjacent;
					}
				}
				if (dispenserBlock == null) {
					// Couldn't find the dispenser
					if (lastFenceBlock == null) { return; }// There's also no further fence blocks to check, so give up
					for (BlockFace face : faces) {
						Block adjacent = lastFenceBlock.getRelative(face);
						if (adjacent.getType() == Material.DISPENSER) {
							// Found dispenser finally
							dispenserBlock = adjacent;
							break;
						}
						// There's also no need to check for more fences, since this should be the last chance to find the dispenser
					}
				}

				if (dispenserBlock != null) {
					// We have a dispenser, so try to get the chest from there
					chestBlock = dispenserBlock.getRelative(BlockFace.UP);
				}
			}

			if (chestBlock != null && chestBlock.getType() == Material.CHEST) {
				boolean isQuarryStructure = Structure.QUARRY.test(chestBlock);// Test with the chest as the base block
				if (isQuarryStructure) {
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler
	public void on(PlayerInteractEvent event) {
		if (event.getClickedBlock() == null) { return; }
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (event.getClickedBlock().getType() == Material.DISPENSER) {
				Dispenser dispenser = (Dispenser) event.getClickedBlock().getState();
				//				System.out.println(dispenser);
				//				System.out.println(dispenser.getCustomName());
				//				if (DISPENSER_IDENTIFIER.equals(dispenser.getCustomName())) {
				//					// Disable dispenser interaction
				//					event.setCancelled(true);
				//				}
				Block topBlock = event.getClickedBlock().getRelative(BlockFace.UP);
				if (topBlock.getType() == Material.CHEST) {
					if (testDispenserContent(dispenser)) {
						// Disable dispenser interaction
						event.setCancelled(true);
					}
				}
			}
			if (event.getClickedBlock().getType() == Material.CHEST) {
				if (event.getPlayer().isSneaking()) {
					boolean isQuarryStructure = Structure.QUARRY.test(event.getClickedBlock());// Test with the chest as the base block
					if (isQuarryStructure) {
						event.setCancelled(true);
						openControlsInventory(event.getPlayer(), event.getClickedBlock().getLocation());
					}
				}
			}
		}
	}

	void openControlsInventory(Player player, Location quarryLocation) {
		QuarryData data = this.registry.getByLocation(quarryLocation);
		if (data == null) {
			player.sendMessage(PREFIX + "§cInvalid quarry!");
			return;
		}

		Inventory inventory = Bukkit.createInventory(null, 9, CONTROLS_TITLE);

		inventory.setItem(0, makeItem(Material.HOPPER, "§aFilters"));

		if (data.size <= 4) {
			inventory.setItem(1, makeItem(Material.WOOD_PLATE, "§a8x8 (Default Size)"));
		} else if (data.size == 8) {
			inventory.setItem(1, makeItem(Material.STONE_PLATE, "§bSize: 16x16"));
		} else if (data.size == 16) {
			inventory.setItem(1, makeItem(Material.IRON_PLATE, "§bSize: 32x32"));
		} else if (data.size == 32) {
			inventory.setItem(1, makeItem(Material.GOLD_PLATE, "§bSize: 64x64"));
		} else if (data.size > 32) {
			ItemStack item = makeItem(Material.GOLD_PLATE, "§b" + (data.size * 2) + "x" + (data.size * 2) + " (Custom Size)");
			ItemMeta meta = item.getItemMeta();
			meta.addEnchant(Enchantment.DURABILITY, 1, true);
			item.setItemMeta(meta);
			inventory.setItem(1, item);
		}

		//TODO
//		if (data.hasUpgrade(Upgrade.FORTUNE) || data.hasUpgrade(Upgrade.SILK_TOUCH)) {
//			List<String> lore = new ArrayList<>();
//			if (data.hasUpgrade(Upgrade.FORTUNE)) {
//				lore.add("§7- Fortune");
//			}
//			if (data.hasUpgrade(Upgrade.SILK_TOUCH)) {
//				lore.add("§7- Silk Touch");
//			}
//			ItemStack itemStack = makeItem(Material.ENCHANTED_BOOK, "§aEnchantment Upgrades", lore);
//			inventory.setItem(7, itemStack);
//		} else {
//			inventory.setItem(7, makeItem(Material.BOOK, "§7No Enchantment upgrades"));
//		}

		if (data.speed == 0) {
			inventory.setItem(8, makeItem(Material.WOOD_PICKAXE, "§aDefault Speed"));
		} else if (data.speed == 1) {
			inventory.setItem(8, makeItem(Material.STONE_PICKAXE, "§bSpeed Upgrade 1"));
		} else if (data.speed == 2) {
			inventory.setItem(8, makeItem(Material.IRON_PICKAXE, "§bSpeed Upgrade 2"));
		} else if (data.speed == 3) { inventory.setItem(8, makeItem(Material.DIAMOND_PICKAXE, "§bSpeed Upgrade 3")); }

		ItemStack startStopItem;
		if (data.active) {
			startStopItem = makeItem(Material.WOOL, "§cStop");
			startStopItem.setDurability((short) 14);

			//TODO: get this stuff working
			//			MaterialData materialData = item.getData();
			//			((Wool) materialData).setColor(DyeColor.RED);
			//			item.setData(materialData);
			//			System.out.println(item);
			//			System.out.println(item.getData());
		} else {
			startStopItem = makeItem(Material.WOOL, "§aStart");
			startStopItem.setDurability((short) 13);
			//			MaterialData materialData = item.getData();
			//			((Wool) materialData).setColor(DyeColor.GREEN);
			//			item.setData(materialData);
			//			System.out.println(item);
			//			System.out.println(item.getData());
		}
		ItemMeta startStopMeta = startStopItem.getItemMeta();
		startStopMeta.setLore(Arrays.asList(
				"§r",
				"§b" + data.blocksScanned + " §7Blocks Scanned",
				"§b" + data.blocksMined + " §7Blocks Mined",
				"§b" + data.blocksTotal + " §7Blocks Total",
				"§r",
				"§7" + (data.active ? "Currently mining" : "Waiting to mine") + " at",
				"§b  " + data.digX + " " + data.digY + " " + data.digZ
		));
		startStopItem.setItemMeta(startStopMeta);
		inventory.setItem(4, startStopItem);

		new BukkitRunnable() {

			@Override
			public void run() {
				if (!player.isOnline()) {
					cancel();
					return;
				}
				if (inventory.getViewers().isEmpty()) {
					cancel();
					return;
				}

				ItemMeta startStopMeta = startStopItem.getItemMeta();
				startStopMeta.setLore(Arrays.asList(
						"§r",
						"§b" + data.blocksScanned + " §7Blocks Scanned",
						"§b" + data.blocksMined + " §7Blocks Mined",
						"§b" + data.blocksTotal + " §7Blocks Total",
						"§r",
						"§7" + (data.active ? "Currently mining" : "Waiting to mine") + " at",
						"§b" + data.digX + " " + data.digY + " " + data.digZ
				));
				startStopItem.setItemMeta(startStopMeta);
				inventory.setItem(4, startStopItem);

				player.updateInventory();
			}
		}.runTaskTimer(this, 20, 20);

		player.openInventory(inventory);
	}

	void openFilterInventory(Player player, Location quarryLocation) {
		QuarryData data = this.registry.getByLocation(quarryLocation);
		if (data == null) {
			player.sendMessage(PREFIX + "§cInvalid quarry!");
			return;
		}

		Inventory inventory = Bukkit.createInventory(null, 27, FILTERS_TITLE);

		for (Material material : data.filters) {
			inventory.addItem(new ItemStack(material));
		}

		if (data.filterMode == FilterMode.WHITELIST) {
			ItemStack item = makeItem(Material.WOOL, "§aFilter Mode", Collections.singletonList("§fWhitelist"));
			item.setDurability((short) 0);
			inventory.setItem(26, item);
		} else if (data.filterMode == FilterMode.BLACKLIST) {
			ItemStack item = makeItem(Material.WOOL, "§aFilter Mode", Collections.singletonList("§8Blacklist"));
			item.setDurability((short) 15);
			inventory.setItem(26, item);
		}

		player.openInventory(inventory);
	}

	@EventHandler
	public void on(InventoryClickEvent event) {
		Inventory inventory = event.getClickedInventory();
		if (inventory == null) { return; }
		if (CONTROLS_TITLE.equals(inventory.getTitle())) {
			event.setCancelled(true);

			if (event.getCurrentItem() != null) {
				Block targetBlock = event.getWhoClicked().getTargetBlock((Set<Material>) null, 5);
				if (targetBlock == null) {
					event.getWhoClicked().sendMessage(PREFIX + "§cNot looking at a quarry block");
					event.getWhoClicked().closeInventory();
					return;
				}
				QuarryData data = this.registry.getByLocation(targetBlock.getLocation());
				if (data == null) {
					event.getWhoClicked().sendMessage(PREFIX + "§cInvalid quarry");
					event.getWhoClicked().closeInventory();
					return;
				}

				ItemStack item = event.getCurrentItem();
				if (item.getType() == Material.HOPPER) {
					event.getWhoClicked().closeInventory();
					openFilterInventory((Player) event.getWhoClicked(), targetBlock.getLocation());
				} else if (item.getType() == Material.WOOL) {
					if (data.active) {
						this.runner.stop(data.getHash());

						data.active = false;
						event.getWhoClicked().sendMessage(PREFIX + "§bQuarry stopped");
					} else {
						this.runner.start(data);

						event.getWhoClicked().sendMessage(PREFIX + "§bQuarry started");
					}
					event.getWhoClicked().closeInventory();
				} else if (item.getType() == Material.WOOD_PICKAXE || item.getType() == Material.STONE_PICKAXE || item.getType() == Material.IRON_PICKAXE || item.getType() == Material.DIAMOND_PICKAXE) {
					ItemStack cursor = event.getCursor();
					if (cursor == null) { return; }
					if (cursor.getType() == Material.STONE_PICKAXE) {
						data.speed = 1;
					} else if (cursor.getType() == Material.IRON_PICKAXE) {
						data.speed = 2;
					} else if (cursor.getType() == Material.DIAMOND_PICKAXE) {
						data.speed = 3;
					} else {
						return;
					}
					event.setCurrentItem(null);
					event.setCancelled(false);

					Bukkit.getScheduler().runTaskLater(this, () -> {
						event.getWhoClicked().closeInventory();
						openControlsInventory((Player) event.getWhoClicked(), targetBlock.getLocation());
					}, 1);
				} else if (item.getType() == Material.BOOK || item.getType() == Material.ENCHANTED_BOOK) {
					ItemStack cursor = event.getCursor();
					if (cursor == null) { return; }
					if (cursor.getType() == Material.ENCHANTED_BOOK) {
						ItemMeta meta = cursor.getItemMeta();
						if (meta instanceof EnchantmentStorageMeta) {
							EnchantmentStorageMeta enchMeta = (EnchantmentStorageMeta) meta;

							if (enchMeta.getStoredEnchants().containsKey(Enchantment.SILK_TOUCH)) {
								data.addUpgrade(Upgrade.SILK_TOUCH);
							} else if (enchMeta.getStoredEnchants().containsKey(Enchantment.LOOT_BONUS_BLOCKS)) {
								data.addUpgrade(Upgrade.FORTUNE);
							} else {
								return;
							}
							event.setCurrentItem(null);
							event.setCancelled(false);

							Bukkit.getScheduler().runTaskLater(this, () -> {
								event.getWhoClicked().closeInventory();
								openControlsInventory((Player) event.getWhoClicked(), targetBlock.getLocation());
							}, 1);
						}
					}
				} else if (item.getType() == Material.WOOD_PLATE || item.getType() == Material.STONE_PLATE || item.getType() == Material.GOLD_PLATE || item.getType() == Material.IRON_PLATE) {
					ItemStack cursor = event.getCursor();
					if (cursor == null) { return; }
					if (cursor.getType() == Material.STONE_PLATE) {
						data.size = 8;
					} else if (cursor.getType() == Material.IRON_PLATE) {
						data.size = 16;
					} else if (cursor.getType() == Material.GOLD_PLATE) {
						data.size = 32;
					} else {
						return;
					}

					event.setCurrentItem(null);
					event.setCancelled(false);

					Bukkit.getScheduler().runTaskLater(this, () -> {
						event.getWhoClicked().closeInventory();
						openControlsInventory((Player) event.getWhoClicked(), targetBlock.getLocation());
					}, 1);
				}
			}
		} else if (FILTERS_TITLE.equals(inventory.getTitle())) {
			event.setCancelled(true);

			if (event.getCurrentItem() != null) {
				Block targetBlock = event.getWhoClicked().getTargetBlock((Set<Material>) null, 5);
				if (targetBlock == null) {
					event.getWhoClicked().sendMessage(PREFIX + "§cNot looking at a quarry block");
					event.getWhoClicked().closeInventory();
					return;
				}
				QuarryData data = this.registry.getByLocation(targetBlock.getLocation());
				if (data == null) {
					event.getWhoClicked().sendMessage(PREFIX + "§cInvalid quarry");
					event.getWhoClicked().closeInventory();
					return;
				}

				ItemStack item = event.getCurrentItem();
				if (item.getType() == Material.WOOL) {// filter mode toggle
					ItemMeta meta = item.getItemMeta();
					if (meta.getDisplayName() != null && "§aFilter Mode".equals(meta.getDisplayName())) {
						if (data.filterMode == FilterMode.WHITELIST) {
							data.filterMode = FilterMode.BLACKLIST;
						} else {
							data.filterMode = FilterMode.WHITELIST;
						}

						Bukkit.getScheduler().runTaskLater(this, () -> {
							event.getWhoClicked().closeInventory();
							openFilterInventory((Player) event.getWhoClicked(), targetBlock.getLocation());
						}, 1);
					}
				} else {// All other items added/removed as filter
					if (!FILTERS_TITLE.equals(event.getClickedInventory().getTitle())) {
						//						event.setCancelled(false);
						return;
					}
					ItemStack cursor = event.getCursor();
					if (item.getType() == Material.AIR && cursor != null && cursor.getType() != Material.AIR) {
						data.addFilter(cursor.getType());
					} else if (item.getType() != Material.AIR && (cursor == null || cursor.getType() == Material.AIR)) {
						data.removeFilter(item.getType());
					}

					for (int i = 0; i < 26; i++) {
						inventory.setItem(i, null);
					}
					for (Material material : data.filters) {
						inventory.addItem(new ItemStack(material));
					}
					Bukkit.getScheduler().runTaskLater(this, new Runnable() {
						@Override
						public void run() {
							((Player) event.getWhoClicked()).updateInventory();
						}
					}, 1);

					//					event.setCurrentItem(null);
					//					event.setCancelled(false);

					//					Bukkit.getScheduler().runTaskLater(this, () -> {
					////						event.getWhoClicked().closeInventory();
					//						openFilterInventory((Player) event.getWhoClicked(), targetBlock.getLocation());
					//					}, 1);
				}
			}
		}
	}

	ItemStack makeItem(Material material, String title) {
		ItemStack itemStack = new ItemStack(material);
		ItemMeta meta = itemStack.getItemMeta();
		meta.setDisplayName(title);
		itemStack.setItemMeta(meta);
		return itemStack;
	}

	ItemStack makeItem(Material material, String title, List<String> lore) {
		ItemStack itemStack = new ItemStack(material);
		ItemMeta meta = itemStack.getItemMeta();
		meta.setDisplayName(title);
		meta.setLore(lore);
		itemStack.setItemMeta(meta);
		return itemStack;
	}

}
