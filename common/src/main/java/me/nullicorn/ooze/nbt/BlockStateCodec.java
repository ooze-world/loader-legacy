package me.nullicorn.ooze.nbt;

import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.ooze.api.ResourceLocation;
import me.nullicorn.ooze.api.serialize.Codec;
import me.nullicorn.ooze.api.serialize.CodingException;
import me.nullicorn.ooze.api.world.BlockState;

/**
 * NBT serialization for {@link BlockState block states}.
 *
 * @author Nullicorn
 */
public class BlockStateCodec implements Codec<BlockState, NBTCompound> {

  // NBT tag names used by Minecraft.
  private static final String TAG_NAME       = "Name";
  private static final String TAG_PROPERTIES = "Properties";

  @Override
  public NBTCompound encode(BlockState state) {
    if (state == null) {
      throw new IllegalArgumentException("Cannot encode null block state");
    }

    NBTCompound encoded = new NBTCompound();
    encoded.put(TAG_NAME, state.getName());
    if (state.hasProperties()) {
      encoded.put(TAG_PROPERTIES, state.getProperties());
    }
    return encoded;
  }

  @Override
  public BlockState decode(NBTCompound encoded) throws CodingException {
    if (encoded == null) {
      throw new IllegalArgumentException("Cannot decoded null block state");
    }

    ResourceLocation name = ResourceLocation.fromString(encoded.getString(TAG_NAME, null));
    if (name == null) {
      throw new CodingException("Block state is missing a name: " + encoded);
    }

    return new BlockState(name, encoded.getCompound(TAG_PROPERTIES));
  }
}
