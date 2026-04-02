package com.surge.agent.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.surge.agent.dto.MarketState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import jakarta.annotation.PostConstruct;

/**
 * OnChainMetricsService — populates EthereumOnChainMetrics from free public APIs.
 *
 * Sources:
 *   Gas price  → Etherscan gasOracle (free key at etherscan.io/register)
 *   DeFi TVL   → DefiLlama /v2/historicalChainTvl/Ethereum (no key required)
 *
 * Wire in AutonomousTradingOrchestrator.tradingLoop():
 *   marketState.setEthOnchain(onChainMetricsService.getLatestMetrics());
 *
 * application.properties:
 *   api.etherscan.key=YOUR_FREE_KEY   (optional but recommended — higher rate limit)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnChainMetricsService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${api.etherscan.key:}")
    private String etherscanKey;

    private volatile MarketState.EthereumOnChainMetrics latestMetrics =
            new MarketState.EthereumOnChainMetrics();

    // Exponential moving average for gas z-score baseline
    private double gasAvg = 20.0;
    private double gasStd = 5.0;

    @PostConstruct
    public void init() { fetchAll(); }

    @Scheduled(fixedDelay = 60_000)
    public void scheduledFetch() { fetchAll(); }

    public MarketState.EthereumOnChainMetrics getLatestMetrics() {
        return latestMetrics;
    }

    private void fetchAll() {
        double gasPriceGwei  = 20.0;
        double gasPriceZScore = 0.0;
        double defiTvlChange = 0.0;

        // Gas price
        try {
            String url = "https://api.etherscan.io/api?module=gastracker&action=gasoracle"
                    + (etherscanKey != null && !etherscanKey.isBlank() ? "&apikey=" + etherscanKey : "");
            JsonNode result = objectMapper.readTree(
                    restTemplate.getForObject(url, String.class)).path("result");
            gasPriceGwei = result.path("ProposeGasPrice").asDouble(20.0);

            // Rolling z-score
            gasAvg = gasAvg * 0.99 + gasPriceGwei * 0.01;
            double diff = gasPriceGwei - gasAvg;
            gasStd = Math.max(1.0, gasStd * 0.99 + Math.abs(diff) * 0.01);
            gasPriceZScore = Math.round(diff / gasStd * 100.0) / 100.0;

            log.debug("Gas: {} gwei z={}", gasPriceGwei, gasPriceZScore);
        } catch (Exception e) {
            log.debug("Gas fetch failed: {}", e.getMessage());
        }

        // DeFi TVL 24h change (no key required)
        try {
            String url = "https://api.llama.fi/v2/historicalChainTvl/Ethereum";
            JsonNode arr = objectMapper.readTree(restTemplate.getForObject(url, String.class));
            if (arr.isArray() && arr.size() >= 2) {
                double latest = arr.get(arr.size() - 1).path("tvl").asDouble(0.0);
                double prev   = arr.get(arr.size() - 2).path("tvl").asDouble(0.0);
                defiTvlChange = prev > 0 ? Math.round((latest - prev) / prev * 10000.0) / 100.0 : 0.0;
                log.debug("DeFi TVL 24h change: {}%", defiTvlChange);
            }
        } catch (Exception e) {
            log.debug("DefiLlama fetch failed: {}", e.getMessage());
        }

        latestMetrics = MarketState.EthereumOnChainMetrics.builder()
                .gasPriceGwei(gasPriceGwei)
                .gasPriceZScore(gasPriceZScore)
                .defiTvlChange24h(defiTvlChange)
                .stakingApy(4.0)          // static baseline — update via Beaconchain if needed
                .validatorQueueSize(0)
                .l2NetInflowEth(0.0)
                .mevBundleCount1h(0)
                .whaleWalletInflow(0.0)
                .exchangeNetFlowEth(0.0)
                .build();
    }
}