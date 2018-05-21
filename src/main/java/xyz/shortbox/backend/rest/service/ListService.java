package xyz.shortbox.backend.rest.service;

import io.swagger.annotations.*;
import xyz.shortbox.backend.dto.ListDTO;
import xyz.shortbox.backend.ejb.ListBean;
import xyz.shortbox.backend.error.Error;
import xyz.shortbox.backend.error.Errors;
import xyz.shortbox.backend.exception.BadRequestException;
import xyz.shortbox.backend.rest.annotation.Secured;
import xyz.shortbox.backend.rest.util.AuthUserProvider;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/list")
@RequestScoped
@Api(value = "List Management")
public class ListService extends BaseService {

    @EJB
    private ListBean listBean;

    @Inject
    private AuthUserProvider producer;

    @GET
    @Secured
    @Produces({MediaType.APPLICATION_JSON})
    @ApiOperation(value = "Finds an list by id",
            notes = "Only a single id greater then 0 is allowed",
            response = ListDTO.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Returned list successfully", response = ListDTO.class),
            @ApiResponse(code = 400, message = "Id is less of equal 0", response = Error.class),
            @ApiResponse(code = 401, message = "User is not logged in or list does not belong to user", response = Error.class, responseHeaders = @ResponseHeader(name = "WWW-Authenticate", description = "Describes the domain on which authentication failed", response = String.class)),
            @ApiResponse(code = 404, message = "List with id not found", response = Error.class)})
    public Response getList(@ApiParam(value = "the id of the requested list", required = true) @QueryParam("id") int id) {
        try {
            if (id <= 0)
                throw new BadRequestException(Errors.INVALID_PARAMETERS);

            ListDTO list = listBean.getList(id, producer.getAuthenticatedUser());

            return Response.ok(list).build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GET
    @Secured
    @Produces({MediaType.APPLICATION_JSON})
    @Path("all")
    @ApiOperation(value = "Finds all lists for current user",
            notes = "<b>The user needs to be logged in</b>",
            response = ListDTO.class, responseContainer = "List"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Returned lists successfully", response = ListDTO.class, responseContainer = "List"),
            @ApiResponse(code = 401, message = "User is not logged in", response = Error.class, responseHeaders = @ResponseHeader(name = "WWW-Authenticate", description = "Describes the domain on which login failed", response = String.class))})
    public Response getAllLists() {
        try {
            List<ListDTO> lists = listBean.getLists(producer.getAuthenticatedUser());

            return Response.ok(lists).build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @DELETE
    @Secured
    @Produces({MediaType.APPLICATION_JSON})
    @Path("clear")
    @ApiOperation(value = "Removes all issues from list",
            notes = "<b>The user needs to be logged in</b>"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "List cleared successfully"),
            @ApiResponse(code = 400, message = "Id is less of equal 0", response = Error.class),
            @ApiResponse(code = 401, message = "User is not logged in or list does not belong to user", response = Error.class, responseHeaders = @ResponseHeader(name = "WWW-Authenticate", description = "Describes the domain on which login failed", response = String.class)),
            @ApiResponse(code = 404, message = "List with id not found", response = Error.class)})
    public Response clear(@ApiParam(value = "the id of the list that should be cleared", required = true) @QueryParam("id") int id) {
        try {
            if (id <= 0)
                throw new BadRequestException(Errors.INVALID_PARAMETERS);

            listBean.clearList(id, producer.getAuthenticatedUser());

            return Response.noContent().build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @POST
    @Secured
    @Produces({MediaType.APPLICATION_JSON})
    @ApiOperation(value = "Creates a new list",
            notes = "Every list name must be unique per user and not empty<br>" +
                    "<b>The user needs to be logged in</b>"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "List created successfully"),
            @ApiResponse(code = 400, message = "Name is empty", response = Error.class),
            @ApiResponse(code = 401, message = "User is not logged in", response = Error.class, responseHeaders = @ResponseHeader(name = "WWW-Authenticate", description = "Describes the domain on which login failed", response = String.class)),
            @ApiResponse(code = 409, message = "List with name already exists for user", response = Error.class)})
    public Response createList(@ApiParam(value = "the name of the list", required = true) @QueryParam("name") String name,
                               @ApiParam(value = "the lists sort position (default = last)") @QueryParam("sort") int sort,
                               @ApiParam(value = "the parameter the list should be grouped by (default = SERIES ASC)") @QueryParam("groupby") String groupby) {
        try {
            if (name == null || name.isEmpty())
                throw new BadRequestException(Errors.INVALID_PARAMETERS);

            listBean.createList(name, sort, groupby, producer.getAuthenticatedUser());

            return Response.created(null).build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @POST
    @Secured
    @Produces({MediaType.APPLICATION_JSON})
    @Path("addIssue")
    @ApiOperation(value = "Adds an issue to a list",
            notes = "If increase is not set method will fail with 409 if issue is already on list" +
                    "<b>The user needs to be logged in</b>"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Issue added to list successfully"),
            @ApiResponse(code = 400, message = "IssueId or ListId is less of equal 0", response = Error.class),
            @ApiResponse(code = 401, message = "User is not logged in or list does not belong to user", response = Error.class, responseHeaders = @ResponseHeader(name = "WWW-Authenticate", description = "Describes the domain on which login failed", response = String.class)),
            @ApiResponse(code = 404, message = "Issue or list with id not found", response = Error.class),
            @ApiResponse(code = 409, message = "Issue is already on list and increase is false", response = Error.class)})
    public Response addIssue(@ApiParam(value = "the id of the list", required = true) @QueryParam("listId") int listId,
                             @ApiParam(value = "the id of the issue", required = true) @QueryParam("issueId") int issueId,
                             @ApiParam(value = "if set and issue is already on list the amount will be increased by one") @QueryParam("increase") boolean increase) {
        try {
            if (listId <= 0 || issueId <= 0)
                throw new BadRequestException(Errors.INVALID_PARAMETERS);

            listBean.addIssue(listId, issueId, increase, producer.getAuthenticatedUser());

            return Response.created(null).build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @DELETE
    @Secured
    @Produces({MediaType.APPLICATION_JSON})
    @Path("removeIssue")
    @ApiOperation(value = "Removes an issue from a list",
            notes = "If decrease is set method will decrease the amount by one and remove the issue if amount is 0 afterwards" +
                    "<b>The user needs to be logged in</b>"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Issue removed from list successfully"),
            @ApiResponse(code = 400, message = "IssueId or ListId is less of equal 0", response = Error.class),
            @ApiResponse(code = 401, message = "User is not logged in or list does not belong to user", response = Error.class, responseHeaders = @ResponseHeader(name = "WWW-Authenticate", description = "Describes the domain on which login failed", response = String.class)),
            @ApiResponse(code = 404, message = "Issue or list with id not found", response = Error.class),
            @ApiResponse(code = 409, message = "Issue is not on list", response = Error.class)})
    public Response removeIssue(@ApiParam(value = "the id of the list", required = true) @QueryParam("listId") int listId,
                                @ApiParam(value = "the id of the issue", required = true) @QueryParam("issueId") int issueId,
                                @ApiParam(value = "if set amount will be decreased by one and issue will be removed if amount is 0 afterwards", required = true) @QueryParam("decrease") boolean decrease) {
        try {
            if (listId <= 0 || issueId <= 0)
                throw new BadRequestException(Errors.INVALID_PARAMETERS);

            listBean.removeIssue(listId, issueId, decrease, producer.getAuthenticatedUser());

            return Response.noContent().build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @DELETE
    @Secured
    @Produces({MediaType.APPLICATION_JSON})
    @ApiOperation(value = "Deletes a list",
            notes = "This process is permanent and can not be undone" +
                    "<b>The user needs to be logged in</b>"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "List deleted successfully"),
            @ApiResponse(code = 400, message = "ListId is less of equal 0", response = Error.class),
            @ApiResponse(code = 401, message = "User is not logged in or list does not belong to user", response = Error.class, responseHeaders = @ResponseHeader(name = "WWW-Authenticate", description = "Describes the domain on which login failed", response = String.class)),
            @ApiResponse(code = 404, message = "List with id not found", response = Error.class)})
    public Response deleteList(@ApiParam(value = "the id of the list", required = true) @QueryParam("listId") int listId) {
        try {
            if (listId <= 0)
                throw new BadRequestException(Errors.INVALID_PARAMETERS);

            listBean.deleteList(listId, producer.getAuthenticatedUser());

            return Response.noContent().build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PUT
    @Secured
    @Produces({MediaType.APPLICATION_JSON})
    @ApiOperation(value = "Updates a list with given information",
            notes = "<b>The user needs to be logged in</b>"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "List updated successfully"),
            @ApiResponse(code = 400, message = "ListId is less of equal 0 or name is empty", response = Error.class),
            @ApiResponse(code = 401, message = "User is not logged in or list does not belong to user", response = Error.class, responseHeaders = @ResponseHeader(name = "WWW-Authenticate", description = "Describes the domain on which login failed", response = String.class)),
            @ApiResponse(code = 404, message = "List with id not found", response = Error.class),
            @ApiResponse(code = 409, message = "List with name already exists for user", response = Error.class)})
    public Response editList(@ApiParam(value = "the id of the list", required = true) @QueryParam("list") ListDTO list) {
        try {
            if (list.getName() == null || list.getName().isEmpty() || list.getId() <= 0)
                throw new BadRequestException(Errors.INVALID_PARAMETERS);

            listBean.editList(list, producer.getAuthenticatedUser());

            return Response.noContent().build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PUT
    @Secured
    @Produces({MediaType.APPLICATION_JSON})
    @Path("merge")
    @ApiOperation(value = "Merges two lists",
            notes = "The lists specified by listFirstId will be the main list<br>" +
                    "All data from the list specified by listSecondId will be merged into the main list<br>" +
                    "If an issue already exists in the main list, the amount will be increased" +
                    "<b>The user needs to be logged in</b>"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Lists merged successfully"),
            @ApiResponse(code = 400, message = "listFirstId or listSecondId is less of equal 0", response = Error.class),
            @ApiResponse(code = 401, message = "User is not logged in or one list does not belong to user", response = Error.class, responseHeaders = @ResponseHeader(name = "WWW-Authenticate", description = "Describes the domain on which login failed", response = String.class)),
            @ApiResponse(code = 404, message = "One list with id not found", response = Error.class)})
    public Response mergeLists(@ApiParam(value = "the id of the first list", required = true) @QueryParam("listFirstId") int listFirstId,
                               @ApiParam(value = "the id of the second list", required = true) @QueryParam("listSecondId") int listSecondId) {
        try {
            if (listFirstId <= 0 || listSecondId <= 0)
                throw new BadRequestException(Errors.INVALID_PARAMETERS);

            listBean.mergeLists(listFirstId, listSecondId, producer.getAuthenticatedUser());

            return Response.noContent().build();
        } catch (Exception e) {
            return handleException(e);
        }
    }
}
