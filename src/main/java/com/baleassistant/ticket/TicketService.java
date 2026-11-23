package com.baleassistant.ticket;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TicketService {
    private final List<TicketDto> tickets = List.of(
            new TicketDto(
                    "BAL-1001", "علی رضایی", "پیگیری وضعیت درخواست",
                    "سلام، برای پیگیری درخواست ثبت‌شده راهنمایی می‌خواستم.",
                    TicketStatus.OPEN, LocalDateTime.now().minusMinutes(8),
                    List.of(
                            msg("m-1", "علی رضایی", "CUSTOMER", "سلام وقت بخیر.", 55),
                            msg("m-2", "پشتیبانی", "SUPPORT", "سلام، بفرمایید در چه موردی راهنمایی می‌خواهید؟", 48),
                            msg("m-3", "علی رضایی", "CUSTOMER", "برای پیگیری درخواست ثبت‌شده باید از کدام قسمت اقدام کنم؟", 8)
                    )
            ),
            new TicketDto(
                    "BAL-1002", "مریم احمدی", "مدارک موردنیاز",
                    "لطفاً مدارک لازم برای تکمیل پرونده را اعلام کنید.",
                    TicketStatus.OPEN, LocalDateTime.now().minusMinutes(24),
                    List.of(
                            msg("m-4", "مریم احمدی", "CUSTOMER", "سلام. پرونده من ناقص اعلام شده است.", 40),
                            msg("m-5", "پشتیبانی", "SUPPORT", "لطفاً عنوان خدمت را اعلام کنید.", 32),
                            msg("m-6", "مریم احمدی", "CUSTOMER", "برای تکمیل پرونده چه مدارکی لازم است؟", 24)
                    )
            ),
            new TicketDto(
                    "BAL-1003", "رضا کریمی", "خطا هنگام ثبت درخواست",
                    "هنگام ثبت درخواست با خطا مواجه می‌شوم.",
                    TicketStatus.WAITING, LocalDateTime.now().minusHours(1),
                    List.of(
                            msg("m-7", "رضا کریمی", "CUSTOMER", "سلام، هنگام ثبت درخواست صفحه خطا نشان می‌دهد.", 70),
                            msg("m-8", "پشتیبانی", "SUPPORT", "لطفاً متن خطا یا تصویر آن را ارسال کنید.", 60),
                            msg("m-9", "رضا کریمی", "CUSTOMER", "خطای ثبت اطلاعات دریافت می‌کنم.", 60)
                    )
            ),
            new TicketDto(
                    "BAL-1004", "سارا محمدی", "ساعات پاسخ‌گویی",
                    "چه زمانی می‌توانم با پشتیبانی تماس بگیرم؟",
                    TicketStatus.CLOSED, LocalDateTime.now().minusHours(3),
                    List.of(
                            msg("m-10", "سارا محمدی", "CUSTOMER", "ساعات پاسخ‌گویی پشتیبانی چه زمانی است؟", 180),
                            msg("m-11", "پشتیبانی", "SUPPORT", "لطفاً ساعات اعلام‌شده در صفحه تماس با ما را بررسی کنید.", 170)
                    )
            )
    );

    public List<TicketDto> findAll() {
        return tickets;
    }

    public Optional<TicketDto> findById(String id) {
        return tickets.stream().filter(ticket -> ticket.id().equals(id)).findFirst();
    }

    private static MessageDto msg(String id, String sender, String type, String content, int minutesAgo) {
        return new MessageDto(id, sender, type, content, LocalDateTime.now().minusMinutes(minutesAgo));
    }
}
