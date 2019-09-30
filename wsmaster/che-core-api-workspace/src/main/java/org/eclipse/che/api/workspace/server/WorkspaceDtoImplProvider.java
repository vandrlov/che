package org.eclipse.che.api.workspace.server;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.model.workspace.devfile.Devfile;
import org.eclipse.che.api.workspace.server.devfile.DevfileManager;
import org.eclipse.che.api.workspace.server.devfile.FileContentProvider;
import org.eclipse.che.api.workspace.server.devfile.URLFetcher;
import org.eclipse.che.api.workspace.server.devfile.URLFileContentProvider;
import org.eclipse.che.api.workspace.server.devfile.exception.DevfileFormatException;
import org.eclipse.che.api.workspace.server.devfile.validator.DevfileIntegrityValidator;
import org.eclipse.che.api.workspace.server.devfile.validator.DevfileSchemaValidator;
import org.eclipse.che.api.workspace.server.dto.DtoServerImpls;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.eclipse.che.api.workspace.server.model.impl.devfile.DevfileImpl;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceDto;
import org.eclipse.che.api.workspace.shared.dto.devfile.DevfileDto;
import org.eclipse.che.dto.server.DtoFactory;
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
@Consumes({APPLICATION_JSON})
public class WorkspaceDtoImplProvider implements MessageBodyReader<WorkspaceDto>, MessageBodyWriter<WorkspaceDto> {
    private final URLFetcher urlFetcher;
    private final DevfileSchemaValidator validator;
    private final URLFileContentProvider devfileContentProvider;
    private DevfileManager devfileManager;
    private DevfileIntegrityValidator devfileIntegrityValidator;
    protected ObjectMapper mapper;

    @Inject
    public WorkspaceDtoImplProvider(URLFetcher urlFetcher, DevfileSchemaValidator validator, DevfileManager devfileManager, DevfileIntegrityValidator devfileIntegrityValidator) {
        this.urlFetcher = urlFetcher;
        this.validator = validator;
        this.devfileContentProvider = new URLFileContentProvider(null, urlFetcher);
        this.devfileManager = devfileManager;
        this.devfileIntegrityValidator = devfileIntegrityValidator;
        mapper = new ObjectMapper();


    }


    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == WorkspaceDto.class;
    }

    @Override
    public WorkspaceDto readFrom(Class<WorkspaceDto> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
//        if (mediaType.equals(MediaType.APPLICATION_JSON_TYPE)) {
//            try {
//                manager.parseJson(new InputStreamReader(entityStream));
//            } catch (DevfileFormatException e) {
//                throw new ClientErrorException(e.getMessage(), 400, e);
//            }
//        } else if (mediaType.toString().equals("text/yaml") || (mediaType.toString().equals("text/x-yaml"))) {
//            try {
//                manager.parseYaml(new InputStreamReader(entityStream));
//            } catch (DevfileFormatException e) {
//                throw new ClientErrorException(e.getMessage(), 400, e);
//            }
//        }
//        throw new ClientErrorException("Unknown media type " + mediaType.toString(), 400);


        try {
            JsonNode wsNode = mapper.readTree(entityStream);
            JsonNode devfileNode = wsNode.path("devfile");
            if (!devfileNode.isNull()) {
                Devfile devfile = devfileManager.parseJson(devfileNode.toString());
                if(devfile !=null) {
                    devfileIntegrityValidator.validateContentReferences(devfile, FileContentProvider.cached(devfileContentProvider));
                }
            }
            return DtoFactory.getInstance().createDtoFromJson(wsNode.toString(), WorkspaceDto.class);
        } catch (DevfileFormatException e) {
            throw new ClientErrorException("Invalid davfile: " + e.getMessage(), 400);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return WorkspaceDto.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(WorkspaceDto devfile, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(WorkspaceDto devfile, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        httpHeaders.putSingle(HttpHeaders.CACHE_CONTROL, "public, no-cache, no-store, no-transform");
        try (Writer w = new OutputStreamWriter(entityStream, StandardCharsets.UTF_8)) {
            ((JsonSerializable) devfile).toJson(w);
        }
    }

}
