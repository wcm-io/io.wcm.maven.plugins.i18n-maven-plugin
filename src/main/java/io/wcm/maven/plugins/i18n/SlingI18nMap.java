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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * Helper class integrating i18n JSON generation into a sorted map.
 */
class SlingI18nMap {

  private static final String JCR_LANGUAGE = "language";
  private static final List<String> JCR_MIX_LANGUAGE = Collections.singletonList("mix:language");
  private static final String JCR_MIXIN_TYPES = "mixinTypes";
  private static final String JCR_NODETYPE_FOLDER = "nt:folder";
  private static final String JCR_PRIMARY_TYPE = "primaryType";

  private static final String SLING_KEY = "key";
  private static final String SLING_MESSAGE = "message";
  private static final List<String> SLING_MESSAGE_MIXIN_TYPE = Collections.singletonList("sling:Message");

  private static final Namespace NAMESPACE_SLING = Namespace.getNamespace("sling", "http://sling.apache.org/jcr/sling/1.0");
  private static final Namespace NAMESPACE_JCR = Namespace.getNamespace("jcr", "http://www.jcp.org/jcr/1.0");
  private static final Namespace NAMESPACE_MIX = Namespace.getNamespace("mix", "http://www.jcp.org/jcr/mix/1.0");
  private static final Namespace NAMESPACE_NT = Namespace.getNamespace("nt", "http://www.jcp.org/jcr/nt/1.0");

  private final String languageKey;
  private final SortedMap<String, String> properties;

  /**
   * @param languageKey Language key
   */
  SlingI18nMap(String languageKey, Map<String, String> properties) {
    this.languageKey = languageKey;
    this.properties = new TreeMap<>(properties);
  }

  /**
   * Build i18n resource JSON in Sling i18n Message format.
   * @return JSON
   */
  public String getI18nJsonString() {
    return JsonUtil.toString(buildI18nJson());
  }

  /**
   * Build i18n resource JSON in Sling i18n Message format.
   * @return JSON
   */
  public String getI18nJsonPropertiesString() {
    return JsonUtil.toString(buildI18nJsonProperties());
  }

  private JsonObject buildI18nJson() {

    // get root
    JsonObjectBuilder jsonDocument = getMixLanguageJsonDocument();

    // add entries
    for (Entry<String, String> entry : properties.entrySet()) {
      String key = entry.getKey();
      String escapedKey = validName(key);
      JsonObject value = getJsonI18nValue(key, entry.getValue(), !StringUtils.equals(key, escapedKey));

      jsonDocument.add(escapedKey, value);
    }

    // return result
    return jsonDocument.build();
  }

  private JsonObject buildI18nJsonProperties() {
    JsonObjectBuilder jsonDocument = Json.createObjectBuilder();

    // add entries
    for (Entry<String, String> entry : properties.entrySet()) {
      String key = entry.getKey();
      String escapedKey = validName(key);
      jsonDocument.add(escapedKey, entry.getValue());
    }

    // return result
    return jsonDocument.build();
  }

  private JsonObjectBuilder getMixLanguageJsonDocument() {
    JsonObjectBuilder root = Json.createObjectBuilder();

    // add boiler plate
    root.add("jcr:" + JCR_PRIMARY_TYPE, JCR_NODETYPE_FOLDER);
    root.add("jcr:" + JCR_MIXIN_TYPES, Json.createArrayBuilder(JCR_MIX_LANGUAGE).build());

    // add language
    root.add("jcr:" + JCR_LANGUAGE, languageKey);

    return root;
  }

  private JsonObject getJsonI18nValue(String key, String value, boolean generatedKeyProperty) {
    JsonObjectBuilder valueNode = Json.createObjectBuilder();

    // add boiler plate
    valueNode.add("jcr:" + JCR_PRIMARY_TYPE, JCR_NODETYPE_FOLDER);
    valueNode.add("jcr:" + JCR_MIXIN_TYPES, Json.createArrayBuilder(SLING_MESSAGE_MIXIN_TYPE).build());

    // add extra key attribute
    if (generatedKeyProperty) {
      valueNode.add("sling:" + SLING_KEY, key);
    }

    // add actual i18n value
    valueNode.add("sling:" + SLING_MESSAGE, value);

    return valueNode.build();
  }

  /**
   * Build i18n resource XML in Sling i18n Message format.
   * @return XML
   */
  public String getI18nXmlString() {
    Format format = Format.getPrettyFormat();
    XMLOutputter outputter = new XMLOutputter(format);
    return outputter.outputString(buildI18nXml());
  }

  private Document buildI18nXml() {

    // get root
    Document xmlDocument = getMixLanguageXmlDocument();

    // add entries
    for (Entry<String, String> entry : properties.entrySet()) {
      String key = entry.getKey();
      String escapedKey = validName(key);
      Element value = getXmlI18nValue(escapedKey, key, entry.getValue(), !StringUtils.equals(key, escapedKey));

      xmlDocument.getRootElement().addContent(value);
    }

    // return result
    return xmlDocument;
  }

  private Document getMixLanguageXmlDocument() {
    Document doc = new Document();
    Element root = new Element("root", NAMESPACE_JCR);
    root.addNamespaceDeclaration(NAMESPACE_JCR);
    root.addNamespaceDeclaration(NAMESPACE_MIX);
    root.addNamespaceDeclaration(NAMESPACE_NT);
    root.addNamespaceDeclaration(NAMESPACE_SLING);
    doc.setRootElement(root);

    // add boiler plate
    root.setAttribute(JCR_PRIMARY_TYPE, JCR_NODETYPE_FOLDER, NAMESPACE_JCR);
    root.setAttribute(JCR_MIXIN_TYPES, "[" + StringUtils.join(JCR_MIX_LANGUAGE, ",") + "]", NAMESPACE_JCR);

    // add language
    root.setAttribute(JCR_LANGUAGE, languageKey, NAMESPACE_JCR);

    return doc;
  }

  private Element getXmlI18nValue(String escapedKey, String key, String value, boolean generatedKeyProperty) {
    Element valueNode = new Element(escapedKey);

    // add boiler plate
    valueNode.setAttribute(JCR_PRIMARY_TYPE, JCR_NODETYPE_FOLDER, NAMESPACE_JCR);
    valueNode.setAttribute(JCR_MIXIN_TYPES, "[" + StringUtils.join(SLING_MESSAGE_MIXIN_TYPE, ",") + "]", NAMESPACE_JCR);

    // add extra key attribute
    if (generatedKeyProperty) {
      valueNode.setAttribute(SLING_KEY, key, NAMESPACE_SLING);
    }

    // add actual i18n value
    valueNode.setAttribute(SLING_MESSAGE, value, NAMESPACE_SLING);

    return valueNode;
  }

  /**
   * Creates a valid node name. Replaces all chars not in a-z, A-Z and 0-9 or '_', '.' with '-'.
   * @param value String to be labelized.
   * @return The labelized string.
   */
  private static String validName(String value) {

    // replace some special chars first
    String text = value;
    text = StringUtils.replace(text, "ä", "ae");
    text = StringUtils.replace(text, "ö", "oe");
    text = StringUtils.replace(text, "ü", "ue");
    text = StringUtils.replace(text, "ß", "ss");

    // replace all invalid chars
    StringBuilder sb = new StringBuilder(text);
    for (int i = 0; i < sb.length(); i++) {
      char ch = sb.charAt(i);
      if (!((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')
          || (ch == '_') || (ch == '.'))) {
        ch = '-';
        sb.setCharAt(i, ch);
      }
    }
    return sb.toString();
  }

  /**
   * Build i18n resource PROPERTIES.
   * @return JSON
   */
  public String getI18nPropertiesString() throws IOException {
    // Load all properties
    Properties i18nProps = new Properties();

    // add entries
    for (Entry<String, String> entry : properties.entrySet()) {
      String key = entry.getKey();
      String escapedKey = validName(key);
      i18nProps.put(escapedKey, entry.getValue());
    }

    try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
      i18nProps.store(outStream, null);
      // Property files are always ISO 8859 encoded
      return outStream.toString(StandardCharsets.ISO_8859_1.name());
    }
  }

}
