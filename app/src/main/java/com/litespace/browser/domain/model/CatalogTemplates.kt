package com.litespace.browser.domain.model

object CatalogTemplates {
    val builtInTemplates: List<CatalogTemplate> = listOf(
        CatalogTemplate(
            name = "Wikipedia",
            startUrl = "https://en.m.wikipedia.org",
            category = LiteAppCategory.NEWS,
            iconText = "W",
            iconColorHex = "#37474F",
            feedUrl = "https://en.wikipedia.org/w/api.php?action=featuredfeed&feed=featured&feedformat=atom",
            descriptionEn = "Fast encyclopedia reader with zero tracking and dark mode support.",
            descriptionFa = "دانشنامه آزاد با حالت مطالعه سریع و بدون ردیاب."
        ),
        CatalogTemplate(
            name = "Hacker News",
            startUrl = "https://news.ycombinator.com",
            category = LiteAppCategory.DEVELOPER,
            iconText = "HN",
            iconColorHex = "#E65100",
            feedUrl = "https://news.ycombinator.com/rss",
            descriptionEn = "Minimal tech & startup news with RSS background notification monitor.",
            descriptionFa = "اخبار فناوری و برنامه‌نویسی با قابلیت پایش خوراک RSS."
        ),
        CatalogTemplate(
            name = "GitHub Mobile Web",
            startUrl = "https://github.com",
            category = LiteAppCategory.DEVELOPER,
            iconText = "GH",
            iconColorHex = "#24292F",
            descriptionEn = "Isolated developer workspace for repositories, PRs, and issues.",
            descriptionFa = "محیط ایزوله گیت‌هاب برای مدیریت مخازن و کدها."
        ),
        CatalogTemplate(
            name = "Mastodon Social",
            startUrl = "https://mastodon.social",
            category = LiteAppCategory.SOCIAL,
            iconText = "M",
            iconColorHex = "#563ACC",
            descriptionEn = "Federated social web app with Web Notification API bridge enabled.",
            descriptionFa = "شبکه اجتماعی غیرمتمرکز ماستودون با پشتیبانی از اعلان‌های وب."
        ),
        CatalogTemplate(
            name = "DuckDuckGo Search",
            startUrl = "https://duckduckgo.com",
            category = LiteAppCategory.PRODUCTIVITY,
            iconText = "DD",
            iconColorHex = "#D84315",
            descriptionEn = "Private search engine with auto-clear cookies on exit.",
            descriptionFa = "موتور جستجوی خصوصی با پاکسازی خودکار هنگام خروج."
        ),
        CatalogTemplate(
            name = "OpenStreetMap",
            startUrl = "https://www.openstreetmap.org",
            category = LiteAppCategory.PRODUCTIVITY,
            iconText = "OS",
            iconColorHex = "#2E7D32",
            descriptionEn = "Community-driven world map with optional on-demand geolocation.",
            descriptionFa = "نقشه جهانی متن‌باز با مدیریت دقیق مجوز مکان‌یابی."
        )
    )
}
