package xyz.shortbox.backend.rest.service;

import io.swagger.annotations.*;
import org.apache.commons.validator.routines.EmailValidator;
import xyz.shortbox.backend.dto.UserDTO;
import xyz.shortbox.backend.ejb.UserBean;
import xyz.shortbox.backend.ejb.entity.UserEntity;
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

@Path("/user")
@RequestScoped
@Api(value = "User Management")
public class UserService extends BaseService {

    @EJB
    private UserBean userBean;

    @Inject
    private AuthUserProvider producer;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Creates a new user",
            notes = "The mail has to be a valid mail<br>" +
                    "You have to call finishRegistration afterwards to be able to log in<br>" +
                    "Token will only be returned in test mode",
            response = String.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "User successfully created", response = String.class),
            @ApiResponse(code = 400, message = "Password or mail are empty or not valid", response = Error.class),
            @ApiResponse(code = 409, message = "A user with this mail already exists", response = Error.class)})
    public Response register(@ApiParam(value = "the users mail", required = true) @QueryParam("mail") String mail,
                             @ApiParam(value = "the users password", required = true) @QueryParam("password") String password) {
        try {
            if (password == null || password.isEmpty() || mail == null)
                throw new BadRequestException(Errors.INVALID_PARAMETERS);

            if (!EmailValidator.getInstance().isValid(mail))
                throw new BadRequestException(Errors.INVALID_MAIL);

            String token = userBean.register(mail, password);

            return Response.created(null).entity(token).build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PUT
    @Path("/finishRegistration")
    @ApiOperation(value = "Finishes the registration process",
            notes = "The mail has to be a valid mail"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "User successfully created"),
            @ApiResponse(code = 400, message = "Token is empty", response = Error.class),
            @ApiResponse(code = 404, message = "No user for token exists", response = Error.class),
            @ApiResponse(code = 409, message = "User is not in state REGISTER", response = Error.class)})
    public Response finishRegistration(@ApiParam(value = "the token that was sent by mail", required = true) @QueryParam("token") String token) {
        try {
            if (token == null || token.isEmpty())
                throw new BadRequestException(Errors.INVALID_PARAMETERS);

            userBean.finishRegistration(token);

            return Response.noContent().build();
        } catch (Exception e) {
            return handleException(e);
        }
    }


    @PUT
    @Path("/forgotPassword")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Starts the forgot password process",
            notes = "The mail has to be a valid and known mail<br>" +
                    "Token will be returned in test mode",
            response = String.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Forgot password process successfully started", response = String.class),
            @ApiResponse(code = 400, message = "Mail is empty", response = Error.class),
            @ApiResponse(code = 404, message = "No user for mail and password found", response = Error.class),
            @ApiResponse(code = 409, message = "User is not in state ACTIVE", response = Error.class)})
    public Response forgotPassword(@ApiParam(value = "the users mail", required = true) @QueryParam("mail") String mail) {
        try {
            if (mail == null || mail.isEmpty())
                throw new BadRequestException(Errors.INVALID_PARAMETERS);

            String token = userBean.forgotPassword(mail);

            return Response.ok(token).build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PUT
    @Path("/resetPassword")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Finalizes the forgot password process and resets the password",
            notes = "The token has been sent via mail before"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Forgot password completed"),
            @ApiResponse(code = 400, message = "Token or new password is empty", response = Error.class),
            @ApiResponse(code = 404, message = "No user for token found", response = Error.class),
            @ApiResponse(code = 409, message = "User is not in state FORGOT_PASSWORD", response = Error.class)})
    public Response resetPassword(@ApiParam(value = "the token that was sent by mail", required = true) @QueryParam("token") String token,
                                  @ApiParam(value = "the new password", required = true) @QueryParam("newPassword") String newPassword) {
        try {
            if (token == null || token.isEmpty() || newPassword == null || newPassword.isEmpty())
                throw new BadRequestException(Errors.INVALID_PARAMETERS);

            userBean.resetPassword(token, newPassword);

            return Response.noContent().build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PUT
    @Secured
    @Path("/changePassword")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Changes the current users password",
            notes = "The old and current password have to match<br>" +
                    "<b>The user needs to be logged in</b>"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Forgot password completed"),
            @ApiResponse(code = 400, message = "Old or new password is empty", response = Error.class),
            @ApiResponse(code = 401, message = "User is not logged in", response = Error.class, responseHeaders = @ResponseHeader(name = "WWW-Authenticate", description = "Describes the domain on which login failed", response = String.class)),
            @ApiResponse(code = 403, message = "Old and new password do not match or user is not USER or ADMINISTRATOR", response = Error.class),
            @ApiResponse(code = 409, message = "User is not in state ACTIVE", response = Error.class)})
    public Response changePassword(@ApiParam(value = "the old password", required = true) @QueryParam("oldPassword") String oldPassword,
                                   @ApiParam(value = "the new password", required = true) @QueryParam("newPassword") String newPassword) {
        try {
            if (oldPassword == null || oldPassword.isEmpty() || newPassword == null || newPassword.isEmpty())
                throw new BadRequestException(Errors.INVALID_PARAMETERS);

            userBean.changePassword(producer.getAuthenticatedUser(), oldPassword, newPassword);

            return Response.noContent().build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @DELETE
    @Secured
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Deletes the current user and all related data",
            notes = "This process is permanent and can not be undone<br>" +
                    "<b>The user needs to be logged in</b>"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "User successfully deleted"),
            @ApiResponse(code = 400, message = "Password is empty", response = Error.class),
            @ApiResponse(code = 401, message = "User is not logged in", response = Error.class, responseHeaders = @ResponseHeader(name = "WWW-Authenticate", description = "Describes the domain on which login failed", response = String.class)),
            @ApiResponse(code = 403, message = "Password and current password do not match or user is not USER or ADMINISTRATOR", response = Error.class),
            @ApiResponse(code = 409, message = "User is not in state ACTIVE", response = Error.class)})
    public Response deleteUser(@ApiParam(value = "the current password", required = true) @QueryParam("password") String password) {
        try {
            if (password == null || password.isEmpty())
                throw new BadRequestException(Errors.INVALID_PARAMETERS);

            userBean.deleteUser(producer.getAuthenticatedUser(), password);

            return Response.noContent().build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GET
    @Secured
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Returns the information about the current user",
            notes = "<b>The user needs to be logged in</b>",
            response = UserEntity.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "User successfully returned", response = UserDTO.class),
            @ApiResponse(code = 401, message = "User is not logged in", response = Error.class, responseHeaders = @ResponseHeader(name = "WWW-Authenticate", description = "Describes the domain on which login failed", response = String.class)),
            @ApiResponse(code = 403, message = "User is not USER or ADMINISTRATOR", response = Error.class)})
    public Response getUser() {
        try {
            UserDTO user = userBean.getUser(producer.getAuthenticatedUser());
            return Response.ok(user).build();
        } catch (Exception e) {
            return handleException(e);
        }
    }
}
