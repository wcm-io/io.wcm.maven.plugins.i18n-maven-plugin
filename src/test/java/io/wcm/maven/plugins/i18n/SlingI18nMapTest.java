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

import static io.wcm.maven.plugins.i18n.FileUtil.getStringFromClasspath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

import org.custommonkey.xmlunit.XMLAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

class SlingI18nMapTest {

  private SlingI18nMap underTest;

  @BeforeEach
  void setUp() {
    underTest = new SlingI18nMap("en", Map.of(
        "key1", "value1",
        "key2.key21.key211", "value2",
        "key3 with special chars äöüß€", "value3",
        "key4", "value4 äöüß€"));
  }

  @Test
  void testGetI18nJsonString() throws Exception {
    JSONAssert.assertEquals(getStringFromClasspath("map/i18n-content.json"), underTest.getI18nJsonString(), true);
  }

  @Test
  void testGetI18nJsonPropertiesString() throws Exception {
    JSONAssert.assertEquals(getStringFromClasspath("map/i18n-properties.json"), underTest.getI18nJsonPropertiesString(), true);
  }

  @Test
  void testGetI18nXmlString() throws Exception {
    XMLAssert.assertXMLEqual(getStringFromClasspath("map/i18n-content.xml"), underTest.getI18nXmlString());
  }

  @Test
  void testGetI18nPropertiesString() throws Exception {
    Properties props = new Properties();
    //Note: as the files in file-system are encoded as utf-8, we need to set it manually to override the properties default
    try (InputStream is = FileUtil.class.getClassLoader().getResourceAsStream("map/i18n-content.properties")) {
      props.load(new InputStreamReader(is, StandardCharsets.UTF_8));
    }

    Properties underTestProperties = new Properties();
    underTestProperties.load(new StringReader(underTest.getI18nPropertiesString()));

    assertEquals(props.keySet().size(), underTestProperties.keySet().size());
    for (Map.Entry<Object, Object> entry : props.entrySet()) {
      assertTrue(underTestProperties.containsKey(entry.getKey()));
      assertEquals(entry.getValue(), underTestProperties.getProperty((String)entry.getKey()));
    }
  }

}
