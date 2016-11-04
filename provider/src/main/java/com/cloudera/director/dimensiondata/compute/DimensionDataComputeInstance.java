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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.director.spi.v1.compute.util.AbstractComputeInstance;
import com.cloudera.director.spi.v1.model.DisplayProperty;
import com.cloudera.director.spi.v1.model.DisplayPropertyToken;
import com.cloudera.director.spi.v1.model.Instance;
import com.cloudera.director.spi.v1.model.util.SimpleDisplayPropertyBuilder;
import com.cloudera.director.spi.v1.util.DisplayPropertiesUtil;
import com.dimensiondata.cloud.client.model.ServerType;

/**
 * Dimension Data instance.
 */
public class DimensionDataComputeInstance
    extends AbstractComputeInstance<DimensionDataComputeInstanceTemplate, ServerType> {

  private static final Logger LOG = LoggerFactory.getLogger(DimensionDataComputeInstance.class);

  /**
   * The list of display properties (including inherited properties).
   */
  private static final List<DisplayProperty> DISPLAY_PROPERTIES =
      DisplayPropertiesUtil.asDisplayPropertyList(DimensionDataComputeInstanceDisplayPropertyToken.values());

  /**
   * Returns the list of display properties for a Dimension Data instance, including inherited properties.
   *
   * @return the list of display properties for a Dimension Data, including inherited properties
   */
  public static List<DisplayProperty> getDisplayProperties() {
    return DISPLAY_PROPERTIES;
  }

  /**
   * Dimension Data server display properties.
   */
  public enum DimensionDataComputeInstanceDisplayPropertyToken implements DisplayPropertyToken {

    IMAGE_ID(new SimpleDisplayPropertyBuilder()
        .displayKey("imageId")
        .name("Image ID")
        .defaultDescription("The ID of the image used to launch the instance.")
        .sensitive(false)
        .build()) {
      @Override
      protected String getPropertyValue(ServerType instance) {
        return instance.getSourceImageId();
      }
    },

    /**
     * The ID of the instance.
     */
    INSTANCE_ID(new SimpleDisplayPropertyBuilder()
        .displayKey("instanceId")
        .name("Instance ID")
        .defaultDescription("The ID of the instance.")
        .sensitive(false)
        .build()) {
      @Override
      protected String getPropertyValue(ServerType instance) {
        return instance.getId();
      }
    },

    /**
     * The instance type.
     */
    INSTANCE_TYPE(new SimpleDisplayPropertyBuilder()
        .displayKey("instanceType")
        .name("Machine type")
        .defaultDescription("The instance type.")
        .sensitive(false)
        .build()) {
      @Override
      protected String getPropertyValue(ServerType instance) {
        return instance.getName();
      }
    },

    /**
     * The time the instance was launched.
     */
    LAUNCH_TIME(new SimpleDisplayPropertyBuilder()
        .displayKey("launchTime")
        .name("Launch time")
        .defaultDescription("The time the instance was launched.")
        .sensitive(false)
        .build()) {
      @Override
      protected String getPropertyValue(ServerType instance) {
        return instance.getCreateTime().toString();
      }
    },

    /**
     * The private IP address assigned to the instance.
     */
    PRIVATE_IP_ADDRESS(new SimpleDisplayPropertyBuilder()
        .displayKey("privateIpAddress")
        .name("Internal IP")
        .defaultDescription("The private IP address assigned to the instance.")
        .sensitive(false)
        .build()) {
      @Override
      protected String getPropertyValue(ServerType instance) {
         if(instance != null){
        	 return instance.getNic().getPrivateIpv4();
         }else{
        	 return null;
         }
      }
    };

    /**
     * The public IP address assigned to the instance.
     */
    /*PUBLIC_IP_ADDRESS(new SimpleDisplayPropertyBuilder()
        .displayKey("publicIpAddress")
        .name("External IP")
        .defaultDescription("The public IP address assigned to the instance.")
        .sensitive(false)
        .build()) {
      @Override
      protected String getPropertyValue(ServerType instance) {
        
      }
    };*/

    /**
     * The display property.
     */
    private final DisplayProperty displayProperty;

    /**
     * Creates a Dimension Data instance display property token with the specified parameters.
     *
     * @param displayProperty the display property
     */
    DimensionDataComputeInstanceDisplayPropertyToken(DisplayProperty displayProperty) {
      this.displayProperty = displayProperty;
    }

    /**
     * Returns the value of the property from the specified instance.
     *
     * @param instance the instance
     * @return the value of the property from the specified instance
     */
    protected abstract String getPropertyValue(ServerType instance);

    @Override
    public DisplayProperty unwrap() {
      return displayProperty;
    }
  }

  public static final Type TYPE = new ResourceType("DimensionDataComputeInstance");

  /**
   * Creates a Dimension Data compute instance with the specified parameters.
   *
   * @param template        the template from which the instance was created
   * @param instanceId      the instance identifier
   * @param instanceDetails the provider-specific instance details
   * @throws IllegalArgumentException if the instance does not have a valid private IP address
   */
  protected DimensionDataComputeInstance(DimensionDataComputeInstanceTemplate template,
      String instanceId, ServerType instanceDetails) {
    super(template, instanceId, getPrivateIpAddress(instanceDetails), null, instanceDetails);
  }

  /**
   * Returns the private IP address of the specified Google instance.
   *
   * @param instance the instance
   * @return the private IP address of the specified Google instance
   * @throws IllegalArgumentException if the instance does not have a valid private IP address
   */
  private static InetAddress getPrivateIpAddress(ServerType instance) {
      try {
        return InetAddress.getByAddress(instance.getNic().getPrivateIpv4().getBytes());
      } catch (UnknownHostException e) {
        throw new IllegalArgumentException("Invalid private IP address", e);
      }
  }

  @Override
  public Type getType() {
    return TYPE;
  }


  @Override
  public Map<String, String> getProperties() {
    Map<String, String> properties = new HashMap<String, String>();
    ServerType instance = unwrap();

    if (instance != null) {
      for (DimensionDataComputeInstanceDisplayPropertyToken propertyToken : DimensionDataComputeInstanceDisplayPropertyToken.values()) {
        properties.put(propertyToken.unwrap().getDisplayKey(), propertyToken.getPropertyValue(instance));
      }
    }

    return properties;
  }

  /**
   * Sets the Dimension Data instance.
   *
   * @param instance the Dimension Data MCP instance
   * @throws IllegalArgumentException if the instance does not have a valid private IP address
   */
  protected void setInstance(ServerType instance) {
    super.setDetails(instance);

    InetAddress privateIpAddress = getPrivateIpAddress(instance);
    setPrivateIpAddress(privateIpAddress);
  }
}
