/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.blocks.cloth;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.client.utils.FontUtils;
import blusunrize.immersiveengineering.common.IETileTypes;
import blusunrize.immersiveengineering.common.blocks.BlockItemIE;
import blusunrize.immersiveengineering.common.blocks.IETileProviderBlock;
import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class StripCurtainBlock extends IETileProviderBlock<StripCurtainTileEntity>
{
	public static BooleanProperty CEILING_ATTACHED = BooleanProperty.create("ceiling_attached");
	public static EnumProperty<Direction> FACING = IEProperties.FACING_HORIZONTAL;

	public StripCurtainBlock()
	{
		super("strip_curtain", IETileTypes.STRIP_CURTAIN,
				Block.Properties.of(Material.WOOL).sound(SoundType.WOOL).strength(0.8F).noOcclusion(),
				BlockItemIE::new);
		setLightOpacity(0);
		setHasColours();
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		builder.add(CEILING_ATTACHED, FACING);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag advanced)
	{
		if(ItemNBTHelper.hasKey(stack, "colour"))
		{
			int color = ItemNBTHelper.getInt(stack, "colour");
			tooltip.add(FontUtils.withAppendColoredColour(new TranslatableComponent(Lib.DESC_INFO+"colour"), color));
		}
	}

	@Override
	public boolean allowHammerHarvest(BlockState blockState)
	{
		return true;
	}
}
