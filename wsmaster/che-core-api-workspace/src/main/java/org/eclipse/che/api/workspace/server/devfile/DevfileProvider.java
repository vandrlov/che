package org.eclipse.che.api.workspace.server.devfile;

import org.eclipse.che.api.core.model.workspace.devfile.Devfile;
import org.eclipse.che.api.workspace.server.DtoConverter;
import org.eclipse.che.api.workspace.server.devfile.exception.DevfileFormatException;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceDto;
import org.eclipse.che.api.workspace.shared.dto.devfile.DevfileDto;
import org.eclipse.che.dto.server.JsonSerializable;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Singleton
@Provider
@Produces({MediaType.APPLICATION_JSON})
@Consumes({APPLICATION_JSON, "text/yaml", "text/x-yaml"})
public class DevfileProvider implements MessageBodyReader<DevfileDto>, MessageBodyWriter<DevfileDto> {

    private DevfileManager manager;

    @Inject
    public DevfileProvider(DevfileManager manager) {
        this.manager = manager;
    }


    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == DevfileDto.class;
    }

    @Override
    public DevfileDto readFrom(Class<DevfileDto> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        if (mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
            try {
                Devfile v = manager.parseJson(new InputStreamReader(entityStream));
                return (DevfileDto) v;
            } catch (DevfileFormatException e) {
                throw new ClientErrorException(e.getMessage(), 400, e);
            }
        } else if (mediaType.isCompatible(MediaType.valueOf("text/yaml")) || mediaType.isCompatible(MediaType.valueOf("text/x-yaml"))) {
            try {
                return DtoConverter.asDto(manager.parseYaml(new InputStreamReader(entityStream)));
            } catch (DevfileFormatException e) {
                throw new ClientErrorException(e.getMessage(), 400, e);
            }
        }
        throw new ClientErrorException("Unknown media type " + mediaType.toString(), 400);
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return DevfileDto.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(DevfileDto devfile, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(DevfileDto devfile, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        httpHeaders.putSingle(HttpHeaders.CACHE_CONTROL, "public, no-cache, no-store, no-transform");
        try (Writer w = new OutputStreamWriter(entityStream, StandardCharsets.UTF_8)) {
            ((JsonSerializable) devfile).toJson(w);
        }
    }
}
