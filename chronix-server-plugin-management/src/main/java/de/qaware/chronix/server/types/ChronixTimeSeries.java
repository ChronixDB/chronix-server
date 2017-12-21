/*
 * Copyright (C) 2018 QAware GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package de.qaware.chronix.server.types;

import java.util.Map;

/**
 * The Chronix time series interface.
 * It is typically a wrapper for a time series class
 *
 * @author f.lautenschlager
 */
public interface ChronixTimeSeries<T> {

    /**
     * @return the type of the chronix time series
     */
    String getType();

    /**
     * @return the name of the time series
     */
    String getName();

    /**
     * @return the start of the time series
     */
    long getStart();

    /**
     * @return the end of the time series
     */
    long getEnd();

    /**
     * @return the getAttributes
     */
    Map<String, Object> getAttributes();

    /**
     * Sorts the time series by time acceding
     */
    void sort();

    /**
     * @return the data as json
     */
    String dataAsJson();

    /**
     * @return as binary large object
     */
    byte[] dataAsBlob();

    /**
     * @return the join key
     */
    String getJoinKey();

    /**
     * @return get raw time series
     */
    T getRawTimeSeries();
}
