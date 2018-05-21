package xyz.shortbox.backend.rest.service;

import io.swagger.annotations.*;
import xyz.shortbox.backend.ejb.SeriesBean;
import xyz.shortbox.backend.ejb.UserBean;
import xyz.shortbox.backend.enumeration.UserGroup;
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

@Path("/series")
@RequestScoped
@Api(value = "Series Management")
public class SeriesService extends BaseService {

    @EJB
    private SeriesBean seriesBean;

    @Inject
    private AuthUserProvider producer;

    @DELETE
    @Secured(UserGroup.ADMINISTRATOR)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteSeries(@QueryParam("seriesId") int seriesId) {
        try {
            if (seriesId <= 0)
                throw new BadRequestException(Errors.INVALID_PARAMETERS);

            seriesBean.deleteList(seriesId);

            return Response.noContent().build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PUT
    @Secured(UserGroup.ADMINISTRATOR)
    @Produces(MediaType.APPLICATION_JSON)
    public Response editSeries(@QueryParam("seriesId") int seriesId) {
        try {
            if (seriesId <= 0)
                throw new BadRequestException(Errors.INVALID_PARAMETERS);

            seriesBean.deleteList(seriesId);

            return Response.noContent().build();
        } catch (Exception e) {
            return handleException(e);
        }
    }
}