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
package io.wcm.maven.plugins.i18n.readers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import io.wcm.maven.plugins.i18n.JsonUtil;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

/**
 * Reads i18n resources from JSON files.
 */
public class JsonI18nReader implements I18nReader {

  @Override
  public Map<String, String> read(File sourceFile) throws IOException {

    String fileContent = IOUtils.toString(sourceFile.toURI().toURL(), StandardCharsets.UTF_8);
    try {
      JsonObject root = JsonUtil.fromString(fileContent);
      Map<String, String> map = new HashMap<>();
      parseJson(root, map, "");
      return map;
    }
    catch (IOException ex) {
      throw new IOException("Unable to read JSON from " + sourceFile.getAbsolutePath(), ex);
    }
  }

  private void parseJson(JsonObject node, Map<String, String> map, String prefix) throws IOException {
    for (Map.Entry<String, JsonValue> entry : node.entrySet()) {
      String key = entry.getKey();
      JsonValue value = entry.getValue();
      switch (value.getValueType()) {
        case OBJECT:
          parseJson(value.asJsonObject(), map, prefix + key + ".");
          break;
        case STRING:
          map.put(prefix + key, node.getString(key));
          break;
        default:
          throw new IOException("Unsupported JSON value: " + node.getValueType());
      }
    }
  }

}
