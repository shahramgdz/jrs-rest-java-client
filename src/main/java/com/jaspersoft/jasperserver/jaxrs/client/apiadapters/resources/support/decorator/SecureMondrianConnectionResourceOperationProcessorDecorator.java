/*
 * Copyright (C) 2005 - 2014 Jaspersoft Corporation. All rights  reserved.
 * http://www.jaspersoft.com.
 *
 * Unless you have purchased  a commercial license agreement from Jaspersoft,
 * the following license terms  apply:
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License  as
 * published by the Free Software Foundation, either version 3 of  the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero  General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public  License
 * along with this program.&nbsp; If not, see <http://www.gnu.org/licenses/>.
 */
package com.jaspersoft.jasperserver.jaxrs.client.apiadapters.resources.support.decorator;

import com.jaspersoft.jasperserver.dto.resources.ClientSecureMondrianConnection;
import com.jaspersoft.jasperserver.jaxrs.client.apiadapters.resources.support.processor.CommonOperationProcessorImpl;
import com.jaspersoft.jasperserver.jaxrs.client.core.SessionStorage;
import com.jaspersoft.jasperserver.jaxrs.client.core.operationresult.OperationResult;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import javax.ws.rs.core.MediaType;

/**
 * @author Alexander Krasnyanskiy
 */
public abstract class SecureMondrianConnectionResourceOperationProcessorDecorator {
    protected CommonOperationProcessorImpl<ClientSecureMondrianConnection> processor;
    protected ClientSecureMondrianConnection connection;
    protected MultipartFormDataOutput multipart;

    public SecureMondrianConnectionResourceOperationProcessorDecorator(SessionStorage sessionStorage, ClientSecureMondrianConnection connection) {
        this.processor = new CommonOperationProcessorImpl(connection, connection.getClass(), sessionStorage);
        this.multipart = new MultipartFormDataOutput();
        this.connection = connection;
    }

    public OperationResult<ClientSecureMondrianConnection> createInFolder(String path) {
        return processor.create(multipart, new MediaType("application", "repository.secureMondrianConnection+xml"), path);
    }

    public CommonOperationProcessorImpl<ClientSecureMondrianConnection> getProcessor() {
        return processor;
    }

    public ClientSecureMondrianConnection getConnection() {
        return connection;
    }

    public MultipartFormDataOutput getMultipart() {
        return multipart;
    }
}
