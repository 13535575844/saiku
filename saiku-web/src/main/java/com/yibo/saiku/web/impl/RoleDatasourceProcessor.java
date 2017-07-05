package com.yibo.saiku.web.impl;

import com.yibo.saiku.datasources.datasource.SaikuDatasource;
import com.yibo.saiku.service.datasource.IDatasourceProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RoleDatasourceProcessor implements IDatasourceProcessor {

	private static final Logger log = LoggerFactory.getLogger(RoleDatasourceProcessor.class);
	private static String ROLEFILTER = "role.filter";
	
	public SaikuDatasource process(SaikuDatasource ds) {
		if (SecurityContextHolder.getContext() != null && SecurityContextHolder.getContext().getAuthentication() != null) {			
			String roles = null;
			String filter = 
					ds.getProperties().containsKey(ROLEFILTER) ? 
							ds.getProperties().getProperty(ROLEFILTER) : null;
			List<String> allowedRoles = new ArrayList<String>();
			if (filter != null) {
				String[] filterRoles = filter.split(",");
				allowedRoles.addAll(Arrays.asList(filterRoles));
			}
					
			for (GrantedAuthority ga : SecurityContextHolder.getContext().getAuthentication().getAuthorities()) {
				String r = ga.getAuthority();
				boolean isAllowed = true;
				if (filter != null) {
					isAllowed = false;
					for (String allowed : allowedRoles) {
						if (r.toUpperCase().contains(allowed.toUpperCase())) {
							isAllowed = true;
						}
					}
				}
				if (isAllowed) {
					if (roles == null) {
						roles =  r;
					} else {
						roles += "," + r;
					}
				}
				
			}
			String location = ds.getProperties().getProperty("location");
			if (!location.endsWith(";")) {
				location +=";";
			}
			if (roles != null) {
				location += "Role=" + roles + ";";
			}
			log.debug(RoleDatasourceProcessor.class.getCanonicalName() + " : location = " + location);
			ds.getProperties().put("location", location);
		}
		return ds;
	}

}
