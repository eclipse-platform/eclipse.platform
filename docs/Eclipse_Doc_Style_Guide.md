Eclipse Doc Style Guide
=======================

This document gives the style conventions to be used in [Eclipse help](/Eclipse_Documentation "Eclipse Documentation"). 
The Eclipse help has the following topic types: Tutorial (Getting Started), Concept, Task, and Reference. 
These topic types are based on the [Darwin Information Typing Architecture (DITA)](http://www.ibm.com/developerworks/library/x-dita1/) standards.

Contents
--------

*   [1 Topic Titles](#Topic-Titles)
*   [2 Lists](#Lists)
*   [3 Inline Markup](#Inline-Markup)
*   [4 Topic Content](#Topic-Content)
*   [5 Tables](#Tables)
*   [6 CSS Class Summary](#CSS-Class-Summary)
*   [7 Graphics](#Graphics)
*   [8 Indexing](#Indexing)

### Topic Titles

| Guideline | Example |
| --- | --- |
| A topic's _title_ (heading) is marked with a heading value that matches the topic's position in the table of contents. The topic shown here is at the top level, so it is tagged as an `<h1>`. | `<h1>A Tour of the Workbench</h1>` |
| The text in a topic's <title\> must match the text in the heading. | From concepts-2.htm:<br> `<title>The Workbench</title>   ...  <h1>Workbench</h1>`       |
| A topic's <title\> must be _unique_ to enable users to make a correct choice after a search.  Use sentence capitalization for all titles.   | Results of searching for **workbench**: <br> `88% The workbench (concepts-2.htm)`<br>  `74% Workbench (ref-10.htm)`<br>  `70% The workbench (qs-02a.htm)`  <br>    Better titles: <br> `88% What is the workbench? (concepts-2.htm)` <br>  `74% Workbench reference (ref-10.htm)` <br> `70% Launching the workbench (qs-02a.htm)`       |
| Begin "task" and "getting started" titles with a [gerund](http://en.wikipedia.org/wiki/Gerund). | **Creating a project** |

### Lists

| Guideline | Example |
| --- | --- |
| Use a sentence fragment followed by a colon before numbered steps. | To hide files by type: |
| Each numbered list item should include only one step. |   |
| If you have an ordered list within an ordered list, use:     `<ol type="a"></ol>`   | To do X:  1.  Step 1 text. 2.  Step 2 introductory text:     1.  Sub-step 1.     2.  Sub-step 2.   |
| If you need to start an ordered list at a number other than 1, use     `<li value="3">text</li>`   |   1.  text   |
| Each task topic should contain a reasonable number of steps.  "Reasonable" is a judgment call, and your freedom may be restricted by the complexity of the software. However, if you find that you are over 10 steps, see if you can break the steps into two sub-topics.   |   |
| When describing menu items, use third-person descriptive verb tenses rather than imperative ones. | ~~Create~~ a new file -> **Creates** a new file |

### Inline Markup

| Guideline | Example |
| --- | --- |
| Elements names that appear in the GUI should be identified as such.  The standard convention for GUI elements is to display the name in a bold font. There are different ways this can be done: <br>   - Use `<b>` tags  <br>  - Use the `<strong>` tags and expect that they will render as bold <br>  - Use `<span>` tags with different class types. For example: <br>       -- `<em class="UILabel">Preferences</em>`     <br>   -- `<span class="guibutton">OK</span>` <br> The benefit of using class types is that the CSS file can then declare:     <br>              EM.UILabel {font-weight: bold;}  <br>        Or for a more elaborate display:               <br>    .guibutton { color: #000000; font-weight: bold;  <br>    font-family: helvetica, sans-serif;  <br>    background-color: #DCDCDC; padding: 1px 5px; <br>      font-size: 10pt; border: outset 2px;  <br>    text-decoration: none; line-height:175%; }           | **Preferences**     OK   |

### Topic Content

| Guideline | Example |
| --- | --- |
| Provide a short description of what the user will accomplish. | Files that you do not need to see can be hidden by file type in  the **C/C++ Projects** view.  (Note: That wording could be better—see below.)   |
| Use the active voice. | You can hide file types that you do not need to see in  the **C/C++ Projects** view.  (Note: That wording could _still_ be better—see below.)   |
| Wherever possible, make statements positive. | In the **C/C++ Projects** view, you can display only the file types that you need to see. |
| Include no more than one task for each topic. |   |
| Document how to access a feature through the menu bar, as opposed to using toolbar buttons or the context menu.  Avoid providing multiple ways to do something unless the menu-driven method is not always available. Project properties are an example of this: you can set project properties in the New Project wizard, but after the project is created you can set properties only by right-clicking on an individual project or in preferences (for all projects).   | Click **File > New > Project**.  Note that the term "menu" is not used and that the menu path is in bold.   |
| When you must provide multiple ways to do something use the format in the example. |   1.  Do one of the following:     *   To set properties for future Standard Make projects, click **Window > Preferences** . Expand **C/C++,** click **New Make Projects**.     *   In the **C/C++ Projects** view, right-click a project, and select **Properties**. Select **C/C++ Make Project** from the list.   |
| To instruct the user to make a choice, use the format in the example. |   1.  Do one of the following:     *   To stop the build when an error is encountered, select **Stop on error**.     *   To continue the build even if an error is encountered, select **Keep going on error**.   |
| When describing how to use the context menu (shortcut menu), instruct the user to "right-click <something> and select <something>."  Do not say, "From the context-menu, choose <something> as the term context-menu is not obvious to new users.   | In the **C/C++ Projects** view, right-click a project and select **Properties**. |
| Bold the name of the item being acted on (that is, the text), not the name of interface control. | Type the name `**JanesProject**` in the **Project name** field. |
| For the results of a step, do not add the sentence, "The New Project wizard opens." Instead, give the name of the dialog box or wizard that opens as an introductory phrase at the beginning of the next step. |   1.  Click **File > New > Project**. 2.  In the **New Project** wizard, click **C** or **C++**.   |
| Begin a step, where applicable, by telling the user "To <do this>, <do that>."  In other words, give the consequences of the action before you give the instructions to perform.   | To change tab settings, type a value in the **Value** box. |
| Do not use the word "button", simply tell the reader to "click <**name of button**>." | Click **Next**. |
| Use the verbs "select" or "clear" for check boxes; bold only the name of the check box. | Select or clear the **Blank** check box. |
| Do not use the word "radio button". Use "click <**name of radio button**>." | To change the background color, click **Custom**. |
| Do not instruct the user to "click on" something. Use "click", "right-click" or "double-click." | In the **C/C++ Projects** view, double-click your project. |
| Do not instruct the user to "click on". Use "click the <**name of tab**\> tab." | Click the **General** tab. |
| Do not use the word "tree". Use "Expand <**something**>, click <**something else**>. |   1.  Click **Window > Preferences**. 2.  Expand **C/C++**, click **C/C++ Editor**.   |
| Use "type" instead of "enter."  ("Enter" means "type <text> and press \[Enter\].)   | In the **Name** box, type a name. |
| Use the [second person](http://en.wikipedia.org/wiki/Grammatical_person) when referring to the intended audience of the document. Do not use "the user" or "the client" to refer to members of your audience. | ~~The user~~ _You_ can preview the files that are about to be committed from the comments page. |
| Consider having inline links to concepts that are Eclipse-specific. |   <p>This tutorial provides a step-by-step walk-through of the features in the Eclipse  <a href="../concepts/concepts-2.htm">Workbench</a>. You will learn how to use these features by creating a <a href="../concepts/concepts-12.htm">project</a>, then creating, running, and debugging a simple program.</p>  This tutorial provides a step-by-step walk-through of the features in the Eclipse Workbench. You will learn how to use these features by creating a project, then creating, running, and debugging a simple program.   |
| Do not use Eclipse implementation details such as plug-in ids, component names, project names, or version numbers in user documentation. Such content is fine in plug-in developer (ISV) documentation. Keep in mind the end user may be using a branded product with a different version number. | ~~Since Equinox 3.3, you can...~~You can... |

### Tables

| Guideline | Example |
| --- | --- |
| Declare tables consistently. |   `<table border="1" cellpadding="3" cellspacing="0">`   |
| Set the vertical alignment for rows to "top" in the CSS file. |   `tr { vertical-align: top; }`   |

### CSS Class Summary

| Guideline | Example |
| --- | --- |
|   To call the book.css file, change the current markup from   `<link rel="stylesheet" href="../book.css" type="text/css">`   to   `<link rel="stylesheet" href="../../PRODUCT_PLUGIN/book.css" type="text/css">`   (The help system will make the appropriate changes to the path at run time.)   |

### Graphics

| Guideline | Example |
| --- | --- |
| There are two types of graphics in the online help at present: icons and screen captures.  Icons are system-independent, so there are no reasons to give them class names or to be concerned about their file type. For icons the ".svg" version of the icons (that are used to generate the ".png" files used in the user interface) should directly be used. This ensures that they are rendered at the exact screen resolution and therefor appear super crisp also on high resolution displays. <br><br>  On the other hand, screen captures are system-dependent, so it is important that different systems can display the appropriate screen captures. To do this, vendors need to be able to create a zip file of screen captures that the help system can display properly and automatically: <br><br> *   Screen captures must be ".png" files. <br>*   Image declarations must **_not_** specify image sizes.<br> *   Image declarations should have "alt" attributes (this is an accessibility feature). <br>*   Image declarations may have a <title> attribute. <br>*   Screen captures should be taken at the minimum supported resolution of 1152x864. <br>*   Images should be no more than 650 pixels wide. <br><br> Here are some general guidelines on taking screen shots:  <br>*   Ensure consistent look within a document: if a document has most of its screenshots done under a certain OS then additional screenshots must also be made under that OS. Ask a friend to capture them for you if it's hard for yourself. <br>*   Use a default theme and not your custom colors and fonts. <br>*   Trim the screenshot to only show the relevant information. <br> Here is how to setup Windows XP for taking screen shots:  <br>*   Display Properties > Themes > Theme: Windows XP <br>*   Display Properties > Appearance > Windows and Buttons: Windows XP Style <br>*   Color Scheme: Default (blue) <br>*   Font Size: Normal <br>*   Display Properties > Appearance > Effects > Use the following method to smooth edges of screen fonts: Off (Not checked) <br><br> Here is how to setup Windows 7 for taking screen shots:<br>  *   Control Panel > System > Advanced system settings > Advanced > (Performance) Settings > Visual Effects > Select 'Custom' and uncheck 'Smooth edges of screen fonts'   | `<img border="0" src="../images/Image201_fillet" alt="Define a New File Type dialog">` |
| Screen captures should generally be PNG-8 (8 bit color) to keep image sizes small. If a particular screen shot uses enough color to warrant additional color depth, use PNG-24. Use your judgement. Start with PNG-8, and if it looks bad then use PNG-24 instead. |  |

### Indexing

| Guideline | Example |
| --- | --- |
| Eclipse indexes its help files by looking at the text in the page and weighting titles over paragraphs. However, there can be times when you want the index to show text that does not display on the page. For example, if Eclipse uses its own terminology for a function, you might want users to be able to look up synonyms.  To have the Eclipse index generate a hit for "autosave" (a function that Eclipse has under another name) add a meta "keywords" tag before the </head> tag.   |                    `<meta name="keywords" content="autosave">`           |


