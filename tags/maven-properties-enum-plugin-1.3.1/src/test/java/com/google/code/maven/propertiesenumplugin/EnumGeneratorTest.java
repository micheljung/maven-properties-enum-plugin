/*
 * Copyright 2010 Michel Jung
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License.You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, softwaredistributed under the License is distributed on an
 * "AS IS" BASIS,WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.See the License for the
 * specific language governing permissions andlimitations under the License.
 */
package com.google.code.maven.propertiesenumplugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test cases for {@link EnumGenerator}.
 * 
 * @author Michel Jung &lt;michel_jung@hotmail.com&gt;
 */
public class EnumGeneratorTest {

  /**
   * Logger.
   */
  private static final Logger logger = LoggerFactory.getLogger(EnumGeneratorTest.class);

  /**
   * System's temporary directory.
   */
  private static final String TMP_DIR = System.getProperty("java.io.tmpdir");

  /**
   * The {@link EnumGenerator} under test.
   */
  private EnumGenerator enumGenerator;

  /**
   * The target java file.
   */
  private File targetFile;

  /**
   * The source property file.
   */
  private File propertiesFile;

  /**
   * The base directory to search the source file in.
   */
  private final String baseDir = TMP_DIR;

  /**
   * The target directory to create the java file in.
   */
  private final String targetDirectoryPath = TMP_DIR;

  /**
   * The target package name.
   */
  private final String packageName = "net.sf.maven.propertyenum";

  /**
   * The name of the enum type that will be created.
   */
  private static final String ENUM_TYPE_NAME = "MyEnum";

  /**
   * The name of the source property file.
   */
  private static final String PROPERTIES_FILE_NAME = "myProperties.properties";

  /**
   * Pattern for valid enum fields.
   */
  private static final String ENUM_FIELD_PATTERN = "^[A-Z][A-Z0-9]*[A-Z0-9_]*$";

  /**
   * Sets up the test case.
   */
  @Before
  public void setUp() {
    enumGenerator = new EnumGenerator(null, ENUM_FIELD_PATTERN, null, null, null, null, null, null, null, null, false,
            null, null);
    targetFile = new File(TMP_DIR + File.separator + ENUM_TYPE_NAME + ".java");
    targetFile.deleteOnExit();

    propertiesFile = new File(PROPERTIES_FILE_NAME);
  }

  /**
   * Code to execute after each test.
   */
  @After
  public void tearDown() {
    if (!targetFile.delete()) {
      logger.warn("Could not delete " + targetFile.getAbsolutePath());
    }
  }

  @Test
  public void testBuildBaseName() {
    File propertiesFile = new File("/foo/bar/com/example/properties.properties");
    File baseDir = new File("/foo/bar");

    String result = enumGenerator.buildBaseName(baseDir, propertiesFile);
    assertEquals("com.example.properties", result);
  }

  /**
   * Test method for
   * {@link com.google.code.maven.propertiesenumplugin.EnumGenerator#buildEnumFieldName(java.lang.String)}.
   * 
   * @throws Exception
   *           if the mojo could not be loaded
   */
  @Test
  public void testBuildEnumFieldName() throws Exception {
    assertEquals("KEY", enumGenerator.buildEnumFieldName("key"));
    assertEquals("MY_KEY", enumGenerator.buildEnumFieldName("myKey"));
    assertEquals("MY_LONG_KEY", enumGenerator.buildEnumFieldName("myLongKey"));
    assertEquals("COM_EXAMPLE_KEY", enumGenerator.buildEnumFieldName("com.example.key"));
    assertEquals("COM_EXAMPLE_MY_LONG_KEY", enumGenerator.buildEnumFieldName("com.example.myLongKey"));
    assertEquals("KEY_WITH_SPACES", enumGenerator.buildEnumFieldName("key with spaces"));
    assertEquals("KEY_WITH_UNDERSCORES", enumGenerator.buildEnumFieldName("key_with_underscores"));
    assertEquals("KEY_WITH_DASHES", enumGenerator.buildEnumFieldName("key-with-dashes"));
    assertEquals("UNDERSCORE_", enumGenerator.buildEnumFieldName("underscore_"));

    try {
      enumGenerator.buildEnumFieldName("dollar$key");
      fail("invalid key was valid");
    } catch (InvalidPropertyKeyException e) {
      // good
    }
    try {
      enumGenerator.buildEnumFieldName("plus+key");
      fail("invalid key was valid");
    } catch (InvalidPropertyKeyException e) {
      // good
    }
  }

  /**
   * Test method for {@link com.google.code.maven.propertiesenumplugin.EnumGenerator#buildEnumTypeName(java.io.File)}.
   */
  @Test
  public void testBuildEnumTypeName() {
    assertEquals(ENUM_TYPE_NAME, enumGenerator.buildEnumTypeName(targetFile));
  }

  /**
   * Test method for {@link com.google.code.maven.propertiesenumplugin.EnumGenerator#buildJavadoc(String, String, int)}.
   */
  @Test
  public void testBuildJavadoc() {
    String expected = "/**\n * This is a javadoc.\n */\n";
    String actual = enumGenerator.buildJavadoc("This is a javadoc.", "", 80);
    assertEquals(expected, actual);

    expected = "  /**\n   * This is an indented javadoc.\n   */\n";
    actual = enumGenerator.buildJavadoc("This is an indented javadoc.", "  ", 80);
    assertEquals(expected, actual);
  }

  /**
   * Test method for {@link com.google.code.maven.propertiesenumplugin.EnumGenerator#buildPackageName(File, String)} .
   */
  @Test
  public void testBuildPackageName() {
    File propertiesFile = new File("/foo/bar/com/example/properties.properties");
    File baseDir = new File("/foo/bar");

    String result = enumGenerator.buildPackageName(propertiesFile, baseDir);
    assertEquals("com.example", result);
  }

  /**
   * Test method for
   * {@link com.google.code.maven.propertiesenumplugin.EnumGenerator#buildTargetFile(File, String, String)}.
   */
  @Test
  public void testBuildTargetFile() {
    enumGenerator.buildTargetFile(propertiesFile, packageName, targetDirectoryPath);
  }

  /**
   * Test method for {@link com.google.code.maven.propertiesenumplugin.EnumGenerator#createDirectories(java.io.File)}.
   * 
   * @throws IOException
   *           if a directory could not be created
   */
  @Test
  public void testCreateDirectories() throws IOException {
    File dir = new File(targetDirectoryPath);
    enumGenerator.createDirectories(dir);
    assertTrue(dir.exists());
  }

  /**
   * Test method for {@link com.google.code.maven.propertiesenumplugin.EnumGenerator#wordWrap(java.lang.String, int)}.
   */
  @Test
  public void testWordWrap() {
    Map<String, List<String>> strings = new HashMap<String, List<String>>();
    strings.put("String to wrap", Arrays.asList(new String[] {"String to wrap"}));
    strings.put("String\nwith\nnew\nlines", Arrays.asList(new String[] {"String", "with", "new", "lines"}));
    strings.put(
            "ThisIsAStringWithoutSpacesAndBreaksThisIsAStringWithoutSpacesAndBreaksThisIsAStringWithoutSpacesAndBreaks",
            Arrays.asList(new String[] {"ThisIsAStringWithoutSpacesAndBreaksThisIsAStringWithoutSpacesAndBreaksThis"
                    + "IsAStringWithoutSpacesAndBreaks"}));
    strings.put(
            "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod "
                    + "tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At "
                    + "vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, "
                    + "no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit am",
            Arrays.asList(new String[] {
                "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod",
                "tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At",
                "vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren,",
                "no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit am"}));
    for (Entry<String, List<String>> entry : strings.entrySet()) {
      String string = entry.getKey();
      List<String> expected = entry.getValue();
      List<String> actual = enumGenerator.wordWrap(string, 80);
      assertEquals(expected, actual);
    }
  }
}
