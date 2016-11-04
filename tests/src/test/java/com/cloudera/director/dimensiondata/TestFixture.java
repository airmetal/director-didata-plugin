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

import java.io.IOException;

public final class TestFixture {

  private String password;
  private String sshusername;
  private String username;
  private String sshpassword;
  private boolean haltAfterAllocation;

  private TestFixture(
      String username, String password, String sshusername, String sshpassword, boolean haltAfterAllocation) {
    this.username = username;
    this.password = password;
    this.sshusername = sshusername;
    this.sshpassword = sshpassword;
    this.haltAfterAllocation = haltAfterAllocation;
  }


public static TestFixture newTestFixture(boolean sshUsernameAndPasswordAreRequired) throws IOException {
    String username = TestUtils.readRequiredSystemProperty("USERNAME");
    String password = TestUtils.readRequiredSystemProperty("PASSWORD");

    String sshUsername = null;
    String sshPassword = null;
    if (sshUsernameAndPasswordAreRequired) {
      sshUsername = TestUtils.readRequiredSystemProperty("SSH_USER_NAME");
      sshPassword = TestUtils.readRequiredSystemProperty("SSH_PASSWORD");
    }

    boolean haltAfterAllocation = Boolean.parseBoolean(System.getProperty("HALT_AFTER_ALLOCATION", "false"));

    return new TestFixture(username, password , sshUsername, sshPassword, haltAfterAllocation);
  }

public String getPassword() {
	return password;
}

public String getSshusername() {
	return sshusername;
}

public String getUsername() {
	return username;
}

public String getSshpassword() {
	return sshpassword;
}

public boolean getHaltAfterAllocation() {
    return haltAfterAllocation;
  }
}
