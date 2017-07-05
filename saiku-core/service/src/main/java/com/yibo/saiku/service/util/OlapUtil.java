/*  
 *   Copyright 2012 OSBI Ltd
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

import org.olap4j.CellSet;

import java.util.HashMap;
import java.util.Map;

public class OlapUtil {


  private static Map<String, CellSet> cellSetMap = new HashMap<String, CellSet>();


  /**
   * storeCellSet stores a cellset generated from a query so we can manipulate it at a later date.
   *
   * @param cellSet
   * @param queryId
   */
  public static void storeCellSet( final String queryId, final CellSet cellSet ) {
    if ( cellSetMap.containsKey( queryId ) ) {
      cellSetMap.remove( queryId );
    }
    cellSetMap.put( queryId, cellSet );
  }

  public static CellSet getCellSet( final String queryId ) {
    return cellSetMap.get( queryId );

  }

  public static void deleteCellSet( final String queryId ) {
    cellSetMap.remove( queryId );
  }

}
