import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.server.api.*;
import org.apache.olingo.commons.api.data.*;
import org.apache.olingo.commons.api.data.ContextURL.Suffix;
import org.apache.olingo.commons.api.edm.*;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.format.ODataFormat;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.deserializer.DeserializerException;
import org.apache.olingo.server.api.processor.*;
import org.apache.olingo.server.api.serializer.*;
import org.apache.olingo.server.api.uri.*;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;

public class TasksProcessor implements EntityCollectionProcessor, EntityProcessor, ComplexProcessor, PrimitiveProcessor {

    private TasksDataProvider dataProvider;
    private OData odata;
    private ServiceMetadata metaData;
    
    public TasksProcessor(TasksDataProvider arg0) {
        dataProvider = arg0;
    }

    private EdmEntitySet getEdmEntitySet(final UriInfoResource uriInfo) throws ODataApplicationException {
        final List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        /*
         * To get the entity set we have to interpret all URI segments
         */
        if (!(resourcePaths.get(0) instanceof UriResourceEntitySet)) {
            throw new ODataApplicationException("Invalid resource type for first segment.", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
                    Locale.ENGLISH);
        }

        /*
         * Here we should interpret the whole URI but in this example we do not
         * support navigation so we throw an exception
         */

        final UriResourceEntitySet uriResource = (UriResourceEntitySet) resourcePaths.get(0);
        return uriResource.getEntitySet();
    }

    private ContextURL getContextUrl(final ODataSerializer serializer, final EdmEntitySet entitySet, final boolean isSingleEntity,
            final ExpandOption expand, final SelectOption select, final String navOrPropertyPath) throws SerializerException {

        return ContextURL.with().entitySet(entitySet)
                .selectList(odata.createUriHelper().buildContextURLSelectList(entitySet.getEntityType(), expand, select))
                .suffix(isSingleEntity ? Suffix.ENTITY : null).navOrPropertyPath(navOrPropertyPath).build();
    }

    private Entity readEntityInternal(final UriInfoResource uriInfo, final EdmEntitySet entitySet)
            throws TasksDataProvider.DataProviderException {

        // This method will extract the key values and pass them to the data
        // provider
        final UriResourceEntitySet resourceEntitySet = (UriResourceEntitySet) uriInfo.getUriResourceParts().get(0);
        return dataProvider.read(entitySet, resourceEntitySet.getKeyPredicates());
    }

    private void readProperty(ODataResponse response, UriInfo uriInfo, ContentType contentType, boolean complex)
            throws ODataApplicationException, SerializerException {

        // To read a property we have to first get the entity out of the entity
        // set
        final EdmEntitySet edmEntitySet = getEdmEntitySet(uriInfo.asUriInfoResource());
        Entity entity;
        try {
            entity = readEntityInternal(uriInfo.asUriInfoResource(), edmEntitySet);
        }
        catch (TasksDataProvider.DataProviderException e) {
            throw new ODataApplicationException(e.getMessage(), 500, Locale.ENGLISH);
        }

        if (entity == null) {
            // If no entity was found for the given key we throw an exception.
            throw new ODataApplicationException("No entity found for this key", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }
        else {
            // Next we get the property value from the entity and pass the value
            // to serialization
            UriResourceProperty uriProperty = (UriResourceProperty) uriInfo.getUriResourceParts()
                    .get(uriInfo.getUriResourceParts().size() - 1); // Last
                                                                    // segment
            EdmProperty edmProperty = uriProperty.getProperty();
            Property property = entity.getProperty(edmProperty.getName());
            if (property == null) {
                throw new ODataApplicationException("No property found", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
            }
            else {
                if (property.getValue() == null) {
                    response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
                }
                else {
                    // Create suitable serializer depending on the HTTP accept
                    // header.
                    final ODataFormat format = ODataFormat.fromContentType(contentType);
                    ODataSerializer serializer = odata.createSerializer(format);

                    // Build context URL. JSON representation with no metadata
                    // do not need a context URL.
                    final ContextURL contextURL = (format == ODataFormat.JSON_NO_METADATA) ? null
                            : getContextUrl(serializer, edmEntitySet, true, null, null, edmProperty.getName());

                    // Serialize
                    InputStream serializerContent = complex
                            ? serializer.complex((EdmComplexType) edmProperty.getType(), property,
                                    ComplexSerializerOptions.with().contextURL(contextURL).build())
                            : serializer.primitive((EdmPrimitiveType) edmProperty.getType(), property,
                                    PrimitiveSerializerOptions.with().contextURL(contextURL).scale(edmProperty.getScale())
                                            .nullable(edmProperty.isNullable()).precision(edmProperty.getPrecision())
                                            .maxLength(edmProperty.getMaxLength()).unicode(edmProperty.isUnicode()).build());

                    // Set result to the OData response object
                    response.setContent(serializerContent);
                    response.setStatusCode(HttpStatusCode.OK.getStatusCode());
                    response.setHeader(HttpHeader.CONTENT_TYPE, contentType.toContentTypeString());
                }
            }
        }
    }

    public void readComplex(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestContentType)
            throws ODataApplicationException, SerializerException {
        readProperty(response, uriInfo, requestContentType, true);
    }

    public void readPrimitive(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestContentType)
            throws ODataApplicationException, SerializerException {
        readProperty(response, uriInfo, requestContentType, false);
    }

    public void readEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestedContentType)
            throws ODataApplicationException, SerializerException {

        // First we have to figure out which entity set the requested entity is
        // in
        final EdmEntitySet edmEntitySet = getEdmEntitySet(uriInfo.asUriInfoResource());

        // Next we fetch the requested entity from the database
        Entity entity = null;
        try {
            entity = readEntityInternal(uriInfo.asUriInfoResource(), edmEntitySet);
        }
        catch (TasksDataProvider.DataProviderException e) {
            throw new ODataApplicationException(e.getMessage(), 500, Locale.ENGLISH);
        }

        if (entity == null) {
            // If no entity was found for the given key we throw an exception.
            throw new ODataApplicationException("No entity found for this key", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }
        else {
            // If an entity was found we proceed by serializing it and sending
            // it to the client.
            final ODataFormat format = ODataFormat.fromContentType(requestedContentType);
            ODataSerializer serializer = odata.createSerializer(format);
            final ExpandOption expand = uriInfo.getExpandOption();
            final SelectOption select = uriInfo.getSelectOption();
            InputStream serializedContent = serializer.entity(edmEntitySet.getEntityType(), entity,
                    EntitySerializerOptions.with()
                            .contextURL(format == ODataFormat.JSON_NO_METADATA ? null
                                    : getContextUrl(serializer, edmEntitySet, true, expand, select, null))
                            .expand(expand).select(select).build());
            response.setContent(serializedContent);
            response.setStatusCode(HttpStatusCode.OK.getStatusCode());
            response.setHeader(HttpHeader.CONTENT_TYPE, requestedContentType.toContentTypeString());
        }
    }

    public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestedContentType)
            throws ODataApplicationException, SerializerException {

        // First we have to figure out which entity set to use
        final EdmEntitySet edmEntitySet = getEdmEntitySet(uriInfo.asUriInfoResource());

        // Second we fetch the data for this specific entity set from the mock
        // database and transform it into an EntitySet
        // object which is understood by our serialization
        EntitySet entitySet = dataProvider.readAll(edmEntitySet);

        // Next we create a serializer based on the requested format. This could
        // also be a custom format but we do not
        // support them in this example
        final ODataFormat format = ODataFormat.fromContentType(requestedContentType);
        ODataSerializer serializer = odata.createSerializer(format);

        // Now the content is serialized using the serializer.
        final ExpandOption expand = uriInfo.getExpandOption();
        final SelectOption select = uriInfo.getSelectOption();
        InputStream serializedContent = serializer.entityCollection(edmEntitySet.getEntityType(), entitySet,
                EntityCollectionSerializerOptions.with()
                        .contextURL(format == ODataFormat.JSON_NO_METADATA ? null
                                : getContextUrl(serializer, edmEntitySet, false, expand, select, null))
                        .count(uriInfo.getCountOption()).expand(expand).select(select).build());

        // Finally we set the response data, headers and status code
        response.setContent(serializedContent);
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, requestedContentType.toContentTypeString());
    }

    /*
     * Methods to implement
     */
    public void deletePrimitive(ODataRequest arg0, ODataResponse arg1, UriInfo arg2) throws ODataApplicationException {
        // TODO Implement this!
        throw new ODataApplicationException("Delete primitive is not supported yet.", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
                Locale.ROOT);
    }

    public void updatePrimitive(ODataRequest arg0, ODataResponse arg1, UriInfo arg2, ContentType arg3, ContentType arg4)
            throws ODataApplicationException, DeserializerException, SerializerException {
        // TODO Implement this!
        throw new ODataApplicationException("Update primitive is not supported yet.", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
                Locale.ROOT);
    }

    public void deleteComplex(ODataRequest arg0, ODataResponse arg1, UriInfo arg2) throws ODataApplicationException {
        // TODO Implement this!
        throw new ODataApplicationException("Delete complex is not supported yet.", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
                Locale.ROOT);
    }

    public void updateComplex(ODataRequest arg0, ODataResponse arg1, UriInfo arg2, ContentType arg3, ContentType arg4)
            throws ODataApplicationException, DeserializerException, SerializerException {
        // TODO Implement this!
        throw new ODataApplicationException("Update complex is not supported yet.", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
                Locale.ROOT);
    }

    public void createEntity(ODataRequest arg0, ODataResponse arg1, UriInfo arg2, ContentType arg3, ContentType arg4)
            throws ODataApplicationException, DeserializerException, SerializerException {
        // TODO Implement this!
        throw new ODataApplicationException("Create entity is not supported yet.", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
                Locale.ROOT);
    }

    public void deleteEntity(ODataRequest arg0, ODataResponse arg1, UriInfo arg2) throws ODataApplicationException {
        // TODO Implement this!
        throw new ODataApplicationException("Delete entity is not supported yet.", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
                Locale.ROOT);
    }

    public void updateEntity(ODataRequest arg0, ODataResponse arg1, UriInfo arg2, ContentType arg3, ContentType arg4)
            throws ODataApplicationException, DeserializerException, SerializerException {
        // TODO Implement this!
        throw new ODataApplicationException("Update entity is not supported yet.", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
                Locale.ROOT);
    }
    
    public void init(OData arg0, ServiceMetadata arg1) {
        // TODO Auto-generated method stub
        odata = arg0;
        metaData = arg1;
    }
    
    public ServiceMetadata getServiceMetadata() {
        return metaData;
    }
    
}
