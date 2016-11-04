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

import static com.cloudera.director.spi.v1.provider.Launcher.DEFAULT_PLUGIN_LOCALIZATION_CONTEXT;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.cloudera.director.dimensiondata.compute.DimensionDataComputeProvider;
import com.cloudera.director.dimensiondata.internal.DimensionDataCredentials;
import com.cloudera.director.dimensiondata.util.Names;
import com.cloudera.director.spi.v1.model.ConfigurationProperty;
import com.cloudera.director.spi.v1.model.LocalizationContext;
import com.cloudera.director.spi.v1.model.util.SimpleConfiguration;
import com.cloudera.director.spi.v1.provider.CloudProviderMetadata;
import com.cloudera.director.spi.v1.provider.CredentialsProviderMetadata;
import com.cloudera.director.spi.v1.provider.ResourceProvider;
import com.cloudera.director.spi.v1.provider.ResourceProviderMetadata;

/**
 * Performs 'live' test of {@link DimensionDataCloudProvider}.
 *
 * This system property is required: USERNAME.
 * This system property is required: PASSWORD.
 * This system property is optional: REGION.
 *
 * @see <a href=""</a>
 */
public class DimensionDataCloudProviderTest {

  private static TestFixture testFixture;

  @BeforeClass
  public static void beforeClass() throws IOException {
/*    Assume.assumeFalse(System.getProperty("GCP_PROJECT_ID", "").isEmpty());

    testFixture = TestFixture.newTestFixture(false);*/
  }

  @Rule
  public TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

  @Test
  public void testProvider() throws IOException {

    CloudProviderMetadata dimensiondataProviderMetadata = DimensionDataCloudProvider.METADATA;

    CredentialsProviderMetadata credentialsProviderMetadata =
        dimensiondataProviderMetadata.getCredentialsProviderMetadata();
    List<ConfigurationProperty> credentialsConfigurationProperties =
        credentialsProviderMetadata.getCredentialsConfigurationProperties();
    assertEquals(2, credentialsConfigurationProperties.size());
    assertTrue(credentialsConfigurationProperties.contains(USERNAME.unwrap()));
    assertTrue(credentialsConfigurationProperties.contains(PASSWORD.unwrap()));

    Config applicationPropertiesConfig = TestUtils.buildApplicationPropertiesConfig();
    DimensionDataCredentialsProvider dimensionDataCredentialsProvider = new DimensionDataCredentialsProvider(applicationPropertiesConfig);
    assertNotNull(dimensionDataCredentialsProvider);

    // In order to create a cloud provider we need to configure credentials
    // (we expect them to be eagerly validated on cloud provider creation).
    Map<String, String> environmentConfig = new HashMap<String, String>();
    environmentConfig.put(USERNAME.unwrap().getConfigKey(), testFixture.getUsername());
    environmentConfig.put(PASSWORD.unwrap().getConfigKey(), testFixture.getPassword());

    LocalizationContext cloudLocalizationContext =
        DimensionDataCloudProvider.METADATA.getLocalizationContext(DEFAULT_PLUGIN_LOCALIZATION_CONTEXT);

    DimensionDataCredentials dimensionDataCredentials = DimensionDataCredentialsProvider.createCredentials(
        new SimpleConfiguration(environmentConfig), cloudLocalizationContext);
    assertNotNull(dimensionDataCredentials);

    // Verify the user agent header string.
    assertEquals(
        Names.buildApplicationNameVersionTag(applicationPropertiesConfig),
        dimensionDataCredentials.getCompute().getApplicationName());

    Config dimensionDataConfig = TestUtils.buildDimensionDataConfig();

    DimensionDataCloudProvider dimensionDataProvider = new DimensionDataCloudProvider(dimensionDataCredentials, applicationPropertiesConfig,
        dimensionDataConfig, cloudLocalizationContext);
    assertNotNull(dimensionDataProvider);
    assertSame(dimensionDataProviderMetadata, dimensionDataProvider.getProviderMetadata());

    ResourceProviderMetadata computeResourceProviderMetadata = null;
    List<ResourceProviderMetadata> resourceProviderMetadatas = dimensionDataProviderMetadata.getResourceProviderMetadata();

    for (ResourceProviderMetadata resourceProviderMetadata : resourceProviderMetadatas) {
      String resourceProviderId = resourceProviderMetadata.getId();

      if (DimensionDataComputeProvider.ID.equals(resourceProviderId)) {
        computeResourceProviderMetadata = resourceProviderMetadata;
      } else {
        throw new IllegalArgumentException("Unexpected resource provider: " + resourceProviderId);
      }
    }
    assertNotNull(computeResourceProviderMetadata);

    ResourceProvider<?, ?> computeResourceProvider =
        dimensionDataProvider.createResourceProvider(DimensionDataComputeProvider.ID,
            new SimpleConfiguration(Collections.<String, String>emptyMap()));
    Assert.assertEquals(DimensionDataComputeProvider.class, computeResourceProvider.getClass());
  }
}
