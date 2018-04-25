package xyz.shortbox.backend.rest.service;

import io.swagger.annotations.*;
import xyz.shortbox.backend.dto.IssueDTO;
import xyz.shortbox.backend.ejb.IssueBean;
import xyz.shortbox.backend.error.Error;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/issue")
@RequestScoped
@Api(value = "Issue")
public class IssueService extends BaseService {

    @EJB
    private IssueBean issueBean;

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @ApiOperation(value = "Finds an issue by id",
            notes = "Only a single id greater then 0 is allowed",
            response = IssueDTO.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Returned issue successfully", response = IssueDTO.class),
            @ApiResponse(code = 400, message = "Id is less of equal 0", response = Error.class),
            @ApiResponse(code = 404, message = "Issue with id not found", response = Error.class)})
    public Response getIssue(@ApiParam(value = "the id of the requested issue", required = true) @QueryParam("id") int id) {
        IssueDTO issue;

        if (id <= 0)
            return Response.status(Response.Status.BAD_REQUEST).build();

        try {
            issue = issueBean.getIssue(id);

            return Response.ok(issue).build();
        } catch (Exception e) {
            return handleException(e);
        }
    }
}
