/*
 * Copyright (c) 2015 DimensionData, Inc.
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

import com.cloudera.director.spi.v1.model.ConfigurationProperty;
import com.cloudera.director.spi.v1.model.ConfigurationPropertyToken;
import com.cloudera.director.spi.v1.model.util.SimpleConfigurationPropertyBuilder;

/**
 * An enum of properties required for building credentials.
 */
public enum DimensionDataCredentialsProviderConfigurationProperty implements ConfigurationPropertyToken {

  USERNAME(new SimpleConfigurationPropertyBuilder()
      .configKey("username")
      .name("Username")
      .defaultDescription("Dimension Data cloud client's Username.")
      .defaultErrorMessage("User name is mandatory")
      .widget(ConfigurationProperty.Widget.TEXT)
      .sensitive(true)
      .required(true)
      .build()),

  PASSWORD(new SimpleConfigurationPropertyBuilder()
      .configKey("password")
      .name("Password.")
      .defaultDescription(
          "Dimension Data cloud account user's password.")
      .widget(ConfigurationProperty.Widget.PASSWORD)
      .defaultErrorMessage("Password is mandatory")
      .required(true)
      .sensitive(true)
      .build()),
  
  REGION(new SimpleConfigurationPropertyBuilder()
      .configKey("region")
      .name("Region")
      .defaultDescription(
          "Dimension Data cloud region.<br />" +
          "Default value will be North America<br />")
      .widget(ConfigurationProperty.Widget.FILE)
      .defaultValue("dd-na")
      .build());

  /**
   * The configuration property.
   */
  private final ConfigurationProperty configurationProperty;

  /**
   * Creates a configuration property token with the specified parameters.
   *
   * @param configurationProperty the configuration property
   */
  DimensionDataCredentialsProviderConfigurationProperty(
      ConfigurationProperty configurationProperty) {
    this.configurationProperty = configurationProperty;
  }

  @Override
  public ConfigurationProperty unwrap() {
    return configurationProperty;
  }
}
