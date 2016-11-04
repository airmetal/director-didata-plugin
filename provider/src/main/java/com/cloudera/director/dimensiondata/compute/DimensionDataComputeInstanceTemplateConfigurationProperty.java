/*
 * Copyright (c) 2015 Dimension Data, Inc.
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

import com.cloudera.director.spi.v1.compute.ComputeInstanceTemplate.ComputeInstanceTemplateConfigurationPropertyToken;
import com.cloudera.director.spi.v1.model.ConfigurationProperty;
import com.cloudera.director.spi.v1.model.ConfigurationPropertyToken;
import com.cloudera.director.spi.v1.model.Property;
import com.cloudera.director.spi.v1.model.util.SimpleConfigurationPropertyBuilder;

/**
 * Dimension Data compute server template configuration properties.
 */
public enum DimensionDataComputeInstanceTemplateConfigurationProperty implements ConfigurationPropertyToken {

  IMAGE_NAME(new SimpleConfigurationPropertyBuilder()
      .configKey(ComputeInstanceTemplateConfigurationPropertyToken.IMAGE.unwrap().getConfigKey())
      .name("Image name")
      .addValidValues("REDHAT764", "REDHAT664")
      .defaultValue("4ef9c9d4-b188-4b71-9c94-c85e8f257b9e")
      .defaultDescription("OS image name for server. Default value is RedHat 6 64 Bit image with 4 CPU and 8GB RAM")
      .defaultErrorMessage("Image name is mandatory")
      .widget(ConfigurationProperty.Widget.OPENLIST)
      .required(true)
      .build()),
  
  DATACENTER(new SimpleConfigurationPropertyBuilder()
	      .configKey("datacenter")
	      .name("datacenter")
	      .defaultDescription(
	          "The DataCenter to target for deployment. " +
	          "The datacenter you specify must be contained within the region you selected.")
	      .defaultErrorMessage("Datacenter is mandatory")
	      .required(true)
	      .build()),


  TYPE(new SimpleConfigurationPropertyBuilder()
      .configKey(ComputeInstanceTemplateConfigurationPropertyToken.TYPE.unwrap().getConfigKey())
      .name("Machine Type")
      .addValidValues(
          "ESSENTIALS", "ADVANCED")
      .defaultDescription(
          "The network domain type.")
      .defaultValue("ADVANCED")
      .widget(ConfigurationProperty.Widget.OPENLIST)
      .required(false)
      .build()),

  NETWORK_NAME(new SimpleConfigurationPropertyBuilder()
      .configKey("networkName")
      .name("Network Domain Name")
      .defaultDescription(
          "The network domain name")
      .defaultErrorMessage("Network Domain Name is Mandatory")
      .required(true)
      .build()),
  
  VLAN_IPV4(new SimpleConfigurationPropertyBuilder()
	      .configKey("baseIpv4")
	      .name("Base IPv4 Address")
	      .defaultDescription(
	          "The base IPv4 address")
	      .defaultValue("10.0.3.0")
	      .required(true)
	      .build()),

  BOOT_DISK_TYPE(new SimpleConfigurationPropertyBuilder()
      .configKey("bootDiskType")
      .name("Boot Disk Type")
      .addValidValues("Economy", "Standard", "High Performance")
      .defaultDescription("The type of boot disk to create (Economy, Standard, High Performance)")
      .defaultValue("High Performance")
      .widget(ConfigurationProperty.Widget.LIST)
      .required(false)
      .build()),

  BOOT_DISK_SIZE_GB(new SimpleConfigurationPropertyBuilder()
      .configKey("bootDiskSizeGb")
      .name("Boot Disk Size (GB)")
      .defaultDescription("The size of the boot disk in GB.")
      .defaultValue("60")
      .type(Property.Type.INTEGER)
      .widget(ConfigurationProperty.Widget.NUMBER)
      .required(false)
      .build()),

  DATA_DISK_COUNT(new SimpleConfigurationPropertyBuilder()
      .configKey("dataDiskCount")
      .name("Data Disk Count")
      .defaultDescription("The number of data disks to create.")
      .defaultValue("2")
      .type(Property.Type.INTEGER)
      .widget(ConfigurationProperty.Widget.NUMBER)
      .required(false)
      .build()),

  DATA_DISK_TYPE(new SimpleConfigurationPropertyBuilder()
      .configKey("dataDiskType")
      .name("Data Disk Type")
      .addValidValues("Economy", "Standard", "High Performance")
      .defaultDescription(
          "The type of data disks to create (Economy, Standard, High Performance)")
      .defaultValue("High Performance")
      .widget(ConfigurationProperty.Widget.LIST)
      .required(false)
      .build()),

  DATA_DISK_SIZE_GB(new SimpleConfigurationPropertyBuilder()
      .configKey("dataDiskSizeGb")
      .name("Data Disk Size")
      .defaultDescription(
          "The size of the data disks in GB.")
      .defaultValue("375")
      .type(Property.Type.INTEGER)
      .widget(ConfigurationProperty.Widget.NUMBER)
      .required(false)
      .build());

/*  LOCAL_SSD_INTERFACE_TYPE(new SimpleConfigurationPropertyBuilder()
      .configKey("localSSDInterfaceType")
      .name("Local SSD Interface Type")
      .addValidValues("SCSI", "NVME")
      .defaultDescription(
          "The Local SSD interface type (SCSI or NVME)")
      .defaultValue("SCSI")
      .widget(ConfigurationProperty.Widget.LIST)
      .required(false)
      .build());*/

  /**
   * The configuration property.
   */
  private final ConfigurationProperty configurationProperty;

  /**
   * Creates a configuration property token with the specified parameters.
   *
   * @param configurationProperty the configuration property
   */
  DimensionDataComputeInstanceTemplateConfigurationProperty(ConfigurationProperty configurationProperty) {
    this.configurationProperty = configurationProperty;
  }

  @Override
  public ConfigurationProperty unwrap() {
    return configurationProperty;
  }
}
