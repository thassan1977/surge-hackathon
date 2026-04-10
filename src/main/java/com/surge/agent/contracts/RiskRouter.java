package com.surge.agent.contracts;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.DynamicStruct;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes1;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple2;
import org.web3j.tuples.generated.Tuple4;
import org.web3j.tuples.generated.Tuple7;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/LFDT-web3j/web3j/tree/main/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 1.8.0.
 */
@SuppressWarnings("rawtypes")
@Generated("org.web3j.codegen.SolidityFunctionWrapperGenerator")
public class RiskRouter extends Contract {
    public static final String BINARY = "Bin file was not provided";

    public static final String FUNC_TRADE_INTENT_TYPEHASH = "TRADE_INTENT_TYPEHASH";

    public static final String FUNC_AGENTREGISTRY = "agentRegistry";

    public static final String FUNC_DOMAINSEPARATOR = "domainSeparator";

    public static final String FUNC_EIP712DOMAIN = "eip712Domain";

    public static final String FUNC_GETINTENTNONCE = "getIntentNonce";

    public static final String FUNC_GETTRADERECORD = "getTradeRecord";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_RISKPARAMS = "riskParams";

    public static final String FUNC_SETRISKPARAMS = "setRiskParams";

    public static final String FUNC_SIMULATEINTENT = "simulateIntent";

    public static final String FUNC_SUBMITTRADEINTENT = "submitTradeIntent";

    public static final Event EIP712DOMAINCHANGED_EVENT = new Event("EIP712DomainChanged", 
            Arrays.<TypeReference<?>>asList());
    ;

    public static final Event RISKPARAMSSET_EVENT = new Event("RiskParamsSet", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event TRADEAPPROVED_EVENT = new Event("TradeApproved", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Bytes32>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event TRADEINTENTSUBMITTED_EVENT = new Event("TradeIntentSubmitted", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Bytes32>(true) {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event TRADEREJECTED_EVENT = new Event("TradeRejected", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Bytes32>(true) {}, new TypeReference<Utf8String>() {}));
    ;

    @Deprecated
    protected RiskRouter(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected RiskRouter(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected RiskRouter(String contractAddress, Web3j web3j, TransactionManager transactionManager,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected RiskRouter(String contractAddress, Web3j web3j, TransactionManager transactionManager,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<EIP712DomainChangedEventResponse> getEIP712DomainChangedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(EIP712DOMAINCHANGED_EVENT, transactionReceipt);
        ArrayList<EIP712DomainChangedEventResponse> responses = new ArrayList<EIP712DomainChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            EIP712DomainChangedEventResponse typedResponse = new EIP712DomainChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static EIP712DomainChangedEventResponse getEIP712DomainChangedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(EIP712DOMAINCHANGED_EVENT, log);
        EIP712DomainChangedEventResponse typedResponse = new EIP712DomainChangedEventResponse();
        typedResponse.log = log;
        return typedResponse;
    }

    public Flowable<EIP712DomainChangedEventResponse> eIP712DomainChangedEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getEIP712DomainChangedEventFromLog(log));
    }

    public Flowable<EIP712DomainChangedEventResponse> eIP712DomainChangedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(EIP712DOMAINCHANGED_EVENT));
        return eIP712DomainChangedEventFlowable(filter);
    }

    public static List<RiskParamsSetEventResponse> getRiskParamsSetEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(RISKPARAMSSET_EVENT, transactionReceipt);
        ArrayList<RiskParamsSetEventResponse> responses = new ArrayList<RiskParamsSetEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RiskParamsSetEventResponse typedResponse = new RiskParamsSetEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.maxPositionUsdScaled = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.maxTradesPerHour = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RiskParamsSetEventResponse getRiskParamsSetEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(RISKPARAMSSET_EVENT, log);
        RiskParamsSetEventResponse typedResponse = new RiskParamsSetEventResponse();
        typedResponse.log = log;
        typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.maxPositionUsdScaled = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.maxTradesPerHour = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<RiskParamsSetEventResponse> riskParamsSetEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRiskParamsSetEventFromLog(log));
    }

    public Flowable<RiskParamsSetEventResponse> riskParamsSetEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(RISKPARAMSSET_EVENT));
        return riskParamsSetEventFlowable(filter);
    }

    public static List<TradeApprovedEventResponse> getTradeApprovedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(TRADEAPPROVED_EVENT, transactionReceipt);
        ArrayList<TradeApprovedEventResponse> responses = new ArrayList<TradeApprovedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            TradeApprovedEventResponse typedResponse = new TradeApprovedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.intentHash = (byte[]) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.amountUsdScaled = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static TradeApprovedEventResponse getTradeApprovedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(TRADEAPPROVED_EVENT, log);
        TradeApprovedEventResponse typedResponse = new TradeApprovedEventResponse();
        typedResponse.log = log;
        typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.intentHash = (byte[]) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.amountUsdScaled = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<TradeApprovedEventResponse> tradeApprovedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getTradeApprovedEventFromLog(log));
    }

    public Flowable<TradeApprovedEventResponse> tradeApprovedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(TRADEAPPROVED_EVENT));
        return tradeApprovedEventFlowable(filter);
    }

    public static List<TradeIntentSubmittedEventResponse> getTradeIntentSubmittedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(TRADEINTENTSUBMITTED_EVENT, transactionReceipt);
        ArrayList<TradeIntentSubmittedEventResponse> responses = new ArrayList<TradeIntentSubmittedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            TradeIntentSubmittedEventResponse typedResponse = new TradeIntentSubmittedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.intentHash = (byte[]) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.pair = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.action = (String) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.amountUsdScaled = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static TradeIntentSubmittedEventResponse getTradeIntentSubmittedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(TRADEINTENTSUBMITTED_EVENT, log);
        TradeIntentSubmittedEventResponse typedResponse = new TradeIntentSubmittedEventResponse();
        typedResponse.log = log;
        typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.intentHash = (byte[]) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.pair = (String) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.action = (String) eventValues.getNonIndexedValues().get(1).getValue();
        typedResponse.amountUsdScaled = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<TradeIntentSubmittedEventResponse> tradeIntentSubmittedEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getTradeIntentSubmittedEventFromLog(log));
    }

    public Flowable<TradeIntentSubmittedEventResponse> tradeIntentSubmittedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(TRADEINTENTSUBMITTED_EVENT));
        return tradeIntentSubmittedEventFlowable(filter);
    }

    public static List<TradeRejectedEventResponse> getTradeRejectedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(TRADEREJECTED_EVENT, transactionReceipt);
        ArrayList<TradeRejectedEventResponse> responses = new ArrayList<TradeRejectedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            TradeRejectedEventResponse typedResponse = new TradeRejectedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.intentHash = (byte[]) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.reason = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static TradeRejectedEventResponse getTradeRejectedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(TRADEREJECTED_EVENT, log);
        TradeRejectedEventResponse typedResponse = new TradeRejectedEventResponse();
        typedResponse.log = log;
        typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.intentHash = (byte[]) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.reason = (String) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<TradeRejectedEventResponse> tradeRejectedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getTradeRejectedEventFromLog(log));
    }

    public Flowable<TradeRejectedEventResponse> tradeRejectedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(TRADEREJECTED_EVENT));
        return tradeRejectedEventFlowable(filter);
    }

    public RemoteFunctionCall<byte[]> TRADE_INTENT_TYPEHASH() {
        final Function function = new Function(FUNC_TRADE_INTENT_TYPEHASH, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<String> agentRegistry() {
        final Function function = new Function(FUNC_AGENTREGISTRY, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<byte[]> domainSeparator() {
        final Function function = new Function(FUNC_DOMAINSEPARATOR, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<Tuple7<byte[], String, String, BigInteger, String, byte[], List<BigInteger>>> eip712Domain(
            ) {
        final Function function = new Function(FUNC_EIP712DOMAIN, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes1>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}, new TypeReference<Address>() {}, new TypeReference<Bytes32>() {}, new TypeReference<DynamicArray<Uint256>>() {}));
        return new RemoteFunctionCall<Tuple7<byte[], String, String, BigInteger, String, byte[], List<BigInteger>>>(function,
                new Callable<Tuple7<byte[], String, String, BigInteger, String, byte[], List<BigInteger>>>() {
                    @Override
                    public Tuple7<byte[], String, String, BigInteger, String, byte[], List<BigInteger>> call(
                            ) throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple7<byte[], String, String, BigInteger, String, byte[], List<BigInteger>>(
                                (byte[]) results.get(0).getValue(), 
                                (String) results.get(1).getValue(), 
                                (String) results.get(2).getValue(), 
                                (BigInteger) results.get(3).getValue(), 
                                (String) results.get(4).getValue(), 
                                (byte[]) results.get(5).getValue(), 
                                convertToNative((List<Uint256>) results.get(6).getValue()));
                    }
                });
    }

    public RemoteFunctionCall<BigInteger> getIntentNonce(BigInteger agentId) {
        final Function function = new Function(FUNC_GETINTENTNONCE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(agentId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<Tuple2<BigInteger, BigInteger>> getTradeRecord(BigInteger agentId) {
        final Function function = new Function(FUNC_GETTRADERECORD, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(agentId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
        return new RemoteFunctionCall<Tuple2<BigInteger, BigInteger>>(function,
                new Callable<Tuple2<BigInteger, BigInteger>>() {
                    @Override
                    public Tuple2<BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple2<BigInteger, BigInteger>(
                                (BigInteger) results.get(0).getValue(), 
                                (BigInteger) results.get(1).getValue());
                    }
                });
    }

    public RemoteFunctionCall<String> owner() {
        final Function function = new Function(FUNC_OWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<Tuple4<BigInteger, BigInteger, BigInteger, Boolean>> riskParams(
            BigInteger param0) {
        final Function function = new Function(FUNC_RISKPARAMS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Bool>() {}));
        return new RemoteFunctionCall<Tuple4<BigInteger, BigInteger, BigInteger, Boolean>>(function,
                new Callable<Tuple4<BigInteger, BigInteger, BigInteger, Boolean>>() {
                    @Override
                    public Tuple4<BigInteger, BigInteger, BigInteger, Boolean> call() throws
                            Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple4<BigInteger, BigInteger, BigInteger, Boolean>(
                                (BigInteger) results.get(0).getValue(), 
                                (BigInteger) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue(), 
                                (Boolean) results.get(3).getValue());
                    }
                });
    }

    public RemoteFunctionCall<TransactionReceipt> setRiskParams(BigInteger agentId,
            BigInteger maxPositionUsdScaled, BigInteger maxDrawdownBps,
            BigInteger maxTradesPerHour) {
        final Function function = new Function(
                FUNC_SETRISKPARAMS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(agentId), 
                new org.web3j.abi.datatypes.generated.Uint256(maxPositionUsdScaled), 
                new org.web3j.abi.datatypes.generated.Uint256(maxDrawdownBps), 
                new org.web3j.abi.datatypes.generated.Uint256(maxTradesPerHour)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Tuple2<Boolean, String>> simulateIntent(TradeIntent intent) {
        final Function function = new Function(FUNC_SIMULATEINTENT, 
                Arrays.<Type>asList(intent), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}, new TypeReference<Utf8String>() {}));
        return new RemoteFunctionCall<Tuple2<Boolean, String>>(function,
                new Callable<Tuple2<Boolean, String>>() {
                    @Override
                    public Tuple2<Boolean, String> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple2<Boolean, String>(
                                (Boolean) results.get(0).getValue(), 
                                (String) results.get(1).getValue());
                    }
                });
    }

    public RemoteFunctionCall<TransactionReceipt> submitTradeIntent(TradeIntent intent,
            byte[] signature) {
        final Function function = new Function(
                FUNC_SUBMITTRADEINTENT, 
                Arrays.<Type>asList(intent, 
                new org.web3j.abi.datatypes.DynamicBytes(signature)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static RiskRouter load(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        return new RiskRouter(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static RiskRouter load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new RiskRouter(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static RiskRouter load(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        return new RiskRouter(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static RiskRouter load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new RiskRouter(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static class TradeIntent extends DynamicStruct {
        public BigInteger agentId;

        public String agentWallet;

        public String pair;

        public String action;

        public BigInteger amountUsdScaled;

        public BigInteger maxSlippageBps;

        public BigInteger nonce;

        public BigInteger deadline;

        public TradeIntent(BigInteger agentId, String agentWallet, String pair, String action,
                BigInteger amountUsdScaled, BigInteger maxSlippageBps, BigInteger nonce,
                BigInteger deadline) {
            super(new org.web3j.abi.datatypes.generated.Uint256(agentId), 
                    new org.web3j.abi.datatypes.Address(160, agentWallet), 
                    new org.web3j.abi.datatypes.Utf8String(pair), 
                    new org.web3j.abi.datatypes.Utf8String(action), 
                    new org.web3j.abi.datatypes.generated.Uint256(amountUsdScaled), 
                    new org.web3j.abi.datatypes.generated.Uint256(maxSlippageBps), 
                    new org.web3j.abi.datatypes.generated.Uint256(nonce), 
                    new org.web3j.abi.datatypes.generated.Uint256(deadline));
            this.agentId = agentId;
            this.agentWallet = agentWallet;
            this.pair = pair;
            this.action = action;
            this.amountUsdScaled = amountUsdScaled;
            this.maxSlippageBps = maxSlippageBps;
            this.nonce = nonce;
            this.deadline = deadline;
        }

        public TradeIntent(Uint256 agentId, Address agentWallet, Utf8String pair, Utf8String action,
                Uint256 amountUsdScaled, Uint256 maxSlippageBps, Uint256 nonce, Uint256 deadline) {
            super(agentId, agentWallet, pair, action, amountUsdScaled, maxSlippageBps, nonce, deadline);
            this.agentId = agentId.getValue();
            this.agentWallet = agentWallet.getValue();
            this.pair = pair.getValue();
            this.action = action.getValue();
            this.amountUsdScaled = amountUsdScaled.getValue();
            this.maxSlippageBps = maxSlippageBps.getValue();
            this.nonce = nonce.getValue();
            this.deadline = deadline.getValue();
        }
    }

    public static class EIP712DomainChangedEventResponse extends BaseEventResponse {
    }

    public static class RiskParamsSetEventResponse extends BaseEventResponse {
        public BigInteger agentId;

        public BigInteger maxPositionUsdScaled;

        public BigInteger maxTradesPerHour;
    }

    public static class TradeApprovedEventResponse extends BaseEventResponse {
        public BigInteger agentId;

        public byte[] intentHash;

        public BigInteger amountUsdScaled;
    }

    public static class TradeIntentSubmittedEventResponse extends BaseEventResponse {
        public BigInteger agentId;

        public byte[] intentHash;

        public String pair;

        public String action;

        public BigInteger amountUsdScaled;
    }

    public static class TradeRejectedEventResponse extends BaseEventResponse {
        public BigInteger agentId;

        public byte[] intentHash;

        public String reason;
    }
}
