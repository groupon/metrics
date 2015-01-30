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
package com.arpnetworking.utility;

import com.arpnetworking.configuration.Configuration;
import com.arpnetworking.configuration.Listener;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Manages configuration and reconfiguration of a <code>Launchable</code> instance
 * using a POJO representation of its configuration. The <code>Launchable</code>
 * is instantiated with each new configuration. The configuration must validate
 * on construction and throw an exception if the configuration is invalid.
 *
 * @param <L> The <code>Launchable</code> type to configure.
 * @param <C> The type representing the validated configuration.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class Configurator<L extends Launchable, C> implements Launchable, Listener {

    /**
     * Public constructor.
     *
     * @param launchableClass The <code>Launchable</code> class.
     * @param configurationClass The configuration class.
     */
    public Configurator(final Class<L> launchableClass, final Class<? extends C> configurationClass) {
        _launchableClass = launchableClass;
        _configurationClass = configurationClass;

        Constructor launchableConstructor;
        try {
            launchableConstructor = launchableClass.getConstructor(_configurationClass);
        } catch (final NoSuchMethodException e) {
            Throwables.propagate(e);
            launchableConstructor = null;
        }
        _launchableConstructor = launchableConstructor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void offerConfiguration(final Configuration configuration) throws Exception {
        _offeredConfiguration = configuration.getAs(_configurationClass);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public synchronized void applyConfiguration() {
        // Shutdown
        shutdown();

        // Swap configurations
        _configuration = _offeredConfiguration;
        try {
            _launchable = Optional.<L>of((L) _launchableConstructor.newInstance(_configuration.get()));
        } catch (final InstantiationException | IllegalAccessException | InvocationTargetException e) {
            Throwables.propagate(e);
        }

        // (Re)launch
        launch();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void launch() {
        if (_launchable.isPresent()) {
            _launchable.get().launch();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void shutdown() {
        if (_launchable.isPresent()) {
            _launchable.get().shutdown();
            _launchable = Optional.absent();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(Configurator.class)
                .add("LaunchableClass", _launchableClass)
                .add("ConfigurationClass", _configurationClass)
                .toString();
    }

    /* package private */ Optional<L> getLaunchable() {
        return _launchable;
    }

    /* package private */ Optional<C> getConfiguration() {
        return _configuration;
    }

    /* package private */ Optional<C> getOfferedConfiguration() {
        return _offeredConfiguration;
    }

    private final Class<L> _launchableClass;
    private final Constructor _launchableConstructor;
    private final Class<? extends C> _configurationClass;

    private Optional<L> _launchable = Optional.absent();
    private Optional<C> _configuration = Optional.absent();
    private Optional<C> _offeredConfiguration = Optional.absent();
}