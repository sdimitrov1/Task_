import java.net.URI;
import java.util.Optional;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.atlassian.util.concurrent.Promise;

public class JRC
{
    public Issue getIssue(String issueKey) throws Exception
    {
        final URI jiraServerUri = new URI("https://jira.atlassian.com/browse/SRCTREEWIN-13670?jql=issuetype%20in%20(Bug%2C%20Documentation%2C%20Enhancement)%20and%20updated%20%3E%20startOfWeek()");
        final JiraRestClient restClient = new AsynchronousJiraRestClientFactory().createWithBasicHttpAuthentication(jiraServerUri, "moni0971@gmail.com", "vXqfHo4BvfC97N4cyqrV9E48");
        Promise issuePromise = restClient.getIssueClient().getIssue(issueKey);
        return Optional.ofNullable((Issue) issuePromise.claim()).orElseThrow(() -> new Exception("No such issue"));
    }

}
