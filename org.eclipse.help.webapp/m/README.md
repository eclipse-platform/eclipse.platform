# Modernized help UI

This prototype is based on HTML 5 with an `iframe` for the content and, unlike
the legacy UI, does not use HTML 4 `frameset`s. Technically, it sits on top of
the legacy UI. For example, when doing a full search, the HTML page of the
legacy UI will be parsed and rendered in the web browser (widget) using
JavaScript. This has disadvantages, but it has made it easier to develop this
prototype as a plugin without the need to patch Eclipse.

Once the new UI no longer requires anything from the legacy UI and is no longer
lagging behind in terms of internationalization and accessibility, the legacy UI
can be deprecated and removed: see [_Further development_](#further-development)
below.

Improvements compared to the legacy UI:
* Responsive, e.g. by automatically hiding the TOC bar on small screens
* Search as you type and auto completion of search terms
* Topic preview when hovering over instant search results
* Search scopes drop-down in the search field to search in all books, in the
  current book, in the current chapter or in a user-defined scope
* Full search results shown as a page instead of as side bar
* Full search results can be filtered by books
* Infocenter: UI designed aware of mobile devices
* Infocenter: URL contains topic or search, so a deep link can be bookmarked or
  shared in the usual way


## Activation

This prototype can be activated with the following Java property:

    -Dorg.eclipse.help.webapp.experimental.ui=true

To test, modify  and/or customize this prototype, the files in this folder can
be put in a separate plugin (in `index.html` the links to the JavaScript and
CSS file have to be changed from `m/...` to something like
`rtopic/com.example.my_plugin/...`) and activated as follows (assuming
the plugin has the symbolic name `com.example.my_plugin`; using the raw
topic help link `rtopic/<plugin>/<optional-path>/<file>`):

     -Dorg.eclipse.help.webapp.experimental.ui=<plugin>/<optional-path>/<file>

For example, when the plugin has the symbolic name `com.example.my_plugin` with
`index.html` in the folder `customized-help`:

     -Dorg.eclipse.help.webapp.experimental.ui=com.example.my_plugin/customized-help/index.html

The `m/index.js` JavaScript file can be customized in the same way:

     -Dorg.eclipse.help.webapp.experimental.ui.js=<plugin>/<optional-path>/<file>

The customized HTML and JavaScript file can contain placeholders (see
[`org.eclipse.help.internal.webapp.HelpUi.resolve(String, HttpServletRequest)`](../src/org/eclipse/help/internal/webapp/HelpUi.java)).


## Further development

Unsorted list of things to improve and where the legacy UI is currently used:
* Create the page dynamically to avoid a callback to determine the mode, whether
Infocenter or Eclipse embedded help (the latter with previous/next navigation
buttons and  bookmark support) and to determine the initial content page.
    * [`/advanced/tabs.jsp`](http://127.0.0.1:49999/help/advanced/tabs.jsp)
    * [`/advanced/content.jsp`](http://127.0.0.1:49999/help/advanced/content.jsp)
* Return search results directly as HTML page instead of parsing and re-rendering legacy HTML page
    * [`/advanced/searchView.jsp`](http://127.0.0.1:49999/help/advanced/searchView.jsp?showSearchCategories=false&searchWord=test&maxHits=500)
* Print chapter
    * [`/advanced/print.jsp`](127.0.0.1:49999/help/advanced/print.jsp?topic=/../nav/0)
* Rest API to get the following as JSON instead of requesting and parsing the legacy UI
    * Search as you type and search term completion: currently, when typing `fo`, a search is executed for `fo*` (which means starts with `fo`), but this disables stemming like in the full search (which means when entering `logging`, pages containing `log` or `logs` are not found; ideally _starts with_ and stemming should be combined; the search term completion proposals are currently computed from the words contained in the results, for which there are better ways to do this on the server side
        * [`/advanced/searchView.jsp`](http://127.0.0.1:49999/help/advanced/searchView.jsp?showSearchCategories=false&searchWord=test*&maxHits=7)
    * User-defined search scopes
        * [`/advanced/workingSetManager.jsp`](http://127.0.0.1:49999/help/advanced/workingSetManager.jsp) - list of scopes
        * [`/advanced/workingSetState.jsp`](http://127.0.0.1:49999/help/advanced/workingSetState.jsp?operation=add&workingSet=example_scope) - add, edit or remove a scope
        * [`/scopeState.jsp`](http://127.0.0.1:49999/scopeState.jsp?workingSet=) - set or unset scope
    * Bookmarks
        * [`/advanced/bookmarksView.jsp`](127.0.0.1:49999/help/advanced/print.jsp?topic=/../nav/0)
    * Storing UI settings: TOC side bar width and show/hidden, search results filter tree expanded or collapsed, etc.
* Things to improve (where this prototype currently falls behind the legacy UI):
    * Right-to-left (RTL) support
    * Accessibility (HTML5 ARIA, keyboard support, color contrast, etc.)
    * Dark theme support
    * User-defined search scope:
        * Prevent to create an empty scope
        * Activate search scope on creation
    * Printing via shortcut (Ctrl+P) should print the content
      only, even when the focus is outside the content (e.g. in
      the search field)
    * Bookmarks: adding a bookmark should be more intuitive
    * Customization, e.g. to be able to specify an Infocenter
      (header/banner, footer, etc.): support of
      [legacy options](https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/guide/ua_help_setup_preferences.htm)
    * ...
* By default redirect via `.../index.jsp#topic/...` instead of via `.../index.jsp?topic=...`, but still support such legacy links
* ...


## Design decisions and debugging hints

To use a web browser for debugging, specify a fixed port for the help server,
e.g. `-Dserver_port=49999`, in Eclipse open the help window
(_Help > Help Contents_) and in the web browser open one or more of the
following pages:
* [Modernized UI: `http://127.0.0.1:49999/help/index.jsp`](http://127.0.0.1:49999/help/index.jsp)
* [Legacy UI: `http://127.0.0.1:49999/help/index.jsp?legacy`](http://127.0.0.1:49999/help/index.jsp?legacy)


### Drop of _Index_ tab

The _Index_ tab of the legacy UI was dropped in favor of a simpler UI.


### Browser support

On the one hand state of the art should be used, on the other hand as many
browsers as possible should be supported.

&#8594; Support browsers that support Flexbox ([98.74%](https://caniuse.com/#feat=flexbox)):
Chrome 21, Internet Explorer 10, Firefox 22, Android Browser 21, etc. and higher

See:
* [Browser support](https://caniuse.com/)
* Tutorial/specification: [CSS](https://www.w3schools.com/csS/default.asp),
  [JavaScript](https://www.w3schools.com/js/default.asp)
* JavaScript minifiers:
  * https://javascript-minifier.com/
  * https://javascriptminifier.com/


### General layout (CSS): [`Flexbox`](https://www.w3schools.com/csS/css3_flexbox.asp) ([tutorial](https://css-tricks.com/snippets/css/a-guide-to-flexbox/), [98.74%](https://caniuse.com/#feat=flexbox))

* Instead of `float`, layout via tables (both are deprecated for that) and `gridx` (since it is too new and not yet widely supported)
* If needed, consider add fallback for IE 6-9 (see [Flexbox Fallbacks](http://maddesigns.de/flexbox-fallbacks-2670.html))


### Navigation and deep linking

Going back in the browser history can cause issues in combination with deep linking, since the navigation is done in
the `iframe` except for the search page which is not shown in the `iframe`.
The problem is that when going back to a search page, the search might need to be submitted again and for this the
query must be known.

Ways that don't work:

* Deep link containing query as hash of the top window (`...#q=...`) set via
  [`history.pushState(...)`](https://developer.mozilla.org/en-US/docs/Web/API/History/pushState):
  top window hash might not be restored when navigation happens also in the content `iframe`
* Query as hash or as query of the content `iframe` set via
  [`history.pushState(...)`](https://developer.mozilla.org/en-US/docs/Web/API/History/pushState):
  conflicts with existing hashes/queries of content pages and does not work with external content pages
* [Data URL](https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/Data_URIs) used in content `iframe`
  containing the query: not allowed in Internet Explorer for security reasons

Chosen solution:

* For full search set content `iframe` instead of doing a remote request and get
  result from live DOM of the `iframe` when loaded


### Search

* Without [interim results](https://github.com/howlger/Eclipse-Help-Modernized/blob/541481f486008f665244446052d2a7e6d147223c/de.agilantis.help_ui_modernized/index.js#L482-L513) displayed [semi-transparent](https://github.com/howlger/Eclipse-Help-Modernized/blob/541481f486008f665244446052d2a7e6d147223c/de.agilantis.help_ui_modernized/index.js#L607) since this is only helpful in rare cases (very slow responds and previous cached query containing hits of current query)

To simulate a slow response replace `return function(data) {` with

```
                return function(data) {
setTimeout(function(data) { return function() {processData(data)}}(data), 1000);
};
function processData(data) {
```


## Issues caused by `<iframe>`

To catch mouse and click events (for slider and drop-down menues) add an overlay element covering the whole page (see `createOverlay()`).

Debug overlay by adding the following line after the line `overlayStyle.width = '100%';`:

```
overlayStyle.background = 'rgba(200, 100, 100, .2)';
```
