package com.surge.agent.model;

import lombok.Builder;
import lombok.Data;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;

import org.web3j.abi.datatypes.Type;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

@Data
@Builder
public class TradeIntent {
    private BigInteger agentId;
    private String tokenIn;
    private String tokenOut;
    private BigInteger amountIn;
    private BigInteger minAmountOut;
    private BigInteger nonce;
    private BigInteger deadline;
    private byte[] riskParams; // Ensure this is 32 bytes

    public List<Type> toSolidityStruct() {
        return Arrays.asList(
                new Uint256(agentId),
                new Address(tokenIn),
                new Address(tokenOut),
                new Uint256(amountIn),
                new Uint256(minAmountOut),
                new Uint256(nonce),
                new Uint256(deadline),
                new Bytes32(riskParams)
        );
    }
}