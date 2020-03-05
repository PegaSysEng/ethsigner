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
package tech.pegasys.ethsigner.tests.multikeysigner;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.ethsigner.tests.dsl.DockerClientFactory;
import tech.pegasys.ethsigner.tests.dsl.hashicorp.HashicorpNode;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HashicorpBasedTomlLoadingAcceptanceTest extends MultiKeyAcceptanceTestBase {

  static final String FILENAME = "fe3b557e8fb62b89f4916b721be55ceb828dbd73";
  static final String HASHICORP_ETHEREUM_ADDRESS = "0x" + FILENAME;

  private static HashicorpNode hashicorpNode;

  @BeforeAll
  static void setUpBase() {
    hashicorpNode =
        HashicorpNode.createAndStartHashicorp(new DockerClientFactory().create(), false);
  }

  @Test
  void hashicorpSignerIsCreatedAndExpectedAddressIsReported(@TempDir final Path tempDir) {
    createHashicorpTomlFileAt(tempDir.resolve(FILENAME + ".toml"), hashicorpNode);
    setup(tempDir);
    assertThat(ethSigner.accounts().list()).containsOnly(HASHICORP_ETHEREUM_ADDRESS);
  }

  @Test
  void incorrectlyNamedHashicorpConfigFileIsNotLoaded(@TempDir final Path tempDir) {
    createHashicorpTomlFileAt(
        tempDir.resolve("ffffffffffffffffffffffffffffffffffffffff.toml"), hashicorpNode);
    setup(tempDir);
    assertThat(ethSigner.accounts().list()).isEmpty();
  }

  @AfterAll
  static void tearDown() {
    hashicorpNode.shutdown();
  }
}
