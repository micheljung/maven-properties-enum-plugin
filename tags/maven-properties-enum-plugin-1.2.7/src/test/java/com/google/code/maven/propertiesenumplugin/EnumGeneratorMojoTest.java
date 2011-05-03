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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test cases for {@link EnumGeneratorMojo}.
 * 
 * @author Michel Jung &lt;michel_jung@hotmail.com&gt;
 */
public class EnumGeneratorMojoTest extends AbstractMojoTestCase {

  /**
   * Logger.
   */
  private static final Logger logger = LoggerFactory.getLogger(EnumGeneratorMojoTest.class);

  /**
   * System's temporary directory.
   */
  private static final String TMP_DIR = System.getProperty("java.io.tmpdir");

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
   * Code to execute before each test.
   */
  @Override
  public void setUp() throws Exception {
    targetFile = new File(TMP_DIR + File.separator + ENUM_TYPE_NAME + ".java");
    targetFile.deleteOnExit();

    propertiesFile = new File(PROPERTIES_FILE_NAME);

    // required for mojo lookups to work
    super.setUp();
  }

  /**
   * Code to execute after each test.
   */
  @Override
  public void tearDown() {
    if (!targetFile.delete()) {
      logger.warn("Could not delete " + targetFile.getAbsolutePath());
    }
  }

  @Test
  public void testBuildBaseName() {
    File propertiesFile = new File("/foo/bar/com/example/properties.properties");
    File baseDir = new File("/foo/bar");

    String result = EnumGeneratorMojo.buildBaseName(baseDir, propertiesFile);
    assertEquals("com.example.properties", result);
  }

  /**
   * Test method for
   * {@link com.google.code.maven.propertiesenumplugin.EnumGeneratorMojo#buildEnumFieldName(java.lang.String)}.
   * 
   * @throws Exception
   *           if the mojo could not be loaded
   */
  @Test
  public void testBuildEnumFieldName() throws Exception {
    File pluginXml = new File(getBasedir(), "src/test/resources/utf8-test-config.xml");
    EnumGeneratorMojo mojo = (EnumGeneratorMojo) lookupMojo("generate", pluginXml);
    assertNotNull(mojo);

    assertEquals("KEY", mojo.buildEnumFieldName("key"));
    assertEquals("MY_KEY", mojo.buildEnumFieldName("myKey"));
    assertEquals("MY_LONG_KEY", mojo.buildEnumFieldName("myLongKey"));
    assertEquals("COM_EXAMPLE_KEY", mojo.buildEnumFieldName("com.example.key"));
    assertEquals("COM_EXAMPLE_MY_LONG_KEY", mojo.buildEnumFieldName("com.example.myLongKey"));
    assertEquals("KEY_WITH_SPACES", mojo.buildEnumFieldName("key with spaces"));
    assertEquals("KEY_WITH_UNDERSCORES", mojo.buildEnumFieldName("key_with_underscores"));
    assertEquals("KEY_WITH_DASHES", mojo.buildEnumFieldName("key-with-dashes"));
    assertEquals("UNDERSCORE_", mojo.buildEnumFieldName("underscore_"));
    try {
      mojo.buildEnumFieldName("dollar$key");
      fail("invalid key was valid");
    } catch (InvalidPropertyKeyException e) {
      // good
    }
    try {
      mojo.buildEnumFieldName("dollar$key");
      fail("invalid key was valid");
    } catch (InvalidPropertyKeyException e) {
      // good
    }
    try {
      mojo.buildEnumFieldName("plus+key");
      fail("invalid key was valid");
    } catch (InvalidPropertyKeyException e) {
      // good
    }
  }

  /**
   * Test method for
   * {@link com.google.code.maven.propertiesenumplugin.EnumGeneratorMojo#buildEnumTypeName(java.io.File)}.
   */
  @Test
  public void testBuildEnumTypeName() {
    assertEquals(ENUM_TYPE_NAME, EnumGeneratorMojo.buildEnumTypeName(targetFile));
  }

  /**
   * Test method for
   * {@link com.google.code.maven.propertiesenumplugin.EnumGeneratorMojo#buildJavadoc(String, String, int)}.
   */
  @Test
  public void testBuildJavadoc() {
    String expected = "/**\n * This is a javadoc.\n */\n";
    String actual = EnumGeneratorMojo.buildJavadoc("This is a javadoc.", "", 80);
    assertEquals(expected, actual);

    expected = "  /**\n   * This is an indented javadoc.\n   */\n";
    actual = EnumGeneratorMojo.buildJavadoc("This is an indented javadoc.", "  ", 80);
    assertEquals(expected, actual);
  }

  /**
   * Test method for {@link com.google.code.maven.propertiesenumplugin.EnumGeneratorMojo#buildPackageName(File, String)}
   * .
   */
  @Test
  public void testBuildPackageName() {
    File propertiesFile = new File("/foo/bar/com/example/properties.properties");
    File baseDir = new File("/foo/bar");

    String result = EnumGeneratorMojo.buildPackageName(propertiesFile, baseDir);
    assertEquals("com.example", result);
  }

  /**
   * Test method for
   * {@link com.google.code.maven.propertiesenumplugin.EnumGeneratorMojo#buildTargetFile(File, String, String)}.
   */
  @Test
  public void testBuildTargetFile() {
    EnumGeneratorMojo.buildTargetFile(propertiesFile, packageName, targetDirectoryPath);
  }

  /**
   * Test method for
   * {@link com.google.code.maven.propertiesenumplugin.EnumGeneratorMojo#createDirectories(java.io.File)}.
   * 
   * @throws IOException
   *           if a directory could not be created
   */
  @Test
  public void testCreateDirectories() throws IOException {
    File dir = new File(targetDirectoryPath);
    EnumGeneratorMojo.createDirectories(dir);
    assertTrue(dir.exists());
  }

  /**
   * Tests whether duplicated keys are detected.
   * 
   * @throws Exception
   *           if the mojo could not be executed
   */
  @Test
  public void testDuplicateFieldDetection() throws Exception {
    File pluginXml = new File(getBasedir(), "src/test/resources/duplicate-enum-plugin-config.xml");
    EnumGeneratorMojo mojo = (EnumGeneratorMojo) lookupMojo("generate", pluginXml);
    assertNotNull(mojo);

    try {
      mojo.execute();
      fail("Duplicated enum field was not detected");
    } catch (DuplicateEnumFieldException e) {
      // good!
    }
  }

  /**
   * Tests whether ISO-8859-1 encoding works fine.
   * 
   * @throws Exception
   *           if an exception occurred
   */
  @Test
  public void testIso88591() throws Exception {
    File pluginXml = new File(getBasedir(), "src/test/resources/iso88591-test-config.xml");
    EnumGeneratorMojo mojo = (EnumGeneratorMojo) lookupMojo("generate", pluginXml);
    assertNotNull(mojo);

    mojo.execute();

    File expectedFile = new File(
            "src/test/resources/com/google/code/maven/propertiesenumplugin/MyIso88591Properties.java");
    File actualFile = new File(
            "target/generated-sources/com/google/code/maven/propertiesenumplugin/MyIso88591Properties.java");
    assertTrue("File with expected result could not be found: " + expectedFile, expectedFile.exists());
    assertTrue("Expected, generated file could not be found: " + actualFile, actualFile.exists());

    assertTrue("Content of file " + actualFile + " does not match content of file " + expectedFile,
            FileUtils.contentEquals(expectedFile, actualFile));
  }

  /**
   * Tests whether prefixedOnly works fine.
   * 
   * @throws Exception
   *           if an exception occurred
   */
  @Test
  public void testPrefixedOnly() throws Exception {
    File pluginXml = new File(getBasedir(), "src/test/resources/prefixed-only-test-config.xml");
    EnumGeneratorMojo mojo = (EnumGeneratorMojo) lookupMojo("generate", pluginXml);
    assertNotNull(mojo);

    mojo.execute();

    File expectedFile = new File("src/test/resources/com/google/code/maven/propertiesenumplugin/PrefixedOnly.java");
    File actualFile = new File("target/generated-sources/com/google/code/maven/propertiesenumplugin/PrefixedOnly.java");
    assertTrue("File with expected result could not be found: " + expectedFile, expectedFile.exists());
    assertTrue("Expected, generated file could not be found: " + actualFile, actualFile.exists());

    assertTrue("Content of file " + actualFile + " does not match content of file " + expectedFile,
            FileUtils.contentEquals(expectedFile, actualFile));
  }

  /**
   * Tests whether UTF-8 encoding works fine.
   * 
   * @throws Exception
   *           if an exception occurred
   */
  @Test
  public void testUtf8() throws Exception {
    File pluginXml = new File(getBasedir(), "src/test/resources/utf8-test-config.xml");
    EnumGeneratorMojo mojo = (EnumGeneratorMojo) lookupMojo("generate", pluginXml);
    assertNotNull(mojo);

    mojo.execute();

    File expectedFile = new File("src/test/resources/com/google/code/maven/propertiesenumplugin/MyUtf8Properties.java");
    File actualFile = new File(
            "target/generated-sources/com/google/code/maven/propertiesenumplugin/MyUtf8Properties.java");
    assertTrue("File with expected result could not be found: " + expectedFile, expectedFile.exists());
    assertTrue("Expected, generated file could not be found: " + actualFile, actualFile.exists());

    assertTrue("Content of file " + actualFile + " does not match content of file " + expectedFile,
            FileUtils.contentEquals(expectedFile, actualFile));
  }

  /**
   * Test method for
   * {@link com.google.code.maven.propertiesenumplugin.EnumGeneratorMojo#wordWrap(java.lang.String, int)}.
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
      List<String> actual = EnumGeneratorMojo.wordWrap(string, 80);
      assertEquals(expected, actual);
    }
  }
}
