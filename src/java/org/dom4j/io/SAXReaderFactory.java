package org.dom4j.io;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * @author Konstantin Yakimov <a href="mailto:kiakimov@gmail.com>kiakimov@gmail.com</a>
 */
public class SAXReaderFactory extends BasePooledObjectFactory<SAXReader> {

    @Override
    public SAXReader create() throws Exception {
        return new SAXReader();
    }

    @Override
    public PooledObject<SAXReader> wrap(SAXReader saxReader) {
        return new DefaultPooledObject<SAXReader>(saxReader);
    }
}
