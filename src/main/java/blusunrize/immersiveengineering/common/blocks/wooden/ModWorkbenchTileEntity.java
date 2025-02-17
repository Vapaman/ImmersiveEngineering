/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.blocks.wooden;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.api.IEProperties.VisibilityList;
import blusunrize.immersiveengineering.api.client.IModelOffsetProvider;
import blusunrize.immersiveengineering.api.tool.IConfigurableTool;
import blusunrize.immersiveengineering.common.IETileTypes;
import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IHasDummyBlocks;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IHasObjProperty;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IInteractionObjectIE;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IStateBasedDirectional;
import blusunrize.immersiveengineering.common.items.EngineersBlueprintItem;
import blusunrize.immersiveengineering.common.util.Utils;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

public class ModWorkbenchTileEntity extends IEBaseTileEntity implements IIEInventory, IStateBasedDirectional,
		IHasDummyBlocks, IInteractionObjectIE, IHasObjProperty, IModelOffsetProvider
{
	public static final BlockPos MASTER_POS = BlockPos.ZERO;
	public static final BlockPos DUMMY_POS = new BlockPos(1, 0, 0);
	NonNullList<ItemStack> inventory = NonNullList.withSize(7, ItemStack.EMPTY);

	public ModWorkbenchTileEntity()
	{
		super(IETileTypes.MOD_WORKBENCH.get());
	}

	@Override
	public void readCustomNBT(CompoundTag nbt, boolean descPacket)
	{
		inventory = Utils.readInventory(nbt.getList("inventory", 10), 7);
	}

	@Override
	public void writeCustomNBT(CompoundTag nbt, boolean descPacket)
	{
		nbt.put("inventory", Utils.writeInventory(inventory));
	}

	@OnlyIn(Dist.CLIENT)
	private AABB renderAABB;

	@OnlyIn(Dist.CLIENT)
	@Override
	public AABB getRenderBoundingBox()
	{
		if(renderAABB==null)
			renderAABB = new AABB(getBlockPos().getX()-1, getBlockPos().getY(), getBlockPos().getZ()-1, getBlockPos().getX()+2, getBlockPos().getY()+2, getBlockPos().getZ()+2);
		return renderAABB;
	}

	@Override
	public NonNullList<ItemStack> getInventory()
	{
		return this.inventory;
	}

	@Override
	public boolean isStackValid(int slot, ItemStack stack)
	{
		return true;
	}

	@Override
	public int getSlotLimit(int slot)
	{
		return slot==0?1: 64;
	}

	@Override
	public void doGraphicalUpdates()
	{
	}

	@Override
	public PlacementLimitation getFacingLimitation()
	{
		return PlacementLimitation.HORIZONTAL;
	}

	@Override
	public boolean mirrorFacingOnPlacement(LivingEntity placer)
	{
		return false;
	}

	@Override
	public boolean canHammerRotate(Direction side, Vec3 hit, LivingEntity entity)
	{
		return false;
	}

	@Override
	public boolean canRotate(Direction axis)
	{
		return false;
	}

	@Override
	public void receiveMessageFromClient(CompoundTag message)
	{
		applyConfigTo(inventory.get(0), message);
	}

	public static void applyConfigTo(ItemStack stack, CompoundTag message)
	{
		if(!(stack.getItem() instanceof IConfigurableTool))
			return;
		for(String key : message.getAllKeys())
		{
			Tag tag = message.get(key);
			if(tag instanceof ByteTag)
				((IConfigurableTool)stack.getItem()).applyConfigOption(stack, key, ((ByteTag)tag).getAsByte()!=0);
			else if(tag instanceof FloatTag)
				((IConfigurableTool)stack.getItem()).applyConfigOption(stack, key, ((FloatTag)tag).getAsFloat());
		}
	}

	@Override
	public boolean isDummy()
	{
		return getState().getValue(IEProperties.MULTIBLOCKSLAVE);
	}

	@Nullable
	@Override
	public ModWorkbenchTileEntity master()
	{
		if(!isDummy())
			return this;
		// Used to provide tile-dependant drops after breaking
		if(tempMasterTE!=null)
			return (ModWorkbenchTileEntity)tempMasterTE;
		Direction dummyDir = isDummy()?getFacing().getCounterClockWise(): getFacing().getClockWise();
		BlockPos masterPos = getBlockPos().relative(dummyDir);
		BlockEntity te = Utils.getExistingTileEntity(level, masterPos);
		return (te instanceof ModWorkbenchTileEntity)?(ModWorkbenchTileEntity)te: null;
	}

	@Override
	public void placeDummies(BlockPlaceContext ctx, BlockState state)
	{
		DeskBlock.placeDummies(getBlockState(), level, worldPosition, ctx);
	}

	@Override
	public void breakDummies(BlockPos pos, BlockState state)
	{
		tempMasterTE = master();
		Direction dummyDir = isDummy()?getFacing().getCounterClockWise(): getFacing().getClockWise();
		level.removeBlock(pos.relative(dummyDir), false);
	}

	@Override
	public boolean canUseGui(Player player)
	{
		return true;
	}

	@Override
	public IInteractionObjectIE getGuiMaster()
	{
		if(!isDummy())
			return this;
		Direction dummyDir = getFacing().getCounterClockWise();
		BlockEntity tileEntityModWorkbench = level.getBlockEntity(worldPosition.relative(dummyDir));
		if(tileEntityModWorkbench instanceof ModWorkbenchTileEntity)
			return (ModWorkbenchTileEntity)tileEntityModWorkbench;
		return null;
	}

	private static final VisibilityList normalDisplayList = VisibilityList.show("cube0");
	private static final VisibilityList blueprintDisplayList = VisibilityList.show("cube0", "blueprint");

	@Override
	public VisibilityList compileDisplayList(BlockState state)
	{
		ModWorkbenchTileEntity master = master();
		if(master!=null&&master.inventory.get(0).getItem() instanceof EngineersBlueprintItem)
			return blueprintDisplayList;
		return normalDisplayList;
	}

	@Override
	public Property<Direction> getFacingProperty()
	{
		return IEProperties.FACING_HORIZONTAL;
	}

	@Override
	public BlockPos getModelOffset(BlockState state, @Nullable Vec3i size)
	{
		if(isDummy())
			return DUMMY_POS;
		else
			return MASTER_POS;
	}
}