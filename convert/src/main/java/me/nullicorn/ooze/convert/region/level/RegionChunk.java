package me.nullicorn.ooze.convert.region.level;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import lombok.Getter;
import me.nullicorn.ooze.Location2D;
import me.nullicorn.ooze.storage.UnpaddedIntArray;
import me.nullicorn.ooze.serialize.OozeDataOutputStream;
import me.nullicorn.ooze.storage.BlockPalette;
import me.nullicorn.ooze.world.BlockState;
import me.nullicorn.ooze.world.Chunk;

/**
 * A chunk stored by Minecraft in a {@link me.nullicorn.ooze.convert.region.RegionFile region
 * file}.
 *
 * @author Nullicorn
 */
public class RegionChunk implements Chunk {

  static final         int WIDTH              = 16;
  static final         int DEPTH              = 16;
  static final         int SECTION_HEIGHT     = 16;
  private static final int SECTIONS_PER_CHUNK = 16;
  private static final int HEIGHT             = SECTION_HEIGHT * SECTIONS_PER_CHUNK;

  @Getter
  private final Location2D location;

  @Getter
  private final int dataVersion;

  // Individual sections in this array may be null if they are empty, though empty sections can also
  // be present.
  private final RegionChunkSection[] sections;

  public RegionChunk(Location2D location, int dataVersion) {
    this.location = location;
    this.dataVersion = dataVersion;
    sections = new RegionChunkSection[SECTIONS_PER_CHUNK];
  }

  @Override
  public int getWidth() {
    return WIDTH;
  }

  @Override
  public int getHeight() {
    return HEIGHT;
  }

  @Override
  public int getDepth() {
    return DEPTH;
  }

  @Override
  public int getMinX() {
    return 0;
  }

  @Override
  public int getMinY() {
    return 0;
  }

  @Override
  public int getMinZ() {
    return 0;
  }

  @Override
  public BlockState getBlockAt(int x, int y, int z) {
    if (!isInBounds(x, y, z)) {
      throw new IllegalArgumentException("Chunk coordinates out of bound: (" +
                                         x + ", " + y + ", " + z + ")");
    }

    RegionChunkSection section = sections[y >>> 4];
    if (section != null) {
      return section.getBlockAt(x, y & 0b1111, z);
    }
    return BlockState.DEFAULT;
  }

  @Override
  public BlockState setBlockAt(int x, int y, int z, BlockState state) {
    if (!isInBounds(x, y, z)) {
      throw new IllegalArgumentException("Chunk coordinates out of bound: (" +
                                         x + ", " + y + ", " + z + ")");
    }

    int sectionHeight = y >>> 4;
    RegionChunkSection section = sections[sectionHeight];
    if (section == null) {
      // Create the section if it doesn't exist yet.
      section = sections[sectionHeight] = new RegionChunkSection();
    }
    return section.setBlockAt(x, y & 0b1111, z, state);
  }

  @Override
  public boolean isEmpty() {
    for (RegionChunkSection section : sections) {
      if ((section != null && !section.isEmpty())) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void serialize(OozeDataOutputStream out) throws IOException {
    out.writeVarInt(dataVersion);

    // TODO: Write biome list.

    BitSet nonEmptySections = new BitSet(sections.length);
    for (int i = 0; i < sections.length; i++) {
      RegionChunkSection section = sections[i];
      if (section != null && !section.isEmpty()) {
        nonEmptySections.set(i);
      }
    }

    out.write(nonEmptySections.toByteArray());

    if (!nonEmptySections.isEmpty()) {
      BlockPalette globalPalette = new BlockPalette();
      List<UnpaddedIntArray> sectionStorage = new ArrayList<>();

      for (int i = 0; i < nonEmptySections.length(); i++) {
        if (nonEmptySections.get(i)) {
          RegionChunkSection section = sections[i];

          // Merge the section's palette into the chunk's, and update the section's block data
          // accordingly.
          UnpaddedIntArray storage = section.getStorage().toUnpadded();
          globalPalette.addAll(section.getPalette()).upgrade(storage);
          sectionStorage.add(storage);
        }
      }

      // Write chunk palette.
      out.writeVarInt(globalPalette.size());
      for (BlockState state : globalPalette) {
        // Least sig bit indicates whether or not the state has properties.
        // 7 most sig bits are the length of the state's name.
        String name = state.getName().toString();
        int length = name.length() << 1;

        boolean hasProperties = state.hasProperties();
        if (hasProperties) {
          length |= 1;
        }

        out.write(length);
        out.writeBytes(name);
        if (hasProperties) {
          out.writeNBT(state.getProperties(), true);
        }
      }

      // Write blocks.
      for (UnpaddedIntArray storage : sectionStorage) {
        out.write(storage);
      }
    }
  }
}
