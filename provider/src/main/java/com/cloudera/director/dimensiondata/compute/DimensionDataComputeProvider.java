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

import static com.cloudera.director.dimensiondata.compute.DimensionDataComputeInstanceTemplateConfigurationProperty.DATACENTER;
import static com.cloudera.director.dimensiondata.compute.DimensionDataComputeInstanceTemplateConfigurationProperty.IMAGE_NAME;
import static com.cloudera.director.dimensiondata.compute.DimensionDataComputeInstanceTemplateConfigurationProperty.NETWORK_NAME;
import static com.cloudera.director.dimensiondata.compute.DimensionDataComputeInstanceTemplateConfigurationProperty.TYPE;
import static com.cloudera.director.dimensiondata.compute.DimensionDataComputeInstanceTemplateConfigurationProperty.VLAN_IPV4;
import static com.cloudera.director.spi.v1.compute.ComputeInstanceTemplate.ComputeInstanceTemplateConfigurationPropertyToken.SSH_PASSWORD;
import static com.cloudera.director.spi.v1.compute.ComputeInstanceTemplate.ComputeInstanceTemplateConfigurationPropertyToken.SSH_USERNAME;
import static com.cloudera.director.spi.v1.model.InstanceTemplate.InstanceTemplateConfigurationPropertyToken.INSTANCE_NAME_PREFIX;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.director.dimensiondata.internal.DimensionDataCredentials;
import com.cloudera.director.spi.v1.compute.util.AbstractComputeProvider;
import com.cloudera.director.spi.v1.model.ConfigurationProperty;
import com.cloudera.director.spi.v1.model.ConfigurationValidator;
import com.cloudera.director.spi.v1.model.Configured;
import com.cloudera.director.spi.v1.model.InstanceState;
import com.cloudera.director.spi.v1.model.InstanceStatus;
import com.cloudera.director.spi.v1.model.LocalizationContext;
import com.cloudera.director.spi.v1.model.Resource;
import com.cloudera.director.spi.v1.model.exception.PluginExceptionConditionAccumulator;
import com.cloudera.director.spi.v1.model.exception.PluginExceptionDetails;
import com.cloudera.director.spi.v1.model.exception.UnrecoverableProviderException;
import com.cloudera.director.spi.v1.model.util.CompositeConfigurationValidator;
import com.cloudera.director.spi.v1.model.util.SimpleInstanceState;
import com.cloudera.director.spi.v1.model.util.SimpleResourceTemplate;
import com.cloudera.director.spi.v1.provider.ResourceProviderMetadata;
import com.cloudera.director.spi.v1.provider.util.SimpleResourceProviderMetadata;
import com.cloudera.director.spi.v1.util.ConfigurationPropertiesUtil;
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
import com.dimensiondata.cloud.client.model.DeployNetworkDomainType;
import com.dimensiondata.cloud.client.model.DeployServerType;
import com.dimensiondata.cloud.client.model.DeployVlanType;
import com.dimensiondata.cloud.client.model.NewNicType;
import com.dimensiondata.cloud.client.model.ResponseType;
import com.dimensiondata.cloud.client.model.ServerType;
import com.dimensiondata.cloud.client.model.Servers;
import com.google.common.collect.Lists;
import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.core.ConditionTimeoutException;
import com.typesafe.config.Config;

/**
 * Compute provider of DimensionData compute instances.
 */
public class DimensionDataComputeProvider
		extends AbstractComputeProvider<DimensionDataComputeInstance, DimensionDataComputeInstanceTemplate> {

	private static final Logger LOG = LoggerFactory.getLogger(DimensionDataComputeProvider.class);

	protected static final List<ConfigurationProperty> CONFIGURATION_PROPERTIES = ConfigurationPropertiesUtil
			.asConfigurationPropertyList(DimensionDataComputeProviderConfigurationProperty.values());

	/**
	 * The resource provider ID.
	 */
	public static final String ID = DimensionDataComputeProvider.class.getCanonicalName();

	public static final ResourceProviderMetadata METADATA = SimpleResourceProviderMetadata.builder().id(ID)
			.name("Dimension Data provider").description("Dimension Data Managed Cloud Platform - Compute")
			.providerClass(DimensionDataComputeProvider.class).providerConfigurationProperties(CONFIGURATION_PROPERTIES)
			.resourceTemplateConfigurationProperties(DimensionDataComputeInstanceTemplate.getConfigurationProperties())
			.resourceDisplayProperties(DimensionDataComputeInstance.getDisplayProperties()).build();

	private DimensionDataCredentials credentials;
	private Config applicationProperties;
	private Config dimensiondataConfig;

	private final ConfigurationValidator resourceTemplateConfigurationValidator;

	public DimensionDataComputeProvider(Configured configuration, DimensionDataCredentials credentials,
			Config applicationProperties, Config dimensiondataConfig, LocalizationContext cloudLocalizationContext) {
		super(configuration, METADATA, cloudLocalizationContext);

		this.credentials = credentials;
		this.applicationProperties = applicationProperties;
		this.dimensiondataConfig = dimensiondataConfig;

		Cloud compute = credentials.getCompute();
		// Verify credentials and connectivity
		try {
			compute.datacenter().listDatacenters(1, 1, OrderBy.EMPTY);
		} catch (ForbiddenException | InvalidParameterException | NotFoundException | RequestException
				| ServiceUnavailableException | UnauthorizedException e) {
			throw new ClientRuntimeException("Unable to list datacenters: " + e);
		}

		this.resourceTemplateConfigurationValidator = new CompositeConfigurationValidator(
				METADATA.getResourceTemplateConfigurationValidator(),
				new DimensionDataComputeInstanceTemplateConfigurationValidator(this));
	}

	@Override
	public ResourceProviderMetadata getProviderMetadata() {
		return METADATA;
	}

	@Override
	public ConfigurationValidator getResourceTemplateConfigurationValidator() {
		return resourceTemplateConfigurationValidator;
	}

	@Override
	public Resource.Type getResourceType() {
		return DimensionDataComputeInstance.TYPE;
	}

	@Override
	public DimensionDataComputeInstanceTemplate createResourceTemplate(String name, Configured configuration,
			Map<String, String> tags) {
		return new DimensionDataComputeInstanceTemplate(name, configuration, tags, getLocalizationContext());
	}

	@Override
	public void allocate(DimensionDataComputeInstanceTemplate template, Collection<String> instanceIds, int minCount)
			throws InterruptedException {

		LOG.info(">> Requesting {} instances for {}", minCount, template);

		PluginExceptionConditionAccumulator accumulator = new PluginExceptionConditionAccumulator();

		LocalizationContext providerLocalizationContext = getLocalizationContext();
		LocalizationContext templateLocalizationContext = SimpleResourceTemplate
				.getTemplateLocalizationContext(providerLocalizationContext);

		Cloud compute = credentials.getCompute();

		// Save the server instance ID's for polling
		List<ResponseType> serverInstanceIds = new ArrayList<ResponseType>();

		// Compose the network domain
		String networkDomainName = template.getConfigurationValue(NETWORK_NAME, templateLocalizationContext);
		String networkDomainType = template.getConfigurationValue(TYPE, templateLocalizationContext);
		String datacenter = template.getConfigurationValue(DATACENTER, templateLocalizationContext);

		DeployNetworkDomainType networkDomain = new DeployNetworkDomainType();
		networkDomain.setDatacenterId(datacenter);
		networkDomain.setType(networkDomainType);
		networkDomain.setName(networkDomainName);
		networkDomain.setDescription("Cloudera Director 2.1.1 dedicated network.");

		// Deploy the network domain
		ResponseType networkdomain = null;
		String networkDomainId = null;
		try {
			networkdomain = compute.networkDomain().deployNetworkDomain(networkDomain);
			networkDomainId = compute.networkDomain().getIdFromDeployResponse(networkdomain);

			LOG.info(">> Issue request to create network domain.");
		} catch (ForbiddenException | InvalidParameterException | NotFoundException | RequestException
				| ServiceUnavailableException | UnauthorizedException ec) {

			LOG.error(ec.getMessage());
		} catch (ConditionTimeoutException e) {
			LOG.error(e.getMessage());
		}

		// Compose the vlan.
		String vlanIp = template.getConfigurationValue(VLAN_IPV4, templateLocalizationContext);
		DeployVlanType vlanType = new DeployVlanType();
		vlanType.setNetworkDomainId(networkDomainId);
		vlanType.setName(networkDomainName+"_Vlan");
		vlanType.setPrivateIpv4BaseAddress(vlanIp);

		// Deploy Vlan
		ResponseType vlan = null;
		String vlanId = null;
		
		if (pollPendingOperations("Network Domain", networkDomainId, "NETWORK", compute, accumulator)) {
			LOG.info(">> Completed create network domain.");
			LOG.info(">> Start creating Vlan.");
			try {
				vlan = compute.vlan().deployVlan(vlanType);
				vlanId = compute.vlan().getIdFromDeployResponse(vlan);
			} catch (ForbiddenException | InvalidParameterException | NotFoundException | RequestException
					| ServiceUnavailableException | UnauthorizedException ec) {
				ec.printStackTrace();
			} catch (Exception c) {
				c.printStackTrace();
			}
		}
		
		if (pollPendingOperations("Vlan", vlanId, "VLAN", compute, accumulator)) {
			// Compose the instance metadata containing the SSH user name,
			// password and tags.
			LOG.info(">> Completed creating the vlan.");
			//List<Metadata.Items> metadataItemsList = new ArrayList<Metadata.Items>();

			String sshUserName = template.getConfigurationValue(SSH_USERNAME, templateLocalizationContext);
			String sshPassword = template.getConfigurationValue(SSH_PASSWORD, templateLocalizationContext);

			/*for (Map.Entry<String, String> tag : template.getTags().entrySet()) {
				metadataItemsList.add(new Metadata.Items().setKey(tag.getKey()).setValue(tag.getValue()));
			}*/

			for (String instanceId : instanceIds) {
				LOG.info(">> Start creating server instance: "+ instanceId);
				String decoratedInstanceName = decorateInstanceName(template, instanceId, templateLocalizationContext);

				if (sshUserName != null && !sshUserName.isEmpty() && sshPassword != null && !sshPassword.isEmpty()) {
					String sshKeysValue = sshUserName + ":" + sshPassword;

					//metadataItemsList.add(new Metadata.Items().setKey("sshKeys").setValue(sshKeysValue));
				} else {
					LOG.info("SSH credentials not set on instance '{}'. ", decoratedInstanceName);
				}

				// Retrieve the source image.
				String sourceImage = template.getConfigurationValue(IMAGE_NAME, templateLocalizationContext);

				// Compose the instance.
				DeployServerType serverType = new DeployServerType();
				serverType.setName(decoratedInstanceName);
				serverType.setStart(Boolean.TRUE);
				serverType.setImageId(sourceImage);
				serverType.setMemoryGb((long) 32);
				DeployServerType.NetworkInfo networkInfoType = new DeployServerType.NetworkInfo();
				networkInfoType.setNetworkDomainId(networkDomainId);
				NewNicType newNicType = new NewNicType();
				newNicType.setVlanId(vlanId);
				networkInfoType.setPrimaryNic(newNicType);
				serverType.setNetworkInfo(networkInfoType);
				serverType.setAdministratorPassword(sshPassword);
				DeployServerType.Cpu cpuType = new DeployServerType.Cpu();
				cpuType.setCount((long) 4);
				serverType.setCpu(cpuType);

				// Fire off a request
				ResponseType server = compute.server().deployServer(serverType);
				pollPendingOperations("Server", networkDomainId, "SERVER", compute, accumulator);
				// Save Response
				serverInstanceIds.add(server);
				LOG.info(">> Completed creating server instance: "+ instanceId);
			}
		}
/*		// Clone a copy of all instances
		List<ResponseType> successfulServers = new ArrayList<ResponseType>(serverInstanceIds);

		// Poll the server list in reverse order, since the last request will
		// take the longest it might be more efficient.
		for (ResponseType serverInstanceId : Lists.reverse(serverInstanceIds)) {
			String id = compute.server().getIdFromDeployResponse((serverInstanceId));
			if (serverInstanceId.getResponseCode().equalsIgnoreCase("200")) {

			} else {
				successfulServers.remove(serverInstanceId);
			}
		}

		int successfulServersCount = successfulServers.size();

		if (successfulServersCount < minCount) {
			LOG.info("Provisioned {} instances out of {}. minCount is {}. Tearing down provisioned instances.",
					successfulServersCount, instanceIds.size(), minCount);

			tearDownResources(serverInstanceIds, compute, accumulator);

			PluginExceptionDetails pluginExceptionDetails = new PluginExceptionDetails(
					accumulator.getConditionsByKey());
			throw new UnrecoverableProviderException("Problem allocating instances.", pluginExceptionDetails);
		} else if (successfulServersCount < instanceIds.size()) {
			LOG.info("Provisioned {} instances out of {}. minCount is {}.", successfulServersCount, instanceIds.size(),
					minCount);*/

			// Even through we are not throwing an exception, we still want to
			// log the errors.
			/*
			 * if (accumulator.hasError()) { Map<String,
			 * Collection<PluginExceptionCondition>> conditionsByKeyMap =
			 * accumulator.getConditionsByKey();
			 * 
			 * for (Map.Entry<String, Collection<PluginExceptionCondition>>
			 * keyToCondition : conditionsByKeyMap.entrySet()) { String key =
			 * keyToCondition.getKey();
			 * 
			 * if (key != null) { for (PluginExceptionCondition condition :
			 * keyToCondition.getValue()) { LOG.info("({}) {}: {}",
			 * condition.getType(), key, condition.getMessage()); } } else { for
			 * (PluginExceptionCondition condition : keyToCondition.getValue())
			 * { LOG.info("({}) {}", condition.getType(),
			 * condition.getMessage()); } } } }
			 */
	//	}

		// TODO
		/*
		 * // Compose the tags for the instance, including a tag identifying the
		 * plugin and version used to create it. // This is not the same as the
		 * template 'tags' which are propagated as instance metadata. String
		 * applicationNameVersionTag =
		 * Names.buildApplicationNameVersionTag(applicationProperties); //
		 * Massage it into a form acceptable for use as a tag (only allows
		 * lowercase letters, numbers and hyphens). applicationNameVersionTag =
		 * applicationNameVersionTag.toLowerCase().replaceAll("\\.|/", "-");
		 */
	}

	// Delete all persistent disks and instances.
	private void tearDownResources(List<ResponseType> vmOperationResponses, Cloud compute,
			PluginExceptionConditionAccumulator accumulator) throws InterruptedException {

		// Use this list to keep track of all instance deletion operations.
		List<ResponseType> tearDownOperations = new ArrayList<ResponseType>();

		// Iterate over each instance creation operation.
		for (ResponseType vmOperation : vmOperationResponses) {
			String serverId = compute.server().getIdFromDeployResponse(vmOperation);
			ResponseType tearDownOperation = compute.server().deleteServer(serverId);
			try {
				tearDownOperations.add(tearDownOperation);
			} catch (ForbiddenException | InvalidParameterException | NotFoundException | RequestException
					| ServiceUnavailableException | UnauthorizedException e) {
				if ((tearDownOperation.getResponseCode().equalsIgnoreCase("404"))) {
					// Since we try to tear down all instances, and some may not
					// have been successfully provisioned in the first
					// place, we don't need to propagate this.
				} else {
					accumulator.addError(null, e.getMessage());
				}
			}
		}

		List<ResponseType> successfulTearDownOperations = new ArrayList<ResponseType>(tearDownOperations);
		for (ResponseType tearDownOperation : Lists.reverse(tearDownOperations)) {
			String serverId = compute.server().getIdFromDeployResponse(tearDownOperation);
			if (tearDownOperation.getResponseCode().equalsIgnoreCase("200")) {
				Awaitility.await().atMost(5, TimeUnit.MINUTES).pollDelay(30, TimeUnit.SECONDS)
						.until(compute.server().isServerDeleted(serverId));
			} else {
				successfulTearDownOperations.remove(tearDownOperation);
			}
		}

		int tearDownOperationCount = tearDownOperations.size();
		int successfulTearDownOperationCount = successfulTearDownOperations.size();

		if (successfulTearDownOperationCount < tearDownOperationCount) {
			accumulator.addError(null, successfulTearDownOperationCount + " of the " + tearDownOperationCount
					+ " tear down operations completed successfully.");
		}
	}

	@Override
	public Collection<DimensionDataComputeInstance> find(DimensionDataComputeInstanceTemplate template,
			Collection<String> instanceIds) throws InterruptedException {
		LocalizationContext providerLocalizationContext = getLocalizationContext();
		LocalizationContext templateLocalizationContext = SimpleResourceTemplate
				.getTemplateLocalizationContext(providerLocalizationContext);

		List<DimensionDataComputeInstance> result = new ArrayList<DimensionDataComputeInstance>();

		// If the prefix is not valid, there is no way the instances could have
		// been created in the first place.
		if (!isPrefixValid(template, templateLocalizationContext)) {
			return result;
		}

		for (String currentId : instanceIds) {
			Cloud compute = credentials.getCompute();
			String decoratedInstanceName = decorateInstanceName(template, currentId, templateLocalizationContext);

			try {
				ServerType instance = compute.server().getServer(currentId);

				result.add(new DimensionDataComputeInstance(template, currentId, instance));
			} catch (ForbiddenException | InvalidParameterException | NotFoundException | RequestException
					| ServiceUnavailableException | UnauthorizedException e) {
				if (e.getClass() == NotFoundException.class) {
					LOG.info("Instance '{}' not found.", decoratedInstanceName);
				} else {
					throw new RuntimeException(e);
				}
			}
		}
		LOG.info("Found {} instances for {} virtual instance IDs", result.size(), instanceIds.size());
		return result;
	}

	@Override
	public Map<String, InstanceState> getInstanceState(DimensionDataComputeInstanceTemplate template,
			Collection<String> instanceIds) {
		LocalizationContext providerLocalizationContext = getLocalizationContext();
		LocalizationContext templateLocalizationContext = SimpleResourceTemplate
				.getTemplateLocalizationContext(providerLocalizationContext);

		Map<String, InstanceState> result = new HashMap<String, InstanceState>();

		// If the prefix is not valid, there is no way the instances could have
		// been created in the first place.
		if (!isPrefixValid(template, templateLocalizationContext)) {
			for (String currentId : instanceIds) {
				result.put(currentId, new SimpleInstanceState(InstanceStatus.UNKNOWN));
			}
		} else {
			for (String currentId : instanceIds) {
				LOG.info("Searching for instance status for: "+currentId);
				
				Cloud compute = credentials.getCompute();
				String decoratedInstanceName = decorateInstanceName(template, currentId, templateLocalizationContext);

				try {
					Servers server = compute.server().listServers(1, 1, OrderBy.EMPTY, new Filter(new Param("name",template.getConfigurationValue(INSTANCE_NAME_PREFIX, templateLocalizationContext)+"-"+currentId)));
					ServerType instance = server.getServer().get(0);
					InstanceStatus instanceStatus = null;
					LOG.info("Instance state: "+instance.getState());
					if (instance.getState().equals("NORMAL")) {
						if (instance.isStarted()) {
							instanceStatus = convertMCPInstanceStatusToDirectorInstanceStatus("RUNNING");
						} else {
							instanceStatus = convertMCPInstanceStatusToDirectorInstanceStatus("TERMINATED");
						}
					} else {
						instanceStatus = convertMCPInstanceStatusToDirectorInstanceStatus(instance.getState());
					}
					result.put(currentId, new SimpleInstanceState(instanceStatus));
				} catch (NotFoundException e) {
					LOG.info("Instance '{}' not found.", decoratedInstanceName);
					result.put(currentId, new SimpleInstanceState(InstanceStatus.DELETED));
				} catch (Exception e) {
					LOG.info("Instance '{}' not found due to unknown issue.", decoratedInstanceName);
					result.put(currentId, new SimpleInstanceState(InstanceStatus.UNKNOWN));
				}
			}
		}
		return result;
	}

	@Override
	public void delete(DimensionDataComputeInstanceTemplate template, Collection<String> instanceIds)
			throws InterruptedException {

		PluginExceptionConditionAccumulator accumulator = new PluginExceptionConditionAccumulator();

		LocalizationContext providerLocalizationContext = getLocalizationContext();
		LocalizationContext templateLocalizationContext = SimpleResourceTemplate
				.getTemplateLocalizationContext(providerLocalizationContext);

		// If the prefix is not valid, there is no way the instances could have
		// been created in the first place.
		// So we shouldn't attempt to delete them, but we also shouldn't report
		// an error.
		if (!isPrefixValid(template, templateLocalizationContext)) {
			return;
		}

		Cloud compute = credentials.getCompute();

		// Use this list to collect the operations that must reach a RUNNING or
		// DEPLOYED state prior to delete() returning.
		List<ResponseType> vmDeletionOperations = new ArrayList<ResponseType>();

		for (String currentId : instanceIds) {
			String decoratedInstanceName = decorateInstanceName(template, currentId, templateLocalizationContext);

			try {
				ResponseType vmDeletionOperation = compute.server().deleteServer(currentId);

				vmDeletionOperations.add(vmDeletionOperation);
			} catch (NotFoundException e) {
				LOG.info("Attempted to delete instance '{}', but it does not exist.", decoratedInstanceName);
			} catch (Exception e) {
				accumulator.addError(null, e.getMessage());
			}
		}

		for (ResponseType tearDownOperation : Lists.reverse(vmDeletionOperations)) {
			String serverId = compute.server().getIdFromDeployResponse(tearDownOperation);
			if (tearDownOperation.getResponseCode().equalsIgnoreCase("200")) {
				Awaitility.await().atMost(5, TimeUnit.MINUTES).pollDelay(30, TimeUnit.SECONDS)
						.until(compute.server().isServerDeleted(serverId));
			} else {
				vmDeletionOperations.remove(tearDownOperation);
			}
		}

		if (accumulator.hasError()) {
			PluginExceptionDetails pluginExceptionDetails = new PluginExceptionDetails(
					accumulator.getConditionsByKey());
			throw new UnrecoverableProviderException("Problem deleting instances.", pluginExceptionDetails);
		}
	}

	public DimensionDataCredentials getCredentials() {
		return credentials;
	}

	public Config getDimensionDataConfig() {
		return dimensiondataConfig;
	}

	private static String decorateInstanceName(DimensionDataComputeInstanceTemplate template, String currentId,
			LocalizationContext templateLocalizationContext) {
		return template.getConfigurationValue(INSTANCE_NAME_PREFIX, templateLocalizationContext) + "-" + currentId;
	}

	/*
	 * MCP Server Status values NORMAL", "PENDING_ADD", "PENDING_CHANGE", "
	 * PENDING_DELETE", "FAILED_ADD", "FAILED_CHANGE", "FAILED_DELETE" and "
	 * REQUIRES_SUPPORT".
	 */
	private static InstanceStatus convertMCPInstanceStatusToDirectorInstanceStatus(String mcpInstanceStatus) {

		if (mcpInstanceStatus.equals("PENDING_ADD") || mcpInstanceStatus.equals("PENDING_CHANGE")) {
			return InstanceStatus.PENDING;
		} else if (mcpInstanceStatus.equals("RUNNING")) {
			return InstanceStatus.RUNNING;
		} else if (mcpInstanceStatus.equals("STOPPING")) {
			return InstanceStatus.STOPPING;
		} else if (mcpInstanceStatus.equals("TERMINATED")) {
			return InstanceStatus.STOPPED;
		} else if (mcpInstanceStatus.equals("PENDING_DELETE")) {
			return InstanceStatus.DELETING;
		} else if (mcpInstanceStatus.equals("FAILED_ADD") || mcpInstanceStatus.equals("FAILED_CHANGE")
				|| mcpInstanceStatus.equals("FAILED_DELETE")) {
			return InstanceStatus.FAILED;
		} else {
			return InstanceStatus.UNKNOWN;
		}
	}

	private boolean isPrefixValid(DimensionDataComputeInstanceTemplate template,
			LocalizationContext templateLocalizationContext) {
		PluginExceptionConditionAccumulator accumulator = new PluginExceptionConditionAccumulator();

		DimensionDataComputeInstanceTemplateConfigurationValidator.checkPrefix(template, accumulator,
				templateLocalizationContext);

		boolean isValid = accumulator.getConditionsByKey().isEmpty();

		if (!isValid) {
			LOG.info("Instance name prefix '{}' is invalid.",
					template.getConfigurationValue(INSTANCE_NAME_PREFIX, templateLocalizationContext));
		}

		return isValid;
	}

	private static boolean pollPendingOperations(String awaitName, String resourceId, String resourceType,
			Cloud compute, PluginExceptionConditionAccumulator accumulator) throws InterruptedException {

		int totalTimePollingSeconds = 0;
		int pollingTimeoutSeconds = 180;
		int maxPollingIntervalSeconds = 8;
		boolean timeoutExceeded = false;

		// Fibonacci backoff in seconds, up to maxPollingIntervalSeconds
		// interval.
		int pollInterval = 1;
		int pollIncrement = 0;

		// Use this list to keep track of each operation that reached one of the
		// acceptable states.
		List<String> successfulOperations = new ArrayList<String>();

		while (!timeoutExceeded) {
			
			Thread.sleep(pollInterval * 1000);
			totalTimePollingSeconds += pollInterval;

			switch (resourceType) {
			case ("NETWORK"):
				LOG.info("Checking network domain Status");
				try {
					String mcpState = compute.networkDomain().getState(resourceId);
					if (mcpState.equals("NORMAL")) {
						return true;
					}
				} catch (ForbiddenException | InvalidParameterException | NotFoundException | RequestException
						| ServiceUnavailableException | UnauthorizedException e) {
					accumulator.addError(null, e.getMessage());
					return false;
				}
				break;
			case ("VLAN"):
				LOG.info("Checking vlan  Status");
				try {
					String mcpState = compute.vlan().getState(resourceId);
					if (mcpState.equals("NORMAL")) {
						return true;
					}
				} catch (ForbiddenException | InvalidParameterException | NotFoundException | RequestException
						| ServiceUnavailableException | UnauthorizedException e) {
					accumulator.addError(null, e.getMessage());
					return false;
				}
				break;
			case ("SERVER"):
				LOG.info("Checking server Status");
				try {
					String mcpState = compute.server().getServer(resourceId).getState();
					if (convertMCPInstanceStatusToDirectorInstanceStatus(mcpState).equals(InstanceStatus.RUNNING)) {
						return true;
					}
				} catch (ForbiddenException | InvalidParameterException | NotFoundException | RequestException
						| ServiceUnavailableException | UnauthorizedException e) {
					accumulator.addError(null, e.getMessage());
					return false;
				}
				break;
			}

			if (totalTimePollingSeconds > pollingTimeoutSeconds) {
				accumulator.addError(null, "Exceeded timeout of '" + pollingTimeoutSeconds + "' seconds while "
						+ "polling for pending operations to complete. ");

				timeoutExceeded = true;
			} else {
				// Update polling interval.
				int oldIncrement = pollIncrement;
				pollIncrement = pollInterval;
				pollInterval += oldIncrement;
				pollInterval = Math.min(pollInterval, maxPollingIntervalSeconds);
			}
		}
		return false;
	}

}
