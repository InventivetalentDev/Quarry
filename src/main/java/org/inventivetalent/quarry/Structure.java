package org.inventivetalent.quarry;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

public class Structure {

	public static final Structure QUARRY = new Structure(new Material[][][] {
			{
					{ Material.FENCE, Material.FENCE, Material.FENCE },
					{ Material.FENCE, Material.DISPENSER, Material.FENCE },
					{ Material.FENCE, Material.FENCE, Material.FENCE }
			},
			{
					{ null, null, null },
					{ null, Material.CHEST, null },
					{ null, null, null }
			}
	});

	private final Material[][][] structure;

	public Structure(Material[][][] structure) {
		this.structure = structure;
	}

	public Material[][][] getStructure() {
		return structure;
	}

	public Material materialAt(int x, int y, int z) {
		if (y > this.structure.length) {
			return null;
		}
		Material[][] yArray = this.structure[y];

		if (z > yArray.length) {
			return null;
		}
		Material[] zArray = this.structure[y][z];

		if (x > zArray.length) {
			return null;
		}
		return zArray[x];
	}

	public boolean test(Block block) {
		Material type = block.getType();

		int ySize = this.structure.length;
		int zSize = 0;
		int xSize = 0;

		// First, find the specified block's position in the structure layout
		Vector relativeBlockPos = null;
		for (int y = 0; y < this.structure.length; y++) {
			int zS = this.structure[y].length;
			if (zS > zSize) { zSize = zS; }
			for (int z = 0; z < zS; z++) {
				int xS = this.structure[y][z].length;
				if (xS > xSize) { xSize = xS; }
				for (int x = 0; x < xS; x++) {
					Material structureType = this.structure[y][z][x];

					if (structureType == type) {
						relativeBlockPos = new Vector(x, y, z);
					}
				}
			}
		}
		if (relativeBlockPos == null) {
			return false;
		}

		// Shift the block's world position by the relative structure position
		Vector startPos = block.getLocation().toVector().subtract(relativeBlockPos);

		for(int x = 0;x<xSize;x++) {
			for(int z = 0;z<zSize;z++) {
				for(int y = 0;y<ySize;y++) {
					Material structureMaterial = materialAt(x, y, z);
					if (structureMaterial == null) {
						// no material specified, so just ignore it
						continue;
					}

					Vector testPos = new Vector(x, y, z).add(startPos);
					Block testBlock = block.getWorld().getBlockAt(testPos.toLocation(block.getWorld()));
					Material testMaterial = testBlock.getType();

					if (testMaterial != structureMaterial) {
						// materials don't match -> stop and return false
						return false;
					}else{
						block.getWorld().playEffect(testBlock.getLocation().add(0.5D, 0.5D, 0.5D), Effect.VILLAGER_PLANT_GROW, 0);
					}
				}
			}
		}

		// no mismatches -> return true
		return true;
	}

}
