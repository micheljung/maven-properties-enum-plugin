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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
 * @author <a href="mailto:michel_jung@hotmail.com">Michel Jung</a>
 * @goal generate
 * @phase generate-sources
 */
public class EnumGeneratorMojo extends AbstractMojo {

  /**
   * Code for the toString() method.
   */
  private static final CharSequence TO_STRING_METHOD = "  @Override\n  public final String toString() {\n"
          + "    return originalKey;\n  }\n";

  /**
   * Code for getBaseName() method. Expects the base name as argument.
   */
  private static final String GET_BASE_NAME_METHOD = "  public final String getBaseName() {\n"
          + "    return \"%s\";\n  }\n\n";

  /**
   * Builds the base name for the given file.
   * 
   * @param baseDir
   *          the base directory
   * @param propertiesFile
   *          the properties file
   * @return e.g. "com.example.Property" if <code>baseDir</code> is "src/main/resources" and <code>propertiesFile</code>
   *         is "src/main/resources/com/example/Property.properties"
   */
  static String buildBaseName(final File baseDir, final File propertiesFile) {
    String fileName = propertiesFile.getName();
    String packageName = buildPackageName(propertiesFile, baseDir);
    return packageName + "." + fileName.substring(0, fileName.indexOf('.'));
  }

  /**
   * Builds the enum type name depending on the target file (e.g. "Enum.java" becomes "Enum").
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
   *          a bunch of spaces, used to indent (e.g. two spaces "&nbsp;&nbsp;");
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
   * @return e.g. "com.example" if baseDir is "src/main/resources" and propertiesFile
   *         "src/main/resources/com/example/File.properties"
   */
  static String buildPackageName(final File propertiesFile, final File baseDir) {
    // substring(1) because there would be a leading dot
    String parentDirPath = propertiesFile.getAbsoluteFile().getParent();
    String relativePath = parentDirPath.replace(baseDir.getAbsolutePath(), "");
    return relativePath.replace(File.separatorChar, '.').substring(1);
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
   * @return e.g. "target/generated-source/enum/com/example/Enum.java"
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
   * The pattern a generated enum field name must match to be valid.
   * 
   * @parameter default-value="^[A-Z][A-Z0-9]*[A-Z0-9_]*$"
   */
  private String enumFieldPattern;

  /**
   * Fully qualified name of an interface to implement. This allows to make multiple generated enum types an
   * implementation of the same interface.
   * 
   * @parameter
   */
  private String implement;

  /**
   * List of files to process. Relative to <code>baseDir</code>. The path will be used as package name, if
   * <code>packageName</code> is not set.
   * 
   * @parameter
   */
  private List<String> files;

  /**
   * Base directory for poperties files.
   * 
   * @parameter default-value="${basedir}/src/main/resources"
   */
  private String baseDir;

  /**
   * The directory to create the files in.
   * 
   * @parameter default-value="${basedir}/target/generated-sources/enum"
   */
  private String generateDirectory;

  /**
   * The maximum line length to use when creating the source file.
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
   * String format for enum field's javadoc. Two strings are given: the first one is the property key, the second one
   * the property value.
   * 
   * @parameter default-value="Key &quot;%1$s&quot; for property with value &quot;%2$s&quot;."
   */
  private String enumJavadoc;

  /**
   * Character encoding of the generated java file.
   * 
   * @parameter default-value="UTF-8"
   */
  private String targetEncoding;

  /**
   * Reference to the maven project.
   * 
   * @parameter expression="${project}"
   * @required
   * @readonly
   */
  private MavenProject project;

  /**
   * A map of enum field names that have already been declared. Key is the value the enum field name, value the property
   * key.
   */
  private final Map<String, String> generatedEnumFieldNames;

  /**
   * The prefix of each key. This is the value that will be skipped when generating the enum field name.
   * 
   * @parameter default-value=""
   */
  private String prefix;

  /**
   * Constructs a new {@link EnumGeneratorMojo}.
   */
  public EnumGeneratorMojo() {
    generatedEnumFieldNames = new HashMap<String, String>();
  }

  /**
   * Builds an enumeration field's name, based on the property key. Converts camelCase to CAMEL_CASE and package.names
   * to PACKAGE_NAMES. Note that if the key starts with {@link #prefix}, this prefix will be skipped when building the
   * enum field name.
   * 
   * @param propertyKey
   *          the property's key
   * @return the enum field name
   * @throws InvalidPropertyKeyException
   *           if the property key is invalid, so that the generated enum field name does not match the pattern
   *           {@link #enumFieldPattern}
   */
  String buildEnumFieldName(final String propertyKey) throws InvalidPropertyKeyException {
    String fieldName = propertyKey;
    if (propertyKey.startsWith(prefix)) {
      fieldName = propertyKey.substring(prefix.length());
    }
    fieldName = fieldName.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
    fieldName = fieldName.replaceAll("([A-Z])[\\.\\s-]([A-Z])", "$1_$2");
    if (!fieldName.matches(enumFieldPattern)) {
      throw new InvalidPropertyKeyException("The key \"" + propertyKey
              + "\" is invalid. The resulting enum must match the pattern " + enumFieldPattern);
    }
    return fieldName;
  }

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
      getLog().error(e);
      throw new MojoExecutionException(e.getMessage(), e);
    } catch (InvalidPropertyKeyException e) {
      getLog().error(e);
      throw new MojoFailureException(e.getMessage(), e);
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
   * @throws InvalidPropertyKeyException
   *           if a property key is invalid, so that the generated enum field name does not match the pattern
   *           {@link #enumFieldPattern}
   */
  void generateEnumFile(final File propertiesFile) throws IOException, InvalidPropertyKeyException {
    if (packageName == null) {
      packageName = buildPackageName(propertiesFile, new File(baseDir));
    }
    if (!Charset.isSupported(targetEncoding)) {
      throw new UnsupportedEncodingException("The target charset " + targetEncoding + " is not supported");
    }

    File targetFile = buildTargetFile(propertiesFile, packageName, generateDirectory);

    // Create package directory
    createDirectories(targetFile.getParentFile());

    /*
     * I don't yet understand why targetEncoding is also needed for input stream (and not ISO-8859-1), but that was the
     * only way that worked.
     */
    Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(propertiesFile), targetEncoding));
    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetFile), targetEncoding));

    try {
      Properties properties = new Properties();
      properties.load(reader);

      writePackageDeclaration(writer, packageName);
      writeEnumTypeJavadoc(writer, propertiesFile);
      writeEnumTypeSignature(writer, buildEnumTypeName(targetFile));

      Iterator<Entry<Object, Object>> iterator = properties.entrySet().iterator();
      while (iterator.hasNext()) {
        Entry<Object, Object> entry = iterator.next();
        writeEnumField(entry.getKey().toString(), entry.getValue().toString(), writer, !iterator.hasNext());
      }
      writeOriginalKeyField(writer);
      writeConstructor(writer, buildEnumTypeName(targetFile));
      writeGetBaseNameMethod(writer, propertiesFile);
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
   * @throws InvalidPropertyKeyException
   *           if the property key is invalid, so that the generated enum field name does not match the pattern
   *           {@link #enumFieldPattern}
   */
  void writeEnumField(final String key, final String value, final Writer writer, final boolean isLast)
          throws IOException, InvalidPropertyKeyException {
    String enumFieldName = buildEnumFieldName(key);

    if (generatedEnumFieldNames.containsKey(enumFieldName)) {
      throw new DuplicateEnumFieldException("Duplicate enum field name. Both, '" + key + "' and '"
              + generatedEnumFieldNames.get(enumFieldName) + "' result in '" + enumFieldName + "'");
    }
    generatedEnumFieldNames.put(enumFieldName, key);

    String description = String.format(enumJavadoc, key, value);
    String javadoc = buildJavadoc(description, "  ", lineLength);

    writer.append(javadoc);
    writer.append("  ");
    writer.append(enumFieldName);
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
   * @param propertiesFile
   *          the source property file
   * @throws IOException
   *           if an I/O error occurred
   */
  void writeEnumTypeJavadoc(final Writer writer, final File propertiesFile) throws IOException {
    StringBuilder builder = new StringBuilder("Auto generated enum type for property file ");
    builder.append("\"");
    // As it's javadoc, we want to have / in the path
    String projectBaseDir = project.getBasedir().getAbsolutePath();
    builder.append(propertiesFile.getAbsolutePath().replace(projectBaseDir, "").replace(File.separatorChar, '/'));
    builder.append("\".");
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
    if (implement != null) {
      writer.append(" implements ");
      writer.append(implement);
    }
    writer.append(" {\n\n");
  }

  /**
   * Writes the method to get the base name (the properties file).
   * 
   * @param writer
   *          the Writer to use
   * @param propertiesFile
   *          the current properties file, needed to create the base name from
   * @throws IOException
   *           if an I/O error occurred
   */
  void writeGetBaseNameMethod(final Writer writer, final File propertiesFile) throws IOException {
    String baseName = buildBaseName(new File(baseDir), propertiesFile);
    String javadoc = buildJavadoc("@return the source properties file's base name", "  ", lineLength);
    writer.append(javadoc);
    writer.append(String.format(GET_BASE_NAME_METHOD, baseName));
  }

  /**
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
    String javadoc = buildJavadoc("@return the original property key.", "  ", lineLength);
    writer.append(javadoc);
    writer.append(TO_STRING_METHOD);
  }
}
