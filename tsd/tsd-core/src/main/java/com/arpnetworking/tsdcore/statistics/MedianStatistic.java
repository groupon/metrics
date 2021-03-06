/**
 * Copyright 2014 Brandon Arp
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
package com.arpnetworking.tsdcore.statistics;

/**
 * Top median statistic (aka 50th percentile).
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class MedianStatistic extends TPStatistic {

    /**
     * Public constructor.
     */
    public MedianStatistic() {
        super(50d);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "median";
    }

    private static final long serialVersionUID = 1L;
}
