package com.surge.agent.services;

import com.surge.agent.dto.AITradeDecision;
import com.surge.agent.dto.MarketState;
import com.surge.agent.dto.request.AnalysisRequest;
import com.surge.agent.dto.request.TradeFeedbackRequest;
import com.surge.agent.enums.MarketRegime;
import com.surge.agent.enums.RiskLevel;
import com.surge.agent.enums.TradeAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * PythonAIClient — V3 final.
 *
 * Maps to the actual Python FastAPI endpoints in app/api/router/analyze.py:
 *
 *   POST /api/v1/analyze          — analyzeMarket()   → AITradeDecision
 *   POST /api/v1/trade/feedback   — sendTradeFeedback() → void
 *   GET  /api/v1/health           — isHealthy()        → boolean
 *
 * V2 had two methods that mapped to endpoints that don't exist:
 *   assessRisk()       → /assess_risk       — never existed in Python
 *   generateArtifact() → /generate_artifact — never existed in Python
 *
 * The only endpoint Python exposes for the trade decision is /api/v1/analyze,
 * which accepts AnalysisRequest (MarketState + news + portfolio context) and
 * returns the full TradeDecision with agent verdicts, TP/SL, and trade_id.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PythonAIClient {

    private final RestTemplate restTemplate;

    @Value("${ai.brain.url:http://localhost:8000}")
    private String baseUrl;

    // ─────────────────────────────────────────────────────────────────────
    // POST /api/v1/analyze
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Sends the full market state to the Python 5-agent council and returns
     * the AI trade decision.
     *
     * Python endpoint: POST /api/v1/analyze
     * Request body:    AnalysisRequest  (market: MarketState, news: String,
     *                                    currentDrawdownPct, openPositionsCount)
     * Response:        TradeDecision    (action, confidence, reasoning, riskLevel,
     *                                    takeProfitPct, stopLossPct, agentVerdicts,
     *                                    marketRegime, tradeId, regimeConfidence)
     *
     * On any error, returns a safe HOLD decision so the trade loop doesn't crash.
     */
    public AITradeDecision analyzeMarket(MarketState marketState) {
        return analyzeMarket(marketState, "No news context available.", 0.0, 0);
    }

    /**
     * Full overload — use this when you have drawdown and position count available.
     *
     * @param marketState          live market data snapshot
     * @param newsContext          aggregated news string from NewsAggregator
     * @param currentDrawdownPct   current portfolio drawdown (0.05 = 5%)
     * @param openPositionsCount   number of currently open positions
     */
    public AITradeDecision analyzeMarket(MarketState marketState,
                                         String newsContext,
                                         double currentDrawdownPct,
                                         int openPositionsCount) {
        String url = baseUrl + "/api/v1/analyze";
        try {
            AnalysisRequest request = AnalysisRequest.builder()
                    .market(marketState)
                    .news(newsContext != null ? newsContext : "")
                    .currentDrawdownPct(currentDrawdownPct)
                    .openPositionsCount(openPositionsCount)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<AnalysisRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<AITradeDecision> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, AITradeDecision.class);

            AITradeDecision decision = response.getBody();
            if (decision == null) {
                log.error("Python /analyze returned null body — defaulting to HOLD");
                return safeHold("Null response from Python brain");
            }

            log.info("AI decision: action={} confidence={} regime={} tradeId={}",
                    decision.getAction(),
                    String.format("%.2f", decision.getConfidence()),
                    decision.getMarketRegime(),
                    decision.hasTradeId() ? decision.getTradeId() : "none");

            return decision;

        } catch (RestClientException e) {
            log.error("Python /analyze call failed (is the brain running on {}?): {}",
                    baseUrl, e.getMessage());
            return safeHold("Python brain unreachable: " + e.getMessage());
        }
    }

    /**
     * Overload that accepts a pre-built AnalysisRequest directly.
     * Use this when the caller has already assembled the request
     * (e.g. AutonomousTradingOrchestrator.tradingLoop()).
     *
     * Usage:
     *   AnalysisRequest request = new AnalysisRequest(marketState, newsContext, 0.0, 0);
     *   AITradeDecision decision = pythonAIClient.analyzeMarket(request);
     */
    public AITradeDecision analyzeMarket(AnalysisRequest prebuiltRequest) {
        String url = baseUrl + "/api/v1/analyze";
        try {
            // Ensure ethOnchain is never null — Python rejects null with 422
            if (prebuiltRequest.getMarket() != null
                    && prebuiltRequest.getMarket().getEthOnchain() == null) {
                prebuiltRequest.getMarket().setEthOnchain(
                        new MarketState.EthereumOnChainMetrics());
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<AnalysisRequest> entity = new HttpEntity<>(prebuiltRequest, headers);

            ResponseEntity<AITradeDecision> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, AITradeDecision.class);

            AITradeDecision decision = response.getBody();
            if (decision == null) {
                log.error("Python /analyze returned null body — defaulting to HOLD");
                return safeHold("Null response from Python brain");
            }

            log.info("AI decision: action={} confidence={} regime={} tradeId={}",
                    decision.getAction(),
                    String.format("%.2f", decision.getConfidence()),
                    decision.getMarketRegime(),
                    decision.hasTradeId() ? decision.getTradeId() : "none");

            return decision;

        } catch (RestClientException e) {
            log.error("Python /analyze failed (is brain running on {}?): {}", baseUrl, e.getMessage());
            return safeHold("Python brain unreachable: " + e.getMessage());
        }
    }


    // ─────────────────────────────────────────────────────────────────────
    // POST /api/v1/trade/feedback
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Called when a trade closes (TP/SL hit or reverted).
     * Updates ChromaDB so the AI learns from this trade outcome.
     * Also updates AgentPerformanceTracker for Judge weighting.
     *
     * Python endpoint: POST /api/v1/trade/feedback
     * Request body:    TradeFeedbackRequest  (trade_id, pnl, exit_reason,
     *                                         agentVerdicts, marketRegime)
     *
     * Non-fatal — a failure here doesn't affect the trade or the artifact.
     */
    public void sendTradeFeedback(TradeFeedbackRequest feedback) {
        String url = baseUrl + "/api/v1/trade/feedback";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<TradeFeedbackRequest> entity = new HttpEntity<>(feedback, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);

            String status = response.getBody() != null
                    ? (String) response.getBody().get("status") : "unknown";
            log.info("Trade feedback sent | tradeId={} pnl={} status={}",
                    feedback.getTradeId(), feedback.getPnl(), status);

        } catch (RestClientException e) {
            // Non-fatal: Python still has the trade in memory from the analyze call.
            // The agent will continue trading; only the learning loop is temporarily broken.
            log.warn("Trade feedback failed (non-fatal) for tradeId={}: {}",
                    feedback.getTradeId(), e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // GET /api/v1/health
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Lightweight health check — use in @PostConstruct or readiness probe.
     */
    public boolean isHealthy() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    baseUrl + "/api/v1/health", Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            log.warn("Python brain health check failed: {}", e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────────────

    /** Returns a safe HOLD decision when Python is unreachable or returns an error. */
    private AITradeDecision safeHold(String reason) {
        AITradeDecision hold = new AITradeDecision();
        hold.setAction(TradeAction.HOLD);
        hold.setConfidence(0.0);
        hold.setReasoning(reason);
        hold.setRiskLevel(RiskLevel.CRITICAL);
        hold.setMarketRegime(MarketRegime.RANGING);
        hold.setRegimeConfidence(0.0);
        return hold;
    }
}
