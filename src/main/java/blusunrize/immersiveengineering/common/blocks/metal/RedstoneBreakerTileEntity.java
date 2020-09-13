/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.blocks.metal;

import blusunrize.immersiveengineering.api.wires.Connection;
import blusunrize.immersiveengineering.api.wires.ConnectionPoint;
import blusunrize.immersiveengineering.common.IETileTypes;
import blusunrize.immersiveengineering.common.util.chickenbones.Matrix4;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RedstoneBreakerTileEntity extends BreakerSwitchTileEntity implements ITickableTileEntity
{
	public RedstoneBreakerTileEntity()
	{
		super(IETileTypes.REDSTONE_BREAKER.get());
	}
	@Override
	public void tick()
	{
		final boolean activeOld = getIsActive();
		if(!world.isRemote&&(isRSPowered())==activeOld)
		{
			setActive(!activeOld);
			updateConductivity();
		}
	}

	@Override
	protected boolean canTakeHV()
	{
		return true;
	}

	protected boolean allowEnergyToPass()
	{
		return getIsActive()^inverted;
	}

	@Override
	public VoxelShape getBlockBounds(@Nullable ISelectionContext ctx)
	{
		Vec3d start = new Vec3d(0, .125f, 0);
		Vec3d end = new Vec3d(1, .875f, 1);
		Matrix4 mat = new Matrix4(getFacing());
		mat.translate(.5, .5, 0).rotate(Math.PI/2*rotation, 0, 0, 1).translate(-.5, -.5, 0);
		start = mat.apply(start);
		end = mat.apply(end);
		return VoxelShapes.create(new AxisAlignedBB(start, end));
	}

	@Override
	public Vec3d getConnectionOffset(@Nonnull Connection con, ConnectionPoint here)
	{
		Matrix4 mat = new Matrix4(getFacing());
		mat.translate(.5, .5, 0).rotate(Math.PI/2*rotation, 0, 0, 1).translate(-.5, -.5, 0);
		boolean isLeft = here.getIndex()==LEFT_INDEX;
		Vec3d ret = mat.apply(isLeft?new Vec3d(.125, .5, 1): new Vec3d(.875, .5, 1));
		return ret;
	}

	@Override
	public int getWeakRSOutput(@Nonnull Direction side)
	{
		return 0;
	}

	@Override
	public int getStrongRSOutput(@Nonnull Direction side)
	{
		return 0;
	}

	@Override
	public boolean interact(Direction side, PlayerEntity player, Hand hand, ItemStack heldItem, float hitX, float hitY, float hitZ)
	{
		return false;
	}

	@Override
	public boolean canConnectRedstone(@Nonnull Direction side)
	{
		return false;
	}
}