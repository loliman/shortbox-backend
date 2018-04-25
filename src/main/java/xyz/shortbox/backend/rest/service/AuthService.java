package xyz.shortbox.backend.rest.service;

import io.swagger.annotations.*;
import org.apache.commons.validator.routines.EmailValidator;
import xyz.shortbox.backend.ejb.AuthBean;
import xyz.shortbox.backend.error.Error;
import xyz.shortbox.backend.error.Errors;
import xyz.shortbox.backend.exception.BadRequestException;
import xyz.shortbox.backend.rest.annotation.Secured;
import xyz.shortbox.backend.rest.util.AuthUserProvider;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;

@Path("/auth")
@RequestScoped
@Api(value = "Authorization")
public class AuthService extends BaseService {

    @EJB
    private AuthBean authBean;

    @Inject
    private AuthUserProvider producer;

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Creates a new session for the provided user",
            notes = "The mail has to be a known and valid mail",
            response = String.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Session successfully created", response = String.class),
            @ApiResponse(code = 400, message = "Mail or password are not set, mail is not a valid mail address", response = Error.class),
            @ApiResponse(code = 401, message = "Unknown mail", response = Error.class, responseHeaders = @ResponseHeader(name = "WWW-Authenticate", description = "Describes the domain on which login failed", response = String.class)),
            @ApiResponse(code = 403, message = "User is not in state ACTIVE", response = Error.class),
            @ApiResponse(code = 409, message = "A session for this user and ip already exists", response = Error.class)})
    public Response login(@ApiParam(value = "mail of the user that should be logged in", required = true) @QueryParam("mail") String mail,
                          @ApiParam(value = "the users password", required = true) @QueryParam("password") String password,
                          @ApiParam(value = "whether the session should timeout after 15 min or not (false = timeout)") @QueryParam("keep") Boolean keep,
                          @Context HttpServletRequest req) {
        try {
            if (mail == null || password == null || password.isEmpty())
                throw new BadRequestException(Errors.INVALID_PARAMETERS);

            if (!EmailValidator.getInstance().isValid(mail))
                throw new BadRequestException(Errors.INVALID_MAIL);

            if (keep == null) keep = false;

            String remoteHost = req.getRemoteHost();
            String remoteAddr = req.getRemoteAddr();
            int remotePort = req.getRemotePort();
            String address = remoteHost + " (" + remoteAddr + ":" + remotePort + ")";
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(address.getBytes());
            address = DatatypeConverter.printHexBinary(md.digest());

            String token = authBean.login(mail, password, address, keep);

            return Response.created(null).entity(token).build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @DELETE
    @Secured
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Deletes the current users session for this IP",
            notes = "Only removes the session for the current user and the current IP<br>" +
                    "Also keep = true sessions will be removed<br>" +
                    "<b>The user needs to be logged in</b>"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Session successfully removed"),
            @ApiResponse(code = 401, message = "User is not logged in", response = Error.class),
            @ApiResponse(code = 404, message = "Unknown session token", response = Error.class)})
    public Response logout() {
        try {
            authBean.logout(producer.getAuthenticationToken());

            return Response.noContent().build();
        } catch (Exception e) {
            return handleException(e);
        }
    }
}
