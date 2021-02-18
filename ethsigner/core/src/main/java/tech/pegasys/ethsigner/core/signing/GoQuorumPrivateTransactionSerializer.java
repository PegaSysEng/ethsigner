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
package tech.pegasys.ethsigner.core.signing;

import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction.Transaction;
import tech.pegasys.signers.secp256k1.api.Signature;
import tech.pegasys.signers.secp256k1.api.Signer;

import org.web3j.crypto.Sign.SignatureData;
import org.web3j.utils.Numeric;

public class GoQuorumPrivateTransactionSerializer extends TransactionSerializer {

  public GoQuorumPrivateTransactionSerializer(Signer signer, long chainId) {
    super(signer, chainId);
  }

  @Override
  public String serialize(final Transaction transaction) {
    final byte[] bytesToSign = transaction.rlpEncode(null);
    final Signature signature = signer.sign(bytesToSign);

    byte[] newV = (getGoQuorumVValue(signature.getV().toByteArray()));

    final SignatureData web3jSignature =
        new SignatureData(newV, signature.getR().toByteArray(), signature.getS().toByteArray());

    final byte[] serializedBytes = transaction.rlpEncode(web3jSignature);
    return Numeric.toHexString(serializedBytes);
  }

  public static byte[] getGoQuorumVValue(byte[] v) {
    if (v[v.length - 1] == (byte) 28) {
      return new byte[] {38};
    } else {
      return new byte[] {37};
    }
  }
}
