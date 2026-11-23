
package com.baleassistant.integration.bale;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bale/browser")
public class BaleBrowserController {

    private final BaleBrowserService service;

    public BaleBrowserController(BaleBrowserService service) {
        this.service = service;
    }

    @PostMapping("/start")
    public Map<String, Object> start() {
        return service.start();
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return service.getStatus();
    }


    @GetMapping("/authentication")
    public Map<String, Object> authentication() {
        return service.checkAuthentication();
    }

    @GetMapping("/inspect")
    public Map<String, Object> inspect() {
        return service.inspectChatPage();
    }
    @GetMapping("/conversations")
    public List<BaleBrowserService.BaleConversationDto> getConversations() {
        return service.extractConversations();
    }


    @GetMapping("/inspect/messages")
    public Object inspectMessages() {
        return service.inspectMessageDom();
    }

    @GetMapping("/inspect/messages/details")
    public Object inspectMessageDetails() {
        return service.inspectMessageDetails();
    }
    // Get UID of currently opened conversation
    @GetMapping("/conversation/current/uid")
    public ResponseEntity<Map<String, Object>> getCurrentConversationUid() {

        String uid = service.extractCurrentConversationUid();

        return ResponseEntity.ok(Map.of(
                "uid", uid != null ? uid : "",
                "success", uid != null
        ));
    }

    // Open conversation by UID
    @PostMapping("/conversation/{uid}/open")
    public ResponseEntity<Map<String, Object>> openConversation(
            @PathVariable String uid
    ) {
        service.openConversation(uid);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "uid", uid,
                "message", "Conversation opened successfully"
        ));
    }

    // Inspect conversation DOM elements
    @GetMapping("/conversations/inspect")
    public ResponseEntity<Object> inspectConversationLinks() {

        Object result = service.inspectConversationLinks();

        return ResponseEntity.ok(result);
    }


    @GetMapping("/conversations/{index}/uid")
    public ResponseEntity<Map<String, Object>> extractConversationUid(
            @PathVariable int index
    ) {
        String uid = service.extractConversationUidByClick(index);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "index", index,
                "uid", uid
        ));
    }
    @GetMapping("/browser/pages")
    public ResponseEntity<?> inspectBrowserPages() {
        return ResponseEntity.ok(
                service.inspectBrowserPages()
        );
    }


    @GetMapping("/messages/current")
    public ResponseEntity<?> extractCurrentMessages() {

        return ResponseEntity.ok(
                service.extractMessages()
        );
    }

}
