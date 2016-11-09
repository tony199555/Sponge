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
package org.spongepowered.mod.mixin.core.server.management;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.ImmutableList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockCommandBlock;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemDoor;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.network.play.server.SPacketCloseWindow;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraft.world.ILockableContainer;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Location;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.interfaces.server.management.IMixinPlayerInteractionManager;
import org.spongepowered.common.registry.provider.DirectionFacingProvider;
import org.spongepowered.common.util.TristateUtil;

import java.util.Optional;

@Mixin(value = PlayerInteractionManager.class, priority = 1001)
public abstract class MixinPlayerInteractionManager implements IMixinPlayerInteractionManager {

    @Shadow public EntityPlayerMP thisPlayerMP;
    @Shadow public World theWorld;
    @Shadow private GameType gameType;

    @Shadow public abstract boolean isCreative();
    @Shadow public abstract EnumActionResult processRightClick(EntityPlayer player, net.minecraft.world.World worldIn, ItemStack stack, EnumHand hand);
    @Shadow(remap = false) public abstract double getBlockReachDistance();
    @Shadow(remap = false) public abstract void setBlockReachDistance(double distance);

    /**
     * @author gabizou - May 5th, 2016
     * @reason Rewrite the firing of interact block events with forge hooks
     * Note: This is a dirty merge of Aaron's SpongeCommon writeup of the interaction events and
     * Forge's additions. There's some overlay between the two events, specifically that there
     * is a SpongeEvent thrown before the ForgeEvent, and yet both are checked in various
     * if statements.
     */
    @Overwrite
    public EnumActionResult processRightClickBlock(EntityPlayer player, World worldIn, ItemStack stack, EnumHand hand, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (this.gameType == GameType.SPECTATOR) {
            TileEntity tileentity = worldIn.getTileEntity(pos);

            if (tileentity instanceof ILockableContainer) {
                Block block = worldIn.getBlockState(pos).getBlock();
                ILockableContainer ilockablecontainer = (ILockableContainer) tileentity;

                if (ilockablecontainer instanceof TileEntityChest && block instanceof BlockChest) {
                    ilockablecontainer = ((BlockChest) block).getLockableContainer(worldIn, pos);
                }

                if (ilockablecontainer != null) {
                    player.displayGUIChest(ilockablecontainer);
                    return EnumActionResult.SUCCESS;
                }
            } else if (tileentity instanceof IInventory) {
                player.displayGUIChest((IInventory) tileentity);
                return EnumActionResult.SUCCESS;
            }

            return EnumActionResult.PASS;
        } else {
            // Sponge start - refactor rest of method

            // Store reference of current player's itemstack in case it changes
            ItemStack oldStack = ItemStack.copyItemStack(stack);


            BlockSnapshot currentSnapshot = ((org.spongepowered.api.world.World) worldIn).createSnapshot(pos.getX(), pos.getY(), pos.getZ());
            InteractBlockEvent.Secondary event = SpongeCommonEventFactory.callInteractBlockEventSecondary(Cause.of(NamedCause.source(player)),
                    Optional.of(new Vector3d(hitX, hitY, hitZ)), currentSnapshot,
                    DirectionFacingProvider.getInstance().getKey(facing).get(), hand);
            if (!ItemStack.areItemStacksEqual(oldStack, this.thisPlayerMP.getHeldItem(hand))) {
                SpongeCommonEventFactory.playerInteractItemChanged = true;
            }

            TileEntity tileEntity = worldIn.getTileEntity(pos);
            if (event.isCancelled()) {
                final IBlockState state = worldIn.getBlockState(pos);

                if (state.getBlock() == Blocks.COMMAND_BLOCK) {
                    // CommandBlock GUI opens solely on the client, we need to force it close on cancellation
                    this.thisPlayerMP.connection.sendPacket(new SPacketCloseWindow(0));

                } else if (state.getProperties().containsKey(BlockDoor.HALF)) {
                    // Stopping a door from opening while interacting the top part will allow the door to open, we need to update the
                    // client to resolve this
                    if (state.getValue(BlockDoor.HALF) == BlockDoor.EnumDoorHalf.LOWER) {
                        this.thisPlayerMP.connection.sendPacket(new SPacketBlockChange(worldIn, pos.up()));
                    } else {
                        this.thisPlayerMP.connection.sendPacket(new SPacketBlockChange(worldIn, pos.down()));
                    }

                } else if (stack != null) {
                    // Stopping the placement of a door or double plant causes artifacts (ghosts) on the top-side of the block. We need to remove it
                    if (stack.getItem() instanceof ItemDoor || (stack.getItem() instanceof ItemBlock && ((ItemBlock) stack.getItem()).getBlock()
                            .equals(Blocks.DOUBLE_PLANT))) {
                        this.thisPlayerMP.connection.sendPacket(new SPacketBlockChange(worldIn, pos.up(2)));
                    }
                }

                // Some mods such as OpenComputers open a GUI on client-side
                // To workaround this, we will always send a SPacketCloseWindow to client if interacting with a TE
                if (tileEntity != null) {
                    this.thisPlayerMP.closeScreen();
                }
                SpongeCommonEventFactory.interactBlockEventCancelled = true;
                return EnumActionResult.FAIL;
            }

            net.minecraft.item.Item item = stack == null ? null : stack.getItem();
            EnumActionResult ret = item == null
                    ? EnumActionResult.PASS
                    : item.onItemUseFirst(stack, player, worldIn, pos, facing, hitX, hitY, hitZ, hand);
            if (ret != EnumActionResult.PASS) {
                return ret;
            }

            boolean bypass = true;
            final ItemStack[] itemStacks = {player.getHeldItemMainhand(), player.getHeldItemOffhand()};
            for (ItemStack s : itemStacks) {
                bypass = bypass && (s == null || s.getItem().doesSneakBypassUse(s, worldIn, pos, player));
            }

            EnumActionResult result = EnumActionResult.PASS;

            if (!player.isSneaking() || bypass || event.getUseBlockResult() == Tristate.TRUE) {
                // Check event useBlockResult, and revert the client if it's FALSE.
                // also, store the result instead of returning immediately
                if (event.getUseBlockResult() != Tristate.FALSE) {
                    IBlockState iblockstate = worldIn.getBlockState(pos);
                    Container lastOpenContainer = player.openContainer;

                    result = iblockstate.getBlock().onBlockActivated(worldIn, pos, iblockstate, player, hand, stack, facing, hitX, hitY, hitZ)
                            ? EnumActionResult.SUCCESS
                            : EnumActionResult.FAIL;
                    // Mods such as StorageDrawers alter the stack on block activation
                    // if itemstack changed, avoid restore
                    if (!ItemStack.areItemStacksEqual(oldStack, this.thisPlayerMP.getHeldItem(hand))) {
                        SpongeCommonEventFactory.playerInteractItemChanged = true;
                    }

                    result = this.handleOpenEvent(lastOpenContainer, this.thisPlayerMP, result);
                } else {
                    this.thisPlayerMP.connection.sendPacket(new SPacketBlockChange(this.theWorld, pos));
                    result = TristateUtil.toActionResult(event.getUseItemResult());
                }
            }

            // Same issue as above with OpenComputers
            // This handles the event not cancelled and block not activated
            if (result != EnumActionResult.SUCCESS && tileEntity != null && hand == EnumHand.MAIN_HAND) {
                this.thisPlayerMP.closeScreen();
            }

            // store result instead of returning
            if (stack == null) {
                result = EnumActionResult.PASS;
            } else if (player.getCooldownTracker().hasCooldown(stack.getItem())) {
                result = EnumActionResult.PASS;
            } else if (stack.getItem() instanceof ItemBlock && ((ItemBlock) stack.getItem()).getBlock() instanceof BlockCommandBlock && !player.canCommandSenderUseCommand(2, "")) {
                result = EnumActionResult.FAIL;
            } else {
                if ((result != EnumActionResult.SUCCESS && event.getUseItemResult() != Tristate.FALSE
                      || result == EnumActionResult.SUCCESS && event.getUseItemResult() == Tristate.TRUE)) {
                    int meta = stack.getMetadata();
                    int size = stack.stackSize;
                    result = stack.onItemUse(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
                    // nest isCreative check instead of calling the method twice.
                    if (this.isCreative()) {
                        stack.setItemDamage(meta);
                        stack.stackSize = size;
                    }
                }
            }

            if (!ItemStack.areItemStacksEqual(player.getHeldItem(hand), oldStack) || result != EnumActionResult.SUCCESS) {
                player.openContainer.detectAndSendChanges();
            }
            return result;
            // Sponge end
        }
    }

    @Redirect(method = "onBlockClicked", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/common/ForgeHooks;onLeftClickBlock(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;Lnet/minecraft/util/math/Vec3d;)Lnet/minecraftforge/event/entity/player/PlayerInteractEvent$LeftClickBlock;", remap = false))
    public PlayerInteractEvent.LeftClickBlock onForgeCallLeftClickBlock(EntityPlayer player, BlockPos pos, EnumFacing side, Vec3d hitVec) {
        // We fire Forge's LeftClickBlock event when InteractBlockEvent.Primary is invoked which occurs before this method.
        // Due to this, we will simply return a dummy event
        return new PlayerInteractEvent.LeftClickBlock(player, pos, side, hitVec);
    }

    // Forge treats their BreakEvent as a Pre event BEFORE the break actually occurs while Sponge 
    // fires a break event AFTER it occurs in order to capture.
    // This causes mods to receive the BreakEvent AFTER HarvestDropsEvent which breaks mods such as
    // EnderIO's BlockPoweredSpawner.
    // To workaround this issue, the BreakEvent will only be fired here along with a ChangeBlockEvent.Pre
    // so plugins have a chance to alter the final result.
    @Redirect(method = "tryHarvestBlock", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/common/ForgeHooks;onBlockBreakEvent(Lnet/minecraft/world/World;Lnet/minecraft/world/GameType;Lnet/minecraft/entity/player/EntityPlayerMP;Lnet/minecraft/util/math/BlockPos;)I", remap = false))
    public int onTryHarvestBlockBreakEvent(World worldIn, GameType gameType, EntityPlayerMP entityPlayer, BlockPos pos) {
        int exp = net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(theWorld, gameType, thisPlayerMP, pos);
        Location<org.spongepowered.api.world.World> location = new Location<>((org.spongepowered.api.world.World) worldIn, pos.getX(), pos.getY(), pos.getZ());
        ChangeBlockEvent.Pre event = SpongeEventFactory.createChangeBlockEventPre(Cause.of(NamedCause.source(entityPlayer)), ImmutableList.of(location),
                (org.spongepowered.api.world.World) worldIn);
        if (exp == -1) {
            event.setCancelled(true);
        }
        if (SpongeImpl.postEvent(event)) {
            return -1;
        }

        return exp;
    }
}
