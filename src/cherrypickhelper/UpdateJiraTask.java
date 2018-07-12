/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cherrypickhelper;

import javafx.concurrent.Task;

/**
 *
 * @author WLKS06
 */
public class UpdateJiraTask extends Task<Boolean>{
    private JiraInfo mJiraInfo;
    private String mJiraLabel;
    private String mJiraComment;
    
    UpdateJiraTask(JiraInfo j, String label, String comment) {
        mJiraInfo = j;
        mJiraLabel = label;
        mJiraComment = comment;
    }
    
    @Override
    protected Boolean call() throws Exception {
        Boolean retval = true;
        if(mJiraLabel != null) {
            //Update label
            if(JiraApi.JiraApiEstablishUpdateLabelConnection(mJiraInfo.id, mJiraLabel) == null) {
                System.out.println("Error updating Label");
                retval = false;
            }
        }
        if(mJiraComment != null && !mJiraComment.isEmpty()) {
            //Update comment
            if(JiraApi.JiraApiEstablishUpdateCommentConnection(mJiraInfo.id, mJiraComment) == null) {
                System.out.println("Error updating Comment");
                retval = false;
            }
        }
        return retval;
    }

    
    
}
