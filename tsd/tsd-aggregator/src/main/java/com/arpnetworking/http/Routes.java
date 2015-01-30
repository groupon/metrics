/**
 * Copyright 2014 Groupon.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arpnetworking.http;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.dispatch.OnComplete;
import akka.http.model.HttpRequest;
import akka.http.model.HttpResponse;
import akka.http.model.HttpResponse$;
import akka.http.model.japi.HttpHeader;
import akka.http.model.japi.HttpMethods;
import akka.http.model.japi.MediaTypes;
import akka.http.model.japi.StatusCodes;
import akka.http.model.japi.headers.CacheControl;
import akka.http.model.japi.headers.CacheDirectives;
import akka.japi.JavaPartialFunction;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.arpnetworking.metrics.Metrics;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.metrics.Timer;
import com.arpnetworking.tsdaggregator.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.runtime.AbstractFunction1;

import java.util.concurrent.TimeUnit;

/**
 * Http server routes.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class Routes extends AbstractFunction1<HttpRequest, Future<HttpResponse>> {

    /**
     * Public constructor.
     *
     * @param actorSystem Instance of <code>ActorSystem</code>.
     * @param metricsFactory Instance of <code>MetricsFactory</code>.
     */
    public Routes(final ActorSystem actorSystem, final MetricsFactory metricsFactory) {
        _actorSystem = actorSystem;
        _metricsFactory = metricsFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<HttpResponse> apply(final akka.http.model.HttpRequest request) {
        final Metrics metrics = _metricsFactory.create();
        final Timer timer = metrics.createTimer(createTimerName(request));
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("Request; %s", request));
        }
        final Future<HttpResponse> futureResponse = process(request);
        futureResponse.onComplete(
                new OnComplete<HttpResponse>() {
                    @Override
                    public void onComplete(final Throwable failure, final HttpResponse response) {
                        timer.close();
                        metrics.close();
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace(String.format("Response; %s", response));
                        }
                    }
                },
                _actorSystem.dispatcher());
        return futureResponse;
    }

    private Future<HttpResponse> process(final HttpRequest request) {
        if (HttpMethods.GET.equals(request.method())) {
            if (PING_PATH.equals(request.getUri().path())) {
                return ask("/user/status", Status.IS_HEALTHY, Boolean.FALSE)
                        .map(
                                new Mapper<Boolean, HttpResponse>() {
                                    @Override
                                    public HttpResponse apply(final Boolean isHealthy) {
                                        return (HttpResponse) response()
                                                .withStatus(isHealthy ? StatusCodes.OK : StatusCodes.INTERNAL_SERVER_ERROR)
                                                .addHeader(
                                                        PING_CACHE_CONTROL_HEADER)
                                                .withEntity(JSON_CONTENT_TYPE,
                                                            "{\"status\":\""
                                                                    + (isHealthy ? HEALTHY_STATE : UNHEALTHY_STATE)
                                                                    + "\"}");
                                    }
                                },
                                _actorSystem.dispatcher()
                        );
            }
        }
        return Futures.successful((HttpResponse) response().withStatus(404));
    }

    private HttpResponse response() {
        return HttpResponse.apply(
                HttpResponse$.MODULE$.apply$default$1(),
                HttpResponse$.MODULE$.apply$default$2(),
                HttpResponse$.MODULE$.apply$default$3(),
                HttpResponse$.MODULE$.apply$default$4()
        );
    }

    private <T> Future<T> ask(final String actorPath, final Object request, final T defaultValue) {
        return _actorSystem.actorSelection(actorPath)
                .resolveOne(Timeout.apply(1, TimeUnit.SECONDS))
                .<T>flatMap(
                        new Mapper<ActorRef, Future<T>>() {
                            @Override
                            public Future<T> apply(final ActorRef actor) {
                                @SuppressWarnings("unchecked")
                                final Future<T> future = (Future<T>) Patterns.ask(
                                        actor,
                                        request,
                                        Timeout.apply(1, TimeUnit.SECONDS));
                                return future;
                            }
                        },
                        _actorSystem.dispatcher())
                .recover(
                        new JavaPartialFunction<Throwable, T>() {
                             @Override
                             public T apply(final Throwable t, final boolean isCheck) throws Exception {
                                 return defaultValue;
                             }
                         },
                        _actorSystem.dispatcher());
    }

    private String createTimerName(final HttpRequest request) {
        final StringBuilder nameBuilder = new StringBuilder()
                .append("RestService/")
                .append(request.method().value());
        if (!request.getUri().path().startsWith("/")) {
            nameBuilder.append("/");
        }
        nameBuilder.append(request.getUri().path());
        return nameBuilder.toString();
    }

    private final ActorSystem _actorSystem;
    private final MetricsFactory _metricsFactory;

    private static final Logger LOGGER = LoggerFactory.getLogger(Routes.class);

    // Ping
    private static final String PING_PATH = "/ping";
    private static final HttpHeader PING_CACHE_CONTROL_HEADER = CacheControl.create(
            CacheDirectives.PRIVATE(),
            CacheDirectives.NO_CACHE,
            CacheDirectives.NO_STORE,
            CacheDirectives.MUST_REVALIDATE);
    private static final String UNHEALTHY_STATE = "UNHEALTHY";
    private static final String HEALTHY_STATE = "HEALTHY";

    private static final akka.http.model.japi.ContentType JSON_CONTENT_TYPE =
            akka.http.model.japi.ContentType.create(MediaTypes.APPLICATION_JSON);
}
