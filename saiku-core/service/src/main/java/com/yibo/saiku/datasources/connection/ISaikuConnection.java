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
package com.yibo.saiku.datasources.connection;

import java.sql.Connection;
import java.util.Properties;

/**
 * @author pmac
 */
public interface ISaikuConnection {

  public static final String OLAP_DATASOURCE = "OLAP"; //$NON-NLS-1$
  public static final String NAME_KEY = "name"; //$NON-NLS-1$
  public static final String DRIVER_KEY = "driver"; //$NON-NLS-1$
  public static final String URL_KEY = "location"; //$NON-NLS-1$
  public static final String USERNAME_KEY = "username"; //$NON-NLS-1$
  public static final String PASSWORD_KEY = "password"; //$NON-NLS-1$
  public static final String SECURITY_ENABLED_KEY = "security.enabled"; //$NON-NLS-1$
  public static final String SECURITY_TYPE_KEY = "security.type"; //$NON-NLS-1$
  public static final String SECURITY_TYPE_SPRING2MONDRIAN_VALUE = "one2one"; //$NON-NLS-1$
  public static final String SECURITY_TYPE_SPRINGLOOKUPMONDRIAN_VALUE = "lookup"; //$NON-NLS-1$
  public static final String SECURITY_TYPE_PASSTHROUGH_VALUE = "passthrough"; //$NON-NLS-1$
  public static final String SECURITY_LOOKUP_KEY = "security.mapping"; //$NON-NLS-1$
  public static final String DATASOURCE_PROCESSORS = "datasource.processors"; //$NON-NLS-1$
  public static final String CONNECTION_PROCESSORS = "connection.processors"; //$NON-NLS-1$


  public static final String[] KEYS = new String[] { NAME_KEY, DRIVER_KEY, URL_KEY,
    USERNAME_KEY, PASSWORD_KEY, SECURITY_ENABLED_KEY, SECURITY_TYPE_KEY, SECURITY_TYPE_PASSTHROUGH_VALUE,
    SECURITY_TYPE_SPRING2MONDRIAN_VALUE, SECURITY_TYPE_SPRINGLOOKUPMONDRIAN_VALUE, DATASOURCE_PROCESSORS,
    CONNECTION_PROCESSORS };

  public static final String[] DATASOURCES = new String[] { OLAP_DATASOURCE };

  /**
   * Sets the properties to be used when the connection is made. The standard keys for the properties are defined in
   * this interface
   *
   * @param props
   */
  void setProperties(Properties props);


  /**
   * @param props Datasource connection properties
   * @return true if the connection was successful
   */
  boolean connect(Properties props) throws Exception;

  boolean connect() throws Exception;

  boolean clearCache() throws Exception;


  /**
   * @return true if the connection has been properly initialized.
   */
  public boolean initialized();

  /**
   * returns the type of connection
   *
   * @return
   */
  public String getDatasourceType();

  public Connection getConnection();

  public String getName();

}
