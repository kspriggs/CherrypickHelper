/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cherrypickhelper;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import javax.net.ssl.HttpsURLConnection;

/**
 *
 * Try the following for gerrit access: http://gerrit.mot.com/a/changes/886264
 * -> returns specific details of a change
 * http://gerrit.mot.com/a/changes/?q=IKSWM-55397
 */
public class GerritApi {

    private static final String GERRIT_CHANGE_URL = "https://gerrit.mot.com/a/changes/";
    private static final String GERRIT_SEARCH_URL = "https://gerrit.mot.com/a/changes/?q=";

    static Boolean GerritApiValidateConnection() {
        Boolean retval = true;    
        URL gerritUrl = null;
        HttpsURLConnection gerritConnection = null;
        try {
            gerritUrl = new URL(GERRIT_SEARCH_URL);
            gerritConnection = (HttpsURLConnection) gerritUrl.openConnection();
            HttpDigestAuth gerritAuth = new HttpDigestAuth();
            HttpsURLConnection gerritAuthConnection = gerritAuth.tryAuth(gerritConnection, CherrypickHelper.LoginCredentials.GetUser(CherrypickHelper.LoginCredentials.LOGIN_TYPE.HTTPDIGEST), CherrypickHelper.LoginCredentials.GetPassword(CherrypickHelper.LoginCredentials.LOGIN_TYPE.HTTPDIGEST));
            if (gerritAuthConnection != null) {
                gerritAuthConnection.getInputStream();
                gerritConnection = gerritAuthConnection;
            }
        } catch (IOException e) {
            if (e.getMessage().contains("401")) {
                System.out.print("Authentication failure - try again");
                retval = false;
                gerritConnection = null;
            } else if (e.getMessage().contains("403")) {
                System.out.print("Authentication Forbidden - blocked");
                retval = false;
                gerritConnection = null;
            }
        }
        return retval;
    }
    //Search for all commits for a given change:
    //https://gerrit.mot.com/a/changes/IKSECURITY-1219
    //https://gerrit.mot.com/a/changes/?q=IKSWM-55397
    static HttpsURLConnection GerritApiEstablishSearchChangeConnection(String changeID) {
        URL gerritUrl = null;
        HttpsURLConnection gerritConnection = null;
        try {
            gerritUrl = new URL(GERRIT_SEARCH_URL + changeID);
            gerritConnection = (HttpsURLConnection) gerritUrl.openConnection();
            HttpDigestAuth gerritAuth = new HttpDigestAuth();
            HttpsURLConnection gerritAuthConnection = gerritAuth.tryAuth(gerritConnection, CherrypickHelper.LoginCredentials.GetUser(CherrypickHelper.LoginCredentials.LOGIN_TYPE.HTTPDIGEST), CherrypickHelper.LoginCredentials.GetPassword(CherrypickHelper.LoginCredentials.LOGIN_TYPE.HTTPDIGEST));
            if (gerritAuthConnection != null) {
                gerritAuthConnection.getInputStream();
                gerritConnection = gerritAuthConnection;
            }
        } catch (IOException e) {
            if (e.getMessage().contains("401")) {
                System.out.print("Authentication failure - try again");
                gerritConnection = null;
            } else if (e.getMessage().contains("403")) {
                System.out.print("Authentication Forbidden - blocked");
                gerritConnection = null;
            }
        }
        return gerritConnection;
    }

    static HttpsURLConnection GerritApiEstablishGetChangeConnection(String changeID) {
        URL gerritUrl = null;
        HttpsURLConnection gerritConnection = null;
        try {
            gerritUrl = new URL(GERRIT_CHANGE_URL + changeID);
            gerritConnection = (HttpsURLConnection) gerritUrl.openConnection();
            HttpDigestAuth gerritAuth = new HttpDigestAuth();
            HttpsURLConnection gerritAuthConnection = gerritAuth.tryAuth(gerritConnection, CherrypickHelper.LoginCredentials.GetUser(CherrypickHelper.LoginCredentials.LOGIN_TYPE.HTTPDIGEST), CherrypickHelper.LoginCredentials.GetPassword(CherrypickHelper.LoginCredentials.LOGIN_TYPE.HTTPDIGEST));
            if (gerritAuthConnection != null) {
                gerritAuthConnection.getInputStream();
                gerritConnection = gerritAuthConnection;
            }
        } catch (IOException e) {
            if (e.getMessage().contains("401")) {
                System.out.print("Authentication failure - try again");
                gerritConnection = null;
            } else if (e.getMessage().contains("403")) {
                System.out.print("Authentication Forbidden - blocked");
                gerritConnection = null;
            }
        }
        return gerritConnection;
    }
}
