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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.yibo.saiku.olap.dto.*;
import com.yibo.saiku.olap.dto.SaikuSelection.Type;
import com.yibo.saiku.olap.util.SaikuProperties;
import com.yibo.saiku.service.olap.OlapQueryService;
import com.yibo.saiku.service.util.KeyValue;
import com.yibo.saiku.service.util.exception.SaikuServiceException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.olap4j.mdx.ParseTreeWriter;
import org.olap4j.mdx.SelectNode;
import org.olap4j.mdx.parser.impl.DefaultMdxParserImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.io.*;
import java.net.URLDecoder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * QueryServlet contains all the methods required when manipulating an OLAP Query.
 * @author Paul Stoellberger
 *
 */
@Component
@Path("/saiku/{username}/tags")
@XmlAccessorType(XmlAccessType.NONE)
public class BasicTagRepositoryResource {

	private static final Logger log = LoggerFactory.getLogger(BasicTagRepositoryResource.class);

	private OlapQueryService olapQueryService;

	private FileObject repo;

	public void setPath(String path) throws Exception {

		FileSystemManager fileSystemManager;
		try {
			 if (!path.endsWith("" + File.separatorChar)) {
				path += File.separatorChar;
			}
			fileSystemManager = VFS.getManager();
			FileObject fileObject;
			fileObject = fileSystemManager.resolveFile(path);
			if (fileObject == null) {
				throw new IOException("File cannot be resolved: " + path);
			}
			if(!fileObject.exists()) {
				throw new IOException("File does not exist: " + path);
			}
			repo = fileObject;
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Autowired
	public void setOlapQueryService(OlapQueryService olapqs) {
		olapQueryService = olapqs;
	}

	@GET
	@Path("/{cubeIdentifier}")
	@Produces({"application/json" })
	public List<SaikuTag> getSavedTags(
			@PathParam("cubeIdentifier") String cubeIdentifier) 
	{
		List<SaikuTag> allTags = new ArrayList<SaikuTag>();
		try {
			if (repo != null) {
				File[] files = new File(repo.getName().getPath()).listFiles();
				for (File file : files) {
					if (!file.isHidden()) {
						
						String filename = file.getName();
						if (filename.endsWith(".tag")) {
							filename = filename.substring(0,filename.length() - ".tag".length());
							if (filename.equals(cubeIdentifier)) {
								FileReader fi = new FileReader(file);
								BufferedReader br = new BufferedReader(fi);
								ObjectMapper om = new ObjectMapper();


//							    om.setVisibilityChecker(om.getVisibilityChecker().withFieldVisibility(JsonAutoDetect.Visibility.ANY));
								om.setVisibility(om.getVisibilityChecker().withFieldVisibility(JsonAutoDetect.Visibility.ANY));
//								List<SaikuTag> tags = om.readValue(file, TypeFactory.defaultInstance().constructType(ArrayList.class, SaikuTag.class));
								List<SaikuTag> tags = om.readValue(file, TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, SaikuTag.class));
//								List<SaikuTag> tags = om.readValue(file, om.getTypeFactory().constructCollectionType(ArrayList.class, SaikuTag.class));
								allTags.addAll(tags);
							}
						}
					}
				}

			}
			else {
				throw new Exception("repo URL is null");
			}
		} catch (Exception e) {
			log.error(this.getClass().getName(),e);
			e.printStackTrace();
		}
		Collections.sort(allTags);
		return allTags;
	}

	/**
	 * Delete Query.
	 * @param queryName - The name of the query.
	 * @return A GONE Status if the query was deleted, otherwise it will return a NOT FOUND Status code.
	 */
	@DELETE
	@Produces({"application/json" })
	@Path("/{cubeIdentifier}/{tagName}")
	public Status deleteTag(
			@PathParam("cubeIdentifier") String cubeIdentifier,
			@PathParam("tagName") String tagName)
	{
		try{
			if (repo != null) {
				List<SaikuTag> tags = getSavedTags(cubeIdentifier);
				List<SaikuTag> remove = new ArrayList<SaikuTag>();
				for(SaikuTag tag : tags) {
					if(tag.getName().equals(tagName)) {
						remove.add(tag);
					}
				}
				tags.removeAll(remove);
				ObjectMapper om = new ObjectMapper();
			    om.setVisibilityChecker(om.getVisibilityChecker().withFieldVisibility(JsonAutoDetect.Visibility.ANY));

				String uri = repo.getName().getPath();
				if (!uri.endsWith("" + File.separatorChar)) {
					uri += File.separatorChar;
				}
				if (!cubeIdentifier.endsWith(".tag")) {
					cubeIdentifier += ".tag";
				}

				File tagFile = new File(uri+URLDecoder.decode(cubeIdentifier, "UTF-8"));
				if (tagFile.exists()) {
					tagFile.delete();
				}
				else {
					tagFile.createNewFile();
				}
				om.writeValue(tagFile, tags);
				return(Status.GONE);
				
			}
			throw new Exception("Cannot delete tag :" + tagName );
		}
		catch(Exception e){
			log.error("Cannot delete tag (" + tagName + ")",e);
			return(Status.NOT_FOUND);
		}
	}

	@POST
	@Produces({"application/json" })
	@Path("/{cubeIdentifier}/{tagname}")
	public SaikuTag saveTag(
			@PathParam("tagname") String tagName,
			@PathParam("cubeIdentifier") String cubeIdentifier,
			@FormParam("queryname") String queryName,
			@FormParam("positions") String positions)
	{
		try {
			List<List<Integer>> cellPositions = new ArrayList<List<Integer>>();
			for (String position : positions.split(",")) {
				String[] ps = position.split(":");
				List<Integer> cellPosition = new ArrayList<Integer>();

				for (String p : ps) {
					Integer pInt = Integer.parseInt(p);
					cellPosition.add(pInt);
				}
				cellPositions.add(cellPosition);
			}
			SaikuTag t = olapQueryService.createTag(queryName, tagName, cellPositions);
			
			if (repo != null) {
				List<SaikuTag> tags = getSavedTags(cubeIdentifier);
				if (!cubeIdentifier.endsWith(".tag")) {
					cubeIdentifier += ".tag";
				}
				List<SaikuTag> remove = new ArrayList<SaikuTag>();
				for(SaikuTag tag : tags) {
					if(tag.getName().equals(tagName)) {
						remove.add(tag);
					}
				}
				tags.removeAll(remove);
				
				tags.add(t);
				ObjectMapper om = new ObjectMapper();
			    om.setVisibilityChecker(om.getVisibilityChecker().withFieldVisibility(JsonAutoDetect.Visibility.ANY));

				String uri = repo.getName().getPath();
				if (!uri.endsWith("" + File.separatorChar)) {
					uri += File.separatorChar;
				}
				File tagFile = new File(uri+URLDecoder.decode(cubeIdentifier, "UTF-8"));
				if (tagFile.exists()) {
					tagFile.delete();
				}
				else {
					tagFile.createNewFile();
				}
				om.writeValue(tagFile, tags);
				return t;
			}
		}
		catch (Exception e) {
			log.error("Cannot add tag " + tagName + " for query (" + queryName + ")",e);
		}
		return null;

	}
	
	@GET
	@Produces({"application/json" })
	@Path("/{cubeIdentifier}/{tagName}")
	public SaikuTag getTag(
			@PathParam("cubeIdentifier") String cubeIdentifier,
			@PathParam("tagName") String tagName)
	{
		try{
			if (repo != null) {
				List<SaikuTag> tags = getSavedTags(cubeIdentifier);
				for(SaikuTag tag : tags) {
					if(tag.getName().equals(tagName)) {
						return tag;
					}
				}
			}
		}
		catch (Exception e) {
			log.error("Cannot get tag " + tagName + " for " + cubeIdentifier ,e);
		}
		return null;
	}
	
	@GET
	@Produces({"text/csv" })
	@Path("/{cubeIdentifier}/{tagName}/export/csv")
	public Response getDrillthroughExport(			
			@PathParam("cubeIdentifier") String cubeIdentifier,
			@PathParam("tagName") String tagName,
			@QueryParam("maxrows") @DefaultValue("0") Integer maxrows,
			@QueryParam("returns") String returns,
			@QueryParam("connection") String connection,
			@QueryParam("catalog") String catalog,
			@QueryParam("schema") String schema,
			@QueryParam("cube") String cube,
			@QueryParam("additional") String additional
			)
	{
		ResultSet rs = null;

		try {
            
			List<Integer> cellPosition = new ArrayList<Integer>();
			cellPosition.add(0);
			List<KeyValue<String,String>> additionalColumns = new ArrayList<KeyValue<String,String>>();
			if (additional != null) {
				for (String kvs : additional.split(",")) {
					String[] kv = kvs.split(":");
					if (kv.length == 2) {
						additionalColumns.add(new KeyValue<String, String>(kv[0], kv[1]));
					}
				}
			}
			
			SaikuTag tag = getTag(cubeIdentifier, tagName);
			if (tag != null) {
				String queryName = UUID.randomUUID().toString();
				SaikuCube saikuCube = new SaikuCube(connection, cube, cube, cube, catalog, schema);
				olapQueryService.createNewOlapQuery(queryName, saikuCube);
				SaikuQuery q = olapQueryService.simulateTag(queryName, tag);
				if (!cube.startsWith("[")) {
					cube = "[" + cube + "]";
				}
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				boolean first = true;
				for (SaikuTuple tuple : tag.getSaikuTuples()) {
					String mdx = null;
					for (SaikuMember member : tuple.getSaikuMembers()) {
						if (mdx == null) {
							mdx = "SELECT (" + member.getUniqueName();
						} else {
							mdx += ", " + member.getUniqueName();
						}
					}
					boolean where = true;
					if (tag.getSaikuDimensionSelections() != null) {
						for (SaikuDimensionSelection sdim : tag.getSaikuDimensionSelections()) {
							if (sdim.getSelections().size() > 1) {
								where = false;
							}
						}
					}
					if (where) {
						mdx += ") ON COLUMNS from " + cube;
						SelectNode sn = (new DefaultMdxParserImpl().parseSelect(q.getMdx())); 
						final Writer writer = new StringWriter();
						sn.getFilterAxis().unparse(new ParseTreeWriter(new PrintWriter(writer)));
						if (StringUtils.isNotBlank(writer.toString())) {
							mdx += "\r\nWHERE " + writer.toString();
						}
						System.out.println("Executing... :" + mdx);
						olapQueryService.executeMdx(queryName, mdx);
						rs = olapQueryService.drillthrough(queryName, cellPosition, maxrows, returns);
						byte[] doc = olapQueryService.exportResultSetCsv(rs,",","\"", first, additionalColumns);
						first = false;
						outputStream.write(doc);
					} else {
						if (tag.getSaikuDimensionSelections() != null) {
							for (SaikuDimensionSelection sdim : tag.getSaikuDimensionSelections()) {
								for (SaikuSelection ss : sdim.getSelections()) {
									if (ss.getType() == Type.MEMBER) {
										String newmdx = mdx;
										newmdx += "," + ss.getUniqueName() + ") ON COLUMNS from " + cube;
										System.out.println("Executing... :" + newmdx);
										olapQueryService.executeMdx(queryName, newmdx);
										rs = olapQueryService.drillthrough(queryName, cellPosition, maxrows, returns);
										byte[] doc = olapQueryService.exportResultSetCsv(rs,",","\"", first, additionalColumns);
										first = false;
										outputStream.write(doc);
									}
								}
							}
						}
					}
				}


				byte csv[] = outputStream.toByteArray();
				
				String name = SaikuProperties.webExportCsvName;
				return Response.ok(csv, MediaType.APPLICATION_OCTET_STREAM).header(
						"content-disposition",
						"attachment; filename = " + name + "-drillthrough.csv").header(
								"content-length",csv.length).build();
			}

		} catch (Exception e) {
			log.error("Cannot export drillthrough tag (" + tagName + ")",e);
			return Response.serverError().build();
		}
		
		finally {
			if (rs != null) {
				try {
					Statement statement = rs.getStatement();
					statement.close();
					rs.close();
				} catch (SQLException e) {
					throw new SaikuServiceException(e);
				} finally {
					rs = null;
				}
			}
		}
		return Response.serverError().build();
	}

	
}
