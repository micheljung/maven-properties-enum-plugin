package com.google.code.maven.propertiesenumplugin;

/**
 * Auto generated enum type for property file
 * "/src/test/resources/com/google/code/maven/propertiesenumplugin/prefixedOnly.properties".
 */
public enum PrefixedOnly implements com.example.MyInterface {

  /**
   * Key "com.example.prefix.key2" for property with value "Key 2 with prefix".
   */
  KEY2("com.example.prefix.key2"),

  /**
   * Key "com.example.prefix.key1" for property with value "Key 1 with prefix".
   */
  KEY1("com.example.prefix.key1");

  /**
   * The original key in the property file.
   */
  private final String originalKey;

  /**
   * Constructs a new {@link PrefixedOnly}.
   * @param originalKey
   *          the property's key as it's denoted in the properties file
   */
  PrefixedOnly(String originalKey) {
    this.originalKey = originalKey;
  }

  /**
   * @return the source properties file's base name
   */
  public static final String getResourceBaseName() {
    return "com.google.code.maven.propertiesenumplugin.prefixedOnly";
  }

  /**
   * @return the property key.
   */
  @Override
  public final String toString() {
    return originalKey;
  }
  /**
   * @return the property key.
   */
  @Override
  public final String key() {
    return originalKey;
  }
}
