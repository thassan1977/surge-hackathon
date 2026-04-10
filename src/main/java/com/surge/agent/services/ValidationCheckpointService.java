package com.surge.agent.services;

import com.surge.agent.contracts.ValidationRegistry;
import com.surge.agent.dto.AITradeDecision;
import com.surge.agent.dto.MarketState;
import com.surge.agent.dto.artifact.AgentVerdict;
import com.surge.agent.enums.MarketRegime;
import com.surge.agent.enums.TradeAction;
import com.surge.agent.model.TradeIntent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.bouncycastle.crypto.digests.KeccakDigest;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ValidationCheckpointService
 *
 * Implements Step 4 of the ERC-8004 hackathon flow:
 * "Post checkpoints to ValidationRegistry after each trade decision."
 *
 * The checkpoint proves your agent's reasoning was committed BEFORE the trade
 * result was known — making it tamper-proof evidence of AI decision quality.
 *
 * Flow:
 *   1. After every trade submission (approved OR rejected), call postCheckpoint()
 *   2. Builds a checkpointHash: EIP-712 digest of {agentId, timestamp, action,
 *      pair, amountUsdScaled, priceUsdScaled, reasoningHash}
 *   3. Calls ValidationRegistry.postEIP712Attestation(agentId, hash, score, notes)
 *   4. Writes entry to checkpoints.jsonl for the audit trail judges read
 *
 * Score logic (0-100):
 *   - Base score from AI confidence (0-100)
 *   - +10 if trade was approved by RiskRouter
 *   - +10 if MACD and EMA stack confirm the direction
 *   - -20 if trade was rejected (signals, not signature error)
 *
 * ValidationRegistry address (Sepolia):
 *   0x92bF63E5C7Ac6980f237a7164Ab413BE226187F1
 *
 * ABI used:
 *   postEIP712Attestation(uint256 agentId, bytes32 checkpointHash, uint8 score, string notes)
 *   getAverageValidationScore(uint256 agentId) → uint256
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationCheckpointService {

    private final ValidationRegistry validationRegistry;
    private final Credentials         credentials;
    private final RiskManagementService riskService;

    @Value("${ai.agent.id:36}")
    private long agentIdLong;

    // checkpoints.jsonl path — judges read this for the audit trail
    private static final String CHECKPOINT_FILE = "checkpoints.jsonl";

    @PostConstruct
    public void verifyValidatorStatus() {
        try {
            boolean isValidator = validationRegistry.validators(credentials.getAddress()).send();
            log.info("=== VALIDATOR STATUS CHECK ===");
            log.info("Wallet: {}", credentials.getAddress());
            log.info("Is Authorized Validator: {}", isValidator ? "YES - Ready to Post" : "NO - Still Lagging");
            log.info("==============================");
        } catch (Exception e) {
            log.error("Could not verify validator status: {}", e.getMessage());
        }
    }
    // ─────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Posts a reputation feedback entry after every trade submission.
     * ReputationRegistry.getAverageScore(agentId) is what the leaderboard shows as "Reputation".
     * Must call this on EVERY trade — approved or rejected — to accumulate score.
     */
//    public void postReputation(
//            TradeIntent intent,
//            AITradeDecision decision,
//            MarketState market,
//            boolean tradeApproved,
//            String txHash
//    ) {
//        try {
//            BigInteger agentId = BigInteger.valueOf(agentIdLong);
//
//            // outcomeRef = keccak256 of txHash — links reputation to specific trade on-chain
//            byte[] outcomeRef = keccak256(
//                    (txHash != null ? txHash : "no_tx").getBytes(StandardCharsets.UTF_8)
//            );
//
//            int score = computeScore(decision, market, tradeApproved);
//
//            // feedbackType: 0 = TRADE_OUTCOME (primary signal for reputation)
//            BigInteger feedbackType = BigInteger.ZERO;
//
//            String shortNotes = String.format("score=%d|conf=%.2f|regime=%s|ATR=%.2f|RSI=%.2f|price=%.2f|Vwap=%.2f",
//                    score, decision.getConfidence(), decision.getMarketRegime(),
//                    market.getAtr(), market.getRsi(), market.getCurrentPrice(), market.getVwap());
//
//            TransactionReceipt receipt = reputationRegistry.submitFeedback(
//                    agentId,
//                    BigInteger.valueOf(score),
//                    outcomeRef,
//                    shortNotes,
//                    feedbackType
//            ).send();
//
//            if (receipt.isStatusOK()) {
//                log.info("Reputation feedback posted | score={}/100 | tx={}",
//                        score, receipt.getTransactionHash().substring(0, 14) + "...");
//            } else {
//                log.error("Reputation feedback reverted.");
//            }
//        } catch (Exception e) {
//            log.error("postReputation failed: {}", e.getMessage(), e);
//        }
//    }

    /**
     * Post a checkpoint to ValidationRegistry after a trade intent is submitted.
     *
     * Call this after blockchainService.executeTrade() — whether approved or rejected.
     *
     * @param intent     The TradeIntent that was submitted
     * @param decision   The AI decision (contains reasoning, confidence, regime)
     * @param market     Market state at time of decision
     * @param tradeApproved  true if RiskRouter emitted TradeApproved, false if rejected
     * @param txHash     The transaction hash of the submitTradeIntent tx
     */
    public void postCheckpoint(
            TradeIntent intent,
            AITradeDecision decision,
            MarketState market,
            boolean tradeApproved,
            String txHash
    ) {
        try {

            Thread.sleep(1000);
            BigInteger agentId = BigInteger.valueOf(agentIdLong);
            long timestamp     = Instant.now().getEpochSecond();
            double price       = market.getCurrentPrice().doubleValue();

            // ── 1. Build checkpointHash (EIP-712 digest) ─────────────────────
            byte[] checkpointHash = buildCheckpointHash(
                    agentId, timestamp, intent, price, decision.getReasoning()
            );

            // ── 2. Compute score (0-100) ──────────────────────────────────────
            int score = computeScore(decision, market, tradeApproved);

            // ── 3. Build notes string for on-chain storage ────────────────────
            String notes = buildNotes(decision, market, tradeApproved, txHash);

            // ── 4. Post to ValidationRegistry on-chain ────────────────────────
            // --- Bug in postEIP712Attestation
            // it uses this.his.postAttestation(agentId,....)
            // which reset the contract address not validator
            // ProofType.EIP712 is enum index 1
            BigInteger proofTypeEIP712 = BigInteger.valueOf(1);
            byte[] emptyProof = new byte[0];

            TransactionReceipt receipt = validationRegistry
                    .postAttestation(
                            agentId,
                            checkpointHash,
                            BigInteger.valueOf(score),
                            proofTypeEIP712,
                            emptyProof,
                            notes
                    )
                    .send();

            if (receipt.isStatusOK()) {
                log.info("Checkpoint posted | score={}/100 | tx={}",
                        score,
                        receipt.getTransactionHash().substring(0, 14) + "...");
            } else {
                log.error("Checkpoint tx reverted | hash={}", receipt.getTransactionHash());
            }

            // ── 5. Write to checkpoints.jsonl (local audit trail) ─────────────
            writeCheckpointJsonl(agentId, timestamp, intent, decision, market,
                    tradeApproved, txHash, score,
                    "0x" + bytesToHex(checkpointHash));

        } catch (Exception e) {
            log.error("postCheckpoint failed: {}", e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // CHECKPOINT HASH CONSTRUCTION
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Builds the EIP-712 digest that commits to this specific trade decision.
     *
     * Struct fields (must match what you document for judges):
     *   agentId          uint256
     *   timestamp        uint256  (unix seconds)
     *   action           string   (keccak256 of "BUY"/"SELL")
     *   pair             string   (keccak256 of "ETHUSD")
     *   amountUsdScaled  uint256
     *   priceUsdScaled   uint256  (price * 100, e.g. $2178.48 → 217848)
     *   reasoningHash    bytes32  (keccak256 of the AI reasoning string)
     */
    private byte[] buildCheckpointHash(
            BigInteger agentId,
            long timestamp,
            TradeIntent intent,
            double price,
            String reasoning
    ) {
        // Type string — must be documented for judges to verify
        String typeString = "TradeCheckpoint(" +
                "uint256 agentId," +
                "uint256 timestamp," +
                "string action," +
                "string pair," +
                "uint256 amountUsdScaled," +
                "uint256 priceUsdScaled," +
                "bytes32 reasoningHash)";

        byte[] typeHash     = keccak256(typeString.getBytes(StandardCharsets.UTF_8));
        byte[] reasoningHash = keccak256(reasoning.getBytes(StandardCharsets.UTF_8));

        // priceUsdScaled: $2178.485 → 217848 (integer, * 100, floor)
        long priceScaled = (long)(price * 100);

        // abi.encode the struct: 8 slots × 32 bytes = 256 bytes
        ByteBuffer buf = ByteBuffer.allocate(256);
        buf.put(typeHash);                                              // 32
        buf.put(uint256(agentId));                                      // 32
        buf.put(uint256(BigInteger.valueOf(timestamp)));                // 32
        buf.put(keccak256(intent.getAction().getBytes(StandardCharsets.UTF_8)));  // 32 (dynamic)
        buf.put(keccak256(intent.getPair().getBytes(StandardCharsets.UTF_8)));    // 32 (dynamic)
        buf.put(uint256(intent.getAmountUsdScaled()));                  // 32
        buf.put(uint256(BigInteger.valueOf(priceScaled)));              // 32
        buf.put(reasoningHash);                                         // 32

        byte[] structHash = keccak256(buf.array());

        // EIP-712 digest with AgentRegistry domain
        // Domain: name="AITradingAgent", version="1", chainId=11155111
        //         verifyingContract=0x97b07dDc405B0c28B17559aFFE63BdB3632d0ca3
        byte[] domainSeparator = buildAgentRegistryDomain();

        ByteBuffer digestBuf = ByteBuffer.allocate(66);
        digestBuf.put((byte) 0x19);
        digestBuf.put((byte) 0x01);
        digestBuf.put(domainSeparator);
        digestBuf.put(structHash);

        return keccak256(digestBuf.array());
    }

    /**
     * AgentRegistry EIP-712 domain for checkpoint signing.
     * From the docs: name="AITradingAgent", version="1", chainId=11155111
     * verifyingContract=0x97b07dDc405B0c28B17559aFFE63BdB3632d0ca3
     */
    private byte[] buildAgentRegistryDomain() {
        String domainType = "EIP712Domain(string name,string version,uint256 chainId,address verifyingContract)";
        byte[] domainTypeHash = keccak256(domainType.getBytes(StandardCharsets.UTF_8));
        byte[] nameHash       = keccak256("AITradingAgent".getBytes(StandardCharsets.UTF_8));
        byte[] versionHash    = keccak256("1".getBytes(StandardCharsets.UTF_8));
        String agentRegistry  = "0x97b07dDc405B0c28B17559aFFE63BdB3632d0ca3";

        ByteBuffer buf = ByteBuffer.allocate(160); // 5 × 32
        buf.put(domainTypeHash);
        buf.put(nameHash);
        buf.put(versionHash);
        buf.put(uint256(BigInteger.valueOf(11155111L))); // Sepolia chainId
        buf.put(address(agentRegistry));

        return keccak256(buf.array());
    }

    // ─────────────────────────────────────────────────────────────────────
    // SCORE COMPUTATION
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Score 0-100 representing the quality of this trade decision.
     * Higher = better validated reasoning.
     */
    private int computeScore(AITradeDecision decision, MarketState market, boolean tradeApproved) {
        TradeAction action = decision.getAction();

        // ── 1. Curved Confidence (0-50 pts) ──────────────────────────────────────
        // Refinement: Use multiplier for signals > 0.70 to reward high conviction.
        double rawConf = decision.getConfidence();
        double curvedConf = Math.min(1.0, rawConf * 1.2);
        int ptsConf = (int) (curvedConf * 50);

        // ── 2. RiskRouter approved (15 pts) ──────────────────────────────────────
        int ptsApproved = tradeApproved ? 15 : 0;

        // ── 3. Weighted Trend Alignment (0-10 pts) ────────────────────────────────
        // (Location + Velocity)
        int ptsTrend = (int) (indicatorsAlignedWeight(decision, market) * 10);

        // ── 4. R:R ratio quality (0-10 pts) ──────────────────────────────────────
        // Reward disciplined setups. 2.0+ is the gold standard.
        int ptsRr = 0;
        double rr = decision.getRewardRiskRatio();
        if      (rr >= 2.0) ptsRr = 10;
        else if (rr >= 1.5) ptsRr = 5;

        // ── 5. Momentum Flow (0-5 pts) ───────────────────────────────────────────
        // Check if we are "swimming with the tide" (Histogram direction)
        int ptsMom = 0;
        double macdHist = market.getMacdHistogram();
        if (TradeAction.BUY.equals(action)  && macdHist > 0) ptsMom = 5;
        if (TradeAction.SELL.equals(action) && macdHist < 0) ptsMom = 5;

        // ── 6. Regime Synergy (0-5 pts) ──────────────────────────────────────────
        int ptsRegime = 0;
        MarketRegime regime = decision.getMarketRegime();
        if (TradeAction.BUY.equals(action) && (regime == MarketRegime.BULL_TREND || regime == MarketRegime.ACCUMULATION)) {
            ptsRegime = 5;
        } else if (TradeAction.SELL.equals(action) && (regime == MarketRegime.BEAR_TREND || regime == MarketRegime.DISTRIBUTION)) {
            ptsRegime = 5;
        }

        // ── 7. Agent Council Consensus (0-5 pts) ─────────────────────────────────
        // Using the Active-Only Consensus logic to ignore "HOLD" votes
        int ptsCons = 0;
        if (decision.hasAgentVerdicts()) {
            String actionName = action != null ? action.name() : "";
            List<AgentVerdict> activeVerdicts = decision.getAgentVerdicts().stream()
                    .filter(v -> !"HOLD".equals(v.getSignal()))
                    .toList();

            if (!activeVerdicts.isEmpty()) {
                long agreeing = activeVerdicts.stream()
                        .filter(v -> actionName.equals(v.getSignal()))
                        .count();
                ptsCons = (int) Math.round(((double) agreeing / activeVerdicts.size()) * 5.0);
            }
        }

        // ── Final Calculation ───────────────────────────────────────────────────
        int rawScore = ptsConf + ptsApproved + ptsTrend + ptsRr + ptsMom + ptsRegime + ptsCons;
        int finalScore = Math.min(100, rawScore);

        String tradeId = decision.getTradeId() != null ? decision.getTradeId() : "UNKNOWN";

        // Detailed Log for Forensic Audit
        log.info("[Score Composition] Trade {} | Final: {}/100 | " +
                        "Conf: {}/50 (Raw: {}), Apprv: {}/15, Trend: {}/10, RR: {}/10, Mom: {}/5, Reg: {}/5, Cons: {}/5",
                tradeId, finalScore, ptsConf, String.format("%.2f", rawConf),
                ptsApproved, ptsTrend, ptsRr, ptsMom, ptsRegime, ptsCons);

        return finalScore;
    }
    /**
     * Returns weighted indicator result
     */
    private double indicatorsAlignedWeight(AITradeDecision decision, MarketState market) {
        TradeAction action = decision.getAction();
        double price = market.getCurrentPrice().doubleValue();
        double ema50 = market.getEma50();
        double ema200 = market.getEma200();
        double slope50 = market.getEma50Slope();

        if (ema200 <= 0) return 0.0;

        double weight = 0.0;

        if (TradeAction.BUY.equals(action)) {
            // 1. Location (50% of weight): Is price above the long-term anchor?
            if (price > ema200) weight += 0.5;

            // 2. Velocity (50% of weight): Is the short-term trend moving up?
            if (slope50 > 0) weight += 0.5;
            // Bonus: Extra credit if price is specifically above EMA50 too
            if (price > ema50) weight = Math.min(1.0, weight + 0.1);
        }
        else if (TradeAction.SELL.equals(action)) {
            // 1. Location: Is price below the anchor?
            if (price < ema200) weight += 0.5;

            // 2. Velocity: Is the short-term trend falling?
            if (slope50 < 0) weight += 0.5;
            // Bonus
            if (price < ema50) weight = Math.min(1.0, weight + 0.1);
        }

        return weight;
    }
    // ─────────────────────────────────────────────────────────────────────
    // NOTES STRING
    // ─────────────────────────────────────────────────────────────────────
    private String buildNotes(
            AITradeDecision decision,
            MarketState market,
            boolean tradeApproved,
            String txHash
    ) {
        // 1. Calculate Risk Adjusted Metrics
        double sharpe = riskService.getSharpeRatio();
        double dd     = riskService.getCurrentDrawdownPct() * 100; // Convert to %
        double wr     = riskService.getWinRate30() * 100;
        double rr     = decision.getRewardRiskRatio();

        // 2. Compact string for Gas Efficiency (Targeting < 150 chars)
        return String.format(
                "act=%s|sh=%.2f|dd=%.1f%%|wr=%.0f%%|rr=%.2f|conf=%.0f|reg=%s|tx=%s",
                decision.getAction(),
                sharpe,
                dd,
                wr,
                rr,
                decision.getConfidence() * 100,
                decision.getMarketRegime(),
                txHash != null ? txHash.substring(0, 8) : "none"
        );
    }
    private String buildRichNotes(
            AITradeDecision decision,
            MarketState market,
            boolean tradeApproved,
            String txHash
    ) {
        // Format: key=value pairs separated by |
        // Judges parse this to evaluate decision quality
        return String.format(
                "action=%s|" +
                        "confidence=%.0f|" +
                        "regime=%s|" +
                        "approved=%s|" +
                        // ── Price context ──
                        "price=%.2f|" +
                        "vwap=%.2f|" +
                        "priceVsVwap=%s|" +
                        // ── RSI with label ──
                        "rsi=%.1f|" +
                        "rsiZone=%s|" +
                        // ── Momentum ──
                        "macdHist=%.4f|" +
                        "macdSignal=%s|" +
                        "ema50Slope=%.4f|" +
                        "emaStack=%s|" +
                        // ── Volatility ──
                        "atr=%.4f|" +
                        "bbWidth=%.4f|" +
                        "bbSqueeze=%s|" +
                        // ── Microstructure ──
                        "obi=%.3f|" +
                        "volumeDelta=%.1f|" +
                        "bidAskSpread=%.4f|" +
                        // ── Sentiment ──
                        "fearGreed=%d|" +
                        "fearGreedZone=%s|" +
                        "sentiment=%.3f|" +
                        // ── Risk params ──
                        "tp=%.4f|" +
                        "sl=%.4f|" +
                        "rr=%.2f|" +
                        "amount=%s|" +
                        // ── Identity ──
                        "agentId=%d|" +
                        "txHash=%s",

                // action
                decision.getAction(),
                decision.getConfidence() * 100,
                decision.getMarketRegime(),
                tradeApproved ? "YES" : "NO",

                // price
                market.getCurrentPrice().doubleValue(),
                market.getVwap(),
                market.getCurrentPrice().doubleValue() > market.getVwap() ? "ABOVE" : "BELOW",

                // rsi
                market.getRsi(),
                rsiZone(market.getRsi()),

                // momentum
                market.getMacdHistogram(),
                market.getMacdHistogram() > 0 ? "BULL" : "BEAR",
                market.getEma50Slope(),
                emaStackLabel(market),

                // volatility
                market.getAtr(),
                market.getBbWidth(),
                market.isBbSqueeze() ? "YES" : "NO",

                // microstructure
                market.getOrderBookImbalance(),
                market.getVolumeDelta(),
                market.getBidAskSpreadDollars(),

                // sentiment
                market.getFearGreedIndex(),
                fearGreedZone(market.getFearGreedIndex()),
                market.getRollingSentimentScore(),

                // risk
                decision.getTakeProfitPct(),
                decision.getStopLossPct(),
                decision.getStopLossPct() > 0
                        ? decision.getTakeProfitPct() / decision.getStopLossPct() : 0.0,
                decision.hasTradeId() ? decision.getTradeId() : "none",

                // identity
                agentIdLong,
                txHash != null ? txHash : "none"
        );
    }

    private static String rsiZone(double rsi) {
        if (rsi < 30)  return "OVERSOLD";
        if (rsi < 45)  return "BEARISH";
        if (rsi < 55)  return "NEUTRAL";
        if (rsi < 70)  return "BULLISH";
        return "OVERBOUGHT";
    }

    private static String fearGreedZone(int fg) {
        if (fg <= 20)  return "EXTREME_FEAR";
        if (fg <= 40)  return "FEAR";
        if (fg <= 60)  return "NEUTRAL";
        if (fg <= 80)  return "GREED";
        return "EXTREME_GREED";
    }

    private static String emaStackLabel(MarketState m) {
        double price = m.getCurrentPrice().doubleValue();
        if (m.getEma50() > 0 && m.getEma100() > 0 && m.getEma200() > 0) {
            if (price > m.getEma50() && m.getEma50() > m.getEma100() && m.getEma100() > m.getEma200())
                return "FULL_BULL";
            if (price < m.getEma50() && m.getEma50() < m.getEma100() && m.getEma100() < m.getEma200())
                return "FULL_BEAR";
        }
        if (m.getEma50() > 0 && price > m.getEma50()) return "ABOVE_EMA50";
        if (m.getEma50() > 0 && price < m.getEma50()) return "BELOW_EMA50";
        return "UNKNOWN";
    }
    // ─────────────────────────────────────────────────────────────────────
    // LOCAL AUDIT TRAIL
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Writes one JSON line per checkpoint to checkpoints.jsonl.
     * Judges can open this file and verify the reasoning audit trail.
     */
    private void writeCheckpointJsonl(
            BigInteger agentId,
            long timestamp,
            TradeIntent intent,
            AITradeDecision decision,
            MarketState market,
            boolean approved,
            String txHash,
            int score,
            String checkpointHashHex
    ) {
        try (FileWriter fw = new FileWriter(CHECKPOINT_FILE, true)) {
            String iso = DateTimeFormatter.ISO_INSTANT
                    .format(Instant.ofEpochSecond(timestamp).atOffset(ZoneOffset.UTC));

            // Write one compact JSON line
            String line = String.format(
                    "{\"ts\":\"%s\",\"agentId\":%s,\"action\":\"%s\",\"pair\":\"%s\"," +
                            "\"amountUsdScaled\":%s,\"price\":%.4f," +
                            "\"confidence\":%.4f,\"regime\":\"%s\"," +
                            "\"rsi\":%.2f,\"macd\":%.5f,\"fearGreed\":%d," +
                            "\"approved\":%s,\"score\":%d," +
                            "\"checkpointHash\":\"%s\",\"txHash\":\"%s\"," +
                            "\"reasoning\":%s}\n",
                    iso,
                    agentId,
                    intent.getAction(),
                    intent.getPair(),
                    intent.getAmountUsdScaled(),
                    market.getCurrentPrice().doubleValue(),
                    decision.getConfidence(),
                    decision.getMarketRegime(),
                    market.getRsi(),
                    market.getMacdHistogram(),
                    market.getFearGreedIndex(),
                    approved,
                    score,
                    checkpointHashHex,
                    txHash != null ? txHash : "null",
                    jsonString(decision.getReasoning())
            );
            fw.write(line);
            log.debug("Checkpoint written to {}", CHECKPOINT_FILE);
        } catch (IOException e) {
            log.warn("Failed to write checkpoint file: {}", e.getMessage());
        }
    }

    private static String jsonString(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "") + "\"";
    }

    // ─────────────────────────────────────────────────────────────────────
    // ENCODING UTILITIES (mirrors EIP712Signer)
    // ─────────────────────────────────────────────────────────────────────

    private static byte[] keccak256(byte[] input) {
        KeccakDigest digest = new KeccakDigest(256);
        digest.update(input, 0, input.length);
        byte[] out = new byte[32];
        digest.doFinal(out, 0);
        return out;
    }

    private static byte[] uint256(BigInteger value) {
        if (value == null) value = BigInteger.ZERO;
        byte[] raw    = value.toByteArray();
        byte[] padded = new byte[32];
        int srcStart  = (raw.length > 32) ? raw.length - 32 : 0;
        int dstStart  = 32 - (raw.length - srcStart);
        System.arraycopy(raw, srcStart, padded, dstStart, raw.length - srcStart);
        return padded;
    }

    private static byte[] address(String hex) {
        if (hex == null || hex.isBlank()) return new byte[32];
        byte[] addr = org.web3j.utils.Numeric.hexStringToByteArray(
                hex.startsWith("0x") ? hex.substring(2) : hex);
        byte[] padded  = new byte[32];
        int srcLen = Math.min(addr.length, 20);
        System.arraycopy(addr, addr.length - srcLen, padded, 32 - srcLen, srcLen);
        return padded;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}