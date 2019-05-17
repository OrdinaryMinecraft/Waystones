package net.blay09.mods.waystones;

import com.google.common.collect.Maps;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import net.blay09.mods.waystones.block.TileWaystone;
import net.blay09.mods.waystones.network.message.MessageDimension;
import net.blay09.mods.waystones.network.message.MessageTeleportEffect;
import net.blay09.mods.waystones.network.message.MessageWaystones;
import net.blay09.mods.waystones.network.NetworkHandler;
import net.blay09.mods.waystones.util.BlockPos;
import net.blay09.mods.waystones.util.MyTeleporter;
import net.blay09.mods.waystones.util.WaystoneEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S1DPacketEntityEffect;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.world.Teleporter;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class WaystoneManager {

    private static final Map<String, WaystoneEntry> serverWaystones = Maps.newHashMap();
    private static final Map<String, WaystoneEntry> knownWaystones = Maps.newHashMap();

    public static void activateWaystone(EntityPlayer player, TileWaystone waystone) {
        WaystoneEntry serverWaystone = getServerWaystone(waystone.getWaystoneName());
        if(serverWaystone != null) {
            PlayerWaystoneData.setLastServerWaystone(player, serverWaystone);
            sendPlayerWaystones(player);
            return;
        }
        PlayerWaystoneData.resetLastServerWaystone(player);
        removePlayerWaystone(player, new WaystoneEntry(waystone));
        addPlayerWaystone(player, waystone);
        sendPlayerWaystones(player);
    }

    public static void sendPlayerWaystones(EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            PlayerWaystoneData waystoneData = PlayerWaystoneData.fromPlayer(player);
            NetworkHandler.channel.sendTo(new MessageWaystones(waystoneData.getWaystones(), getServerWaystones().toArray(new WaystoneEntry[getServerWaystones().size()]), waystoneData.getLastServerWaystoneName(), waystoneData.getLastFreeWarp(), waystoneData.getLastWarpStoneUse()), (EntityPlayerMP) player);
        }
    }

    public static void addPlayerWaystone(EntityPlayer player, TileWaystone waystone) {
        NBTTagCompound tagCompound = PlayerWaystoneData.getOrCreateWaystonesTag(player);
        NBTTagList tagList = tagCompound.getTagList(PlayerWaystoneData.WAYSTONE_LIST, Constants.NBT.TAG_COMPOUND);
        tagList.appendTag(new WaystoneEntry(waystone).writeToNBT());
        tagCompound.setTag(PlayerWaystoneData.WAYSTONE_LIST, tagList);
    }

    public static boolean removePlayerWaystone(EntityPlayer player, WaystoneEntry waystone) {
        NBTTagCompound tagCompound = PlayerWaystoneData.getWaystonesTag(player);
        NBTTagList tagList = tagCompound.getTagList(PlayerWaystoneData.WAYSTONE_LIST, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < tagList.tagCount(); i++) {
            NBTTagCompound entryCompound = tagList.getCompoundTagAt(i);
            if (WaystoneEntry.read(entryCompound).equals(waystone)) {
                tagList.removeTag(i);
                return true;
            }
        }
        return false;
    }

    public static boolean checkAndUpdateWaystone(EntityPlayer player, WaystoneEntry waystone) {
        WaystoneEntry serverEntry = getServerWaystone(waystone.getName());
        if(serverEntry != null) {
            if(getWaystoneInWorld(serverEntry) == null) {
                removeServerWaystone(serverEntry);
                return false;
            }
            if(removePlayerWaystone(player, waystone)) {
                sendPlayerWaystones(player);
            }
            return true;
        }
        NBTTagCompound tagCompound = PlayerWaystoneData.getWaystonesTag(player);
        NBTTagList tagList = tagCompound.getTagList(PlayerWaystoneData.WAYSTONE_LIST, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < tagList.tagCount(); i++) {
            NBTTagCompound entryCompound = tagList.getCompoundTagAt(i);
            if (WaystoneEntry.read(entryCompound).equals(waystone)) {
                TileWaystone tileEntity = getWaystoneInWorld(waystone);
                if (tileEntity != null) {
                    if (!entryCompound.getString("Name").equals(tileEntity.getWaystoneName())) {
                        entryCompound.setString("Name", tileEntity.getWaystoneName());
                        sendPlayerWaystones(player);
                    }
                    return true;
                } else {
                    removePlayerWaystone(player, waystone);
                    sendPlayerWaystones(player);
                }
                return false;
            }
        }
        return false;
    }

    public static TileWaystone getWaystoneInWorld(WaystoneEntry waystone) {
        World targetWorld = MinecraftServer.getServer().worldServerForDimension(waystone.getDimensionId());
        TileEntity tileEntity = targetWorld.getTileEntity(waystone.getPos().getX(), waystone.getPos().getY(), waystone.getPos().getZ());
        if(tileEntity instanceof TileWaystone) {
            return (TileWaystone) tileEntity;
        }
        return null;
    }

    public static boolean isDimensionWarpAllowed(WaystoneEntry waystone) {
        return waystone.isGlobal() ? Waystones.getConfig().globalInterDimension : Waystones.getConfig().interDimension;
    }

    public static boolean isBetween(int a, int b, int c) {
        return b > a ? c > a && c < b : c > b && c < a;
    }

    public static boolean teleportToWaystone(EntityPlayer player, WaystoneEntry waystone) {
        if (player.worldObj.provider.dimensionId == Integer.parseInt(Waystones.getConfig().stopWorldName) && isBetween(Waystones.getConfig().stopX1, Waystones.getConfig().stopX2, (int) player.posX) && isBetween(Waystones.getConfig().stopZ1, Waystones.getConfig().stopZ2, (int) player.posZ)) {
        	player.addChatComponentMessage(new ChatComponentTranslation("waystones:noDimensionWarp"));
        	return false;
        }

        if(!checkAndUpdateWaystone(player, waystone)) {
            ChatComponentTranslation chatComponent = new ChatComponentTranslation("waystones:waystoneBroken");
            chatComponent.getChatStyle().setColor(EnumChatFormatting.RED);
            player.addChatComponentMessage(chatComponent);
            return false;
        }
        WaystoneEntry serverEntry = getServerWaystone(waystone.getName());
        World targetWorld = MinecraftServer.getServer().worldServerForDimension(waystone.getDimensionId());
        int x = waystone.getPos().getX();
        int y = waystone.getPos().getY();
        int z = waystone.getPos().getZ();
        boolean dimensionWarp = waystone.getDimensionId() != player.getEntityWorld().provider.dimensionId;
        if (dimensionWarp && !Waystones.getConfig().interDimension && !(serverEntry == null || !Waystones.getConfig().globalInterDimension)) {
            player.addChatComponentMessage(new ChatComponentTranslation("waystones:noDimensionWarp"));
            return false;
        }
        sendTeleportEffect(player.worldObj, new BlockPos(player));
        player.addPotionEffect(new PotionEffect(Potion.blindness.getId(), 20, 3));
        if(dimensionWarp) {
            NetworkHandler.channel.sendTo(new MessageDimension(targetWorld.provider.dimensionId), (EntityPlayerMP) player);
            MinecraftServer minecraftserver = MinecraftServer.getServer();
            WorldServer worldserver = minecraftserver.worldServerForDimension(targetWorld.provider.dimensionId);
            transferPlayerToDimension(((EntityPlayerMP) player), waystone.getDimensionId(), new MyTeleporter(worldserver), minecraftserver);
        }
        ForgeDirection facing = ForgeDirection.getOrientation(targetWorld.getBlockMetadata(x, y, z));
        BlockPos targetPos = waystone.getPos().offset(facing);
        player.rotationYaw = getRotationYaw(facing);
        player.setPositionAndUpdate(targetPos.getX() > 0 ? targetPos.getX() + 0.5 : targetPos.getX() - 0.5,targetPos.getY() + 0.5, targetPos.getZ() > 0 ? targetPos.getZ() + 0.5 : targetPos.getZ() - 0.5);
        sendTeleportEffect(player.worldObj, targetPos);
        return true;
    }

    public static void transferPlayerToDimension(EntityPlayerMP p_72356_1_, int p_72356_2_, Teleporter teleporter, MinecraftServer mcServer)
    {
        int j = p_72356_1_.dimension;
        WorldServer worldserver = mcServer.worldServerForDimension(p_72356_1_.dimension);
        p_72356_1_.dimension = p_72356_2_;
        WorldServer worldserver1 = mcServer.worldServerForDimension(p_72356_1_.dimension);
        p_72356_1_.playerNetServerHandler.sendPacket(new S07PacketRespawn(p_72356_1_.dimension, worldserver1.difficultySetting, worldserver1.getWorldInfo().getTerrainType(), p_72356_1_.theItemInWorldManager.getGameType())); // Forge: Use new dimensions information
        worldserver.removePlayerEntityDangerously(p_72356_1_);
        p_72356_1_.isDead = false;
        transferEntityToWorld(p_72356_1_, j, worldserver, worldserver1, teleporter);
        mcServer.getConfigurationManager().func_72375_a(p_72356_1_, worldserver);
        p_72356_1_.playerNetServerHandler.setPlayerLocation(p_72356_1_.posX, p_72356_1_.posY, p_72356_1_.posZ, p_72356_1_.rotationYaw, p_72356_1_.rotationPitch);
        p_72356_1_.theItemInWorldManager.setWorld(worldserver1);
        mcServer.getConfigurationManager().updateTimeAndWeatherForPlayer(p_72356_1_, worldserver1);
        mcServer.getConfigurationManager().syncPlayerInventory(p_72356_1_);
        Iterator iterator = p_72356_1_.getActivePotionEffects().iterator();

        while (iterator.hasNext())
        {
            PotionEffect potioneffect = (PotionEffect)iterator.next();
            p_72356_1_.playerNetServerHandler.sendPacket(new S1DPacketEntityEffect(p_72356_1_.getEntityId(), potioneffect));
        }
        FMLCommonHandler.instance().firePlayerChangedDimensionEvent(p_72356_1_, j, p_72356_2_);
    }


    public static void transferEntityToWorld(Entity p_82448_1_, int p_82448_2_, WorldServer p_82448_3_, WorldServer p_82448_4_, Teleporter teleporter)
    {
        WorldProvider pOld = p_82448_3_.provider;
        WorldProvider pNew = p_82448_4_.provider;
        double moveFactor = pOld.getMovementFactor() / pNew.getMovementFactor();
        double d0 = p_82448_1_.posX * moveFactor;
        double d1 = p_82448_1_.posZ * moveFactor;
        double d3 = p_82448_1_.posX;
        double d4 = p_82448_1_.posY;
        double d5 = p_82448_1_.posZ;
        float f = p_82448_1_.rotationYaw;
        p_82448_3_.theProfiler.startSection("moving");

        if (p_82448_1_.dimension == 1)
        {
            ChunkCoordinates chunkcoordinates;

            if (p_82448_2_ == 1)
            {
                chunkcoordinates = p_82448_4_.getSpawnPoint();
            }
            else
            {
                chunkcoordinates = p_82448_4_.getEntrancePortalLocation();
            }

            d0 = (double)chunkcoordinates.posX;
            d1 = (double)chunkcoordinates.posZ;

            if (p_82448_1_.isEntityAlive())
            {
                p_82448_3_.updateEntityWithOptionalForce(p_82448_1_, false);
            }
        }

        p_82448_3_.theProfiler.endSection();

        if (p_82448_2_ != 1)
        {
            p_82448_3_.theProfiler.startSection("placing");
            d0 = (double) MathHelper.clamp_int((int)d0, -29999872, 29999872);
            d1 = (double) MathHelper.clamp_int((int)d1, -29999872, 29999872);

            if (p_82448_1_.isEntityAlive())
            {
                teleporter.placeInPortal(p_82448_1_, d3, d4, d5, f);
                p_82448_4_.spawnEntityInWorld(p_82448_1_);
                p_82448_4_.updateEntityWithOptionalForce(p_82448_1_, false);
            }

            p_82448_3_.theProfiler.endSection();
        }

        p_82448_1_.setWorld(p_82448_4_);
    }

    public static void sendTeleportEffect(World world, BlockPos pos) {
        NetworkHandler.channel.sendToAllAround(new MessageTeleportEffect(pos), new NetworkRegistry.TargetPoint(world.provider.dimensionId, pos.getX(), pos.getY(), pos.getZ(), 64));
    }

    public static float getRotationYaw(ForgeDirection facing) {
        switch(facing) {
            case NORTH:
                return 180f;
            case SOUTH:
                return 0f;
            case WEST:
                return 90f;
            case EAST:
                return -90f;
        }
        return 0f;
    }

    public static void addServerWaystone(WaystoneEntry entry) {
        serverWaystones.put(entry.getName(), entry);
        WaystoneConfig.storeServerWaystones(Waystones.configuration, serverWaystones.values());
    }

    public static void removeServerWaystone(WaystoneEntry entry) {
        serverWaystones.remove(entry.getName());
        WaystoneConfig.storeServerWaystones(Waystones.configuration, serverWaystones.values());
    }

    public static void setServerWaystones(WaystoneEntry[] entries) {
        serverWaystones.clear();
        for(WaystoneEntry entry : entries) {
            serverWaystones.put(entry.getName(), entry);
        }
    }

    public static void setKnownWaystones(WaystoneEntry[] entries) {
        knownWaystones.clear();
        for(WaystoneEntry entry : entries) {
            knownWaystones.put(entry.getName(), entry);
        }
    }

    public static WaystoneEntry getKnownWaystone(String name) {
        return knownWaystones.get(name);
    }

    public static Collection<WaystoneEntry> getServerWaystones() {
        return serverWaystones.values();
    }

    public static WaystoneEntry getServerWaystone(String name) {
        return serverWaystones.get(name);
    }
}