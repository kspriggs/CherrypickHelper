/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cherrypickhelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Pair;
import javax.json.JsonObject;
import org.controlsfx.control.CheckComboBox;

/**
 *
 * @author WLKS06
 */
public class CherrypickHelper extends Application {
    private final static int OVERALL_HEIGHT = 800;
    private final static int OVERALL_WIDTH = 1050;
    private final static int CPREADYLIST_HEIGHT = 600;
    private final static int CPREADYLIST_WIDTH = 600;
    private final static int CPREADYCOMMITLIST_HEIGHT = 600;
    private final static int CPREADYCOMMITLIST_WIDTH = 400;
    private final static int JIRADESC_WIDTH = 1000;
    private final static int JIRADESC_HEIGHT = 400;
    private final static int JIRA_DISPLAY_WIDTH = 440;
    private final static int BLANK_WIDTH = 440;
    private final static int FOUND_WIDTH = 300;
    private final static int JIRACOMMENTENTRYWIDTH = 1000;
    private final static int JIRACOMMENTENTRYHEIGHT = 10;
    private final static String NUMJIRAFOUNDTEXT = "CRs: ";
    private final static String NUMCOMMITSFOUNDTEXT = "Commits: ";
    private final static String DOUBLECLICKPROMPT = "  DoubleClick to launch";
    
    private ChoiceBox mStableLineChoice;
    //The following arrays must be in sync and updated when adding new stable lines
    private static final String[] STABLELINESELECTIONS = {"test_only", "mmnc-stable1","mmnc-stable3","mmnc-stable6", "stable3_smr50"};
    private static final String[] STABLELINECPAPPROVEDLABEL = {"ignore_cp_approved", "pmst1_cp_approved", "pmst3_cp_approved", "pmst6_cp_approved", "pmst3smr50_cp_approved"};
    private static final String[] STABLELINECPREJECTLABEL = {"ignore_cp_rejected", "pmst1_cp_rejected", "pmst3_cp_rejected", "pmst6_cp_rejected", "pmst3smr50_cp_rejected"};
    private static final String[] STABLELINECPREADYFILTER = {"134823","129708", "129703", "132232", "135066"};
    //End synced stable line arrays

    private ChoiceBox mJiraDisplayOptions;
    private static final Number ALL = 0;
    private static final Number READY = 1;
    private static final Number READYAPP = 2;
    private static final Number READYNOTAPP = 3;
    private static final Number NOTREADY = 4;
    private static final String[] JIRADISPLAYOPTIONS = {"All", "Ready", "Ready applicable", "Ready not applicable", "Not Ready"};
    
    private ChoiceBox mCommitSortOptions;
    private static final Number SUBJECT = 0;
    private static final Number BRANCH = 1;
    private static final Number REPO = 2;
    private static final String[] COMMITSORTOPTIONS = {"Sort By Subject", "Sort by Branch", "Sort by Repo"};
    
    private int mActiveStableLine;
    private Label mNumJiraFound;
    private Label mCommitsJiraFound;
    private Label mBlankLabel;
    private ObservableList<JiraInfo> mCpReadyList;
    private ObservableList<JiraInfo> mCpReadyListDisplayed;
    private ListView<JiraInfo> mCpReadyListview;
    private ObservableList<GerritInfo> mCpReadyCommitList;
    private ObservableList<GerritInfo> mCpReadyCommitListDisplayed;
    private ListView<GerritInfo> mCpReadyCommitListview;
    private ObservableList<String> mCpReadyCommitBranchList;
    private CheckComboBox<String> mCommitBranchesToShow;
    private TextArea mJiraDescription;
    private ScrollPane mJiraScrollDescription;
    private QueryJiraCpReadyFilterTask mQueryJiraCpReadyTask;
  
    public static class LoginCredentials {
        public enum LOGIN_TYPE {ONEIT, HTTPDIGEST};
        private static String mUsername = "";
        private static String mPassword = "";
        //Gerrit uses Digest Authentication - and requires the HTTP password
        //that can be configured/found from Gerrit->Settings
        private static String mGerritUsername = "";
        private static String mGerritPassword = "";
        
        public static String GetUser(LOGIN_TYPE type) {
            if(type == LOGIN_TYPE.HTTPDIGEST) {
                return mGerritUsername;
            }
            else {
                return mUsername;
            }
        }

        public static String GetPassword(LOGIN_TYPE type) {
            if(type == LOGIN_TYPE.HTTPDIGEST) {
                return mGerritPassword;
            }
            else {
                return mPassword;
            }
        }
        

        public static void ClearCredentials(LOGIN_TYPE type) {
            if(type == LOGIN_TYPE.HTTPDIGEST) { 
                mGerritUsername = "";
                mGerritPassword = "";
            }
            else {
                mUsername = "";
                mPassword = "";
            }
        }

        public static Boolean HaveCredentials(LOGIN_TYPE type) {
            if(type == LOGIN_TYPE.HTTPDIGEST) {
                return (!mGerritUsername.isEmpty() && !mGerritPassword.isEmpty());
            }
            else {
                return (!mUsername.isEmpty() && !mPassword.isEmpty());
            }
        }

        public static Boolean ConfirmCredentials(LOGIN_TYPE type) {
            if(!HaveCredentials(type))
                GetLoginCredentials(type);
            if(type == LOGIN_TYPE.HTTPDIGEST) {
                while(!GerritApi.GerritApiValidateConnection()){
                    ClearCredentials(type);
                    GetLoginCredentials(type);
                }
                return HaveCredentials(type);
            }
            else {
                //TODO - clean this up eventually - for now an infinite loop
                while(!JiraApi.JiraApiValidateConnection()) {
                    ClearCredentials(type);
                    GetLoginCredentials(type);
                }
                return true;
            }
        }
        public static Boolean GetLoginCredentials(LOGIN_TYPE type) {
            // Create the custom dialog.
            Dialog<Pair<String, String>> dialog = new Dialog<>();
            dialog.setTitle("Login Dialog");
            if(type == LOGIN_TYPE.HTTPDIGEST) {
                dialog.setHeaderText("Enter Gerrit Credentials (\"HTTP Password\" in Gerrit Account Settings)");
            }
            else {
                dialog.setHeaderText("Enter OneIT userid and password");
            }

            // Set the button types.
            ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

            // Create the username and password labels and fields.
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            TextField username = new TextField();
            username.setPromptText("Username");
            PasswordField password = new PasswordField();
            if(type == LOGIN_TYPE.HTTPDIGEST) {
                password.setPromptText("Gerrit HTTP Password");
            }
            else {
                password.setPromptText("OneIT Password");
            }

            grid.add(new Label("Username:"), 0, 0);
            grid.add(username, 1, 0);
            grid.add(new Label("Password:"), 0, 1);
            grid.add(password, 1, 1);

            // Enable/Disable login button depending on whether a username was entered.
            Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
            loginButton.setDisable(true);

            // Do some validation (using the Java 8 lambda syntax).
            username.textProperty().addListener((observable, oldValue, newValue) -> {
                loginButton.setDisable(newValue.trim().isEmpty());
            });

            dialog.getDialogPane().setContent(grid);

            // Request focus on the username field by default.
            Platform.runLater(() -> username.requestFocus());

            // Convert the result to a username-password-pair when the login button is clicked.
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == loginButtonType) {
                    return new Pair<>(username.getText(), password.getText());
                }
                return null;
            });

            Optional<Pair<String, String>> result = dialog.showAndWait();

            result.ifPresent(usernamePassword -> {
                if (type == LOGIN_TYPE.HTTPDIGEST) {
                    mGerritUsername = usernamePassword.getKey();
                    mGerritPassword = usernamePassword.getValue();
                } else {
                    mUsername = usernamePassword.getKey();
                    mPassword = usernamePassword.getValue();
                }
            });
            return HaveCredentials(type);
        }
    }

    @Override
    public void start(Stage stage) {
        start_CherryPickUi(stage);
    }

    public void start_CherryPickUi(Stage stage) {
        //See https://docs.oracle.com/javafx/2/layout/builtin_layouts.htm for layout 
        //details
        stage.setTitle("Stable Line Cherrypick Helper");
        stage.setWidth(OVERALL_WIDTH);
        //stage.setHeight(OVERALL_HEIGHT);
        stage.setHeight(Screen.getPrimary().getVisualBounds().getHeight());
        Scene scene = new Scene(new Group());

        final VBox root = new VBox();
        root.setPadding(new Insets(8, 8, 8, 8));
        root.setSpacing(5);
        root.setAlignment(Pos.TOP_LEFT);

        final GridPane grid = new GridPane();
        grid.setVgap(5);
        grid.setHgap(10);

        //UI Element: ControlsBox - create Horizontal Box to hold Jira text entry and get button
        //http://docs.oracle.com/javafx/2/ui_controls/choice-box.htm
        final GridPane controlsPane = new GridPane();
        controlsPane.setPadding(new Insets(8, 8, 8, 8));
        controlsPane.setHgap(10);
        controlsPane.setVgap(10);
        controlsPane.setStyle("-fx-background-color: #336699;");
        mStableLineChoice = new ChoiceBox(FXCollections.observableArrayList(STABLELINESELECTIONS));
        mNumJiraFound = new Label();
        mNumJiraFound.setPrefWidth(FOUND_WIDTH);
        mBlankLabel = new Label();
        mBlankLabel.setText("");
        mBlankLabel.setPrefWidth(BLANK_WIDTH);
        mNumJiraFound.setTextFill(Color.WHITE);
        mCommitsJiraFound = new Label();
        mCommitsJiraFound.setTextFill(Color.WHITE);
        mCommitsJiraFound.setPrefWidth(FOUND_WIDTH);
        mStableLineChoice.setTooltip(new Tooltip("Select the stable line desired."));
        //TODO - add stableline listener
        mStableLineChoice.getSelectionModel().selectedIndexProperty().addListener(
                new ChangeListener<Number>() {
            public void changed(ObservableValue ov,
                    Number old_val, Number new_val) {
                mActiveStableLine = new_val.intValue();
                if (mQueryJiraCpReadyTask != null) {
                    //Task running - kill
                    if(!mQueryJiraCpReadyTask.cancel()) {
                        System.out.println("Error cancelling - check");
                    }
                }
                mQueryJiraCpReadyTask = new QueryJiraCpReadyFilterTask(STABLELINECPREADYFILTER[new_val.intValue()]);
                ProgressBar bar = new ProgressBar();
                bar.progressProperty().bind(mQueryJiraCpReadyTask.progressProperty());
                Stage waitStage = waitTaskDialog(bar, mQueryJiraCpReadyTask);
                mQueryJiraCpReadyTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {

                    @Override
                    public void handle(WorkerStateEvent event) {
                        List<JiraInfo> output = (List<JiraInfo>) event.getSource().getValue();
                        if (output != null) {
                            mCpReadyList.clear();
                            mCpReadyListDisplayed.clear();
                            mCpReadyCommitList.clear();
                            mCpReadyCommitListDisplayed.clear();
                            mCpReadyCommitBranchList.clear();
                            mCommitsJiraFound.setText("");
                            mJiraDisplayOptions.getSelectionModel().clearSelection();
                            mNumJiraFound.setText("");
                            Iterator i = output.iterator();
                            while (i.hasNext()) {
                                JiraInfo j = (JiraInfo) i.next();
                                mCpReadyList.add(j);
                            }
                            mCpReadyListDisplayed.addAll(mCpReadyList);
                            mNumJiraFound.setText(NUMJIRAFOUNDTEXT + Integer.toString(mCpReadyListDisplayed.size()) + DOUBLECLICKPROMPT);
                        }
                        mQueryJiraCpReadyTask = null;
                        waitStage.close();
                    }
                });
                
                if(LoginCredentials.ConfirmCredentials(CherrypickHelper.LoginCredentials.LOGIN_TYPE.ONEIT) &&
                   LoginCredentials.ConfirmCredentials(CherrypickHelper.LoginCredentials.LOGIN_TYPE.HTTPDIGEST))
                    new Thread(mQueryJiraCpReadyTask).start();
                
            }
        });
        mNumJiraFound.setText("");
        
        mJiraDisplayOptions = new ChoiceBox(FXCollections.observableArrayList(JIRADISPLAYOPTIONS));
        mJiraDisplayOptions.setPrefWidth(JIRA_DISPLAY_WIDTH);
        mJiraDisplayOptions.setTooltip(new Tooltip("Select sorting method."));
        mJiraDisplayOptions.getSelectionModel().selectedIndexProperty().addListener(
                new ChangeListener<Number>() {
            public void changed(ObservableValue ov,
                    Number old_val, Number new_val) {
                if (mCpReadyList.size() > 0) {
                    mCpReadyCommitList.clear();
                    mCpReadyCommitListDisplayed.clear();
                    mCpReadyCommitBranchList.clear();
                    mJiraDescription.clear();
                    if (new_val == ALL) {
                        System.out.println("Showing All");
                        mCpReadyListDisplayed.clear();
                        mCpReadyListDisplayed.addAll(mCpReadyList);
                    } else if (new_val == READY) {
                        System.out.println("Diplaying only CRs in ready");
                        mCpReadyListDisplayed.clear();
                        for(JiraInfo j : mCpReadyList) {
                            if(j.isReady()) {
                                mCpReadyListDisplayed.add(j);
                            }
                        }
                    }
                    else if(new_val == READYAPP) {
                        System.out.println("Displaying only CRs ready and with applicable branches");
                        
                        //TODO - need to implement something that looks up by applicable branches to the stable
                        //line in question
                        showCodeNoteImplemented();

                    }
                    else if(new_val == READYNOTAPP) {
                        System.out.println("Displaying only CRs ready but with not applicable branches");
                        //TODO - need to implement something that looks up by applicable branches to the stable
                        //line in question
                        showCodeNoteImplemented();
                    }
                    else if(new_val == NOTREADY) {
                        System.out.println("Sorting by repo");
                        mCpReadyListDisplayed.clear();
                        for(JiraInfo j : mCpReadyList) {
                            if(!j.isReady()) {
                                mCpReadyListDisplayed.add(j);
                            }
                        }
                    }
                    mNumJiraFound.setText(NUMJIRAFOUNDTEXT + Integer.toString(mCpReadyListDisplayed.size()) + DOUBLECLICKPROMPT);
                }
                
            }
        });
        
        mCommitSortOptions = new ChoiceBox(FXCollections.observableArrayList(COMMITSORTOPTIONS));
        mCommitSortOptions.setTooltip(new Tooltip("Select sorting method."));
        mCommitSortOptions.getSelectionModel().selectedIndexProperty().addListener(
                new ChangeListener<Number>() {
            public void changed(ObservableValue ov,
                    Number old_val, Number new_val) {
                if(mCpReadyCommitListDisplayed.size() > 0) {
                    if(new_val == BRANCH) {
                        System.out.println("Sorting by branch name");
                        FXCollections.sort(mCpReadyCommitListDisplayed, GerritInfo.BranchComparator);
                    }
                    else if(new_val == SUBJECT) {
                        System.out.println("Sorting by commit info");
                        FXCollections.sort(mCpReadyCommitListDisplayed, GerritInfo.InfoComparator);
                    }
                    else if(new_val == REPO) {
                        System.out.println("Sorting by repo");
                        FXCollections.sort(mCpReadyCommitListDisplayed, GerritInfo.RepoComparator);
                    }
                }
                
            }
        });
        mCpReadyCommitBranchList = FXCollections.observableArrayList();
        mCommitBranchesToShow = new CheckComboBox<String>(mCpReadyCommitBranchList);
        mCommitBranchesToShow.setTooltip(new Tooltip("Select branches to limit display"));
        mCommitBranchesToShow.getCheckModel().getCheckedItems().addListener(new ListChangeListener<String>() {
            public void onChanged(ListChangeListener.Change<? extends String> c) {
                ObservableList<String> selected = mCommitBranchesToShow.getCheckModel().getCheckedItems();
                ObservableList<GerritInfo> newlist = FXCollections.observableArrayList();
                for(String selection : selected) {
                    System.out.println("Adding commits with branch: "+selection);
                    for(GerritInfo g : mCpReadyCommitList) {
                        if(g.branch.equals(selection)) {
                            newlist.add(g);
                            System.out.println("Added commit: "+g.commit_id);
                        }
                    }
                }
                mCpReadyCommitListDisplayed.clear();
                mCpReadyCommitListDisplayed.addAll(newlist);
                mCommitsJiraFound.setText("");
                if (mCpReadyCommitListDisplayed.size() > 0) {
                    mCommitsJiraFound.setText(NUMCOMMITSFOUNDTEXT + Integer.toString(mCpReadyCommitListDisplayed.size())+DOUBLECLICKPROMPT);

                    Number sortby = mCommitSortOptions.getSelectionModel().getSelectedIndex();
                    if (sortby == BRANCH) {
                        System.out.println("Sorting by branch name");
                        FXCollections.sort(mCpReadyCommitListDisplayed, GerritInfo.BranchComparator);
                    } else if (sortby == SUBJECT) {
                        System.out.println("Sorting by commit info");
                        FXCollections.sort(mCpReadyCommitListDisplayed, GerritInfo.InfoComparator);
                    } else if (sortby == REPO) {
                        System.out.println("Sorting by repo");
                        FXCollections.sort(mCpReadyCommitListDisplayed, GerritInfo.RepoComparator);
                    }
                }
            }
        });
        GridPane.setConstraints(mStableLineChoice, 0, 0);
        GridPane.setConstraints(mNumJiraFound, 0, 1);
        GridPane.setColumnSpan(mNumJiraFound, 2);
        GridPane.setConstraints(mJiraDisplayOptions, 1, 0);        
        //GridPane.setConstraints(mBlankLabel, 1, 0);
        GridPane.setConstraints(mCommitSortOptions, 2, 0);
        GridPane.setConstraints(mCommitBranchesToShow, 3, 0);
        GridPane.setConstraints(mCommitsJiraFound, 2, 1);
        GridPane.setColumnSpan(mCommitsJiraFound, 2);
        controlsPane.getChildren().addAll(mStableLineChoice, mNumJiraFound, mJiraDisplayOptions, mCommitSortOptions, mCommitBranchesToShow, mCommitsJiraFound);
        GridPane.setRowIndex(controlsPane, 0);
        GridPane.setColumnSpan(controlsPane, 2);
        grid.getChildren().add(controlsPane);

        //UI Element: CR List - a list of ready to cherrypick CRs from selected stableline
        //http://docs.oracle.com/javafx/2/ui_controls/list-view.htm
        mCpReadyListview = new ListView<JiraInfo>();
        mCpReadyList = FXCollections.observableArrayList();
        mCpReadyListDisplayed = FXCollections.observableArrayList();
        mCpReadyListview.setItems(mCpReadyListDisplayed);
        mCpReadyListview.setPrefHeight(CPREADYLIST_HEIGHT);
        mCpReadyListview.setPrefWidth(CPREADYLIST_WIDTH);
        mCpReadyListview.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        mCpReadyListview.getSelectionModel().selectedItemProperty().addListener(
                new ChangeListener<JiraInfo>() {
            public void changed(ObservableValue<? extends JiraInfo> ov,
                    JiraInfo old_val, JiraInfo new_val) {
                //TODO - implement change listener
                if (new_val != null) {
                    mCpReadyCommitList.clear();
                    mCpReadyCommitListDisplayed.clear();
                    mCpReadyCommitBranchList.clear();
                    mJiraDescription.clear();
                    mJiraDescription.setText(new_val.description);
                    if (new_val.comments != null) {
                        for (int k = 0; k < new_val.comments.size(); k++) {
                            JsonObject object = new_val.comments.getJsonObject(k);
                            String comment = "";
                            String author = "";
                            String date = "";
                            try {
                                comment = object.getString("body");
                                date = object.getString("created");
                                JsonObject authorObject = object.getJsonObject("author");
                                author = authorObject.getString("displayName");
                            } catch (Exception e) {
                                System.out.println("Error getting comment details" + e);
                            }
                            mJiraDescription.appendText("\n<--------------------- Comment: " + author + " : " + date + "--------------------->\n" + comment);
                        }
                    }
                    mJiraDescription.positionCaret(0);
                    mJiraScrollDescription.setVvalue(0);
                    for (GerritInfo gerrit : new_val.gerritList) {
                        mCpReadyCommitList.add(gerrit);
                        if(!mCpReadyCommitBranchList.contains(gerrit.branch)) {
                            mCpReadyCommitBranchList.add(gerrit.branch);
                        }
                    }
                    mCpReadyCommitListDisplayed.addAll(mCpReadyCommitList);
                    mCommitsJiraFound.setText("");
                    if(mCpReadyCommitList.size() > 0) {
                        mCommitsJiraFound.setText(NUMCOMMITSFOUNDTEXT+Integer.toString(mCpReadyCommitList.size())+DOUBLECLICKPROMPT);
                    }
                }
            }
        });

        mCpReadyListview.setOnMouseClicked(new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent click) {

                if (click.getClickCount() == 2) {
                    //Use ListView's getSelected Item
                    JiraInfo currentItemSelected = mCpReadyListview.getSelectionModel().getSelectedItem();
                    System.out.println("Double Clicked Jira item: "+currentItemSelected.id);
                    getHostServices().showDocument(JiraInfo.launchURL+currentItemSelected.id);
                }
            }
        });
        GridPane.setConstraints(mCpReadyListview, 0, 1);
        grid.getChildren().add(mCpReadyListview);

        //UI Element: Commit List - a list of commits associated with the selected CR
        //http://docs.oracle.com/javafx/2/ui_controls/list-view.htm
        mCpReadyCommitListview = new ListView<GerritInfo>();
        mCpReadyCommitList = FXCollections.observableArrayList();
        mCpReadyCommitListDisplayed = FXCollections.observableArrayList();
        mCpReadyCommitListview.setItems(mCpReadyCommitListDisplayed);
        mCpReadyCommitListview.setPrefHeight(CPREADYCOMMITLIST_HEIGHT);
        mCpReadyCommitListview.setPrefWidth(CPREADYCOMMITLIST_WIDTH);
        mCpReadyCommitListview.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        mCpReadyCommitListview.getSelectionModel().selectedItemProperty().addListener(
                new ChangeListener<GerritInfo>() {
                    public void changed(ObservableValue<? extends GerritInfo> ov,
                            GerritInfo old_val, GerritInfo new_val) {
                        //TODO - implement change listener
                    }
                });
        mCpReadyCommitListview.setOnMouseClicked(new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent click) {

                if (click.getClickCount() == 2) {
                    //Use ListView's getSelected Item
                    GerritInfo currentItemSelected = mCpReadyCommitListview.getSelectionModel().getSelectedItem();
                    System.out.println("Double Clicked Gerrit item: "+currentItemSelected.commit_id);
                    getHostServices().showDocument(GerritInfo.launchURL+currentItemSelected.commit_id);
                }
            }
        });
        GridPane.setConstraints(mCpReadyCommitListview, 1, 1);
        grid.getChildren().add(mCpReadyCommitListview);
        
        mJiraDescription = new TextArea();
        mJiraDescription.setWrapText(true);
        mJiraDescription.setPrefWidth(JIRADESC_WIDTH);
        mJiraDescription.setMaxHeight(JIRADESC_HEIGHT);
        mJiraScrollDescription = new ScrollPane();
        mJiraScrollDescription.getStyleClass().add("noborder-scroll-pane");
        mJiraScrollDescription.setContent(mJiraDescription);
        mJiraScrollDescription.setPrefWidth(JIRADESC_WIDTH);
        mJiraScrollDescription.setMaxHeight(JIRADESC_HEIGHT);
        GridPane.setRowIndex(mJiraScrollDescription, 2);
        GridPane.setColumnSpan(mJiraScrollDescription, 2);
        grid.getChildren().add(mJiraScrollDescription);
        
        final GridPane cpActivityPane = new GridPane();
        cpActivityPane.setPadding(new Insets(8, 8, 8, 8));
        cpActivityPane.setHgap(10);
        cpActivityPane.setVgap(10);
        cpActivityPane.setStyle("-fx-background-color: #336699;");
        /*
        final TextField jiraCommentText = new TextField();
        jiraCommentText.setPromptText("Add a comment to be added to the CR.");
        jiraCommentText.setPrefColumnCount(JIRACOMMENTENTRYWIDTH);
        jiraCommentText.setStyle("-fx-base: #8CAB35;");
        */
        final TextArea jiraCommentText = new TextArea();
        jiraCommentText.setWrapText(true);
        jiraCommentText.setPrefWidth(JIRACOMMENTENTRYWIDTH);
        jiraCommentText.setPrefHeight(JIRACOMMENTENTRYHEIGHT);
        jiraCommentText.setPromptText("Add a comment to be added to the CR.");
        
        final ScrollPane jiraScrollDescription = new ScrollPane();
        jiraScrollDescription.getStyleClass().add("noborder-scroll-pane");
        jiraScrollDescription.setContent(jiraCommentText);
        jiraScrollDescription.setPrefWidth(JIRACOMMENTENTRYWIDTH);
        //jiraScrollDescription.setPrefHeight(1000);

        final Button approveCpButton = new Button("Approve");
        approveCpButton.setStyle("-fx-base: #8CAB35;");
        approveCpButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                ObservableList m = mCpReadyListview.getSelectionModel().getSelectedItems();
                Iterator i = m.iterator();
                while (i.hasNext()) {
                    JiraInfo j = (JiraInfo) i.next();
                    if (j != null) {
                        String jiraLabel = STABLELINECPAPPROVEDLABEL[mActiveStableLine];
                        if (!jiraLabel.isEmpty()) {
                            UpdateJiraTask t = new UpdateJiraTask(j, jiraLabel, null);
                            t.setOnSucceeded(new EventHandler<WorkerStateEvent>() {

                                @Override
                                public void handle(WorkerStateEvent event) {
                                    if((Boolean)event.getSource().getValue() == false) {
                                        Alert alert = new Alert(AlertType.INFORMATION);
                                        alert.setHeaderText("Error updating labels or comments!");
                                        alert.setContentText("There has been an error updating: "+j.id);
                                        alert.showAndWait();
                                    }
                                }
                            });
                            t.run();
                        }

                        String jiraComment = jiraCommentText.getText();
                        if (!jiraComment.isEmpty()) {
                            UpdateJiraTask t = new UpdateJiraTask(j, null, jiraComment.replaceAll("\n", "\\\\n"));
                            t.setOnSucceeded(new EventHandler<WorkerStateEvent>() {

                                @Override
                                public void handle(WorkerStateEvent event) {
                                    if((Boolean)event.getSource().getValue() == false) {
                                        Alert alert = new Alert(AlertType.INFORMATION);
                                        alert.setHeaderText("Error updating labels or comments!");
                                        alert.setContentText("There has been an error updating: "+j.id);
                                        alert.showAndWait();
                                    }
                                }
                            });
                            t.run();
                        }
                    }
                }
            }
        });
        final Button rejectCpButton = new Button("Reject");
        rejectCpButton.setStyle("-fx-base: #cc0000;");
        rejectCpButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                ObservableList m = mCpReadyListview.getSelectionModel().getSelectedItems();
                Iterator i = m.iterator();
                while (i.hasNext()) {
                    JiraInfo j = (JiraInfo) i.next();
                    if (j != null) {
                        String jiraLabel = STABLELINECPREJECTLABEL[mActiveStableLine];
                        if (!jiraLabel.isEmpty()) {
                            UpdateJiraTask t = new UpdateJiraTask(j, jiraLabel, null);
                            t.setOnSucceeded(new EventHandler<WorkerStateEvent>() {

                                @Override
                                public void handle(WorkerStateEvent event) {
                                    if ((Boolean) event.getSource().getValue() == false) {
                                        Alert alert = new Alert(AlertType.INFORMATION);
                                        alert.setHeaderText("Error updating labels or comments!");
                                        alert.setContentText("There has been an error updating: "+j.id);
                                        alert.showAndWait();
                                    }
                                }
                            });
                            t.run();
                        }

                        String jiraComment = jiraCommentText.getText();
                        if (!jiraComment.isEmpty()) {
                            UpdateJiraTask t = new UpdateJiraTask(j, null, jiraComment.replaceAll("\n", "\\\\n"));
                            t.setOnSucceeded(new EventHandler<WorkerStateEvent>() {

                                @Override
                                public void handle(WorkerStateEvent event) {
                                    if((Boolean)event.getSource().getValue() == false) {
                                        Alert alert = new Alert(AlertType.INFORMATION);
                                        alert.setHeaderText("Error updating labels or comments!");
                                        alert.setContentText("There has been an error updating: "+j.id);
                                        alert.showAndWait();
                                    }
                                }
                            });
                            t.run();
                        }
                    }
                }
            }
        });
        final Button commentCpButton = new Button("Comment Only");
        commentCpButton.setStyle("-fx-base: #cc66ff;");
        commentCpButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                ObservableList m = mCpReadyListview.getSelectionModel().getSelectedItems();
                Iterator i = m.iterator();
                while (i.hasNext()) {
                    JiraInfo j = (JiraInfo) i.next();
                    if (j != null) {
                        String jiraComment = jiraCommentText.getText();
                        if (!jiraComment.isEmpty()) {
                            UpdateJiraTask t = new UpdateJiraTask(j, null, jiraComment.replaceAll("\n", "\\\\n"));
                            t.run();
                        }
                    }
                }
            }
        });
        final Button clearCommentCpButton = new Button("Clear Comment");
        clearCommentCpButton.setStyle("-fx-base: #ffffff;");
        clearCommentCpButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                jiraCommentText.clear();
            }
        });
        GridPane.setConstraints(approveCpButton, 0,0);
        GridPane.setConstraints(rejectCpButton, 1,0);
        GridPane.setConstraints(commentCpButton, 2,0);
        GridPane.setConstraints(clearCommentCpButton, 3,0);
        GridPane.setHalignment(clearCommentCpButton, HPos.RIGHT);
        GridPane.setConstraints(jiraCommentText, 0,1);
        GridPane.setColumnSpan(jiraCommentText, 4);
        cpActivityPane.getChildren().addAll(approveCpButton, rejectCpButton, commentCpButton, clearCommentCpButton, jiraCommentText);
        GridPane.setRowIndex(cpActivityPane, 3);
        GridPane.setColumnSpan(cpActivityPane, 2);
        grid.getChildren().add(cpActivityPane);
        root.getChildren().add(grid);
        scene.setRoot(root);
        stage.setScene(scene);
        stage.show();
    }
    
    public static void showCodeNoteImplemented() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setHeaderText("Code Not Implemented!");
        alert.setContentText("This feature has not yet been implemented....");
        alert.showAndWait();
    }
    public static Stage waitTaskDialog(ProgressBar p, QueryJiraCpReadyFilterTask q) {
        Stage dialogStage = new Stage();
        dialogStage.initStyle(StageStyle.UTILITY);
        dialogStage.setResizable(false);
        dialogStage.initModality(Modality.APPLICATION_MODAL);

        final Label label = new Label();
        label.setText("Collecting Jira info......may take a while.");

        final Button cancelTask = new Button();
        cancelTask.setText("Cancel");
        cancelTask.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                q.cancel();
                dialogStage.close();
            }
        });
        
        
        final GridPane DialogPane = new GridPane();
        DialogPane.setPadding(new Insets(8, 8, 8, 8));
        DialogPane.setHgap(10);
        DialogPane.setVgap(10);
        DialogPane.setConstraints(label, 0,0);
        DialogPane.setConstraints(p, 1,0);
        DialogPane.setConstraints(cancelTask, 0,1);
        DialogPane.getChildren().addAll(label, p, cancelTask);
        
        Scene scene = new Scene(DialogPane);
        dialogStage.setScene(scene);
        dialogStage.show();
        return dialogStage;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
