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
import java.io.FileNotFoundException;
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

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 * FIXME javadoc
 * 
 * @author <a href="mailto:michel_jung@hotmail.com">Michel Jung</a>
 */
public class EnumGenerator {

  /**
   * Code for getBaseName() method. Expects the base name as argument.
   */
  private static final String GET_BASE_NAME_METHOD = "  public final String getBaseName() {\n"
      + "    return \"%s\";\n  }\n\n";

  /**
   * Code for the toString() method.
   */
  private static final CharSequence TO_STRING_METHOD = "  @Override\n  public final String toString() {\n"
      + "    return originalKey;\n  }\n";

  /**
   * Base directory for poperties files.
   */
  private String baseDir;

  /**
   * The pattern a generated enum field name must match to be valid.
   */
  private final String enumFieldPattern;

  /**
   * String format for enum field's javadoc. Two strings are given: the first one is the property key, the second one
   * the property value.
   */
  private final String enumJavadoc;

  /**
   * List of files to process. Relative to <code>baseDir</code>. The path will be used as package name, if
   * <code>packageName</code> is not set.
   */
  private final List<String> files;

  /**
   * A map of enum field names that have already been declared. Key is the value the enum field name, value the property
   * key.
   */
  private final Map<String, String> generatedEnumFieldNames;

  /**
   * The directory to create the files in.
   */
  private String generateDirectory;

  /**
   * Fully qualified name of an interface to implement. This allows to make multiple generated enum types an
   * implementation of the same interface.
   */
  private final String implement;

  /**
   * The maximum line length to use when creating the source file.
   */
  private final Integer lineLength;

  /**
   * Logger.
   */
  private final Log logger;

  /**
   * Name of the target package. If not given, the package name will be same as the property's path, relative to
   * <code>baseDir</code>.
   */
  private String packageName;

  /**
   * The prefix of each key. This is the value that will be skipped when generating the enum field name. See also:
   * <code>prefixedOnly</code>.
   */
  private String prefix;

  /**
   * If <code>true</code>, only keys starting with <code>prefix</code> will be processed.
   */
  private final boolean prefixedOnly;

  /**
   * Character encoding of the generated java file.
   */
  private final String targetEncoding;

  /**
   * Reference to the maven project.
   */
  private final MavenProject project;

  /**
   * Constructs a new {@link EnumGenerator}.
   * 
   * @param baseDir
   * @param enumFieldPattern
   * @param enumJavadoc
   * @param files
   * @param generateDirectory
   * @param implement
   * @param lineLength
   * @param logger
   * @param packageName
   * @param prefix
   * @param prefixedOnly
   * @param project
   * @param targetEncoding
   */
  public EnumGenerator(final String baseDir, final String enumFieldPattern, final String enumJavadoc,
      final List<String> files, final String generateDirectory, final String implement, final Integer lineLength,
      final Log logger, final String packageName, final String prefix, final boolean prefixedOnly,
      final MavenProject project, final String targetEncoding) {
    generatedEnumFieldNames = new HashMap<String, String>();

    this.baseDir = baseDir;
    this.enumFieldPattern = enumFieldPattern;
    this.enumJavadoc = enumJavadoc;
    this.files = files;
    this.generateDirectory = generateDirectory;
    this.implement = implement;
    this.lineLength = lineLength;
    this.logger = logger;
    this.packageName = packageName;
    this.prefix = prefix;

    this.prefixedOnly = prefixedOnly;
    this.project = project;
    this.targetEncoding = targetEncoding;
  }

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
  String buildBaseName(final File baseDir, final File propertiesFile) {
    String fileName = propertiesFile.getName();
    String packageName = buildPackageName(propertiesFile, baseDir);
    return packageName + "." + fileName.substring(0, fileName.indexOf('.'));
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
    if (prefix != null) {
      String prefixWithPoint = prefix + ".";
      if (propertyKey.startsWith(prefixWithPoint)) {
        fieldName = propertyKey.substring(prefixWithPoint.length());
      }
    }
    fieldName = fieldName.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase();
    fieldName = fieldName.replaceAll("([A-Z0-9])[\\.\\s-]([A-Z0-9])", "$1_$2");
    if (!fieldName.matches(enumFieldPattern)) {
      throw new InvalidPropertyKeyException("The key \"" + propertyKey
          + "\" is invalid. The resulting enum must match the pattern " + enumFieldPattern + " but was: "
          + fieldName);
    }
    return fieldName;
  }

  /**
   * Builds the enum type name depending on the target file (e.g. "Enum.java" becomes "Enum").
   * 
   * @param targetFile
   *          the enum target file
   * @return the enum type name
   */
  String buildEnumTypeName(final File targetFile) {
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
  String buildJavadoc(final String description, final String indent, final int lineLength) {
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
  String buildPackageName(final File propertiesFile, final File baseDir) {
    // substring(1) because there would be a leading dot
    String parentDirPath = propertiesFile.getAbsoluteFile().getParent();
    String relativePath = parentDirPath.replace(baseDir.getAbsolutePath(), "");
    if (relativePath.length() > 1) {
      return relativePath.replace(File.separatorChar, '.').substring(1);
    }

    return "";
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
  File buildTargetFile(final File propertiesFile, final String packageName, final String targetDirectory) {
    String propertyFileName = propertiesFile.getName();
    char[] charArray = propertyFileName.substring(0, propertyFileName.lastIndexOf('.')).toCharArray();
    charArray[0] = Character.toUpperCase(charArray[0]);
    String packageDirectory = packageName.replace('.', File.separatorChar);
    return new File(targetDirectory + File.separator + packageDirectory, new String(charArray) + ".java");
  }

  /**
   * Create a directory and all its parent directories.
   * 
   * @param directory
   *          the directory to create
   * @throws IOException
   *           if a directory could not be created
   */
  void createDirectories(final File directory) throws IOException {
    if (!directory.exists() && !directory.mkdirs()) {
      throw new IOException("Could not create directory: " + directory.getAbsolutePath());
    }
  }

  /**
   * Removes all properties that won't be written to the enum, as {@link #prefixedOnly} is set and the key does not
   * start with {@link #prefix}.
   * 
   * @param properties
   *          the properties to filter
   */
  void filterProperties(final Properties properties) {
    Iterator<Entry<Object, Object>> iterator = properties.entrySet().iterator();
    while (iterator.hasNext()) {
      Entry<Object, Object> entry = iterator.next();
      String key = entry.getKey().toString();

      if (prefixedOnly && !key.startsWith(prefix)) {
        iterator.remove();
      }
    }
  }

  /**
   * @throws InvalidPropertyKeyException
   * @throws IOException
   */
  public void generate() throws IOException, InvalidPropertyKeyException {
    prepare();
    for (String fileName : files) {
      File sourceFile = new File(baseDir, fileName);
      if (!sourceFile.exists()) {
        throw new FileNotFoundException("The file " + sourceFile.getAbsolutePath() + " could not be found");
      }
      generateEnumFile(sourceFile);
    }
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

      String enumTypeName = buildEnumTypeName(targetFile);
      writeEnumTypeSignature(writer, enumTypeName);

      filterProperties(properties);

      writeEnumFields(writer, properties, enumTypeName);
      writeOriginalKeyField(writer);
      writeConstructor(writer, buildEnumTypeName(targetFile));
      writeGetBaseNameMethod(writer, propertiesFile);
      writeToStringMethod(writer);
      writer.write("}\n");
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        logger.error("Could not close reader");
      }
      try {
        writer.close();
      } catch (IOException e) {
        logger.error("Could not close writer");
      }
    }
  }

  /**
   * 
   */
  private void prepare() {

    // Normalize directories
    baseDir = new File(baseDir).getAbsolutePath();
    generateDirectory = new File(generateDirectory).getAbsolutePath();

    if (prefix == null) {
      prefix = "";
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
  List<String> wordWrap(final String string, final int length) {
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
   * @param enumTypeName
   *          the name of the target enum type
   * @throws IOException
   *           if an I/O error occurred
   * @throws InvalidPropertyKeyException
   *           if the property key is invalid, so that the generated enum field name does not match the pattern
   *           {@link #enumFieldPattern}
   */
  void writeEnumField(final String key, final String value, final Writer writer, final boolean isLast,
      String enumTypeName)
      throws IOException, InvalidPropertyKeyException {
    String enumFieldName = buildEnumFieldName(key);

    String fieldIdentifier = String.format("%s.%s", enumTypeName, enumFieldName);

    if (generatedEnumFieldNames.containsKey(fieldIdentifier)) {
      throw new DuplicateEnumFieldException("Duplicate enum field name. Both, '" + key + "' and '"
          + generatedEnumFieldNames.get(fieldIdentifier) + "' result in '" + enumFieldName + "'");
    }
    generatedEnumFieldNames.put(fieldIdentifier, key);

    String description = String.format(enumJavadoc, key, value);
    String javadoc = buildJavadoc(description, "  ", lineLength);

    writer.append(javadoc);
    writer.append("  ");
    writer.append(enumFieldName);
    writer.append("(\"");
    writer.append(key);
    writer.append("\")");

    if (!isLast) {
      writer.append(",\n\n");
    }
  }

  /**
   * Writes the enum fields to the given writer.
   * 
   * @param writer
   *          the {@link Writer} to use
   * @param properties
   *          the properties to generate enum fields for
   * @param enumTypeName
   *          the name of the target enum type
   * @throws IOException
   *           if an I/O error occurred
   * @throws InvalidPropertyKeyException
   *           if a property key is invalid, so that the generated enum field name does not match the pattern
   *           {@link #enumFieldPattern}
   */
  private void writeEnumFields(final Writer writer, final Properties properties, String enumTypeName)
      throws IOException,
      InvalidPropertyKeyException {
    Iterator<Entry<Object, Object>> iterator = properties.entrySet().iterator();
    while (iterator.hasNext()) {
      Entry<Object, Object> entry = iterator.next();
      String key = entry.getKey().toString();

      writeEnumField(key, entry.getValue().toString(), writer, !iterator.hasNext(), enumTypeName);
    }
    writer.append(";\n\n");
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
    if (packageName != null && !packageName.isEmpty()) {
      writer.append("package ");
      writer.append(packageName);
      writer.append(";\n\n");
    }
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
