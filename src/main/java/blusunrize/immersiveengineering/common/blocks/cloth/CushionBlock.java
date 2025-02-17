/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.blocks.cloth;

import blusunrize.immersiveengineering.common.blocks.IEBaseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import blusunrize.immersiveengineering.common.blocks.BlockItemIE;

public class CushionBlock extends IEBaseBlock
{
	public CushionBlock()
	{
		super("cushion", Block.Properties.of(Material.WOOL).sound(SoundType.WOOL).strength(0.8F), BlockItemIE::new);
	}

	@Override
	public void fallOn(Level w, BlockPos pos, Entity entity, float fallStrength)
	{
		entity.fallDistance = 0;
	}
}
