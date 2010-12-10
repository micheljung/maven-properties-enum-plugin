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
package net.sf.maven.plugins.propertyenum;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * This Mojo generates a Java enum based on the keys of a properties file.
 * 
 * @author Michel Jung &lt;michel_jung@hotmail.com&gt;
 * @goal generate
 * @phase generate-sources
 */
public class EnumGeneratorMojo extends AbstractMojo {

  /**
   * Code for the toString() method
   */
  private static final CharSequence TO_STRING_METHOD = "  @Override\n  public String toString() {\n"
          + "    return originalKey;\n  }\n";

  /**
   * Builds an enumeration field's name, based on the property key. Converts camelCase to CAMEL_CASE and package.names
   * to PACKAGE_NAMES
   * 
   * @param propertyKey
   *          the property's key
   * @return the enum field name
   */
  static String buildEnumFieldName(final String propertyKey) {
    String fieldName = propertyKey.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
    fieldName = fieldName.replaceAll("([A-Z])\\.([A-Z])", "$1_$2");
    return fieldName;
  }

  /**
   * Builds the enum type name depending on the target file (e.g. &quot;Enum.java&quot; becomes &quot;Enum&quot;)
   * 
   * @param targetFile
   *          the enum target file
   * @return the enum type name
   */
  static String buildEnumTypeName(final File targetFile) {
    String fileName = targetFile.getName();
    return fileName.substring(0, fileName.lastIndexOf('.'));
  }

  /**
   * Builds a javadoc with the given description and indentation.
   * 
   * @param description
   *          the javadoc description
   * @param indent
   *          a bunch of spaces, used to indent (e.g. two spaces &quot;&nbsp;&nbsp;&quot);
   * @param lineLength
   *          the maximum line length
   * @return the builded javadoc string
   */
  static String buildJavadoc(final String description, final String indent, final int lineLength) {
    String javaDocPrefix = String.format("%s * ", indent);

    StringBuilder builder = new StringBuilder();
    builder.append(indent);
    builder.append("/**");

    for (String string : wordWrap(description, lineLength - javaDocPrefix.length())) {
      builder.append('\n');
      builder.append(javaDocPrefix);
      builder.append(string);
    }
    builder.append('\n');
    builder.append(indent);
    builder.append(" */\n");
    return builder.toString();
  }

  /**
   * Builds the package name based on the properties file path, starting from {@link #baseDir}.
   * 
   * @param propertiesFile
   *          the properties file
   * @param baseDir
   *          the base directory path to cut off
   * @return e.g. &quot;com.example&quot; if baseDir is &quot;src/main/resources&quot; and propertiesFile
   *         &quot;src/main/resources/com/example/File.properties&quot;
   */
  static String buildPackageName(final File propertiesFile, final String baseDir) {
    // substring(1) because there would be a leading dot
    return propertiesFile.getAbsoluteFile().getParent().replace(baseDir, "").replace(File.separatorChar, '.')
            .substring(1);
  }

  /**
   * Builds the target file using generateDirectory, packageName and the source properties file's name.
   * 
   * @param propertiesFile
   *          the properties file name
   * @param packageName
   *          the target file's package name
   * @param targetDirectory
   *          the directory where the files will be generated in
   * @return e.g. &quot;target/generated-source/enum/com/example/Enum.java&quot;
   */
  static File buildTargetFile(final File propertiesFile, final String packageName, final String targetDirectory) {
    String propertyFileName = propertiesFile.getName();
    char[] charArray = propertyFileName.substring(0, propertyFileName.lastIndexOf('.')).toCharArray();
    charArray[0] = Character.toUpperCase(charArray[0]);
    String packageDirectory = packageName.replace('.', File.separatorChar);
    return new File(targetDirectory + File.separator + packageDirectory + File.separator + new String(charArray)
            + ".java");
  }

  /**
   * Create a directory and all its parent directories.
   * 
   * @param directory
   *          the directory to create
   * @throws IOException
   *           if a directory could not be created
   */
  static void createDirectories(final File directory) throws IOException {
    if (!directory.exists() && !directory.mkdirs()) {
      throw new IOException("Could not create directory: " + directory.getAbsolutePath());
    }
  }

  /**
   * Word wrapping. Fails if a string is longer than <code>length</code> characters (I guess this string won't be
   * returned then).
   * 
   * @param string
   *          the string to wrap
   * @param length
   *          the maximum line length
   * @return wrapped lines
   */
  static List<String> wordWrap(final String string, final int length) {
    Pattern pattern = Pattern.compile("(.{1," + length + "})(?:[\\s]|$)|([\\S]{" + length + ",})");
    Matcher m = pattern.matcher(string);

    List<String> list = new LinkedList<String>();
    while (m.find()) {
      String group1 = m.group(1);
      String group2 = m.group(2);
      if (group1 != null) {
        list.add(group1);
      } else {
        list.add(group2);
      }
    }

    return list;
  }

  /**
   * List of files to process. Relative to <code>baseDir</code>.
   * 
   * @parameter
   */
  private List<String> files;

  /**
   * Base directory for poperties files. Default is "${basedir}/src/main/resources".
   * 
   * @parameter default-value="${basedir}/src/main/resources"
   */
  private String baseDir;

  /**
   * The directory to create the file in. Default is "${basedir}/target/generated-sources/enum".
   * 
   * @parameter default-value="${basedir}/target/generated-sources/enum"
   */
  private String generateDirectory;

  /**
   * The maximum line length. Default is 80.
   * 
   * @parameter default-value="80"
   */
  private Integer lineLength;

  /**
   * Name of the target package. If not given, the package name will be same as the property's path, relative to
   * <code>baseDir</code>.
   * 
   * @parameter
   */
  private String packageName;

  /**
   * String format for enum javadoc. Two strings are given: the first one is the property key, the second one the
   * property value.
   * 
   * @parameter default-value="Key &quot;%1$s&quot; for property with value &quot;%2$s&quot;."
   */
  private String enumJavadoc;

  /**
   * Reference to the maven project.
   * 
   * @parameter expression="${project}"
   * @required
   * @readonly
   */
  private MavenProject project;

  /**
   * Generates an enum based on properties file.
   * 
   * @throws MojoExecutionException
   *           if an exception occurred
   * @throws MojoFailureException
   *           of the mojo failed
   */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().debug("baseDir: " + baseDir);
    getLog().debug("generateDirectory: " + generateDirectory);
    // Normalize directories
    baseDir = new File(baseDir).getAbsolutePath();
    generateDirectory = new File(generateDirectory).getAbsolutePath();
    try {
      for (String fileName : files) {
        File sourceFile = new File(baseDir, fileName);
        generateEnumFile(sourceFile);
      }
    } catch (IOException e) {
      throw new MojoExecutionException("An IO Exception occurred", e);
    }

    project.addCompileSourceRoot(generateDirectory);
  }

  /**
   * Generates an enum file based on a properties file.
   * 
   * @param propertiesFile
   *          the properties file read
   * @throws IOException
   *           if an I/O error occurred
   */
  void generateEnumFile(final File propertiesFile) throws IOException {
    if (packageName == null) {
      packageName = buildPackageName(propertiesFile, baseDir);
    }
    File targetFile = buildTargetFile(propertiesFile, packageName, generateDirectory);

    // Create package directory
    createDirectories(targetFile.getParentFile());

    BufferedReader reader = new BufferedReader(new FileReader(propertiesFile));
    Writer writer = new BufferedWriter(new FileWriter(targetFile));

    try {
      Properties properties = new Properties();
      properties.load(reader);

      writePackageDeclaration(writer, packageName);
      writeEnumTypeJavadoc(writer, propertiesFile.getName());
      writeEnumTypeSignature(writer, buildEnumTypeName(targetFile));

      Iterator<Entry<Object, Object>> iterator = properties.entrySet().iterator();
      while (iterator.hasNext()) {
        Entry<Object, Object> entry = iterator.next();
        writeEnumField(entry.getKey().toString(), entry.getValue().toString(), writer, !iterator.hasNext());
      }
      writeOriginalKeyField(writer);
      writeConstructor(writer, buildEnumTypeName(targetFile));
      writeToStringMethod(writer);
      writer.write("}\n");
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        getLog().error("Could not close reader");
      }
      try {
        writer.close();
      } catch (IOException e) {
        getLog().error("Could not close writer");
      }
    }
  }

  /**
   * Writes the enum type's constructor.
   * 
   * @param writer
   *          the Writer to use
   * @param enumTypeName
   *          the enum type's name
   * @throws IOException
   *           if an I/O error occurred
   */
  void writeConstructor(final Writer writer, final String enumTypeName) throws IOException {
    StringBuilder builder = new StringBuilder();
    builder.append("Constructs a new {@link ");
    builder.append(enumTypeName);
    builder.append("}.\n\n@param originalKey\n         the property's key as it's denoted in the properties file");
    String description = builder.toString();
    String javadoc = buildJavadoc(description, "  ", lineLength);
    writer.append(javadoc);
    writer.append("  ");
    writer.append(enumTypeName);
    writer.append("(String originalKey) {\n    this.originalKey = originalKey;\n  }\n\n");
  }

  /**
   * Writes an enum field based on a property key/value. The key will be used to create the field's name, the value will
   * be used in the javadoc.
   * 
   * @param key
   *          the property's key
   * @param value
   *          the property's value
   * @param writer
   *          the Writer to use
   * @param isLast
   *          <code>true</code> if this is the last enum field, <code>false</code> otherwise
   * @throws IOException
   *           if an I/O error occurred
   */
  void writeEnumField(final String key, final String value, final Writer writer, final boolean isLast)
          throws IOException {
    String enumName = buildEnumFieldName(key);

    String description = String.format(enumJavadoc, key, value);
    String javadoc = buildJavadoc(description, "  ", lineLength);

    writer.append(javadoc);
    writer.append("  ");
    writer.append(enumName);
    writer.append("(\"");
    writer.append(key);
    writer.append("\")");

    if (isLast) {
      writer.append(";\n\n");
    } else {
      writer.append(",\n\n");
    }
  }

  /**
   * @param writer
   *          the Writer to use
   * @param propertyFileName
   *          the path to the source property file, relative to baseDir.
   * @throws IOException
   *           if an I/O error occurred
   */
  void writeEnumTypeJavadoc(final Writer writer, final String propertyFileName) throws IOException {
    StringBuilder builder = new StringBuilder("Auto generated enum type for property file ");
    builder.append("&quot;");
    builder.append(baseDir);
    builder.append(File.separatorChar);
    builder.append(propertyFileName);
    builder.append("&quot;");
    String description = builder.toString();

    String javadoc = buildJavadoc(description, "", lineLength);
    writer.append(javadoc);
  }

  /**
   * @param writer
   *          the Writer to use
   * @param name
   *          the enum type's name
   * @throws IOException
   *           if an I/O error occurred
   */
  void writeEnumTypeSignature(final Writer writer, final String name) throws IOException {
    writer.append("public enum ");
    writer.append(name);
    writer.append(" {\n\n");
  }

  /**
   * 
   * @param writer
   *          the Writer to use
   * @throws IOException
   *           if an I/O error occurred
   */
  void writeOriginalKeyField(final Writer writer) throws IOException {
    String javadoc = buildJavadoc("The original key in the property file.", "  ", lineLength);
    writer.append(javadoc);
    writer.append("  private final String originalKey;\n\n");
  }

  /**
   * @param writer
   *          the Writer to use
   * @param packageName
   *          the package name
   * @throws IOException
   *           if an I/O error occurred
   */
  void writePackageDeclaration(final Writer writer, final String packageName) throws IOException {
    writer.append("package ");
    writer.append(packageName);
    writer.append(";\n\n");
  }

  /**
   * @param writer
   *          the Writer to use
   * @throws IOException
   *           if an I/O error occurred
   */
  void writeToStringMethod(final Writer writer) throws IOException {
    String javadoc = buildJavadoc("@return the original property key", "  ", lineLength);
    writer.append(javadoc);
    writer.append(TO_STRING_METHOD);
  }
}
