/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.blocks.metal;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.common.IEContent;
import blusunrize.immersiveengineering.common.blocks.BlockItemIE;
import blusunrize.immersiveengineering.common.items.HammerItem;
import blusunrize.immersiveengineering.common.util.DirectionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public class MetalLadderBlock extends LadderBlock
{
	private static final Map<Direction, VoxelShape> FRAMES = new EnumMap<>(Direction.class);

	static
	{
		for(Direction dir : DirectionUtils.BY_HORIZONTAL_INDEX)
		{
			VoxelShape forDir = Shapes.empty();
			if(dir!=Direction.NORTH)
				forDir = merge(forDir, new AABB(0, 0, .9375, 1, 1, 1));
			if(dir!=Direction.EAST)
				forDir = merge(forDir, new AABB(0, 0, 0, .0625, 1, 1));
			if(dir!=Direction.SOUTH)
				forDir = merge(forDir, new AABB(0, 0, 0, 1, 1, .0625));
			if(dir!=Direction.WEST)
				forDir = merge(forDir, new AABB(.9375, 0, 0, 1, 1, 1));
			FRAMES.put(dir, forDir);
		}
	}

	private static VoxelShape merge(VoxelShape a, AABB b)
	{
		return Shapes.joinUnoptimized(a, Shapes.create(b), BooleanOp.OR);
	}

	private final CoverType type;

	public MetalLadderBlock(CoverType type)
	{
		super(
				Properties.of(Material.METAL)
						.sound(SoundType.METAL)
						.strength(3, 15)
						.noOcclusion()
		);
		this.type = type;
		setRegistryName(new ResourceLocation(ImmersiveEngineering.MODID, "metal_ladder_"+type.name().toLowerCase(Locale.US)));

		IEContent.registeredIEBlocks.add(this);
		IEContent.registeredIEItems.add(new BlockItemIE(this));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx)
	{
		VoxelShape base = super.getShape(state, world, pos, ctx);
		if(type==CoverType.NONE)
			return base;
		else
		{
			Direction ladderSide = state.getValue(LadderBlock.FACING);
			return Shapes.joinUnoptimized(base, FRAMES.get(ladderSide), BooleanOp.OR);
		}
	}

	@Override
	public boolean isToolEffective(BlockState state, ToolType tool)
	{
		return super.isToolEffective(state, tool)||tool==HammerItem.HAMMER_TOOL;
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx)
	{
		BlockState baseState = super.getStateForPlacement(ctx);
		if(type==CoverType.NONE||baseState==null)
			return baseState;
		else
			return baseState.setValue(LadderBlock.FACING, Direction.fromYRot(ctx.getRotation()).getOpposite());
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos)
	{
		if(type==CoverType.NONE)
			return super.canSurvive(state, world, pos);
		else
			return true;
	}

	public enum CoverType
	{
		NONE,
		ALU,
		STEEL;
	}
}