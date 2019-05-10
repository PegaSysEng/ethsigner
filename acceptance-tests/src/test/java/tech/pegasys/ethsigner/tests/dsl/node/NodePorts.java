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
package tech.pegasys.ethsigner.tests.dsl.node;

public class NodePorts {

  private final int httpRpc;
  private final int webSocketRpc;

  public NodePorts(final int tcp, final int webSocket) {
    this.httpRpc = tcp;
    this.webSocketRpc = webSocket;
  }

  public int getHttpRpc() {
    return httpRpc;
  }

  public int getWebSocketRpc() {
    return webSocketRpc;
  }
}
