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

package actors;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.tsdcore.sources.FileSource;
import com.arpnetworking.tsdcore.sources.Source;
import com.arpnetworking.tsdcore.tailer.InitialPosition;
import com.arpnetworking.utility.observer.Observable;
import com.arpnetworking.utility.observer.Observer;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import models.LogLineParser;
import models.messages.LogFileAppeared;
import models.messages.LogFileDisappeared;
import models.messages.LogLine;
import org.joda.time.Duration;
import play.Configuration;

import java.nio.file.Path;
import java.util.Map;


/**
 * Actor for handling messages related to live log reporting.
 *
 * @author Mohammed Kamel (mkamel at groupon dot com)
 */
public class FileSourcesManager extends UntypedActor {
    //TODO(barp): Add metrics to this class [MAI-406]

    /**
     * Public constructor.
     *
     * @param configuration Play app configuration
     * @param streamContextActor <code>ActorRef</code> for the singleton stream context actor
     */
    @Inject
    public FileSourcesManager(final Configuration configuration, @Named("StreamContext") final ActorRef streamContextActor) {
        _fileSourceIntervalMilliseconds = Duration.millis(configuration.getLong("fileSource.interval", 500L));

        _streamContextActor = streamContextActor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Object message) throws Exception {
        LOGGER.trace()
                .setMessage("Received message")
                .addData("actor", self())
                .addData("data", message)
                .log();

        if (message instanceof LogFileAppeared) {
            final LogFileAppeared logFileAppeared = (LogFileAppeared) message;
            addSource(logFileAppeared.getFile());
        } else if (message instanceof LogFileDisappeared) {
            final LogFileDisappeared logFileDisappeared = (LogFileDisappeared) message;
            removeSource(logFileDisappeared.getFile());
        } else {
            LOGGER.warn()
                    .setMessage("Unsupported message")
                    .addData("actor", self())
                    .addData("data", message)
                    .log();
            unhandled(message);
        }
    }

    private void addSource(final Path filepath) {
        LOGGER.info()
                .setMessage("Adding new log file source")
                .addData("actor", self())
                .addData("path", filepath)
                .log();
        if (!_fileSources.containsKey(filepath)) {
            final Source source =
                    new FileSource.Builder<LogLine>()
                            .setName("File: " + filepath)
                            .setSourceFile(filepath.toFile())
                            .setInterval(_fileSourceIntervalMilliseconds)
                            .setParser(new LogLineParser(filepath))
                            .setInitialPosition(InitialPosition.END)
                            .build();
            source.attach(new LogFileObserver(_streamContextActor, getSelf()));
            source.start();

            _fileSources.put(filepath, source);

            _streamContextActor.tell(new LogFileAppeared(filepath), getSelf());
        }
    }

    private void removeSource(final Path filepath) {
        _streamContextActor.tell(new LogFileDisappeared(filepath), getSelf());
        LOGGER.info()
            .setMessage("Removing log file source")
            .addData("actor", self())
            .addData("path", filepath)
            .log();
        final Source source = _fileSources.remove(filepath);
        if (source != null) {
            source.stop();
        } else {
            LOGGER.warn()
                .setMessage("Attempted to removed a non existing file source")
                .addData("actor", self())
                .addData("path", filepath)
                .log();
        }
    }

    private final Duration _fileSourceIntervalMilliseconds;
    private final Map<Path, Source> _fileSources = Maps.newHashMap();
    private final ActorRef _streamContextActor;

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSourcesManager.class);

    /*package private*/
    static class LogFileObserver implements Observer {

        /*package private*/LogFileObserver(final ActorRef streamContextActor, final ActorRef messageSender) {
            _streamContextActor = streamContextActor;
            _messageSender = messageSender;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void notify(final Observable observable, final Object event) {
            final LogLine logLine = (LogLine) event;
            _streamContextActor.tell(logLine, _messageSender);
        }

        private final ActorRef _streamContextActor;
        private final ActorRef _messageSender;
    }
}
