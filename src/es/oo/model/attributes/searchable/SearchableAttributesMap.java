package es.oo.model.attributes.searchable;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.oo.model.attributes.AttributesMap;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This class represent a AND phrase of items that can be converted to a QDSL query.
 *
 * For more info see option 2 of
 * https://w.amazon.com/bin/view/FlexPlatform/Internal/AccountManagement/Documentation/ProviderSearchSystem/PAS-To-PSS/
 */
public class SearchableAttributesMap extends AttributesMap {
    public SearchableAttributesMap(final Map<String, Map<String, Object>> namespaceMap) {
        super(namespaceMap);
    }

    public static SearchableAttributesMap toSearchableAttributesMap(
            final List<Object> objectList) throws IOException, IllegalAccessException {
        final AttributesMap attributesMap = toAttributesMap(objectList);

        return new SearchableAttributesMap(attributesMap.getNamespaceMap());
    }

    public BoolQueryBuilder toQueryBuilder() {
        final BoolQueryBuilder queryBuilder = new BoolQueryBuilder();

        for (final String namespace: this.getNamespaceMap().keySet()) {
            final Map<String, Object> attributesMap = this.getNamespaceMap().get(namespace);

            final String fullNamespace = AttributesMap.NAMESPACE_MAP + "." + namespace;
            populateTerms(queryBuilder, fullNamespace, attributesMap);
        }

        return queryBuilder;
    }

    private void populateTerms(final BoolQueryBuilder queryBuilder,
                               final String namespace,
                               final Map<String, Object> attributesMap) {
        // Read this method carefully - god is in the details.
        // When we look at an attribute and we want to convert it to a dsl query, then we need to
        // interpret it properties a bit different then they are being interpreted by the AttributeMap object.
        for (final Map.Entry<String, Object> attributeEntry: attributesMap.entrySet()) {
            final String attributeName = attributeEntry.getKey();
            final String qdslPath = namespace + "." + attributeName;
            final Object attributeValue = attributeEntry.getValue();

            // This is possible and it's a result of a recursive call.
            if (attributeValue == null) {
                return;
            }

            // AttributeMap might have map values.
            // An example: an object that contains a map as one of it properties.
            // let's assume object X contains property Y which is a map.
            // Let's assume for a certain instance of X, Y contains the key "someKey".
            // Then we would like to create a query which is x.y.somekey = value.
            // The following achieves that.
            if (attributeValue instanceof Map) {
                final Map<String, Object> mapValue = (Map<String, Object>) attributeValue;
                populateTerms(queryBuilder, qdslPath, mapValue);
                continue;
            }

            // If a property of an item is a collection then AttributeMap won't try to disassemble it.
            // The reason for that is so we save an item to ES with multiple values.
            // On the other hand we would like to to disassemble the collection here because we need to convert the
            // collection object to an (OR) query somehow.
            if (attributeValue instanceof Collection) {
                final Collection collectionValue = (Collection) attributeValue;
                final BoolQueryBuilder collectionQueryBuilder = new BoolQueryBuilder();

                for (final Object object: collectionValue) {
                    final ObjectMapper mapper = new ObjectMapper();
                    final Map<String, Object> objectProperties = mapper.convertValue(object, Map.class);
                    final BoolQueryBuilder objectInCollectionQueryBuilder = new BoolQueryBuilder();
                    populateTerms(objectInCollectionQueryBuilder, qdslPath, objectProperties);
                    collectionQueryBuilder.should(objectInCollectionQueryBuilder);
                }

                queryBuilder.must().add(collectionQueryBuilder);

                continue;
            }

            final MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder(qdslPath, attributeValue);

            queryBuilder.must().add(matchQueryBuilder);
        }
    }
}