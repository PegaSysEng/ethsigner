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
package tech.pegasys.ethsigner.core.requesthandler.sendtransaction;

import static io.netty.handler.codec.http.HttpResponseStatus.GATEWAY_TIMEOUT;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static java.util.Collections.singletonList;

import tech.pegasys.ethsigner.core.http.HttpResponseFactory;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethsigner.core.requesthandler.JsonRpcBody;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.RetryMechanism.RetryException;
import tech.pegasys.ethsigner.core.signing.TransactionSerialiser;

import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TransactionTransmitter {

  private static final Logger LOG = LogManager.getLogger();

  private static final String JSON_RPC_VERSION = "2.0";
  private static final String JSON_RPC_METHOD = "eth_sendRawTransaction";

  private final HttpClient ethNodeClient;
  private final TransactionSerialiser transactionSerialiser;
  private final SendTransactionContext context;
  private final RetryMechanism<SendTransactionContext> retryMechanism;
  private final HttpResponseFactory responder;
  private final Duration httpRequestTimeout;

  public TransactionTransmitter(
      final HttpClient ethNodeClient,
      final SendTransactionContext context,
      final TransactionSerialiser transactionSerialiser,
      final RetryMechanism<SendTransactionContext> retryMechanism,
      final HttpResponseFactory responder,
      final Duration httpRequestTimeout) {
    this.ethNodeClient = ethNodeClient;
    this.context = context;
    this.transactionSerialiser = transactionSerialiser;
    this.retryMechanism = retryMechanism;
    this.responder = responder;
    this.httpRequestTimeout = httpRequestTimeout;
  }

  public void send() {
    final JsonRpcBody body = createSignedTransactionBody();
    if (body.hasError()) {
      reportError();
    } else {
      LOG.info("Sending transaction to web3jProvider");
      sendTransaction(body.body());
    }
  }

  private JsonRpcBody createSignedTransactionBody() {
    // This assumes the parameters have already been validated and are correct for the unlocked
    // account.
    final String signedTransactionHexString;
    try {
      final Transaction transaction = context.getTransaction();
      signedTransactionHexString = transactionSerialiser.serialise(transaction);
    } catch (final IllegalArgumentException e) {
      LOG.debug("Failed to encode transaction: {}", context.getTransaction(), e);
      return new JsonRpcBody(JsonRpcError.INVALID_PARAMS);
    } catch (final Throwable e) {
      LOG.debug("Failed to encode/serialise transaction: {}", context.getTransaction(), e);
      return new JsonRpcBody(JsonRpcError.INTERNAL_ERROR);
    }

    final JsonRpcRequest sendRawTransaction = new JsonRpcRequest(JSON_RPC_VERSION, JSON_RPC_METHOD);
    sendRawTransaction.setParams(singletonList(signedTransactionHexString));
    sendRawTransaction.setId(context.getId());

    try {
      return new JsonRpcBody(Json.encodeToBuffer(sendRawTransaction));
    } catch (final IllegalArgumentException e) {
      LOG.debug("JSON Serialisation failed for: {}", sendRawTransaction, e);
      return new JsonRpcBody(JsonRpcError.INTERNAL_ERROR);
    }
  }

  private void sendTransaction(final Buffer bodyContent) {
    final HttpServerRequest httpServerRequest = context.getInitialRequest();
    final HttpClientRequest request =
        ethNodeClient.request(
            httpServerRequest.method(), httpServerRequest.uri(), this::handleResponse);

    request.setTimeout(httpRequestTimeout.toMillis());
    request.exceptionHandler(thrown -> exceptionHandler(context.getRoutingContext(), thrown));
    request.headers().setAll(httpServerRequest.headers());
    request.headers().remove("Content-Length"); // created during 'end'.
    request.setChunked(false);
    request.end(bodyContent);
  }

  private void exceptionHandler(final RoutingContext context, final Throwable thrown) {
    LOG.info("An exception was thrown by the transaction submission, {}", thrown);
    if (thrown instanceof TimeoutException) {
      context.fail(GATEWAY_TIMEOUT.code());
    } else if (thrown instanceof ConnectException) {
      context.fail(GATEWAY_TIMEOUT.code());
    } else {
      context.fail(INTERNAL_SERVER_ERROR.code());
    }
  }

  private void handleResponse(final HttpClientResponse response) {
    logResponse(response);

    response.bodyHandler(
        body -> {
          LOG.info("Executing Body Handler");
          context
              .getRoutingContext()
              .vertx()
              .executeBlocking(
                  future -> {
                    logResponseBody(body);
                    handleResponseBody(response, body);
                    future.complete();
                  },
                  false,
                  (res) -> {
                    if (res.failed()) {
                      LOG.error(
                          "An unhandled error occurred while processing {}",
                          context.getRoutingContext().getBodyAsString(),
                          res.cause());
                    }
                  });
        });
  }

  private void handleResponseBody(final HttpClientResponse response, final Buffer body) {
    try {
      LOG.info("Handling Web3j response");
      if (response.statusCode() != HttpResponseStatus.OK.code()
          && retryMechanism.mustRetry(response, body)) {
        retryMechanism.retry(context, this::send);
        return;
      }
    } catch (final RetryException e) {
      LOG.info("Retry mechanism failed, reporting error.");
      context.getRoutingContext().fail(GATEWAY_TIMEOUT.code());
      return;
    }

    final HttpServerRequest httpServerRequest = context.getInitialRequest();
    httpServerRequest.response().setStatusCode(response.statusCode());
    httpServerRequest.response().headers().setAll(response.headers());
    httpServerRequest.response().setChunked(false);
    httpServerRequest.response().end(body);
  }

  private void logResponse(final HttpClientResponse response) {
    LOG.debug("Response status: {}", response.statusCode());
  }

  private void logResponseBody(final Buffer body) {
    LOG.debug("Response body: {}", body);
  }

  private void reportError() {
    final JsonRpcErrorResponse errorResponse =
        new JsonRpcErrorResponse(context.getId(), JsonRpcError.INTERNAL_ERROR);

    LOG.debug(
        "Dropping request method: {}, uri: {}, body: {}, Error body: {}",
        context.getInitialRequest()::method,
        context.getInitialRequest()::absoluteURI,
        context::getTransaction,
        () -> Json.encode(errorResponse));

    responder.create(
        context.getInitialRequest(), HttpResponseStatus.BAD_REQUEST.code(), errorResponse);
  }
}
