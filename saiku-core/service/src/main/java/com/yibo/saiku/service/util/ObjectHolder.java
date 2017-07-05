/*
 *   Copyright 2014 OSBI Ltd
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
package com.yibo.saiku.service.util;

import com.yibo.saiku.olap.query.IQuery;
import com.yibo.saiku.service.util.exception.SaikuServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ObjectHolder {

  ThreadLocal<Map<String, IQuery>> threadQueries = new ThreadLocal<Map<String, IQuery>>();

  private static final Logger LOG = LoggerFactory.getLogger( ObjectHolder.class );

  public void putIQuery( String queryName, IQuery query ) {
    getIQueryMap().put( queryName, query );
  }

  public void removeIQuery( String queryName ) {
    getIQueryMap().remove( queryName );
  }


  public IQuery getIQuery( String queryName ) {
    IQuery query = getIQueryMap().get( queryName );
    if ( query == null ) {
      throw new SaikuServiceException( "No query with name (" + queryName + ") found" );
    }
    return query;
  }

  public Map<String, IQuery> getIQueryMap() {
    LOG.trace(
      "ObjectHoler.getIQueryMap : Thread ID " + Thread.currentThread().getId() + " Name: " + Thread.currentThread()
        .getName() );
    if ( threadQueries.get() == null ) {
      threadQueries.set( new HashMap<String, IQuery>() );
    }
    return threadQueries.get();
  }


}
