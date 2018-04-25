package xyz.shortbox.backend.rest;

import xyz.shortbox.backend.dto.*;
import xyz.shortbox.backend.rest.service.AuthService;
import xyz.shortbox.backend.rest.service.IssueService;
import xyz.shortbox.backend.rest.service.UserService;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/api")
public class ShortboxApp extends Application {

    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();

        //DTOs
        classes.add(IssueDTO.class);
        classes.add(ListDTO.class);
        classes.add(PublisherDTO.class);
        classes.add(SeriesDTO.class);
        classes.add(StoryDTO.class);
        classes.add(TagDTO.class);
        classes.add(UserDTO.class);
        classes.add(Error.class);

        //Services
        classes.add(IssueService.class);
        classes.add(AuthService.class);
        classes.add(UserService.class);

        classes.add(com.github.phillipkruger.apiee.ApieeService.class);
        return classes;
    }
}
