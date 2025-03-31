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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import jakarta.json.Json;
import jakarta.json.JsonObject;

class JsonUtilTest {

  static final String JSON_STRING = "{\"key1\":\"value1\",\"key2\":55}";

  @Test
  void testToString() throws Exception {
    JsonObject jsonObject = Json.createObjectBuilder()
        .add("key1", "value1")
        .add("key2", 55)
        .build();
    JSONAssert.assertEquals(JSON_STRING, JsonUtil.toString(jsonObject), true);
  }

  @Test
  void testFromString() throws Exception {
    JsonObject jsonObject = JsonUtil.fromString(JSON_STRING);
    JSONAssert.assertEquals(JSON_STRING, JsonUtil.toString(jsonObject), true);
  }

  @Test
  void testFromString_WithComment() throws Exception {
    JsonObject jsonObject = JsonUtil.fromString("/* Comment before */\n" + JSON_STRING);
    JSONAssert.assertEquals(JSON_STRING, JsonUtil.toString(jsonObject), true);
  }

  @Test
  void testFromString_Invalid() {
    assertThrows(IOException.class, () -> {
      JsonUtil.fromString("{...");
    });
  }

}
