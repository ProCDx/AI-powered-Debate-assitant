package com.app.debate.api;

import com.app.debate.core.DebateService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class DebateController {

    private final DebateService svc;

    public DebateController(DebateService svc) {
        this.svc = svc;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("ok", "true");
    }

    @GetMapping("/debate/topics")
    public List<String> topics() {
        return svc.topics();
    }

    @GetMapping("/debate/generate")
    public Map<String, Object> generate(
            @RequestParam String topic,
            @RequestParam(defaultValue = "pro") String stance) {
        return svc.generatePack(topic, stance);
    }

    // -------------------- Solo Sparring -----------------------

    @PostMapping(value = "/spar/counter", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> counter(@RequestBody SparRequest req) {

        // Convert history: List<Turn> -> List<Map<String,Object>>
        List<Map<String, Object>> hist = new ArrayList<>();
        if (req.history != null) {
            for (SparRequest.Turn t : req.history) { // <-- use SparRequest.Turn
                Map<String, Object> m = new HashMap<>();
                if (t.speaker != null)
                    m.put("speaker", t.speaker);
                if (t.text != null)
                    m.put("text", t.text);
                if (t.cwi != null)
                    m.put("cwi", t.cwi);
                hist.add(m);
            }
        }

        String topic = req.topic == null ? "" : req.topic.trim();
        String side = req.side == null ? "pro" : req.side.trim().toLowerCase();
        String difficulty = req.difficulty == null ? "basic" : req.difficulty.trim().toLowerCase();
        String mode = req.mode == null ? "free" : req.mode.trim().toLowerCase();

        String userText = "";
        Map<String, String> cwi = null;
        if (req.userTurn != null) {
            userText = req.userTurn.text == null ? "" : req.userTurn.text.trim();
            cwi = req.userTurn.cwi;
        }

        Map<String, Object> resp = svc.sparCounter(
                topic, side, difficulty, mode, userText, cwi, hist);

        // Important: return the correctly-typed ResponseEntity
        return ResponseEntity.ok(resp);
    }

    // DTOs
    public static class SparRequest {
        public String topic;
        public String side; // pro|con
        public String difficulty; // basic|advanced
        public String mode; // free|cwi
        public Turn userTurn;
        public List<Turn> history;

        public static class Turn {
            public String speaker; // you|bot
            public String text;
            public Map<String, String> cwi;
        }
    }
}
