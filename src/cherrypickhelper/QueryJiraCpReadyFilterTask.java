/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cherrypickhelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javafx.concurrent.Task;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.stream.JsonParser;
import javax.net.ssl.HttpsURLConnection;

/**
 *
 * @author WLKS06
 */
public class QueryJiraCpReadyFilterTask extends Task<List<JiraInfo>> {

    private String mJiraFilter;
    private static Boolean DEBUG = false;

    QueryJiraCpReadyFilterTask(String id) {
        mJiraFilter = id;
    }

    @Override
    protected List<JiraInfo> call() throws Exception {
        List<JiraInfo> jiraList = new ArrayList<JiraInfo>();
        HttpURLConnection connection = JiraApi.JiraApiGetSearchUrlConnectionForFilter(mJiraFilter);
        int totalCrs = 0;
        int startAt = 0;
        int maxResults = 0;
        if (connection != null) {
            String baseSearchUrl = connection.getURL().toString();
            System.out.println("Base search URL is : "+baseSearchUrl);
            do {
                try (final JsonReader reader = Json.createReader(connection.getInputStream())) {
                    final JsonObject jsonObject = reader.readObject();

                    if (jsonObject.containsKey("startAt") && !jsonObject.isNull("startAt")) {
                        startAt = jsonObject.getInt("startAt");
                        System.out.println("CRs starting at: " + startAt);
                    }
                    if (jsonObject.containsKey("maxResults") && !jsonObject.isNull("maxResults")) {
                        maxResults = jsonObject.getInt("maxResults");
                        System.out.println("Max CRs returned: " + maxResults);
                    }
                    if (jsonObject.containsKey("total") && !jsonObject.isNull("total")) {
                        totalCrs = jsonObject.getInt("total");
                        System.out.println("Total CRs returned: " + totalCrs);
                    }
                    updateProgress(jiraList.size(), totalCrs);
                    JsonString errormessage = jsonObject.getJsonString("errorMessages");
                    if (errormessage != null) {
                        System.out.println("Received an error " + errormessage);
                    } else {
                        JsonArray issues = jsonObject.getJsonArray("issues");
                        for (int i = 0; i < issues.size(); ++i) {
                            //Parse out individual CRs
                            if(isCancelled()){
                                break;
                            }
                            JsonObject cr = issues.getJsonObject(i);
                            JiraInfo j = JiraInfo.createJiraInfoFromJsonObject(cr);
                            if (j != null) {
                                jiraList.add(j);
                            }
                        }
                    }
                    updateProgress(jiraList.size(), totalCrs);
                } catch (IOException e) {
                    System.out.print("Error retrieving data - possible invalid login?");
                }
            } while (!isCancelled() && (jiraList.size() < totalCrs) && 
                    (connection = JiraApi.JiraApiGetConnectionForSearchUrl(JiraApi.getAdditionalSearchResults(baseSearchUrl,  maxResults+startAt)))!= null);
            if(isCancelled()) {
                System.out.println("Task has been cancelled - returning");
            }
        }
        return jiraList;
    }

}
