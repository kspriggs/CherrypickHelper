/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cherrypickhelper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.json.JsonArray;
import javax.json.JsonObject;


/**
 *
 * @author wlks06
 */
public class JiraInfo {

    static enum JiraAttachmentType {
        URL, BUG2GO
    };

    public String id;
    public String summary;
    public String description;
    public String status;
    public String resolution;
    public String changes_merged;
    public String dependencies;
    public String[] commits;
    public JsonArray comments;
    public List<String> labels;
    public List<GerritInfo> gerritList;
    public static String launchURL = "https://idart.mot.com/browse/";

    private Boolean DEBUG = false;

    JiraInfo() {
        id = "?";
        summary = "?";
        description = "?";
        status = "?";
        resolution = "?";
        changes_merged = "?";
        dependencies = "";
        gerritList = new ArrayList();
        labels = new ArrayList();
        commits = null;
    }

    @Override
    public String toString() {
        String out = id+" : "+summary+"\n"+status+" "+resolution+" - ";
        if (gerritList!= null && !gerritList.isEmpty()) {
            for (GerritInfo gerrit : gerritList) {
                out += gerrit.branch + " ";
            }
        }
        if(!dependencies.isEmpty()) {
            out += "\nDependent CRs: "+dependencies;
        }
        if(!labels.isEmpty()) {
            out += "\nLabels: ";
            Boolean first = true;
            for(String k : labels) {
                if(!first) {
                    out += ", ";
                }
                else {
                    first = false;
                }
                out += k;
            }
        }
        return out; //To change body of generated methods, choose Tools | Templates.
    }

    public Boolean isReady() {
        return status.equals("Closed") && !gerritList.isEmpty();
    }
    public static JiraInfo createJiraInfoFromJsonObject(JsonObject o) {
        JiraInfo j = new JiraInfo();
        Boolean ignoreCrDueToLackOfContent = true;
        try {
            if (o.containsKey("key")) {
                j.id = o.getString("key");
            }
            //Look into fields for all other data
            JsonObject fields = o.getJsonObject("fields");
            if (fields.containsKey("description")&& !fields.isNull("description")) {
                j.description = fields.getString("description");
            }
            if(fields.containsKey("summary")&& !fields.isNull("summary")) {
                j.summary = fields.getString("summary");
                ignoreCrDueToLackOfContent = false;
            }
            if(fields.containsKey("labels")&& !fields.isNull("labels")) {
                JsonArray labelsArray = fields.getJsonArray("labels");
                if(labelsArray != null) {
                    Iterator i = labelsArray.iterator();
                    while(i.hasNext()) {
                        j.labels.add(i.next().toString().replaceAll("\"", ""));
                    }
                }
            }
            //Comment not returned - will need to add something to query further
            //TODO - add code to query CR directly again if comments needed
            if (fields.containsKey("comment")&& !fields.isNull("comment")) {
                JsonObject jiraComments = fields.getJsonObject("comment");
                if (jiraComments != null) {
                    if (jiraComments.containsKey("comments")) {
                        j.comments = jiraComments.getJsonArray("comments");
                    }
                }
            }
            //Check for gerrit commits
            //customfield_11510 - changes merged?
            //customfield_10020 - commits?
            //resolution - resolution?
            //status - look for name
            //customfield_10127 - dependencies
            if(fields.containsKey("status")&& !fields.isNull("status")) {
                JsonObject res = fields.getJsonObject("status");
                if(res != null) {
                    j.status = res.getString("name");
                }
            }  
            if(fields.containsKey("resolution") && !fields.isNull("resolution")) {
                JsonObject res = fields.getJsonObject("resolution");
                if(res != null) {
                    j.resolution = res.getString("name");
                }
            }
 
            if(fields.containsKey("customfield_11510") && !fields.isNull("customfield_11510")) {
                j.changes_merged = fields.getString("customfield_11510");
            }
            /*
            if(fields.containsKey("customfield_10020")) {
                String commits = fields.getString("customfield_10020");
                j.commits = commits.split(", ");
                for(String commitId : j.commits) {
                    GerritInfo g = GerritInfo.obtainGerritDetails(commitId);
                    if(g != null) {
                        j.gerritList.add(g);
                    }
                }
            }
            */
            j.gerritList = GerritInfo.obtainGerritList(j.id);
            
            if(fields.containsKey("customfield_10127")& !fields.isNull("customfield_10127")) {
                j.dependencies = fields.getString("customfield_10127");
            }         
         
        } catch (Exception e) {
            System.out.println("Error creating JiraInfo" + e);
            if(ignoreCrDueToLackOfContent) {
                j = null;
            }
        }
        return j;
    }

}
