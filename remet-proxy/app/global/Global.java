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

package global;

import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import play.Application;
import play.GlobalSettings;

/**
 * Setup the global application components.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class Global extends GlobalSettings {
    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart(final Application app) {
        LOGGER.info().setMessage("Starting application...").log();

        // Start-up parent
        super.onStart(app);

        LOGGER.debug().setMessage("Startup complete").log();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop(final Application app) {
        LOGGER.info().setMessage("Shutting down application...").log();

        // Shutdown parent
        super.onStop(app);

        LOGGER.debug().setMessage("Shutdown complete").log();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Global.class);
}
