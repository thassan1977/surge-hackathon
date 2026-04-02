package com.surge.agent.config;

import com.surge.agent.contracts.MockVault;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.tx.gas.ContractGasProvider;

@Configuration
public class ContractFactory {

    @Bean
    public MockVault mockVault(
            Web3j web3j,
            Credentials credentials,
            ContractGasProvider gasProvider,
            ContractConfig config) {

        return MockVault.load(
                config.getVault(),
                web3j,
                credentials,
                gasProvider
        );
    }

    // @Bean
    // public RiskRouter riskRouter(...) { ... }
}