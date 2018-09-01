

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.olingo.commons.api.*;
import org.apache.olingo.commons.api.edm.*;
import org.apache.olingo.server.api.edm.provider.*;

public class TasksEdmProvider extends EdmProvider {

    // Service Namespace
    public static final String NAMESPACE = "";
    
    // EDM Container
    public static final String CONTAINER_NAME = "Container";
    public static final FullQualifiedName CONTAINER_FQN = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

    // Entity Types Names
    public static final FullQualifiedName ET_TASK = new FullQualifiedName(NAMESPACE, "Task");

    // Entity Set Names
    public static final String ES_TASKS_NAME = "Tasks";
    
    @Override
    public List<Schema> getSchemas() throws ODataException {
      List<Schema> schemas = new ArrayList<Schema>();
      Schema schema = new Schema();
      schema.setNamespace(NAMESPACE);

      // EntityTypes
      List<EntityType> entityTypes = new ArrayList<EntityType>();
      entityTypes.add(getEntityType(ET_TASK));
      schema.setEntityTypes(entityTypes);
      
      // EntityContainer
      schema.setEntityContainer(getEntityContainer());
      schemas.add(schema);

      return schemas;
    }
    
    @Override
    public EntityType getEntityType(final FullQualifiedName entityTypeName) throws ODataException {
      if (ET_TASK.equals(entityTypeName)) {
        return new EntityType()
          .setName(ET_TASK.getName())
          .setKey(Arrays.asList(
            new PropertyRef().setPropertyName("Id")))
          .setProperties(Arrays.asList(
            new Property().setName("Id").setType(EdmPrimitiveTypeKind.Int16.getFullQualifiedName()),
            new Property().setName("Name").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
            new Property().setName("Details").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
            new Property().setName("DueDate").setType(EdmPrimitiveTypeKind.Date.getFullQualifiedName()),
            new Property().setName("Alert").setType(EdmPrimitiveTypeKind.Boolean.getFullQualifiedName()),
            new Property().setName("Priority").setType(EdmPrimitiveTypeKind.Int16.getFullQualifiedName())
          ));

      }

      return null;
    }

    @Override
    public EntitySet getEntitySet(final FullQualifiedName entityContainer, final String entitySetName) throws ODataException {
      if (CONTAINER_FQN.equals(entityContainer)) {
        if (ES_TASKS_NAME.equals(entitySetName)) {
          return new EntitySet()
              .setName(ES_TASKS_NAME)
              .setType(ET_TASK);
        }
      }
      return null;
    }
    
    @Override
    public EntityContainer getEntityContainer() throws ODataException {
      EntityContainer container = new EntityContainer();
      container.setName(CONTAINER_FQN.getName());

      // EntitySets
      List<EntitySet> entitySets = new ArrayList<EntitySet>();
      container.setEntitySets(entitySets);
      entitySets.add(getEntitySet(CONTAINER_FQN, ES_TASKS_NAME));

      return container;
    }

    @Override
    public EntityContainerInfo getEntityContainerInfo(final FullQualifiedName entityContainerName) throws ODataException {
      if (entityContainerName == null || CONTAINER_FQN.equals(entityContainerName)) {
        return new EntityContainerInfo().setContainerName(CONTAINER_FQN);
      }
      return null;
    }
    
}
