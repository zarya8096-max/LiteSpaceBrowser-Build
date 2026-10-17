(function() {
    if (window.__litespaceReaderActive) {
        window.location.reload();
        return;
    }
    window.__litespaceReaderActive = true;
    const candidates = document.querySelectorAll('article, [role="main"], main, .post-content, .article-body, .entry-content');
    let bestNode = null;
    let maxScore = 0;
    candidates.forEach(node => {
        const textLen = (node.innerText || '').trim().length;
        if (textLen > maxScore) {
            maxScore = textLen;
            bestNode = node;
        }
    });
    if (!bestNode) {
        const paragraphs = Array.from(document.querySelectorAll('p')).filter(p => (p.innerText || '').length > 60);
        bestNode = (paragraphs.length > 0 && paragraphs[0].parentElement) ? paragraphs[0].parentElement : document.body;
    }
    const title = document.title || location.hostname;
    const contentHtml = bestNode.innerHTML;
    const isRtl = document.dir === 'rtl' || getComputedStyle(document.body).direction === 'rtl';
    document.head.innerHTML = `
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>${title}</title>
        <style>
            :root { color-scheme: light dark; }
            body {
                max-width: 700px;
                margin: 0 auto;
                padding: 24px 20px 80px 20px;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Noto Sans Arabic", sans-serif;
                font-size: 18px;
                line-height: 1.75;
                background: #FAF9F6;
                color: #1C1E21;
                direction: ${isRtl ? 'rtl' : 'ltr'};
            }
            @media (prefers-color-scheme: dark) {
                body { background: #0E131B; color: #E4E6EB; }
                a { color: #8AB4F8; }
            }
            h1.litespace-reader-title { font-size: 1.65rem; line-height: 1.3; margin-bottom: 12px; }
            .litespace-reader-meta { font-size: 0.85rem; opacity: 0.7; margin-bottom: 24px; padding-bottom: 12px; border-bottom: 1px solid rgba(128,128,128,0.25); }
            img, video, figure { max-width: 100%; height: auto; border-radius: 8px; margin: 16px 0; }
            nav, aside, footer, .ad, .advertisement, .social-share, .related-posts, script, style, iframe { display: none !important; }
        </style>
    `;
    document.body.innerHTML = `
        <article>
            <h1 class="litespace-reader-title">${title}</h1>
            <div class="litespace-reader-meta">LiteSpace Reader View &bull; ${location.hostname}</div>
            <div class="litespace-reader-content">${contentHtml}</div>
        </article>
    `;
    window.scrollTo(0, 0);
})();
