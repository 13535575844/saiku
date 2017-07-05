/*  
 *   Copyright 2014 Paul Stoellberger
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.yibo.saiku.query;
public enum SortOrder {
    /**
     * Ascending sort order. Members of
     * the same hierarchy are still kept together.
     */
    ASC,
    /**
     * Descending sort order. Members of
     * the same hierarchy are still kept together.
     */
    DESC,
    /**
     * Sorts in ascending order, but does not
     * maintain members of a same hierarchy
     * together. This is known as a "break
     * hierarchy ascending sort".
     */
    BASC,
    /**
     * Sorts in descending order, but does not
     * maintain members of a same hierarchy
     * together. This is known as a "break
     * hierarchy descending sort".
     */
    BDESC
}
