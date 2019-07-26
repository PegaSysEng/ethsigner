/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EeaUtilsTest {

  @Test
  void createsPrivacyGroupIdWithSamePrivateFromAndPrivateFor() {
    final PrivacyIdentifier privateFrom =
        PrivacyIdentifier.fromBase64String("negmDcN2P4ODpqn/6WkJ02zT/0w0bjhGpkZ8UP6vARk=");
    final PrivacyIdentifier privateFor =
        PrivacyIdentifier.fromBase64String("negmDcN2P4ODpqn/6WkJ02zT/0w0bjhGpkZ8UP6vARk=");

    final String privacyGroupId =
        EeaUtils.generatePrivacyGroupId(privateFrom, singletonList(privateFor));
    assertThat(privacyGroupId).isEqualTo("kAbelwaVW7okoEn1+okO+AbA4Hhz/7DaCOWVQz9nx5M=");
  }
}