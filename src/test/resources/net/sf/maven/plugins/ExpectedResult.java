package net.sf.maven.plugins;

/**
 * Auto generated enum type for property file
 * &quot;C:\Users\Downlord\workspace\maven-properties-enum-plugin\src\test\resources\myProperties.properties&quot;
 */
public enum MyProperties {

  /**
   * Key "aQuietLongKey" for property with value "This is a quiet long key"
   */
  A_QUIET_LONG_KEY("aQuietLongKey"),

  /**
   * Key "value1" for property with value "Value 1"
   */
  VALUE1("value1"),

  /**
   * Key "myKey" for property with value "My Key"
   */
  MY_KEY("myKey");

  /**
   * The original key in the property file.
   */
  private final String originalKey;

  /**
   * Constructs a new {@link MyProperties}.
   * @param originalKey
   *          the property's key as it's denoted in the properties file
   */
  MyProperties(String originalKey) {
    this.originalKey = originalKey;
  }

  @Override
  public String toString() {
    return originalKey;
  }
}
