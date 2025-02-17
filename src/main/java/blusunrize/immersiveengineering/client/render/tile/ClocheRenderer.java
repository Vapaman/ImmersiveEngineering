/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.client.render.tile;

import blusunrize.immersiveengineering.api.crafting.ClocheRecipe;
import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.utils.RenderUtils;
import blusunrize.immersiveengineering.client.utils.TransformingVertexBuilder;
import blusunrize.immersiveengineering.common.blocks.metal.ClocheTileEntity;
import blusunrize.immersiveengineering.common.util.Utils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.EmptyModelData;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Consumer;

public class ClocheRenderer extends BlockEntityRenderer<ClocheTileEntity>
{
	private static final Map<BlockState, List<BakedQuad>> plantQuads = new HashMap<>();

	public ClocheRenderer(BlockEntityRenderDispatcher rendererDispatcherIn)
	{
		super(rendererDispatcherIn);
	}

	@Override
	public void render(ClocheTileEntity tile, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn)
	{
		if(tile.dummy!=0||!tile.getWorldNonnull().hasChunkAt(tile.getBlockPos()))
			return;
		final BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
		BlockPos blockPos = tile.getBlockPos();

		// Render particles in the TER rather than using the standard particle engine to avoid depth issues/the
		// particles not rendering at all outside of fabulous mode
		tile.particles.get().render(matrixStack, blockPos, bufferIn, partialTicks);

		ClocheRecipe recipe = tile.cachedRecipe.get();
		if(recipe!=null)
		{
			VertexConsumer baseBuilder = bufferIn.getBuffer(RenderType.cutout());
			matrixStack.pushPose();
			matrixStack.translate(0, 1.0625, 0);

			NonNullList<ItemStack> inventory = tile.getInventory();
			ItemStack seed = inventory.get(ClocheTileEntity.SLOT_SEED);
			ItemStack soil = inventory.get(ClocheTileEntity.SLOT_SOIL);
			float growth = Mth.clamp(tile.renderGrowth/recipe.getTime(seed, soil), 0, 1);
			float scale = recipe.renderFunction.getScale(seed, growth);
			matrixStack.translate((1-scale)/2, 0, (1-scale)/2);
			matrixStack.scale(scale, scale, scale);

			Collection<Pair<BlockState, Transformation>> blocks = recipe.renderFunction.getBlocks(seed, growth);
			for(Pair<BlockState, Transformation> block : blocks)
			{
				BlockState state = block.getLeft();
				List<BakedQuad> plantQuadList = plantQuads.get(state);
				if(plantQuadList==null)
				{
					BakedModel plantModel = blockRenderer.getBlockModel(state);
					plantQuadList = new ArrayList<>(plantModel.getQuads(state, null, Utils.RAND, EmptyModelData.INSTANCE));
					for(Direction f : Direction.values())
						plantQuadList.addAll(plantModel.getQuads(state, f, Utils.RAND, EmptyModelData.INSTANCE));
					plantQuads.put(state, plantQuadList);
				}
				int col = ClientUtils.mc().getBlockColors().getColor(state, null, blockPos, -1);
				block.getRight().push(matrixStack);
				RenderUtils.renderModelTESRFancy(plantQuadList, new TransformingVertexBuilder(baseBuilder, matrixStack),
						tile.getWorldNonnull(), blockPos, false, col, combinedLightIn);
				matrixStack.popPose();
			}

			// Injection of quads from dynamic recipes
			List<BakedQuad> injectedQuadList = new ArrayList<>();
			Consumer<?> quadInjector = (object) -> {if(object instanceof BakedQuad) injectedQuadList.add((BakedQuad) object);};
			recipe.renderFunction.injectQuads(seed, growth, quadInjector);
			if(injectedQuadList.size() > 0)
			{
				RenderUtils.renderModelTESRFancy(injectedQuadList, new TransformingVertexBuilder(baseBuilder, matrixStack),
						tile.getWorldNonnull(), blockPos, false, -1, combinedLightIn);
			}

			matrixStack.popPose();
		}
	}

	public static void reset()
	{
		plantQuads.clear();
	}
}