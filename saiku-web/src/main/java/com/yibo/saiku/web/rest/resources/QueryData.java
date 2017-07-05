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
package com.yibo.saiku.web.rest.resources;
import com.yibo.saiku.olap.dto.*;
import com.yibo.saiku.olap.dto.resultset.CellDataSet;
import com.yibo.saiku.service.olap.OlapDiscoverService;
import com.yibo.saiku.service.olap.OlapQueryService;
import com.yibo.saiku.web.rest.util.RestUtil;
import com.yibo.saiku.web.rest.util.ServletUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.ws.rs.*;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import com.yibo.saiku.web.rest.objects.resultset.QueryResult;

/**
 * QueryServlet contains all the methods required when manipulating an OLAP Query.
 * @author Tom Barber
 *
 */
@Component
@Path("/saiku/{username}/query/data")
@XmlAccessorType(XmlAccessType.NONE)
public class QueryData {

	private static final Logger log = LoggerFactory.getLogger(QueryResult.class);

	private OlapQueryService olapQueryService;
	private OlapDiscoverService olapDiscoverService;
	private ISaikuRepository repository;
	
	@Autowired
	public void setOlapQueryService(OlapQueryService olapqs) {
		olapQueryService = olapqs;
	}

	@Autowired
	public void setRepository(ISaikuRepository repository){
		this.repository = repository;
	}



	@Autowired
	public void setOlapDiscoverService(OlapDiscoverService olapds) {
		olapDiscoverService = olapds;
	}

	/**
	 * Create a new Saiku Query.
	 * @param connectionName the name of the Saiku connection.
	 * @param cubeName the name of the cube.
	 * @param catalogName the catalog name.
	 * @param schemaName the name of the schema.
	 * @param queryName the name you want to assign to the query.
	 * @return
	 *
	 * @return a query model.
	 *
	 * @see
	 */
	@POST
	@Produces({"application/json" })
	@Path("/{queryname}")
	public QueryResult createQuery(
			@FormParam("connection") String connectionName,
			@FormParam("cube") String cubeName,
			@FormParam("catalog") String catalogName,
			@FormParam("schema") String schemaName,
			@FormParam("xml") String xmlOld,
			@PathParam("queryname") String queryName,
			MultivaluedMap<String, String> formParams) throws ServletException {
		try {
			String file = null, xml = null;
			if (formParams != null) {
				xml = formParams.containsKey("xml") ? formParams.getFirst("xml") : xmlOld;
				file = formParams.containsKey("file") ? formParams.getFirst("file") : null;
				if (StringUtils.isNotBlank(file)) {
					Response f = repository.getResource(file);
					xml = new String( (byte[]) f.getEntity());
				}
			} else {
				xml = xmlOld;
			}
			if (log.isDebugEnabled()) {
				log.debug("TRACK\t"  + "\t/query/" + queryName + "\tPOST\t xml:" + (xml == null) + " file:" + (file));
			}
			SaikuCube cube = new SaikuCube(connectionName, cubeName,cubeName,cubeName, catalogName, schemaName);
			//if (StringUtils.isNotBlank(xml)) {
				String query = ServletUtil.replaceParameters(formParams, xml);
				olapQueryService.createNewOlapQuery(queryName, query);
			//}
			// olapQueryService.createNewOlapQuery(queryName, cube);
			 CellDataSet cs = olapQueryService.execute(queryName,"flattened");
			return RestUtil.convert(cs,0);
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
}

