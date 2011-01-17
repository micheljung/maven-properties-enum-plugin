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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michel Jung &lt;michel_jung@hotmail.com&gt;
 */
public class EnumGeneratorMojoTest extends AbstractMojoTestCase {

  /**
   * Logger.
   */
  private static final Logger logger = LoggerFactory.getLogger(EnumGeneratorMojoTest.class);

  private static final String TMP_DIR = System.getProperty("java.io.tmpdir");

  /**
   * The {@link EnumGeneratorMojo} to test.
   */
  private EnumGeneratorMojo enumGeneratorMojo;

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
   * The property file's package path.
   */
  private final String packagePath = packageName.replace('.', '/');

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
    enumGeneratorMojo = new EnumGeneratorMojo();
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

  /**
   * Test method for
   * {@link com.google.code.maven.propertiesenumplugin.EnumGeneratorMojo#buildEnumFieldName(java.lang.String)}.
   * 
   * @throws InvalidPropertyKeyException
   *           if a property key was invalid
   */
  public void testBuildEnumFieldName() throws InvalidPropertyKeyException {
    assertEquals("KEY", EnumGeneratorMojo.buildEnumFieldName("key"));
    assertEquals("MY_KEY", EnumGeneratorMojo.buildEnumFieldName("myKey"));
    assertEquals("MY_LONG_KEY", EnumGeneratorMojo.buildEnumFieldName("myLongKey"));
    assertEquals("COM_EXAMPLE_KEY", EnumGeneratorMojo.buildEnumFieldName("com.example.key"));
    assertEquals("COM_EXAMPLE_MY_LONG_KEY", EnumGeneratorMojo.buildEnumFieldName("com.example.myLongKey"));
    assertEquals("KEY_WITH_SPACES", EnumGeneratorMojo.buildEnumFieldName("key with spaces"));
    assertEquals("KEY_WITH_UNDERSCORES", EnumGeneratorMojo.buildEnumFieldName("key_with_underscores"));
    assertEquals("KEY_WITH_DASHES", EnumGeneratorMojo.buildEnumFieldName("key-with-dashes"));
    try {
      EnumGeneratorMojo.buildEnumFieldName("dollar$key");
      fail("invalid key was valid");
    } catch (InvalidPropertyKeyException e) {
      // good
    }
    try {
      EnumGeneratorMojo.buildEnumFieldName("dollar$key");
      fail("invalid key was valid");
    } catch (InvalidPropertyKeyException e) {
      // good
    }
    try {
      EnumGeneratorMojo.buildEnumFieldName("plus+key");
      fail("invalid key was valid");
    } catch (InvalidPropertyKeyException e) {
      // good
    }
  }

  /**
   * Test method for
   * {@link com.google.code.maven.propertiesenumplugin.EnumGeneratorMojo#buildEnumTypeName(java.io.File)}.
   */

  public void testBuildEnumTypeName() {
    assertEquals(ENUM_TYPE_NAME, EnumGeneratorMojo.buildEnumTypeName(targetFile));
  }

  /**
   * Test method for
   * {@link com.google.code.maven.propertiesenumplugin.EnumGeneratorMojo#buildJavadoc(String, String, int)}.
   */
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
  public void testBuildPackageName() {
    EnumGeneratorMojo.buildPackageName(propertiesFile, baseDir);
  }

  /**
   * Test method for
   * {@link com.google.code.maven.propertiesenumplugin.EnumGeneratorMojo#buildTargetFile(File, String, String)}.
   */
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
  public void testCreateDirectories() throws IOException {
    File dir = new File(targetDirectoryPath);
    EnumGeneratorMojo.createDirectories(dir);
    assertTrue(dir.exists());
  }

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
   * Test method for {@link com.google.code.maven.propertiesenumplugin.EnumGeneratorMojo#execute()}.
   * 
   * @throws Exception
   *           if an exception occurred
   */
  public void testExecute() throws Exception {
    File pluginXml = new File(getBasedir(), "src/test/resources/plugin-config.xml");
    EnumGeneratorMojo mojo = (EnumGeneratorMojo) lookupMojo("generate", pluginXml);
    assertNotNull(mojo);

    mojo.execute();

    File expectedFile = new File("src/test/resources/com/google/code/maven/propertiesenumplugin/ExpectedResult.java");
    File actualFile = new File("target/generated-sources/com/google/code/maven/propertiesenumplugin/MyProperties.java");
    assertTrue("File with expected result could not be found: " + expectedFile, expectedFile.exists());
    assertTrue("Expected, generated file could not be found: " + actualFile, actualFile.exists());

    assertTrue("Content of file " + actualFile + " does not match content of file " + expectedFile,
            FileUtils.contentEquals(expectedFile, actualFile));
  }

  /**
   * Test method for
   * {@link com.google.code.maven.propertiesenumplugin.EnumGeneratorMojo#wordWrap(java.lang.String, int)}.
   */
  public void testWordWrap() {
    Map<String, List<String>> strings = new HashMap<String, List<String>>();
    strings.put("String to wrap", Arrays.asList(new String[] {"String to wrap"}));
    strings.put("String\nwith\nnew\nlines", Arrays.asList(new String[] {"String", "with", "new", "lines"}));
    strings.put(
            "ThisIsAStringWithoutSpacesAndBreaksThisIsAStringWithoutSpacesAndBreaksThisIsAStringWithoutSpacesAndBreaks",
            Arrays.asList(new String[] {"ThisIsAStringWithoutSpacesAndBreaksThisIsAStringWithoutSpacesAndBreaksThisIsAStringWithoutSpacesAndBreaks"}));
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
