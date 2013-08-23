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

import java.io.IOException;
import java.util.List;

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
   * Base directory for poperties files.
   * 
   * @parameter default-value="${basedir}/src/main/resources"
   */
  private String baseDir;

  /**
   * The pattern a generated enum field name must match to be valid.
   * 
   * @parameter default-value="^[A-Z][A-Z0-9]*[A-Z0-9_]*$"
   */
  private String enumFieldPattern;

  /**
   * String format for enum field's javadoc. Two strings are given: the first one is the property key, the second one
   * the property value.
   * 
   * @parameter default-value="Key &quot;%1$s&quot; for property with value &quot;%2$s&quot;."
   */
  private String enumJavadoc;

  /**
   * List of files to process. Relative to <code>baseDir</code>. The path will be used as package name, if
   * <code>packageName</code> is not set.
   * 
   * @parameter
   */
  private List<String> files;

  /**
   * The directory to create the files in.
   * 
   * @parameter default-value="${basedir}/target/generated-sources/enum"
   */
  private String generateDirectory;

  /**
   * Fully qualified name of an interface to implement. This allows to make multiple generated enum types an
   * implementation of the same interface. If given, the interface has to define {@code key()}.
   * 
   * @parameter
   */
  private String implement;

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
   * The prefix of each key. This is the value that will be skipped when generating the enum field name. See also:
   * <code>prefixedOnly</code>.
   * 
   * @parameter default-value=""
   */
  private String prefix;

  /**
   * If <code>true</code>, only keys starting with <code>prefix</code> will be processed.
   * 
   * @parameter default-value=true
   */
  private boolean prefixedOnly;

  /**
   * Reference to the maven project.
   * 
   * @parameter expression="${project}"
   * @required
   * @readonly
   */
  private MavenProject project;

  /**
   * Character encoding of the generated java file.
   * 
   * @parameter default-value="UTF-8"
   */
  private String targetEncoding;

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
    EnumGenerator enumGenerator = new EnumGenerator(baseDir, enumFieldPattern, enumJavadoc, files, generateDirectory,
        implement, lineLength, getLog(), packageName, prefix, prefixedOnly, project, targetEncoding);
    try {
      enumGenerator.generate();
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
   * @return the baseDir
   */
  public String getBaseDir() {
    return baseDir;
  }

  /**
   * @return the enumFieldPattern
   */
  public String getEnumFieldPattern() {
    return enumFieldPattern;
  }

  /**
   * @return the enumJavadoc
   */
  public String getEnumJavadoc() {
    return enumJavadoc;
  }

  /**
   * @return the files
   */
  public List<String> getFiles() {
    return files;
  }

  /**
   * @return the generateDirectory
   */
  public String getGenerateDirectory() {
    return generateDirectory;
  }

  /**
   * @return the implement
   */
  public String getImplement() {
    return implement;
  }

  /**
   * @return the lineLength
   */
  public Integer getLineLength() {
    return lineLength;
  }

  /**
   * @return the packageName
   */
  public String getPackageName() {
    return packageName;
  }

  /**
   * @return the prefix
   */
  public String getPrefix() {
    return prefix;
  }

  /**
   * @return the project
   */
  public MavenProject getProject() {
    return project;
  }

  /**
   * @return the targetEncoding
   */
  public String getTargetEncoding() {
    return targetEncoding;
  }

  /**
   * @return the prefixedOnly
   */
  public boolean isPrefixedOnly() {
    return prefixedOnly;
  }

  /**
   * @param baseDir
   *          the baseDir to set
   */
  public void setBaseDir(final String baseDir) {
    this.baseDir = baseDir;
  }

  /**
   * @param enumFieldPattern
   *          the enumFieldPattern to set
   */
  public void setEnumFieldPattern(final String enumFieldPattern) {
    this.enumFieldPattern = enumFieldPattern;
  }

  /**
   * @param enumJavadoc
   *          the enumJavadoc to set
   */
  public void setEnumJavadoc(final String enumJavadoc) {
    this.enumJavadoc = enumJavadoc;
  }

  /**
   * @param files
   *          the files to set
   */
  public void setFiles(final List<String> files) {
    this.files = files;
  }

  /**
   * @param generateDirectory
   *          the generateDirectory to set
   */
  public void setGenerateDirectory(final String generateDirectory) {
    this.generateDirectory = generateDirectory;
  }

  /**
   * @param implement
   *          the implement to set
   */
  public void setImplement(final String implement) {
    this.implement = implement;
  }

  /**
   * @param lineLength
   *          the lineLength to set
   */
  public void setLineLength(final Integer lineLength) {
    this.lineLength = lineLength;
  }

  /**
   * @param packageName
   *          the packageName to set
   */
  public void setPackageName(final String packageName) {
    this.packageName = packageName;
  }

  /**
   * @param prefix
   *          the prefix to set
   */
  public void setPrefix(final String prefix) {
    this.prefix = prefix;
  }

  /**
   * @param prefixedOnly
   *          the prefixedOnly to set
   */
  public void setPrefixedOnly(final boolean prefixedOnly) {
    this.prefixedOnly = prefixedOnly;
  }

  /**
   * @param project
   *          the project to set
   */
  public void setProject(final MavenProject project) {
    this.project = project;
  }

  /**
   * @param targetEncoding
   *          the targetEncoding to set
   */
  public void setTargetEncoding(final String targetEncoding) {
    this.targetEncoding = targetEncoding;
  }
}
