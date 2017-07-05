package com.yibo.saiku.web.rest.resources;

import com.yibo.saiku.web.rest.objects.repository.IRepositoryObject;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;

public interface ISaikuRepository {

	/**
	 * Get Saved Queries.
	 * @return A list of SavedQuery Objects.
	 */
	@GET
	@Produces({ "application/json" })
	public List<IRepositoryObject> getRepository(
            @QueryParam("path") String path, @QueryParam("type") String type);

	/**
	 * Load a resource.
	 * @param file - The name of the repository file to load.
	 * @param path - The path of the given file to load.
	 * @return A Repository File Object.
	 */
	@GET
	@Produces({ "text/plain" })
	@Path("/resource")
	public Response getResource(@QueryParam("file") String file);

	/**
	 * Save a resource.
	 * @param file - The name of the repository file to load.
	 * @param path - The path of the given file to load.
	 * @param content - The content to save.
	 * @return Status
	 */
	@POST
	@Path("/resource")
	public Response saveResource(@FormParam("file") String file,
                                 @FormParam("content") String content);

	/**
	 * Delete a resource.
	 * @param file - The name of the repository file to load.
	 * @param path - The path of the given file to load.
	 * @return Status
	 */
	@DELETE
	@Path("/resource")
	public Response deleteResource(@QueryParam("file") String file);

}