import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntitySet;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.core.data.EntityImpl;
import org.apache.olingo.commons.core.data.EntitySetImpl;
import org.apache.olingo.commons.core.data.PropertyImpl;
import org.apache.olingo.server.api.uri.UriParameter;

public class TasksDataProvider {

    private Map<String, EntitySet> data;

    public TasksDataProvider() {
        data = new HashMap<String, EntitySet>();
        data.put("Tasks", createTasks());
    }

    public class DataProviderException extends Exception {

        private static final long serialVersionUID = 1L;

        public DataProviderException(String message, Throwable e) {
            super(message, e);
        }

    }

    private EntitySet createTasks() {
        EntitySet entitySet = new EntitySetImpl();

        entitySet.getEntities()
                .add(new EntityImpl()
                        .addProperty(createPrimitive("Id", 1))
                        .addProperty(createPrimitive("Name", "Test 1"))
                        .addProperty(createPrimitive("Details", "Some details"))
                        .addProperty(createPrimitive("DueDate", new Date()))
                        .addProperty(createPrimitive("Alert", true))
                        .addProperty(createPrimitive("Status", "Complete"))
                        .addProperty(createPrimitive("Priority", 1)));

        entitySet.getEntities()
        .add(new EntityImpl()
                .addProperty(createPrimitive("Id", 2))
                .addProperty(createPrimitive("Name", "Test 1"))
                .addProperty(createPrimitive("Details", "Some more details"))
                .addProperty(createPrimitive("DueDate", new Date()))
                .addProperty(createPrimitive("Alert", true)).addProperty(createPrimitive("Status", "Complete"))
                .addProperty(createPrimitive("Priority", 1)));
        
        return entitySet;
    }

    private Property createPrimitive(final String name, final Object value) {
        return new PropertyImpl(null, name, ValueType.PRIMITIVE, value);
    }

    public EntitySet readAll(EdmEntitySet edmEntitySet) {
        return data.get(edmEntitySet.getName());
    }

    public Entity read(final EdmEntitySet edmEntitySet, final List<UriParameter> keys) throws DataProviderException {
        final EdmEntityType entityType = edmEntitySet.getEntityType();
        final EntitySet entitySet = readAll(edmEntitySet);

        if (entitySet == null) {
            return null;
        }
        else {
            try {
                for (final Entity entity : entitySet.getEntities()) {
                    boolean found = true;
                    for (final UriParameter key : keys) {
                        final EdmProperty property = (EdmProperty) entityType.getProperty(key.getName());
                        final EdmPrimitiveType type = (EdmPrimitiveType) property.getType();

                        if (!type.valueToString(entity.getProperty(key.getName()).getValue(), property.isNullable(),
                                property.getMaxLength(), property.getPrecision(), property.getScale(), property.isUnicode())
                                .equals(key.getText())) {
                            found = false;
                            break;
                        }
                    }
                    if (found) {
                        return entity;
                    }
                }
                return null;
            }
            catch (final EdmPrimitiveTypeException e) {
                throw new DataProviderException("Wrong key!", e);
            }
        }
    }
}