/*
 * Copyright (c) 2015 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudera.director.dimensiondata;

import static org.junit.Assert.fail;

import com.cloudera.director.dimensiondata.compute.util.ComputeUrls;
import com.cloudera.director.dimensiondata.shaded.com.typesafe.config.Config;
import com.cloudera.director.dimensiondata.shaded.com.typesafe.config.ConfigFactory;
import com.cloudera.director.dimensiondata.sql.util.SQLUrls;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class TestUtils {

  public static String readFile(String path, Charset encoding) throws IOException {
    return Files.toString(new File(path), encoding);
  }

  public static String readRequiredSystemProperty(String systemPropertyKey) {
    String systemPropertyValue = System.getProperty(systemPropertyKey, "");

    if (systemPropertyValue.isEmpty()) {
      fail("System property '" + systemPropertyKey + "' is required.");
    }

    return systemPropertyValue;
  }

  public static String readFileIfSpecified(String fileName) throws IOException {
    if (fileName != null && !fileName.isEmpty()) {
      return TestUtils.readFile(fileName, Charset.defaultCharset());
    } else {
      return null;
    }
  }

  public static Config buildApplicationPropertiesConfig() throws IOException {
    Map<String, String> applicationProperties = new HashMap<String, String>();
    applicationProperties.put("application.name", "Cloudera-Director-DimensionData-Plugin");
    applicationProperties.put("application.version", "1.0.0");

    return ConfigFactory.parseMap(applicationProperties);
  }

/*  public static Config buildDimensionDataConfig() throws IOException {
    Map<String, String> dimensionDataConfig = new HashMap<String, String>();
    dimensionDataConfig.put(
        Configurations.CREDS_COMPUTE_SECTION + "centos6",
        buildImageUrl("centos-cloud", "centos-6-v20150526"));
    dimensionDataConfig.put(
        Configurations.CREDS_COMPUTE_SECTION + "rhel6",
        buildImageUrl("rhel-cloud", "rhel-6-v20150526"));


    return ConfigFactory.parseMap(dimensionDataConfig);
  }*/

  public static String buildComputeInstanceUrl(String region) {
    return ComputeUrls.buildDimensionDataComputeApisUrl(region);
  }

}
