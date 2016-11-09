/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
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
package org.spongepowered.mod.mixin.core.event.inventory;

import net.minecraft.entity.item.EntityItem;
import net.minecraftforge.event.entity.item.ItemEvent;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.event.entity.AffectEntityEvent;
import org.spongepowered.api.event.entity.ExpireEntityEvent;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.mod.mixin.core.event.entity.MixinEventEntity;

import java.util.ArrayList;
import java.util.List;

@NonnullByDefault
@Mixin(value = ItemEvent.class, remap = false)
public abstract class MixinEventItem extends MixinEventEntity implements AffectEntityEvent {

    protected List<Entity> entities;

    @Shadow @Final private EntityItem entityItem;
    @Shadow public abstract EntityItem getEntityItem();

    @Inject(method = "<init>", at = @At("RETURN"))
    public void onConstructed(EntityItem itemEntity, CallbackInfo ci) {
        this.entities = new ArrayList<>();
        this.entities.add((Entity) itemEntity);
    }

    @Override
    public List<Entity> getEntities() {
        return this.entities;
    }

//    @Override
//    public List<EntitySnapshot> getEntitySnapshots() {
//        return this.entitySnapshots;
//    }

    @Mixin(value = ItemExpireEvent.class, remap = false)
    static abstract class Expire extends MixinEventItem implements ExpireEntityEvent.TargetItem {

        @Override
        public Item getTargetEntity() {
            return (Item) this.getEntityItem();
        }

        @Override
        public World getTargetWorld() {
            return (World) this.getEntityItem().worldObj;
        }

    }

}
