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

package com.cloudera.director.dimensiondata.compute;

import static com.cloudera.director.dimensiondata.compute.DimensionDataComputeInstanceTemplateConfigurationProperty.BOOT_DISK_SIZE_GB;
import static com.cloudera.director.dimensiondata.compute.DimensionDataComputeInstanceTemplateConfigurationProperty.BOOT_DISK_TYPE;
import static com.cloudera.director.dimensiondata.compute.DimensionDataComputeInstanceTemplateConfigurationProperty.DATACENTER;
import static com.cloudera.director.dimensiondata.compute.DimensionDataComputeInstanceTemplateConfigurationProperty.DATA_DISK_COUNT;
import static com.cloudera.director.dimensiondata.compute.DimensionDataComputeInstanceTemplateConfigurationProperty.DATA_DISK_SIZE_GB;
import static com.cloudera.director.dimensiondata.compute.DimensionDataComputeInstanceTemplateConfigurationProperty.DATA_DISK_TYPE;
import static com.cloudera.director.dimensiondata.compute.DimensionDataComputeInstanceTemplateConfigurationProperty.IMAGE_NAME;
import static com.cloudera.director.dimensiondata.compute.DimensionDataComputeInstanceTemplateConfigurationProperty.NETWORK_NAME;
import static com.cloudera.director.dimensiondata.compute.DimensionDataComputeInstanceTemplateConfigurationProperty.VLAN_IPV4;
import static com.cloudera.director.spi.v1.model.InstanceTemplate.InstanceTemplateConfigurationPropertyToken.INSTANCE_NAME_PREFIX;
import static com.cloudera.director.spi.v1.model.util.Validations.addError;

import java.security.InvalidParameterException;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.director.dimensiondata.internal.DimensionDataCredentials;
import com.cloudera.director.spi.v1.model.ConfigurationValidator;
import com.cloudera.director.spi.v1.model.Configured;
import com.cloudera.director.spi.v1.model.LocalizationContext;
import com.cloudera.director.spi.v1.model.exception.PluginExceptionConditionAccumulator;
import com.cloudera.director.spi.v1.model.exception.TransientProviderException;
import com.dimensiondata.cloud.client.ClientRuntimeException;
import com.dimensiondata.cloud.client.Cloud;
import com.dimensiondata.cloud.client.Filter;
import com.dimensiondata.cloud.client.OrderBy;
import com.dimensiondata.cloud.client.Param;
import com.dimensiondata.cloud.client.http.ForbiddenException;
import com.dimensiondata.cloud.client.http.NotFoundException;
import com.dimensiondata.cloud.client.http.RequestException;
import com.dimensiondata.cloud.client.http.ServiceUnavailableException;
import com.dimensiondata.cloud.client.http.UnauthorizedException;
import com.dimensiondata.cloud.client.model.DatacenterType;
import com.dimensiondata.cloud.client.model.NetworkDomainType;
import com.dimensiondata.cloud.client.model.NetworkDomains;
import com.dimensiondata.cloud.client.model.VlanType;
import com.dimensiondata.cloud.client.model.Vlans;

/**
 * Validates Google compute instance template configuration.
 */
public class DimensionDataComputeInstanceTemplateConfigurationValidator implements ConfigurationValidator {

  private static final Logger LOG =
      LoggerFactory.getLogger(DimensionDataComputeInstanceTemplateConfigurationValidator.class);

  static final int MIN_BOOT_DISK_SIZE_GB = 10;

  static final int MIN_DATA_DISK_SIZE_GB = 10;
  static final int EXACT_LOCAL_SSD_DATA_DISK_SIZE_GB = 375;

  static final int MIN_LOCAL_SSD_COUNT = 0;

  static final int MAX_LOCAL_SSD_COUNT = 4;

 
  static final String DATACENTER_NOT_FOUND_MSG = "Datacenter '%s' not found '%s'.";

  static final String DATACENTER_NOT_FOUND_IN_REGION_MSG = "Datacenter '%s' not found in region '%s' for project '%s'.";

 
  static final String NETWORK_NOT_FOUND_MSG = "Unable to locate '%s'network domain.";
  

  static final String MAPPING_FOR_IMAGE_ALIAS_NOT_FOUND = "Mapping for image alias '%s' not found.";
 
  static final String IMAGE_NOT_FOUND_MSG = "Image '%s' not found for project '%s'.";

  static final String MALFORMED_IMAGE_URL_MSG = "Malformed image url '%s'.";

  static final String INVALID_BOOT_DISK_TYPE_MSG =
      "Invalid boot disk type '%s'. Available options: %s";


  static final String INVALID_BOOT_DISK_SIZE_FORMAT_MSG = "Boot disk size must be an integer: '%s'.";

  static final String INVALID_BOOT_DISK_SIZE_MSG =
      "Boot disk size must be at least '%dGB'. Current configuration: '%dGB'.";

  static final String INVALID_NETWORK_NAME_MESSAGE = "Network Domain name does not exist in this region";
  
  static final String INVALID_DATA_DISK_COUNT_FORMAT_MSG = "Data disk count must be an integer: '%s'.";

  static final String INVALID_DATA_DISK_COUNT_NEGATIVE_MSG =
      "Data disk count must be non-negative. Current configuration: '%d'.";

  static final String INVALID_LOCAL_SSD_DATA_DISK_COUNT_MSG =
      "Data disk count when using local SSD drives must be between '%d' and '%d', inclusive. " +
      "Current configuration: '%d'.";


  static final String INVALID_DATA_DISK_SIZE_FORMAT_MSG = "Data disk size must be an integer: '%s'.";
  
  static final String INVALID_DATA_DISK_SIZE_MSG =
      "Data disk size must be at least '%dGB'. Current configuration: '%dGB'.";

  static final String INVALID_LOCAL_SSD_DATA_DISK_SIZE_MSG =
      "Data disk size when using local SSD drives must be exactly '%dGB'. Current configuration: '%dGB'.";


  static final String INVALID_DATA_DISK_TYPE_MSG =
      "Invalid data disk type '%s'. Available options: %s";


  static final String MACHINE_TYPE_NOT_FOUND_IN_ZONE_MSG =
      "Machine type '%s' not found in zone '%s' for project '%s'.";


  static final String NETWORK_DOMAIN_DESCRIPTION = "Cloudera Director 2.1.1 dedicated network";


  static final String VLAN_NOT_FOUND_MSG = "Vlan '%s' not found '%s'.";
  

  static final String PREFIX_MISSING_MSG = "Instance name prefix must be provided.";
 
  static final String INVALID_PREFIX_LENGTH_MSG = "Instance name prefix must be between 1 and 26 characters.";

  static final String INVALID_PREFIX_MSG = "Instance name prefix must follow this pattern: " +
      "The first character must be a lowercase letter, and all following characters must be a dash, lowercase " +
      "letter, or digit.";

  /**
   * The Dimension Data compute provider.
   */
  private final DimensionDataComputeProvider provider;

  /**
   * The pattern to which instance name prefixes must conform. The pattern is the same as that for instance names in
   * general, except that we allow a trailing dash to be used. This is allowed since we always append a dash and the
   * specified instance id to the prefix.
   *
   *  @see <a href="" />
   */
  private final static Pattern instanceNamePrefixPattern = Pattern.compile("[a-z][-a-z0-9]*");

  /**
   * Creates a Dimension Data instance template configuration validator with the specified parameters.
   *
   * @param provider the Dimension Data compute provider
   */
  public DimensionDataComputeInstanceTemplateConfigurationValidator(DimensionDataComputeProvider provider) {
    this.provider = provider;
  }

  @Override
  public void validate(String name, Configured configuration,
      PluginExceptionConditionAccumulator accumulator, LocalizationContext localizationContext) {

    checkDataCenter(configuration, accumulator, localizationContext);
    checkImage(configuration, accumulator, localizationContext);
    checkBootDiskType(configuration, accumulator, localizationContext);
    checkBootDiskSize(configuration, accumulator, localizationContext);
    checkDataDiskCount(configuration, accumulator, localizationContext);
    checkDataDiskType(configuration, accumulator, localizationContext);
    checkDataDiskSize(configuration, accumulator, localizationContext);
 //   checkMachineType(configuration, accumulator, localizationContext);
    checkNetworkDomain(configuration, accumulator, localizationContext);
    checkVlan(configuration, accumulator, localizationContext);
    checkPrefix(configuration, accumulator, localizationContext);
  }

  /**
   * Validates the configured datacenter.
   *
   * @param configuration       the configuration to be validated
   * @param accumulator         the exception condition accumulator
   * @param localizationContext the localization context
   */
  void checkDataCenter(Configured configuration,
      PluginExceptionConditionAccumulator accumulator,
      LocalizationContext localizationContext) {

    String dcName = configuration.getConfigurationValue(DATACENTER, localizationContext);

    if (dcName != null) {
      LOG.info(">> Querying Datacenter '{}'", dcName);

      DimensionDataCredentials credentials = provider.getCredentials();
      Cloud compute = credentials.getCompute();
      //String regionName = credentials.getRegion();

      try {
    	  compute.datacenter().getDatacenter(dcName);
      } catch (ForbiddenException | InvalidParameterException | NotFoundException | RequestException | ServiceUnavailableException | UnauthorizedException  e) {
        if (e.getClass() == NotFoundException.class) {
          addError(accumulator, DATACENTER, localizationContext, null, DATACENTER_NOT_FOUND_MSG,
              dcName);
        } else {
          throw new TransientProviderException(e);
        }
      } catch (ClientRuntimeException e) {
        throw new TransientProviderException(e);
      }
    }
  }

  /**
   * Validates the configured image.
   *
   * @param configuration       the configuration to be validated
   * @param accumulator         the exception condition accumulator
   * @param localizationContext the localization context
   */
  void checkImage(Configured configuration,
      PluginExceptionConditionAccumulator accumulator,
      LocalizationContext localizationContext) {

    String sourceImage = configuration.getConfigurationValue(IMAGE_NAME, localizationContext);

      if (sourceImage != null && !sourceImage.isEmpty()) {
        DimensionDataCredentials credentials = provider.getCredentials();
        Cloud compute = credentials.getCompute();

        try {
            compute.image().getOsImage(sourceImage);
        }catch (ForbiddenException | InvalidParameterException | NotFoundException | RequestException | ServiceUnavailableException | UnauthorizedException  e) 
        {
           if (e.getClass() == NotFoundException.class) {
                addError(accumulator, IMAGE_NAME, localizationContext, null, IMAGE_NOT_FOUND_MSG,
                		sourceImage);
           }else {
                throw new TransientProviderException(e);
           }      
        } catch (ClientRuntimeException e) {
              throw new TransientProviderException(e);
           }
      	}
      }
  

  /**
   * Validates the configured boot disk type.
   *
   * @param configuration       the configuration to be validated
   * @param accumulator         the exception condition accumulator
   * @param localizationContext the localization context
   */
  void checkBootDiskType(Configured configuration,
      PluginExceptionConditionAccumulator accumulator,
      LocalizationContext localizationContext) {

  	}


  /**
   * Validates the configured boot disk size.
   *
   * @param configuration       the configuration to be validated
   * @param accumulator         the exception condition accumulator
   * @param localizationContext the localization context
   */
  void checkBootDiskSize(Configured configuration,
      PluginExceptionConditionAccumulator accumulator,
      LocalizationContext localizationContext) {

    String bootDiskSizeGBString = configuration.getConfigurationValue(BOOT_DISK_SIZE_GB, localizationContext);

    if (bootDiskSizeGBString != null) {
      try {
        int bootDiskSizeGB = Integer.parseInt(bootDiskSizeGBString);

        if (bootDiskSizeGB < MIN_BOOT_DISK_SIZE_GB) {
          addError(accumulator, BOOT_DISK_SIZE_GB, localizationContext, null, INVALID_BOOT_DISK_SIZE_MSG,
              MIN_BOOT_DISK_SIZE_GB, bootDiskSizeGB);
        }
      } catch (NumberFormatException e) {
        addError(accumulator, BOOT_DISK_SIZE_GB, localizationContext, null, INVALID_BOOT_DISK_SIZE_FORMAT_MSG,
            bootDiskSizeGBString);
      }
    }
  }

  /**
   * Validates the configured number of data disks.
   *
   * @param configuration       the configuration to be validated
   * @param accumulator         the exception condition accumulator
   * @param localizationContext the localization context
   */
  void checkDataDiskCount(Configured configuration,
      PluginExceptionConditionAccumulator accumulator,
      LocalizationContext localizationContext) {

    String dataDiskCountString = configuration.getConfigurationValue(DATA_DISK_COUNT, localizationContext);

    if (dataDiskCountString != null) {
      try {
        int dataDiskCount = Integer.parseInt(dataDiskCountString);
        if (dataDiskCount < 0) {
          addError(accumulator, DATA_DISK_COUNT, localizationContext, null, INVALID_DATA_DISK_COUNT_NEGATIVE_MSG,
              dataDiskCount);
        }
      } catch (NumberFormatException e) {
        addError(accumulator, DATA_DISK_COUNT, localizationContext, null, INVALID_DATA_DISK_COUNT_FORMAT_MSG,
            dataDiskCountString);
      }
    }
  }

  /**
   * Validates the configured data disk type.
   *
   * @param configuration       the configuration to be validated
   * @param accumulator         the exception condition accumulator
   * @param localizationContext the localization context
   */
  void checkDataDiskType(Configured configuration,
      PluginExceptionConditionAccumulator accumulator,
      LocalizationContext localizationContext) {

    String dataDiskType = configuration.getConfigurationValue(DATA_DISK_TYPE, localizationContext);

  }

  /**
   * Validates the configured data disk size.
   *
   * @param configuration       the configuration to be validated
   * @param accumulator         the exception condition accumulator
   * @param localizationContext the localization context
   */
  void checkDataDiskSize(Configured configuration,
      PluginExceptionConditionAccumulator accumulator,
      LocalizationContext localizationContext) {

    String dataDiskSizeGBString = configuration.getConfigurationValue(DATA_DISK_SIZE_GB, localizationContext);

    if (dataDiskSizeGBString != null) {
      try {
        int dataDiskSizeGB = Integer.parseInt(dataDiskSizeGBString);
        if (dataDiskSizeGB < MIN_DATA_DISK_SIZE_GB) {
          addError(accumulator, DATA_DISK_SIZE_GB, localizationContext, null, INVALID_DATA_DISK_SIZE_MSG,
              MIN_DATA_DISK_SIZE_GB, dataDiskSizeGB);
        }
      } catch (NumberFormatException e) {
        addError(accumulator, DATA_DISK_SIZE_GB, localizationContext, null, INVALID_DATA_DISK_SIZE_FORMAT_MSG,
            dataDiskSizeGBString);
      }
    }
  }

  /**
   * Validates the configured machine type.
   *
   * @param configuration       the configuration to be validated
   * @param accumulator         the exception condition accumulator
   * @param localizationContext the localization context
   */
  /*void checkMachineType(Configured configuration,
      PluginExceptionConditionAccumulator accumulator,
      LocalizationContext localizationContext) {

    String type = configuration.getConfigurationValue(TYPE, localizationContext);

    // Machine types are a zonal resource. Only makes sense to check it if the zone itself is valid.
    Collection<PluginExceptionCondition> zoneErrors =
        accumulator.getConditionsByKey().get(ZONE.unwrap().getConfigKey());

    if (zoneErrors != null && zoneErrors.size() > 0) {
      LOG.info("Machine type '{}' not being checked since zone was not found.", type);

      return;
    }

    if (type != null) {
      LOG.info(">> Querying machine type '{}'", type);

      DimensionDataCredentials credentials = provider.getCredentials();
      Compute compute = credentials.getCompute();
      String projectId = credentials.getProjectId();
      String zoneName = configuration.getConfigurationValue(ZONE, localizationContext);

      try {
        compute.machineTypes().get(projectId, zoneName, type).execute();
      } catch (DimensiondataJsonResponseException e) {
        if (e.getStatusCode() == 404) {
          addError(accumulator, TYPE, localizationContext, null, MACHINE_TYPE_NOT_FOUND_IN_ZONE_MSG,
              type, zoneName, projectId);
        } else {
          throw new TransientProviderException(e);
        }
      } catch (IOException e) {
        throw new TransientProviderException(e);
      }
    }
  }*/

  /**
   * Validates the configured network domain.
   *
   * @param configuration       the configuration to be validated
   * @param accumulator         the exception condition accumulator
   * @param localizationContext the localization context
   */
  void checkNetworkDomain(Configured configuration,
      PluginExceptionConditionAccumulator accumulator,
      LocalizationContext localizationContext) {
	  
	String datacenterId = configuration.getConfigurationValue(DATACENTER, localizationContext);
    String networkName = configuration.getConfigurationValue(NETWORK_NAME, localizationContext);

    if (networkName != null) {
      LOG.info(">> Querying network '{}'", networkName);

      DimensionDataCredentials credentials = provider.getCredentials();
      Cloud compute = credentials.getCompute();
      
      try {
        DatacenterType datacenterType = compute.datacenter().getDatacenter(datacenterId);
        NetworkDomains networkDomains = compute.networkDomain().listNetworkDomains(20, 1, OrderBy.EMPTY, new Filter(
        	    											new Param("datacenterId", datacenterType.getId())));
        boolean found = false;
        for(NetworkDomainType network: networkDomains.getNetworkDomain()){
        	if(network.getName().equals(networkName)){
        		found = true;
        		break;
        	}
        }
        if(!found){
        	throw new NotFoundException();
        }
        
      }catch (ForbiddenException | InvalidParameterException | NotFoundException | RequestException | ServiceUnavailableException | UnauthorizedException  e) {
          if (e.getClass() == NotFoundException.class) {
        	  addError(accumulator, NETWORK_NAME, localizationContext, null, NETWORK_NOT_FOUND_MSG, networkName);
            } else {
              throw new TransientProviderException(e);
            }
          } catch (ClientRuntimeException e) {
            throw new TransientProviderException(e);
          } 
    }
  }
  
  /**
   * Validates the configured vlan.
   *
   * @param configuration       the configuration to be validated
   * @param accumulator         the exception condition accumulator
   * @param localizationContext the localization context
   */
  void checkVlan(Configured configuration,
      PluginExceptionConditionAccumulator accumulator,
      LocalizationContext localizationContext) {

	  String datacenterId = configuration.getConfigurationValue(DATACENTER, localizationContext);
	  String networkName = configuration.getConfigurationValue(NETWORK_NAME, localizationContext);
	  String vlanIp = configuration.getConfigurationValue(VLAN_IPV4, localizationContext);

    if (vlanIp != null) {
      LOG.info(">> Querying vlan '{}'", vlanIp);

      DimensionDataCredentials credentials = provider.getCredentials();
      Cloud compute = credentials.getCompute();
      
      try {
    	DatacenterType datacenterType = compute.datacenter().getDatacenter(datacenterId);
        NetworkDomains networkDomains = compute.networkDomain().listNetworkDomains(20, 1, OrderBy.EMPTY, new Filter(
          	    											new Param("datacenterId", datacenterType.getId())));
        boolean found = false;
        for(NetworkDomainType network: networkDomains.getNetworkDomain()){
        	if(network.getName().equals(networkName)){
        		Vlans vlans = compute.vlan().listVlans(20, 1, OrderBy.EMPTY, new Filter(
          	    											new Param("networkDomainId", network.getId())));
        		for(VlanType vlan: vlans.getVlan()){
        			if(vlan.getIpv4GatewayAddress().equals(vlanIp)){
        				found = true;
        				break;
        			}
        		}
        	}
        }
        
        if(!found){
        	throw new NotFoundException();
        }
      }catch (ForbiddenException | InvalidParameterException | NotFoundException | RequestException | ServiceUnavailableException | UnauthorizedException  e) {
          if (e.getClass() == NotFoundException.class) {
        	  addError(accumulator, VLAN_IPV4, localizationContext, null, VLAN_NOT_FOUND_MSG, vlanIp);
            } else {
              throw new TransientProviderException(e);
            }
          } catch (ClientRuntimeException e) {
            throw new TransientProviderException(e);
          } 
    }
  }

  /**
   * Validates the configured prefix.
   *
   * @param configuration       the configuration to be validated
   * @param accumulator         the exception condition accumulator
   * @param localizationContext the localization context
   */
  static void checkPrefix(Configured configuration,
      PluginExceptionConditionAccumulator accumulator,
      LocalizationContext localizationContext) {

    String instanceNamePrefix = configuration.getConfigurationValue(INSTANCE_NAME_PREFIX, localizationContext);

    LOG.info(">> Validating prefix '{}'", instanceNamePrefix);

    if (instanceNamePrefix == null) {
      addError(accumulator, INSTANCE_NAME_PREFIX, localizationContext, null, PREFIX_MISSING_MSG);
    } else {
      int length = instanceNamePrefix.length();

      if (length < 1 || length > 26) {
        addError(accumulator, INSTANCE_NAME_PREFIX, localizationContext, null, INVALID_PREFIX_LENGTH_MSG);
      } else if (!instanceNamePrefixPattern.matcher(instanceNamePrefix).matches()) {
        addError(accumulator, INSTANCE_NAME_PREFIX, localizationContext, null, INVALID_PREFIX_MSG);
      }
    }
  }
}