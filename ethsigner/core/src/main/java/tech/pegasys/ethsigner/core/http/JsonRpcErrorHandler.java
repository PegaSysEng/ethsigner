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
package tech.pegasys.ethsigner.core.http;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequestId;
import tech.pegasys.ethsigner.core.jsonrpc.exception.JsonRpcException;
import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLException;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class JsonRpcErrorHandler implements Handler<RoutingContext> {

  private final HttpResponseFactory httpResponseFactory;

  public JsonRpcErrorHandler(final HttpResponseFactory httpResponseFactory) {
    this.httpResponseFactory = httpResponseFactory;
  }

  @Override
  public void handle(final RoutingContext context) {
    final JsonRpcRequestId requestId = context.get("JsonRpcId");
    final int statusCode =
        context.statusCode() == -1 ? INTERNAL_SERVER_ERROR.code() : context.statusCode();

    final Throwable failure = context.failure();
    if (failure instanceof JsonRpcException) {
      final JsonRpcException ex = (JsonRpcException) context.failure();
      httpResponseFactory.failureResponse(
          context.response(), requestId, statusCode, ex.getJsonRpcError());
    } else if (failure instanceof ConnectException || failure instanceof SSLException) {
      httpResponseFactory.failureResponse(
          context.response(),
          requestId,
          statusCode,
          JsonRpcError.FAILED_TO_CONNECT_TO_DOWNSTREAM_NODE);
    } else if (failure instanceof TimeoutException) {
      httpResponseFactory.failureResponse(
          context.response(),
          requestId,
          statusCode,
          JsonRpcError.CONNECTION_TO_DOWNSTREAM_NODE_TIMED_OUT);
    } else {
      context.response().setStatusCode(statusCode);
      context.response().end();
    }
  }

}
