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
package tech.pegasys.ethfirewall.jsonrpcproxy;

import static java.util.Collections.emptyMap;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

public class ProxyIntegrationTest extends IntegrationTestBase {

  @Test
  public void requestWithHeadersIsProxied() {
    final String proxyBodyRequest =
        "{\"jsonrpc\":\"2.0\",\"method\":\"net_version\",\"params\":[],\"id\":1}";
    final Map<String, String> proxyHeadersRequest = ImmutableMap.of("Accept", "*/*");
    final String ethNodeResponse = "{\"jsonrpc\": \"2.0\",\"id\" : 1,\"result\":\"4\"}";
    final Map<String, String> ethNodeHeaders = ImmutableMap.of("Content-Type", "Application/Json");

    configureEthNode("net_version", ethNodeResponse, ethNodeHeaders);
    sendRequestAndVerify(
        proxyBodyRequest, proxyHeadersRequest, ethNodeResponse, 200, ethNodeHeaders);
  }

  @Test
  public void requestWithSendTransactionIsSignedBeforeProxying() {
    final String ethNodeResponse = "{\"jsonrpc\": \"2.0\",\"id\" : 1,\"result\":\"4\"}";
    final String proxyBodyRequest =
        "{\"jsonrpc\":\"2.0\",\"method\":\"eth_sendRawTransaction\",\"params\":[\"0xf8b2a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f07244567849184e72aa9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f0724456751ca03631b1cec6e5033e8a2bff6d1b2d08bfe106cbbb82df5eb7b380a1fdb5c06be2a06d15eeb833f26114de087c930e37556d93a47d86e6554e988d32cbbb273cfda4\"],\"id\":1}";

    configureEthNode("eth_sendRawTransaction", ethNodeResponse, new HashMap<>());
    sendRequestAndVerify(proxyBodyRequest, emptyMap(), ethNodeResponse, 200, emptyMap());
  }
}
