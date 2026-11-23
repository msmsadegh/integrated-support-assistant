
package com.baleassistant.integration.bale;

import com.microsoft.playwright.*;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class BaleBrowserService {

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private Playwright playwright;
    private BrowserContext context;
    private Page page;

    private volatile boolean browserStarted = false;
    private volatile boolean authenticated = false;

    public Map<String, Object> start() {

        return execute(() -> {

            if (context != null && page != null && !page.isClosed()) {
                detectAuthentication();
                return buildStatus();
            }

            Path profile = Path.of(".bale-browser-profile")
                    .toAbsolutePath();

            Files.createDirectories(profile);

            playwright = Playwright.create();

            context = playwright.chromium()
                    .launchPersistentContext(
                            profile,
                            new BrowserType.LaunchPersistentContextOptions()
                                    .setHeadless(false)
                    );

            page = context.pages().isEmpty()
                    ? context.newPage()
                    : context.pages().get(0);

            browserStarted = true;

            page.navigate("https://web.bale.ai/chat");

            detectAuthentication();

            return buildStatus();
        });
    }

    public Map<String, Object> getStatus() {

        return execute(() -> {

            detectAuthentication();

            return buildStatus();
        });
    }

    private Map<String, Object> buildStatus() {

        return Map.of(
                "browserStarted", browserStarted,
                "authenticated", authenticated,
                "message", !browserStarted
                        ? "Browser is not running"
                        : authenticated
                        ? "Authenticated successfully"
                        : "Browser is running, authentication not confirmed"
        );
    }

    private void detectAuthentication() {

        try {

            if (page == null || page.isClosed()) {
                browserStarted = false;
                authenticated = false;
                return;
            }

            browserStarted = true;

            String currentUrl = page.url().toLowerCase();

            String bodyText = page.locator("body")
                    .innerText()
                    .toLowerCase();

            // Check login page
            boolean loginPage = currentUrl.contains("/login");

            boolean phoneInput =
                    page.locator("input[type='tel']").count() > 0;

            boolean loginText =
                    bodyText.contains("ورود") ||
                            bodyText.contains("شماره موبایل");

            boolean loginRequired =
                    loginPage || (phoneInput && loginText);

            // Check Bale chat interface
            int chatSignals = 0;

            String[] chatMarkers = {
                    "گفتگو",
                    "مخاطبین",
                    "مجله",
                    "خدمات"
            };

            for (String marker : chatMarkers) {

                if (bodyText.contains(marker)) {
                    chatSignals++;
                }
            }

            // Authentication is confirmed only when
            // the chat interface is actually visible.

            authenticated =
                    !loginRequired && chatSignals >= 2;

        } catch (Exception e) {

            authenticated = false;
            browserStarted = page != null && !page.isClosed();
        }
    }

    public Map<String, Object> checkAuthentication() {

        return execute(() -> {

            detectAuthentication();

            String currentUrl =
                    page != null && !page.isClosed()
                            ? page.url()
                            : "";

            boolean loginRequired = !authenticated;

            return Map.of(
                    "authenticated", authenticated,
                    "loginRequired", loginRequired,
                    "browserStarted", browserStarted,
                    "currentUrl", currentUrl,
                    "message", authenticated
                            ? "Bale session is active"
                            : "Authentication not confirmed"
            );
        });
    }

    private <T> T execute(Callable<T> task) {

        try {

            return executor.submit(task).get();

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Bale browser operation failed", e
            );
        }
    }

    @PreDestroy
    public void shutdown() {

        try {

            if (executor.isShutdown()) {
                return;
            }

            execute(() -> {

                if (context != null) {
                    context.close();
                    context = null;
                }

                if (playwright != null) {
                    playwright.close();
                    playwright = null;
                }

                page = null;
                browserStarted = false;
                authenticated = false;

                return null;
            });

        } finally {

            executor.shutdown();
        }
    }


    public Map<String, Object> inspectChatPage() {

        return execute(() -> {

            if (page == null || page.isClosed()) {
                throw new IllegalStateException("Browser is not running");
            }

            String title = page.title();
            String url = page.url();

            String bodyText = page.locator("body").innerText();

            String html = page.locator("body").innerHTML();

            return Map.of(
                    "title", title,
                    "url", url,
                    "bodyText", bodyText,
                    "html", html
            );
        });
    }

    public List<BaleConversationDto> extractConversations() {
        return execute(() -> {
            if (page == null || page.isClosed()) {
                throw new IllegalStateException("Browser is not running");
            }

            Locator dialogs = page.locator("[aria-label='dialog-item']");
            List<BaleConversationDto> result = new ArrayList<>();

            int count = dialogs.count();

            for (int i = 0; i < count; i++) {
                Locator dialog = dialogs.nth(i);

                String name = getText(
                        dialog.locator(".gw_0GR bdi")
                );

                String time = getText(
                        dialog.locator(".rPpZWP > span")
                );

                String preview = dialog
                        .locator(".aqFHpt")
                        .getAttribute("title");

                result.add(new BaleConversationDto(
                        name,
                        time,
                        preview != null ? preview : ""
                ));
            }

            return result;
        });
    }

    private String getText(Locator locator) {
        try {
            return locator.count() > 0
                    ? locator.first().innerText().trim()
                    : "";
        } catch (Exception e) {
            return "";
        }
    }

    public Object inspectMessageDom() {

        return execute(() -> {

            if (page == null || page.isClosed()) {
                throw new IllegalStateException("Browser is not running");
            }

            return page.evaluate("""
            () => {
                const elements = Array.from(
                    document.querySelectorAll(
                        '[role], [aria-label], [data-testid]'
                    )
                );

                return elements
                    .map(el => ({
                        tag: el.tagName,
                        className: typeof el.className === 'string'
                            ? el.className : '',
                        role: el.getAttribute('role'),
                        ariaLabel: el.getAttribute('aria-label'),
                        testId: el.getAttribute('data-testid'),
                        text: (el.innerText || '').trim().slice(0, 150),
                        childCount: el.children.length
                    }))
                    .filter(el =>
                        el.text.length > 0 ||
                        el.role ||
                        el.ariaLabel ||
                        el.testId
                    )
                    .slice(-100);
            }
        """);
        });
    }

    public Object inspectMessageDetails() {

        return execute(() -> {

            if (page == null || page.isClosed()) {
                throw new IllegalStateException("Browser is not running");
            }

            return page.locator("[aria-label='message-item']")
                    .evaluateAll("""
                    elements => elements.slice(-15).map((el, index) => ({
                        index: index,
                        text: el.innerText,
                        html: el.outerHTML,
                        directionIcons: Array.from(
                            el.querySelectorAll('svg[aria-label]')
                        ).map(svg => svg.getAttribute('aria-label')),
                        images: Array.from(
                            el.querySelectorAll('img')
                        ).map(img => ({
                            src: img.getAttribute('src'),
                            alt: img.getAttribute('alt'),
                            testId: img.getAttribute('data-testid')
                        }))
                    }))
                """);
        });
    }
    public String extractCurrentConversationUid() {
        return execute(() -> {
            if (page == null || page.isClosed()) {
                throw new IllegalStateException("Browser is not running");
            }

            return page.evaluate("""
            () => {
                const params = new URL(window.location.href).searchParams;
                return params.get('uid');
            }
        """).toString();
        });
    }
    public void openConversation(String uid) {
        execute(() -> {
            if (page == null || page.isClosed()) {
                throw new IllegalStateException("Browser is not running");
            }

            page.navigate("https://web.bale.ai/chat?uid=" + uid);

            page.waitForSelector("[aria-label='message-item']");

            return null;
        });
    }
    public Object inspectConversationLinks() {
        return execute(() -> {
            if (page == null || page.isClosed()) {
                throw new IllegalStateException("Browser is not running");
            }

            return page.locator("[aria-label='dialog-item']")
                    .evaluateAll("""
                    elements => elements.slice(0, 10).map(el => ({
                        name: el.querySelector('.gw_0GR bdi')?.innerText ?? '',
                        hrefs: Array.from(el.querySelectorAll('a'))
                            .map(a => a.href),
                        attributes: Array.from(el.attributes)
                            .reduce((result, attr) => {
                                result[attr.name] = attr.value;
                                return result;
                            }, {})
                    }))
                """);
        });
    }


    public String extractConversationUidByClick(int index) {
        return execute(() -> {

            if (page == null || page.isClosed()) {
                throw new IllegalStateException("Browser is not running");
            }

            Locator dialogs = page.locator("[aria-label='dialog-item']");

            if (index < 0 || index >= dialogs.count()) {
                throw new IllegalArgumentException(
                        "Invalid conversation index: " + index
                );
            }

            dialogs.nth(index).click();
            // Also works when the selected conversation is already open.
            page.waitForTimeout(900);
            String uid = extractCurrentUid();
            if (uid == null || uid.isBlank()) {
                throw new IllegalStateException("Conversation UID was not found in the URL");
            }
            return uid;
        });
    }

    private String extractCurrentUid() {
        Object value = page.evaluate("""
        () => new URL(window.location.href)
            .searchParams.get('uid')
    """);
        return value == null ? null : value.toString();
    }


    public Map<String, Object> inspectBrowserPages() {
        return execute(() -> {

            if (context == null) {
                throw new IllegalStateException("Browser context is null");
            }

            List<Map<String, Object>> pages = new ArrayList<>();

            for (Page currentPage : context.pages()) {
                pages.add(Map.of(
                        "url", currentPage.url(),
                        "closed", currentPage.isClosed(),
                        "current", currentPage == page
                ));
            }

            return Map.of(
                    "pageCount", context.pages().size(),
                    "pages", pages
            );
        });
    }

    public List<Map<String, Object>> extractMessages() {
        return execute(() -> {

            if (page == null || page.isClosed()) {
                throw new IllegalStateException("Browser is not running");
            }

            return (List<Map<String, Object>>) page
                    .locator("[aria-label='message-item']")
                    .evaluateAll("""
            elements => elements.map((el, index) => {

                        const text = el.querySelector('.KTwPFW .p')
                            ?.innerText?.trim() ?? '';

                        const icons = Array.from(
                            el.querySelectorAll('svg[aria-label]')
                        ).map(svg => svg.getAttribute('aria-label'));

                        const direction = icons.includes('RightBubble-icon')
                            ? 'OUTGOING'
                            : icons.includes('LeftBubble-icon')
                                ? 'INCOMING'
                                : 'UNKNOWN';

                        const forwardedElement =
                            el.querySelector('.i7gGd9');

                        const isForwarded =
                            icons.includes('BoldForwardF-icon') ||
                            forwardedElement !== null;

                        const attachments = [];

                        el.querySelectorAll('.h7PFux').forEach(file => {
                            attachments.push({
                                type: 'DOCUMENT',
                                fileName: file.querySelector('.rCv9Za')
                                    ?.innerText?.trim() ?? '',
                                fileSize: file.querySelector('.Vo61jG')
                                    ?.innerText?.trim() ?? ''
                            });
                        });

                        el.querySelectorAll('.iI3L80, [data-testid="thumbnail"]')
                            .forEach(image => {
                                attachments.push({
                                    type: 'PHOTO',
                                    fileName: '',
                                    fileSize: '',
                                    source: image.getAttribute('src') ?? ''
                                });
                            });

                        const type = attachments.some(a => a.type === 'DOCUMENT')
                            ? 'DOCUMENT'
                            : attachments.some(a => a.type === 'PHOTO')
                                ? 'PHOTO'
                                : text.length > 0
                                    ? 'TEXT'
                                    : 'UNKNOWN';

                        return {
                            id: el.getAttribute('data-sid') ?? '',
                            timestamp: el.getAttribute('data-date') ?? '',
                            index: index,
                            direction: direction,
                            type: type,
                            isForwarded: isForwarded,
                            forwardedFrom: forwardedElement
                                ?.innerText?.trim() ?? null,
                            text: text,
                            attachments: attachments
                        };
                    })
                """);
        });
    }

    public record BaleConversationDto(
            String name,
            String lastMessageTime,
            String lastMessagePreview
    ) {}
}
