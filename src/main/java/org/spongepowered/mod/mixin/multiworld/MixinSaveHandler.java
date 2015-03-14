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

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.mod.SpongeMod;
import org.spongepowered.mod.interfaces.IMixinSaveHandler;
import org.spongepowered.mod.interfaces.IMixinWorldInfo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

@NonnullByDefault
@Mixin(net.minecraft.world.storage.SaveHandler.class)
public class MixinSaveHandler implements IMixinSaveHandler {

    private UUID uuid = null;

    @Shadow
    private File worldDirectory;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void onSaveHandlerConstruction(File savesDirectory, String directoryName, boolean playersDirectoryIn, CallbackInfo ci) {
        this.uuid = getUniqueId();
    }

    @Overwrite
    public WorldInfo loadWorldInfo() {
        File file1 = new File(this.worldDirectory, "level.dat");
        NBTTagCompound nbttagcompound;
        NBTTagCompound nbttagcompound1;

        WorldInfo worldInfo = null;

        if (file1.exists()) {
            try {
                nbttagcompound = CompressedStreamTools.readCompressed(new FileInputStream(file1));
                nbttagcompound1 = nbttagcompound.getCompoundTag("Data");
                worldInfo = new WorldInfo(nbttagcompound1);
                if (((IMixinWorldInfo) worldInfo).getDimension() == 0) {
                    net.minecraftforge.fml.common.FMLCommonHandler.instance().handleWorldDataLoad((SaveHandler) (Object) this, worldInfo,
                            nbttagcompound);
                }
                return new WorldInfo(nbttagcompound1);
            } catch (net.minecraftforge.fml.common.StartupQuery.AbortedException e) {
                throw e;
            } catch (Exception exception1) {
                exception1.printStackTrace();
            }
        }

        net.minecraftforge.fml.common.FMLCommonHandler.instance().confirmBackupLevelDatUse((SaveHandler) (Object) this);
        file1 = new File(this.worldDirectory, "level.dat_old");

        if (file1.exists()) {
            try {
                nbttagcompound = CompressedStreamTools.readCompressed(new FileInputStream(file1));
                nbttagcompound1 = nbttagcompound.getCompoundTag("Data");
                worldInfo = new WorldInfo(nbttagcompound1);
                if (((IMixinWorldInfo) worldInfo).getDimension() == 0) {
                    net.minecraftforge.fml.common.FMLCommonHandler.instance().handleWorldDataLoad((SaveHandler) (Object) this, worldInfo,
                            nbttagcompound);
                }
                return worldInfo;
            } catch (net.minecraftforge.fml.common.StartupQuery.AbortedException e) {
                throw e;
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public UUID getUniqueId() {
        if (this.uuid != null) {
            return this.uuid;
        }

        File datFile = new File(this.worldDirectory, "uid.dat");

        if (datFile.exists()) {
            DataInputStream dis = null;

            try {
                dis = new DataInputStream(new FileInputStream(datFile));
                return this.uuid = new UUID(dis.readLong(), dis.readLong());
            } catch (IOException ex) {
                SpongeMod.instance.getLogger().warn("Failed to read " + datFile + ", generating new random UUID", ex);
            } finally {
                if (dis != null)
                {
                    try {
                        dis.close();
                    } catch (IOException ex) {
                    }
                }
            }
        }

        this.uuid = UUID.randomUUID();
        DataOutputStream dos = null;

        try {
            dos = new DataOutputStream(new FileOutputStream(datFile));
            dos.writeLong(this.uuid.getMostSignificantBits());
            dos.writeLong(this.uuid.getLeastSignificantBits());
        } catch (IOException ex) {
            SpongeMod.instance.getLogger().warn("Failed to write " + datFile, ex);
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException ex) {
                }
            }
        }

        return this.uuid;
    }
}
