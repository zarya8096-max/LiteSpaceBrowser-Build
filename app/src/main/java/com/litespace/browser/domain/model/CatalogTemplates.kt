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
            name = "Digikala (دیجی‌کالا)",
            startUrl = "https://www.digikala.com",
            category = LiteAppCategory.SHOPPING,
            iconText = "DK",
            iconColorHex = "#E6123D",
            descriptionEn = "Isolated shopping Lite App without background battery drain.",
            descriptionFa = "نسخه سبک و بدون مصرف باتری پس‌زمینه برای خرید آنلاین."
        ),
        CatalogTemplate(
            name = "Divar (دیوار)",
            startUrl = "https://divar.ir",
            category = LiteAppCategory.SHOPPING,
            iconText = "DV",
            iconColorHex = "#A62626",
            descriptionEn = "Fast classifieds Lite App with isolated cookie vault.",
            descriptionFa = "لایت‌اپ سریع دیوار با کوکی و حافظه کاملاً ایزوله."
        ),
        CatalogTemplate(
            name = "Telegram Web",
            startUrl = "https://web.telegram.org",
            category = LiteAppCategory.SOCIAL,
            iconText = "TG",
            iconColorHex = "#0088CC",
            descriptionEn = "Multi-account web messenger using isolated LiteSpace profiles.",
            descriptionFa = "پیام‌رسان وب تلگرام با قابلیت چندحسابه از طریق پروفایل ایزوله."
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
        )
    )

    val builtInExtensions: List<BuiltInUserExtension> = listOf(
        BuiltInUserExtension(
            id = "unlock_copy",
            titleEn = "Unlock Copy & Selection",
            titleFa = "فعال‌سازی کپی و انتخاب متن",
            descriptionEn = "Re-enables text selection, copy, and context menus on restricted pages.",
            descriptionFa = "حذف محدودیت انتخاب متن و راست‌کلیک در سایت‌های قفل‌شده.",
            cssSnippet = "* { -webkit-user-select: text !important; user-select: text !important; }",
            jsSnippet = """
                (function() {
                    ['copy', 'cut', 'contextmenu', 'selectstart', 'dragstart'].forEach(function(ev) {
                        window.addEventListener(ev, function(e) { e.stopPropagation(); }, true);
                    });
                })();
            """.trimIndent()
        ),
        BuiltInUserExtension(
            id = "hide_cookie_banners",
            titleEn = "Hide Cookie & GDPR Popups",
            titleFa = "مخفی‌سازی بنرهای کوکی",
            descriptionEn = "Hides common cookie consent and GDPR overlay banners via CSS.",
            descriptionFa = "پاکسازی پاپ‌آپ‌های مزاحم کوکی و رضایت‌نامه‌های تبلیغاتی.",
            cssSnippet = """
                #onetrust-consent-sdk, .onetrust-pc-dark-filter, #CybotCookiebotDialog,
                .cc-window, .cc-banner, [id*="cookie-banner"], [class*="cookie-banner"],
                [id*="cookieConsent"], [class*="cookie-consent"], .fc-consent-root {
                    display: none !important;
                }
            """.trimIndent()
        ),
        BuiltInUserExtension(
            id = "kill_sticky_headers",
            titleEn = "Unpin Sticky Headers",
            titleFa = "حذف نوارهای چسبان مزاحم",
            descriptionEn = "Converts fixed/sticky headers into normal scrolling elements.",
            descriptionFa = "آزادسازی فضای عمودی صفحه با غیرشناور کردن هدرهای چسبان.",
            jsSnippet = """
                (function() {
                    document.querySelectorAll('header, nav, [class*="sticky"], [class*="fixed"]').forEach(function(el) {
                        var pos = window.getComputedStyle(el).position;
                        if (pos === 'fixed' || pos === 'sticky') {
                            el.style.setProperty('position', 'relative', 'important');
                        }
                    });
                })();
            """.trimIndent()
        ),
        BuiltInUserExtension(
            id = "video_speed_controller",
            titleEn = "Video 2x Speed Controller",
            titleFa = "کنترل سرعت ویدیو (۲ برابر)",
            descriptionEn = "Toggles HTML5 video playback speed between 1.0x, 1.5x, and 2.0x.",
            descriptionFa = "افزایش سرعت پخش ویدیوهای HTML5 به ۱.۵ و ۲ برابر.",
            jsSnippet = """
                (function() {
                    var videos = document.querySelectorAll('video');
                    videos.forEach(function(v) {
                        v.playbackRate = (v.playbackRate >= 2.0) ? 1.0 : (v.playbackRate >= 1.5 ? 2.0 : 1.5);
                    });
                })();
            """.trimIndent()
        ),
        BuiltInUserExtension(
            id = "persian_rtl_typography",
            titleEn = "Persian RTL Typography Polish",
            titleFa = "بهبود تایپوگرافی و خوانایی فارسی",
            descriptionEn = "Enhances line-height and font rendering for Persian/Arabic web pages.",
            descriptionFa = "تنظیم فاصله خطوط و فونت استاندارد برای مطالعه راحت‌تر متون فارسی.",
            cssSnippet = """
                body, article, p, li {
                    line-height: 1.85 !important;
                    font-family: system-ui, -apple-system, "Noto Sans Arabic", "Tahoma", sans-serif !important;
                }
            """.trimIndent()
        )
    )
}
