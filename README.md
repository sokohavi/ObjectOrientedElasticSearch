# ObjectOrientedElasticSearch
An object oriented version of elastic search.

## What is it ?
The Object oriented elastic search is a a package that allow you to index\pull objects to\from elastic 
search easily. The idea behind this project is to eliminate the need to create and maintain an sdk for 
objects which you want to index\search for. This tool like most tool isn't a magic :), it's good for 
some scenarios and might not be good for others.

Let's look at a simple example:
Here is a simple and not very elegant class to describe a school. 

    public class School {
        private List<Student> studentsList;
        private String schoolId;
        private String address;
        private SchoolStaff manager;
    
        public School() {}
    
        public School(final List<Student> studentsList, final String schoolId,
                      final SchoolStaff manager, final String address) {
            this.studentsList = studentsList;
            this.schoolId = schoolId;
            this.manager = manager;
            this.address = address;
        }
    
        public List<Student> getStudentsList() {
            return this.studentsList;
        }
    
        public String getSchoolId() {
            return this.schoolId;
        }
    
        public String getAddress() {
            return this.address;
        }
    
        public SchoolStaff getManager() {
            return this.manager;
        }
    }
   
And here are some dependencies of School:
   
    public class Student extends Person {
        // TODO: add some attributes related to a student like exam scores.
    
        public Student() {}
    
        public Student(final String name, final String id) {
            super(id, name);
        }
    }

    public class SchoolStaff extends Person {
        private SchoolStaff supervisor;
    
        public SchoolStaff() {}
    
        public SchoolStaff(final String name, final String id, final SchoolStaff supervisor) {
            super(id, name);
            this.supervisor = supervisor;
        }
    
        public SchoolStaff getSupervisor() {
            return this.supervisor;
        }
    }
    
    public class Person {
        private String id;
        private String name;
    
        public Person() {}
    
        public Person(final String id, final String name) {
            this.id = id;
            this.name = name;
        }
    
        public String getId() {
            return this.id;
        }
    
        public String getName() {
            return this.name;
        }
    }
    
Now, let's assume we want to index school objects. 
1. In order to index school objects, we can go ahead and create some 'school' specific indexing logic. For example in 
this case you might want to define that 'School' is a nested object.
2. More than that, if we want to allow clients to search of school related data - 
for example "Which school has manager named 'Adam' ?", or "Which school has a student named 'Mark Twain' ?", or 
"which school is located in King County Seattle ?" 
Then you might need to create a specific sdk that accept a specific input (of a specific query type) and convert
the input to QDSL. An example for methods of such an sdk:
    
    
    public class Sdk {
        public String createDslQueryForManagerName(final String managerName) {
            ...
        }
    
        public String createDslQueryForStudentName(final String managerName) {
            ...
        }
    
        public String createDslQueryForStudentName(final String schoolAddress) {
            ...
        }
        
        // a lot of other specific logic.
    }

Creating this sdk is quite a lot of work. Now, to make the work needed for creating the sdk even more "fun", 
let's assume you are asked to have complex queries like:
1. "Schools which are located in Seattle or in Portland".
2. "Schools which has are located in Seattle and has a a student named 'James Brown'".

As you can see your sdk can get quite complex pretty fast.

## How is it different than using ES directly ?

The difference with this tool, is that here we don't try to construct a search dsl query using custom logic. 
Instead we use a simple conventions that allow us to convert any list object to a DSL query. 
The meaning of this is that we will reuse the object type, that we want to search for as a simple container
for our query. Let's look at a few preliminary examples:

1. "Which school has manager named 'Adam' ?"


    final SchoolStaff queryStudent = new SchoolStaff("Mark Twain", null, null);
    final School querySchool = new School(null, null, "Adam", null);
    
2. "Which school has a student named 'Mark Twain' ?"
    
    
    final Student queryStudent = new Student("Mark Twain", null);
    final List<Student> studentsList = Collections.singletonList(queryStudent);
    final School querySchool = new School(studentsList, null, null, null);
    
3. "Schools which has are located in Seattle and has a a student named 'James Brown'"
    
    
    final Student queryStudent = new Student("James Brown", null);
    final List<Student> studentsList = Collections.singletonList(queryStudent);
    final School querySchool = new School(studentsList, null, null, "Seattle");

    
As you can see we can express the kind of school that we want to search for, using the same object type as indexed 
object. 

Generally speaking, we can describe the search problem as a sub problem of the 'objects similarity' problem. 
I.e. given an objects space, OS, find a sub set of these objects, QS, that are the most similar to a given object, O 
(or a list of object, OL). 

Let's look at another example:
4. "Schools which are located in Seattle or in Portland".

    This is query actually contains two objects one is school that is located in Seattle and one that is 
    Located in Portland.
    
    
    final School querySchool1 = new School(null, null, null, "Seattle");
    final School querySchool2 = new School(studentsList, null, null, "Portland");
    final List<School> query = Arrays.asList(querySchool1, querySchool2);
        

Generally speaking, a single query object (querySchool represent an 'AND' query of it properties.
If we want to achieve an 'OR' query then we need to create a list of query objects.

### Best usage for this tool
This tool execute any DSL query that you provide it. Yet as described above it's most suitable for scenarios 
where:
You have many objects from different types which you want to index or\and many types of simple queries.
where Simple queries - Your search queries contains only the following elements: "AND", "OR", "Match".

Revisit the examples mentioned above, you can realize that despite these "limitations" this tool can cover quite 
a few scenarios and save you a lot of work. 


### Usage examples

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

For more info refer to the demo (part of this package)).