package xyz.shortbox.backend.ejb;

import xyz.shortbox.backend.rest.util.UniqueTokenGenerator;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Base class for all Stateless Beans.
 */
@Stateless
public class BaseBean {
    @PersistenceContext(unitName = "shortbox-unit")
    EntityManager em;

    UniqueTokenGenerator keyGen;

    @PostConstruct
    public void init() {
        keyGen = new UniqueTokenGenerator(64);
    }
}
