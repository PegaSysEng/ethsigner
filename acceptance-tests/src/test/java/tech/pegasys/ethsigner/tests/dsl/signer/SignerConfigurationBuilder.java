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
package tech.pegasys.ethsigner.tests.dsl.signer;

public class SignerConfigurationBuilder {

  /** ChainId defined in the Pantheon dev mode genesis. */
  private static final String CHAIN_ID = "2018";

  private static final String LOCALHOST = "127.0.0.1";

  private String chainId = CHAIN_ID;

  public SignerConfigurationBuilder withChainId(final String chainId) {
    this.chainId = chainId;
    return this;
  }

  public SignerConfiguration build() {
    return new SignerConfiguration(chainId, LOCALHOST);
  }
}
