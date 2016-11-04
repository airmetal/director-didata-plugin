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

package com.cloudera.director.dimensiondata.internal;

import com.cloudera.director.dimensiondata.compute.util.ComputeUrls;
import com.dimensiondata.cloud.client.Cloud;
import com.dimensiondata.cloud.client.User;
import com.dimensiondata.cloud.client.UserSession;
import com.dimensiondata.cloud.client.http.CloudImpl;
import com.typesafe.config.Config;

public class DimensionDataCredentials {

  private final Config applicationProperties;
  private final User user;
  private final String username;
  private final String password; 
  private final String region; 
  private final Cloud compute;

  public DimensionDataCredentials(Config applicationProperties, String username, String password, String region) {
    this.applicationProperties = applicationProperties;
    this.username = username;
    this.password = password;
    this.region = region;
    this.user = new User(username,password);
    this.compute = buildCompute(this.user);

  }

  private Cloud buildCompute(User myUser) {
    try {
        UserSession.set(myUser);
    	if(!region.isEmpty()){
    		return new CloudImpl(ComputeUrls.buildDimensionDataComputeApisUrl(region));
    	}else{
    		return new CloudImpl(ComputeUrls.buildDimensionDataComputeApisUrl("dd-na"));
    	}
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public String getUserName() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public User getUser() {
    return user;
  }
  
  public Cloud getCompute() {
	    return compute;
  }
  
  public String getRegion() {
	  return region;
  }

}
