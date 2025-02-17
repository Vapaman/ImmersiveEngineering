/*
 * BluSunrize
 * Copyright (c) 2018
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.util.loot;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.Serializer;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;

/**
 * @author BluSunrize - 16.08.2018
 */
public class IELootFunctions
{
	public static LootItemFunctionType bluprintz;
	public static LootItemFunctionType windmill;
	public static LootItemFunctionType propertyCount;

	public static LootPoolEntryType dropInventory;
	public static LootPoolEntryType tileDrop;
	public static LootPoolEntryType multiblockOrigBlock;

	public static void register()
	{
		bluprintz = registerFunction(BluprintzLootFunction.ID, new BluprintzLootFunction.Serializer());
		windmill = registerFunction(WindmillLootFunction.ID, new WindmillLootFunction.Serializer());
		propertyCount = registerFunction(PropertyCountLootFunction.ID, new PropertyCountLootFunction.Serializer());

		dropInventory = registerEntry(DropInventoryLootEntry.ID, new DropInventoryLootEntry.Serializer());
		tileDrop = registerEntry(TileDropLootEntry.ID, new TileDropLootEntry.Serializer());
		multiblockOrigBlock = registerEntry(MBOriginalBlockLootEntry.ID, new MBOriginalBlockLootEntry.Serializer());
	}

	private static LootPoolEntryType registerEntry(ResourceLocation id, Serializer<? extends LootPoolEntryContainer> serializer)
	{
		return Registry.register(
				Registry.LOOT_POOL_ENTRY_TYPE, id, new LootPoolEntryType(serializer)
		);
	}

	private static LootItemFunctionType registerFunction(ResourceLocation id, Serializer<? extends LootItemFunction> serializer)
	{
		return Registry.register(
				Registry.LOOT_FUNCTION_TYPE, id, new LootItemFunctionType(serializer)
		);
	}
}
