/*
 * Copyright 2017 Crown Copyright
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
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import stroom.pipeline.server.ext.source.HttpProcessor;
import stroom.pipeline.server.ext.source.HttpProcessorConfig;
import stroom.pipeline.server.ext.source.HttpSource;
import stroom.util.spring.StroomScope;

import javax.inject.Provider;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class ProxyConfiguration {
    @Bean
    public HttpProcessor httpProcessor(final HttpProcessorConfig httpProcessorConfig,
                                       final Provider<HttpSource> httpSourceProvider) {
        return new HttpProcessor(httpProcessorConfig, httpSourceProvider);
    }
}
