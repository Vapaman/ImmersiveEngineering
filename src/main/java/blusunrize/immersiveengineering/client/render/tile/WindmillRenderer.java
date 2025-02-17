/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.client.render.tile;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.api.IEProperties.IEObjState;
import blusunrize.immersiveengineering.api.IEProperties.VisibilityList;
import blusunrize.immersiveengineering.api.client.IVertexBufferHolder;
import blusunrize.immersiveengineering.api.utils.client.SinglePropertyModelData;
import blusunrize.immersiveengineering.common.blocks.IEBlocks.WoodenDevices;
import blusunrize.immersiveengineering.common.blocks.wooden.WindmillTileEntity;
import blusunrize.immersiveengineering.common.util.Utils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraftforge.client.model.data.IModelData;

import java.util.ArrayList;
import java.util.List;

//TODO maybe replace with Forge animations?
public class WindmillRenderer extends BlockEntityRenderer<WindmillTileEntity>
{
	public static DynamicModel<Void> MODEL;
	private static final IVertexBufferHolder[] BUFFERS = new IVertexBufferHolder[9];

	public WindmillRenderer(BlockEntityRenderDispatcher rendererDispatcherIn)
	{
		super(rendererDispatcherIn);
	}

	private static IVertexBufferHolder getBufferHolder(int sails)
	{
		if(BUFFERS[sails]==null)
			BUFFERS[sails] = IVertexBufferHolder.create(() -> {
				BakedModel model = MODEL.get(null);
				List<String> parts = new ArrayList<>();
				parts.add("base");
				for(int i = 1; i <= sails; i++)
					parts.add("sail_"+i);
				IModelData data = new SinglePropertyModelData<>(
						new IEObjState(VisibilityList.show(parts)), IEProperties.Model.IE_OBJ_STATE);
				return model.getQuads(WoodenDevices.windmill.defaultBlockState(), null, Utils.RAND, data);
			});
		return BUFFERS[sails];
	}

	@Override
	public void render(WindmillTileEntity tile, float partialTicks, PoseStack transform, MultiBufferSource bufferIn,
					   int combinedLightIn, int combinedOverlayIn)
	{
		if(!tile.getWorldNonnull().hasChunkAt(tile.getBlockPos()))
			return;
		transform.pushPose();
		transform.translate(.5, .5, .5);

		float dir = tile.getFacing()==Direction.SOUTH?0: tile.getFacing()==Direction.NORTH?180: tile.getFacing()==Direction.EAST?90: -90;
		float rot = 360*(tile.rotation+(!tile.canTurn||tile.rotation==0?0: partialTicks)*tile.perTick);

		transform.mulPose(new Quaternion(new Vector3f(tile.getFacing().getAxis()==Axis.X?1: 0, 0, tile.getFacing().getAxis()==Axis.Z?1: 0), rot, true));
		transform.mulPose(new Quaternion(new Vector3f(0, 1, 0), dir, true));

		transform.translate(-.5, -.5, -.5);
		getBufferHolder(tile.sails)
				.render(RenderType.cutoutMipped(), combinedLightIn, combinedOverlayIn, bufferIn, transform);
		transform.popPose();
	}

	public static void reset()
	{
		for(IVertexBufferHolder vbo : BUFFERS)
			if(vbo!=null)
				vbo.reset();
	}
}