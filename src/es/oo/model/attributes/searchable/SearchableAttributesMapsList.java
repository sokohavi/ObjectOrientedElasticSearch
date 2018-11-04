package es.oo.model.attributes.searchable;

import es.oo.model.attributes.AttributesMap;
import es.oo.model.attributes.AttributesMapsList;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * This class represent a OR phrase of items Lists that can be converted to a QDSL query.
 * Each Item list will be converted to a AND phrase using {@link SearchableAttributesMap}.
 */
public class SearchableAttributesMapsList extends ArrayList<SearchableAttributesMap> {

    public SearchableAttributesMapsList(final List<List<Object>> objectsLists)
            throws IOException, IllegalAccessException {
        for (final List<Object> objectsList: objectsLists) {
            this.add(SearchableAttributesMap.toSearchableAttributesMap(objectsList));
        }
    }

    public SearchableAttributesMapsList(final AttributesMapsList attributesMapsList) {
        for (final AttributesMap attributesMap: attributesMapsList) {
            this.add(new SearchableAttributesMap(attributesMap.getNamespaceMap()));
        }
    }

    public String toDslQueryString() {
        final BoolQueryBuilder booleanQueryBuilder = new BoolQueryBuilder();

        for (final SearchableAttributesMap searchableAttributesMap: this) {
            final BoolQueryBuilder singleObjectQueryBuilder = searchableAttributesMap.toQueryBuilder();
            booleanQueryBuilder.should().add(singleObjectQueryBuilder);
        }

        final NestedQueryBuilder nestedQueryBuilder = new NestedQueryBuilder(AttributesMap.NAMESPACE_MAP,
                booleanQueryBuilder, ScoreMode.Max);

        return "{ \"query\": " + nestedQueryBuilder.toString() + " }";
    }
}