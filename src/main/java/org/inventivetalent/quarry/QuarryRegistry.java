package org.inventivetalent.quarry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class QuarryRegistry {

	protected static MessageDigest MD5;

	static {
		try {
			MD5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private Quarry plugin;
	private final File dataDir;
	private final Map<String, QuarryData> byHash = new HashMap<>();

	private boolean saveLock = false;

	public static String makeLocationHash(String world, int x, int y, int z) {
		MD5.reset();
		MD5.update(StandardCharsets.UTF_8.encode(world + ";" + x + ";" + y + ";" + z));
		return String.format("%032x", new BigInteger(1, MD5.digest()));
	}

	public QuarryRegistry(Quarry plugin,File dataDir) {
		this.plugin=plugin;
		this.dataDir = dataDir;
		if (!this.dataDir.exists()) {
			this.dataDir.mkdirs();
		}
	}

	public QuarryData register(Location location) {
		QuarryData data = new QuarryData(location);
		String hash = data.getHash();

		if (this.byHash.containsKey(hash)) {
			throw new IllegalArgumentException("Quarry at this position is already registered");
		}

		this.byHash.put(hash, data);
		return data;
	}

	private QuarryData register(QuarryData data) {
		String hash = data.getHash();

		if (this.byHash.containsKey(hash)) {
			throw new IllegalArgumentException("Quarry with this hash is already registered");
		}

		this.byHash.put(hash, data);
		saveQuarry(hash, data);

		if (data.active) {// was active (probably before restarting the server)
			// Auto-start the quarry
			Bukkit.getScheduler().runTaskLater(this.plugin, () -> QuarryRegistry.this.plugin.runner.start(data), 20L);
		}

		return data;
	}

	public QuarryData unregister(Location location) {
		String hash = makeLocationHash(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
		deleteDataFile(hash);
		return byHash.remove(hash);
	}

	public QuarryData getByHash(String hash) {
		return byHash.get(hash);
	}

	public QuarryData getByLocation(Location location) {
		String hash = makeLocationHash(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
		return byHash.get(hash);
	}

	protected void deleteDataFile(String hash) {
		File file = new File(this.dataDir, hash + ".qry");
		file.delete();
	}

	public void save() {
		if (saveLock) { return; }
		saveLock = true;

		for (QuarryData data : this.byHash.values()) {
			String hash = data.getHash();

			saveQuarry(hash, data);
		}

		saveLock = false;
	}

	protected boolean saveQuarry(String hash,QuarryData data) {
		File file = new File(this.dataDir, hash + ".qry");
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				System.err.println("Failed to create quarry data file: " + hash);
				e.printStackTrace();
				return false;
			}
		}

		String serialized = data.serialize().toString();
		try (FileWriter writer = new FileWriter(file)) {
			writer.write(serialized);
		} catch (IOException e) {
			System.err.println("Failed to write quarry data file: " + hash);
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public void load() {
		JsonParser parser = new JsonParser();
		for (File file : this.dataDir.listFiles()) {
			String fileName = file.getName();
			if (fileName.endsWith(".qry")) {
				try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
					JsonObject json = parser.parse(reader).getAsJsonObject();
					QuarryData data = QuarryData.deserialize(json);
					this.register(data);
				} catch (IOException e) {
					System.err.println("Failed to read quarry data file: " + fileName);
					e.printStackTrace();
				}

			}
		}
	}

}
