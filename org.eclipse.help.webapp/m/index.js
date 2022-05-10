/*******************************************************************************
 * Copyright (c) 2020 Holger Voormann and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
(function(window, document) {
    var SMALL_SCREEN_WIDTH = 768;
    var LOGO_ICON_WIDTH = 36;
    var LOGO_FULL_WIDTH = 146;
    var MENU_FONT_SIZING = 0;
    var MENU_HELP = 0; // 0 or e.g. 'topic/org.eclipse.help.base/doc/help_home.html'
    var MENU_HELP_LABEL = '<%js:menu_item_help_label%>';
    var MENU_HELP_DESCRIPTION = '<%js:menu_item_help_description%>';
    var MENU_ABOUT = 0;
    var TOC_SIDEBAR_DEFAULT_WIDTH = 380;
    var TOC_SIDEBAR_MINIMUM_WIDTH = 76;
    var TOC_SIDEBAR_WIDTH_COOKIE_NAME = 'toc_width';
    var TOC_ICON = '<svg width="20" height="20" viewBox="0 0 20 20"><path fill="currentColor" d="M19 5H1V3h18v2zm0 10H1v2h18v-2zm-4-6H1v2h14V9z"/></svg>';
    var HISTORY_BACK_ICON = '<svg width="20" height="20" viewBox="0 0 20 20"><path fill="currentColor" d="m 8.27224,17.95644 c 0.39048,0.390343 1.023592,0.390211 1.413938,0 0.390613,-0.390612 0.390613,-1.023597 -1.33e-4,-1.41421 L 4.144057,11.000499 18.27041,10.999748 c 0.55219,-1.31e-4 0.999731,-0.447671 0.999731,-1.0001299 -1.34e-4,-0.552189 -0.447674,-0.999595 -0.999864,-0.999595 l -14.126755,9.99e-4 5.542723,-5.543204 c 0.390479,-0.390479 0.390479,-1.023727 0,-1.414074 -0.195307,-0.195173 -0.451138,-0.292892 -0.707102,-0.292892 -0.255832,0 -0.511664,0.09772 -0.70697,0.292759 l -7.249421,7.250043 c -0.187575,0.18744 -0.292893,0.441666 -0.292893,0.7069649 1.33e-4,0.2653 0.105451,0.519396 0.293025,0.707237 z"/></svg>';
    var HISTORY_FORWARD_ICON = '<svg width="20" height="20" viewBox="0 0 20 20"><path fill="currentColor" d="m 11.72776,17.95644 c -0.39048,0.390343 -1.023592,0.390211 -1.413938,0 -0.390613,-0.390612 -0.390613,-1.023597 1.33e-4,-1.41421 L 15.855943,11.000499 1.72959,10.999748 C 1.1774,10.999617 0.729859,10.552077 0.729859,9.9996181 0.729993,9.4474291 1.177533,9.0000231 1.729723,9.0000231 l 14.126755,9.99e-4 -5.542723,-5.543204 c -0.390479,-0.390479 -0.390479,-1.023727 0,-1.414074 0.195307,-0.195173 0.451138,-0.292892 0.707102,-0.292892 0.255832,0 0.511664,0.09772 0.70697,0.292759 l 7.249421,7.250043 c 0.187575,0.18744 0.292893,0.441666 0.292893,0.7069649 -1.33e-4,0.2653 -0.105451,0.519396 -0.293025,0.707237 z"/></svg>';
    var BOOKMARKS_ICON = '<svg width="20" height="20" viewBox="0 0 20 20"><path fill="currentColor" d="M 10.019531 0.5 A 1.000065 1.000065 0 0 0 9.1035156 1.0566406 L 6.5742188 6.1816406 L 0.91796875 7.0039062 A 1.000065 1.000065 0 0 0 0.36523438 8.7089844 L 4.4570312 12.699219 L 3.4902344 18.332031 A 1.000065 1.000065 0 0 0 4.9414062 19.384766 L 10 16.726562 L 15.058594 19.384766 A 1.000065 1.000065 0 0 0 16.509766 18.332031 L 15.542969 12.699219 L 19.634766 8.7089844 A 1.000065 1.000065 0 0 0 19.082031 7.0039062 L 13.425781 6.1816406 L 10.896484 1.0566406 A 1.000065 1.000065 0 0 0 10.019531 0.5 z M 10 3.7597656 L 11.865234 7.5390625 A 1.000065 1.000065 0 0 0 12.617188 8.0859375 L 16.789062 8.6914062 L 13.771484 11.632812 A 1.000065 1.000065 0 0 0 13.482422 12.517578 L 14.195312 16.673828 L 10.464844 14.710938 A 1.000065 1.000065 0 0 0 9.5351562 14.710938 L 5.8046875 16.673828 L 6.5175781 12.517578 A 1.000065 1.000065 0 0 0 6.2285156 11.632812 L 3.2109375 8.6914062 L 7.3828125 8.0859375 A 1.000065 1.000065 0 0 0 8.1347656 7.5390625 L 10 3.7597656 z"/></svg>';
    var BOOKMARKS_PATTERN = new RegExp('<tr[^<]*<td[^<]*<a\\s+(?:(?!href)[\\w\\-]+\\s*=\\s*(?:(?:\'[^\']*\')|(?:"[^"]*"))\\s+)*href\\s*=\\s*\'([^\']*)\'[^<]*<img[^>]*>\\s*([^<]*)</a>', 'gi');
    var MENU_ICON = '<svg width="20" height="20" viewBox="0 0 20 20"><path fill="currentColor" d="M 10 1.5 A 2 2 0 0 0 8 3.5 A 2 2 0 0 0 10 5.5 A 2 2 0 0 0 12 3.5 A 2 2 0 0 0 10 1.5 z M 10 8 A 2 2 0 0 0 8 10 A 2 2 0 0 0 10 12 A 2 2 0 0 0 12 10 A 2 2 0 0 0 10 8 z M 10 14.5 A 2 2 0 0 0 8 16.5 A 2 2 0 0 0 10 18.5 A 2 2 0 0 0 12 16.5 A 2 2 0 0 0 10 14.5 z"/></svg>';
    var MENU_CLOSE_ICON = '<svg width="20" height="20" viewBox="0 0 20 20"><path fill="currentColor" d="M 4.34375 2.9296875 L 2.9296875 4.34375 L 8.5859375 10 L 2.9296875 15.65625 L 4.34375 17.070312 L 10 11.414062 L 15.65625 17.070312 L 17.070312 15.65625 L 11.414062 10 L 17.070312 4.34375 L 15.65625 2.9296875 L 10 8.5859375 L 4.34375 2.9296875 z"/></svg>';
    var TREE_HANDLE = '<svg width="24" height="24" viewBox="0 0 24 24" focusable="false" role="presentation">-<path d="M10.294 9.698a.988.988 0 0 1 0-1.407 1.01 1.01 0 0 1 1.419 0l2.965 2.94a1.09 1.09 0 0 1 0 1.548l-2.955 2.93a1.01 1.01 0 0 1-1.42 0 .988.988 0 0 1 0-1.407l2.318-2.297-2.327-2.307z" fill="currentColor"/></svg>';
    var BOOK_NAME_SHORTENER = function shortenBookName(bookName) { return bookName.replace(/\s+(Documentation\s*)?(\-\s+([0-9,\-]+\s+)?Preview(\s+[0-9,\-]+)?\s*)?$/i, ''); };
    var BOOK_SCOPE_BY_DEFAULT = 0;
    var BOOK_SCOPE_COOKIE = 'book-scope';
    var SEARCH_ICON = '<svg width="20" height="20" viewBox="0 0 20 20"><g fill="#fff"><path fill="currentColor" d="M 7.5 0 C 3.3578644 0 0 3.3578644 0 7.5 C 0 11.642136 3.3578644 15 7.5 15 C 8.8853834 14.997 10.242857 14.610283 11.421875 13.882812 L 17.185547 19.662109 C 17.632478 20.113489 18.36112 20.112183 18.8125 19.660156 L 19.623047 18.845703 C 20.072507 18.398153 20.072507 17.665594 19.623047 17.214844 L 13.871094 11.447266 C 14.607206 10.26212 14.998156 8.8951443 15 7.5 C 15 3.3578644 11.642136 0 7.5 0 z M 7.5 2 A 5.5 5.5 0 0 1 13 7.5 A 5.5 5.5 0 0 1 7.5 13 A 5.5 5.5 0 0 1 2 7.5 A 5.5 5.5 0 0 1 7.5 2 z"/></g></svg>';
    var BASE_URL;
    var SEARCH_BASE_URL;
    var SEARCH_HITS_MAX = 500;
    var SEARCH_AS_YOU_TYPE_PROPOSAL_MAX = 7;
    var SEARCH_RESULTS_INDEXING_PATTERN = new RegExp('[\'"]divProgress[\'"]\\s+STYLE\\s*=\\s*[\'"]\\s*width\\s*:\\s*([\\d]+)\\s*px', 'i');
    var SEARCH_RESULTS_PATTERN = new RegExp('<tr[^<]*<td[^<]*<img[^<]*</td[^<]*<td[^<]*<a\\s+(?:(?!href)(?!title)[\\w\\-]+\\s*=\\s*(?:(?:\'[^\']*\')|(?:"[^"]*"))\\s+)*(href|title)\\s*=\\s*"([^"]*)"\\s+(?:(?!href)(?!title)[\\w\\-]+\\s*=\\s*(?:(?:\'[^\']*\')|(?:"[^"]*"))\\s+)*(href|title)\\s*=\\s*"([^"]*)"[^>]*>([^<]*)</a>(?:(?:(?!<[/]?tr)[\\s\\S])*</tr\\s*>\\s*<tr(?:(?!</tr)(?!class="location">)[\\s\\S])*class="location">((?:(?!</div)[\\s\\S])*))?(?:(?:(?!</tr)(?!\\sclass=["\']description["\'])[\\s\\S])*</tr){1,2}(?:(?!</tr)(?!\\sclass=["\']description["\'])[\\s\\S])*\\sclass=["\']description["\'][^>]*>([^<]*)', 'gi');
    var SEARCH_RESULTS_BREADCRUMB_SNIPPET_PATTERN = new RegExp('<a\\s+href="([^"]+)">([^<]+)</a>', 'gi');
    var SEARCH_SCOPE_CURRENT_PATTERN = new RegExp('<div\\s+id\\s*=\\s*"scope"\\s*>([^<]*)<');
    var SEARCH_SCOPE_ALL_PATTERN = new RegExp('<a\\s+(?:(?!title)[\\w\\-]+\\s*=\\s*(?:(?:\'[^\']*\')|(?:"[^"]*"))\\s+)*title\\s*=\\s*"([^"]*)"', 'gi');
    var SEARCH_SCOPE_NAME_PATTERN = new RegExp('<input\\s+type\\s*=\\s*["\']text["\']\\s+(?:(?!value)[\\w\\-]+\\s*=\\s*(?:(?:\'[^\']*\')|(?:"[^"]*"))\\s+)*value\\s*=\\s*\'([^\']*)\'', 'i');
    var SEARCH_SCOPE_IS_NEW_PATTERN = new RegExp('oldName\\s*=\\s*\'\'', 'i');
    var SEARCH_SCOPE_HREFS_PATTERN = new RegExp('<input(?:\\s+(?!checked)[\\w\\-]+\\s*=\\s*(?:(?:\'[^\']*\')|(?:"[^"]*")))*(\\s+checked)?[^<]+<label\\s+for\\s*=\\s*"([^"]*)"[^>]*>([^<]*)</label>\\s*(</div)?', 'gi');
    var SEARCH_AS_YOU_TYPE_CACHE_SIZE = 7;
    var SEARCH_FULL_SEARCH_CACHE_SIZE = 3;
    var SEARCH_DELAY_IN_MILLISECOND = 99;
    var SEARCH_CACHE = {fi: -1, f: [], ti: -1, t: [] };

    // TODO integration: the browser should not have to calculate the following variables itself;
    //                   only "init()" should be called instead
    var embeddedMode = 1;
    var title = '<%js:Help%>';
    var bookmarksPage;
    var scopesPage;
    var searchPage;
    var renderFullSearch;
    var updateScopeByToc;
    var setSearchScope;
    var setSearchFieldValue;
    var currentSearch = {};
    var searchScope = {l: 0, s: '', t: 0};

    addEvent(window, 'load', function() {

        // (search) base URL
        var a = createElement(0, 'a');
        a.href = window.location.pathname.indexOf('/index.jsp') >= 0 ? 'x' : '../../x';
        BASE_URL = a.href.substring(0, a.href.length - 1);
        SEARCH_BASE_URL = BASE_URL + 'advanced/searchView.jsp?showSearchCategories=false&searchWord=';

        remoteRequest(BASE_URL + 'advanced/search.jsp', function(responseText) {

            // read user defined search scope
            var match = SEARCH_SCOPE_CURRENT_PATTERN.exec(responseText);
            searchScope.i = match ? match[1] : match;

            // make sure no scope is set (a scope might have been set via the legacy UI before)
            remoteRequest(BASE_URL + 'scopeState.jsp?workingSet=');

            // title
            remoteRequest(BASE_URL + 'index.jsp?legacy', function(responseText) {
                var match = new RegExp('<title>([^<]*)</title>').exec(responseText);
                if (!match) return;
                title = decodeHtml(match[1]);
                document.title = title;
            });

            // embedded or Infocenter mode? + read scopes
            remoteRequest(BASE_URL + 'advanced/tabs.jsp', function(responseText) {
                if (responseText.indexOf('e_bookmarks_view.') < 0) embeddedMode = 0;

                // read scopes
                remoteRequest(BASE_URL + 'advanced/workingSetManager.jsp?t=' + Date.now(), function(responseText) {
                    var scopeIndex = 0;
                    SEARCH_SCOPE_ALL_PATTERN.lastIndex = 0;
                    for (var match; (match = SEARCH_SCOPE_ALL_PATTERN.exec(responseText)) != null;) {
                         var scopeName = decodeHtml(match[1]);
                         if (scopeName.substring(0, 1) == '\u200B') {
                             if (scopeName.length < 3) {
                                 searchScope.l = scopeName.length;
                                 break;
                             } else {
                                 searchScope.l = 4;
                                 searchScope.t = scopeName.length - 3;
                                 continue;
                             }
                         }
                         if (searchScope.i && searchScope.i == scopeName) {
                             searchScope.l = 4;
                             searchScope.s = scopeName;
                             break;
                         } else if (searchScope.l == 4 && searchScope.t == scopeIndex) {
                             searchScope.s = scopeName;
                         }
                         scopeIndex++;
                    }

                    init();

                });
            });

        });

    });

    function init() {

        // bookmarks page
        bookmarksPage = createElement(getElementById('m'), 0, 'c');
        bookmarksPage.id = 'b';
        bookmarksPage.s = function(show) {
            bookmarksPage.o = !!show;
            if (show && scopesPage) scopesPage.s();
            bookmarksPage.style.display = show ? 'block' : 'none';
            getElementById('c').style.display = show || (searchPage && searchPage.o) ? 'none' : 'block';
            if (searchPage) searchPage.style.display = searchPage.o && !show ? 'block' : 'none';
            if (!show) return;
            bookmarksPage.innerHTML = '';

            // add bookmark
            try {
                var url = frames.c.location.href;
                var title = searchPage.o ? searchPage.l : frames.c.document.title;
                if (title == null || title == '') {
                    title = url;
                }
                createElement(bookmarksPage, 'span', 'g', '<%js:bookmarks_add_field_label%>');
                var addArea = createElement(bookmarksPage, 'span', 'a');
                var input = createElement(addArea, 'input');
                input.type = 'text';
                input.autocomplete = 'off';
                input.value = title;
                var addButtonTooltip = searchPage.o ? '<%js:bookmarks_add_button_description_search%>' : '<%js:bookmarks_add_button_description_page%>';
                createButton(addArea, BOOKMARKS_ICON, addButtonTooltip, function() {
                    try {
                        var url = frames.c.location.href;
                        if (url.substring(0, BASE_URL.length + 6) == BASE_URL + 'topic/') {
                            url = url.substring(BASE_URL.length + 5);

                        // workaround for a bug of the legacy UI: non-topic links (e.g. ".../nav/..." instead of
                        // ".../topic/...") are stored absolutely instead of relatively (which doesn't work, because by
                        // default a random port number is chosen when restarting Eclipse)
                        } else if (url.substring(0, BASE_URL.length) == BASE_URL) {
                            url = '/../' + url.substring(BASE_URL.length);
                        }

                        var title = frames.c.document.title;
                        if (title == null || title == '') {
                            title = url;
                        }
                        remoteRequest(  BASE_URL + 'advanced/bookmarksView.jsp?operation=add&'
                                      + 'bookmark=' + encodeURIComponent(url)
                                      + '&title=' + encodeURIComponent(input.value)
                                      + '&t=' + Date.now());
                        bookmarksPage.s();
                     } catch (e) {}
                });
            } catch (e) {}

            // "x" button
            setClassName(createButton(bookmarksPage, MENU_CLOSE_ICON, '<%js:bookmarks_button_description_close%>', function() {
                bookmarksPage.s();
            }), 'b bc');

            remoteRequest(BASE_URL + 'advanced/bookmarksView.jsp?t=' + Date.now(), function(responseText) {
                var ol;
                var element = createElement();
                var deleteButtons = [];
                BOOKMARKS_PATTERN.lastIndex = 0;
                for (var match; (match = BOOKMARKS_PATTERN.exec(responseText)) != null;) {
                    if (!ol) {
                        createElement(bookmarksPage, 0, 'g', '<%js:bookmarks_heading%>');
                        ol = createElement(bookmarksPage, 'ol');
                    }
                    var groups = [];
                    for (var i = 1; i < 4; i++) {
                        element.innerHTML = match[i];
                        groups.push((element.textContent ? element.textContent : element.innerText).replace(/^\s+|\s+$/g,'').replace(/\s+/g,' '));
                    }
                    var li = createElement(ol, 'li');
                    var href = groups[0].substring(0, 3) == '../' ? BASE_URL + '/' + groups[0] : groups[0];
                    var deleteButton = createButton(li,
                                                    '<%js:bookmarks_delete_button_label%>',
                                                    '<%js:bookmarks_delete_button_description%>',
                                                    function(href, title, li) {
                        return function() {
                            remoteRequest(  BASE_URL + 'advanced/bookmarksView.jsp?operation=remove&'
                                          + 'bookmark=' + encodeURIComponent(href)
                                          + '&title=' + encodeURIComponent(title)
                                          + '&t=' + Date.now());
                            li.style.display = 'none';
                        }
                    }(groups[0].substring(0, 9) == '../topic/' ? groups[0].substring(8) : groups[0], groups[1], li));
                    deleteButton.style.display = 'none';
                    deleteButton.className = 'b br';
                    deleteButtons.push(deleteButton);
                    var a = createElement(li, 'a');
                    a.target = 'c';
                    a.href = href;
                    createElement(a, 'span').innerHTML = BOOKMARKS_ICON;
                    createElement(a, 'span', 0, groups[1]);
                }
                if (deleteButtons.length) {
                    var editButton = createButton(bookmarksPage, '<%js:bookmarks_edit_button_label%>', '<%js:bookmarks_edit_button_description%>', function() {
                        var editMode = editButton.innerHTML == decodeHtml('<%js:bookmarks_edit_button_label%>');
                        editButton.innerHTML = decodeHtml(editMode ? '<%js:bookmarks_delete_all_button_label%>' : '<%js:bookmarks_edit_button_label%>');
                        editButton.title = editMode ? '<%js:bookmarks_delete_all_button_description%>' : '<%js:bookmarks_edit_button_description%>';
                        setClassName(editButton, editMode ? 'b br' : 'b');
                        for (var i = 0; i < deleteButtons.length; i++) {
                            deleteButtons[i].style.display = 'inline-block';
                        }
                        if (!editMode) {
                            remoteRequest(BASE_URL + 'advanced/bookmarksView.jsp?operation=removeAll&t=' + Date.now());
                            bookmarksPage.s();
                        }
                    });
                }
                createButton(bookmarksPage, '<%js:bookmarks_button_label_close%>', '<%js:bookmarks_button_description_close%>', function() { bookmarksPage.s(); });

            });
        }
        bookmarksPage.s();

        // scopes page
        scopesPage = createElement(getElementById('m'), 0, 'c');
        scopesPage.id = 'o';
        scopesPage.s = function(show) {
            scopesPage.o = !!show;
            if (show && bookmarksPage) bookmarksPage.s();
            scopesPage.style.display = show ? 'block' : 'none';
            getElementById('c').style.display = show || (searchPage && searchPage.o) ? 'none' : 'block';
            if (searchPage) searchPage.style.display = searchPage.o && !show ? 'block' : 'none';
            if (!show) return;
            function showScopesPage(query, scopeNr) {
                scopesPage.innerHTML = '';

                // "x" button
                setClassName(createButton(scopesPage, MENU_CLOSE_ICON, '<%js:scopes_close_button_description%>', function() {
                    scopesPage.s();
                }), 'b bc');

                // get and show content
                remoteRequest(  BASE_URL
                              + 'advanced/workingSet'
                              + (query ? '.jsp?' + query + '&' : 'Manager.jsp?')
                              + 't=' + Date.now(),
                              function(responseText) {
                    var match = SEARCH_SCOPE_NAME_PATTERN.exec(responseText);
                    if (match) {
                        createElement(scopesPage, 'span', 'g', '<%js:scopes_scope_field_label%>');
                        var scopeName = decodeHtml(match[1]);
                        var nameInput = createElement(createElement(scopesPage, 'span', 'a'), 'input');
                        nameInput.type = 'text';
                        nameInput.value = scopeName;
                        var tree = [];
                        var allNodes = [];
                        var parentNode;
                        SEARCH_SCOPE_HREFS_PATTERN.lastIndex = 0;
                        for (; (match = SEARCH_SCOPE_HREFS_PATTERN.exec(responseText)) != null;) {
                            var node = {n: {l: decodeHtml(match[3]), v: decodeHtml(match[2]), c: [], x: !!match[1]}, l: 1};
                            allNodes.push(node.n);
                            if (match[4] && parentNode) {
                                parentNode.l = 0;
                                parentNode.n.c.push(node);
                                node.n.p = parentNode.n;
                            } else {
                                parentNode = node;
                                tree.push(node);
                            }
                        }
                        createTree(scopesPage,

                            // content provider
                            function(node, processChildrenFn) {
                                processChildrenFn(node ? node.c : tree);
                            },

                            // label provider
                            function(li, node) {
                                var checkboxWithLabel = createElement(li);
                                var checkbox = createElement(checkboxWithLabel, 'input');
                                checkbox.type = 'checkbox';
                                checkbox.id = node.v;
                                if (node.x) {
                                    checkbox.checked = 'checked';
                                }
                                node.b = checkbox;
                                checkbox.n = node;
                                updateOrGetScopeCheckboxStates(node);
                                if (updateOrGetScopeCheckboxStates(node, 1)) {
                                    setClassName(li, 'open');
                                }
                                addEvent(checkbox, 'click', (function(node) {
                                    return function() {
                                        updateOrGetScopeCheckboxStates(node.p);
                                        setSubtreeState(node, node.b.checked);
                                    };
                                })(node));
                                var label = createElement(checkboxWithLabel, 'label', 0, node.l);
                                setAttribute(label, 'for', node.v);
                                return checkboxWithLabel;
                            }

                        );
                        function getHrefs() {
                            hrefs = '';
                            for (var i = 0; i < allNodes.length; i++) {
                                var node = allNodes[i];
                                if (   (node.b ? node.b.checked : node.x)
                                    && (!node.p || !(node.p.b ? node.p.b.checked : node.p.x))) {
                                    hrefs += '&hrefs=' + node.v;
                                }
                            }
                            return hrefs;
                        }
                        if (SEARCH_SCOPE_IS_NEW_PATTERN.exec(responseText)) {
                            createButton(scopesPage, '<%js:scopes_scope_create_button_label%>', '<%js:scopes_scope_create_button_description%>', function() {
                                doScopesOperation(  'add&oldName=&workingSet='
                                                  + encodeURIComponent(nameInput.value)
                                                  + getHrefs());
                            });
                        } else {
                            var deleteButton = createButton(scopesPage, '<%js:scopes_scope_delete_button_label%>', '<%js:scopes_scope_delete_button_description%>', function() {
                                doScopesOperation('remove&workingSet=' + encodeURIComponent(scopeName));
                            });
                            deleteButton.className = 'b br';
                            createButton(scopesPage, '<%js:scopes_scope_apply_button_label%>', '<%js:scopes_scope_apply_button_description%>', function() {
                                doScopesOperation(  'edit&oldName='
                                                  + encodeURIComponent(scopeName)
                                                  + '&workingSet='
                                                  + encodeURIComponent(nameInput.value)
                                                  + getHrefs());
                                setSearchScope([4, scopeName, scopeNr]);
                            });
                        }
                        createButton(scopesPage, '<%js:scopes_scope_cancel_button_label%>', '<%js:scopes_scope_cancel_button_description%>', function() { showScopesPage(); });

                    } else {
                        createElement(scopesPage, 0, 'g', '<%js:scopes_heading%>');
                        var ol = createElement(scopesPage, 'ol');
                        SEARCH_SCOPE_ALL_PATTERN.lastIndex = 0;
                        for (var scopeIndex = 0; (match = SEARCH_SCOPE_ALL_PATTERN.exec(responseText)) != null;) {
                            var scopeName = decodeHtml(match[1]);
                            if (scopeName.substring(0, 1) == '\u200B') continue;
                            var li = createElement(ol, 'li');
                            createButton(li, scopeName, 0, function(scopeName, scopeIndex) {
                                return function() { showScopesPage('operation=edit&workingSet=' + encodeURIComponent(scopeName), scopeIndex); };
                            }(scopeName, scopeIndex), 'ba');
                            scopeIndex++;
                        }
                        createButton(scopesPage, '<%js:scopes_new_button_label%>', '<%js:scopes_new_button_description%>', function() { showScopesPage('operation=add'); });
                        createButton(scopesPage, '<%js:scopes_close_button_label%>', '<%js:scopes_close_button_description%>', function() { scopesPage.s(); });
                    }
                });
            }
            function doScopesOperation(query) {
                remoteRequest(  BASE_URL
                              + 'workingSetState.jsp?operation='
                              + query
                              + '&t=' + Date.now(),
                              function() { showScopesPage(); });
            }
            showScopesPage();
        }
        function updateOrGetScopeCheckboxStates(node, computeState) {
            for (var parent = node; parent; parent = parent.p) {
                var allChecked = 1;
                var allUnchecked = 1;
                for (var i = 0; i < parent.c.length; i++) {
                    var childNode = parent.c[i].n;
                    if (childNode.b ? childNode.b.checked : childNode.x) {
                        allUnchecked = 0;
                    } else {
                        allChecked = 0;
                    }
                }
                if (computeState) {
                    return !allChecked && !allUnchecked;
                }
                if (!parent.c.length) continue;
                parent.b.checked = !!allChecked;
                parent.b.indeterminate = !allChecked && !allUnchecked;
            }
        }
        function setSubtreeState(node, state) {
            for (var i = 0; i < node.c.length; i++) {
                var childNode = node.c[i].n;
                if (childNode.b) {
                    childNode.b.checked = !!state;
                } else {
                    childNode.x = state;
                }
                setSubtreeState(childNode, state);
            }
        }
        scopesPage.s();

        // search page
        searchPage = createElement(getElementById('m'), 0, 'c', '<%js:search_page_loading%>');
        searchPage.id = 'r';
        searchPage.s = function(show) {
            searchPage.o = !!show;
            searchPage.style.display = show ? 'block' : 'none';
            getElementById('c').style.display = show ? 'none' : 'block';
            if (bookmarksPage) bookmarksPage.s();
            if (scopesPage) scopesPage.s();
            if (show) {
                document.title = searchPage.l + ' - ' + title;
            } else {
                try {
                    var topicTitle = contentFrame.contentDocument.title;
                    document.title = topicTitle ? (topicTitle + ' - ' + title) : title;
                } catch(e) {
                    document.title = title;
                }
            }
        }
        searchPage.s();

        // toolbar: TOC sidebar button and history Back/Forward buttons (in embedded help, but not in Infocenter mode)
        var header = getElementById('h');
        var toolbarContainer = createElement(header);
        var toolbar = createElement(toolbarContainer, 0, 'y');
        var tocSidebarToggleButton = createButton(toolbar, TOC_ICON, '<%js:toc_toggle_button_description%>');
        if (embeddedMode) {
            createButton(toolbar, HISTORY_BACK_ICON, '<%js:back_button_description%>', function() {
                window.history.back();
            });
            createButton(toolbar, HISTORY_FORWARD_ICON, '<%js:forward_button_description%>', function() {
                window.history.forward();
            });
        }

        // TOC slider (to change TOC sidebar width by moving the slider)
        var smallScreenAutoCloseFn = createSlider(tocSidebarToggleButton, toolbarContainer, createElement(header, 0, 'i'));

        // fill TOC and create search field
        var toc = getElementById('t');
        createTree(toc,
                   tocContentProvider,
                   function(li, node) {
                       if (node.toc) {
                           li.toc = node.toc;
                       }
                       li.n = node;
                       var a = createElement(li, 'a');
                       a.href = node.h;
                       a.target = 'c';
                       addEvent(a, 'click', smallScreenAutoCloseFn);
                       li.h = a.href;
                       li.a = a.hash;
                       li.b = a.protocol + '//' + a.host + a.pathname;
                       if (node.i) {
                           var iconImg = createElement(a, 'img');
                           iconImg.setAttribute('src', BASE_URL
                                                       + 'advanced/images/'
                                                       + node.i
                                                       + '.svg');
                       }
                       a.appendChild(document.createTextNode(node.t));
                       return a;
                   },
                   1);
        toc.c = 1; // scroll into view if needed: to upper third instead of scroll as less as possible
        var contentFrame = getElementById('c');
        addEvent(contentFrame, 'load', function() {

            // full search?
            var contentFrameHref = frames.c.location.href;
            if (contentFrameHref.substring(0, SEARCH_BASE_URL.length) == SEARCH_BASE_URL) {
                var data = frames.c.document.documentElement.innerHTML;
                renderFullSearch(contentFrameHref.substring(SEARCH_BASE_URL.length), data);
                searchPage.s(1);
                return;
            }

            // close maybe open bookmarks, scopes or search page
            if (bookmarksPage) bookmarksPage.s();
            if (scopesPage) scopesPage.s();
            searchPage.s(); // unhides content frame
            updateDeepLink();

            // search result: scroll to first match (does not work automatically when the content frame is hidden)
            try {
                if (contentFrame.contentDocument.location.href.indexOf('?resultof=') > 0) {
                    var contentFrameWindow = contentFrame.contentWindow;
                    if (contentFrameWindow.pageYOffset == 0) {
                        contentFrameWindow.scrollIntoView(
                            contentFrameWindow.getVerticalScroll(contentFrameWindow.firstNodeToBeHighlighted));
                    }
                }
            } catch(e) {}

            // font sizing
            setFontSize(0, 1, 1);

            // update title and deep link
            try {
                var topicTitle = contentFrame.contentDocument.title;
                document.title = topicTitle ? (topicTitle + ' - ' + title) : title;
            } catch(e) {
                document.title = title;
            }

            // sync with TOC
            try {
                syncToc();
                addEvent(contentFrame.contentWindow, 'hashchange', function() { syncToc(); updateDeepLink(); });
            } catch(e) {
                toc.x(0);
            }

        });
        addEvent(window, 'hashchange', function() {
            var newHash = window.location.hash;
            if (isQueryHash(newHash)) {
                searchFullByHash(newHash);
            } else {
                searchPage.s();
            }
        });

    }
    function createButton(parent, innerHtml, description, clickFn, className, text) {
        var button = createElement(parent, 'button', className ? className : 'b', text);
        if (description) {
            button.title = description;
        }
        if (innerHtml) {
            setInnerHtml(button, innerHtml);
        }
        if (clickFn) {
            addEvent(button, 'click', function(e) { preventDefault(e); clickFn(e); });
        }
        return button;
    }

    function initContentPage() {

        // set initial start/cover page...
        // ...by hash
        var hash = window.location.hash;
        try {
            if (hash && (   'q=' == hash.substring(1, 3)
                         || 'nav/' == hash.substring(1, 5)
                         || 'topic/' == hash.substring(1, 7)
                         || 'rtopic/' == hash.substring(1, 8)
                         || 'ntopic/' == hash.substring(1, 8)
                         || 'nftopic/' == hash.substring(1, 9))) {
                if ('q=' == hash.substring(1, 3)) {
                    searchFullByHash(hash);
                } else {
                    getElementById('c').src = BASE_URL + hash.substring(1);
                }
                return;
            }
        } catch(e) {}

        // ...by legacy query parameters (topic/nav or search link)
        var params = getParams(window.location.href.replace(/^[^#\?]*(?:\?([^#\?]*))?(#.*)?$/, '$1')
                                                   .replace('&resultof%3D', '&r_='));
        var topicOrNav = params.topic || params.nav;
        if (params.searchWord && params.tab == 'search') {
            window.history.replaceState(null, '', window.location.pathname);
            searchFullByHash('#q=' + encodeURIComponent(params.searchWord));
            return;
        }
        if (topicOrNav) {
            getElementById('c').src =   BASE_URL
                                      + (params.nav ? 'nav' : 'topic')
                                      + topicOrNav
                                      + (params.r_ ? '?resultof=' + decodeURIComponent(params.r_) : '')
                                      + (params.anchor ? '#' + params.anchor : '');
            window.history.replaceState(null, '', window.location.pathname);
            updateDeepLink();
            return;
        }

        // ...default start/cover page
        remoteRequest(BASE_URL + 'advanced/content.jsp', function(responseText) {
            var match = new RegExp('name="ContentViewFrame"\\s+title="[^"]+"\\s+src=\'([^\']+)\'', 'i').exec(responseText);
            if (match) {
                getElementById('c').src = BASE_URL + 'topic/' + decodeHtml(match[1]);
                updateDeepLink();
            }
        });

    }

    function syncToc() {
        var newLocation = getElementById('c').contentWindow.location;
        var toc = getElementById('t');
        if (toc.s && toc.s.h == newLocation.href) return;
        var newLocationHrefWithoutQueryAndHash = newLocation.protocol + '//' + newLocation.host + newLocation.pathname;
        var newLocationHash = newLocation.hash;
        var liMatch;
        var liWhithoutHashMatch;
        toc.v(function(li) {
            if (newLocationHrefWithoutQueryAndHash != li.b) return 1;
            if (!newLocationHash || newLocationHash == li.a) {
                liMatch = li;
                return 0;
            }
            if (!liWhithoutHashMatch) {
                liWhithoutHashMatch = li;
            }
            return 1;
        });
        if (liMatch) {
            toc.x(liMatch, toc, 1);
        } else if (liWhithoutHashMatch) {
            toc.x(liWhithoutHashMatch, toc, 1);
        } else {
            toc.y(newLocation.href, toc, 1);
        }
    }

    function updateDeepLink(query) {
        var hash;
        if (query) {
            hash = 'q=' + query;
            var start = query.indexOf('&');
            setSearchFieldValue(decodeURIComponent(start < 0 ? query : query.substring(0, start)));
        } else {
            try {
                var src = getElementById('c').contentDocument.location.href;
                if (BASE_URL != src.substring(0, BASE_URL.length)) return;
                var current = src.substring(BASE_URL.length);
                if (   'nav/' == current.substring(0, 4)
                    || 'topic/' == current.substring(0, 6)
                    || 'rtopic/' == current.substring(0, 7)
                    || 'ntopic/' == current.substring(0, 7)
                    || 'nftopic/' == current.substring(0, 8)) {
                    hash = current;
                }

                // fill search field, if the URL contains a query ...&q=...
                if (src.indexOf('&q=') > 0) {
                    setSearchFieldValue(decodeURIComponent(src.substring(src.indexOf('&q=') + 3)));
                }

                // focus on content iFrame
                getElementById('c').contentWindow.focus();

            } catch(e) {}
        }
        try {
            var url = hash ? '#' + hash : window.location.href.replace(/^([^#\?]*(?:\?([^#\?]*))?)(#.*)?$/, '$1');
            window.history.replaceState(null, '', url);
        } catch(e) {}
    }
    function isQueryHash(hash) {
        return hash && (   (hash.length > 1 && hash.substring(0, 2) == 'q=')
                        || (hash.length > 2 && hash.substring(0, 3) == '#q='));
    }

    function createSlider(tocSidebarToggleButton, toolbar, headSpacerElement) {

        // create slider element
        var slider = createElement();
        slider.id = 's';
        getElementById('m').insertBefore(slider, getElementById('c'));
        var sliderWidth = slider.getBoundingClientRect().width;
        var sliderHalfWidth = sliderWidth > 0 ? sliderWidth / 2 : 0;

        // create overlay required for smooth slider drag'n'drop
        var overlay = createOverlay();

        // TOC sidebar with its style and width
        var tocWidth;
        var tocSidebar = getElementById('t');
        var tocSidebarStyle = tocSidebar.style;
        var headSpacerElementStyle = headSpacerElement.style;
        var toolbarWidth = toolbar.getBoundingClientRect().width;
        var tocSidebarMinimumWidth = TOC_SIDEBAR_MINIMUM_WIDTH > toolbarWidth ? TOC_SIDEBAR_MINIMUM_WIDTH : toolbarWidth;

        // slider movement
        function move(e) {
            tocWidth = (e.touches ? e.touches[0].clientX : e.pageX) - sliderHalfWidth;
            if (tocWidth < 0) {
                tocWidth = 0;
            }
            tocSidebarStyle.width = tocWidth + 'px';
            updateSpacerWidth();
            preventDefault(e);
        }
        function moveEnd(e) {
            if (e.touches) {
                addOrRemoveEventListener(0, 'touchmove', move, 1);
                addOrRemoveEventListener(0, 'touchcancel', moveEnd);
                addOrRemoveEventListener(0, 'touchend', moveEnd);
            } else {
                addOrRemoveEventListener(0, 'mousemove', move);
                addOrRemoveEventListener(0, 'mouseup', moveEnd);
            }
            overlay.o();
            tocSidebarStyle.userSelect = '';
            if (tocWidth < tocSidebarMinimumWidth) {
                var oldWidth = getCookie('toc-width');
                tocWidth = oldWidth ? oldWidth : TOC_SIDEBAR_DEFAULT_WIDTH;
                toggleTocSidebar();
            }
            setCookie(TOC_SIDEBAR_WIDTH_COOKIE_NAME, tocWidth);
            preventDefault(e);
            stopPropagation(e);
        }
        function moveStart(e) {
            if (e.which && e.which != 1) return;
            if (e.touches) {
                addOrRemoveEventListener(1, 'touchend', moveEnd);
                addOrRemoveEventListener(1, 'touchcancel', moveEnd);
                addOrRemoveEventListener(1, 'touchmove', move, 1);
            } else {
                addOrRemoveEventListener(1, 'mouseup', moveEnd);
                addOrRemoveEventListener(1, 'mousemove', move);
            }
            overlay.a();
            setClassName(tocSidebar, '');
            tocSidebarStyle.transition = '';
            headSpacerElementStyle.transition = '';
            preventDefault(e);
            stopPropagation(e);
        }
        var documentElement = document.documentElement;
        function addOrRemoveEventListener(add, event, fn, passive) {
            if (add) {
                documentElement.addEventListener(event, fn, passive ? { passive: false } : false);
            } else {
                documentElement.removeEventListener(event, fn, passive ? { passive: false } : false);
            }
        }
        function updateSpacerWidth(hideToc, tocWidth, withTransition) {
            if (withTransition) {
                headSpacerElementStyle.transition = 'width .25s ease-in';
            }
            var spacerWidth =   (tocWidth || tocSidebar.getBoundingClientRect().right)
                              + sliderWidth
                              - toolbar.getBoundingClientRect().right;
            var displayWidth = hideToc || spacerWidth < 0 ? 0 : spacerWidth;
            headSpacerElementStyle.width = displayWidth + 'px';
            var widthPostfix = displayWidth >= LOGO_ICON_WIDTH ? (displayWidth >= LOGO_FULL_WIDTH ? 'f' : 1) : 0;
            setClassName(headSpacerElement, 'k' + (embeddedMode ? 'e' : '') + widthPostfix);
        }

        addEvent(slider, 'mousedown', moveStart);
        addEvent(slider, 'touchstart', moveStart);

        // TOC sidebar toggling
        function toggleTocSidebar(e, initialize, asSmallScreenAutoCloseFn) {
            if (!asSmallScreenAutoCloseFn) {
                preventDefault(e);
            }
            var isSmall = isSmallScreen();
            var currentClass = getClassName(tocSidebar);
            if (asSmallScreenAutoCloseFn && (!isSmall || currentClass != 'show')) return;
            var hideToc = isSmall ? currentClass == 'show' : tocWidth > 0;
            if (initialize) {
                tocWidth = -getCookie(TOC_SIDEBAR_WIDTH_COOKIE_NAME, TOC_SIDEBAR_DEFAULT_WIDTH);
                hideToc = isSmall || tocWidth > 0;
            } else {
                tocSidebarStyle.transition = 'width .25s ease-in';
                headSpacerElementStyle.transition = 'margin-right .25s ease-in';
            }
            setClassName(tocSidebar, isSmall ? (hideToc ? '' : 'show') : (hideToc ? 'hide' : ''));
            if (initialize || !isSmall) {
                tocWidth = -tocWidth;
                if (!initialize) {
                    setCookie(TOC_SIDEBAR_WIDTH_COOKIE_NAME, tocWidth);
                }
            }
            if (initialize || !isSmall) {
                tocSidebarStyle.width =   (hideToc ? 0 : tocWidth > tocSidebarMinimumWidth
                                                         ? tocWidth
                                                         : TOC_SIDEBAR_DEFAULT_WIDTH)
                                        + 'px';
            }
            tocSidebarStyle.userSelect = hideToc ? 'none' : '';
            if (!hideToc && tocSidebar.f) {
                tocSidebar.f();
            }
            if (!isSmall) {
                updateSpacerWidth(hideToc, tocWidth, 1);
            }
        }
        toggleTocSidebar(0, 1);
        addEvent(slider, 'dblclick', toggleTocSidebar);
        addEvent(tocSidebarToggleButton, 'click', toggleTocSidebar);

        // function to close TOC if screen is small
        return function(e) {
            toggleTocSidebar(e, 0, 1);
        };

    }

    function tocContentProvider(node, processChildrenFn) {
        var callbackUrl =   BASE_URL + 'advanced/tocfragment'
                          + (node
                             ?   (node.toc ? '?toc=' + node.toc : '')
                               + (node.path ? '&path=' + node.path : '')
                               + (node.topic ? '?errorSuppress=true&topic=' + node.topic : '')
                               + (node.expand ? '?errorSuppress=true&expandPath=' + node.expand : '')
                             : '');
        remoteRequest(callbackUrl, function(responseText) {
            var nodes = tocXmlNodes(parseXml(responseText), node ? node.toc : 0, node ? node.path : 0);
            var children;
            for (var i = 0; i < nodes.length; i++) {
                var n = nodes[i];
                if (n.tagName == 'numeric_path') {
                    tocContentProvider({expand: getAttribute(n, 'path')}, processChildrenFn);
                    return;
                }
                if (n.tagName == 'node') {
                    children = tocToNodes(nodes, node ? node.toc : 0, node ? node.expand : 0);
                    break;
                }
            }
            if (!node) {
                createSearchField();
                initContentPage();
                setFontSize(0, 1);
            }
            processChildrenFn(children);
        });
    }
    function tocXmlNodes(xml, toc, path) {
        var books = xml.documentElement.childNodes;
        if (!toc) return books;
        var book;
        for (var i = 0; i < books.length; i++) {
            book = books[i];
            if (book.tagName == 'node' && toc == book.getAttribute('id')) {
                if (!path) return book.childNodes;
                break;
            }
        }
        var nodes = book.childNodes;
        tocLevelLoop: while (1) {
            for (var i = 0; i < nodes.length; i++) {
                n = nodes[i];
                if (n.tagName != 'node') continue;
                var id = n.getAttribute('id');
                if (path == id) return n.childNodes;
                if (   id
                    && path.length > id.length
                    && path.substring(0, id.length + 1) == id + '_') {
                    nodes = n.childNodes;
                    continue tocLevelLoop;
                }
            }
            break;
        }
        return [];
    }
    function tocToNodes(xmlChildren, toc, expandPath) {
        var children = [];
        for (var i = 0; i < xmlChildren.length; i++) {
            var n = xmlChildren[i];
            if (n.tagName != 'node') continue;
            var currentToc = toc ? toc : getAttribute(n, 'id');
            children.push({
                n/*node*/: {
                    toc: currentToc,
                    path: toc ? getAttribute(n, 'id') : 0,
                    t: getAttribute(n, 'title'),
                    h: BASE_URL + getAttribute(n, 'href').substring(3),
                    i: n.getAttribute('image'),
                    y: expandPath,
                    l/*is leaf*/: getAttribute(n, 'is_leaf')
                },
                l/*is leaf*/: getAttribute(n, 'is_leaf'),
                c/*children*/: tocToNodes(n.childNodes, currentToc)
            });
        }
        return children;
    }

    function isSmallScreen() {
        var clientWidth = document.documentElement.clientWidth || document.body.clientWidth;
        return clientWidth <= SMALL_SCREEN_WIDTH;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Search: search-as-you-type ('t') and full search ('f')

    function createSearchField() {
        var currentTocLi;

        // create overlay required for closing proposals drop-down even when clicking into the content iframe
        var overlay = createOverlay();
        addEvent(overlay, 'click', hideProposals);

        // area (containing scope drop-down, search field and button)
        // "searchFieldAreaWrapper" as workaround for sub-pixel problem in Firefox (1px border might become 0.8px border
        // to align real pixels on high-DPI to CSS px), otherwise proposals drop-down might not correctly aligned with
        // search field area
        var searchFieldAreaWrapper = createElement(getElementById('h'), 0, 'q0');
        var searchFieldArea = createElement(createElement(searchFieldAreaWrapper, 0, 'q1'), 'form', 'q');

        if (embeddedMode) {
            createButton(searchFieldAreaWrapper, BOOKMARKS_ICON, '<%js:bookmarks_button_description%>', function() {
                bookmarksPage.s(!bookmarksPage.o);
            });
        }

        var searchFieldAreaHasFocus;
        var searchFieldAreaContainsQuery;
        var proposals;
        function updateSearchFieldAreaClass() {
            setClassName(searchFieldArea,
                             'q' + (proposals.style.display == 'block' ? 'm' : (searchFieldAreaHasFocus ? 'f' : ''))
                           + (searchFieldAreaContainsQuery ? ' qa' : '')
                         );
        }
        createMenu();

        // scopes drop-down
        var scopeButtonWrapper = createElement(searchFieldArea, 0, 's0');

        var booksButton = createElement(scopeButtonWrapper, 'button', 's');
        setAttribute(booksButton, 'type', 'button');
        booksButtonText = createElement(booksButton, 'span');
        var dropDownHandle = createElement(booksButton, 'span', 'de');
        setInnerHtml(dropDownHandle, TREE_HANDLE);
        var booksDropDown = createElement(scopeButtonWrapper, 0, 'u');
        booksDropDown.s = function(show) {
            var isOpen = booksDropDown.style.display == 'block';
            if (!isOpen == !show) return;
            booksDropDown.style.display = show ? 'block' : 'none';
            if (!show) return;
            setInnerHtml(booksDropDown);
            var booksDropDownUl = createElement(booksDropDown, 'ul', 'r');
            var menuItems = [];
            var dataItems = [];
            var currentBook;
            var currentChapter;
            for (var tocLi = currentTocLi; tocLi && tocLi.n; tocLi = tocLi.p) {
                if (!tocLi.n.path) {
                    currentBook = tocLi.n.t;
                    if (!currentChapter) {
                        currentChapter = currentBook;
                    }
                } else if (!currentChapter && !tocLi.n.l) {
                    currentChapter = tocLi.n.t;
                }
            }
            menuItems.push(createElement(booksDropDownUl, 'li', searchScope.l == 0 ? 'x': 0, '<%js:scope_none_label%>'));
            dataItems.push([0]);
            menuItems.push(createElement(booksDropDownUl, 'li', searchScope.l == 1 ? 'x': 0, '<%js:scope_book_label%>' + (currentBook ? ': ' + currentBook : '')));
            dataItems.push([1]);
            menuItems.push(createElement(booksDropDownUl, 'li', searchScope.l == 2 ? 'x': 0, '<%js:scope_chapter_label%>' + (currentChapter ? ': ' + currentChapter : '')));
            dataItems.push([2]);
            remoteRequest(BASE_URL + 'advanced/workingSetManager.jsp?t=' + Date.now(), function(menuItems, dataItems) {
                return function(responseText) {
                    var delimiter = 'l ';
                    var scopeIndex = 0;
                    SEARCH_SCOPE_ALL_PATTERN.lastIndex = 0;
                    for (var match; (match = SEARCH_SCOPE_ALL_PATTERN.exec(responseText)) != null;) {
                         var label = decodeHtml(match[1]);
                         if (label.substring(0, 1) == '\u200B') continue;
                         var className = delimiter + (searchScope.l == 4 && searchScope.s == label ? 'x' : '');
                         menuItems.push(createElement(booksDropDownUl, 'li', className, label));
                         delimiter = '';
                         dataItems.push([4, label, scopeIndex]);
                         scopeIndex++;
                    }
                    menuItems.push(createElement(booksDropDownUl, 'li', 'l y', '<%js:scope_manage_label%>'));
                    dataItems.push([5]);
                    toMenu(booksButton, menuItems, dataItems, setSearchScope);
                }
            }(menuItems, dataItems));
        }
        booksDropDown.s();
        var scopeOverlay = createOverlay(4);
        addEvent(scopeOverlay, 'click', function() { booksDropDown.s(); scopeOverlay.o(); });
        addEvent(booksButton, 'mousedown', function(e) {
            var isOpen = booksDropDown.style.display == 'block';
            try {
                booksButton.focus();
            } catch(e) {}
            booksDropDown.s(!isOpen);
            if (isOpen) {
                scopeOverlay.o();
            } else {
                scopeOverlay.a();
            }
            preventDefault(e);
            stopPropagation(e);
        });
        addEvent(booksButton, 'click', function(e) {stopPropagation(e)});
        addEvent(booksButton, 'focus', function() {
            booksDropDown.hasFocus = true;
            booksDropDown.s(1);
            scopeOverlay.a();
        });
//        addEvent(booksButton, 'blur', function() {
//            booksDropDown.hasFocus = false;
//            setTimeout(function() {if (!booksDropDown.hasFocus) booksDropDown.style.display = 'none'}, 200);
//        });
        if (!embeddedMode && BOOK_SCOPE_BY_DEFAULT && searchScope.l == 0 && getCookie(BOOK_SCOPE_COOKIE) != 'init') {
            setCookie(BOOK_SCOPE_COOKIE, 'init');
            setTimeout(function(){ if(setSearchScope) setSearchScope([1]); }, 420);
        } else {
            updateScopeButtonLabel();
        }

        setSearchScope = function(scopeData) {
            booksDropDown.s();
            scopeOverlay.o();
            if (scopeData[0] == 5) {
                scopesPage.s(1);
                return;
            }
            if (searchScope.l != scopeData[0]) {
                var scopeToRemove;
                if (searchScope.l == 1 || searchScope.l == 2 || searchScope.l == 4) {
                    scopeToRemove = '%E2%80%8B' + (searchScope.l > 1 ? '%E2%80%8B' : '');
                    for (var i = 0; searchScope.l == 4 && i <= searchScope.t; i++) {
                        scopeToRemove += '%E2%80%8B';
                    }
                }
                var scopeToAdd;
                if (scopeData[0] == 1 || scopeData[0] == 2 || scopeData[0] == 4) {
                    scopeToAdd = '%E2%80%8B' + (scopeData[0] > 1 ? '%E2%80%8B' : '');
                    for (var i = 0; scopeData[0] == 4 && i <= scopeData[2]; i++) {
                        scopeToAdd += '%E2%80%8B';
                    }
                }
                if (scopeToRemove || scopeToAdd) {
                    remoteRequest(  BASE_URL
                                  + 'workingSetState.jsp?operation='
                                  + (scopeToRemove && scopeToAdd ? 'edit' : (scopeToRemove ? 'remove' : 'add'))
                                  + (scopeToRemove && scopeToAdd ? '&oldName=' + scopeToRemove : '')
                                  + '&workingSet='
                                  + (!scopeToAdd ? scopeToRemove : scopeToAdd)
                                  + '&t=' + Date.now());
                }
            }
            searchScope.l = scopeData[0];
            searchScope.s = searchScope.l == 4 ? scopeData[1] : 0;
            searchScope.t = searchScope.l == 4 ? scopeData[2] : 0;
            updateScopeButtonLabel();
        }
        updateScopeByToc = function(li) {
            currentTocLi = li;
            updateScopeButtonLabel();
        }
        function updateScopeButtonLabel() {
            setInnerHtml(booksButtonText, '');
            if (searchScope.l == 0 || (searchScope.l < 3 && !currentTocLi)) return;
            var newButtonlabel;
            for (var tocLi = currentTocLi; searchScope.l < 3 && tocLi && tocLi.n; tocLi = tocLi.p) {
                if (!tocLi.n.path || (searchScope.l == 2 && !tocLi.n.l)) {
                    newButtonlabel = tocLi.n.t;
                    break;
                }
            }
            if (searchScope.l == 4) {
                newButtonlabel = searchScope.s;
            }
            if (newButtonlabel) {
                booksButtonText.appendChild(document.createTextNode(BOOK_NAME_SHORTENER(newButtonlabel)));
            }
        }

        // search field
        var wrap = createElement(searchFieldArea, 0, 'q2');
        wrap.style.position = 'relative';
        var searchField = createElement(wrap, 'input');
        searchField.id = 'q';
        searchField.type = 'text';
        searchField.alt = '<%js:search_field_description%>';
        searchField.title = '<%js:search_field_description%>';
        searchField.autocomplete = 'off';
        searchField.placeholder = '<%js:search_field_placeholder%>';
        addEvent(searchField, 'input', search); // for IE 8 do also on 'propertychange'
        addEvent(searchField, 'focus', search);

        var searchButton = createElement(searchFieldArea, 'button', 'b');
        setInnerHtml(searchButton, SEARCH_ICON);
        addEvent(searchFieldArea, 'submit', function(e) {preventDefault(e); search(e, 1);});

        // hint
        var hintField = createElement(wrap, 'input', 'qh');
        if (searchField.nextSibling) {
            searchField.parentNode.insertBefore(hintField, searchField.nextSibling);
        }
        searchField.style.position = 'relative';
        hintField.style.position = 'absolute';
        hintField.style.left = 0;
        hintField.style.top = 0;
        hintField.style.height = '100%';
        hintField.style.width = '100%';
        hintField.style.background = 'transparent';
        hintField.style.borderColor = 'transparent';
        hintField.setAttribute('disabled', 'disabled');
        wrap.appendChild(searchField);
        searchField.style.background = 'url("data:image/gif;base64,R0lGODlhAQABAID/AMDAwAAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw%3D%3D") repeat';

        setSearchFieldValue = function(value) {
            if (value == searchField.value) return;
            searchField.value = value;
            hintField.value = '';
        }

        var searchFieldAreaElements = [booksButton, searchField, searchButton];
        for (var i = 0; i < searchFieldAreaElements.length; i++) {
            addEvent(searchFieldAreaElements[i], 'focus', function() {
                searchFieldAreaHasFocus = 1;
                updateSearchFieldAreaClass();
            });
            addEvent(searchFieldAreaElements[i], 'blur', function() {
                searchFieldAreaHasFocus = 0;
                updateSearchFieldAreaClass();
            });
        }

        // proposals drop-down
        proposals = createElement(searchFieldArea, 0, 'p');
        addEvent(proposals, 'click', function(e) {stopPropagation(e)});
        function showProposals() {
            proposals.style.display = 'block';
            overlay.a();
            updateSearchFieldAreaClass();
        }
        function hideProposals() {
            proposals.style.display = 'none';
            overlay.o();
            updateSearchFieldAreaClass();
        }
        hideProposals();

        // focus search field
        searchField.focus();

        function search(e, fullSearch) {
            var noPendingQueries = !currentSearch[getSearchTypeId(fullSearch)];

            // get (trimmed) search word, query, URL and remember query to detect stale responses
            var searchWord =  searchField.value.replace(/(^\s+|\s+$)/ig, '');

            // TODO if Eclipse bug 351077 (https://bugs.eclipse.org/351077), remove following line
            if (!fullSearch || searchWord.indexOf('*') >= 0 || searchWord.indexOf('?') >= 0) {
                searchWord = searchWord.replace(/\-([^\-\s]*$)/ig, ' $1');
            }

            var tocScope = currentTocLi ? currentTocLi.n : currentTocLi;
            for (var tocLi = currentTocLi; searchScope.l < 3 && tocLi && tocLi.n; tocLi = tocLi.p) {
                if (!tocLi.n.path || (searchScope.l == 2 && !tocLi.n.l)) {
                    tocScope = tocLi.n;
                    break;
                }
            }
            var query = searchWord.length
                        ? (  encodeURIComponent(searchWord)
                           + ((searchScope.l == 1 || searchScope.l == 2) && tocScope && tocScope.toc ? '&toc=' + encodeURIComponent(tocScope.toc) : '')
                           + (searchScope.l == 2 && tocScope && tocScope.path ? '&path=' + tocScope.path : '')
                           + (searchScope.l == 4 && searchScope.s ? '&scope=' + encodeURIComponent(searchScope.s) : ''))
                        : '';
            var url =   SEARCH_BASE_URL
                      + query.replace(/(\&|$)/, (fullSearch ? '' : '*') + '$1')
                      + '&maxHits='
                      + (fullSearch ? SEARCH_HITS_MAX : SEARCH_AS_YOU_TYPE_PROPOSAL_MAX)
                      + (query.indexOf('&toc=') < 0 ? '' : '&quickSearch=true&quickSearchType=QuickSearchToc');
            currentSearch[getSearchTypeId(fullSearch)] = query;
            if (fullSearch) {
                currentSearch['t'] = 0;
                hideProposals();
                if (bookmarksPage) bookmarksPage.s();
                if (scopesPage) scopesPage.s();
                updateDeepLink(query);
            } else {

                // hide hint
                hintField.value = '';

            }

            // empty search?
            searchFieldAreaContainsQuery = searchWord.length;
            updateSearchFieldAreaClass();
            if (!searchWord.length) {
                if (!fullSearch) {
                    hideProposals();
                }
                return;
            }

            // init UI
            if (fullSearch) {
                if (searchPage.o && query == searchPage.q) return;
                searchPage.s(1);
                if (query == searchPage.q) {
                    window.frames.c.location = url;
                    return;
                }
                setInnerHtml(searchPage, '<%js:search_page_loading%>');
                searchPage.scrollTop = 0;
            } else if (query == proposals.q) {
                preventDefault(e);
                showProposals();
                return;
            }

            // cached?
            var cache = SEARCH_CACHE[getSearchTypeId(fullSearch)];
            for (var i = 0; i < cache.length; i++) {
                var r = cache[i];
                if (query == r.q) {
                    if (fullSearch) {
                        window.frames.c.location = url;
                    }
                    renderResults(fullSearch, r.r, r.b, query, searchWord, r.s);
                    return;
                }
            }

            // submit query to server
            if (fullSearch) {
                window.frames.c.location = url;
                return;
            }
            var currentSearchScope = {
                /* level */               l: searchScope.l,
                /* (custom) scope name */ s: searchScope.s,
                /* path */                p: 0
            };
            if (searchScope.l > 0 && searchScope.l < 3 && currentTocLi) {
                currentSearchScope.p = [currentTocLi.n];
                for (var parentLi = currentTocLi.p; searchScope.l > 1 && parentLi && parentLi.n; parentLi = parentLi.p) {
                    currentSearchScope.p.unshift(parentLi.n);
                }
            }
            var callbackFn = callbackFor(fullSearch, query, searchWord, currentSearchScope);
            if (noPendingQueries) {
                remoteRequest(url, callbackFn, getSearchTypeId(fullSearch));
            } else {
                setTimeout(function() {

                    // remote request if and only if not staled/outdated
                    if (query == currentSearch[getSearchTypeId(fullSearch)])
                        remoteRequest(url, callbackFn, getSearchTypeId(fullSearch));

                }, SEARCH_DELAY_IN_MILLISECOND);
            }
        }

        renderFullSearch = function(queryPart, data) {
            var query = queryPart.substring(0, queryPart.indexOf('&maxHits='));
            updateDeepLink(query);
            if (query == searchPage.q) return;
            var valuePairs = queryPart.split('&');
            var searchWord;
            var toc;
            var path;
            var scope;
            for (var i = 0; i < valuePairs.length; i++) {
                if (!searchWord && valuePairs[i].indexOf('=') < 0) {
                    searchWord = decodeURIComponent(valuePairs[i]);
                } else if (valuePairs[i].substring(0, 4) == 'toc=') {
                    toc = decodeURIComponent(valuePairs[i].substring(4));
                } else if (valuePairs[i].substring(0, 5) == 'path=') {
                    path = decodeURIComponent(valuePairs[i].substring(5));
                } else if (valuePairs[i].substring(0, 6) == 'scope=') {
                    scope = decodeURIComponent(valuePairs[i].substring(6));
                }
            }
            var currentSearchScope = {
                /* level */               l: scope ? 4 : (toc ? (path ? 2 : 1) : 0),
                /* (custom) scope name */ s: scope,
                /* path */                p: 0
            };
            var scopeFound = !toc;
            if (toc && currentTocLi && currentTocLi.n) {
                var nodes = [currentTocLi.n];
                for (var parentLi = currentTocLi.p; parentLi && parentLi.n; parentLi = parentLi.p) {
                    if (path) {
                        nodes.unshift(parentLi.n);
                    } else {
                        nodes = [parentLi.n];
                    }
                }
                if (toc == nodes[0].toc && (!path || path == nodes[nodes.length - 1].path)) {
                    currentSearchScope.p = nodes;
                    scopeFound = 1;
                }
            }
            if (!scopeFound) {
                remoteRequest(  BASE_URL + 'advanced/tocfragment?toc=' + encodeURIComponent(toc)
                              + (path ? '&path=' + path : ''), (function(toc, path, currentSearchScope, searchWord, query, data) {
                    return function(responseText) {
                        var nodePath = [];
                        var books = parseXml(responseText).documentElement.childNodes;
                        var book;
                        for (var i = 0; i < books.length; i++) {
                            book = books[i];
                            if (book.tagName == 'node' && toc == book.getAttribute('id')) {
                                nodePath.push({t: book.getAttribute('title'), toc: toc});
                                break;
                            }
                        }
                        var nodes = book.childNodes;
                        tocLevelLoop: while (path) {
                            for (var i = 0; i < nodes.length; i++) {
                                n = nodes[i];
                                if (n.tagName != 'node') continue;
                                var id = n.getAttribute('id');
                                if (path == id) {
                                    nodePath.push({t: n.getAttribute('title'), toc: toc, path: id});
                                    break tocLevelLoop;
                                };
                                if (   id
                                    && path.length > id.length
                                    && path.substring(0, id.length + 1) == id + '_') {
                                    nodes = n.childNodes;
                                    nodePath.push({t: n.getAttribute('title'), toc: toc, path: id});
                                    continue tocLevelLoop;
                                }
                            }
                            break;
                        }
                        currentSearchScope.p = nodePath;
                        (callbackFor(1, query, searchWord, currentSearchScope))(data);
                    };})(toc, path, currentSearchScope, searchWord, query, data));
                return;
            }
            (callbackFor(1, query, searchWord, currentSearchScope))(data);
        }

        function callbackFor(fullSearch, query, searchWord, searchScope) {
            return function(data) {

                // indexing in progress?
                var match = SEARCH_RESULTS_INDEXING_PATTERN.exec(data);
                if (match != null) {
                    if (fullSearch) {
                        setInnerHtml(searchPage, '<%js:search_indexing%>'.replace('{0}', match[1]));
                    }
                    return;
                }

                // parse HTML for results
                var element = createElement();
                var hasBreadcrumbs = 0;
                var results = [];

                SEARCH_RESULTS_PATTERN.lastIndex = 0;
                for (var match; (match = SEARCH_RESULTS_PATTERN.exec(data)) != null;) {
                    var items = [];
                    for (var i = 2; i < 8; i++) {
                        element.innerHTML = match[i];
                        items.push((element.textContent ? element.textContent : element.innerText).replace(/^\s+|\s+$/g,'').replace(/\s+/g,' '));
                    }
                    var breadcrumb = [];
                    if (match[6]) {
                        SEARCH_RESULTS_BREADCRUMB_SNIPPET_PATTERN.lastIndex = 0;
                        for (var breadcrumbMatch; (breadcrumbMatch = SEARCH_RESULTS_BREADCRUMB_SNIPPET_PATTERN.exec(match[6])) != null;) {
                            for (var i = 1; i < 3; i++) {
                                element.innerHTML = breadcrumbMatch[i];
                                breadcrumb.push((element.textContent ? element.textContent : element.innerText).replace(/^\s+|\s+$/g,'').replace(/\s+/g,' '));
                            }
                        }
                        hasBreadcrumbs = 1;
                    }
                    var hrefFollowedByTitle = 'href' == match[1];
                    results.push({
                        /* title       */ t: items[3],
                        /* description */ d: items[5],
                        /* href        */ h: items[hrefFollowedByTitle ? 0 : 2].substring(8) + '&q=' + encodeURIComponent(searchWord),
                        /* breadcrumb  */ b: match[6] ? breadcrumb : [0, items[hrefFollowedByTitle ? 2 : 0]]
                    });
                }

                // cache parsed results
                var queryResult = {
                    /* results         */ r: results,
                    /* has breadcrumbs */ b: hasBreadcrumbs,
                    /* query           */ q: query,
                    /* search scope    */ s: searchScope
                }
                var cache = SEARCH_CACHE[getSearchTypeId(fullSearch)];
                var cacheIndexId = getSearchTypeId(fullSearch) + 'i';
                var cacheSize = fullSearch ? SEARCH_FULL_SEARCH_CACHE_SIZE : SEARCH_AS_YOU_TYPE_CACHE_SIZE;
                SEARCH_CACHE[cacheIndexId] = (SEARCH_CACHE[cacheIndexId] + 1) % cacheSize;
                if (cache.length < cacheSize) {
                    cache.push(queryResult);
                } else {
                    cache[SEARCH_CACHE[cacheIndexId]] = queryResult;
                }

                renderResults(fullSearch, results, hasBreadcrumbs, query, searchWord, searchScope);
            }

        }

        function renderResults(fullSearch, results, hasBreadcrumbs, query, searchWord, searchScope) {

            // staled?
            if (!fullSearch && query != currentSearch[getSearchTypeId(fullSearch)]) return;

            // show results
            var items = [];
            var data = [];
            var filters = [];
            var filterValues = [];
            function applyFilters(e) {
                var includeFilters = [];
                var excludeFilters = [];
                for (var i = 0; i < filters.length; i++) {
                    var f = filters[i];
                    if (!f.checked && !f.indeterminate) excludeFilters.push(filterValues[i]);
                    if (f.checked && !f.indeterminate) includeFilters.push(filterValues[i]);
                }
                for (var i = 0; i < items.length; i++) {
                    items[i].style.display =    arrayContainsPrefix(includeFilters, data[i])
                                             && !arrayContainsPrefix(excludeFilters, data[i])
                                             ? 'block'
                                             : 'none';
                }
                stopPropagation(e);
            }

            var searchScopeLabel = searchScope.l > 3 ? searchScope.s : '';
            if (searchScope.p) {
                for (var i = 0; i < searchScope.p.length; i++) {
                    if (i > 0) {
                        searchScopeLabel += ' > ';
                    }
                    searchScopeLabel += searchScope.p[i].t;
                }
            }
            var parentElement = fullSearch ? searchPage : proposals;
            setInnerHtml(parentElement, '');
            parentElement.q = query;
            parentElement.l = (searchScope.l > 0 ? '<%js:search_scoped_title%>' : '<%js:search_title%>').replace('{0}', searchWord).replace('{1}', searchScopeLabel);
            if (fullSearch) {
                document.title = searchPage.l + ' - ' + title;

                // no results?
                if (!results.length) {
                    var noResults = createElement(searchPage, 0, 'r0', '<%js:searchpage_nothing_found_prefix%>' + ' ');
                    createElement(noResults, 'strong', 0, searchWord);
                    return;
                }

                // filter tree
                var filterTree = asTree(results, [], 9, true);
                // TODO correction of tree for deeper scopes (below) might be done in "asTree" or "asTree" might be better simplified
                if (searchScope.p && filterTree.length == 1 && filterTree[0].isNode) {
                    var afterCutOff = searchScope.p.length * 2 - filterTree[0].name.length;
                    if (afterCutOff < 0) {
                        filterTree[0].name = filterTree[0].name.slice(searchScope.p.length * 2);
                    } else if (afterCutOff >= 0) {
                        filterTree = filterTree[0].children;
                        if (afterCutOff > 0 && filterTree.length > 0 && filterTree[0].isNode) {
                            if (filterTree[0].name.length <= afterCutOff) {
                                filterTree = filterTree[0].children;
                            } else {
                                filterTree[0].name = filterTree[0].name.slice(afterCutOff);
                            }
                        }
                    }
                }
                createTree(searchPage,

                    // content provider
                    function(node, processChildrenFn) {
                        if (!node) {
                            processChildrenFn([{ n/*ode*/: {children: filterTree}, l/*eaf*/: 0 }], 1);
                            return;
                        }
                        var children = [];
                        for (var i = 0; i < (node.isNode ? node.children.length : filterTree.length); i++) {
                            var childNode = node ? node.children[i] : filterTree[i];
                            if (childNode.isNode) {
                                var isLeaf = 1;
                                for (var j = 0; j < childNode.children.length; j++) {
                                    if (childNode.children[j].isNode) {
                                        isLeaf = 0;
                                        break;
                                    }
                                }
                                children.push({ n/*ode*/: childNode, l/*eaf*/: isLeaf });
                                childNode.p/*arent*/ = node;
                            }
                        }
                        processChildrenFn(children);
                    },

                    // label provider
                    function(li, node) {
                        var isRoot = !node.isNode;

                        // checkbox
                        var checkboxWithLabel = createElement(li);
                        var checkbox = createElement(checkboxWithLabel, 'input');
                        checkbox.type = 'checkbox';
                        checkbox.checked = node.p ? node.p.x.checked : true;
                        node.x = checkbox;
                        if (node.p) {
                            checkbox.parentCheckbox = node.p.x;
                        }
                        checkbox.numberOfResults = isRoot ? results.length : node.count;
                        filters.push(checkbox);
                        filterValues.push(isRoot ? '' : toValue(node.l.concat(node.name)));

                        // label
                        var labelText = '';
                        if (isRoot) {
                            checkbox.style.display = 'none';
                            labelText = (searchScope && searchScope.l > 0
                                        ? '<%js:searchpage_heading_scoped%>'
                                        : '<%js:searchpage_heading%>') + ' ';
                        } else {
                            addEvent(checkbox, 'click', (function(liCheck, li) {
                                return function() {
                                    selectSubtree(li, liCheck.checked);
                                    updateParentsChecks(liCheck);
                                    applyFilters();
                                };
                            })(checkbox, li));
                            for (var i = 0; i < node.name.length; i+=2) {
                                labelText += (i == 0 ? '' : ' > ') + node.name[i+1];
                            }
                        }
                        var label = createElement(checkboxWithLabel, 'span', node.isNode ? 0 : 't', labelText + ' ');
                        if (isRoot && searchScope && searchScope.l > 0 ) {
                            createElement(label, 'span', 'tl', searchScopeLabel + ' ');
                        }
                        createElement(label, 'span', 'count', checkbox.numberOfResults);
                        addEvent(label, 'click', (function(checkbox, li) {
                            return function() {
                                var root;
                                for (root = checkbox; root.parentCheckbox; root = root.parentCheckbox);
                                for (var i = 0;  i < 5; i++) {
                                    root = getParentElement(root);
                                    if (root.tagName == 'UL') break;
                                }
                                selectSubtree(root, false);
                                selectSubtree(li, true);
                                updateParentsChecks(checkbox);
                                applyFilters();
                            };
                        })(checkbox, li));

                        return checkboxWithLabel;
                    },
                    0);

            }
            var resultList = createElement(parentElement, 'ol', 'j');
            if (!fullSearch) {

                // no results?
                if (!results.length) return;

                // hint
                var wordBeginRegEx = queryToRegEx(query);
                var newHints = {};
                for (var i = 0; i < results.length && searchWord.length < 36; i++) {
                    var match = wordBeginRegEx.exec(results[i].t/*title*/);
                    if (match) {
                        var pHint = match[0].toLowerCase();
                        newHints[pHint] =   (newHints[pHint] ? newHints[pHint] : 0)
                                          + (1 + (results.length - i) / results.length) / results.length;
                    }
                    match = wordBeginRegEx.exec(results[i].d/*description*/);
                    if (match) {
                        var pHint = match[0].toLowerCase();
                        newHints[pHint] =   (newHints[pHint] ? newHints[pHint] : 0)
                                          + (0.7 + (results.length - i) / results.length) / results.length;
                    }
                }
                var allHints = [];
                for (var i in newHints) {
                    if (newHints[i] < 1.8 / results.length) continue;
                    allHints.push(newHints[i].toFixed(7) + i);
                }
                allHints.sort().reverse();
                hintField.value = allHints.length > 0
                                  ? searchField.value + wordBeginRegEx.exec(allHints[0].substring(9))[1]
                                  : '';

                // query proposals
                for (var i = 0; i < allHints.length && i < 3; i++) {
                    var hintText = allHints[i].substring(9);
                    var li = createElement(resultList, 'li');
                    var button = createElement(li, 'button');
                    var spacerElementStyle = createElement(button, 'span').style;
                    spacerElementStyle.display = 'inline-block';
                    spacerElementStyle.width = booksButton.offsetWidth + 'px';
                    createElement(button, 'span', null, hintText.substring(0, searchWord.length));
                    createElement(button, 'strong', null, hintText.substring(searchWord.length));
                    items.push(li);
                    data.push([hintText]);
                }

            }

            function toValue(path) {
                var result = '';
                for (var i = 0; i < path.length; i++) result += (i > 0 ? '\n' : '') + path[i];
                return result;
            }

            // list results
            for (var i = 0; i < results.length; i++) {
                var node = results[i];
                var li = createElement(resultList, 'li');
                var a = createElement(li, 'a');
                a.href = BASE_URL + 'topic' + node.h/*href*/;
                a.target = 'c';
                var titleAndLocation = createElement(a, 0, 'm');

                // title
                addHighlightedText(createElement(titleAndLocation, 0, 'v'), node.t/*title*/, searchWord);

                // show book title only for no book/chapter scope
                if (!fullSearch && !searchScope.p) {
                    createElement(titleAndLocation, 0, 'w', node.b/*breadcrumb*/[1]);
                }

                // breadcrumb
                if (fullSearch && hasBreadcrumbs && node.b/*breadcrumb*/) {
                    var location = createElement(titleAndLocation, 0, 'w');
                    for (var j = searchScope && searchScope.p ? searchScope.p.length * 2 : 0; j < node.b/*breadcrumb*/.length; j+=2) {
                        createElement(location, 'span', 0, node.b/*breadcrumb*/[j+1]);
                        if (j < node.b/*breadcrumb*/.length-2) {
                            createElement(location, 'span', 0, ' > ');
                        }
                    }
                }

                // description
                addHighlightedText(createElement(a, 0, 'n'), node.d/*description*/, searchWord);

                // UI element and corresponding data
                items.push(li);
                if (fullSearch) {
                    var resultofStart = node.h/*href*/.indexOf('?resultof=');
                    var hrefNormed = '../topic' + (resultofStart < 0 ? node.h/*href*/ : node.h/*href*/.substring(0, resultofStart));
                    data.push(toValue(node.b/*breadcrumb*/.slice().concat(hrefNormed).concat(node.t/*title*/)));
                } else {
                    data.push([node.t/*title*/, node.h/*href*/]);
                }

            }

            // add key support (and show proposals)
            if (fullSearch) {
//                toMenu(searchField, items, results, function(d) {
//                        getElementById('c').src = BASE_URL + 'topic' + d.h/*href*/;
//                    },
//                    0,
//                    0,
//                    function(item, data, viaMouse) {
//                        if (!viaMouse) {
//                            scrollIntoViewIfNeeded(searchPage, item);
//                        }
//                    },
//                    1);
            } else {

                // key support
                toMenu(searchField, items, data, function(d) {

                        // apply hint
                        if (d.length < 2) {
                            hintField.value = '';
                            searchField.value = d;
                            search();
                            return;
                        }

                        // show search result
                        var searchWord = d[0];
                        var toc;
                        var tocStart = searchWord.indexOf('&toc=');
                        if (tocStart > 0) {
                            toc = decodeURIComponent(searchWord.substring(tocStart + 5));
                            searchWord = searchWord.substring(0, tocStart);
                        }
                        if (searchSearchWord(searchWord + '*', toc, d[1], false, true)) return;
                        getElementById('c').src = BASE_URL + 'topic' + d[1];
                        hideProposals();

                    },
                    function(d, key) {

                        // empty search field?
                        if (!searchField.value) return false;

                        // ignore RIGHT (key: 39) if cursor not at the end
                        if (   key == 39
                            && searchField
                            && searchField.selectionStart
                            && searchField.value
                            && searchField.value.length != searchField.selectionStart)
                            return false;

                        if (d && d.length > 0 && d[0].length < 2) {
                            searchField.value = d[0][0];
                            search();
                            return true;
                        }
                        return false;
                    },
                    hideProposals,
                    function(a, b) {
                        if (b.length < 2 || a.armed) return;
                        a.armed = true;
                        var iFrame = createElement(a, 'iframe', 'f');
                        iFrame.frameBorder = 0;

                        // TODO handle absolute paths
                        iFrame.src = BASE_URL + 'topic' + b[1];
                    });

                // show proposals
                showProposals();

            }

            // done (no pending queries)
            if (query == currentSearch[getSearchTypeId(fullSearch)]) {
                currentSearch[getSearchTypeId(fullSearch)] = 0;
            }

        }

        function getSearchTypeId(fullSearch) {
            return fullSearch ? 'f' : 't';
        }

        function asTree(results, path, depth) {
            if (depth < 1) return results;
            var tree = [];
            var grouped = {};
            for (var i = 0; i < results.length; i++) {
                var r = results[i];
                r.p = i;
                r.q = r.b/*breadcrumb*/.slice();
                var resultofStart = r.h/*href*/.indexOf('?resultof=');
                r.q.push('../topic' + (resultofStart < 0 ? r.h/*href*/ : r.h/*href*/.substring(0, resultofStart)));
                r.q.push(r.t/*title*/);

                // child?
                if (!r.b/*breadcrumb*/ || r.b/*breadcrumb*/.length <= path.length) {
                    tree.push(r);
                    continue;
                }

                // not child -> contained in a subtree
                var key = r.b/*breadcrumb*/[path.length] + '\n' + r.b/*breadcrumb*/[path.length+1];
                if (!grouped[key]) {
                    var node = {
                        isNode: true,
                        name: [r.b/*breadcrumb*/[path.length], r.b/*breadcrumb*/[path.length+1]],
                        l/*ocation*/: path.slice(),
                        children: [r]
                    };
                    grouped[key] = node;
                    tree.push(node);
                } else {
                    grouped[key].children.push(r);
                }

            }

            // calculate count and set the root (if it exists)
            for (var i = 0; i < tree.length; i++) {
                var r = tree[i];
                if (r.isNode) {
                    r.count = r.children.length + (r.root ? 1 : 0);
                    continue;
                }
                var rootOfGroup = grouped[r.q[r.q.length-2] + '\n' + r.t/*title*/];
                if (rootOfGroup) {
                    rootOfGroup.children.push(r);
                    rootOfGroup.count++;
                    tree.splice(i,1);
                    i--;
                }
            }

            // compact and recursion
            for (var i = 0; i < tree.length; i++) {
                var r = tree[i];
                if (!r.isNode) continue;
                compact(path, r);
                r.children = asTree(r.children, r.children[0].q.slice(0, path.length + r.name.length), depth - 1);
            }

            return tree;
        }
        function compact(path, r) {
            for (var i = path.length+2; r.children.length > 0 && !r.root; i+=2) {
                if (r.children.length == 1 && r.children[0].b/*breadcrumb*/.length == i) return;
                for (var j = 0; j < r.children.length; j++) {
                    var p0 = r.children[0].q;
                    var p = r.children[j].q;
                    if (   p.length < i+1
                        || p[i] != p0[i]
                        || p[i+1] != p0[i+1]) return;
                }
                var p0 = r.children[0].q;
                r.name.push(p0[i]);
                r.name.push(p0[i+1]);
            }
        }

        function queryToRegEx(query) {
            query = query.indexOf('&') < 0
                    ? query
                    : query.substr(0, query.indexOf('&'));
            query = decodeURIComponent(query.replace(/\+/g, '%20'));
            query = query.replace(/([\.\?\*\+\-\(\)\[\]\{\}\\])/g, '\\$1');
            return new RegExp("\\b(?:" + query + ")((?:\\w+|\\W+\\w+))", "i");
        }

        function searchSearchWord(searchWord, toc, href, path, isSearchWordDecoded) {
            try {

                // No SearchFrame or no NavFrame? -> exception handling
                var root = parent.parent.parent;
                var searchFrame = root.HelpToolbarFrame.SearchFrame;
                var navFrame = root.HelpFrame.NavFrame;

                var scopeElement = searchFrame.document.getElementById('scope');
                var searchInput = searchFrame.document.getElementById('searchWord');
                if (   searchInput
                    && scopeElement
                    && scopeElement.firstChild.nodeValue == 'All topics') {

                    // no scope -> update top left search input field only
                    searchInput.value = searchWord;

                } else {

                    // disable scope and update top left search input field
                    searchFrame.location.replace(  BASE_URL
                                                 + 'scopeState.jsp?workingSet=&searchWord='
                                                 + encodeURIComponent(searchWord));

                }
                var newNavUrl =   BASE_URL
                                + 'advanced/nav.jsp?e=h&tab=search&searchWord=' // 'e=h' for tracking (to distinguish normal queries from queries done with this script)
                                + (isSearchWordDecoded ? searchWord : encodeURIComponent(searchWord));
                if (toc) newNavUrl += '&quickSearch=true&quickSearchType=QuickSearchToc&toc=' + encodeURIComponent(toc);
                if (path) newNavUrl += '&path=' + path;
                navFrame.location.replace(newNavUrl);

                // topic (use 'setTimeout()' otherwise in Internet Explorer
                //        'Go Back' does not work sometimes)
                if (href) {
                    setTimeout(function(){window.location.href = BASE_URL + 'topic' + href}, 9);
                }

                return true;
            } catch(e) {
                return false;
            }
        }

        // filter tree functions
        function selectSubtree(element, checkStatus) {
            for (var i = 0; i < element.children.length; i++) {
                var n = element.children[i];
                if ('UL' == n.tagName || 'LI' == n.tagName || 'DIV' == n.tagName) {
                    selectSubtree(n, checkStatus);
                } else if ('INPUT' == n.tagName) {
                    n.indeterminate = false;
                    n.checked = checkStatus;
                    n.notAllChecked = false;
                }
            }
        }
        function getChildrenChecks(checkbox) {
            if (!checkbox || !checkbox.parentElement || !checkbox.parentElement.parentElement || 'LI' != checkbox.parentElement.parentElement.tagName) return [];
            var li = checkbox.parentElement.parentElement;
            var children = [];
            for (var i = 0; i < li.children.length; i++) {
                var n1 = li.children[i];
                if ('UL' != n1.tagName) continue;
                for (var j = 0; j < n1.children.length; j++) {
                    var n2 = n1.children[j];
                    if ('LI' != n2.tagName) continue;
                    for (var k = 0; k < n2.children.length; k++) {
                        var n3 = n2.children[k];
                        for (var l = 0; l < n3.children.length; l++) {
                            var n4 = n3.children[l];
                            if ('INPUT' == n4.tagName) children.push(n4);
                        }
                    }
                }
            }
            return children;
        }
        function updateParentsChecks(checkbox) {
            for (var parentCheckbox = checkbox.parentCheckbox; parentCheckbox; parentCheckbox = parentCheckbox.parentCheckbox) {
                var checkedNumberOfResults = 0;
                var uncheckedNumberOfResults = 0;
                var uncheckedAll = true;
                var notAllChecked = false;
                var totalNumberOfResults = 0;
                var indeterminateChildren = 0;
                var children = getChildrenChecks(parentCheckbox);
                for (var i = 0; i < children.length; i++) {
                    var n = children[i];
                    if (n.notAllChecked) notAllChecked = true;
                    if (n.indeterminate) {
                        indeterminateChildren++;
                        uncheckedAll = false;
                        notAllChecked = true;
                    } else if (n.checked) {
                        checkedNumberOfResults += n.numberOfResults;
                        totalNumberOfResults += n.numberOfResults;
                        uncheckedAll = false;
                    } else {
                        uncheckedNumberOfResults += n.numberOfResults;
                        totalNumberOfResults += n.numberOfResults;
                        notAllChecked = true;
                    }
                }
                if (checkedNumberOfResults == parentCheckbox.numberOfResults && !notAllChecked) {
                    parentCheckbox.indeterminate = false;
                    parentCheckbox.checked = true;
                    parentCheckbox.notAllChecked = false;
                } else if (   uncheckedNumberOfResults == parentCheckbox.numberOfResults
                           || (parentCheckbox.indeterminate && uncheckedAll)) {
                    parentCheckbox.indeterminate = false;
                    parentCheckbox.checked = false;
                } else if (   totalNumberOfResults == parentCheckbox.numberOfResults
                           || (   !parentCheckbox.indeterminate
                               && !parentCheckbox.checked
                               && (indeterminateChildren || checkedNumberOfResults || notAllChecked))){
                    parentCheckbox.indeterminate = true;
                } else {
                    parentCheckbox.notAllChecked = notAllChecked;
                }
            }
        }

        function toMenu(master, items, data, chooseFn, applyFn, cancelFn, armFn) {
            //var isNotInputField = master.nodeName != 'INPUT';
            var cursorIndex = 0;
            var isInit = 0;
            master.onkeydown = function(e) {
                e = e || window.event;
                var key = e.keyCode || e.charCode;

                if (   cursorIndex > 0
                    && getClassName(items[cursorIndex-1]) == items[cursorIndex-1].z) {
                    cursorIndex = 0;
                }

                // RIGHT (key: 39) or TAB without SHIFT (key: 9) to apply
                if (applyFn && (key == 39 || (key == 9 && !e.shiftKey))) {
                    if (applyFn(data, key)) preventDefault(e);
                }

                // ESC to cancel
                if (cancelFn && key == 27) {
                    cancelFn();
                    preventDefault(e);
                    return;
                }

                // ENTER to choose
                if (key == 13 && cursorIndex > 0) {
                    preventDefault(e);
                    stopPropagation(e);
                    setClassName(items[cursorIndex-1], items[cursorIndex-1].z);
                    chooseFn(data[cursorIndex-1]);
                    cursorIndex = 0;
                    return;
                }

                // select by UP and DOWN
                if (key != 40 && key != 38) return;
                preventDefault(e);
                var isDown = key == 40;
                if (cursorIndex > 0) {
                    setClassName(items[cursorIndex-1], items[cursorIndex-1].z);
                }
                cursorIndex = cursorIndex < 1
                              ? (isDown ? 1 : items.length)
                              : (cursorIndex + (isDown ? 1 : -1)) % (items.length + 1);
                if (cursorIndex > 0) {
                   var item = items[cursorIndex-1];
                   setClassName(item, item.z + ' z');
                   if (armFn) armFn(item, data[cursorIndex-1], 0);
                }
            }
            for (var i = 0; i < items.length; i++) {
                items[i].z = getClassName(items[i]);
                items[i].onmousedown = function() {setTimeout(function() {if (master && !master.hasFocus) master.focus()}, 42)};
                items[i].onmouseup = items[i].ontouchend = function(a, b) {return function(e) {preventDefault(e); if (!a.canceled) {chooseFn(b); setClassName(a, a.z); cursorIndex = 0}}}(items[i], data[i]);
                items[i].onmouseover = items[i].ontouchstart = function(a, b, c) {return function() {if (!isInit) return; if (cursorIndex > 0) setClassName(items[cursorIndex-1], items[cursorIndex-1].z); setClassName(a, a.z + ' z'); cursorIndex = b; a.canceled = ''; if (armFn && b > 0) armFn(a, c, 1)}}(items[i], i+1, data[i]);
                items[i].onmouseout = function(a) {return function() {setClassName(a, a.z)}}(items[i]);
            }
            setTimeout(function() {isInit = 1; }, 142);
        }

        function addHighlightedText(element, text, searchWord) {
            var searchWordLowerCase = searchWord.toLowerCase();
            var textLowerCase = text.toLowerCase();
            var hIndex = textLowerCase.indexOf(searchWordLowerCase);
            if (hIndex < 0) {
                element.appendChild(document.createTextNode(text));
                return;
            }

            var lastEnd = 0;
            while (hIndex >=0) {
                element.appendChild(document.createTextNode(text.substring(lastEnd, hIndex)));
                lastEnd = hIndex + searchWord.length;
                if (   hIndex == 0
                    || text.substring(hIndex - 1, hIndex).replace(/\w/, "").length != 0) {
                    var strong = createElement(element, 'strong');
                    strong.appendChild(document.createTextNode(text.substring(hIndex, lastEnd)));
                } else {
                    element.appendChild(document.createTextNode(text.substring(hIndex, lastEnd)));
                }
                hIndex = textLowerCase.indexOf(searchWordLowerCase, lastEnd);
            }
            element.appendChild(document.createTextNode(text.substring(lastEnd)));
        }

    }

    function searchFullByHash(hash) {
        var url =   SEARCH_BASE_URL + hash.substring(3) + '&maxHits=' + SEARCH_HITS_MAX
                  + (hash.indexOf('&toc=') < 0 ? '' : '&quickSearch=true&quickSearchType=QuickSearchToc');
        window.frames.c.location = url;

        // fill search field
        var searchWord = getParams(hash)['#q'];
        if (searchWord && searchWord != getElementById('q').value) {
            getElementById('q').value = searchWord;
        }

        // TODO set search socpe?

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Menu

    function createMenu() {

        // menu
        var menu = createOverlay(9, 1);
        var overlay = createOverlay(8);
        menu.id = 'a';
        menuStyle = menu.style;
        menu.a = function(e) { preventDefault(e); overlay.a(); menuStyle.width = '270px'; };
        menu.o = function(e) { preventDefault(e); overlay.o(); menuStyle.width = '0'; };
        menu.o();
        addEvent(overlay, 'click', menu.o);
        function createMenuItem(label, description, fn, id, href, parent) {
            var item = createElement(parent ? parent : menu, fn ? 'button' : 'a', 'b', label);
            item.href = href ? (BASE_URL + href) : '#';
            item.target = 'c';
            item.title = description;
            if (id) {
                item.id = id;
            }
            addEvent(item, 'click', function(e) {
                if (fn) { preventDefault(e); fn(e); }
                if (href) { getElementById('c').contentDocument.location.href = BASE_URL + href; }
                if (!parent) menu.o();
            });
            return item;
        }

        // "x" button
        var closeMenuButton = createElement(createElement(menu, 0, 'e'), 'a', 'b');
        closeMenuButton.href = '#';
        addEvent(closeMenuButton, 'click', menu.o);
        closeMenuButton.alt = '<%js:menu_button_close_description%>';
        closeMenuButton.title = '<%js:menu_button_close_description%>';
        setInnerHtml(closeMenuButton, MENU_CLOSE_ICON);

        // "Highlight search terms" dummy
        function HighlightConnector() {};
        HighlightConnector.prototype.setButtonState = function(/*name, state*/) {
            // dummy for highlight() in org.eclipse.help.webapp/advanced/highlight.js
        };
        window.ContentToolbarFrame = new HighlightConnector();
        var highlight = createMenuItem(0, '<%js:menu_item_highlighting_label_description%>', toggleHighlight, 'ah');
        createElement(highlight, 'span', 'hl', '<%js:menu_item_highlighting_label_prefix%>');
        createElement(highlight, 'span', 'hs', ' ');
        createElement(highlight, 'span', 'ht', '<%js:menu_item_highlighting_label_highlighting%>');
        createElement(highlight, 'span', 'hs', ' ');
        createElement(highlight, 'span', 'hl', '<%js:menu_item_highlighting_label_suffix%>');
        toggleHighlight(0, 1);

        // "Font: - +"
        if (MENU_FONT_SIZING) {
            var fontSizer = createElement(menu);
            fontSizer.id = 'af';
            createElement(fontSizer, 'span', 0, '<%js:menu_item_font_label%>');
            createMenuItem('\u2013', '<%js:menu_item_font_minus_button_description%>', function() { setFontSize(0); }, 'afm', 0, fontSizer);
            createMenuItem('+', '<%js:menu_item_font_plus_button_description%>', function() { setFontSize(1); }, 'afp', 0, fontSizer);
        }

        // "Bookmarks..."
        if (embeddedMode) {
            createMenuItem('<%js:menu_item_bookmarks_label%>', '<%js:menu_item_bookmarks_description%>', function() {
                bookmarksPage.s(1);
            });
        }

        // "Search scopes..."
        createMenuItem('<%js:menu_item_scopes_label%>', '<%js:menu_item_scopes_description%>', function() {
            scopesPage.s(1);
        });

        // "Print topic..."
        createMenuItem('<%js:menu_item_print_topic_label%>', '<%js:menu_item_print_topic_description%>', function() {
            try {
                getElementById('c').contentWindow.print();
            } catch (e) {
            }
        }, 'ap');

        // "Print chapter..."
        createMenuItem('<%js:menu_item_print_chapter_label%>', '<%js:menu_item_print_chapter_description%>', printChapter, 'app');

        // "Help"
        if (MENU_HELP) {
            createMenuItem(MENU_HELP_LABEL, MENU_HELP_DESCRIPTION, 0, 'ai', MENU_HELP);
        }

        // "About"
        if (MENU_ABOUT) {
            createMenuItem('<%js:menu_item_about_label%>', '<%js:menu_item_about_description%>', 0, 'aa', 'about.html');
        }

        // show menu button
        var menuButton = createElement(getElementById('h'), 'a', 'b');
        menuButton.href = '#';
        menuButton.alt = '<%js:menu_button_open_description%>';
        menuButton.title = '<%js:menu_button_open_description%>';
        setInnerHtml(menuButton, MENU_ICON);
        addEvent(menuButton, 'click', menu.a);

    }

    function toggleHighlight(_event, initalize) {

        var enableHighlighting = 'false' == getCookie('highlight');
        if (initalize) {
            enableHighlighting = !enableHighlighting;
        } else {
            setCookie('highlight', enableHighlighting ? 'true' : 'false');
            var contentFrameWindow = getElementById('c').contentWindow;
            if (contentFrameWindow && contentFrameWindow.highlight && contentFrameWindow.toggleHighlight) {
                contentFrameWindow.toggleHighlight();
                contentFrameWindow.highlight();
            }
        }
        setClassName(getElementById('ah'), enableHighlighting ? 'b x' : 'b');
    }

    function setFontSize(increase, initalize, updateContentFrameOnly) {
        if (!MENU_FONT_SIZING) return;
        var newFontSize;
        var contentFrameDocument = getElementById('c').contentWindow.document;
        var contentFrameDocumentElement = contentFrameDocument.documentElement || contentFrameDocument.body;
        var toc = document.getElementById('t');
        var tocStyle = getComputedStyle(toc, null).getPropertyValue('font-size');
        var tocFontSize = parseFloat(tocStyle);
        if (initalize) {
            newFontSize = getCookie('font-size');
        } else if (increase && !initalize && tocFontSize < 64) {
            newFontSize = (tocFontSize + 3);
        } else if (!increase && !initalize && tocFontSize > 12) {
            newFontSize = (tocFontSize - 3);
        }
        if (!newFontSize) return;
        contentFrameDocumentElement.style.fontSize = newFontSize + 'px';
        if (!updateContentFrameOnly) {
            toc.style.fontSize = newFontSize + 'px';
            searchPage.style.fontSize = newFontSize + 'px';
        }
        setCookie('font-size', newFontSize, 365);
    }

    function printChapter() {
        var contentElement = getElementById('c');
        var contentWindow = contentElement.contentWindow;
        var topicHref = contentWindow.location.href;
        if (!topicHref) return;
        var dummy = document.createElement('a');
        dummy.href = BASE_URL + 'x';
        var topic = topicHref.substring(dummy.href.length - 2);
        if (topic.length > 7 && '/topic/' == topic.substring(0, 7)) topic = topic.substring(6);
        else if (topic.length > 5 && '/nav/' == topic.substring(0, 5)) topic = '/..' + topic;
        else if (topic.length > 8 && ('/rtopic/' == topic.substring(0, 8) || '/ntopic/' == topic.substring(0, 8))) topic = topic.substring(7);
        var w = contentWindow.innerWidth || contentWindow.document.body.clientWidth;
        var h = contentWindow.innerHeight || contentWindow.document.body.clientHeight;
        var x = window.screenX;
        var y = window.screenY;
        for (var e = contentElement; !!e; e = e.offsetParent) {
            if (e.tagName == "BODY") {
                var xScroll = e.scrollLeft || document.documentElement.scrollLeft;
                var yScroll = e.scrollTop || document.documentElement.scrollTop;
                x += (e.offsetLeft - xScroll + e.clientLeft);
                y += (e.offsetTop  - yScroll + e.clientTop);
            } else {
                x += (e.offsetLeft - e.scrollLeft + e.clientLeft);
                y += (e.offsetTop  - e.scrollTop  + e.clientTop);
            }
        }
        var anchor = '';
        var anchorStart = topic.indexOf('#');
        if (anchorStart > 0) {
            anchor = '&anchor=' + topic.substr(anchorStart + 1);
            topic = topic.substr(0, anchorStart);
        }
        var query = '';
        var queryStart = topic.indexOf('?');
        if (queryStart > 0) {
            query = '&' + topic.substr(queryStart + 1);
            topic = topic.substr(0, queryStart);
        }
        window.open(BASE_URL + 'advanced/print.jsp?topic=' + topic + query + anchor, 'printWindow', 'directories=yes,location=no,menubar=yes,resizable=yes,scrollbars=yes,status=yes,titlebar=yes,toolbar=yes,width=' + w + ',height=' + h + ',left=' + x + ',top=' + y);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Tree

    function createTree(element, contentProvider, labelProvider, selectable) {
        var root = createElement(element, 0, 'tree');
        function createNode(parent, node) {
            contentProvider(node, createNodeChildrenFn(parent, node));
        };
        function createNodeChildrenFn(parent) {
            return function(children, open) {
                var ul = createElement(parent, 'ul');
                for (var i = 0; children && i < children.length; i++) {
                    var li = createElement(ul, 'li', 'closed');
                    li.p = parent;
                    var child = children[i];
                    if (!child.l) {

                        // c(hildren): yes
                        li.c = 1;

                        // i(nit) function to load children
                        li.i = (function(li, node) {
                            return function() {
                                createNode(li, node);
                                li.i = 0;
                            };
                        })(li, child.n);

                        // handle (to toggle subtree)
                        var handle = createElement(li, 'span', 'h');
                        handle.innerHTML = TREE_HANDLE;
                        addEvent(handle, 'click', (function(li) {
                            return function(e) {
                                toggleLi(li);
                                stopPropagation(e);

                                // focus next element (to avoid losing focus since the handle cannot be focused)
                                try {
                                    li.childNodes[1].focus();
                                } catch(e) {}

                            };
                        })(li));

                    }
                    var label = labelProvider(li, child.n);
                    setClassName(label, 'l');
                    if (selectable) {
                        addEvent(label, 'click', (function(li) {
                            return function(e) {
                                if (element.s === li) return;
                                element.x(li);
                                stopPropagation(e);
                            };
                        })(li));
                    }
                    addEvent(label, 'dblclick', (function(li) {
                        return function(e) {
                            toggleLi(li);
                            stopPropagation(e);
                        }
                    })(li));
                    if (open || getClassName(li) == 'open') toggleLi(li);
                }
            }
        };
        element.x = function(li, scrollArea, closeSiblings) {
            for (var n = element.s; isLi(n); n = n.p) {
                n.xx = 0;
                n.x = 0;
                updateLiClasses(n);
            }
            element.s = li;
            if (!li) return;
            li.xx = 1;
            updateLiClasses(li);
            for (var n = li.p; isLi(n); n = n.p) {
                if (!n.o) {
                    toggleLi(n);
                }
                n.x = 1;
                updateLiClasses(n);
            }

            // close siblings of the selected node and its ancestors
            for (var current = li; closeSiblings && current.tagName == 'LI'; current = current.p) {
                var ul = getParentElement(current);
                for (var i = 0; i < ul.childNodes.length; i++) {
                    var n = ul.childNodes[i];
                    if (current !== n && n.o) {
                        toggleLi(n);
                    }
                }
            }

            // scroll label into view if needed
            for (var i = 0; i < li.childNodes.length; i++) {
                var n = li.childNodes[i];
                if (n.tagName == 'A' || n.tagName == 'BUTTON') {
                    scrollIntoViewIfNeeded(scrollArea, n);
                    break;
                }
            }

            // update search field book scope
            if (updateScopeByToc && li.toc) {
                updateScopeByToc(li);
            }

        };
        element.y = function(href, scrollArea, closeSiblings) {
            contentProvider({topic: href}, function(children) {
                if (!children || children.length != 1 || !children[0].n || !children[0].n.y) {

                    // deselect current selection
                    element.x(0);

                    return;
                }
                var expandPath = children[0].n.y.split('_');
                var parentLi = root;
                var parentNode = {};
                var childNodes = children;
                for (var i = 0; i < expandPath.length; i++) {
                    var nr = parseInt(expandPath[i]);
                    if (parentLi.i) {
                        (createNodeChildrenFn(parentLi, parentNode))(childNodes);
                        parentLi.i = 0;
                    }
                    var ul = 0;
                    for (var j = 0; j < parentLi.childNodes.length; j++) {
                        var n = parentLi.childNodes[j];
                        if (n.tagName == 'UL') {
                            ul = n;
                            break;
                        }
                    }
                    var li = 0;
                    var liNr = 0;
                    for (var j = 0; j < ul.childNodes.length; j++) {
                        var n = ul.childNodes[j];
                        if (n.tagName == 'LI') {
                            if (liNr == nr) {
                                li = n;
                                break;
                            }
                            liNr++;
                        }
                    }
                    if (i == expandPath.length - 1) {
                        element.x(li, scrollArea, closeSiblings);
                    }
                    parentLi = li;
                    var child = childNodes[i == 0 ? 0 : nr];
                    parentNode = child.n;
                    childNodes = child.c;
                }
            });
        }
        createNode(root);

        // handling via the keys up, down, left, right, home and end
        addEvent(element, 'keydown', function(e) {
            var keyCode = e.keyCode || window.event.keyCode;
            if (keyCode < 35 || keyCode > 40) return;

            // compute focused tree node
            var li;
            for (li = e.target || e.srcElement; li && li !== root; ) {
                if (isLi(li)) break;
                li = getParentElement(li);
            }
            if (!li) return;

            // left/right
            if (keyCode == 37 || keyCode == 39) {
                if (keyCode == 37 ^ !li.o) {
                    toggleLi(li);
                } else if (keyCode == 37) {
                    focusTreeNode(li.p);
                } else {
                    focusFirstChildNode(li);
                }

            // down
            } else if(keyCode == 40) {

                // expanded? -> focus first child, ...
                if (li.o) {
                    focusFirstChildNode(li);
                    preventDefault(e);
                    return;
                }

                // ...otherwise -> focus next sibling at this or higher level
                for (var level = li; isLi(level); level = level.p) {
                    for (var next = getNextSibling(level); next; next = getNextSibling(next)) {
                        if (!isLi(next)) continue;
                        focusTreeNode(next);
                        preventDefault(e);
                        return;
                    }
                }

            // up
            } else if(keyCode == 38) {

                // previous sibling? -> focus previous sibling, ...
                for (var prev = getPreviousSibling(li); prev !== null; prev = getPreviousSibling(prev)) {
                    if (!isLi(prev)) continue;
                    focusDeepestVisibleChild(prev);
                    preventDefault(e);
                    return;
                }

                // ...otherwise -> focus parent
                focusTreeNode(li.p);

            // home
            } else if(keyCode == 36) {
                focusFirstChildNode(root);

            // end
            } else if(keyCode == 35) {
                focusDeepestVisibleChild(root);

            }
            preventDefault(e);

        });
        element.f = function() {
            focusTreeNode(element.s);
        };
        if (selectable) {
            addEvent(element, 'click', element.f);
        }

        // visitor pattern: visit the nearby nodes first (first the selected node with its subtree deep-first, then the
        // parent, then the siblings with their subtrees and repeating for each higher level with the parent (if any)
        // and the siblings, without the already processed node and its subtree)
        element.v = function(vistorFn) {
            var todoSiblingsAndAncestorsOf = element.s;
            var todoSubtrees = element.s ? [element.s] : toArray(root.childNodes[0].childNodes);
            while (todoSubtrees.length || todoSiblingsAndAncestorsOf) {
                var next;

                // subtree done? -> go one level up
                if (!todoSubtrees.length) {
                    var sibling = todoSiblingsAndAncestorsOf;
                    while (sibling = getNextSibling(sibling)) {
                        todoSubtrees.unshift(sibling);
                    }
                    sibling = todoSiblingsAndAncestorsOf;
                    while (sibling = getPreviousSibling(sibling)) {
                        todoSubtrees.unshift(sibling);
                    }
                    if (todoSiblingsAndAncestorsOf.p && todoSiblingsAndAncestorsOf.p.tagName == 'LI') {
                        next = todoSiblingsAndAncestorsOf = todoSiblingsAndAncestorsOf.p;
                    } else {
                        todoSiblingsAndAncestorsOf = 0;
                        continue;
                    }
                } else {
                    next = todoSubtrees.pop();

                    // add children of next (if any)
                    for (var i = 0; i < next.childNodes.length; i++) {
                        var n = next.childNodes[i];
                        if (n.tagName != 'UL') continue;
                        for (var j = n.childNodes.length - 1; j >= 0; j--) {
                            var m = n.childNodes[j];
                            if (isLi(m)) {
                                todoSubtrees.push(m);
                            }
                        }
                    }

                }

                // call visitor
                if (!vistorFn(next)) return;

            }

        }
        function toArray(nodeList) {
            var result = [];
            for (var i = 0; i < nodeList.length; i++) {
                result.push(nodeList[i]);
            }
            return result;
        }

        function toggleLi(li) {
            if (!li.c) return;
            if (li.i) {
                li.i();
            }
            li.o = !li.o;
            updateLiClasses(li);
        }
        function isLi(element) {
            return element && element.tagName == 'LI'
        }
        function updateLiClasses(li) {
            setClassName(li, (li.o ? 'open': 'closed') + (li.xx ? ' xx' : '') + (li.x ? ' x' : ''))
        }
        function focusFirstChildNode(li) {
            for (var i = 0; i < li.childNodes.length; i++) {
                var n = li.childNodes[i];
                if (n.tagName != 'UL') continue;
                for (var j = 0; j < n.childNodes.length; j++) {
                    var m = n.childNodes[j];
                    if (isLi(m)) {
                        focusTreeNode(m);
                        return;
                    }
                }
            }
        }
        function focusDeepestVisibleChild(li) {
            for (var i = 0; li.o && i < li.childNodes.length; i++) {
                var n = li.childNodes[i];
                if (n.tagName != 'UL') continue;
                for (var j = n.childNodes.length - 1; j >= 0; j--) {
                    var m = n.childNodes[j];
                    if (!isLi(m)) continue;
                    focusDeepestVisibleChild(m);
                    return;
                }
            }
            focusTreeNode(li);
        }
        function focusTreeNode(li) {
            if (!li) return;
            for (var i = 0; i < li.childNodes.length; i++) {
                var n = li.childNodes[i];
                if (n.tagName != 'A' && n.tagName != 'BUTTON') continue;
                try {
                    n.focus();
                } catch(e) {}
                return;
            }
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Overlay

    function createOverlay(zIndex, withoutStyles) {
        var overlay = createElement();
        var header =  getElementById('h');
        getParentElement(header).insertBefore(overlay, header);
        if (!withoutStyles) {
            var overlayStyle = overlay.style;
            overlayStyle.display = 'none';
            overlayStyle.zIndex  = zIndex ? zIndex : 1;
            overlayStyle.position = 'absolute';
            overlayStyle.height = '100%';
            overlayStyle.width = '100%';
        }
        overlay.a = function() { overlayStyle.display = 'block'; };
        overlay.o = function() { overlayStyle.display = 'none'; };
        return overlay;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Utility functions (polyfill/retrofit functions see below)

    function addEvent(element, type, fn) {
        if (element.addEventListener) {
            element.addEventListener(type, fn, false);
        } else if (element.attachEvent) {
            element['e' + type + fn] = fn;
            element[type + fn] = function() {
                element['e' + type + fn](window.event);
            }
            element.attachEvent('on' + type, element[type + fn]);
        }
    }

    function decodeHtml(htmlString) {
        if (!htmlString) return htmlString;
        var element = createElement();
        setInnerHtml(element, htmlString);
        return element.textContent || element.innerText;
    }

    function getElementById(id) {
        return document.getElementById(id);
    }

    function getParentElement(element) {
        return element.parentElement;
    }

    function getPreviousSibling(element) {
        return element.previousSibling;
    }

    function getNextSibling(element) {
        return element.nextSibling;
    }

    function getClassName(element) {
        return element.className;
    }

    function setClassName(element, value) {
        element.className = value;
        return element;
    }

    function getAttribute(element, attribute) {
        return element.getAttribute(attribute);
    }

    function setAttribute(element, attribute, value) {
        element.setAttribute(attribute, value);
        return element;
    }

    function setInnerHtml(element, innerHtml) {
        element.innerHTML = innerHtml ? innerHtml : '';
    }

    function preventDefault(e) {
        e = e || window.event;
        if (!e) return;
        try {
            if (e.preventDefault) e.preventDefault();
            e.returnValue = false;
        } catch(e) {}
    }

    function stopPropagation(e) {
        e = e || window.event;
        if (!e) return;
        e.cancelBubble = true;
        if (e.stopPropagation) e.stopPropagation();
    }

    function getParams(queryPart) {
        var params = {};
        queryPart.replace(/(?:^|&+)([^=&]+)=([^&]*)/gi,
            function(_match, group1Param, group2Value) { params[group1Param] = decodeURIComponent(group2Value.replace(/\+/g, ' ')); });
        return params;
    }

    function getCookie(cookieName, defaultValue) {
        var name = cookieName + "=";
        var decodedCookie = decodeURIComponent(document.cookie);
        var allCookies = decodedCookie.split(';');
        for (var i = 0; i < allCookies.length; i++) {
            var cookie = allCookies[i];
            while (cookie.charAt(0) == ' ') {
                cookie = cookie.substring(1);
            }
            if (cookie.indexOf(name) == 0) {
                return cookie.substring(name.length, cookie.length);
            }
        }
        return defaultValue;
    }

    function setCookie(cookieName, value) {
        var d = new Date();
        d.setTime(d.getTime() + (365 * 24 * 60 * 60 * 1000));
        var expires = 'expires=' + d.toUTCString();
        document.cookie = cookieName + '=' + value + ';' + expires + ';path=/;samesite=strict';
    }

    var openRequests = {};
    function remoteRequest(url, callbackFn, cancelId) {
        var request = new XMLHttpRequest();
        if (callbackFn) request.onreadystatechange = function() {
            if (request.readyState == 4 && request.status == 200) callbackFn(request.responseText);
        }
        request.open('GET', url);
        request.send();
        if (cancelId) {
            if (openRequests[cancelId] && openRequests[cancelId].abort) openRequests[cancelId].abort();
            openRequests[cancelId] = request;
        }
    }

    var parseXml;
    if (typeof window.DOMParser != 'undefined') {
        parseXml = function(xmlStr) {
            return (new window.DOMParser()).parseFromString(xmlStr, 'text/xml');
        };
    } else if (   typeof window.ActiveXObject != 'undefined'
               && new window.ActiveXObject('Microsoft.XMLDOM')) {
        parseXml = function(xmlStr) {
            var xmlDoc = new window.ActiveXObject('Microsoft.XMLDOM');
            xmlDoc.async = 'false';
            xmlDoc.loadXML(xmlStr);
            return xmlDoc;
        };
    }

    function scrollIntoViewIfNeeded(scrollArea, element) {
        if (!scrollArea) return;
        try {
            var scrollAreaBoundaries = scrollArea.getBoundingClientRect();
            var elementBoundaries = element.getBoundingClientRect();
            if (   elementBoundaries.top >= scrollAreaBoundaries.top
                && elementBoundaries.bottom <= scrollAreaBoundaries.bottom) return;

            scrollArea.scrollTop += scrollArea.c

                                    // show element in upper third
                                    ? ((  (elementBoundaries.bottom - scrollAreaBoundaries.bottom)
                                        + (elementBoundaries.top - scrollAreaBoundaries.top) * 2) / 3)

                                    // scroll as less as possible
                                    : (elementBoundaries.bottom <= scrollAreaBoundaries.bottom
                                       ? elementBoundaries.top - scrollAreaBoundaries.top
                                       : elementBoundaries.bottom - scrollAreaBoundaries.bottom);

        } catch (e) {}
    }

    function arrayContainsPrefix(array, value) {
        for (var i = 0; i < array.length; i++) {
            if (array[i].length <= value.length && value.substring(0, array[i].length) == array[i]) return true;
        }
        return false;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Polyfill/retrofit utility functions

    function createElement(parent, name, className, text) {
        var element = document.createElement(name ? name : 'div');
        if (parent) {
            try {
                parent.appendChild(element);
            } catch(e) {

                // POLYFILL IE<=8
                // HTML5 semantic tags (https://www.w3schools.com/html/html5_browsers.asp)
                // polyfill adding child element to the empty HTML 5 semantic element 'aside' (for IE 8 and lower)
                if (parent.tagName == 'ASIDE') {
                    var pp = parent.parentElement;
                    var rest = [];
                    for (var i = 0; i < pp.childNodes.length; i++) {
                        if (parent === pp.childNodes[i]) {
                            if (div) continue;
                            var div = document.createElement('div');
                            for (var j = 0; j < parent.attributes.length; j++) {
                                var attribute = parent.attributes[j];
                                if ('' + parent.getAttribute(attribute.name) != '' + div.getAttribute(attribute.name)
                                    && !('' + parent.getAttribute(attribute.name) == 'null'
                                         && '' + div.getAttribute(attribute.name) == '')) {
                                    setAttribute(div, attribute.name, attribute.value);
                                }
                            }
                            pp.removeChild(parent);
                            div.appendChild(element);
                            rest.push(div);
                            i--;
                            continue;
                        }
                        if (rest && pp.childNodes[i].tagName == '/' + parent.tagName) {
                            pp.removeChild(pp.childNodes[i]);
                            i--;
                            continue;
                        }
                        if (!rest) continue;
                        rest.push(pp.removeChild(pp.childNodes[i]));
                        i--;
                    }
                    for (var i = 0; i < rest.length; i++) pp.appendChild(rest[i]);
                } else throw e;

            }
        }
        if (className) {
            setClassName(element, className);
        }
        if (text) {
            element.appendChild(document.createTextNode(text));
        }
        return element;
    }

}(window, document));
