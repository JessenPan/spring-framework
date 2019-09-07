/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jca.cci.connection;

import org.springframework.beans.factory.InitializingBean;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.cci.*;

/**
 * CCI {@link ConnectionFactory} implementation that delegates all calls
 * to a given target {@link ConnectionFactory}.
 * <p>
 * <p>This class is meant to be subclassed, with subclasses overriding only
 * those methods (such as {@link #getConnection()}) that should not simply
 * delegate to the target {@link ConnectionFactory}.
 *
 * @author Juergen Hoeller
 * @see #getConnection
 * @since 1.2
 */
@SuppressWarnings("serial")
public class DelegatingConnectionFactory implements ConnectionFactory, InitializingBean {

    private ConnectionFactory targetConnectionFactory;

    /**
     * Return the target ConnectionFactory that this ConnectionFactory should delegate to.
     */
    public ConnectionFactory getTargetConnectionFactory() {
        return this.targetConnectionFactory;
    }

    /**
     * Set the target ConnectionFactory that this ConnectionFactory should delegate to.
     */
    public void setTargetConnectionFactory(ConnectionFactory targetConnectionFactory) {
        this.targetConnectionFactory = targetConnectionFactory;
    }

    public void afterPropertiesSet() {
        if (getTargetConnectionFactory() == null) {
            throw new IllegalArgumentException("Property 'targetConnectionFactory' is required");
        }
    }


    public Connection getConnection() throws ResourceException {
        return getTargetConnectionFactory().getConnection();
    }

    public Connection getConnection(ConnectionSpec connectionSpec) throws ResourceException {
        return getTargetConnectionFactory().getConnection(connectionSpec);
    }

    public RecordFactory getRecordFactory() throws ResourceException {
        return getTargetConnectionFactory().getRecordFactory();
    }

    public ResourceAdapterMetaData getMetaData() throws ResourceException {
        return getTargetConnectionFactory().getMetaData();
    }

    public Reference getReference() throws NamingException {
        return getTargetConnectionFactory().getReference();
    }

    public void setReference(Reference reference) {
        getTargetConnectionFactory().setReference(reference);
    }

}
