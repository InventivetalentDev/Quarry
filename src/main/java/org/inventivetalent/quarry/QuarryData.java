package org.inventivetalent.quarry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class QuarryData {

	public final String world;
	public final int    x;
	public final int    y;
	public final int    z;

	public boolean active;
	public int     digX;
	public int     digY;
	public int     digZ;
	public final List<Material> filters    = new ArrayList<>();//TODO: should probably also contain block data (maybe wait for 1.13)
	public       FilterMode     filterMode = FilterMode.BLACKLIST;
	public final List<Upgrade>  upgrades   = new ArrayList<>();
	public       int            speed      = 0;
	public       int            size       = 4;
	public int blocksTotal;
	public int blocksScanned;
	public int blocksMined;

	private final String hash;

	public QuarryData(Location location) {
		this(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
	}

	public QuarryData(World world, int x, int y, int z) {
		this(world.getName(), x, y, z);
	}

	public QuarryData(String world, int x, int y, int z) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;

		this.hash = QuarryRegistry.makeLocationHash(world, x, y, z);
		System.out.println("Hash: " + this.hash);
	}

	public String getHash() {
		return hash;
	}

	public void addFilter(Material filter) {
		if (filter == null) { return; }
		if (!this.filters.contains(filter)) {
			this.filters.add(filter);
		}
	}

	public void removeFilter(Material filter) {
		this.filters.remove(filter);
	}

	public void clearFilters() {
		this.filters.clear();
	}

	public void addUpgrade(Upgrade upgrade) {
		if (upgrade == null) { return; }
		if (!this.upgrades.contains(upgrade)) {
			this.upgrades.add(upgrade);
		}
	}

	public void removeUpgrade(Upgrade upgrade) {
		this.upgrades.remove(upgrade);
	}

	public boolean hasUpgrade(Upgrade upgrade) {
		return this.upgrades.contains(upgrade);
	}

	public void clearUpgrades() {
		this.upgrades.clear();
	}

	public boolean shouldMine(Material material) {
		if (this.filterMode == FilterMode.WHITELIST) {
			return this.filters.contains(material);
		} else if (this.filterMode == FilterMode.BLACKLIST) {
			return !this.filters.contains(material);
		}
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) { return true; }
		if (o == null || getClass() != o.getClass()) { return false; }

		QuarryData that = (QuarryData) o;

		if (x != that.x) { return false; }
		if (y != that.y) { return false; }
		if (z != that.z) { return false; }
		if (active != that.active) { return false; }
		if (digX != that.digX) { return false; }
		if (digY != that.digY) { return false; }
		if (digZ != that.digZ) { return false; }
		if (speed != that.speed) { return false; }
		if (size != that.size) { return false; }
		if (blocksTotal != that.blocksTotal) { return false; }
		if (blocksScanned != that.blocksScanned) { return false; }
		if (blocksMined != that.blocksMined) { return false; }
		if (!world.equals(that.world)) { return false; }
		if (!filters.equals(that.filters)) { return false; }
		if (filterMode != that.filterMode) { return false; }
		if (!upgrades.equals(that.upgrades)) { return false; }
		return hash.equals(that.hash);
	}

	@Override
	public int hashCode() {
		int result = world.hashCode();
		result = 31 * result + x;
		result = 31 * result + y;
		result = 31 * result + z;
		result = 31 * result + (active ? 1 : 0);
		result = 31 * result + digX;
		result = 31 * result + digY;
		result = 31 * result + digZ;
		result = 31 * result + filters.hashCode();
		result = 31 * result + filterMode.hashCode();
		result = 31 * result + upgrades.hashCode();
		result = 31 * result + speed;
		result = 31 * result + size;
		result = 31 * result + blocksTotal;
		result = 31 * result + blocksScanned;
		result = 31 * result + blocksMined;
		result = 31 * result + hash.hashCode();
		return result;
	}

	public JsonObject serialize() {
		JsonObject main = new JsonObject();
		main.addProperty("hash", this.hash);
		main.addProperty("world", this.world);
		main.addProperty("x", this.x);
		main.addProperty("y", this.y);
		main.addProperty("z", this.z);
		main.addProperty("active", this.active);
		main.addProperty("digX", this.digX);
		main.addProperty("digY", this.digY);
		main.addProperty("digZ", this.digZ);

		main.addProperty("speed", this.speed);
		main.addProperty("size", this.size);
		main.addProperty("filterMode", this.filterMode.name());
		main.addProperty("blocksTotal", this.blocksTotal);
		main.addProperty("blocksScanned", this.blocksScanned);
		main.addProperty("blocksMined",this.blocksMined);

		JsonArray filterArray = new JsonArray();
		for (Material filter : this.filters) {
			filterArray.add(filter.name());
		}
		main.add("filters", filterArray);

		JsonArray upgradeArray = new JsonArray();
		for (Upgrade upgrade : this.upgrades) {
			upgradeArray.add(upgrade.name());
		}
		main.add("upgrades", upgradeArray);

		return main;
	}

	public static QuarryData deserialize(JsonObject json) {
		String world = json.get("world").getAsString();
		int x = json.get("x").getAsInt();
		int y = json.get("y").getAsInt();
		int z = json.get("z").getAsInt();

		QuarryData data = new QuarryData(world, x, y, z);

		data.active = json.get("active").getAsBoolean();
		data.digX = json.get("digX").getAsInt();
		data.digY = json.get("digY").getAsInt();
		data.digZ = json.get("digZ").getAsInt();

		data.speed = json.get("speed").getAsInt();
		data.size = json.get("size").getAsInt();
		data.filterMode = FilterMode.valueOf(json.get("filterMode").getAsString());
		data.blocksTotal = json.get("blocksTotal").getAsInt();
		data.blocksScanned = json.get("blocksScanned").getAsInt();
		data.blocksMined = json.get("blocksMined").getAsInt();

		JsonArray filterArray = json.get("filters").getAsJsonArray();
		for (JsonElement element : filterArray) {
			data.filters.add(Material.valueOf(element.getAsString()));
		}

		JsonArray upgradeArray = json.get("upgrades").getAsJsonArray();
		for (JsonElement element : upgradeArray) {
			data.upgrades.add(Upgrade.valueOf(element.getAsString()));
		}

		return data;
	}

}
