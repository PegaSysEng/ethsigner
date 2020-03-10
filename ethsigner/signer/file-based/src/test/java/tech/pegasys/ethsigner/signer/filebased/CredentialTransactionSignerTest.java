package tech.pegasys.ethsigner.signer.filebased;/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import tech.pegasys.ethsigner.core.jsonrpc.EthSendTransactionJsonParameters;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequestId;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction.EthTransaction;
import tech.pegasys.ethsigner.core.signing.Signature;
import tech.pegasys.ethsigner.core.signing.TransactionSerializer;

class CredentialTransactionSignerTest {

  @Test
  void ensureZenDesk32060IsResolved() {
    final long chainId = 44844;

    final EthSendTransactionJsonParameters txnParams =
        new EthSendTransactionJsonParameters("0xf17f52151ebef6c7334fad080c5704d77216b732");
    txnParams.gasPrice("0x0");
    txnParams.gas("0x7600");
    txnParams.nonce("0x46");
    txnParams.value("0x1");
    txnParams.data("0x0");
    txnParams.receiver("0x627306090abaB3A6e1400e9345bC60c78a8BEf57");

    final EthTransaction txn = new EthTransaction(txnParams, null, new JsonRpcRequestId(1));

    final byte[] serialisedBytes = txn.rlpEncode(chainId);

    CredentialTransactionSigner signer = new CredentialTransactionSigner(
        Credentials.create("ae6ae8e5ccbfb04590405997ee2d52d2b330726137b875053c36d94e974d162f"));

    final Signature signature = signer.sign(serialisedBytes);

    assertThat(signature.getR().signum()).isOne();
    assertThat(signature.getS().signum()).isOne();
  }

}