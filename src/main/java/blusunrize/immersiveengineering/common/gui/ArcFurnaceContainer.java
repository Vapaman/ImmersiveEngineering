/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.gui;

import blusunrize.immersiveengineering.common.blocks.metal.ArcFurnaceTileEntity;
import javax.annotation.Nonnull;
import blusunrize.immersiveengineering.common.gui.sync.GenericContainerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ArcFurnaceContainer extends IEBaseContainer<ArcFurnaceTileEntity>
{
	public ArcFurnaceContainer(int id, Inventory inventoryPlayer, ArcFurnaceTileEntity tile)
	{
		super(tile, id);
		this.tile = tile;
		for(int i = 0; i < 12; i++)
			this.addSlot(new IESlot.ArcInput(this, this.inv, i, 10+i%3*21, 34+i/3*18));
		for(int i = 0; i < 4; i++)
			this.addSlot(new IESlot.ArcAdditive(this, this.inv, 12+i, 114+i%2*18, 34+i/2*18));
		for(int i = 0; i < 6; i++)
			this.addSlot(new IESlot.Output(this, this.inv, 16+i, 78+i%3*18, 80+i/3*18));
		this.addSlot(new IESlot.Output(this, this.inv, 22, 132, 98));

		this.addSlot(new IESlot.ArcElectrode(this, this.inv, 23, 62, 10));
		this.addSlot(new IESlot.ArcElectrode(this, this.inv, 24, 80, 10));
		this.addSlot(new IESlot.ArcElectrode(this, this.inv, 25, 98, 10));

		slotCount = 26;

		for(int i = 0; i < 3; i++)
			for(int j = 0; j < 9; j++)
				addSlot(new Slot(inventoryPlayer, j+i*9+9, 8+j*18, 126+i*18));
		for(int i = 0; i < 9; i++)
			addSlot(new Slot(inventoryPlayer, i, 8+i*18, 184));
		addGenericData(GenericContainerData.energy(tile.energyStorage));
	}

	@Nonnull
	@Override
	public ItemStack quickMoveStack(Player player, int slot)
	{
		return super.quickMoveStack(player, slot);
//		ItemStack stack = null;
//		Slot slotObject = (Slot) inventorySlots.get(slot);
//
//		if (slotObject != null && slotObject.getHasStack())
//		{
//			ItemStack stackInSlot = slotObject.getStack();
//			stack = stackInSlot.copy();
//
//			if (slot < slotCount)
//			{
//				if(!this.mergeItemStack(stackInSlot, slotCount, (slotCount + 36), true))
//					return null;
//			}
//			else
//			{
//				int i = -1;
//				int j = -1;
//				if(ArcFurnaceRecipe.isValidRecipeInput(stackInSlot))
//				{
//					i=0;
//					j=12;
//				}
//				else if(ArcFurnaceRecipe.isValidRecipeAdditive(stackInSlot))
//				{
//					i=12;
//					j=16;
//				}
//				else if(IEContent.itemGraphiteElectrode.equals(stack.getItem()))
//				{
//					i=23;
//					j=26;
//				}
//				if(i!=-1 && j!=-1)
//					if(!this.mergeItemStack(stackInSlot, i,j, false))
//						return null;
//			}
//
//			if (stackInSlot.stackSize == 0)
//				slotObject.putStack(null);
//			else
//				slotObject.onSlotChanged();
//
//			if (stackInSlot.stackSize == stack.stackSize)
//				return null;
//			slotObject.onTake(player, stackInSlot);
//		}
//		return stack;
	}
}