package me.nullicorn.ooze.storage;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import me.nullicorn.ooze.serialize.IntArray;
import me.nullicorn.ooze.serialize.OozeDataOutputStream;
import me.nullicorn.ooze.serialize.OozeSerializable;

/**
 * An integer array that internally packs values as close as possible to maintain low footprint
 * in-memory and when serialized. Loosely based on Minecraft's block storage format.
 *
 * @author Nullicorn
 */
public class UnpaddedIntArray implements IntArray, OozeSerializable {

  private final byte[] data;

  // Number of "cells" in the array.
  private final int size;

  /*
   * The highest value that can be stored in any cell. This value is not a necessarily a technical
   * limitation, but since it must be provided by the user, we also check against it in #set() to
   * avoid confusion.
   */
  private final int maxValue;

  // The length of each cell in bits.
  private final int bitsPerCell;

  // A mask of [bitsPerCell] set bits.
  private final int cellMask;

  public UnpaddedIntArray(int size, int maxValue) {
    this.size = size;
    this.maxValue = maxValue;
    this.bitsPerCell = Math.max(1, Integer.SIZE - Integer.numberOfLeadingZeros(maxValue));
    this.cellMask = (1 << bitsPerCell) - 1;

    int bytesNeeded = (int) Math.ceil(size * bitsPerCell / (double) Byte.SIZE);
    this.data = new byte[bytesNeeded];
  }

  @Override
  public int get(int index) {
    if (index < 0 || index >= size) {
      throw new ArrayIndexOutOfBoundsException(index);
    }

    int value = 0;
    int bitIndex = index * bitsPerCell;
    int bitOffset = bitIndex % Byte.SIZE;
    int byteIndex = bitIndex / Byte.SIZE;
    int valueMask = cellMask;
    int totalBitsRead = 0;

    while (valueMask != 0) {
      value |= (((data[byteIndex] & 0xFF) >> bitOffset) & valueMask) << totalBitsRead;

      int bitsRead = Math.min(Integer.bitCount(valueMask), Byte.SIZE - bitOffset);
      totalBitsRead += bitsRead;
      valueMask >>>= bitsRead;
      byteIndex++;
      bitOffset = 0;
    }

    return value;
  }

  @Override
  public void set(int index, int value) {
    if (index < 0 || index >= size) {
      throw new ArrayIndexOutOfBoundsException(index);
    } else if (value < 0) {
      throw new IllegalArgumentException("Value is not a positive integer: " + value);
    } else if (value > maxValue) {
      throw new IllegalArgumentException("Value is not <= " + maxValue + ": " + value);
    }

    int bitIndex = index * bitsPerCell;
    int bitOffset = bitIndex % Byte.SIZE;
    int byteIndex = bitIndex / Byte.SIZE;
    int valueMask = cellMask;

    while (valueMask != 0) {
      data[byteIndex] &= ~(valueMask << bitOffset); // Clear all bits in the cell.
      data[byteIndex] |= ((value & valueMask) << bitOffset); // Insert the value into the cell.

      int bitsWritten = Math.min(Integer.bitCount(valueMask), Byte.SIZE - bitOffset);
      value >>>= bitsWritten;
      valueMask >>>= bitsWritten;
      byteIndex++;
      bitOffset = 0;
    }
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public int maxValue() {
    return maxValue;
  }

  @Override
  public void forEach(DataConsumer action) {
    for (int cellIndex = 0; cellIndex < size; cellIndex++) {
      action.accept(cellIndex, get(cellIndex));
    }
  }

  @Override
  public void serialize(OozeDataOutputStream out) throws IOException {
    out.writeVarInt(maxValue);
    out.write(data);
  }

  @Override
  public String toString() {
    if (size == 0) {
      return "[]";
    }

    StringBuilder b = new StringBuilder();
    b.append('[');

    forEach((index, value) -> {
      b.append(value);
      if (index < size - 1) {
        b.append(", ");
      }
    });

    b.append(']');
    return b.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UnpaddedIntArray that = (UnpaddedIntArray) o;
    return size == that.size &&
           maxValue == that.maxValue &&
           Arrays.equals(data, that.data);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(size, maxValue);
    result = 31 * result + Arrays.hashCode(data);
    return result;
  }
}