---
title: OpenMRS Configuration
---

Add Demo Data
-------------

When doing the initial setup wizard of OpenMRS, in Step 2, add the demo data to the database. This will add the basic form and load in the basic OpenMRS concept dictionary.

Add New Concepts
----------------

[600px](/Image:Creatediagnosis.png "wikilink")
[600px](/Image:Pic.png "wikilink")

Add the following concepts. Be careful not to set the wrong properties and note that you cannot delete concepts (only void them). Go to the Dictionary tab and then click "Add New Concept."

1.  **DOCTOR DIAGNOSIS**
     Description: Diagnosis for Sana telereferral case
     Class: Question
     Datatype: Text
2.  **DOCTOR URGENCY LEVEL**
     Description: Urgency level for Sana telereferral case
     Class: Question
     Datatype: Text
3.  **DOCTOR TREATMENT RECOMMENDATION**
     Description: Treatment recommendation for Sana telereferral case
     Class: Question
     Datatype: Text
4.  **DOCTOR COMMENTS**
     Description: Comments for Sana telereferral case
     Class: Question
     Datatype: Text
5.  **DOCTOR INFO REQUEST**
     Description: More information request for Sana telereferral case before diagnosis can be made
     Class: Question
     Datatype: Text
6.  **PICTURE:**
     Description: Image media file for patient encounter
     Class: Misc
     Datatype: Complex
     Handler: ThumbnailImageHandler
     Note: Sana and Media Viewer Modules must be loaded in for these handlers to appear as drop down options
7.  **SOUND:**
     Description: Audio media file for patient encounter
     Class: Misc
     Datatype: Complex
     Handler: MediaFileHandler
    Note: Sana and Media Viewer Modules must be loaded in for these handlers to appear as drop down options
8.  **VIDEO:**
     Description: Video media file for patient encounter
     Class: Misc
     Datatype: Complex
     Handler: MediaFileHandler
     Note: Sana and Media Viewer Modules must be loaded in for these handlers to appear as drop down options

At the end of adding all these concepts, search the dictionary for these concepts to ensure that they appear. (Example: <http://localhost/openmrs/dictionary/index.htm>)

Add Basic Form
--------------

The Basic Form is used when creating a new patient encounter when Sana cases are uploaded from the phone. When doing the initial setup wizard for OpenMRS, if you didn't select the "Add Demo Data" option, then you won't have the basic form loaded in. Check if you have it loaded in by going to the Administration tab \> Forms \> Manage Forms.
[600px](/Image:Manageforms.png "wikilink")
If you don't have the Basic Form already loaded in, go to "Add Form" and fill it out. Upload new XSLT file by copying the following file located on the MIT Media server (save this to a form.xslt file): <https://demo.sana.csail.mit.edu/openmrs/admin/forms/formViewXslt.form?formId=1>

Create New Roles and Manage Privileges
--------------------------------------

Restrict access to certain functionality in OpenMRS (such as permission to view patient records or permission to add a patient encounter) according to the healthcare worker's role. Manage these privileges by going to the Administration tab \> Users \> Manage Roles. Make sure that the below roles have the specified privileges. If the role doesn't exist already (Specialist and Remote Healthcare Worker), create it by clicking "Add Role" under the Role Management title. Note that the Specialist and Remote Healthcare Worker roles INHERIT privileges from the Provider role.

[600px](/Image:Createrole.png "wikilink")

#### Provider

This is an existing role already, so just modify its privileges.

-   Add Observations
-   Edit Encounters
-   Edit Observations
-   Edit Patients
-   Manage Encounter Queue
-   Patient Dashboard - View Demographics Section
-   Patient Dashboard - View Encounters Section
-   Patient Dashboard - View Forms Section
-   Patient Dashboard - View Graphs Section
-   Patient Dashboard - View Overview Section
-   Patient Dashboard - View Patient Summary
-   Patient Dashboard - View Regimen Section
-   View Concepts
-   View Concept Classes
-   View Concept Datatypes
-   View Concept Sources
-   View Encounters
-   View Encounter Types
-   View Forms
-   View Observations
-   View Navigation Menu
-   View Patient Identifiers
-   View Patients
-   View Person Attribute Types (Updated 5/1/10)
-   View Privileges
-   View Reports
-   View Roles (Updated 5/1/10)

#### Remote Healthcare Worker

NOTE: Remote Healthcare Worker inherits privileges from the role: Provider

-   Access REST API (for syncing the phone with patient database in OpenMRS)
-   Add People
-   Add Patients
-   Add Encounters
-   Manage Concepts
-   Manage Encounter Queue (to upload a case to the Sana queue)

#### Specialist

NOTE: Specialist inherits privileges from the role: Provider

-   View Navigation Bar (to access the menu bar tab to link to the Sana queue)
-   View Sana Queue (to view queue)

#### On-Call Provider

NOTE: Make sure the capitalization is precisely as above, this role doesn't inherit any other role

Add New Person Attributes
-------------------------

[600px](/Image:Addcontactphone.png "wikilink")

-   Contact Phone
    Format: java.lang.String
    Description: Phone number for person for Sana notifications
-   Contact Email
    Format: java.lang.String
    Description: Email for person for Sana notifications

#### Issue with OpenMRS User Attributes not displaying

The additional **Person Attributes** for the **Contact Phone** and **Contact Email** have shown to have some issues displaying in the **Admin --\> Manage Users** page with some of the 1.6 versions of OpenMRS. As a workaround, have each user go into the **My Profile --\> Notfications** page and enter an email address. The return sms number will be the one included with the mobile client's original upload and stored with the entry in the queue. (Note the issue seems to be fixed in more recent versions of OpenMRS. The next release of the Sana module will return to the original behavior utilizing the **Person Attributes**).

Create New Users
----------------

Because you don't want to give admin privileges out to the doctors who will be using the system, you should create new user accounts for each doctor and healthcare worker. Go to Administration tab \> Manage Users \> Add User

-   Select Role as Specialist (reviewing cases in Sana queue) or Remote Healthcare Worker (uploading cases to Sana queue)
-   Click "Show Advanced Options"
-   Fill in an email and contact phone number (in the format: 16173334567)
-   On the Sana phone app, in the Settings page, change the OpenMRS username and password to match this new user's credentials

[600px](/Image:_Newuser.png "wikilink")

To Receive Email Notifications
------------------------------

Sana is setup to only send SMS and email notifications when

-   Diagnosis is made by the specialist
-   Retake image request (or request for more information) is made by the specialist

The SMS will be sent to the phone number from which the case was uploaded.
The email will be sent to the "Contact Email" of the user who uploaded the case (the user being the OpenMRS username specified in the settings on the Sana Android phone app). You can modify the email address by going to Administration tab \> Users \> Manage Users \> click on user you want \> click on advanced options (Make sure the contact email attribute has been added, see above directions).

Set Global Properties so OpenMRS can point to MDS
-------------------------------------------------

Make sure that OpenMRS knows which Sana Dispatch Server it is communicating with. On the OpenMRS admin page, under "Maintenance," click on "Manage Global Properties." Scroll down to the following properties and set them to appropriate values according to your Sana Dispatch Server instance. Below are example values for the MIT server.
 [600px](/Image:Globalprops.png "wikilink")
\# **moca.mdsUri:** Location of the MDS. This site should say "Sanamobile MDS : Online" (Example: <http://localhost/mds>)

1.  **moca.email_notification_server_url:** For email notifications from OpenMRS that get routed through the MDS and out to the recipient according to the email server specified in the MDS settings (Example: <http://localhost/mds/notifications/submit/email/>)
2.  **moca.notification_server_url:** For SMS notifications from OpenMRS that get routed through the MDS and out to the recipient according to the SMS gateway specified in the MDS settings. (Example: <http://localhost/mds/notifications/submit/>)
3.  The default values for the other properties are fine.
4.  Scroll to the bottom of the page and **'SAVE**' changes.

Load in a Medical Ontology
--------------------------

When a specialist makes a diagnosis for a patient, instead of having them fill in the diagnosis in a free-text box, we wanted to have them select from a set of coded medical terms such as from the SNOMED vocabulary. Hence, the doctor can search for a diagnosis like "pneumonia," see the relevant search results in that medical vocabulary, and select ones to be tagged to the particular patient case. There would still be a "Comments" free text box, in case the specialist wasn't able to find the proper terms to describe the patient's condition.
[600px](/Image:Diagnosis.png "wikilink")
In order to load in an ontology (such as SNOMED, ICD-10, or others), perform a one-time upload procedure from a csv file of all the coded medical terms into the OpenMRS concept dictionary. To upload the vocabulary, go to the Administration tab \> Sana \> Manage Medical Vocabularies.
[600px](/Image:Lexicon.png "wikilink")
[600px](/Image:Snomedlist.png "wikilink")
Make sure that you have a csv file of the medical vocabulary.
For our demo server, we have loaded in the UMLS Enhanced VA/KP Problem List Subset of SNOMED CT, which is about 17,000 terms. For our reasons behind this choice, see this paper: <http://web.mit.edu/kkuan87/Public/HST950Paper.pdf>. You can obtain a copy of this medical vocabulary list by downloading it here: <http://www.nlm.nih.gov/research/umls/Snomed/core_subset.html>. You may need to apply for a free license.