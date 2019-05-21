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
package tech.pegasys.ethsigner.core.requesthandler.utils;

import static io.netty.handler.codec.http.HttpResponseStatus.GATEWAY_TIMEOUT;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class ResponseHandler {

  private static final Logger LOG = LogManager.getLogger();

  protected void handleException(final RoutingContext context, final Throwable thrown) {
    if (thrown instanceof TimeoutException || thrown instanceof ConnectException) {
      context.fail(GATEWAY_TIMEOUT.code(), thrown);
    } else {
      context.fail(INTERNAL_SERVER_ERROR.code());
    }
  }

  protected void handleResponse(final RoutingContext context, final HttpClientResponse response) {
    logResponse(response);

    response.bodyHandler(
        body -> {
          LOG.info("Handling received body");
          context
              .vertx()
              .executeBlocking(
                  future -> {
                    logResponseBody(body);
                    handleResponseBody(context, response, body);
                    future.complete();
                  },
                  false,
                  (res) -> {
                    if (res.failed()) {
                      LOG.error(
                          "An unhandled error occurred while processing {}",
                          context.getBodyAsString(),
                          res.cause());
                    }
                  });
        });
  }

  protected abstract void handleResponseBody(
      final RoutingContext context, final HttpClientResponse response, final Buffer body);

  private void logResponse(final HttpClientResponse response) {
    LOG.debug("Response status: {}", response.statusCode());
  }

  private void logResponseBody(final Buffer body) {
    LOG.debug("Response body: {}", body);
  }
}