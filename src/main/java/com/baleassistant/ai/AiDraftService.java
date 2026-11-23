package com.baleassistant.ai;

import com.baleassistant.integration.bale.BaleBrowserService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
//@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "openai")
public class AiDraftService {

    private final ChatClient chatClient;
    private final BaleBrowserService baleBrowserService;

    public AiDraftService(ChatClient.Builder builder,
                          BaleBrowserService baleBrowserService) {
        this.chatClient = builder.build();
        this.baleBrowserService = baleBrowserService;
    }

    public AiDraftResponse generate(String requestedUid, String tone) {
        String currentUid = baleBrowserService.extractCurrentConversationUid();

        if (currentUid == null || currentUid.isBlank()) {
            throw new IllegalStateException("هیچ گفتگوی فعالی در Bale باز نیست.");
        }
        if (!currentUid.equals(requestedUid)) {
            throw new IllegalStateException(
                    "گفتگوی بازشده در Bale با گفتگوی انتخاب‌شده در پنل یکسان نیست. گفتگو را دوباره انتخاب کنید."
            );
        }

        List<Map<String, Object>> messages = baleBrowserService.extractMessages();
        if (messages.isEmpty()) {
            throw new IllegalStateException("پیامی برای تولید پاسخ در این گفتگو پیدا نشد.");
        }

        Map<String, Object> latestCandidate = findLatestCustomerMessage(messages);
        if (latestCandidate == null) {
            throw new IllegalStateException(
                    "پیام دریافتیِ متنی برای پاسخ‌دادن پیدا نشد. آخرین پیام احتمالاً از طرف اپراتور ارسال شده است."
            );
        }

        String question = text(latestCandidate.get("text"));
        String messageId = text(latestCandidate.get("id"));
        boolean directionUncertain = !"INCOMING".equals(text(latestCandidate.get("direction")));

        List<String> styleExamples = collectOutgoingExamples(messages, 8);
        List<String> qaExamples = collectQuestionAnswerExamples(messages, 5);
        String history = buildRecentHistory(messages, 24);

        String systemPrompt = """
                تو دستیار نگارش پاسخ برای اپراتور پشتیبانی هستی. وظیفه تو فقط تهیه پیش‌نویس است؛
                هرگز ادعا نکن پاسخی ارسال کرده‌ای یا اقدامی در سامانه انجام داده‌ای.

                زبان پاسخ فارسی باشد، مگر آنکه متن مخاطب صراحتاً زبان دیگری داشته باشد.
                سبک نگارش را از نمونه پیام‌های واقعی اپراتور استخراج کن:
                - میزان رسمیت، طول جمله‌ها، نوع سلام و احوال‌پرسی، واژگان پرتکرار و نحوه تشکر را تقلید کن.
                - نمونه‌ها فقط برای تقلید سبک هستند، نه برای استخراج واقعیت یا وعده.
                - پاسخ باید طبیعی و شبیه پیام انسانی همان اپراتور باشد؛ نه متن تبلیغاتی و نه ادبیات کلیشه‌ای هوش مصنوعی.
                - کوتاه و روشن بنویس؛ فقط در صورت نیاز توضیح بیشتر بده.
                - اگر اطلاعات لازم در تاریخچه وجود ندارد، چیزی جعل نکن؛ یک سؤال مشخص برای تکمیل اطلاعات بپرس.
                - زمان انجام کار، نتیجه بررسی، شماره پیگیری، مقررات یا تعهدی را که در تاریخچه/اطلاعات ورودی نیست، اختراع نکن.
                - اگر موضوع نیازمند بررسی فنی یا اداری است، دریافت/ارجاع موضوع را قطعی جلوه نده؛ فقط متناسب با شواهد پاسخ بده.
                - فقط متن پیشنهادی پاسخ را برگردان؛ عنوان، توضیح درباره سبک، نقل‌قول یا Markdown اضافه نکن.

                ترجیح لحنی انتخاب‌شده توسط اپراتور: %s
                اگر گزینه «مطابق سبک نگارش من» انتخاب شده، نمونه‌های واقعی اپراتور اولویت دارند.
                اگر «رسمی و اداری» انتخاب شده، لحن رسمی‌تر و اداری باشد.
                اگر «صمیمی و حرفه‌ای» انتخاب شده، کمی محاوره‌ای‌تر اما محترمانه باشد.
                """.formatted(toneLabel(tone));

        String userPrompt = """
                اطلاعات گفتگوی جاری:
                UID: %s

                تاریخچه اخیر گفتگو (ترتیب زمانی؛ جهت UNKNOWN را قطعی فرض نکن):
                %s

                نمونه پاسخ‌های قبلی اپراتور برای تقلید سبک:
                %s

                نمونه‌های پرسش و پاسخ قبلی در همین گفتگو:
                %s

                پیام هدف برای پاسخ:
                %s

                فقط پیش‌نویس پاسخ مناسب به پیام هدف را بنویس.
                """.formatted(
                requestedUid,
                history,
                numbered(styleExamples),
                numbered(qaExamples),
                question
        );

        String content = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();

        if (content == null || content.isBlank()) {
            throw new IllegalStateException("مدل هوش مصنوعی پاسخ خالی برگرداند.");
        }

        return new AiDraftResponse(
                requestedUid,
                messageId,
                content.trim(),
                "SPRING_AI",
                Instant.now().toString(),
                styleExamples.size(),
                qaExamples.size(),
                directionUncertain
                        ? "جهت پیام هدف در DOM بله قطعی تشخیص داده نشده است؛ قبل از استفاده، متن پیام را بررسی کنید."
                        : null
        );
    }

    private Map<String, Object> findLatestCustomerMessage(List<Map<String, Object>> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Map<String, Object> message = messages.get(i);
            String direction = text(message.get("direction"));
            String body = text(message.get("text"));
            if (!body.isBlank() && !"OUTGOING".equals(direction)) {
                return message;
            }
            if (!body.isBlank() && "OUTGOING".equals(direction)) {
                return null;
            }
        }
        return null;
    }

    private List<String> collectOutgoingExamples(List<Map<String, Object>> messages, int limit) {
        List<String> result = new ArrayList<>();
        for (int i = messages.size() - 1; i >= 0 && result.size() < limit; i--) {
            Map<String, Object> message = messages.get(i);
            if ("OUTGOING".equals(text(message.get("direction")))) {
                String body = text(message.get("text"));
                if (!body.isBlank()) result.add(0, body);
            }
        }
        return result;
    }

    private List<String> collectQuestionAnswerExamples(List<Map<String, Object>> messages, int limit) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < messages.size() - 1 && result.size() < limit; i++) {
            Map<String, Object> question = messages.get(i);
            if (!"INCOMING".equals(text(question.get("direction")))) continue;
            String questionText = text(question.get("text"));
            if (questionText.isBlank()) continue;

            for (int j = i + 1; j < messages.size(); j++) {
                Map<String, Object> answer = messages.get(j);
                String direction = text(answer.get("direction"));
                if ("INCOMING".equals(direction)) break;
                if ("OUTGOING".equals(direction)) {
                    String answerText = text(answer.get("text"));
                    if (!answerText.isBlank()) {
                        result.add("مخاطب: " + questionText + "\nاپراتور: " + answerText);
                    }
                    i = j;
                    break;
                }
            }
        }
        return result;
    }

    private String buildRecentHistory(List<Map<String, Object>> messages, int limit) {
        int from = Math.max(0, messages.size() - limit);
        StringBuilder result = new StringBuilder();
        for (int i = from; i < messages.size(); i++) {
            Map<String, Object> message = messages.get(i);
            String direction = text(message.get("direction"));
            String role = switch (direction) {
                case "INCOMING" -> "مخاطب";
                case "OUTGOING" -> "اپراتور";
                default -> "جهت نامشخص";
            };
            String body = text(message.get("text"));
            if (body.isBlank()) {
                Object attachments = message.get("attachments");
                if (attachments instanceof List<?> list && !list.isEmpty()) {
                    body = "[پیام دارای پیوست]";
                } else {
                    continue;
                }
            }
            result.append(role).append(": ").append(body).append('\n');
        }
        return result.toString();
    }

    private String numbered(List<String> values) {
        if (values.isEmpty()) return "نمونه‌ای در این گفتگو در دسترس نیست.";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            result.append(i + 1).append(") ").append(values.get(i)).append('\n');
        }
        return result.toString();
    }

    private String toneLabel(String tone) {
        return switch (tone == null ? "MATCH_USER" : tone) {
            case "FORMAL" -> "رسمی و اداری";
            case "FRIENDLY" -> "صمیمی و حرفه‌ای";
            default -> "مطابق سبک نگارش من";
        };
    }

    private String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    public record AiDraftResponse(
            String conversationUid,
            String targetMessageId,
            String content,
            String source,
            String generatedAt,
            int styleExamplesUsed,
            int qaExamplesUsed,
            String warning
    ) {}
}
