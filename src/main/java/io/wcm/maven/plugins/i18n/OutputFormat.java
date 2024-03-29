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

/**
 * Output format for Sling i18n message file.
 */
enum OutputFormat {

  /**
   * JCR JSON
   */
  JSON("json"),

  /**
   * Flat list of properties in JSON format.
   */
  JSON_PROPERTIES("json"),

  /**
   * JCR XML
   */
  XML("xml"),

  /**
   * PROPERTIES
   */
  PROPERTIES("properties");

  private final String fileExtension;

  OutputFormat(String fileExtension) {
    this.fileExtension = fileExtension;
  }

  /**
   * @return File extension
   */
  public String getFileExtension() {
    return this.fileExtension;
  }

}
