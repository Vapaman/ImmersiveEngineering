/*
 * BluSunrize
 * Copyright (c) 2020
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.items;

import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.api.client.TextUtils;
import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.render.IEOBJItemRenderer;
import blusunrize.immersiveengineering.common.gui.IESlot;
import blusunrize.immersiveengineering.common.items.IEItemInterfaces.IScrollwheel;
import blusunrize.immersiveengineering.common.items.IEItems.Misc;
import blusunrize.immersiveengineering.common.items.IEItems.Tools;
import blusunrize.immersiveengineering.common.items.ToolUpgradeItem.ToolUpgrade;
import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import blusunrize.immersiveengineering.common.util.Utils;
import blusunrize.immersiveengineering.common.util.fluids.IEItemFluidHandler;
import com.google.common.collect.ImmutableSet;
import com.mojang.math.Quaternion;
import com.mojang.math.Transformation;
import com.mojang.math.Vector3f;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;

public class BuzzsawItem extends DieselToolItem implements IScrollwheel
{
	public static final Collection<SawbladeItem> SAWBLADES = new ArrayList<>(2);

	public BuzzsawItem()
	{
		super("buzzsaw", withIEOBJRender().stacksTo(1).setISTER(() -> () -> IEOBJItemRenderer.INSTANCE), "BUZZSAW");
	}

	/* ------------- WORKBENCH & INVENTORY ------------- */
	@Override
	public int getSlotCount()
	{
		return 5;
	}

	@Override
	public Slot[] getWorkbenchSlots(AbstractContainerMenu container, ItemStack stack, Level level, Supplier<Player> getPlayer, IItemHandler toolInventory)
	{
		final boolean hasQuiver = hasQuiverUpgrade(stack);
		final int mainHeadX = hasQuiver?88: 98;
		List<Slot> slots = new ArrayList<>(5);
		slots.add(new IESlot.WithPredicate(
				toolInventory, 0, mainHeadX, 22, BuzzsawItem::isSawblade, newBlade -> setHead(stack, newBlade)
		));
		slots.add(new IESlot.Upgrades(container, toolInventory, 1, 88, 52, "BUZZSAW", stack, true, level, getPlayer));
		slots.add(new IESlot.Upgrades(container, toolInventory, 2, 108, 52, "BUZZSAW", stack, true, level, getPlayer));
		if(hasQuiverUpgrade(stack))
		{
			slots.add(new IESlot.WithPredicate(toolInventory, 3, 108, 12, BuzzsawItem::isSawblade));
			slots.add(new IESlot.WithPredicate(toolInventory, 4, 108, 32, BuzzsawItem::isSawblade));
		}
		return slots.toArray(new Slot[0]);
	}

	@Override
	public ItemStack removeUpgrade(ItemStack stack, Player player, ItemStack upgrade)
	{
		if(upgrade.getItem()==Misc.toolUpgrades.get(ToolUpgrade.BUZZSAW_SPAREBLADES))
			for(int i = 1; i <= 2; i++)
			{
				ItemStack sawblade = getSawblade(stack, i);
				if(!sawblade.isEmpty())
				{
					ItemNBTHelper.setItemStack(upgrade, "sawblade"+i, sawblade);
					setSawblade(stack, ItemStack.EMPTY, i);
				}
			}
		return upgrade;
	}

	@Override
	public void removeFromWorkbench(Player player, ItemStack stack)
	{
		LazyOptional<IItemHandler> invCap = stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
		invCap.ifPresent(inv -> {
			if(!inv.getStackInSlot(0).isEmpty()&&!inv.getStackInSlot(1).isEmpty()&&!inv.getStackInSlot(2).isEmpty())
				Utils.unlockIEAdvancement(player, "main/upgrade_buzzsaw");
		});
	}

	@Override
	public void recalculateUpgrades(ItemStack stack, Level w, Player player)
	{
		super.recalculateUpgrades(stack, w, player);
		LazyOptional<IItemHandler> invCap = stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
		invCap.ifPresent(inv -> {
			for(int iUpgrade = 1; iUpgrade <= 2; iUpgrade++)
			{
				ItemStack upgrade = inv.getStackInSlot(iUpgrade);
				if(upgrade.getItem()==Misc.toolUpgrades.get(ToolUpgrade.BUZZSAW_SPAREBLADES))
					for(int i = 1; i <= 2; i++)
						if(ItemNBTHelper.hasKey(upgrade, "sawblade"+i))
						{
							ItemStack sawblade = ItemNBTHelper.getItemStack(upgrade, "sawblade"+i);
							setSawblade(stack, sawblade, i);
							ItemNBTHelper.remove(upgrade, "sawblade"+i);
						}
			}
		});
	}

	@Override
	public ItemStack getHead(ItemStack itemStack)
	{
		return getSawblade(itemStack, 0);
	}

	public ItemStack getSawblade(ItemStack itemStack, int spare)
	{
		if(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY==null)
			return ItemStack.EMPTY;
		LazyOptional<IItemHandler> cap = itemStack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
		// handle spares
		int slot = spare==0?0: 2+spare;
		ItemStack sawblade = cap.orElseThrow(RuntimeException::new).getStackInSlot(slot);
		return !sawblade.isEmpty()&&isSawblade(sawblade)?sawblade: ItemStack.EMPTY;
	}

	@Override
	public void setHead(ItemStack buzzsaw, ItemStack sawblade)
	{
		setSawblade(buzzsaw, sawblade, 0);
	}

	public void setSawblade(ItemStack buzzsaw, ItemStack sawblade, int spare)
	{
		int slot = spare==0?0: 2+spare;
		IItemHandler inv = buzzsaw.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).orElseThrow(RuntimeException::new);
		((IItemHandlerModifiable)inv).setStackInSlot(slot, sawblade);

		if(spare==0)
		{
			ListTag enchants = null;
			if(sawblade.getItem() instanceof SawbladeItem)
				enchants = ((SawbladeItem)sawblade.getItem()).getSawbladeEnchants();
			if(enchants!=null)
				buzzsaw.getOrCreateTag().put("Enchantments", enchants);
			else
				buzzsaw.getOrCreateTag().remove("Enchantments");
		}
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> list, TooltipFlag flag)
	{
		list.add(IEItemFluidHandler.fluidItemInfoFlavor(getFluid(stack), getCapacity(stack, CAPACITY)));
		if(getHead(stack).isEmpty())
			list.add(TextUtils.applyFormat(
					new TranslatableComponent(Lib.DESC_FLAVOUR+"buzzsaw.noBlade"),
					ChatFormatting.GRAY
			));
		else
		{
			int maxDmg = getMaxHeadDamage(stack);
			int dmg = maxDmg-getHeadDamage(stack);
			float quote = dmg/(float)maxDmg;
			ChatFormatting status = (quote < .1?ChatFormatting.RED: quote < .3?ChatFormatting.GOLD: quote < .6?ChatFormatting.YELLOW: ChatFormatting.GREEN);
			list.add(TextUtils.applyFormat(new TranslatableComponent(Lib.DESC_FLAVOUR+"buzzsaw.bladeDamage"), ChatFormatting.GRAY)
					.append(" ")
					.append(TextUtils.applyFormat(
							new TranslatableComponent(Lib.DESC_INFO+"percent", (int)(quote*100)),
							status
					)));
		}
	}

	@Override
	public Rarity getRarity(ItemStack stack)
	{
		return Rarity.COMMON;
	}

	@Override
	protected double getAttackDamage(ItemStack stack, ItemStack sawblade)
	{
		return ((SawbladeItem)sawblade.getItem()).getSawbladeDamage();
	}

	@Override
	public void onScrollwheel(ItemStack stack, Player playerEntity, boolean forward)
	{
		if(hasQuiverUpgrade(stack))
		{
			ItemStack sawblade = getHead(stack);
			ItemStack spare1 = getSawblade(stack, 1);
			ItemStack spare2 = getSawblade(stack, 2);
			if(forward)
			{
				setHead(stack, spare2);
				setSawblade(stack, sawblade, 1);
				setSawblade(stack, spare1, 2);
			}
			else
			{
				setHead(stack, spare1);
				setSawblade(stack, spare2, 1);
				setSawblade(stack, sawblade, 2);
			}
		}
	}

	/* ------------- DIGGING ------------- */

	@Override
	public boolean canToolBeUsed(ItemStack stack, @Nullable LivingEntity player)
	{
		if(getHeadDamage(stack) >= getMaxHeadDamage(stack))
			return false;
		return !getFluid(stack).isEmpty();
	}

	@Override
	public int getMaxHeadDamage(ItemStack stack)
	{
		ItemStack sawblade = getHead(stack);
		return !sawblade.isEmpty()?sawblade.getMaxDamage(): 0;
	}

	@Override
	public int getHeadDamage(ItemStack stack)
	{
		ItemStack sawblade = getHead(stack);
		return !sawblade.isEmpty()?sawblade.getDamageValue(): 0;
	}

	@Override
	public boolean mineBlock(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity living)
	{
		consumeDurability(stack, world, state, pos, living);
		if(!world.isClientSide&&!living.isShiftKeyDown()&&living instanceof ServerPlayer)
			if(canFellTree(stack)&&canToolBeUsed(stack, living)&&isTree(world, pos))
				fellTree(world, pos, (ServerPlayer)living, stack);
		return true;
	}

	@Override
	protected void damageHead(ItemStack head, int amount, LivingEntity living)
	{
		head.hurtAndBreak(amount, living, entity -> entity.broadcastBreakEvent(InteractionHand.MAIN_HAND));
	}

	@Override
	protected void consumeDurability(ItemStack stack, Level world, @Nullable BlockState state, @Nullable BlockPos pos, LivingEntity living)
	{
		if(state==null||!state.is(BlockTags.LEAVES)||Utils.RAND.nextInt(10)==0)
			super.consumeDurability(stack, world, state, pos, living);
	}

	@Override
	public int getHarvestLevel(ItemStack stack, @Nonnull ToolType tool, @Nullable Player player, @Nullable BlockState blockState)
	{
		ItemStack sawblade = getHead(stack);
		if(!sawblade.isEmpty())
			return 3;
		return -1;
	}

	@Override
	public Set<ToolType> getToolTypes(ItemStack stack)
	{
		if(!getHead(stack).isEmpty()&&canToolBeUsed(stack, null))
			return ImmutableSet.of(ToolType.AXE);
		return super.getToolTypes(stack);
	}

	public boolean isEffective(ItemStack stack, Material mat)
	{
		Material[] validMaterials = null;
		ItemStack sawblade = getHead(stack);
		if(sawblade.getItem() instanceof SawbladeItem)
			validMaterials = ((SawbladeItem)sawblade.getItem()).getSawbladeMaterials();

		if(validMaterials!=null)
			for(Material m : validMaterials)
				if(m==mat)
					return true;
		return false;
	}

	@Override
	public float getDestroySpeed(ItemStack stack, BlockState state)
	{
		if(isEffective(stack, state.getMaterial()))
		{
			ItemStack sawblade = getHead(stack);
			if(!sawblade.isEmpty()&&canToolBeUsed(stack, null))
				return ((SawbladeItem)sawblade.getItem()).getSawbladeSpeed();
		}
		return super.getDestroySpeed(stack, state);
	}

	/**
	 * Check if there is a tree sprouting from the given position.
	 * We define a tree as a vertical stack of logs, up to 32 blocks tall
	 * which can go diagonal by one block per level (acacia)
	 * and with a leaf block at its top
	 *
	 * @param world
	 * @param initialPos
	 * @return
	 */
	private boolean isTree(Level world, BlockPos initialPos)
	{
		int logs = 0;
		boolean leafTop = false;
		BlockPos pos = initialPos;
		for(int y = 0; y < 32; y++)
		{
			pos = pos.above();
			BlockState state = world.getBlockState(pos);
			if(state.is(BlockTags.LOGS))
				logs++;
			else
			{
				if(state.is(BlockTags.LEAVES))
					leafTop = true;
				boolean foundLog = false;
				if(!leafTop)
				{
					// Yay, Acacia trees grow diagonally >_>
					boolean loop = true;
					for(int z = -1; z <= 1&&loop; z++)
						for(int x = -1; x <= 1&&loop; x++)
						{
							state = world.getBlockState(pos.offset(x, 0, z));
							if(state.is(BlockTags.LOGS))
							{
								pos = pos.offset(x, 0, z);
								foundLog = true;
								logs++;
								loop = false;
							}
						}
				}
				// If there is no diagonal growth, the tree ends
				if(!foundLog)
					break;
			}
		}
		return logs >= 3&&leafTop;
	}

	private boolean canFellTree(ItemStack stack)
	{
		ItemStack sawblade = getHead(stack);
		if(sawblade.getItem() instanceof SawbladeItem)
			return ((SawbladeItem)sawblade.getItem()).canSawbladeFellTree();
		return false;
	}

	/**
	 * The max distance a block can be from the initial hit
	 * to still be considered part of the tree
	 * This is based on the largest vanilla Jungle Trees
	 */
	private static final int MAX_HORIZONTAL_DISTANCE = 7;

	private boolean fellTree(Level world, BlockPos initialPos, ServerPlayer player, ItemStack stack)
	{
		int logs = 0;
		Deque<BlockPos> openList = new ArrayDeque<>();
		Deque<BlockPos> closedList = new ArrayDeque<>();
		openList.add(initialPos);
		while(!openList.isEmpty()&&closedList.size() < 512&&logs < 256)
		{
			BlockPos next = openList.pollFirst();

			// Ignore blocks too far away
			if(Math.abs(next.getX()-initialPos.getX()) > MAX_HORIZONTAL_DISTANCE
					||Math.abs(next.getZ()-initialPos.getZ()) > MAX_HORIZONTAL_DISTANCE)
				continue;

			if(!closedList.contains(next))
			{
				BlockState state = world.getBlockState(next);
				if(state.is(BlockTags.LOGS))
				{
					closedList.add(next);
					logs++;
					// Find all at same level or above, including diagonals
					for(int y = 0; y <= 1; y++)
						for(int z = -1; z <= 1; z++)
							for(int x = -1; x <= 1; x++)
								openList.add(next.offset(x, y, z));
				}
				else if(state.is(BlockTags.LEAVES))
				{
					closedList.add(next);
					int trunkDist = state.getBlock() instanceof LeavesBlock?state.getValue(LeavesBlock.DISTANCE): 0;
					// Leaves only propagate in cardinal directions, and only to other leaves
					for(Direction dir : new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST})
					{
						BlockPos adj = next.relative(dir);
						BlockState adjState = world.getBlockState(adj);
						if(adjState.is(BlockTags.LEAVES))
						{
							int adjDist = adjState.getBlock() instanceof LeavesBlock?adjState.getValue(LeavesBlock.DISTANCE): 0;
							if(adjDist < trunkDist) // We don't want to get closer
								continue;
						}
						openList.add(adj);
					}
				}
			}
		}

		if(closedList.size()==0)
			return false;
		// Register a Tick Handler to break the blocks, 5 at a time
		MinecraftForge.EVENT_BUS.register(new Object()
		{
			@SubscribeEvent
			public void onTick(TickEvent.WorldTickEvent event)
			{
				if(event.phase==Phase.START&&event.world==world)
				{
					breakFromList(closedList, 5, world, player, stack);
					if(closedList.isEmpty())
						MinecraftForge.EVENT_BUS.unregister(this);
				}
			}
		});
		return true;
	}

	private void breakFromList(Deque<BlockPos> closedList, int maxAmount, Level world, ServerPlayer player, ItemStack stack)
	{
		int count = 0;
		while(count++ < maxAmount&&!closedList.isEmpty())
		{
			BlockPos pos = closedList.pollFirst();

			int xpDropEvent = ForgeHooks.onBlockBreakEvent(world, player.gameMode.getGameModeForPlayer(), player, pos);
			if(xpDropEvent < 0)
				continue;

			BlockState state = world.getBlockState(pos);
			Block block = state.getBlock();

			if(!block.isAir(state, world, pos)&&state.getDestroyProgress(player, world, pos)!=0)
			{
				if(player.abilities.instabuild)
				{
					block.playerWillDestroy(world, pos, state, player);
					if(block.removedByPlayer(state, world, pos, player, false, state.getFluidState()))
						block.destroy(world, pos, state);
				}
				else
				{
					block.playerWillDestroy(world, pos, state, player);
					BlockEntity te = world.getBlockEntity(pos);
					consumeDurability(stack, world, state, pos, player);
					if(block.removedByPlayer(state, world, pos, player, true, state.getFluidState()))
					{
						block.destroy(world, pos, state);
						block.playerDestroy(world, player, pos, state, te, stack);
						if (world instanceof ServerLevel)
							block.popExperience((ServerLevel)world, pos, xpDropEvent);
					}
				}
				world.levelEvent(2001, pos, Block.getId(state));
				player.connection.send(new ClientboundBlockUpdatePacket(world, pos));
			}
		}
	}
	/* ------------- RENDERING ------------- */

	@OnlyIn(Dist.CLIENT)
	@Override
	public TextureAtlasSprite getTextureReplacement(ItemStack stack, String group, String material)
	{
		if("blade".equals(material))
		{
			int spare = "upgrade_blades1".equals(group)?1: "upgrade_blades2".equals(group)?2: 0;
			ItemStack sawblade = getSawblade(stack, spare);
			if(sawblade.getItem() instanceof SawbladeItem)
			{
				ResourceLocation rl = ((SawbladeItem)sawblade.getItem()).getSawbladeTexture();
				if(rl!=null)
					return ClientUtils.getSprite(rl);
			}
		}
		return null;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public boolean shouldRenderGroup(ItemStack stack, String group)
	{
		if("body".equals(group))
			return true;
		if("blade".equals(group))
			return !this.getHead(stack).isEmpty();

		CompoundTag upgrades = this.getUpgrades(stack);
		if("upgrade_lube".equals(group))
			return upgrades.getBoolean("oiled");
		if("upgrade_launcher".equals(group))
			return upgrades.getBoolean("launcher");
		if("upgrade_blades0".equals(group))
			return hasQuiverUpgrade(stack);
		if("upgrade_blades1".equals(group))
			return hasQuiverUpgrade(stack)&&!this.getSawblade(stack, 1).isEmpty();
		if("upgrade_blades2".equals(group))
			return hasQuiverUpgrade(stack)&&!this.getSawblade(stack, 2).isEmpty();
		return true;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public Transformation applyTransformations(ItemStack stack, String group, Transformation transform)
	{
//		CompoundNBT upgrades = this.getUpgrades(stack);
//		if(group.equals("drill_sawblade")&&upgrades.getInt("damage") <= 0)
//		{
//			Matrix4 mat = new Matrix4(transform.getMatrixVec());
//			mat.translate(-.25f, 0, 0);
//			return new TransformationMatrix(mat.toMatrix4f());
//		}
//		Matrix4 mat = new Matrix4(transform.getMatrixVec());
//		mat.translate(.25f, -0.25f, 0);
//		return new TransformationMatrix(mat.toMatrix4f());
		return transform;
	}


	private static final String[][] GROUP_BLADE = {{"blade"}};

	@Override
	@OnlyIn(Dist.CLIENT)
	public String[][] getSpecialGroups(ItemStack stack, TransformType transform, LivingEntity entity)
	{
		return GROUP_BLADE;
	}

	@OnlyIn(Dist.CLIENT)
	private static Transformation MAT_FIXED;

	@Nonnull
	@Override
	@OnlyIn(Dist.CLIENT)
	public Transformation getTransformForGroups(ItemStack stack, String[] groups, TransformType transform, LivingEntity entity, float partialTicks)
	{
		if(MAT_FIXED==null)
			MAT_FIXED = new Transformation(new Vector3f(0.60945f, 0, 0), null, null, null);
		if(!shouldRotate(entity, stack, transform))
			return MAT_FIXED;
		float ticksPerRotation = 10f;
		float angle = (entity.tickCount%ticksPerRotation+partialTicks)/ticksPerRotation*(float)(2*Math.PI);
		return new Transformation(
				new Vector3f(0.60945f, 0, 0),
				new Quaternion(0, angle, 0, false),
				null, null);
	}

	public static boolean hasQuiverUpgrade(ItemStack stack)
	{
		return ((BuzzsawItem)Tools.buzzsaw).getUpgrades(stack).getBoolean("spareblades");
	}

	public static boolean isSawblade(ItemStack stack)
	{
		Item item = stack.getItem();
		return item instanceof SawbladeItem&&SAWBLADES.contains(item);
	}
}
