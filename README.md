# Cherrypick Helper
This is a simple JavaFX app with an interface into Jira and Gerrit which pulls key data from each 
to allow for easier and more efficient review of content that has been marked for integration
into a project

## Key assumptions
- CRs in Jira are marked for integration with the use of labels.  The Cherrypick Helper tool uses
Jira filters which are setup to identify the content with these labels as the lists of interest.
- For each CR in a given Jira filter, the tool finds embedded Gerrit commits and pulls details
of each commit such as branchname and lines of code changed to allow the user to better identify
which commit should be reviewed in further detail
- The tool allows for the addition of comments and labels in a selected Jira CR - this is the
mechanism to identify the given CR as approved or rejected for integration purposes

