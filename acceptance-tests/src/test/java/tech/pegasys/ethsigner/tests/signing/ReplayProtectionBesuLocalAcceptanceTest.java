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
package tech.pegasys.ethsigner.tests.signing;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.REPLAY_PROTECTED_SIGNATURES_NOT_SUPPORTED;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.WRONG_CHAIN_ID;
import static tech.pegasys.ethsigner.tests.dsl.Gas.GAS_PRICE;
import static tech.pegasys.ethsigner.tests.dsl.Gas.INTRINSIC_GAS;

import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethsigner.tests.dsl.Account;
import tech.pegasys.ethsigner.tests.dsl.node.besu.BesuLocalNode;
import tech.pegasys.ethsigner.tests.dsl.node.besu.BesuLocalNodeFactory;
import tech.pegasys.ethsigner.tests.dsl.node.besu.BesuNodeConfig;
import tech.pegasys.ethsigner.tests.dsl.node.besu.BesuNodeConfigBuilder;
import tech.pegasys.ethsigner.tests.dsl.signer.Signer;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfiguration;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerResponse;

import java.math.BigInteger;

import org.apache.commons.lang.time.StopWatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

public class ReplayProtectionBesuLocalAcceptanceTest {

  private static final String RECIPIENT = "0x1b00ba00ca00bb00aa00bc00be00ac00ca00da00";
  private static final BigInteger TRANSFER_AMOUNT_WEI =
      Convert.toWei("1.75", Unit.ETHER).toBigIntegerExact();

  private BesuLocalNode ethNode;
  private Signer ethSigner;

  private Account richBenefactor() {
    return ethSigner.accounts().richBenefactor();
  }

  @BeforeEach
  public void setUp() {
    // Runtime.getRuntime().addShutdownHook(new Thread((this::tearDown)));
  }

  @AfterEach
  public synchronized void tearDown() {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    System.out.println("Starting TearDown");
    if (ethNode != null) {
      ethNode.shutdown();
      ethNode = null;
    }

    if (ethSigner != null) {
      ethSigner.shutdown();
      ethSigner = null;
    }

    stopWatch.stop();
    System.out.println("Ending Starting TearDown: " + stopWatch);
  }

  private void setUp(final String genesis) {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    //    final NodeConfiguration nodeConfig =
    //        new NodeConfigurationBuilder().withGenesis(genesis).build();
    final BesuNodeConfig besuNodeConfig =
        BesuNodeConfigBuilder.aBesuNodeConfig().withName("test").withGenesisFile(genesis).build();

    final SignerConfiguration signerConfig = new SignerConfigurationBuilder().build();

    ethNode = new BesuLocalNodeFactory().create(besuNodeConfig);

    // ethNode = new BesuDockerNode(DOCKER, nodeConfig);
    ethNode.start();
    ethNode.awaitStartupCompletion();

    stopWatch.split();

    ethSigner = new Signer(signerConfig, "127.0.0.1", ethNode.ports());
    ethSigner.start();
    ethSigner.awaitStartupCompletion();

    System.out.println("Besu Node Time Taken: " + stopWatch.toSplitString());
    stopWatch.split();
    System.out.println("EthSigner Time Taken: " + stopWatch.toSplitString());

    stopWatch.stop();
    System.out.println("Total Time taken: " + stopWatch.toString());
  }

  @Test
  public void wrongChainId() {
    setUp("eth_hash_4404.json");

    final SignerResponse<JsonRpcErrorResponse> signerResponse =
        ethSigner
            .transactions()
            .submitExceptional(
                Transaction.createEtherTransaction(
                    richBenefactor().address(),
                    richBenefactor().nextNonceAndIncrement(),
                    GAS_PRICE,
                    INTRINSIC_GAS,
                    RECIPIENT,
                    TRANSFER_AMOUNT_WEI));

    assertThat(signerResponse.status()).isEqualTo(BAD_REQUEST);
    assertThat(signerResponse.jsonRpc().getError()).isEqualTo(WRONG_CHAIN_ID);
  }

  @Test
  public void unnecessaryChainId() {
    setUp("eth_hash_2018_no_replay_protection.json");

    final SignerResponse<JsonRpcErrorResponse> signerResponse =
        ethSigner
            .transactions()
            .submitExceptional(
                Transaction.createEtherTransaction(
                    richBenefactor().address(),
                    richBenefactor().nextNonceAndIncrement(),
                    GAS_PRICE,
                    INTRINSIC_GAS,
                    RECIPIENT,
                    TRANSFER_AMOUNT_WEI));

    assertThat(signerResponse.status()).isEqualTo(BAD_REQUEST);
    assertThat(signerResponse.jsonRpc().getError())
        .isEqualTo(REPLAY_PROTECTED_SIGNATURES_NOT_SUPPORTED);
  }
}
