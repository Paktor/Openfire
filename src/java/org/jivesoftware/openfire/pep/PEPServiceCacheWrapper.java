package org.jivesoftware.openfire.pep;

import org.jivesoftware.util.cache.Cacheable;
import org.jivesoftware.util.cache.CannotCalculateSizeException;

/**
 * Wrapper to avoid caching null values
 *
 * @author Konstantin Yakimov <a href="mailto:kiakimov@gmail.com>kiakimov@gmail.com</a>
 */
public class PEPServiceCacheWrapper implements Cacheable {
    private PEPService service = null;

    public PEPServiceCacheWrapper(PEPService service) {
        this.service = service;
    }

    public PEPService getService() {
        return service;
    }

    public boolean isNull(){
        return service == null;
    }

    @Override
    public int getCachedSize() throws CannotCalculateSizeException {
        // Rather arbitrary. Don't use this for size-based eviction policies!
        return service == null ? 10 : service.getCachedSize();
    }
}
