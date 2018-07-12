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
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;

public class JiraApi {
    //See reference https://docs.atlassian.com/jira/REST/cloud/#d2e308
    //Get issue - GET /rest/api/2/issue/{issueIdOrKey}
    //Get filter - GET /rest/api/2/filter/{id}
    // ->this returns a searchUrl which is then used
    //Example - http://jira.mot.com/rest/api/latest/issue/IKSWM-1117.json (regular attachment example)
    //http://jira.mot.com/rest/api/latest/issue/IKSWM-6899.json - bug2go link in custom field 
    //http://jira.mot.com/rest/api/latest/issue/IKUT-745325.json - bug2go link in description
    //https://jira.mot.com/rest/api/latest/issue/IKSWM-57466 - get issue
    //https://jira.mot.com/rest/api/latest/issue/IKSWM-57466 - post comment
    private static final String JIRA_ID_SAMPLE = "IKSWM-57466";
    private static final String JIRA_ISSUE_URL = "http://jira.mot.com/rest/api/latest/issue/";
    private static final String JIRA_FILTER_URL = "http://jira.mot.com/rest/api/latest/filter/";
    private static final String JIRA_SEARCH_GETMORE = "&startAt=";
    
    public static final String BUG2GO_REPORT_URL_MARKER = "b2gadm-mcloud101-blur.svcmot.com/bugreport";
    
    public static String getAdditionalSearchResults(String baseUrl, int startAt) {
        return baseUrl+JIRA_SEARCH_GETMORE+Integer.toString(startAt);
    }
    static Boolean JiraApiValidateConnection() {
        URL jiraUrl = null;
        HttpURLConnection jiraConnection = null;
        try {
            //From http://info.michael-simons.eu/2014/10/22/getting-started-with-javafx-8-developing-a-rest-client-application-from-scratch/
            //and 
            jiraUrl = new URL(JIRA_ISSUE_URL + JIRA_ID_SAMPLE + ".json");
            jiraConnection = (HttpURLConnection) jiraUrl.openConnection();
            jiraConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            jiraConnection.setRequestProperty("Authorization", String.format("Basic %s", Base64.getEncoder().encodeToString(String.format("%s:%s", CherrypickHelper.LoginCredentials.GetUser(CherrypickHelper.LoginCredentials.LOGIN_TYPE.ONEIT), CherrypickHelper.LoginCredentials.GetPassword(CherrypickHelper.LoginCredentials.LOGIN_TYPE.ONEIT)).getBytes())));
            jiraConnection.setDoInput(true);
            jiraConnection.setDoOutput(true);
            if (jiraConnection != null) {
                jiraConnection.getInputStream();
                System.out.println("Authentication Successful");
            }
        } catch (IOException e) {
            if (e.getMessage().contains("401")) {
                System.out.print("Authentication failure - try again");
                jiraConnection = null;
                CherrypickHelper.LoginCredentials.ClearCredentials(CherrypickHelper.LoginCredentials.LOGIN_TYPE.ONEIT);
            } else if (e.getMessage().contains("403")) {
                System.out.print("Authentication Forbidden - blocked");
                jiraConnection = null;
                CherrypickHelper.LoginCredentials.ClearCredentials(CherrypickHelper.LoginCredentials.LOGIN_TYPE.ONEIT);
            }
        }
        return (jiraConnection != null) ? true : false;
    }
    static HttpURLConnection JiraApiEstablishGetIssueConnection(String jiraId) {
        URL jiraUrl = null;
        HttpURLConnection jiraConnection = null;
        try {
            //From http://info.michael-simons.eu/2014/10/22/getting-started-with-javafx-8-developing-a-rest-client-application-from-scratch/
            //and 
            jiraUrl = new URL(JIRA_ISSUE_URL + jiraId + ".json");
            jiraConnection = (HttpURLConnection) jiraUrl.openConnection();
            jiraConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            jiraConnection.setRequestProperty("Authorization", String.format("Basic %s", Base64.getEncoder().encodeToString(String.format("%s:%s", CherrypickHelper.LoginCredentials.GetUser(CherrypickHelper.LoginCredentials.LOGIN_TYPE.ONEIT), CherrypickHelper.LoginCredentials.GetPassword(CherrypickHelper.LoginCredentials.LOGIN_TYPE.ONEIT)).getBytes())));
            jiraConnection.setDoInput(true);
            jiraConnection.setDoOutput(true);
            if (jiraConnection != null) {
                jiraConnection.getInputStream();
                System.out.println("Authentication Successful");
            }
        } catch (IOException e) {
            if (e.getMessage().contains("401")) {
                System.out.print("Authentication failure - try again");
                jiraConnection = null;
            } else if (e.getMessage().contains("403")) {
                System.out.print("Authentication Forbidden - blocked");
                jiraConnection = null;
            }
        }
        return jiraConnection;
    }

    static HttpURLConnection JiraApiEstablishDownloadAttachmentConnection(String urlstring) {
        URL jiraUrl = null;
        HttpURLConnection jiraConnection = null;
        try {
            jiraUrl = new URL(urlstring);
            jiraConnection = (HttpURLConnection) jiraUrl.openConnection();
            jiraConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            jiraConnection.setRequestProperty("Authorization", String.format("Basic %s", Base64.getEncoder().encodeToString(String.format("%s:%s", CherrypickHelper.LoginCredentials.GetUser(CherrypickHelper.LoginCredentials.LOGIN_TYPE.ONEIT), CherrypickHelper.LoginCredentials.GetPassword(CherrypickHelper.LoginCredentials.LOGIN_TYPE.ONEIT)).getBytes())));
            jiraConnection.setDoInput(true);
            jiraConnection.setDoOutput(true);
        } catch (IOException e) {
            if (e.getMessage().contains("401")) {
                System.out.print("Authentication failure - try again");
                jiraConnection = null;
            } else if (e.getMessage().contains("403")) {
                System.out.print("Authentication Forbidden - blocked");
                jiraConnection = null;
            }
        }
        return jiraConnection;
    }
    
    //Sample jira issue https://jira.mot.com/rest/api/latest/issue/2872764
    //Sample jira filter https://jira.mot.com/rest/api/latest/filter/129708
    //   -->This includes embedded searchURL 
    //   https://jira.mot.com/rest/api/latest/search?jql=labels+in+(pmst3_cp_approved)+AND+labels+not+in+(pmst3_cp_done,+pmst3_cp_other_means)
    //
    //   stable3 searchURL - 
    //   https://jira.mot.com/rest/api/latest/search?jql=(resolved+is+EMPTY+OR+resolved+%3E+%222016/03/20%22)+AND+labels+in+(m_cherrypick_request)+AND+labels+not+in+(pmst3_cp_approved,+pmst3_cp_done,+pmst3_cp_rejected,+pmst3_cp_other_means)+AND+(status+%3D+Closed+OR+status+%3D+Ready)+ORDER+BY+cf%5B11510%5D+ASC,+status+ASC,+cf%5B10052%5D+DESC,+resolution+ASC
    static HttpURLConnection JiraApiGetSearchUrlConnectionForFilter(String jiraFilterId) {
        URL jiraUrl = null;
        HttpURLConnection filterConnection = null;
        try {
            jiraUrl = new URL(JIRA_FILTER_URL + jiraFilterId + ".json");
            filterConnection = (HttpURLConnection) jiraUrl.openConnection();
            if (filterConnection != null) {
                filterConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                filterConnection.setRequestProperty("Authorization", String.format("Basic %s", Base64.getEncoder().encodeToString(String.format("%s:%s", CherrypickHelper.LoginCredentials.GetUser(CherrypickHelper.LoginCredentials.LOGIN_TYPE.ONEIT), CherrypickHelper.LoginCredentials.GetPassword(CherrypickHelper.LoginCredentials.LOGIN_TYPE.ONEIT)).getBytes())));
                filterConnection.setDoInput(true);
                filterConnection.setDoOutput(true);
                filterConnection.getInputStream();
                System.out.println("Authentication Successful");
            }
        } catch (IOException e) {
            if (e.getMessage().contains("401") || e.getMessage().contains("403")) {
                System.out.print("Authentication failure - try again");
                filterConnection = null;
            } else if (e.getMessage().contains("403")) {
                System.out.print("Authentication Forbidden - blocked");
                filterConnection = null;
            }
        }
        if (filterConnection != null) {
            //Have initial filter result - which includes the embedded searchUrl which is the actual URL 
            //we want to return
            try (final JsonReader reader = Json.createReader(filterConnection.getInputStream())) {
                final JsonObject jsonObject = reader.readObject();
                JsonString errormessage = jsonObject.getJsonString("errorMessages");
                if (errormessage != null) {
                    System.out.println("Found it");
                } else {
                    JsonString searchURL = jsonObject.getJsonString("searchUrl");
                    if (searchURL != null) {
                        try {
                            jiraUrl = new URL(searchURL.getString());
                            filterConnection = (HttpURLConnection) jiraUrl.openConnection();
                            if (filterConnection != null) {
                                filterConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                                filterConnection.setRequestProperty("Authorization", String.format("Basic %s", Base64.getEncoder().encodeToString(String.format("%s:%s", CherrypickHelper.LoginCredentials.GetUser(CherrypickHelper.LoginCredentials.LOGIN_TYPE.ONEIT), CherrypickHelper.LoginCredentials.GetPassword(CherrypickHelper.LoginCredentials.LOGIN_TYPE.ONEIT)).getBytes())));
                                filterConnection.setDoInput(true);
                                filterConnection.setDoOutput(true);
                                filterConnection.getInputStream();
                                System.out.println("Authentication Successful");
                            }
                        } catch (IOException e) {
                            if (e.getMessage().contains("401") || e.getMessage().contains("403")) {
                                System.out.print("Authentication failure - try again");
                                filterConnection = null;
                            } else if (e.getMessage().contains("403")) {
                                System.out.print("Authentication Forbidden - blocked");
                                filterConnection = null;
                            }
                        }
                    } else {
                        System.out.print("Error finding embedded search URL");
                    }
                }
            } catch (IOException e) {
                System.out.print("Error retrieving data from filer URL");
            }
        }
        return filterConnection;
    }
    static HttpURLConnection JiraApiGetConnectionForSearchUrl(String searchURL) {
        URL jiraUrl = null;
        HttpURLConnection filterConnection = null;
        try {
            jiraUrl = new URL(searchURL);
            filterConnection = (HttpURLConnection) jiraUrl.openConnection();
            if (filterConnection != null) {
                filterConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                filterConnection.setRequestProperty("Authorization", String.format("Basic %s", Base64.getEncoder().encodeToString(String.format("%s:%s", CherrypickHelper.LoginCredentials.GetUser(CherrypickHelper.LoginCredentials.LOGIN_TYPE.ONEIT), CherrypickHelper.LoginCredentials.GetPassword(CherrypickHelper.LoginCredentials.LOGIN_TYPE.ONEIT)).getBytes())));
                filterConnection.setDoInput(true);
                filterConnection.setDoOutput(true);
                filterConnection.getInputStream();
                System.out.println("Authentication Successful");
            }
        } catch (IOException e) {
            if (e.getMessage().contains("401")) {
                System.out.print("Authentication failure - try again");
                filterConnection = null;
            } else if (e.getMessage().contains("403")) {
                System.out.print("Authentication Forbidden - blocked");
                filterConnection = null;
            }
        }
    return filterConnection ;
}
    
    static HttpURLConnection JiraApiEstablishUpdateCommentConnection(String jiraId, String comment) {
        URL jiraUrl = null;
        HttpURLConnection jiraConnection = null;
        try {
            //From http://info.michael-simons.eu/2014/10/22/getting-started-with-javafx-8-developing-a-rest-client-application-from-scratch/
            //and 
            jiraUrl = new URL(JIRA_ISSUE_URL + jiraId + "/comment");
            jiraConnection = (HttpURLConnection) jiraUrl.openConnection();
            if (jiraConnection != null) {
                jiraConnection.setRequestMethod("POST");
                jiraConnection.setRequestProperty("Accept", "*/*"); //Needed????
                jiraConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                jiraConnection.setRequestProperty("Authorization", String.format("Basic %s", Base64.getEncoder().encodeToString(String.format("%s:%s", CherrypickHelper.LoginCredentials.GetUser(CherrypickHelper.LoginCredentials.LOGIN_TYPE.ONEIT), CherrypickHelper.LoginCredentials.GetPassword(CherrypickHelper.LoginCredentials.LOGIN_TYPE.ONEIT)).getBytes())));
                jiraConnection.setDoInput(true);
                jiraConnection.setDoOutput(true);
                jiraConnection.connect();

                String jsonComment = "{\n\"body\":\"" + comment + "\"\n}";
                OutputStreamWriter wr = new OutputStreamWriter(jiraConnection.getOutputStream());
                wr.write(jsonComment);
                wr.flush();
                wr.close();

                Reader in = new BufferedReader(new InputStreamReader(jiraConnection.getInputStream()));
                for (int c; (c = in.read()) >= 0; System.out.print((char) c));
                System.out.println("Added Comment");
            }
        } catch (IOException e) {
            if (e.getMessage().contains("401")) {
                System.out.print("Authentication failure - try again");
                jiraConnection = null;
            } else if (e.getMessage().contains("403")) {
                System.out.print("Authentication Forbidden - blocked");
                jiraConnection = null;
            } else {
                System.out.print(e);
                try {
                    InputStream errorStream = jiraConnection.getErrorStream();
                    if (errorStream != null) {
                        Reader in = new BufferedReader(new InputStreamReader(errorStream));
                        for (int c; (c = in.read()) >= 0; System.out.print((char) c));
                    }
                } catch (IOException k) {
                    System.out.println(k);
                }
            }
        }
        return jiraConnection;
    }
    
    //Upgdate labels using Edit.  Editmeta advises which fields can be changed. https://developer.atlassian.com/jiradev/jira-apis/jira-rest-apis/jira-rest-api-tutorials/updating-an-issue-via-the-jira-rest-apis
    //This Works:
    //curl -u 'username:password' -H "Content-Type: application/json" -X PUT --data '{ "update": { "labels": [ {"add": "newlabel2"} ] } }' http://jira.mot.com/rest/api/latest/issue/IKSWM-57466
    static HttpURLConnection JiraApiEstablishUpdateLabelConnection(String jiraId, String label) {
        URL jiraUrl = null;
        HttpURLConnection jiraConnection = null;
        try {
            //From http://info.michael-simons.eu/2014/10/22/getting-started-with-javafx-8-developing-a-rest-client-application-from-scratch/
            //and 
            jiraUrl = new URL(JIRA_ISSUE_URL + jiraId);
            jiraConnection = (HttpURLConnection) jiraUrl.openConnection();
            if (jiraConnection != null) {
                jiraConnection.setRequestMethod("PUT");
                jiraConnection.setRequestProperty("Accept", "*/*"); //Needed????
                jiraConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                jiraConnection.setRequestProperty("Authorization", String.format("Basic %s", Base64.getEncoder().encodeToString(String.format("%s:%s", CherrypickHelper.LoginCredentials.GetUser(CherrypickHelper.LoginCredentials.LOGIN_TYPE.ONEIT), CherrypickHelper.LoginCredentials.GetPassword(CherrypickHelper.LoginCredentials.LOGIN_TYPE.ONEIT)).getBytes())));
                jiraConnection.setDoInput(true);
                jiraConnection.setDoOutput(true);
                jiraConnection.connect();

                //{ "update": { "labels": [ {"add": "newlabel"} ] } }
                String jsonLabel = "{ \"update\": { \"labels\": [ {\"add\": \"" + label + "\"} ] } }";
                OutputStreamWriter wr = new OutputStreamWriter(jiraConnection.getOutputStream());
                wr.write(jsonLabel);
                wr.flush();
                wr.close();
                System.out.println("Wrote Label: " + jsonLabel + " TO: " + jiraUrl + "\n");

                Reader in = new BufferedReader(new InputStreamReader(jiraConnection.getInputStream()));
                for (int c; (c = in.read()) >= 0; System.out.print((char) c));
            }
        } catch (IOException e) {
            if (e.getMessage().contains("401")) {
                System.out.print("Authentication failure - try again");
            } else if (e.getMessage().contains("403")) {
                System.out.print("Authentication Forbidden - blocked");
            } else {
                System.out.print(e);
                try {
                    InputStream errorStream = jiraConnection.getErrorStream();
                    if (errorStream != null) {
                        Reader in = new BufferedReader(new InputStreamReader(errorStream));
                        for (int c; (c = in.read()) >= 0; System.out.print((char) c));
                    }
                } catch (IOException k) {
                    System.out.println(k);
                }
            }
            jiraConnection = null;
        }
        return jiraConnection;
    }
}
