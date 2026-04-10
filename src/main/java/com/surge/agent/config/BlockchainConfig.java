package com.surge.agent.config;

import com.surge.agent.contracts.HackathonVault;
import com.surge.agent.contracts.ReputationRegistry;
import com.surge.agent.contracts.RiskRouter;
import com.surge.agent.contracts.ValidationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.gas.StaticGasProvider;

import java.math.BigInteger;

@Configuration
public class BlockchainConfig {

    @Bean
    public RiskRouter riskRouter(Web3j web3j, Credentials credentials,
                                 ContractConfig config, DefaultGasProvider gasProvider) {
        return RiskRouter.load(config.getRouter(), web3j, credentials, gasProvider);
    }

    @Bean
    public ValidationRegistry validationRegistry(Web3j web3j, Credentials credentials,
                                                 ContractConfig config) {
        StaticGasProvider customProvider = new StaticGasProvider(
                BigInteger.valueOf(30_000_000_000L), // 30 Gwei
                BigInteger.valueOf(1_400_000L)       // 1M Gas Limit safety buffer
        );
        return ValidationRegistry.load(config.getValidation(), web3j, credentials, customProvider);
    }

    @Bean
    public HackathonVault hackathonVault(Web3j web3j, Credentials credentials,
                                         ContractConfig config, DefaultGasProvider gasProvider) {
        return HackathonVault.load(config.getVault(), web3j, credentials, gasProvider);
    }
}
