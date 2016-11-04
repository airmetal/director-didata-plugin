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

import static com.cloudera.director.dimensiondata.DimensionDataCredentialsProviderConfigurationProperty.USERNAME;
import static com.cloudera.director.dimensiondata.DimensionDataCredentialsProviderConfigurationProperty.PASSWORD;
import static com.cloudera.director.dimensiondata.DimensionDataCredentialsProviderConfigurationProperty.REGION;

import com.cloudera.director.dimensiondata.internal.DimensionDataCredentials;
import com.cloudera.director.spi.v1.model.ConfigurationProperty;
import com.cloudera.director.spi.v1.model.Configured;
import com.cloudera.director.spi.v1.model.LocalizationContext;
import com.cloudera.director.spi.v1.provider.CredentialsProvider;
import com.cloudera.director.spi.v1.provider.CredentialsProviderMetadata;
import com.cloudera.director.spi.v1.provider.util.SimpleCredentialsProviderMetadata;
import com.cloudera.director.spi.v1.util.ConfigurationPropertiesUtil;
import com.typesafe.config.Config;

import java.util.List;

/**
 * Convert a configuration to a cloud provider specific credentials object.
 */
public class DimensionDataCredentialsProvider implements CredentialsProvider<DimensionDataCredentials> {

  private static final List<ConfigurationProperty> CONFIGURATION_PROPERTIES =
      ConfigurationPropertiesUtil.asConfigurationPropertyList(
    		  DimensionDataCredentialsProviderConfigurationProperty.values());

  public static CredentialsProviderMetadata METADATA =
      new SimpleCredentialsProviderMetadata(CONFIGURATION_PROPERTIES);

  private Config applicationProperties;

  public DimensionDataCredentialsProvider(Config applicationProperties) {
    this.applicationProperties = applicationProperties;
  }

  @Override
  public CredentialsProviderMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public DimensionDataCredentials createCredentials(Configured configuration,
      LocalizationContext localizationContext) {
    return new DimensionDataCredentials(
        applicationProperties,
        configuration.getConfigurationValue(USERNAME, localizationContext),
        configuration.getConfigurationValue(PASSWORD, localizationContext),
        configuration.getConfigurationValue(REGION, localizationContext)
    );
  }
}
