package es.oo.demo;

import com.google.common.collect.Lists;
import es.oo.endpoint.ElasticSearchProxy;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

import java.util.List;
import java.util.UUID;

public class SimpleWriteSearchApp {

    public static void main(final String[] args) throws Exception {
        final Student student = new Student("April Shterling", "123");
        final SchoolStaff manager = new SchoolStaff("Tal", "456", null);
        final String addressString = "somewhere over the rainbow";
        final School school = new School(Lists.newArrayList(student),
                UUID.randomUUID().toString(), manager, addressString);

        final Student student2 = new Student("Michael Jackson", "987");
        final SchoolStaff manager2 = new SchoolStaff("Prince", "098", null);
        final String addressString2 = "las vegas";
        final School school2 = new School(Lists.newArrayList(student2),
                UUID.randomUUID().toString(), manager2, addressString2);

        // use https://www.elastic.co/guide/en/elasticsearch/reference/current/windows.html
        // to install ES on your local machine.
        final String esEndpointUrl = "http://localhost:9200";
        final RestHighLevelClient highLevelClient =
                new RestHighLevelClient(RestClient.builder(HttpHost.create(esEndpointUrl)));
        final ElasticSearchProxy elasticSearchProxy = new ElasticSearchProxy(highLevelClient);

        Thread.sleep(10);

        elasticSearchProxy.writeItem(school.getSchoolId(), school);
        elasticSearchProxy.writeItem(school2.getSchoolId(), school2);

        // Sleep a little bit - ES is eventual consistency.
        Thread.sleep(1000);

        // Search for bogus school - no results.
        final List<School> searchResult1 = elasticSearchProxy.searchForSingleObject(
                new School(null, "129999", null, "hmmm..."), School.class);
        assert (searchResult1.size() == 0);

        // Search for school with Students - exact match
        final List<School> searchResult2 = elasticSearchProxy.searchForSingleObject(school, School.class);
        assert (searchResult2.size() == 1);

        // Search for school with Students - partial match
        final School schoolQ = new School(Lists.newArrayList(student), null, null, null);
        final List<School> searchResult3 = elasticSearchProxy.searchForSingleObject(schoolQ, School.class);
        assert (searchResult3.size() > 1);

        // Search for school with Students - partial match
        final School school3 = new School(null, null, null, addressString);
        final List<School> searchResult4 = elasticSearchProxy.searchForSingleObject(school3, School.class);
        assert (searchResult4.size() > 1);

        // Search for both schools at once (or query)
        final List<School> searchQueryList5 = Lists.newArrayList(school, school2);
        final List<School> searchResult5 =
                elasticSearchProxy.searchForMultipleObjects(searchQueryList5, School.class);
        assert (searchResult5.size() == 2);

        // Search for two schools at once (or query) - one is real other is bogus.
        final School school4 = new School(null, "123", null, "Bogus address");
        final List<School> searchQueryList6 = Lists.newArrayList(school2, school4);
        final List<School> searchResult6 =
                elasticSearchProxy.searchForMultipleObjects(searchQueryList6, School.class);
        assert (searchResult6.size() == 1);

        // Search for school with the student as a manager
        final SchoolStaff staff = new SchoolStaff(student.getName(), student.getId(), null);
        final School schoolQ2 = new School(null, null, staff, null);
        final List<School> searchResult7 = elasticSearchProxy.searchForSingleObject(schoolQ2, School.class);
        assert (searchResult7.size() == 0);
    }


}
