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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.net.ssl.HttpsURLConnection;

/**
 *
 * @author WLKS06
 */
public class GerritInfo {

    public int commit_id;
    public String commit_info;
    public String branch;
    public String repo;
    public int lines_added;
    public int lines_removed;
    public static String launchURL = "https://gerrit.mot.com/#/c/";

    GerritInfo() {

    }
    
    public static Comparator<GerritInfo> BranchComparator = new Comparator<GerritInfo>() {

        @Override
        public int compare(GerritInfo o1, GerritInfo o2) {
            return(o1.branch.compareTo(o2.branch));
        }
        
    };
    
    public static Comparator<GerritInfo> InfoComparator = new Comparator<GerritInfo>() {

        @Override
        public int compare(GerritInfo o1, GerritInfo o2) {
            return(o1.commit_info.compareTo(o2.commit_info));
        }
        
    };
    
    public static Comparator<GerritInfo> RepoComparator = new Comparator<GerritInfo>() {

        @Override
        public int compare(GerritInfo o1, GerritInfo o2) {
            return(o1.repo.compareTo(o2.repo));
        }
        
    };
    
    @Override
    public String toString() {
        String out = commit_id + " : "+commit_info+"\n"+repo+"\n"+branch+" - Added: "+lines_added+" Removed: "+lines_removed;
       
        return out;
    }
    
    public static GerritInfo createGerritInfoFromJsonObject(JsonObject o) {
        GerritInfo g = new GerritInfo();
        try {
            if (o.containsKey("_number")) {
                g.commit_id = o.getInt("_number");
            }
            if (o.containsKey("subject")) {
                g.commit_info = o.getString("subject");
            }
            if (o.containsKey("branch")) {
                g.branch = o.getString("branch");
            }
            if (o.containsKey("project")) {
                g.repo = o.getString("project");
            }
            if (o.containsKey("insertions")) {
                g.lines_added = o.getInt("insertions");
            }
            if (o.containsKey("deletions")) {
                g.lines_removed = o.getInt("deletions");
            }
        } catch (Exception e) {
            System.out.println("Error creating gerrit info from JsonObject:" + e);
            g = null;
        }
        return g;
    }

    public static List<GerritInfo> obtainGerritList(String jiraId) {
        List<GerritInfo> out = new ArrayList();
        int fail_count = 0;
        Boolean retry = false;
        HttpsURLConnection gerritconnection;
        do {
            retry = false;
            //System.out.println("Trying to establish connection to search for: "+jiraId);
            gerritconnection = GerritApi.GerritApiEstablishSearchChangeConnection(jiraId);
            if (gerritconnection != null) {
                try {
                    InputStream is = gerritconnection.getInputStream();
                    if (false) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(is));
                        String inputLine;
                        System.out.println("Printing reply from Gerrit request");
                        while ((inputLine = in.readLine()) != null) {
                            System.out.println(inputLine);
                        }
                        in.close();
                    }

                    //For some reason, gerrit server returns a line at the top that needs
                    //to be thrown out in order to get to the JSON structure
                    //OK, then....
                    BufferedReader in = new BufferedReader(new InputStreamReader(is));
                    String firstLine = in.readLine();
                    //if (firstLine.contains(")]}")) {
                    //    System.out.println("Ignoring first line returned from server:" + firstLine);
                    //}
                    try (final JsonReader gerritReader = Json.createReader(in)) {
                        final JsonArray gerritArray = gerritReader.readArray();
                        if (gerritArray != null) {
                            Iterator i = gerritArray.iterator();
                            while (i.hasNext()) {
                                JsonObject o = (JsonObject) i.next();
                                GerritInfo g = GerritInfo.createGerritInfoFromJsonObject(o);
                                if (g != null) {
                                    //System.out.println("Found Commit: "+g.commit_id);
                                    out.add(g);
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Error getting Gerrit info" + e);
                    if(++fail_count <= 1) {
                        System.out.println("Retrying Gerrit search - as we received an exception");
                        retry = true;
                    }
                    else {
                        out = null;
                    }
                }
            }
        } while(retry);
        return out;
    }

    public static GerritInfo obtainGerritDetails(String commitId) {
        GerritInfo g = null;
        HttpsURLConnection gerritconnection = GerritApi.GerritApiEstablishGetChangeConnection(commitId);
        if (gerritconnection != null) {
            try {
                InputStream is = gerritconnection.getInputStream();
                if (false) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(is));
                    String inputLine;
                    System.out.println("Printing reply from Gerrit request");
                    while ((inputLine = in.readLine()) != null) {
                        System.out.println(inputLine);
                    }
                    in.close();
                }

                //For some reason, gerrit server returns a line at the top that needs
                //to be thrown out in order to get to the JSON structure
                //OK, then....
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                String firstLine = in.readLine();
                //if (firstLine.contains(")]}")) {
                //    System.out.println("Ignoring first line returned from server:" + firstLine);
                //}
                try (final JsonReader gerritReader = Json.createReader(in)) {
                    final JsonObject gerritObject = gerritReader.readObject();
                    if (gerritObject != null) {
                        g = GerritInfo.createGerritInfoFromJsonObject(gerritObject);
                        if (true) {
                            System.out.println("Created new GerritInfo:" + g.commit_id);
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Error getting Gerrit info"+e);
            }
        }
        return g;
    }
}
