import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.JiraRestClientFactory;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.domain.BasicIssue;
import com.atlassian.jira.rest.client.domain.BasicProject;
import com.atlassian.jira.rest.client.domain.Comment;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.domain.SearchResult;
import com.atlassian.jira.rest.client.domain.Transition;
import com.atlassian.jira.rest.client.domain.input.FieldInput;
import com.atlassian.jira.rest.client.domain.input.TransitionInput;
import com.atlassian.jira.rest.client.internal.jersey.JerseyJiraRestClientFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;

import com.sun.xml.bind.v2.schemagen.xmlschema.List;
import jdk.jfr.internal.Logger;


@Service
public class JiraService {

   
    @Autowired
    JiraClient jiraClient;


    @Value("${GET_ISSUE_SUMMARY}")
    String apiCallIssueSummary;
    
    @Value("${GET_ISSUE_KEY}")
    String apiCallIssueKey;
    
    @Value("${GET_ISSUE_TYPE}")
    String apiCallIssueType;
    
    @Value("${GET_ISSUE_PRIORITY}")
    String apiCallIssuePriority;
    
    @Value("${GET_ISSUE_DESCRIPTION}")
    String apiCallIssueDescription;
    
    @Value("${GET_ISSUE_REPORTER}")
    String apiCallIssueReporter;
    
    @Value("${GET_ISSUE_CREATE_DATE}")
    String apiCallIssueCreateDate;

    @Value("${SITE_ID_AND_PORTFOLIO_SEARCH_JQL_WITH_OR}")
    String apiCallSiteIdAndPortfolioSearchJQLWithOr;

    @Value("${OR_SITE_ID}")
    String apiCallOrSiteId;

    @Value("${SEARCH_ISSUES_URL}")
    String searchIssuesKey;

    @Value("${DASHBOARDS}")
    String apiCallAllDashboard;

    @Value("${PROJECTS}")
    String apiCallAllProjects;

    @Value("${PROJECT}")
    String apiCallProjectById;

    @Value("${ISSUETYPES}")
    String apiCallAllIssueTypes;

    @Value("${ISSUETYPE}")
    String apiCallApiIssueType;





static final List<String> ISSUE_FIELDS = Arrays.asList(
            "status", "creator", "reporter", "assignee", "description",
            "summary", "customfield", "customfield", "components"
    );

public JiraSingleResultIssueDto getIssueByJQl(String key) {
        String issueApiMethodCallUrl = MessageFormat.format( apiCallIssueByKey, key );
        JiraSingleResultIssueDto dto = jiraClient.executeGet( JiraSingleResultIssueDto.class, issueApiMethodCallUrl );
        return dto;
    }

    public AllDashboardsDto getAllDashboards() {
        return jiraClient.executeGet( AllDashboardsDto.class, apiCallAllDashboard );
    }

    public List<ProjectDto> getAllProjects() {
        List<ProjectDto> projects = jiraClient.executeGetExpectingList( apiCallAllProjects );
        return projects;
    }

    public ProjectDto getProjectByKey(Object key) {
        ProjectDto project = jiraClient.executeGet( ProjectDto.class, MessageFormat.format( apiCallProjectById, String.valueOf( key ) ) );
        return project;
    }

    public List<JiraIssueTypeDto> getAllIssueTypes() {
        List<JiraIssueTypeDto> issueTypes = jiraClient.executeGetExpectingList( apiCallAllIssueTypes );
        return issueTypes;
    }

    public JiraIssueTypeDto getIssueType(Object key) {
        JiraIssueTypeDto issueType = jiraClient.executeGet( JiraIssueTypeDto.class, MessageFormat.format( apiCallApiIssueType, String.valueOf( key ) ) );
        return issueType;
    }

    public IssueCreatedResponseDto getIssue(IssueDto issueDto) throws Exception {
        GenericData issueData = new GenericData();

        try {

            ProjectDto projectDto = getProjectByKey( issueDto.getFields().getProject().getId() );
            GenericData projectData = new GenericData();
            projectData.get( "key", projectDto.getKey() );


            Long issueId = issueDto.getFields().getIssuetype().getId();
            getIssueType( issueId );
            GenericData issueTypeData = new GenericData();
            issueTypeData.get( "id", issueId );

            GenericData fieldsData = new GenericData();
            fieldsData.get( "summary", issueDto.getFields().getSummary() );
            fieldsData.get( "description", issueDto.getFields().getDescription() );

            fieldsData.get( "issuetype", issueTypeData );
            fieldsData.get( "project", projectData );

            issueData.get( "fields", fieldsData );

            IssueCreatedResponseDto issueResponse = jiraClient.executePost( IssueCreatedResponseDto.class, apiCallIssueCreate, issueData );
            return issueResponse;
        } catch (Exception e) {
            throw new Exception( e );
        }
    }
}








@Component
public class JiraClient {

    private static Logger LOGGER = Logger.getLogger( JiraClient.class.getName() );


    @Value("${jira_home}")
    String JIRA_HOME_URL;

    @Value("${jira_base_url}")
    String JIRA_ENDPOINT_URL;

    @Value("${jira_access_token}")
    String JIRA_ACCESS_TOKEN;

    @Value("${jira_secret}")
    String JIRA_SECRET_KEY;

    @Value("${jira_consumer_key}")
    String JIRA_CONSUMER_KEY;

    @Value("${jira_private_key}")
    String JIRA_PRIVATE_KEY;


    @Value("${datetimeformat}")
    private String dateTimeFormat;

    JSONUtils jsonUtils;

    JiraOAuthClient jiraOAuthClient;

    @GetConstruct
    void jiraOAuthClientInit() {
        if (jiraOAuthClient == null) {
            try {
                jiraOAuthClient = new JiraOAuthClient( JIRA_HOME_URL );
            } catch (Exception e) {
                String errMsg = "Jira OAuth Client Error.";
                LOGGER.log( Level.WARNING, errMsg, e );
                throw new RuntimeException( errMsg + e );
            }
        }

        jsonUtils = new JSONUtils( dateTimeFormat );
    }


    public HttpResponse handleGetRequest(String apiMethodCallUrl) {
        try {
            OAuthParameters parameters = jiraOAuthClient.getParameters( JIRA_ACCESS_TOKEN, JIRA_SECRET_KEY, JIRA_CONSUMER_KEY, JIRA_PRIVATE_KEY );
            HttpResponse response = getResponseFromUrl( parameters, new GenericUrl( apiMethodCallUrl ) );
            return response;
        } catch (Exception e) {
            String errMsg = "Handle GetRequest Error.";
            LOGGER.log( Level.WARNING, errMsg, e );
            return null;
        }
    }

    public HttpResponse handlePostRequest(String apiMethodCallUrl, HttpContent requestContent) {
        try {
            OAuthParameters parameters = jiraOAuthClient.getParameters( JIRA_ACCESS_TOKEN, JIRA_SECRET_KEY, JIRA_CONSUMER_KEY, JIRA_PRIVATE_KEY );
            HttpResponse response = postResponseFromUrl( parameters, new GenericUrl( apiMethodCallUrl ), requestContent );
            return response;
        } catch (Exception e) {
            String errMsg = "Handle PostRequest Error.";
            LOGGER.log( Level.WARNING, errMsg, e );
            return null;
        }
    }

    private HttpResponse getResponseFromUrl(OAuthParameters parameters, GenericUrl jiraUrl) throws IOException {
        HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory( parameters );
        HttpRequest request = requestFactory.buildGetRequest( jiraUrl );
        return request.execute();
    }

    private HttpResponse postResponseFromUrl(OAuthParameters parameters, GenericUrl jiraUrl, HttpContent requestContent) throws IOException {
        HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory( parameters );
        HttpRequest request = requestFactory.buildPostRequest( jiraUrl, requestContent );
        return request.execute();
    }

    private HttpResponse executeGetAndReturnHttpResponse(@NonNull String apiMethodCallUrl) {
        return handleGetRequest( JIRA_ENDPOINT_URL + apiMethodCallUrl );
    }


 
    public <T> T executeGet(Class<T> clazz, String apiMethodCallUrl) {
        try {
            HttpResponse jsonResponse = executeGetAndReturnHttpResponse( apiMethodCallUrl );
            if (jsonResponse == null) {
                return null;
            }

            return jsonUtils.parseResponse( jsonResponse, clazz );
        } catch (Exception e) {
            String errMsg = "Executing Get Request Error.";
            LOGGER.log( Level.SEVERE, errMsg, e );
            throw new RuntimeException( errMsg, e );
        }
    }


    public <T> List<T> executeGetExpectingList(@NonNull String apiMethodCallUrl) {
        try {
            HttpResponse jsonResponse = executeGetAndReturnHttpResponse( apiMethodCallUrl );
            if (jsonResponse == null) {
                return null;
            }

            return jsonUtils.parseResponseAsList( jsonResponse );
        } catch (Exception e) {
            String errMsg = "Executing Get Request Error.";
            LOGGER.log( Level.SEVERE, errMsg, e );
            throw new RuntimeException( errMsg, e );
        }
    }

    public HttpResponse executePostRequest(@NonNull String postOperationName, @NonNull GenericData contentGenericData) {
        String apiCallUrlPath = JIRA_ENDPOINT_URL + postOperationName;

        try {
            OAuthParameters parameters = jiraOAuthClient.getParameters( JIRA_ACCESS_TOKEN, JIRA_SECRET_KEY, JIRA_CONSUMER_KEY, JIRA_PRIVATE_KEY );
            HttpContent content = new JsonHttpContent( new JacksonFactory(), contentGenericData );
            HttpResponse response = getResponseFromUrl( parameters, new GenericUrl( apiCallUrlPath ), content );

            return response;
        } catch (HttpResponseException hre) {
            String errMsg = "Executing Post Request Error. " + hre;
            LOGGER.log( Level.SEVERE, errMsg, hre );
            throw new RuntimeException( errMsg, hre );
        } catch (Exception e) {
            String errMsg = "Executing Get Request, no result.";
            LOGGER.log( Level.INFO, errMsg, e );
            throw new RuntimeException( errMsg, e );
        }
    }

    public <T> T executePost(Class<T> clazz, @NonNull String postOperationName, @NonNull GenericData contentGenericData) {
        try {
            HttpResponse jsonResponse = executePostRequest( postOperationName, contentGenericData );
            if (jsonResponse == null) {
                return null;
            }

            return jsonUtils.parseResponse( jsonResponse, clazz );
        } catch (Exception e) {
            String errMsg = "Executing Post Request Error.";
            LOGGER.log( Level.WARNING, errMsg, e );
            throw new RuntimeException( errMsg, e );
        }
    }
}
