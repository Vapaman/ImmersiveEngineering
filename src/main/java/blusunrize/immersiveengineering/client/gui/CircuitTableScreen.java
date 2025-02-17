/*
 * BluSunrize
 * Copyright (c) 2021
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.client.gui;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.api.client.TextUtils;
import blusunrize.immersiveengineering.api.tool.LogicCircuitHandler.LogicCircuitInstruction;
import blusunrize.immersiveengineering.api.tool.LogicCircuitHandler.LogicCircuitOperator;
import blusunrize.immersiveengineering.api.tool.LogicCircuitHandler.LogicCircuitRegister;
import blusunrize.immersiveengineering.api.utils.ResettableLazy;
import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.elements.GuiButtonLogicCircuitRegister;
import blusunrize.immersiveengineering.client.gui.elements.GuiButtonState;
import blusunrize.immersiveengineering.client.gui.elements.GuiSelectingList;
import blusunrize.immersiveengineering.common.blocks.wooden.CircuitTableTileEntity;
import blusunrize.immersiveengineering.common.gui.CircuitTableContainer;
import blusunrize.immersiveengineering.common.items.LogicCircuitBoardItem;
import blusunrize.immersiveengineering.common.network.MessageContainerUpdate;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.fml.client.gui.GuiUtils;

import javax.annotation.Nullable;
import java.util.*;

import static blusunrize.immersiveengineering.client.ClientUtils.mc;
import static blusunrize.immersiveengineering.common.blocks.wooden.CircuitTableTileEntity.SLOT_TYPES;

public class CircuitTableScreen extends IEContainerScreen<CircuitTableContainer>
{
	private static final ResourceLocation TEXTURE = IEContainerScreen.makeTextureLocation("circuit_table");

	private final CircuitTableTileEntity tile;

	// Buttons
	private GuiSelectingList operatorList;
	private final List<GuiButtonState<LogicCircuitRegister>> inputButtons = new ArrayList<>(LogicCircuitOperator.TOTAL_MAX_INPUTS);
	private GuiButtonState<LogicCircuitRegister> outputButton;

	private final ResettableLazy<Optional<LogicCircuitInstruction>> instruction = new ResettableLazy<>(() -> {
		LogicCircuitOperator operator = getSelectedOperator();
		if(operator==null)
			return Optional.empty();
		// collect inputs
		LogicCircuitRegister[] inputs = inputButtons.stream()
				.map(GuiButtonState::getState)
				.filter(Objects::nonNull)
				.limit(operator.getArgumentCount())
				.toArray(LogicCircuitRegister[]::new);
		// if input array is too short, can't make an instruction
		if(inputs.length < operator.getArgumentCount())
			return Optional.empty();
		// else, build instruction
		return Optional.of(new LogicCircuitInstruction(operator, outputButton.getState(), inputs));
	});

	public CircuitTableScreen(CircuitTableContainer container, Inventory inventoryPlayer, Component title)
	{
		super(container, inventoryPlayer, title);
		this.tile = container.tile;
		this.imageWidth = 234;
		this.imageHeight = 182;
	}

	@Override
	public void init()
	{
		super.init();
		mc().keyboardHandler.setSendRepeatsToGui(true);

		this.operatorList = (GuiSelectingList)this.addButton(new GuiSelectingList(this, leftPos+58, topPos+16, 36, 56, btn -> {
			this.minecraft.tell(this::updateButtons);
			this.minecraft.tell(this::updateInstruction);
		}, Arrays.stream(LogicCircuitOperator.values()).map(Enum::name).toArray(String[]::new)).setPadding(1, 1, 2, 0));

		this.outputButton = this.addButton(new GuiButtonLogicCircuitRegister(
				leftPos+121, topPos+56,
				new TextComponent("Output"), btn -> this.minecraft.tell(this::updateInstruction))
		);
		this.updateButtons();
	}

	@Nullable
	private LogicCircuitOperator getSelectedOperator()
	{
		return LogicCircuitOperator.getByString(operatorList.getSelectedString());
	}

	private void updateInstruction()
	{
		this.instruction.reset();
		this.instruction.get().ifPresent(instr -> {
			this.menu.instruction = instr;
			ImmersiveEngineering.packetHandler.sendToServer(new MessageContainerUpdate(this.menu.containerId, instr.serialize()));
		});
	}

	private void updateButtons()
	{
		LogicCircuitOperator operator = getSelectedOperator();
		if(operator!=null)
		{
			int inputCount = operator.getArgumentCount();
			int inputStart = 130-(inputCount*10-1);
			if(inputCount < this.inputButtons.size())
			{
				Iterator<GuiButtonState<LogicCircuitRegister>> it = this.inputButtons.iterator();
				int i = 0;
				// Reposition buttons and remove excess
				while(it.hasNext())
				{
					GuiButtonState btn = it.next();
					btn.x = leftPos+inputStart+20*i;
					if(++i > inputCount)
					{
						this.buttons.remove(btn);
						this.children.remove(btn);
						it.remove();
					}
				}
			}
			else
			{
				for(int i = 0; i < inputCount; i++)
				{
					if(i < this.inputButtons.size()) // Reposition buttons
						this.inputButtons.get(i).x = leftPos+inputStart+20*i;
					else // Add new ones
						this.inputButtons.add(this.addButton(new GuiButtonLogicCircuitRegister(
								leftPos+inputStart+20*i, topPos+18,
								new TextComponent("Input "+(i+1)), btn -> this.minecraft.tell(this::updateInstruction))
						));
				}
			}
		}
	}

	@Override
	public void render(PoseStack transform, int mx, int my, float partial)
	{
		super.render(transform, mx, my, partial);

		List<Component> tooltip = new ArrayList<>();

		if(this.hoveredSlot!=null&&this.hoveredSlot.index < SLOT_TYPES.length&&!this.hoveredSlot.hasItem())
		{
			int slotNum = this.hoveredSlot.index;
			tooltip.add(TextUtils.applyFormat(
					new TranslatableComponent(Lib.DESC_INFO+"circuit_table.slot."+SLOT_TYPES[slotNum]),
					ChatFormatting.GRAY
			));
		}
		if(isMouseIn(mx, my, 217, 16, 7, 46))
			tooltip.add(new TextComponent(tile.getEnergyStored(null)+"/"+tile.getMaxEnergyStored(null)+" IF"));

		if(isMouseIn(mx, my, 52, 7, 100, 70)&&this.inventory.getCarried().getItem() instanceof LogicCircuitBoardItem)
			tooltip.add(TextUtils.applyFormat(
					new TranslatableComponent(Lib.DESC_INFO+"circuit_table.copy"),
					ChatFormatting.GRAY
			));

		for(GuiButtonState<LogicCircuitRegister> input : this.inputButtons)
			if(input.isHovered())
				tooltip.add(TextUtils.applyFormat(input.getState().getDescription(), ChatFormatting.GRAY));
		if(this.outputButton.isHovered())
			tooltip.add(TextUtils.applyFormat(outputButton.getState().getDescription(), ChatFormatting.GRAY));

		if(!tooltip.isEmpty())
			GuiUtils.drawHoveringText(transform, tooltip, mx, my, width, height, -1, font);
	}

	@Override
	protected void renderLabels(PoseStack transform, int mouseX, int mouseY)
	{
		drawCenteredString(transform, this.font, "Operator:", 76, 4, DyeColor.LIGHT_GRAY.getColorValue());
		drawCenteredString(transform, this.font, "Inputs:", 130, 8, DyeColor.LIGHT_GRAY.getColorValue());
		drawCenteredString(transform, this.font, "Outputs:", 130, 42, DyeColor.LIGHT_GRAY.getColorValue());

		for(int i = 0; i < SLOT_TYPES.length; i++)
		{
			int amount = 0;
			DyeColor col = DyeColor.LIGHT_GRAY;
			if(this.instruction.get().isPresent())
			{
				amount = CircuitTableTileEntity.getIngredientAmount(this.instruction.get().get(), i);
				if(this.menu.slots.get(i).getItem().getCount() >= amount)
					col = DyeColor.GREEN;
				else
					col = DyeColor.RED;
			}
			this.font.draw(transform, "x "+amount, 30, 18+20*i, col.getColorValue());
		}
	}

	@Override
	protected void renderBg(PoseStack transform, float f, int mx, int my)
	{
		RenderSystem.color3f(1.0F, 1.0F, 1.0F);
		ClientUtils.bindTexture(TEXTURE);
		this.blit(transform, leftPos, topPos, 0, 0, imageWidth, imageHeight);

		int stored = (int)(46*(tile.getEnergyStored(null)/(float)tile.getMaxEnergyStored(null)));
		fillGradient(transform, leftPos+217, topPos+16+(46-stored), leftPos+224, topPos+62, 0xffb51500, 0xff600b00);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers)
	{
		for(GuiButtonState<?> input : this.inputButtons)
			if(input.isHovered())
				return input.charTyped(codePoint, modifiers);
		if(this.outputButton.isHovered())
			return this.outputButton.charTyped(codePoint, modifiers);
		return super.charTyped(codePoint, modifiers);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		if(isMouseIn((int)mouseX, (int)mouseY, 52, 7, 100, 70)&&this.inventory.getCarried().getItem() instanceof LogicCircuitBoardItem)
		{
			LogicCircuitInstruction instr = LogicCircuitBoardItem.getInstruction(this.inventory.getCarried());
			if(instr!=null)
			{
				this.operatorList.setSelectedString(instr.getOperator().name());
				this.updateButtons();
				this.outputButton.setStateByInt(instr.getOutput().ordinal());
				LogicCircuitRegister[] inputs = instr.getInputs();
				for(int i = 0; i < inputs.length; i++)
					this.inputButtons.get(i).setStateByInt(inputs[i].ordinal());
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}
}