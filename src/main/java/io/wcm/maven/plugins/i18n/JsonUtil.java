/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2023 wcm.io
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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParsingException;

/**
 * Helper methods for JSON handling.
 */
public final class JsonUtil {

  private static final JsonWriterFactory JSON_WRITER_FACTORY = Json.createWriterFactory(Map.of(
      JsonGenerator.PRETTY_PRINTING, true));

  private static final JsonReaderFactory JSON_READER_FACTORY = Json.createReaderFactory(Map.of(
      "org.apache.johnzon.supports-comments", true));

  private JsonUtil() {
    // static methods only
  }

  /**
   * Serialize JSON object to String.
   * @param jsonObject JSON object
   * @return String
   */
  public static String toString(JsonObject jsonObject) {
    StringWriter writer = new StringWriter();
    try (JsonWriter jsonWriter = JSON_WRITER_FACTORY.createWriter(writer)) {
      jsonWriter.write(jsonObject);
      return writer.toString();
    }
  }

  /**
   * Parse JSON string to JSON object.
   * @param jsonString JSON string
   * @return JSON object
   * @throws IOException if JSON parsing fails
   */
  public static JsonObject fromString(String jsonString) throws IOException {
    StringReader reader = new StringReader(jsonString);
    try (JsonReader jsonReader = JSON_READER_FACTORY.createReader(reader)) {
      return jsonReader.readObject();
    }
    catch (JsonParsingException ex) {
      throw new IOException(ex.getMessage(), ex);
    }
  }

}
