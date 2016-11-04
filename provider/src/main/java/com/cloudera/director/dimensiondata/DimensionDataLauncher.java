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

import java.io.File;
import java.util.Collections;
import java.util.Locale;

import com.cloudera.director.dimensiondata.internal.DimensionDataCredentials;
import com.cloudera.director.spi.v1.common.http.HttpProxyParameters;
import com.cloudera.director.spi.v1.model.Configured;
import com.cloudera.director.spi.v1.model.LocalizationContext;
import com.cloudera.director.spi.v1.model.exception.InvalidCredentialsException;
import com.cloudera.director.spi.v1.provider.CloudProvider;
import com.cloudera.director.spi.v1.provider.CredentialsProvider;
import com.cloudera.director.spi.v1.provider.util.AbstractLauncher;
import com.dimensiondata.cloud.client.Cloud;
import com.dimensiondata.cloud.client.OrderBy;
import com.dimensiondata.cloud.client.model.Datacenters;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigSyntax;

public class DimensionDataLauncher extends AbstractLauncher {

  private Config applicationProperties = null;

  protected Config dimensiondataConfig = null;

  public DimensionDataLauncher() {
    super(Collections.singletonList(DimensionDataCloudProvider.METADATA), null);
  }

  /**
   * The config is loaded from a dimensiondata.conf file on the classpath. If a dimensiondata.conf file also exists in the
   * configuration directory, its values will override the values defined in the google.conf file on the
   * classpath.
   */
  @Override
  public void initialize(File configurationDirectory, HttpProxyParameters httpProxyParameters) {
    try {
      dimensiondataConfig = parseConfigFromClasspath(Configurations.DIMENSIONDATA_CONFIG_QUALIFIED_FILENAME);
      applicationProperties = parseConfigFromClasspath(Configurations.APPLICATION_PROPERTIES_FILENAME);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // Check if an additional dimensiondata.conf file exists in the configuration directory.
    File configFile = new File(configurationDirectory, Configurations.DIMENSIONDATA_CONFIG_FILENAME);

    if (configFile.canRead()) {
      try {
        Config configFromFile = parseConfigFromFile(configFile);

        // Merge the two configurations, with values in configFromFile overriding values in dimensiondataConfig.
        dimensiondataConfig = configFromFile.withFallback(dimensiondataConfig);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

  }

  /**
   * Parses the specified configuration file from the classpath.
   *
   * @param configPath the path to the configuration file
   * @return the parsed configuration
   */
  private static Config parseConfigFromClasspath(String configPath) {
    ConfigParseOptions options = ConfigParseOptions.defaults()
        .setSyntax(ConfigSyntax.CONF)
        .setAllowMissing(false);

    return ConfigFactory.parseResourcesAnySyntax(DimensionDataLauncher.class, configPath, options);
  }

  /**
   * Parses the specified configuration file.
   *
   * @param configFile the configuration file
   * @return the parsed configuration
   */
  private static Config parseConfigFromFile(File configFile) {
    ConfigParseOptions options = ConfigParseOptions.defaults()
        .setSyntax(ConfigSyntax.CONF)
        .setAllowMissing(false);

    return ConfigFactory.parseFileAnySyntax(configFile, options);
  }

  @Override
  public CloudProvider createCloudProvider(String cloudProviderId, Configured configuration,
      Locale locale) {

    if (!DimensionDataCloudProvider.ID.equals(cloudProviderId)) {
      throw new IllegalArgumentException("Cloud provider not found: " + cloudProviderId);
    }

    LocalizationContext localizationContext = getLocalizationContext(locale);

    // At this point the configuration object will already contain
    // the required data for authentication.

    CredentialsProvider<DimensionDataCredentials> provider = new DimensionDataCredentialsProvider(applicationProperties);
    DimensionDataCredentials credentials = provider.createCredentials(configuration, localizationContext);
    Cloud compute = credentials.getCompute();

    if (compute == null) {
      throw new InvalidCredentialsException("Invalid cloud provider credentials.");
    } else { 
      String user = credentials.getUserName();	
    	
      try {
        // Attempt CloudControl api call to verify credentials.
        Datacenters datacenter = compute.datacenter().listDatacenters(2, 1, OrderBy.EMPTY);
      } catch (Exception e) {
        throw new InvalidCredentialsException(
            "Invalid cloud provider credentials for user '" + user + "'.", e);
      }
    }

    return new DimensionDataCloudProvider(credentials, applicationProperties, dimensiondataConfig, localizationContext);
  }
}
