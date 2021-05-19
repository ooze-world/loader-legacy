package me.nullicorn.ooze.storage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import lombok.Getter;
import me.nullicorn.ooze.world.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * A set of {@link BlockState block states} that exist within a volume of Minecraft blocks. Each
 * state is identified by a positive integer, and no state or identifier can be used more than once
 * at any given time in the same palette.
 * <p>
 * When used in conjunction with an {@link me.nullicorn.ooze.serialize.IntArray integer array} or
 * similar structure, it provides a compact means of storing states for large volumes of blocks.
 *
 * @author Nullicorn
 */
public class BlockPalette implements Iterable<BlockState> {

  private final List<BlockState> registeredStates;

  /**
   * The block state that volumes using this palette can fall-back to if a block's state is not
   * specified. This state's ID can be quickly accessed via {@link #getDefaultStateId()}.
   */
  @Getter
  private final BlockState defaultState;

  /**
   * The ID of the palette's {@link #getDefaultState() default state}.
   */
  @Getter
  private final int defaultStateId;

  public BlockPalette() {
    this(BlockState.DEFAULT);
  }

  public BlockPalette(BlockState defaultState) {
    this.registeredStates = new ArrayList<>();
    this.defaultState = defaultState;
    defaultStateId = 0;

    registeredStates.add(defaultState);
  }

  /**
   * @return The block state associated with that ID, or null if none exists in this palette.
   * @see #getStateId(BlockState)
   */
  public BlockState getState(int stateId) {
    return registeredStates.get(stateId);
  }

  /**
   * @return The integer used to identify the {@code state} in the palette, or {@code -1} if the
   * palette does not contain that state.
   * @see #getOrAddStateId(BlockState)
   */
  public int getStateId(BlockState state) {
    return registeredStates.indexOf(state);
  }

  /**
   * Same as {@link #getStateId(BlockState)}, but the {@code state} is automatically added if the
   * palette does not already contain it. If that happens, the returned value is the new identifier
   * created for the state.
   *
   * @return The integer used to identify the {@code state} in the palette.
   */
  public int getOrAddStateId(BlockState state) {
    int stateId = registeredStates.indexOf(state);
    if (stateId < 0) {
      registeredStates.add(state);
      stateId = registeredStates.indexOf(state);
    }
    return stateId;
  }


  /**
   * Same as {@link #removeState(int)}, but {@code stateId} is determined automatically based on the
   * provided {@code state}.
   *
   * @throws IllegalArgumentException If the requested state is the {@link #getDefaultState()
   *                                  default state} for the palette.
   * @see #removeState(int)
   */
  public PaletteUpgrader removeState(BlockState state) {
    int idToRemove = getStateId(state);
    if (idToRemove < 0) {
      return PaletteUpgrader.dummy;
    }
    return removeState(idToRemove);
  }

  /**
   * Removes a state from the palette if it was already present. This operation may alter the IDs of
   * other states, so the returned upgrader should be used to update dependent data accordingly.
   * This method cannot be used to remove the palette's {@link #defaultState default state}.
   *
   * @return A tool for applying the changes to any data that uses this palette.
   * @throws IllegalArgumentException If the requested state is the {@link #getDefaultState()
   *                                  default state} for the palette.
   */
  public PaletteUpgrader removeState(int stateId) {
    if (stateId == defaultStateId) {
      throw new IllegalStateException("Cannot remove the default state from a palette: " +
                                      defaultState);
    } else if (stateId < 0 || stateId >= registeredStates.size()) {
      // State does not exist.
      return PaletteUpgrader.dummy;
    }

    registeredStates.remove(stateId);
    if (stateId == registeredStates.size()) {
      // If the last element is removed, there is nothing to shift.
      return PaletteUpgrader.dummy;
    }

    // Shift any IDs after the removed ID down by 1.
    PaletteUpgrader upgrader = new PaletteUpgrader(registeredStates.size() - stateId);
    for (int id = stateId; id < registeredStates.size(); id++) {
      upgrader.registerChange(id + 1, id);
    }
    upgrader.lock();
    return upgrader;
  }

  /**
   * Adds all block states from another palette into this one if they are not already present.
   *
   * @return A tool for upgrading any data dependent on the {@code otherPalette} to be compatible
   * with this one.
   */
  public PaletteUpgrader addAll(BlockPalette otherPalette) {
    final PaletteUpgrader upgrader = new PaletteUpgrader(otherPalette.size());

    for (int oldId = 0; oldId < otherPalette.registeredStates.size(); oldId++) {
      BlockState state = otherPalette.getState(oldId);
      int newId = getOrAddStateId(state);
      upgrader.registerChange(oldId, newId);
    }
    upgrader.lock();

    return upgrader;
  }

  /**
   * @return The number of unique block states stored in the palette.
   */
  public int size() {
    return registeredStates.size();
  }

  @NotNull
  @Override
  public Iterator<BlockState> iterator() {
    return registeredStates.iterator();
  }

  @Override
  public String toString() {
    if (registeredStates.isEmpty()) {
      return "{}";
    }

    StringBuilder b = new StringBuilder();
    b.append('{');
    for (int i = 0; i < registeredStates.size(); i++) {
      b.append(i).append(": ").append(registeredStates.get(i));
      if (i < registeredStates.size() - 1) {
        b.append(", ");
      }
    }
    b.append('}');
    return b.toString();
  }
}
