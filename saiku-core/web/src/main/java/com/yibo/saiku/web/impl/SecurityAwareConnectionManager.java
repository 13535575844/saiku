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
package com.yibo.saiku.web.impl;

import com.yibo.saiku.datasources.connection.AbstractConnectionManager;
import com.yibo.saiku.datasources.connection.ISaikuConnection;
import com.yibo.saiku.datasources.connection.SaikuConnectionFactory;
import com.yibo.saiku.datasources.datasource.SaikuDatasource;
import com.yibo.saiku.service.ISessionService;
import mondrian.olap4j.SaikuMondrianHelper;
import org.apache.commons.lang.StringUtils;
import org.olap4j.OlapConnection;
import org.olap4j.OlapException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.Serializable;
import java.sql.Connection;
import java.util.*;

public class SecurityAwareConnectionManager extends AbstractConnectionManager implements Serializable {

	/**
	 * serialisation UID
	 */
	private static final long serialVersionUID = -5912836681963684201L;

	private transient Map<String, ISaikuConnection> connections = new HashMap<String, ISaikuConnection>();

	private List<String> errorConnections = new ArrayList<String>();

	private ISessionService sessionService;

	public void setSessionService(ISessionService ss) {
		this.sessionService = ss;
	}

	@Override
	public void init() {
		this.connections = getAllConnections();
	}
	
	@Override
	public void destroy() {
		if (connections != null && !connections.isEmpty()) {
			for (ISaikuConnection con : connections.values()) {
				try {
					Connection c = con.getConnection();
						if (!c.isClosed()) {
							c.close();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			connections.clear();
	}
	
	
	@Override
	protected ISaikuConnection getInternalConnection(String name, SaikuDatasource datasource) {

		ISaikuConnection con = null;
		if (isDatasourceSecurity(datasource, ISaikuConnection.SECURITY_TYPE_PASSTHROUGH_VALUE) && sessionService != null) {
			datasource = handlePassThrough(datasource);
		}

		String newName = name;
		if (isDatasourceSecurityEnabled(datasource) && sessionService != null) {
			Map<String, Object> session = sessionService.getAllSessionObjects();
			String username = (String) session.get("username");
			if (username != null) {
				newName = name + "-" + username;
			}
		}



		if (!connections.containsKey(newName)) {
			con =  connect(name, datasource);
			if (con != null) {
				connections.put(newName, con);
			} else {
				if (!errorConnections.contains(newName)) {
					errorConnections.add(newName);
				}
			}

		} else {
			con = connections.get(newName);
		}
		if (con != null && !isDatasourceSecurity(datasource, ISaikuConnection.SECURITY_TYPE_PASSTHROUGH_VALUE)) {
			con = applySecurity(con, datasource);
		}
		return con;



	}

	@Override
	protected ISaikuConnection refreshInternalConnection(String name, SaikuDatasource datasource) {
		try {
			String newName = name;
			if (isDatasourceSecurityEnabled(datasource) && sessionService != null) {
				Map<String, Object> session = sessionService.getAllSessionObjects();
				String username = (String) session.get("username");
				if (username != null) {
					newName = name + "-" + username;
				}
			}

			ISaikuConnection con = connections.remove(newName);
			if (con != null) {
				con.clearCache();
			}
			con = null;
			return getInternalConnection(name, datasource);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private SaikuDatasource handlePassThrough(SaikuDatasource datasource) {

		Map<String, Object> session = sessionService.getAllSessionObjects();
		String username = (String) session.get("username");

		if (username != null) {
			String password = (String) session.get("password");
			datasource.getProperties().setProperty("username",username);
			if (password != null) {
				datasource.getProperties().setProperty("password",password);
			}
			return datasource;
		}

		return null;
	}

	private ISaikuConnection applySecurity(ISaikuConnection con, SaikuDatasource datasource) {
		if (con == null) {
			throw new IllegalArgumentException("Cannot apply Security to NULL connection object");
		}

		if (isDatasourceSecurity(datasource, ISaikuConnection.SECURITY_TYPE_SPRING2MONDRIAN_VALUE)) {
			List<String> springRoles = getSpringRoles();
			List<String> conRoles = getConnectionRoles(con);
			String roleName = null;

			for (String sprRole : springRoles) {
				if (conRoles.contains(sprRole)) {
					if (roleName == null) {
						roleName = sprRole;
					} else {
						roleName += "," + sprRole;
					}
				}
			}

			if (setRole(con, roleName, datasource)) {
				return con;
			}

		} else if (isDatasourceSecurity(datasource, ISaikuConnection.SECURITY_TYPE_SPRINGLOOKUPMONDRIAN_VALUE)) {
			Map<String, List<String>> mapping = getRoleMapping(datasource);
			List<String> springRoles = getSpringRoles();
			String roleName = null;
			for (String sprRole : springRoles) {
				if (mapping.containsKey(sprRole)) {
					List<String> roles = mapping.get(sprRole);
					for (String role : roles) {
						if (roleName == null) {
							roleName = role;
						} else {
							roleName += "," + role;
						}

					}
				}
			}
			if (setRole(con, roleName, datasource)) {
				return con;
			}

		} 

		return con;

	}

	private boolean setRole(ISaikuConnection con, String roleName, SaikuDatasource datasource) {
		if (con.getConnection() instanceof OlapConnection) 
		{
			OlapConnection c = (OlapConnection) con.getConnection();
			if (c == null)
				return false;
			
			System.out.println("Setting role to datasource:" + datasource.getName() + " role:" + roleName);
			try {
				if (StringUtils.isNotBlank(roleName) && SaikuMondrianHelper.isMondrianConnection(c) && roleName.split(",").length > 1) {
					SaikuMondrianHelper.setRoles(c, roleName.split(","));
				} else {
					c.setRoleName(roleName);
				}
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}


	private List<String> getSpringRoles() {
		List<String> roles = new ArrayList<String>();
		if (SecurityContextHolder.getContext() != null && SecurityContextHolder.getContext().getAuthentication() != null) {
			Collection<? extends GrantedAuthority> auths = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
			for (GrantedAuthority a : auths) {
				roles.add(a.getAuthority());
			}
		}
		return roles;
	}

	private List<String> getConnectionRoles(ISaikuConnection con) {
		if (con.getDatasourceType().equals(ISaikuConnection.OLAP_DATASOURCE) 
				&& con.getConnection() instanceof OlapConnection) 
		{
			OlapConnection c = (OlapConnection) con.getConnection();
			try {
				return c.getAvailableRoleNames();
			} catch (OlapException e) {
				e.printStackTrace();
			}
		}
		return new ArrayList<String>();
	}

	private Map<String,List<String>> getRoleMapping(SaikuDatasource datasource) {
		Map<String,List<String>> result = new HashMap<String,List<String>>();
		if (datasource.getProperties().containsKey(ISaikuConnection.SECURITY_LOOKUP_KEY)) {
			String mappings = datasource.getProperties().getProperty(ISaikuConnection.SECURITY_LOOKUP_KEY);
			if (mappings != null) {
				String[] maps = mappings.split(";");
				for (String map : maps) {
					String[] m = map.split("=");
					if (m.length == 2) {
						if (!result.containsKey(m[0])) {
							result.put(m[0], new ArrayList<String>());
						}
						result.get(m[0]).add(m[1]);
					}
				}
			}
		}
		return result;
	}


	private ISaikuConnection connect(String name, SaikuDatasource datasource) {
		try {
			ISaikuConnection con = SaikuConnectionFactory.getConnection(datasource);
			if (con.initialized()) {
				return con;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
}
