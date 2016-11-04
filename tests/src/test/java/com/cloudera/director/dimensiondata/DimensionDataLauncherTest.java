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
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import com.cloudera.director.spi.v1.model.ConfigurationProperty;
import com.cloudera.director.spi.v1.model.util.SimpleConfiguration;
import com.cloudera.director.spi.v1.provider.CloudProvider;
import com.cloudera.director.spi.v1.provider.CloudProviderMetadata;
import com.cloudera.director.spi.v1.provider.Launcher;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Performs 'live' test of {@link DimensionDataCloudProvider}.
 *
 * This system property is required: USERNAME. This system property is required:
 * PASSWORD. This system property is optional: REGION.
 *
 * @see <a href=""</a>
 */
public class DimensionDataLauncherTest {

	@Rule
	public TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

	@Test
	public void testLauncher() throws IOException {
		
	    TestFixture testFixture = TestFixture.newTestFixture(false);

		Launcher launcher = new DimensionDataLauncher();
		launcher.initialize(TEMPORARY_FOLDER.getRoot(), null);

		assertEquals(1, launcher.getCloudProviderMetadata().size());
		CloudProviderMetadata metadata = launcher.getCloudProviderMetadata().get(0);

		assertEquals(DimensionDataCloudProvider.ID, metadata.getId());

		List<ConfigurationProperty> providerConfigurationProperties = metadata.getProviderConfigurationProperties();
		assertEquals(0, providerConfigurationProperties.size());

		List<ConfigurationProperty> credentialsConfigurationProperties = metadata.getCredentialsProviderMetadata()
				.getCredentialsConfigurationProperties();
		assertEquals(2, credentialsConfigurationProperties.size());
		assertTrue(credentialsConfigurationProperties.contains(USERNAME.unwrap()));
		assertTrue(credentialsConfigurationProperties.contains(PASSWORD.unwrap()));

		// In order to create a cloud provider we need to configure credentials
		// (we expect them to be eagerly validated on cloud provider creation).
		Map<String, String> environmentConfig = new HashMap<String, String>();
		environmentConfig.put(USERNAME.unwrap().getConfigKey(), testFixture.getUsername());
		environmentConfig.put(PASSWORD.unwrap().getConfigKey(), testFixture.getPassword());

		CloudProvider cloudProvider = launcher.createCloudProvider(DimensionDataCloudProvider.ID,
				new SimpleConfiguration(environmentConfig), Locale.getDefault());
		assertEquals(DimensionDataCloudProvider.class, cloudProvider.getClass());

		CloudProvider cloudProvider2 = launcher.createCloudProvider(DimensionDataCloudProvider.ID,
				new SimpleConfiguration(environmentConfig), Locale.getDefault());
		assertNotSame(cloudProvider, cloudProvider2);
	}

	@Test
  public void testLauncherConfig() throws IOException {
	  DimensionDataLauncher launcher = new DimensionDataLauncher();
    File configDir = TEMPORARY_FOLDER.getRoot();
    File configFile = new File(configDir, Configurations.DIMENSIONDATA_CONFIG_FILENAME);
    PrintWriter printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
        new FileOutputStream(configFile), "UTF-8")));
    
    printWriter.println("configurationDirectory = /tmp");
    printWriter.println("dimensiondata {");
    		printWriter.println("com.cloudera.director.dimensiondata.compute.DimensionDataComputeProvider{");
    			printWriter.println("configs {");
    				printWriter.println("username: adas");
    				printWriter.println("password: 'P@$$w0rd01'");
    				printWriter.println("region: dd-na");
    				printWriter.println("# and other things needed to create the cloud provider");
    				printWriter.println("resourceConfigs {");
    					printWriter.println("name: cloudera");
    						printWriter.println("image: 'RedHat 6 64-bit 4 CPU'");
    						printWriter.println("sshUsername: root");
    						printWriter.println("sshPassword: AS12qwas");
    						printWriter.println("datacenter: na12");
    						printWriter.println("networkName: Cloudera-Director-Network");
    						printWriter.println("baseIpv4: 10.0.3.0");
    						printWriter.println("instanceNamePrefix: spi-tck-${?USER}");
    					printWriter.println("}");
    					printWriter.println("resourceTags {");
    						printWriter.println("owner: ${?USER}");
    					printWriter.println("}");
    					printWriter.println("expectedOpenPort: 22");
    		printWriter.println("}");
    		printWriter.println(" configs {");
    			printWriter.println("username: adas");
    			printWriter.println("password: 'P@$$w0rd01'");
    			printWriter.println("region: dd-na");
    			printWriter.println("# and other things needed to create the cloud provider");
    		printWriter.println("}");
    printWriter.println("}");
    printWriter.close();


    launcher.initialize(configDir, null);
/*
    // Verify that base config is reflected.
    assertEquals("https://www.googleapis.com/compute/v1/projects/centos-cloud/global/images/centos-6-v20160526",
        launcher.googleConfig.getString(Configurations.IMAGE_ALIASES_SECTION + "centos6"));

    // Verify that overridden config is reflected.
    assertEquals("https://www.googleapis.com/compute/v1/projects/rhel-cloud/global/images/rhel-6-v20150430",
        launcher.googleConfig.getString(Configurations.IMAGE_ALIASES_SECTION + "rhel6"));

    // Verify that new config is reflected.
    assertEquals("https://www.googleapis.com/compute/v1/projects/ubuntu-os-cloud/global/images/ubuntu-1404-trusty-v20150128",
        launcher.googleConfig.getString(Configurations.IMAGE_ALIASES_SECTION + "ubuntu"));*/
  }
}
