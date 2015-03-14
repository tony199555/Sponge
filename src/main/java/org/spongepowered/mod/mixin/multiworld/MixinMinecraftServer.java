/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered.org <http://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.mod.mixin.multiworld;

import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldManager;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerMulti;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.storage.AnvilSaveHandler;
import net.minecraft.world.demo.DemoWorldServer;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.DimensionManager;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.mod.interfaces.IMixinWorld;

import java.io.File;

@NonnullByDefault
@Mixin(net.minecraft.server.MinecraftServer.class)
public abstract class MixinMinecraftServer {

    @Shadow
    public WorldServer[] worldServers;

    @Shadow
    private ServerConfigurationManager serverConfigManager;

    @Shadow
    public Profiler theProfiler;

    @Shadow
    private boolean enableBonusChest;

    @Shadow
    private boolean worldIsBeingDeleted;

    @Shadow
    protected abstract void convertMapIfNeeded(String worldNameIn);

    @Shadow
    protected abstract void setUserMessage(String message);

    @Shadow
    protected abstract void initialWorldChunkLoad();

    @Shadow
    protected abstract void setResourcePackFromWorld(String worldNameIn, ISaveHandler saveHandlerIn);

    @Shadow
    public abstract boolean canStructuresSpawn();

    @Shadow
    public abstract WorldSettings.GameType getGameType();

    @Shadow
    public abstract EnumDifficulty getDifficulty();

    @Shadow
    public abstract boolean isHardcore();

    @Shadow
    public abstract boolean isSinglePlayer();

    @Shadow
    public abstract boolean isDemo();

    @Shadow
    public abstract String getFolderName();

    @Shadow
    public abstract void setDifficultyForAllWorlds(EnumDifficulty difficulty);

    @Overwrite
    protected void loadAllWorlds(String overworldFolder, String unused, long seed, WorldType type, String generator) {
        this.convertMapIfNeeded(overworldFolder);
        this.setUserMessage("menu.loadingLevel");

        AnvilSaveHandler savehandler = new AnvilSaveHandler(getWorldContainer(), overworldFolder, true);
        this.setResourcePackFromWorld(this.getFolderName(), savehandler);
        WorldInfo worldinfo = savehandler.loadWorldInfo();
        WorldSettings worldsettings = null;
        if (worldinfo == null) {
            if (this.isDemo()) {
                worldsettings = DemoWorldServer.demoWorldSettings;
            } else {
                worldsettings = new WorldSettings(seed, this.getGameType(), this.canStructuresSpawn(), this.isHardcore(), type);
                worldsettings.setWorldName(generator);

                if (this.enableBonusChest) {
                    worldsettings.enableBonusChest();
                }
            }

            worldinfo = new WorldInfo(worldsettings, overworldFolder);
        } else {
            worldinfo.setWorldName(overworldFolder);
            worldsettings = new WorldSettings(worldinfo);
        }

        WorldServer overWorld =
                (WorldServer) (isDemo() ? new DemoWorldServer((MinecraftServer) (Object) this, savehandler, worldinfo, 0,
                        this.theProfiler).init() : new WorldServer((MinecraftServer) (Object) this, savehandler, worldinfo, 0,
                        this.theProfiler).init());
        overWorld.initialize(worldsettings);
        for (int dim : net.minecraftforge.common.DimensionManager.getStaticDimensionIDs()) {
            WorldProvider provider = WorldProvider.getProviderForDimension(dim);
            String worldFolder = provider.getSaveFolder();

            WorldInfo newWorldInfo = null;
            WorldSettings newWorldSettings = null;
            AnvilSaveHandler worldsavehandler = null;

            if (dim != 0) {
                worldsavehandler = new AnvilSaveHandler(getWorldContainer(), worldFolder, true);
                newWorldInfo = worldsavehandler.loadWorldInfo();
                if (newWorldInfo == null) {
                    newWorldSettings = new WorldSettings(seed, this.getGameType(), this.canStructuresSpawn(), this.isHardcore(), type);
                    newWorldSettings.setWorldName(generator);

                    if (this.enableBonusChest) {
                        newWorldSettings.enableBonusChest();
                    }

                    newWorldInfo = new WorldInfo(newWorldSettings, worldFolder);
                } else {
                    newWorldInfo.setWorldName(worldFolder);
                    newWorldSettings = new WorldSettings(newWorldInfo);
                }

                ((IMixinWorld) overWorld).setWorldInfo(newWorldInfo); // Pass new WorldInfo to delegate
            }

            WorldServer world =
                    (dim == 0 ? overWorld : (WorldServer) new WorldServerMulti((MinecraftServer) (Object) this, worldsavehandler, dim, overWorld,
                            this.theProfiler).init());

            if (newWorldInfo != null) {
                ((IMixinWorld) overWorld).setWorldInfo(worldinfo);
                ((IMixinWorld) world).setWorldInfo(newWorldInfo);
                world.initialize(newWorldSettings);
            }
            world.addWorldAccess(new WorldManager((MinecraftServer) (Object) this, world));

            if (!this.isSinglePlayer()) {
                world.getWorldInfo().setGameType(this.getGameType());
            }
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.WorldEvent.Load(world));
        }

        this.serverConfigManager.setPlayerManager(DimensionManager.getWorlds());
        this.setDifficultyForAllWorlds(this.getDifficulty());
        this.initialWorldChunkLoad();
    }

    public File getWorldContainer() {
        if (DimensionManager.getWorld(0) != null) {
            return ((SaveHandler) DimensionManager.getWorld(0).getSaveHandler()).getWorldDirectory();
        }
        return null;
    }
}
