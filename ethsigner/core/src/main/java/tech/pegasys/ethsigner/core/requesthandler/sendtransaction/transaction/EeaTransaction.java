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

import tech.pegasys.ethsigner.core.jsonrpc.EeaSendTransactionJsonParameters;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequestId;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.MoreObjects;
import org.web3j.crypto.Sign;
import org.web3j.crypto.Sign.SignatureData;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;

public class EeaTransaction implements Transaction {
  private static final String JSON_RPC_METHOD = "eea_sendRawTransaction";
  private final EeaSendTransactionJsonParameters eeaSendTransactionJsonParameters;
  private final RawPrivateTransactionBuilder rawPrivateTransactionBuilder;

  EeaTransaction(final EeaSendTransactionJsonParameters eeaSendTransactionJsonParameters) {
    this.eeaSendTransactionJsonParameters = eeaSendTransactionJsonParameters;
    this.rawPrivateTransactionBuilder =
        RawPrivateTransactionBuilder.from(eeaSendTransactionJsonParameters);
  }

  @Override
  public void updateNonce(final BigInteger nonce) {
    rawPrivateTransactionBuilder.updateNonce(nonce);
  }

  @Override
  public byte[] rlpEncode(final SignatureData signatureData) {
    final RawPrivateTransaction rawTransaction = rawPrivateTransactionBuilder.build();
    final List<RlpType> values = asRlpValues(rawTransaction, signatureData);
    final RlpList rlpList = new RlpList(values);
    return RlpEncoder.encode(rlpList);
  }

  /**
   * Modified from Web3J PrivateTransactionEncoder to use our RawPrivateTransaction that can have
   * non-zero values.
   */
  private static List<RlpType> asRlpValues(
      final RawPrivateTransaction privateTransaction, final Sign.SignatureData signatureData) {
    final List<RlpType> result =
        new ArrayList<>(
            TransactionEncoder.asRlpValues(privateTransaction.asRawTransaction(), signatureData));

    result.add(RlpString.create(privateTransaction.getPrivateFrom()));

    result.add(
        new RlpList(
            privateTransaction.getPrivateFor().stream()
                .map(RlpString::create)
                .collect(Collectors.toList())));

    result.add(RlpString.create(privateTransaction.getRestriction()));

    return result;
  }

  @Override
  public boolean hasNonce() {
    return eeaSendTransactionJsonParameters.nonce().isPresent();
  }

  @Override
  public String sender() {
    return eeaSendTransactionJsonParameters.sender();
  }

  @Override
  public JsonRpcRequest jsonRpcRequest(
      final String signedTransactionHexString, final JsonRpcRequestId id) {
    return Transaction.jsonRpcRequest(signedTransactionHexString, id, JSON_RPC_METHOD);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("eeaSendTransactionJsonParameters", eeaSendTransactionJsonParameters)
        .add("rawPrivateTransactionBuilder", rawPrivateTransactionBuilder)
        .toString();
  }
}
