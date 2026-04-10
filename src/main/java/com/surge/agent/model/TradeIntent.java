package com.surge.agent.model;

import com.surge.agent.contracts.RiskRouter;
import lombok.Builder;
import lombok.Data;
import java.math.BigInteger;


@Data
@Builder
public class TradeIntent {
    private BigInteger agentId;
    private String agentWallet;     // address
    private String pair;            // string, e.g., "ETHUSD"
    private String action;          // string, e.g., "BUY" or "SELL"
    private BigInteger amountUsdScaled; // uint256, e.g., 50000 for $500.00
    private BigInteger maxSlippageBps;  // uint256, e.g., 50 for 0.5%
    private BigInteger nonce;       // uint256
    private BigInteger deadline;    // uint256

    public RiskRouter.TradeIntent toContractStruct() {
        return new RiskRouter.TradeIntent(
                this.agentId,
                this.agentWallet,
                this.pair,
                this.action,
                this.amountUsdScaled,
                this.maxSlippageBps,
                this.nonce,
                this.deadline
        );
    }
}