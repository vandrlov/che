package org.eclipse.che.api.workspace.server;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.eclipse.che.api.core.rest.CheJsonProvider;
import org.eclipse.che.api.workspace.server.devfile.DevfileManager;
import org.eclipse.che.api.workspace.server.devfile.exception.DevfileFormatException;
import org.eclipse.che.api.workspace.server.devfile.validator.DevfileSchemaValidator;
import org.eclipse.che.api.workspace.server.dto.DtoServerImpls;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.eclipse.che.api.workspace.server.model.impl.devfile.DevfileImpl;
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
@Consumes({APPLICATION_JSON})
public class WorkspaceDtoImplProvider extends CheJsonProvider<WorkspaceDto> {
    protected DevfileSchemaValidator validator;
    protected ObjectMapper mapper;

    @Inject
    public WorkspaceDtoImplProvider(DevfileSchemaValidator validator) {
        super(null);
        this.validator = validator;
        mapper =   new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(DevfileDto.class, new DevfileDtoDeserializer());
        mapper.registerModule(module);

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
            return mapper.readValue(entityStream, DtoServerImpls.WorkspaceDtoImpl.class);
        }catch (Exception e){
            e.printStackTrace();
            throw  e;
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

     class DevfileDtoDeserializer extends JsonDeserializer<DevfileDto> {


        @Override
        public DevfileDto deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            try {
                TreeNode value = p.readValueAsTree();
                validator.validate((JsonNode) value);
                return (DevfileDto) mapper.treeToValue(value, DevfileImpl.class);
            } catch (DevfileFormatException e) {
                throw JsonMappingException.from(ctxt, e.getMessage(), e);
            }

        }
    }
}
