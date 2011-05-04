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

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;

/**
 * Test cases for {@link EnumGenerator}.
 * 
 * @author Michel Jung &lt;michel_jung@hotmail.com&gt;
 */
public class EnumGeneratorMojoTest extends AbstractMojoTestCase {

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

}
