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
package tech.pegasys.ethsigner.tests;

import static java.nio.charset.StandardCharsets.UTF_8;

import tech.pegasys.ethsigner.tests.dsl.Account;
import tech.pegasys.ethsigner.tests.dsl.DockerClientFactory;
import tech.pegasys.ethsigner.tests.dsl.node.Node;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfiguration;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfigurationBuilder;
import tech.pegasys.ethsigner.tests.dsl.node.PantheonNode;
import tech.pegasys.ethsigner.tests.dsl.signer.Signer;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfiguration;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfigurationBuilder;

import java.io.IOException;
import java.net.URL;

import com.github.dockerjava.api.DockerClient;
import com.google.common.io.Resources;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class AcceptanceTestBase {

  private static String ENCLAVE_PUBLIC_KEY_FILENAME = "enclave_key.pub";

  private static Node ethNode;
  private static Signer ethSigner;

  protected Account richBenefactor() {
    return ethSigner.accounts().richBenefactor();
  }

  protected Signer ethSigner() {
    return ethSigner;
  }

  protected Node ethNode() {
    return ethNode;
  }

  protected String enclavePublicKey() {
    return readResource(ENCLAVE_PUBLIC_KEY_FILENAME);
  }

  @SuppressWarnings("UnstableApiUsage")
  private String readResource(final String resourceName) {
    final URL resource = Resources.getResource(resourceName);
    try {
      return new String(Resources.toString(resource, UTF_8).getBytes(UTF_8), UTF_8);
    } catch (final IOException e) {
      throw new RuntimeException("Unable to load resource " + resourceName);
    }
  }

  @BeforeAll
  public static void setUpBase() {
    Runtime.getRuntime().addShutdownHook(new Thread(AcceptanceTestBase::tearDownBase));

    final DockerClient docker = new DockerClientFactory().create();
    final NodeConfiguration nodeConfig = new NodeConfigurationBuilder().build();
    final SignerConfiguration signerConfig = new SignerConfigurationBuilder().build();

    ethNode = new PantheonNode(docker, nodeConfig);
    ethNode.start();
    ethNode.awaitStartupCompletion();

    ethSigner = new Signer(signerConfig, nodeConfig, ethNode.ports());
    ethSigner.start();
    ethSigner.awaitStartupCompletion();
  }

  @AfterAll
  public static void tearDownBase() {
    if (ethNode != null) {
      ethNode.shutdown();
    }

    if (ethSigner != null) {
      ethSigner.shutdown();
    }
  }
}
