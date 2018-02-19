/*
 * Copyright 2016 Crown Copyright
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

package stroom.pipeline.server.ext.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import stroom.feed.StroomStreamException;
import stroom.task.server.GenericServerTask;
import stroom.util.task.TaskScopeRunnable;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * Servlet that streams files to disk based on meta input arguments.
 * </p>
 */
@Component
public class HttpProcessor extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpProcessor.class);
    private static final AtomicInteger concurrentRequestCount = new AtomicInteger(0);

    private final HttpProcessorConfig httpProcessorConfig;
    private final Provider<HttpSource> httpSourceProvider;

    @Inject
    HttpProcessor(final HttpProcessorConfig httpProcessorConfig,
                  final Provider<HttpSource> httpSourceProvider) {
        this.httpProcessorConfig = httpProcessorConfig;
        this.httpSourceProvider = httpSourceProvider;
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest, HttpServletResponse)
     */
    @Override
    public void doPost(final HttpServletRequest request, final HttpServletResponse response) {
        handleRequest(request, response);
    }

    /**
     * @see HttpServlet#doPut(HttpServletRequest, HttpServletResponse)
     */
    @Override
    protected void doPut(final HttpServletRequest request, final HttpServletResponse response) {
        handleRequest(request, response);
    }

    /**
     * Do handle the request.... spring from here on.
     *
     * @param request
     * @param response
     */
    private void handleRequest(final HttpServletRequest request, final HttpServletResponse response) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(getRequestTrace(request));
        }

        concurrentRequestCount.incrementAndGet();
        try {
            new TaskScopeRunnable(GenericServerTask.create("Feed Servlet", null)) {
                @Override
                protected void exec() {
                    try {
                        final HttpSource httpSource = httpSourceProvider.get();
                        httpSource.exec(httpProcessorConfig.getPipelineRef(), request, response);
                    } catch (final Exception ex) {
                        StroomStreamException.sendErrorResponse(response, ex);
                    }
                }
            }.run();
        } catch (final Exception ex) {
            StroomStreamException.sendErrorResponse(response, ex);
        } finally {
            concurrentRequestCount.decrementAndGet();
        }
    }

    /**
     * <p>
     * Utility to log out some trace info.
     * </p>
     *
     * @param request
     * @return
     */
    @SuppressWarnings("unchecked")
    private String getRequestTrace(final HttpServletRequest request) {
        final StringBuilder trace = new StringBuilder();
        trace.append("request.getAuthType()=");
        trace.append(request.getAuthType());
        trace.append("\n");
        trace.append("request.getProtocol()=");
        trace.append(request.getProtocol());
        trace.append("\n");
        trace.append("request.getScheme()=");
        trace.append(request.getScheme());
        trace.append("\n");
        trace.append("request.getQueryString()=");
        trace.append(request.getQueryString());
        trace.append("\n");

        final Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements()) {
            String header = headers.nextElement();
            trace.append("request.getHeader('");
            trace.append(header);
            trace.append("')='");
            trace.append(request.getHeader(header));
            trace.append("'\n");
        }

        final Enumeration<String> attributes = request.getAttributeNames();
        while (attributes.hasMoreElements()) {
            String attr = attributes.nextElement();
            trace.append("request.getAttribute('");
            trace.append(attr);
            trace.append("')='");
            trace.append(request.getAttribute(attr));
            trace.append("'\n");
        }

        trace.append("request.getRequestURI()=");
        trace.append(request.getRequestURI());

        return trace.toString();
    }
}