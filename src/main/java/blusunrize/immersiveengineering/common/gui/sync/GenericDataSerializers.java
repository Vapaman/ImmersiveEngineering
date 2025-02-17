/*
 * BluSunrize
 * Copyright (c) 2021
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 *
 */

package blusunrize.immersiveengineering.common.gui.sync;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class GenericDataSerializers
{
	private static final List<DataSerializer<?>> SERIALIZERS = new ArrayList<>();
	public static final DataSerializer<Integer> INT32 = register(
			FriendlyByteBuf::readVarInt, FriendlyByteBuf::writeVarInt
	);

	private static <T> DataSerializer<T> register(
			Function<FriendlyByteBuf, T> read, BiConsumer<FriendlyByteBuf, T> write
	)
	{
		DataSerializer<T> serializer = new DataSerializer<>(read, write, SERIALIZERS.size());
		SERIALIZERS.add(serializer);
		return serializer;
	}

	public static DataPair<?> read(FriendlyByteBuf buffer)
	{
		DataSerializer<?> serializer = SERIALIZERS.get(buffer.readVarInt());
		return serializer.read(buffer);
	}

	public static final class DataSerializer<T>
	{
		private final Function<FriendlyByteBuf, T> read;
		private final BiConsumer<FriendlyByteBuf, T> write;
		private final int id;

		public DataSerializer(Function<FriendlyByteBuf, T> read, BiConsumer<FriendlyByteBuf, T> write, int id)
		{
			this.read = read;
			this.write = write;
			this.id = id;
		}

		public DataPair<T> read(FriendlyByteBuf from)
		{
			return new DataPair<>(this, read.apply(from));
		}
	}

	public static final class DataPair<T>
	{
		private final DataSerializer<T> serializer;
		private final T data;

		public DataPair(DataSerializer<T> serializer, T data)
		{
			this.serializer = serializer;
			this.data = data;
		}

		public void write(FriendlyByteBuf to)
		{
			to.writeVarInt(serializer.id);
			serializer.write.accept(to, data);
		}

		public T data()
		{
			return data;
		}
	}
}
