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
package tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc;

import tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.PrivateTransaction.ValueHolder;

import java.util.Optional;

public class Transaction {

  private final Optional<ValueHolder<String>> from;
  private final Optional<ValueHolder<String>> nonce;
  private final Optional<ValueHolder<String>> gasPrice;
  private final Optional<ValueHolder<String>> gas;
  private final Optional<ValueHolder<String>> to;
  private final Optional<ValueHolder<String>> value;
  private final Optional<ValueHolder<String>> data;

  public Transaction(
      final Optional<ValueHolder<String>> from,
      final Optional<ValueHolder<String>> nonce,
      final Optional<ValueHolder<String>> gasPrice,
      final Optional<ValueHolder<String>> gas,
      final Optional<ValueHolder<String>> to,
      final Optional<ValueHolder<String>> value,
      final Optional<ValueHolder<String>> data) {
    this.from = from;
    this.nonce = nonce;
    this.gasPrice = gasPrice;
    this.gas = gas;
    this.to = to;
    this.value = value;
    this.data = data;
  }

  public Optional<ValueHolder<String>> getFrom() {
    return from;
  }

  public Optional<ValueHolder<String>> getNonce() {
    return nonce;
  }

  public Optional<ValueHolder<String>> getGasPrice() {
    return gasPrice;
  }

  public Optional<ValueHolder<String>> getGas() {
    return gas;
  }

  public Optional<ValueHolder<String>> getTo() {
    return to;
  }

  public Optional<ValueHolder<String>> getValue() {
    return value;
  }

  public Optional<ValueHolder<String>> getData() {
    return data;
  }

  public static class Builder {
    private Optional<ValueHolder<String>> from = Optional.empty();
    private Optional<ValueHolder<String>> nonce = Optional.empty();
    private Optional<ValueHolder<String>> gasPrice = Optional.empty();
    private Optional<ValueHolder<String>> gas = Optional.empty();
    private Optional<ValueHolder<String>> to = Optional.empty();
    private Optional<ValueHolder<String>> value = Optional.empty();
    private Optional<ValueHolder<String>> data = Optional.empty();

    public Builder withFrom(final String from) {
      this.from = createValue(from);
      return this;
    }

    public Builder missingFrom() {
      this.from = Optional.empty();
      return this;
    }

    public Builder withNonce(final String nonce) {
      this.nonce = createValue(nonce);
      return this;
    }

    public Builder missingNonce() {
      this.nonce = Optional.empty();
      return this;
    }

    public Builder withGasPrice(final String gasPrice) {
      this.gasPrice = createValue(gasPrice);
      return this;
    }

    public Builder missingGasPrice() {
      this.gasPrice = Optional.empty();
      return this;
    }

    public Builder withGas(final String gas) {
      this.gas = createValue(gas);
      return this;
    }

    public Builder missingGas() {
      this.gas = Optional.empty();
      return this;
    }

    public Builder withTo(final String to) {
      this.to = createValue(to);
      return this;
    }

    public Builder missingTo() {
      this.to = Optional.empty();
      return this;
    }

    public Builder withValue(final String value) {
      this.value = createValue(value);
      return this;
    }

    public Builder missingValue() {
      this.value = Optional.empty();
      return this;
    }

    public Builder withData(final String data) {
      this.data = createValue(data);
      return this;
    }

    public Builder missingData() {
      this.data = Optional.empty();
      return this;
    }

    public Transaction build() {
      return new Transaction(from, nonce, gasPrice, gas, to, value, data);
    }

    private <T> Optional<ValueHolder<T>> createValue(final T from) {
      return Optional.of(new ValueHolder<>(from));
    }
  }
}