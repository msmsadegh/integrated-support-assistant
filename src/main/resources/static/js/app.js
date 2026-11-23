
let conversations = [];
let selectedConversationUid = null;
let currentMessages = [];
let generatedDraft = null;

const $ = (id) => document.getElementById(id);

async function api(url, options = {}) {
    const response = await fetch(url, options);
    const payload = await response.json().catch(() => null);

    if (!response.ok) {
        throw new Error(
            payload?.message ||
            payload?.error ||
            `HTTP ${response.status}`
        );
    }

    return payload;
}

function showToast(message) {
    const el = $("toast");
    if (!el) return;

    el.textContent = message;
    el.classList.add("show");
    window.setTimeout(() => el.classList.remove("show"), 2800);
}

function setConnectionState(status) {
    const started = Boolean(status?.browserStarted);
    const authenticated = Boolean(status?.authenticated);

    $("browserState").textContent = started ? "فعال" : "خاموش";
    $("browserStateHint").textContent = started
        ? "مرورگر Playwright اجرا شده"
        : "مرورگر اجرا نشده";

    $("authState").textContent = authenticated ? "متصل" : "نیازمند ورود";
    $("browserState").classList.toggle("good", started);
    $("authState").classList.toggle("good", authenticated);

    const label = authenticated
        ? "اتصال برقرار است"
        : started
            ? "منتظر ورود به Bale"
            : "قطع";

    $("connectionPill").innerHTML =
        `<i class="${authenticated ? "online" : "offline"}"></i> ${label}`;

    $("sidebarConnection").innerHTML =
        `<i class="${authenticated ? "online" : "offline"}"></i><span>${label}</span>`;

    $("sidebarConnectionDetail").textContent =
        status?.message || "Bale browser status";

    $("startBrowserButton").textContent =
        started ? "بررسی اتصال" : "اتصال به Bale";
}

async function refreshStatus() {
    const status = await api("/api/bale/browser/status");
    setConnectionState(status);
    return status;
}

async function startBaleBrowser() {
    const button = $("startBrowserButton");
    button.disabled = true;
    button.textContent = "در حال اتصال...";

    try {
        const status = await api("/api/bale/browser/start", {
            method: "POST"
        });

        setConnectionState(status);

        if (status.authenticated) {
            await loadConversations();
            showToast("اتصال به Bale برقرار شد");
        } else {
            showToast(
                "مرورگر باز شد؛ وارد حساب Bale شوید و سپس بروزرسانی را بزنید"
            );
        }
    } catch (error) {
        showToast("اجرای مرورگر ناموفق بود: " + error.message);
    } finally {
        button.disabled = false;
    }
}

async function refreshBaleData() {
    const button = $("refreshButton");
    button.disabled = true;

    try {
        const status = await refreshStatus();

        if (!status.authenticated) {
            showToast(
                status.browserStarted
                    ? "ابتدا در پنجره Bale وارد حساب شوید"
                    : "ابتدا مرورگر Bale را اجرا کنید"
            );
            return;
        }

        await loadConversations();
        showToast("فهرست گفتگوها بروزرسانی شد");
    } catch (error) {
        showToast("دریافت اطلاعات ناموفق بود: " + error.message);
    } finally {
        button.disabled = false;
    }
}

async function loadConversations() {
    const list = $("conversationList");
    list.innerHTML =
        '<div class="empty-state">در حال دریافت گفتگوها...</div>';

    try {
        conversations = await api("/api/bale/browser/conversations");

        $("conversationCount").textContent =
            conversations.length.toLocaleString("fa-IR");

        $("conversationBadge").textContent =
            `${conversations.length.toLocaleString("fa-IR")} گفتگو`;

        if (!conversations.length) {
            list.innerHTML =
                '<div class="empty-state">گفتگویی در فهرست فعلی Bale پیدا نشد.</div>';
            return;
        }

        list.innerHTML = "";

        conversations.forEach((conversation, index) => {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "conversation-item";

            const top = document.createElement("div");
            top.className = "conversation-item-top";

            const name = document.createElement("strong");
            name.textContent = conversation.name || "گفتگوی بدون نام";

            const time = document.createElement("time");
            time.textContent = conversation.lastMessageTime || "";

            top.append(name, time);

            const preview = document.createElement("div");
            preview.className = "conversation-item-preview";
            preview.textContent =
                conversation.lastMessagePreview || "بدون پیش‌نمایش پیام";

            button.append(top, preview);
            button.addEventListener("click", () =>
                selectConversation(index, button)
            );

            list.append(button);
        });
    } catch (error) {
        list.innerHTML =
            '<div class="empty-state error-state">دریافت گفتگوها ناموفق بود. وضعیت اتصال Bale را بررسی کنید.</div>';
        throw error;
    }
}

function setDraftControlsEnabled(enabled) {
    $("draftContent").disabled = !enabled;
    $("generateDraftButton").disabled = !enabled;
    $("copyDraftButton").disabled =
        !enabled || !$("draftContent").value.trim();
}

async function selectConversation(index, button) {
    document.querySelectorAll(".conversation-item")
        .forEach((item) => item.classList.remove("selected"));

    button.classList.add("selected");

    const conversation = conversations[index];

    selectedConversationUid = null;
    currentMessages = [];
    generatedDraft = null;

    $("draftContent").value = "";
    setDraftControlsEnabled(false);

    $("draftStatus").textContent = "در حال بارگذاری گفتگو...";
    $("draftWarning").hidden = true;

    $("customerName").textContent =
        conversation?.name || "گفتگوی بدون نام";

    $("conversationPreview").textContent =
        conversation?.lastMessagePreview || "";

    $("conversationUid").textContent = "در حال باز کردن گفتگو...";
    $("messageStatus").textContent = "در حال دریافت";

    $("messages").innerHTML =
        '<div class="empty-state">در حال بارگذاری پیام‌ها...</div>';

    try {
        const selected = await api(
            `/api/bale/browser/conversations/${index}/uid`
        );

        // پشتیبانی از پاسخ String و پاسخ JSON
        const uid = typeof selected === "string"
            ? selected
            : selected?.uid;

        if (!uid || !String(uid).trim()) {
            throw new Error(
                "UID گفتگو از سرور دریافت نشد. پاسخ endpoint را بررسی کنید."
            );
        }

        selectedConversationUid = String(uid).trim();

        $("conversationUid").textContent =
            `UID: ${selectedConversationUid}`;

        currentMessages = await api(
            "/api/bale/browser/messages/current"
        );

        renderMessages(currentMessages);

        $("messageCount").textContent =
            currentMessages.length.toLocaleString("fa-IR");

        $("messageStatus").textContent =
            `${currentMessages.length.toLocaleString("fa-IR")} پیام`;

        // فعال‌سازی دکمه فقط پس از دریافت موفق UID و پیام‌ها
        setDraftControlsEnabled(true);

        $("draftStatus").textContent =
            "آماده تولید پاسخ بر اساس سبک نگارش پیام‌های قبلی شما";

    } catch (error) {
        selectedConversationUid = null;
        setDraftControlsEnabled(false);

        $("messages").innerHTML =
            '<div class="empty-state error-state">بارگذاری پیام‌ها ناموفق بود. دوباره گفتگو را انتخاب کنید.</div>';

        $("messageStatus").textContent = "خطا";
        $("draftStatus").textContent = "گفتگو بارگذاری نشد.";

        showToast("باز کردن گفتگو ناموفق بود: " + error.message);
    }
}

function formatTime(value) {
    const number = Number(value);

    return Number.isFinite(number)
        ? new Date(number).toLocaleString("fa-IR", {
            year: "numeric",
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit"
        })
        : "";
}

function renderMessages(messages) {
    const box = $("messages");
    box.innerHTML = "";

    if (!Array.isArray(messages) || !messages.length) {
        box.innerHTML =
            '<div class="empty-state">برای این گفتگو پیامی در DOM فعلی پیدا نشد.</div>';
        return;
    }

    messages.forEach((message) => {
        const direction = message.direction || "UNKNOWN";

        const item = document.createElement("article");
        item.className = `message ${direction.toLowerCase()}`;

        const meta = document.createElement("div");
        meta.className = "message-meta";
        meta.textContent = direction === "INCOMING"
            ? "دریافتی"
            : direction === "OUTGOING"
                ? "ارسالی شما"
                : "جهت نامشخص";

        const bubble = document.createElement("div");
        bubble.className = "bubble";

        if (message.text) {
            const text = document.createElement("div");
            text.className = "message-text";
            text.textContent = message.text;
            bubble.append(text);
        }

        (message.attachments || []).forEach((attachment) => {
            const element = document.createElement("div");
            element.className = "attachment";

            element.textContent = attachment.type === "DOCUMENT"
                ? `📎 ${attachment.fileName || "سند"} ${
                    attachment.fileSize ? "· " + attachment.fileSize : ""
                }`
                : attachment.type === "PHOTO"
                    ? "تصویر پیوست‌شده"
                    : "پیوست";

            bubble.append(element);
        });

        if (
            !message.text &&
            (!message.attachments || !message.attachments.length)
        ) {
            bubble.textContent = "محتوای این پیام قابل نمایش نیست.";
        }

        if (message.isForwarded) {
            const forwarded = document.createElement("div");
            forwarded.className = "forwarded-label";
            forwarded.textContent =
                message.forwardedFrom || "پیام بازفرستاده‌شده";
            bubble.prepend(forwarded);
        }

        const time = document.createElement("time");
        time.className = "message-time";
        time.textContent = formatTime(message.timestamp);

        item.append(meta, bubble, time);
        box.append(item);
    });

    requestAnimationFrame(() => {
        box.scrollTop = box.scrollHeight;
    });
}

async function generateAiDraft() {
    if (!selectedConversationUid) {
        showToast("ابتدا یک گفتگو انتخاب کنید");
        return;
    }

    const button = $("generateDraftButton");
    button.disabled = true;
    button.textContent = "در حال تولید...";

    $("draftStatus").textContent =
        "در حال تحلیل تاریخچه و سبک نگارش پیام‌های قبلی...";

    $("draftWarning").hidden = true;

    try {
        const result = await api("/api/v1/ai/drafts/generate", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                conversationUid: selectedConversationUid,
                tone: $("responseTone").value
            })
        });

        generatedDraft = result;
        $("draftContent").value = result.content || "";

        $("copyDraftButton").disabled = !result.content;

        $("draftStatus").textContent =
            `تولید شد · ${result.styleExamplesUsed} نمونه سبک · ${result.qaExamplesUsed} نمونه پرسش‌وپاسخ`;

        if (result.warning) {
            $("draftWarning").textContent = result.warning;
            $("draftWarning").hidden = false;
        }

        showToast("پیش‌نویس پاسخ تولید شد؛ قبل از استفاده بررسی کنید.");

    } catch (error) {
        $("draftStatus").textContent = "تولید پاسخ انجام نشد.";
        showToast("خطا در تولید پاسخ: " + error.message);

    } finally {
        button.disabled = !selectedConversationUid;
        button.textContent = "✧ تولید پاسخ";
    }
}

async function copyDraft() {
    const text = $("draftContent").value.trim();
    if (!text) return;

    try {
        await navigator.clipboard.writeText(text);
        showToast("پاسخ کپی شد");
    } catch (error) {
        $("draftContent").select();
        document.execCommand("copy");
        showToast("پاسخ کپی شد");
    }
}

document.addEventListener("DOMContentLoaded", async () => {
    // تا زمان بارگذاری موفق یک گفتگو، کنترل‌های Draft غیرفعال هستند.
    setDraftControlsEnabled(false);

    // با ویرایش متن، وضعیت دکمه کپی به‌روزرسانی می‌شود.
    $("draftContent").addEventListener("input", () => {
        $("copyDraftButton").disabled =
            !$("draftContent").value.trim();
    });

    try {
        const status = await refreshStatus();

        if (status.authenticated) {
            await loadConversations();
        } else if (status.browserStarted) {
            $("conversationList").innerHTML =
                '<div class="empty-state">مرورگر فعال است. وارد حساب Bale شوید و سپس «بروزرسانی» را انتخاب کنید.</div>';
        }
    } catch (error) {
        $("conversationList").innerHTML =
            '<div class="empty-state error-state">ارتباط با Backend برقرار نشد. اجرای Spring Boot را بررسی کنید.</div>';
    }
});
function syncAiDraftButton() {
    const button = document.getElementById("generateDraftButton");
    const uidElement = document.getElementById("conversationUid");
    const messages = document.querySelectorAll("#messages .message");

    if (!button || !uidElement) return;

    const uidText = uidElement.textContent.trim();
    const hasUid = /^UID:\s*\S+/.test(uidText);
    const hasMessages = messages.length > 0;

    button.disabled = !(hasUid && hasMessages);
}

// تعریف تابع به‌صورت global برای onclick در HTML
window.generateAiDraft = async function () {

    const uidElement = document.getElementById("conversationUid");
    const button = document.getElementById("generateDraftButton");
    const draftContent = document.getElementById("draftContent");
    const draftStatus = document.getElementById("draftStatus");
    const draftWarning = document.getElementById("draftWarning");
    const copyButton = document.getElementById("copyDraftButton");
    const toneSelect = document.getElementById("responseTone");

    const uidText = uidElement?.textContent.trim() || "";
    const uidMatch = uidText.match(/^UID:\s*(\S+)/);
    const conversationUid = uidMatch?.[1];

    if (!conversationUid) {
        showToast("ابتدا یک گفتگوی معتبر انتخاب کنید.");
        return;
    }

    if (!document.querySelector("#messages .message")) {
        showToast("پیام‌های گفتگو هنوز بارگذاری نشده‌اند.");
        return;
    }

    button.disabled = true;
    button.textContent = "در حال تولید...";

    draftStatus.textContent =
        "در حال تحلیل تاریخچه و سبک نگارش پیام‌های قبلی...";

    draftWarning.hidden = true;

    try {
        const response = await fetch("/api/v1/ai/drafts/generate", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                conversationUid: conversationUid,
                tone: toneSelect?.value || "MATCH_USER"
            })
        });

        const result = await response.json();

        if (!response.ok) {
            throw new Error(
                result?.message ||
                result?.error ||
                `HTTP ${response.status}`
            );
        }

        if (!result.content) {
            throw new Error("مدل پاسخ معتبری تولید نکرد.");
        }

        draftContent.value = result.content;

        if (copyButton) {
            copyButton.disabled = false;
        }

        draftStatus.textContent =
            `تولید شد · ${result.styleExamplesUsed ?? 0} نمونه سبک · ${result.qaExamplesUsed ?? 0} نمونه پرسش‌وپاسخ`;

        if (result.warning) {
            draftWarning.textContent = result.warning;
            draftWarning.hidden = false;
        }

        showToast("پیش‌نویس پاسخ با موفقیت تولید شد.");

    } catch (error) {
        draftStatus.textContent = "تولید پاسخ انجام نشد.";
        showToast("خطا در تولید پاسخ: " + error.message);

    } finally {
        button.textContent = "✧ تولید پاسخ";
        syncAiDraftButton();
    }
};

// اتصال مجدد تابع کپی به فضای سراسری
if (typeof copyDraft === "function") {
    window.copyDraft = copyDraft;
}

// نظارت بر تغییر UID و پیام‌ها
const aiDraftObserver = new MutationObserver(() => {
    syncAiDraftButton();
});

document.addEventListener("DOMContentLoaded", () => {
    const messages = document.getElementById("messages");
    const uid = document.getElementById("conversationUid");

    if (messages) {
        aiDraftObserver.observe(messages, {
            childList: true,
            subtree: true
        });
    }

    if (uid) {
        aiDraftObserver.observe(uid, {
            childList: true,
            subtree: true,
            characterData: true
        });
    }

    syncAiDraftButton();
});

// اجرای اولیه
syncAiDraftButton();