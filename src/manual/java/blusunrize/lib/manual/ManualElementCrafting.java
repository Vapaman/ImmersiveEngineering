/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.lib.manual;

import blusunrize.lib.manual.gui.GuiButtonManualNavigation;
import blusunrize.lib.manual.gui.ManualScreen;
import blusunrize.lib.manual.utils.PrivateAccess;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.common.crafting.IShapedRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ManualElementCrafting extends SpecialManualElements
{
	private Object[] recipeRows;
	private List<PositionedItemStack[]>[] recipeLayout;
	private int recipePage[];
	private int heightPixels[];

	public ManualElementCrafting(ManualInstance manual, Object... stacks)
	{
		super(manual);
		this.recipeRows = stacks;
		this.recipePage = new int[stacks.length];
		this.heightPixels = new int[stacks.length];
		this.recipeLayout = (List<PositionedItemStack[]>[])new List[stacks.length];
		for(int i = 0; i < stacks.length; ++i)
			recipeLayout[i] = new ArrayList<>();
		recalculateCraftingRecipes();
	}

	@Override
	public void recalculateCraftingRecipes()
	{
		if(Minecraft.getInstance().level==null)
			return;
		this.providedItems.clear();
		for(int iStack = 0; iStack < recipeRows.length; iStack++)
		{
			this.recipeLayout[iStack].clear();
			Object stack = recipeRows[iStack];
			if(stack instanceof PositionedItemStack[])
				addFixedRecipe(iStack, (PositionedItemStack[])stack);
			else if(stack instanceof Object[])
				for(Object subStack : (Object[])stack)
				{
					if(subStack instanceof PositionedItemStack[])
						addFixedRecipe(iStack, (PositionedItemStack[])subStack);
					else
						checkAllRecipesFor(subStack, iStack);
				}
			else
				checkAllRecipesFor(stack, iStack);
		}
	}

	private void checkAllRecipesFor(Object stack, int recipeIndex)
	{
		RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
		Map<ResourceLocation, Recipe<CraftingContainer>> recipes = PrivateAccess.getRecipes(
				recipeManager, RecipeType.CRAFTING
		);
		if(stack instanceof ResourceLocation)
		{
			Recipe<CraftingContainer> recipe = recipes.get(stack);
			if(recipe!=null)
				checkRecipe(recipe, stack, recipeIndex);
		}
		else
			for(Recipe<CraftingContainer> recipe : recipes.values())
				checkRecipe(recipe, stack, recipeIndex);
	}

	private void checkRecipe(Recipe<CraftingContainer> rec, Object stack, int recipeIndex)
	{
		boolean matches = !rec.getResultItem().isEmpty()&&ManualUtils.stackMatchesObject(rec.getResultItem(), stack);
		if(!matches&&stack instanceof ResourceLocation&&stack.equals(rec.getId()))
			matches = true;
		if(matches)
		{
			NonNullList<Ingredient> ingredientsPre = rec.getIngredients();
			int recipeWidth;
			int recipeHeight;
			if(rec instanceof ShapelessRecipe)
			{
				recipeWidth = Mth.clamp(ingredientsPre.size(), 1, 3);
				recipeHeight = (ingredientsPre.size()-1)/3+1;
			}
			else if(rec instanceof IShapedRecipe)
			{
				recipeWidth = ((IShapedRecipe<?>)rec).getRecipeWidth();
				recipeHeight = ((IShapedRecipe<?>)rec).getRecipeHeight();
			}
			else
				return;

			int yOffset = (this.heightPixels[recipeIndex]-18*recipeHeight)/2;
			if(yOffset < 0)
				yOffset = 0;
			PositionedItemStack[] pIngredients = new PositionedItemStack[ingredientsPre.size()+1];
			int xBase = (120-(recipeWidth+2)*18)/2;
			for(int heightPos = 0; heightPos < recipeHeight; heightPos++)
				for(int widthPos = 0; widthPos < recipeWidth; widthPos++)
				{
					int index = heightPos*recipeWidth+widthPos;
					if(index < ingredientsPre.size())
						pIngredients[index] = new PositionedItemStack(ingredientsPre.get(index),
								xBase+widthPos*18, heightPos*18+yOffset);
				}
			pIngredients[pIngredients.length-1] = new PositionedItemStack(rec.getResultItem(), xBase+recipeWidth*18+18,
					recipeHeight*9-8+yOffset);
			if(this.heightPixels[recipeIndex] < recipeHeight*18)
			{
				this.heightPixels[recipeIndex] = recipeHeight*18;
				for(int prevId = 0; prevId <= recipeIndex; ++prevId)
					for(PositionedItemStack[] oldStacks : recipeLayout[prevId])
						for(PositionedItemStack oldStack : oldStacks)
							oldStack.y += yOffset;
			}
			this.recipeLayout[recipeIndex].add(pIngredients);
			addProvidedItem(rec.getResultItem());
		}
	}

	private void addFixedRecipe(int index, PositionedItemStack[] recipe)
	{
		int height = 0;
		for(PositionedItemStack stack : recipe)
			if(stack.y > height)
				height = stack.y;
		height += 18;
		if(this.heightPixels[index] < height)
		{
			int offset = (height-heightPixels[index])/2;
			this.heightPixels[index] = height;
			for(int prevId = 0; prevId <= index; ++prevId)
				for(PositionedItemStack[] oldStacks : recipeLayout[prevId])
					for(PositionedItemStack oldStack : oldStacks)
						oldStack.y += offset;
		}
		else
		{
			int offset = (heightPixels[index]-height)/2;
			for(PositionedItemStack stack : recipe)
				stack.y += offset;
		}
		recipeLayout[index].add(recipe);
	}

	@Override
	public void onOpened(ManualScreen gui, int x, int y, List<Button> pageButtons)
	{
		int recipeYOffset = 0;
		for(int i = 0; i < this.recipeRows.length; i++)
		{
			if(this.recipeLayout[i].size() > 1)
			{
				final int iFinal = i;
				pageButtons.add(new GuiButtonManualNavigation(gui, x-2, y+recipeYOffset+heightPixels[i]/2-5, 8, 10, 0, btn -> {
					recipePage[iFinal]--;
					if(recipePage[iFinal] < 0)
						recipePage[iFinal] = recipeLayout[iFinal].size()-1;
				}));
				pageButtons.add(new GuiButtonManualNavigation(gui, x+122-16, y+recipeYOffset+heightPixels[i]/2-5, 8, 10, 1, btn -> {
					recipePage[iFinal]++;
					if(recipePage[iFinal] >= recipeLayout[iFinal].size())
						recipePage[iFinal] = 0;
				}));
			}
			if(this.recipeLayout[i].size() > 0)
				recipeYOffset += heightPixels[i]+8;
		}
		super.onOpened(gui, x, y+recipeYOffset-2, pageButtons);
	}

	@Override
	public void render(PoseStack transform, ManualScreen gui, int x, int y, int mx, int my)
	{
		Lighting.turnBackOn();

		int totalYOff = 0;
		highlighted = ItemStack.EMPTY;
		for(int i = 0; i < recipeRows.length; i++)
		{
			List<PositionedItemStack[]> rList = this.recipeLayout[i];
			if(!rList.isEmpty()&&recipePage[i] >= 0&&recipePage[i] < rList.size())
			{
				int maxX = 0;
				for(PositionedItemStack pstack : rList.get(recipePage[i]))
					if(pstack!=null)
					{
						if(pstack.x > maxX)
							maxX = pstack.x;
						GuiComponent.fill(transform, x+pstack.x, y+totalYOff+pstack.y, x+pstack.x+16, y+totalYOff+pstack.y+16, 0x33666666);
					}

				MultiBufferSource.BufferSource buffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
				ManualUtils.drawTexturedRect(transform, buffers, manual.texture, x+maxX-17,
						y+totalYOff+heightPixels[i]/2-5, 16, 10, 0/256f,
						16/256f, 226/256f, 236/256f);
				buffers.endBatch();

				totalYOff += heightPixels[i]+8;
			}
		}

		totalYOff = 0;
		for(int i = 0; i < recipeLayout.length; i++)
		{
			List<PositionedItemStack[]> rList = this.recipeLayout[i];
			if(!rList.isEmpty()&&recipePage[i] >= 0&&recipePage[i] < rList.size())
			{
				for(PositionedItemStack pstack : rList.get(recipePage[i]))
					if(pstack!=null)
						if(!pstack.getStack().isEmpty())
						{
							ManualUtils.renderItemStack(transform, pstack.getStack(), x+pstack.x, y+totalYOff+pstack.y, true);
							if(mx >= x+pstack.x&&mx < x+pstack.x+16&&my >= y+totalYOff+pstack.y&&my < y+totalYOff+pstack.y+16)
								highlighted = pstack.getStack();
						}
				totalYOff += heightPixels[i]+8;
			}
		}

		this.renderHighlightedTooltip(transform, gui, mx, my);
	}

	@Override
	public boolean listForSearch(String searchTag)
	{
		for(Object stack : recipeRows)
		{
			if(stack instanceof Object[])
			{
				for(Object subStack : (Object[])stack)
					if(subStack instanceof ItemStack&&ManualUtils.listStack(searchTag, (ItemStack)subStack))
						return true;
			}
			else if(stack instanceof ItemStack)
				if(ManualUtils.listStack(searchTag, (ItemStack)stack))
					return true;
		}
		return false;
	}

	@Override
	public int getPixelsTaken()
	{
		int yOff = 0;
		for(int heightPixel : this.heightPixels)
			yOff += heightPixel+8;
		return yOff;
	}
}
