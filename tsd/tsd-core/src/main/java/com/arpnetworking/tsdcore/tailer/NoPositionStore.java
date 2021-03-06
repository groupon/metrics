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
package com.arpnetworking.tsdcore.tailer;

import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.steno.LogReferenceOnly;
import com.google.common.base.Optional;

import java.io.IOException;

/**
 * A <code>PositionStore</code> that always returns absent.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class NoPositionStore implements PositionStore {
    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Long> getPosition(final String identifier) {
        return Optional.absent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPosition(final String identifier, final long position) {
        // No need to do anything
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        // No need to do anything
    }

    /**
     * Generate a Steno log compatible representation.
     *
     * @return Steno log compatible representation.
     */
    @LogValue
    public Object toLogValue() {
        return LogReferenceOnly.of(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toLogValue().toString();
    }
}
