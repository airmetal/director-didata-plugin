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

package com.cloudera.director.dimensiondata.compute;

import static com.cloudera.director.dimensiondata.compute.DimensionDataComputeProviderConfigurationProperty.REGION;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.director.dimensiondata.compute.util.ComputeUrls;
import com.cloudera.director.dimensiondata.internal.DimensionDataCredentials;
import com.cloudera.director.spi.v1.model.ConfigurationValidator;
import com.cloudera.director.spi.v1.model.Configured;
import com.cloudera.director.spi.v1.model.LocalizationContext;
import com.cloudera.director.spi.v1.model.exception.PluginExceptionConditionAccumulator;

/**
 * Validates Dimension Data compute provider configuration.
 */
public class DimensionDataComputeProviderConfigurationValidator implements ConfigurationValidator {

  private static final Logger LOG =
      LoggerFactory.getLogger(DimensionDataComputeProviderConfigurationValidator.class);

  static final String REGION_NOT_FOUND_MSG = "Region '%s' not found for project '%s'.";

  private DimensionDataCredentials credentials;

  /**
   * Creates a DimensionData compute provider configuration validator with the specified parameters.
   */
  public DimensionDataComputeProviderConfigurationValidator(DimensionDataCredentials credentials) {
    this.credentials = credentials;
  }

  @Override
  public void validate(String name, Configured configuration,
      PluginExceptionConditionAccumulator accumulator, LocalizationContext localizationContext) {

    checkRegion(configuration, accumulator, localizationContext);
  }

  /**
   * Validates the configured region.
   *
   * @param configuration       the configuration to be validated
   * @param accumulator         the exception condition accumulator
   * @param localizationContext the localization context
   */
  void checkRegion(Configured configuration,
      PluginExceptionConditionAccumulator accumulator,
      LocalizationContext localizationContext) {

    String regionName = configuration.getConfigurationValue(REGION, localizationContext);

    LOG.info(">> Querying region '{}'", regionName);

    String regionResolved = ComputeUrls.buildDimensionDataComputeApisUrl(regionName);
    
    LOG.info(">> Resolved region to '{}'", regionResolved);
  }
}
