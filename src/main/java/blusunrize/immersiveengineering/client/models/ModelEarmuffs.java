/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.client.models;

import net.minecraft.client.model.geom.ModelPart;

public class ModelEarmuffs extends ModelIEArmorBase
{
	public ModelPart[] modelParts;
	public ModelPart[] colouredParts;

	public ModelEarmuffs(float modelSize, float yOffsetIn, int textureWidthIn, int textureHeightIn)
	{
		super(modelSize, yOffsetIn, textureWidthIn, textureHeightIn);
		modelParts = new ModelPart[8];
		colouredParts = new ModelPart[4];
		this.modelParts[0] = new ModelPart(this, 0, 16);
		this.modelParts[0].addBox(-4.5f, -8.5f, 0f, 9, 1, 1, modelSize);
		this.head.addChild(modelParts[0]);
		this.modelParts[1] = new ModelPart(this, 0, 18);
		this.modelParts[1].addBox(-4.5f, -7.5f, 0f, 1, 4, 1, modelSize);
		this.modelParts[0].addChild(modelParts[1]);
		this.modelParts[2] = new ModelPart(this, 0, 18);
		this.modelParts[2].addBox(3.5f, -7.5f, 0f, 1, 4, 1, modelSize);
		this.modelParts[0].addChild(modelParts[2]);

		this.modelParts[3] = new ModelPart(this, 0, 16);
		this.modelParts[3].addBox(-4.5f, -8.5f, -1.5f, 9, 1, 1, modelSize);
		this.head.addChild(modelParts[3]);
		this.modelParts[4] = new ModelPart(this, 0, 18);
		this.modelParts[4].addBox(-4.5f, -7.5f, -1.5f, 1, 4, 1, modelSize);
		this.modelParts[3].addChild(modelParts[4]);
		this.modelParts[5] = new ModelPart(this, 0, 18);
		this.modelParts[5].addBox(3.5f, -7.5f, -1.5f, 1, 4, 1, modelSize);
		this.modelParts[3].addChild(modelParts[5]);

		this.modelParts[6] = new ModelPart(this, 20, 24);
		this.modelParts[6].addBox(-4.375f, -5.5f, -1.75f, 1, 4, 3, modelSize+.125f);
		this.head.addChild(modelParts[6]);
		this.colouredParts[0] = new ModelPart(this, 20, 16);
		this.colouredParts[0].addBox(-4.875f, -6.5f, -1.75f, 1, 5, 3, modelSize);
		this.head.addChild(colouredParts[0]);
		this.colouredParts[1] = new ModelPart(this, 28, 16);
		this.colouredParts[1].addBox(-5.25f, -5f, -1.25f, 1, 3, 2, modelSize+.125f);
		this.colouredParts[0].addChild(colouredParts[1]);

		this.modelParts[7] = new ModelPart(this, 20, 24);
		this.modelParts[7].addBox(3.375f, -5.5f, -1.75f, 1, 4, 3, modelSize+.125f);
		this.head.addChild(modelParts[7]);
		this.colouredParts[2] = new ModelPart(this, 20, 16);
		this.colouredParts[2].addBox(3.875f, -6.5f, -1.75f, 1, 5, 3, modelSize);
		this.head.addChild(colouredParts[2]);
		this.colouredParts[3] = new ModelPart(this, 28, 16);
		this.colouredParts[3].addBox(4.25f, -5f, -1.25f, 1, 3, 2, modelSize+.125f);
		this.colouredParts[2].addChild(colouredParts[3]);

		this.hat.visible = false;
		this.body.visible = false;
		this.leftLeg.visible = false;
		this.rightLeg.visible = false;
	}

	static ModelEarmuffs modelInstance;

	public static ModelEarmuffs getModel()
	{
		if(modelInstance==null)
			modelInstance = new ModelEarmuffs(.0625f, 0, 64, 32);
		return modelInstance;
	}
}