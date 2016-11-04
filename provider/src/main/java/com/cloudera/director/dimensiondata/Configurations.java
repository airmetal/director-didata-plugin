/*
 * Copyright (c) 2016 Dimension Data, Inc.
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

/**
 * Constants for important properties and sections in the configuration file.
 *
 * @see <a href="https://github.com/typesafehub/config" />
 */
public class Configurations {

  private Configurations() {
  }

  /**
   * The configuration file name.
   */
  public static final String DIMENSIONDATA_CONFIG_FILENAME = "dimensiondata.conf";

  /**
   * The package.
   */
  public static final String DIMENSIONDATA_PLUGIN_PACKAGE = "/com/cloudera/director/dimensiondata/";

  /**
   * The configuration file name including package qualification.
   */
  public static final String DIMENSIONDATA_CONFIG_QUALIFIED_FILENAME = DIMENSIONDATA_PLUGIN_PACKAGE + DIMENSIONDATA_CONFIG_FILENAME;

  /**
   * The application properties file name including package qualification.
   */
  public static final String APPLICATION_PROPERTIES_FILENAME = DIMENSIONDATA_PLUGIN_PACKAGE + "application.properties";

  /**
   * The HOCON path prefix for image aliases configuration.
   */
  public static final String CREDS_COMPUTE_SECTION = "dimensiondata.compute.credentials.";

  /**
   * The HOCON path prefix for Cloud SQL regions configuration.
   */
  public static final String CLOUD_SQL_REGIONS_ALIASES_SECTION = "dimensiondata.cloudSQL.regions.toComputeRegion.";
}
