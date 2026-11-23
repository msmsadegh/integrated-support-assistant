package com.baleassistant.draft;

import com.baleassistant.ticket.TicketDto;
import com.baleassistant.ticket.TicketService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DraftService {
    private final TicketService ticketService;
    private final Map<String, DraftDto> drafts = new ConcurrentHashMap<>();

    public DraftService(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    public DraftDto generate(String ticketId) {
        TicketDto ticket = ticketService.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        String answer = switch (ticketId) {
            case "BAL-1001" -> "سلام و وقت بخیر. برای پیگیری درخواست ثبت‌شده، لطفاً وارد پنل کاربری خود شوید و از بخش «درخواست‌های من» گزینه «پیگیری درخواست» را انتخاب کنید. در صورت مشاهده خطا، لطفاً شماره درخواست و تصویر پیام خطا را ارسال فرمایید.";
            case "BAL-1002" -> "سلام و وقت بخیر. مدارک موردنیاز با توجه به نوع خدمت و وضعیت پرونده متفاوت است. لطفاً عنوان خدمت یا شماره پرونده را ارسال کنید تا اطلاعات دقیق‌تری در اختیارتان قرار دهیم.";
            case "BAL-1003" -> "سلام. برای بررسی دقیق‌تر مشکل، لطفاً تصویر کامل خطا، زمان وقوع آن و مرحله‌ای را که خطا در آن نمایش داده می‌شود ارسال فرمایید. پس از دریافت اطلاعات، موضوع قابل بررسی خواهد بود.";
            default -> "سلام و وقت بخیر. برای راهنمایی دقیق‌تر، لطفاً جزئیات بیشتری درباره درخواست خود ارسال فرمایید.";
        };

        DraftDto draft = new DraftDto(
                UUID.randomUUID().toString(),
                ticket.id(),
                answer,
                "PENDING_REVIEW",
                LocalDateTime.now(),
                "MOCK_TEMPLATE"
        );
        drafts.put(ticketId, draft);
        return draft;
    }

    public DraftDto update(String ticketId, String content) {
        DraftDto current = drafts.get(ticketId);
        if (current == null) {
            throw new IllegalArgumentException("No draft exists for ticket: " + ticketId);
        }
        DraftDto updated = new DraftDto(
                current.id(), current.ticketId(), content,
                "EDITED", current.createdAt(), current.source()
        );
        drafts.put(ticketId, updated);
        return updated;
    }

    public DraftDto findByTicketId(String ticketId) {
        return drafts.get(ticketId);
    }
}
