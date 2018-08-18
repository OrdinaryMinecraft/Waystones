package net.blay09.mods.waystones;

import io.netty.buffer.ByteBuf;
import net.blay09.mods.waystones.util.BlockPos;
import net.blay09.mods.waystones.util.WaystoneEntry;
import net.minecraftforge.common.config.Configuration;

import java.util.Collection;

public class WaystoneConfig {

	public static int teleportButtonX;
	public static int teleportButtonY;
	public static boolean disableParticles;
	public static boolean disableTextGlow;

	public boolean teleportButton;
	public int teleportButtonCooldown;
	public boolean teleportButtonReturnOnly;

	public boolean allowReturnScrolls;
	public boolean allowWarpStone;

	public int warpStoneCooldown;

	public boolean interDimension;

	public boolean creativeModeOnly;
	public boolean setSpawnPoint;

	public boolean globalNoCooldown;
	public boolean globalInterDimension;

	public String stopWorldName;
	public int stopX1;
	public int stopX2;
	public int stopZ1;
	public int stopZ2;

	public void reloadLocal(Configuration config) {
		teleportButton = config.getBoolean("Teleport Button in GUI", "general", false, "Should there be a button in the inventory to access the waystone menu?");
		teleportButtonCooldown = config.getInt("Teleport Button Cooldown", "general", 300, 0, 86400, "The cooldown between usages of the teleport button in seconds.");
		teleportButtonReturnOnly = config.getBoolean("Teleport Button Return Only", "general", false, "If true, the teleport button will only let you return to the last activated waystone, instead of allowing to choose.");

		allowReturnScrolls = config.getBoolean("Allow Return Scrolls", "general", true, "If true, return scrolls will be craftable.");
		allowWarpStone = config.getBoolean("Allow Warp Stone", "general", true, "If true, the warp stone will be craftable.");

		teleportButtonX = config.getInt("Teleport Button GUI X", "client", 60, -100, 250, "The x position of the warp button in the inventory.");
		teleportButtonY = config.getInt("Teleport Button GUI Y", "client", 60, -100, 250, "The y position of the warp button in the inventory.");
		disableTextGlow = config.getBoolean("Disable Text Glow", "client", false, "If true, the text overlay on waystones will no longer always render at full brightness.");
		disableParticles = config.getBoolean("Disable Particles", "client", false, "If true, activated waystones will not emit particles.");

		warpStoneCooldown = config.getInt("Warp Stone Cooldown", "general", 300, 0, 86400, "The cooldown between usages of the warp stone in seconds.");

		setSpawnPoint = config.getBoolean("Set Spawnpoint on Activation", "general", false, "If true, the player's spawnpoint will be set to the last activated waystone.");
		interDimension = config.getBoolean("Interdimensional Teleport", "general", true, "If true, all waystones work inter-dimensionally.");

		creativeModeOnly = config.getBoolean("Creative Mode Only", "general", false, "If true, waystones can only be placed in creative mode.");

		globalNoCooldown = config.getBoolean("No Cooldown on Global Waystones", "general", true, "If true, waystones marked as global have no cooldown.");
		globalInterDimension = config.getBoolean("Interdimensional Teleport on Global Waystones", "general", true, "If true, waystones marked as global work inter-dimensionally.");

		stopWorldName = config.getString("stopWorld", "stop", "3", "DIM ID");
		stopX1 = config.getInt("stopX1", "stop", 0, -99999, 99999, "X1");
		stopX2 = config.getInt("stopX2", "stop", 0, -99999, 99999, "X2");
		stopZ1 = config.getInt("stopZ1", "stop", 0, -99999, 99999, "Z1");
		stopZ2 = config.getInt("stopZ2", "stop", 0, -99999, 99999, "Z2");

		String[] serverWaystoneData = config.getStringList("Server Waystones", "generated", new String[0], "This option is automatically populated by the server when using the Server Hub Mode. Do not change.");
		WaystoneEntry[] serverWaystones = new WaystoneEntry[serverWaystoneData.length];
		for(int i = 0; i < serverWaystones.length; i++) {
			String[] split = serverWaystoneData[i].split("\u00a7");
			if(split.length < 3) {
				serverWaystones[i] = new WaystoneEntry("Invalid Waystone", 0, new BlockPos(0, 0, 0));
				serverWaystones[i].setGlobal(true);
				continue;
			}
			try {
				int dimensionId = Integer.parseInt(split[1]);
				String[] pos = split[2].split(",");
				serverWaystones[i] = new WaystoneEntry(split[0], dimensionId, new BlockPos(Integer.parseInt(pos[0]), Integer.parseInt(pos[1]), Integer.parseInt(pos[2])));
				serverWaystones[i].setGlobal(true);
			} catch (NumberFormatException e) {
				serverWaystones[i] = new WaystoneEntry("Invalid Waystone", 0, new BlockPos(0, 0, 0));
				serverWaystones[i].setGlobal(true);
			}
		}
		WaystoneManager.setServerWaystones(serverWaystones);
	}

	public static void storeServerWaystones(Configuration config, Collection<WaystoneEntry> entries) {
		String[] serverWaystones = new String[entries.size()];
		int i = 0;
		for(WaystoneEntry entry : entries) {
			serverWaystones[i] = entry.getName() + "\u00a7" + entry.getDimensionId() + "\u00a7" + entry.getPos().getX() + "," + entry.getPos().getY() + "," + entry.getPos().getZ();
			i++;
		}
		config.get("generated", "Server Waystones", "This option is automatically populated by the server when using the Server Hub Mode. Do not change.").set(serverWaystones);
		if(config.hasChanged()) {
			config.save();
		}
	}

	public static WaystoneConfig read(ByteBuf buf) {
		WaystoneConfig config = new WaystoneConfig();
		config.teleportButton = buf.readBoolean();
		config.teleportButtonCooldown = buf.readInt();
		config.teleportButtonReturnOnly = buf.readBoolean();
		config.warpStoneCooldown = buf.readInt();
		config.interDimension = buf.readBoolean();
		config.creativeModeOnly = buf.readBoolean();
		config.setSpawnPoint = buf.readBoolean();
		return config;
	}

	public void write(ByteBuf buf) {
		buf.writeBoolean(teleportButton);
		buf.writeInt(teleportButtonCooldown);
		buf.writeBoolean(teleportButtonReturnOnly);
		buf.writeInt(warpStoneCooldown);
		buf.writeBoolean(interDimension);
		buf.writeBoolean(creativeModeOnly);
		buf.writeBoolean(setSpawnPoint);
	}
}
