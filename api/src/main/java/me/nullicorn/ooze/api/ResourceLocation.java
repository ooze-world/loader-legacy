package me.nullicorn.ooze.api;

import java.util.Objects;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

/**
 * A path identified by its namespace. Used by Minecraft for block and item IDs among other things.
 *
 * @author Nullicorn
 */
public final class ResourceLocation {

  // Used when no namespace is provided.
  private static final String DEFAULT_NAMESPACE = "minecraft";

  // Sequence used to separate namespace from path.
  private static final Pattern PATH_INDICATOR = Pattern.compile(":");

  /**
   * Constructs a resource location by parsing the provided <code>value</code>.
   * <ul>
   *   <li>If <code>value</code> is a full resource location, (e.g. "minecraft:stone") then all parts are used.</li>
   *   <li>If <code>value</code> is missing a namespace (e.g. "stone"), then the default namespace "minecraft" is used.</li>
   * </ul>
   *
   * @return The parsed resource location, or {@code null} if the input {@code value} cannot be
   * parsed as a resource location.
   */
  @Nullable
  public static ResourceLocation fromString(@Nullable String value) {
    if (value == null) {
      return null;
    }

    String namespace;
    String path;

    String[] segments = PATH_INDICATOR.split(value, 3);
    switch (segments.length) {
      case 1:
        namespace = DEFAULT_NAMESPACE;
        path = value;
        break;

      case 2:
        namespace = segments[0];
        path = segments[1];
        break;

      default:
        return null;
    }

    return new ResourceLocation(namespace.intern(), path.intern());
  }

  /**
   * @return Whether or not the character is allowed in a resource path.
   */
  private static boolean isValidPathChar(char c) {
    // Paths can contain all the same characters as namespaces, in addition to forward slashes.
    return isValidNamespaceChar(c) || c == '/';
  }

  /**
   * @return Whether or not the character is allowed in a resource namespace.
   */
  private static boolean isValidNamespaceChar(char c) {
    // Namespaces can contain characters a-z (lowercase), 0-9, periods, underscores, and dashes.
    return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '.' || c == '_' || c == '-';
  }

  private final String namespace;
  private final String path;

  /**
   * Same as {@link ResourceLocation#ResourceLocation(String, String)}, but {@code namespace}
   * defaults to "minecraft".
   */
  public ResourceLocation(String path) {
    this(DEFAULT_NAMESPACE, path);
  }

  public ResourceLocation(String namespace, String path) {
    // Only validate namespace if it isn't the default, which should already be valid.
    if (!namespace.equals(DEFAULT_NAMESPACE)) {
      for (int i = 0; i < namespace.length(); i++) {
        if (!isValidNamespaceChar(namespace.charAt(i))) {
          throw new IllegalArgumentException("Invalid namespace: \"" + namespace + "\"");
        }
      }
    }

    for (int i = 0; i < path.length(); i++) {
      if (!isValidPathChar(path.charAt(i))) {
        throw new IllegalArgumentException("Invalid path: \"" + path + "\"");
      }
    }

    this.namespace = namespace;
    this.path = path;
  }

  public String getNamespace() {
    return namespace;
  }

  public String getPath() {
    return path;
  }

  @Override
  public String toString() {
    return namespace + PATH_INDICATOR + path;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResourceLocation that = (ResourceLocation) o;
    return namespace.equals(that.namespace) &&
           path.equals(that.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(namespace, path);
  }
}
