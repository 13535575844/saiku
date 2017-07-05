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


import com.yibo.saiku.web.export.PdfReport;
import com.yibo.saiku.web.rest.objects.resultset.QueryResult;
import com.yibo.saiku.web.rest.util.ServletUtil;
import com.yibo.saiku.web.svg.Converter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;


/**
 * QueryServlet contains all the methods required when manipulating an OLAP Query.
 * @author Paul Stoellberger
 *
 */
@Component
@Path("/saiku/{username}/export")
@XmlAccessorType(XmlAccessType.NONE)
public class ExporterResource {

	private static final Logger log = LoggerFactory.getLogger(ExporterResource.class);

	private ISaikuRepository repository;

	private QueryResource queryResource;

	public void setQueryResource(QueryResource qr){
		this.queryResource = qr;
	}

	public void setRepository(ISaikuRepository repository){
		this.repository = repository;
	}


	@GET
	@Produces({"application/json" })
	@Path("/saiku/xls")
	public Response exportExcel(@QueryParam("file") String file, 
			@QueryParam("formatter") String formatter,
			@Context HttpServletRequest servletRequest) 
	{
		try {
			Response f = repository.getResource(file);
			String fileContent = new String( (byte[]) f.getEntity());
			String queryName = UUID.randomUUID().toString();
			fileContent = ServletUtil.replaceParameters(servletRequest, fileContent);
			queryResource.createQuery(null,  null,  null, null, fileContent, queryName, null);
			queryResource.execute(queryName, formatter, 0);
			return queryResource.getQueryExcelExport(queryName, formatter);
		} catch (Exception e) {
			log.error("Error exporting XLS for file: " + file, e);
			return Response.serverError().entity(e.getMessage()).status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GET
	@Produces({"application/json" })
	@Path("/saiku/csv")
	public Response exportCsv(@QueryParam("file") String file, 
			@QueryParam("formatter") String formatter,
			@Context HttpServletRequest servletRequest) 
	{
		try {
			Response f = repository.getResource(file);
			String fileContent = new String( (byte[]) f.getEntity());
			fileContent = ServletUtil.replaceParameters(servletRequest, fileContent);
			String queryName = UUID.randomUUID().toString();
			queryResource.createQuery(null,  null,  null, null, fileContent, queryName, null);
			queryResource.execute(queryName,formatter, 0);
			return queryResource.getQueryCsvExport(queryName);
		} catch (Exception e) {
			log.error("Error exporting CSV for file: " + file, e);
			return Response.serverError().entity(e.getMessage()).status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}


	@GET
	@Produces({"application/json" })
	@Path("/saiku/pdf")
	public Response exportPdf(@QueryParam("file") String file, 
			@QueryParam("formatter") String formatter,
			@Context HttpServletRequest servletRequest) 
	{
		try {
			Response f = repository.getResource(file);
			String fileContent = new String( (byte[]) f.getEntity());
			String queryName = UUID.randomUUID().toString();
			fileContent = ServletUtil.replaceParameters(servletRequest, fileContent);
			queryResource.createQuery(null,  null,  null, null, fileContent, queryName, null);
			QueryResult qr = queryResource.execute(queryName, formatter, 0);
			PdfReport pdf = new PdfReport();
			byte[] doc  = pdf.pdf(qr, null);
			return Response.ok(doc).type("application/pdf").header(
					"content-disposition",
					"attachment; filename = export.pdf").header(
							"content-length",doc.length).build();
			
		} catch (Exception e) {
			log.error("Error exporting PDF for file: " + file, e);
			return Response.serverError().entity(e.getMessage()).status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GET
	@Produces({"application/json" })
	@Path("/saiku/json")
	public Response exportJson(@QueryParam("file") String file, 
			@QueryParam("formatter") String formatter,
			@Context HttpServletRequest servletRequest) 
	{
		try {
			Response f = repository.getResource(file);
			String fileContent = new String( (byte[]) f.getEntity());
			fileContent = ServletUtil.replaceParameters(servletRequest, fileContent);
			String queryName = UUID.randomUUID().toString();
			queryResource.createQuery(null,  null,  null, null, fileContent, queryName, null);
			QueryResult qr = queryResource.execute(queryName, formatter, 0);
			return Response.ok().entity(qr).build();
		} catch (Exception e) {
			log.error("Error exporting JSON for file: " + file, e);
			return Response.serverError().entity(e.getMessage()).status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@POST
	@Produces({"image/*" })
	@Path("/saiku/chart")
	public Response exportChart(
			@FormParam("type") @DefaultValue("png")  String type,
			@FormParam("svg") String svg,
			@FormParam("size") Integer size) 
	{
		try {
			final String imageType = type.toUpperCase();
			Converter converter = Converter.byType(imageType);
			if (converter == null)
			{
				throw new Exception("Image convert is null");
			}


			//		       resp.setContentType(converter.getContentType());
			//		       resp.setHeader("Content-disposition", "attachment; filename=chart." + converter.getExtension());
			//		       final Integer size = req.getParameter("size") != null? Integer.parseInt(req.getParameter("size")) : null;
			//		       final String svgDocument = req.getParameter("svg");
			//		       if (svgDocument == null)
			//		       {
			//		           resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'svg' parameter");
			//		           return;
			//		       }
			if (StringUtils.isBlank(svg)) {
				throw new Exception("Missing 'svg' parameter");
			}
			final InputStream in = new ByteArrayInputStream(svg.getBytes("UTF-8"));
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			converter.convert(in, out, size);
			out.flush();
			byte[] doc = out.toByteArray();
			return Response.ok(doc).type(converter.getContentType()).header(
					"content-disposition",
					"attachment; filename = chart." + converter.getExtension()).header(
							"content-length",doc.length).build();
		} catch (Exception e) {
			log.error("Error exporting Chart to  " + type, e);
			return Response.serverError().entity(e.getMessage()).status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
}

