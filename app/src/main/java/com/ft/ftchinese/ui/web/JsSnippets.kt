package com.ft.ftchinese.ui.web

import com.ft.ftchinese.model.enums.Tier

object JsSnippets {
    fun lockerIcon(tier: Tier?): String {
        val prvl = when (tier) {
            Tier.STANDARD -> """['premium']"""
            Tier.PREMIUM -> """['premium', 'EditorChoice']"""
            else -> "[]"
        }

        return """
        (function() {
            window.gPrivileges=$prvl;
            updateHeadlineLocks();
        return window.gPrivileges;
        })()
    """.trimIndent()
    }

    val openGraph = """
    (function getOpenGraph() {
        var metaElms = document.getElementsByTagName('meta');
        var graph = {};
        var standfirst = "";
        for (var index = 0; index < metaElms.length; index++) {
            var elm = metaElms[index];
            if (elm.hasAttribute("name")) {
                var nameVal = elm.getAttribute("name")
                switch (nameVal) {
                    case "keywords":
                        graph.keywords = elm.getAttribute("content");
                        break;
                    case "description":
                        standfirst = elm.getAttribute("content");
                        break;
                }
                continue;
            }
            if (!elm.hasAttribute('property')) {
                continue;
            }
            var prop = elm.getAttribute('property');
            if (!prop.startsWith('og:')) {
                continue;
            }
            var key = prop.split(":")[1];
            var value = elm.getAttribute('content');
            graph[key] = value;
        }

        if (!graph["title"]) {
            graph["title"] = document.title;
        }

        if (!graph["description"]) {
            graph["description"] = standfirst;
        }

        return graph;
    })();
    """.trimIndent()

    // Call JS function in a very short timeout
    // (you can actually set the timeout to zero, but let's be safe here)
    // so you don't have to wait for page loaded
    // and search result comes out almost instantly.
    // Loaded content is a list of links.
    // Navigation is handled by analyzing the
    // content of each url.
    fun search(keyword: String): String {
        return """
           search('$keyword');
        """.trimIndent()
    }
}
