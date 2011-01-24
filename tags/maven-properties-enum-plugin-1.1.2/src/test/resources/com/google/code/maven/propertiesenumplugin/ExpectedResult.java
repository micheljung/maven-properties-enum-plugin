package com.google.code.maven.propertiesenumplugin;

/**
 * Auto generated enum type for property file
 * "/com/google/code/maven/propertiesenumplugin/myProperties.properties".
 */
public enum MyProperties implements com.example.MyInterface {

  /**
   * Key "aQuietLongKey" for property with value "This is a quiet long key".
   */
  A_QUIET_LONG_KEY("aQuietLongKey"),

  /**
   * Key "value1" for property with value "Value 1".
   */
  VALUE1("value1"),

  /**
   * Key "com.example.myDottedKey" for property with value "My dotted Key".
   */
  COM_EXAMPLE_MY_DOTTED_KEY("com.example.myDottedKey"),

  /**
   * Key "myKey" for property with value "My Key".
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

  /**
   * @return the original property key.
   */
  @Override
  public String toString() {
    return originalKey;
  }
}
