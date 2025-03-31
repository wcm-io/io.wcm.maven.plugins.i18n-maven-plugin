/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2014 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.maven.plugins.i18n;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Build;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

import io.wcm.maven.plugins.i18n.readers.I18nReader;
import io.wcm.maven.plugins.i18n.readers.JsonI18nReader;
import io.wcm.maven.plugins.i18n.readers.PropertiesI18nReader;
import io.wcm.maven.plugins.i18n.readers.XmlI18nReader;

/**
 * Transform i18n resources in Java Properties, JSON or XML file format to Sling i18n Messages JSON or XML format.
 */
@Mojo(name = "transform", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresProject = true, threadSafe = true)
public class TransformMojo extends AbstractMojo {

  // file extensions
  private static final String FILE_EXTENSION_JSON = "json";
  private static final String FILE_EXTENSION_XML = "xml";
  private static final String FILE_EXTENSION_PROPERTIES = "properties";

  private static final String ALL_FILES = "**/*.";
  private static final String[] SOURCE_FILES_INCLUDES = new String[] {
      ALL_FILES + FILE_EXTENSION_PROPERTIES,
      ALL_FILES + FILE_EXTENSION_XML,
      ALL_FILES + FILE_EXTENSION_JSON
  };

  /**
   * Source path containing the i18n source .properties or .xml files.
   */
  @Parameter(defaultValue = "${basedir}/src/main/resources/i18n")
  private String source;

  /**
   * Relative target path for the generated resources.
   */
  @Parameter(defaultValue = "SLING-INF/app-root/i18n")
  private String target;

  /**
   * Output format. Possible values:
   * <ul>
   * <li><code>JSON</code>: Sling Message format serialized as JSON.</li>
   * <li><code>JSON_PROPERTIES</code>: Flat list of key/value pairs in JSON format.</li>
   * <li><code>XML</code>: Sling Message format serialized as JCR XML.</li>
   * <li><code>PROPERTIES</code>: Flat list of key/value pairs in Java Properties format.</li>
   * </ul>
   */
  @Parameter(defaultValue = "JSON")
  private String outputFormat;

  @Parameter(defaultValue = "generated-i18n-resources")
  private String generatedResourcesFolderPath;

  @Parameter(property = "project", required = true, readonly = true)
  private MavenProject project;

  @Component
  private BuildContext buildContext;

  private File generatedResourcesFolder;
  private List<File> i18nSourceFiles;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    OutputFormat selectedOutputFormat = OutputFormat.valueOf(StringUtils.upperCase(outputFormat));
    try {
      File sourceDirectory = getSourceDirectory();
      intialize(sourceDirectory);

      // skip incremental build if no i18n source file was changed
      if (buildContext.isIncremental() && !isI18nSourceFileChanged(sourceDirectory)) {
        return;
      }

      List<File> sourceFiles = getI18nSourceFiles(sourceDirectory);
      for (File file : sourceFiles) {
        transformFile(file, selectedOutputFormat);
      }
    }
    catch (IOException ex) {
      throw new MojoFailureException("Failure to transform i18n resources", ex);
    }
  }

  private void transformFile(File file, OutputFormat selectedOutputFormat) throws MojoFailureException {
    try {
      // transform i18n files
      String languageKey = FileUtils.removeExtension(file.getName());
      I18nReader reader = getI18nReader(file);
      SlingI18nMap i18nMap = new SlingI18nMap(languageKey, reader.read(file));

      // write mappings to target file
      File targetFile = getTargetFile(file, selectedOutputFormat);
      writeTargetI18nFile(i18nMap, targetFile, selectedOutputFormat);

      getLog().info("Transformed " + file.getPath() + " to  " + targetFile.getPath());
    }
    catch (IOException ex) {
      throw new MojoFailureException("Unable to transform i18n resource: " + file.getPath(), ex);
    }
  }

  /**
   * Checks if and i18n source file was changes in incremental build.
   * @param sourceDirectory Source directory
   * @return true if changes detected
   */
  private boolean isI18nSourceFileChanged(File sourceDirectory) {
    Scanner scanner = buildContext.newScanner(sourceDirectory);
    Scanner deleteScanner = buildContext.newDeleteScanner(sourceDirectory);
    return isI18nSourceFileChanged(scanner) || isI18nSourceFileChanged(deleteScanner);
  }

  private boolean isI18nSourceFileChanged(Scanner scanner) {
    scanner.setIncludes(SOURCE_FILES_INCLUDES);
    scanner.addDefaultExcludes();
    scanner.scan();
    return scanner.getIncludedFiles().length > 0;
  }

  /**
   * Initialize parameters, which cannot get defaults from annotations. Currently only the root nodes.
   * @throws IOException I/O exception
   */
  private void intialize(File sourceDirectory) throws IOException {
    getLog().debug("Initializing i18n plugin...");

    // resource
    if (!getI18nSourceFiles(sourceDirectory).isEmpty()) {
      File myGeneratedResourcesFolder = getGeneratedResourcesFolder();
      addResource(myGeneratedResourcesFolder.getPath(), target);
    }

  }

  private void addResource(String generatedResourcesDirectory, String targetPath) {

    // construct resource
    Resource resource = new Resource();
    resource.setDirectory(generatedResourcesDirectory);
    resource.setTargetPath(targetPath);

    // add to build
    Build build = this.project.getBuild();
    build.addResource(resource);
    getLog().debug("Added resource: " + resource.getDirectory() + " -> " + resource.getTargetPath());
  }

  /**
   * Fetches i18n source files from source directory.
   * @param sourceDirectory Source directory
   * @return a list of XML files
   */
  private List<File> getI18nSourceFiles(File sourceDirectory) throws IOException {

    if (i18nSourceFiles == null) {
      if (!sourceDirectory.isDirectory()) {
        i18nSourceFiles = Collections.emptyList();
      }
      else {
        // get list of source files
        String includes = StringUtils.join(SOURCE_FILES_INCLUDES, ",");
        String excludes = FileUtils.getDefaultExcludesAsString();

        i18nSourceFiles = FileUtils.getFiles(sourceDirectory, includes, excludes);
      }
    }

    return i18nSourceFiles;
  }

  /**
   * Get directory containing source i18n files.
   * @return directory containing source i18n files.
   */
  private File getSourceDirectory() throws IOException {
    File file = new File(source);
    if (!file.isDirectory()) {
      getLog().debug("Could not find directory at '" + source + "'");
    }
    return file.getCanonicalFile();
  }

  /**
   * Writes mappings to file in Sling compatible JSON format.
   * @param i18nMap mappings
   * @param targetfile target file
   * @param selectedOutputFormat Output format
   */
  private void writeTargetI18nFile(SlingI18nMap i18nMap, File targetfile, OutputFormat selectedOutputFormat) throws IOException {
    switch (selectedOutputFormat) {
      case XML:
        FileUtils.fileWrite(targetfile, StandardCharsets.UTF_8.name(), i18nMap.getI18nXmlString());
        break;
      case PROPERTIES:
        FileUtils.fileWrite(targetfile, StandardCharsets.ISO_8859_1.name(), i18nMap.getI18nPropertiesString());
        break;
      case JSON:
        FileUtils.fileWrite(targetfile, StandardCharsets.UTF_8.name(), i18nMap.getI18nJsonString());
        break;
      case JSON_PROPERTIES:
        FileUtils.fileWrite(targetfile, StandardCharsets.UTF_8.name(), i18nMap.getI18nJsonPropertiesString());
        break;
      default:
        throw new IllegalArgumentException("Unsupported ouptut format: " + selectedOutputFormat);

    }
    buildContext.refresh(targetfile);
  }

  /**
   * Get the JSON file for source file.
   * @param sourceFile the source file
   * @param selectedOutputFormat Output format
   * @return File with name and path based on file parameter
   */
  private File getTargetFile(File sourceFile, OutputFormat selectedOutputFormat) throws IOException {

    File sourceDirectory = getSourceDirectory();
    String relativePath = StringUtils.substringAfter(sourceFile.getAbsolutePath(), sourceDirectory.getAbsolutePath());
    String relativeTargetPath = FileUtils.removeExtension(relativePath) + "." + selectedOutputFormat.getFileExtension();

    File jsonFile = new File(getGeneratedResourcesFolder().getPath() + relativeTargetPath);

    jsonFile = jsonFile.getCanonicalFile();

    File parentDirectory = jsonFile.getParentFile();
    if (!parentDirectory.exists()) {
      if (!parentDirectory.mkdirs()) {
        throw new IOException("Unable to create directory: " + parentDirectory.getPath());
      }
      buildContext.refresh(parentDirectory);
    }

    return jsonFile;
  }

  private File getGeneratedResourcesFolder() throws IOException {
    if (generatedResourcesFolder == null) {
      generatedResourcesFolder = new File(this.project.getBuild().getDirectory(), generatedResourcesFolderPath);
      if (!generatedResourcesFolder.exists()) {
        if (!generatedResourcesFolder.mkdirs()) {
          throw new IOException("Unable to create directory: " + generatedResourcesFolder.getPath());
        }
        buildContext.refresh(generatedResourcesFolder);
      }
    }
    return generatedResourcesFolder;
  }

  /**
   * Get i18n reader for source file.
   * @param sourceFile Source file
   * @return I18n reader
   */
  private I18nReader getI18nReader(File sourceFile) throws MojoFailureException {
    String extension = FileUtils.getExtension(sourceFile.getName());
    if (StringUtils.equalsIgnoreCase(extension, FILE_EXTENSION_PROPERTIES)) {
      return new PropertiesI18nReader();
    }
    if (StringUtils.equalsIgnoreCase(extension, FILE_EXTENSION_XML)) {
      return new XmlI18nReader();
    }
    if (StringUtils.equalsIgnoreCase(extension, FILE_EXTENSION_JSON)) {
      return new JsonI18nReader();
    }
    throw new MojoFailureException("Unsupported file extension '" + extension + "': " + sourceFile.getAbsolutePath());
  }

}
