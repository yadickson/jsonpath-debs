/*
 * Copyright 2011 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jayway.jsonpath;

import com.jayway.jsonpath.spi.JsonProvider;
import com.jayway.jsonpath.spi.JsonProviderFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static com.jayway.jsonpath.internal.Utils.*;

/**
 * User: kalle
 * Date: 8/30/13
 * Time: 12:05 PM
 */
public class Configuration {


    private final JsonProvider provider;
    private final EnumSet<Option> options;

    private Configuration(JsonProvider provider, EnumSet<Option> options) {
        notNull(provider, "provider can not be null");
        notNull(options, "options can not be null");
        this.provider = provider;
        this.options = options;
    }

    public Configuration provider(JsonProvider provider){
        return Configuration.builder().jsonProvider(provider).options(options).build();
    }

    public JsonProvider getProvider() {
        return provider;
    }

    public Configuration options(Option... options){
        return Configuration.builder().jsonProvider(provider).options(options).build();
    }

    public Set<Option> getOptions() {
        return Collections.unmodifiableSet(options);
    }

    public static Configuration defaultConfiguration(){
        return new Configuration(JsonProviderFactory.createProvider(), EnumSet.noneOf(Option.class));
    }

    public static ConfigurationBuilder builder(){
        return new ConfigurationBuilder();
    }

    public static class ConfigurationBuilder  {

        private JsonProvider provider;
        private EnumSet<Option> options = EnumSet.noneOf(Option.class);

        public ConfigurationBuilder jsonProvider(JsonProvider provider) {
            this.provider = provider;
            return this;
        }

        public ConfigurationBuilder options(Option... flags) {
            this.options.addAll(Arrays.asList(flags));
            return this;
        }

        public ConfigurationBuilder options(Set<Option> options) {
            this.options.addAll(options);
            return this;
        }

        public Configuration build(){
            if(provider == null){
                provider = JsonProviderFactory.createProvider();
            }
            return new Configuration(provider, options);
        }
    }
}
