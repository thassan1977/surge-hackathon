package com.surge.agent.controllers;

import com.surge.agent.model.TradeSignal;
import com.surge.agent.services.IdentityService;
import com.surge.agent.services.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;

@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final IdentityService identityService;
    private final TradeService tradeService;

    @GetMapping("/identity")
    public ResponseEntity<BigInteger> getIdentity() {
        if (identityService.isRegistered()) {
            return ResponseEntity.ok(identityService.getAgentId());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/trade/manual")
    public ResponseEntity<String> manualTrade(@RequestBody TradeSignal signal) {
        log.info("Manual trade triggered with signal: {}", signal);
        tradeService.processSignal(signal);
        return ResponseEntity.accepted().body("Trade processing started");
    }

    @PostMapping("/register")
    public ResponseEntity<BigInteger> registerAgent(@RequestParam String metadataUri) throws Exception {
        BigInteger agentId = identityService.registerAgent(metadataUri);
        return ResponseEntity.ok(agentId);
    }
}