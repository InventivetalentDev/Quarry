package org.inventivetalent.quarry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

	public UUID owner;

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
		main.addProperty("owner", this.owner.toString());

		JsonArray filterArray = new JsonArray();
		for (Material filter : this.filters) {
			filterArray.add(new JsonPrimitive(filter.name()));
		}
		main.add("filters", filterArray);

		JsonArray upgradeArray = new JsonArray();
		for (Upgrade upgrade : this.upgrades) {
			upgradeArray.add(new JsonPrimitive(upgrade.name()));
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
		data.owner = UUID.fromString(json.get("owner").getAsString());

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
