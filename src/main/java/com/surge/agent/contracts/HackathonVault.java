package com.surge.agent.contracts;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
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
public class HackathonVault extends Contract {
    public static final String BINARY = "Bin file was not provided";

    public static final String FUNC_AGENTREGISTRY = "agentRegistry";

    public static final String FUNC_ALLOCATE = "allocate";

    public static final String FUNC_ALLOCATEDCAPITAL = "allocatedCapital";

    public static final String FUNC_ALLOCATIONPERTEAM = "allocationPerTeam";

    public static final String FUNC_CLAIMALLOCATION = "claimAllocation";

    public static final String FUNC_DEPOSIT = "deposit";

    public static final String FUNC_GETBALANCE = "getBalance";

    public static final String FUNC_HASCLAIMED = "hasClaimed";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_RELEASE = "release";

    public static final String FUNC_SETALLOCATIONPERTEAM = "setAllocationPerTeam";

    public static final String FUNC_TOTALALLOCATED = "totalAllocated";

    public static final String FUNC_TOTALVAULTBALANCE = "totalVaultBalance";

    public static final String FUNC_UNALLOCATEDBALANCE = "unallocatedBalance";

    public static final String FUNC_WITHDRAW = "withdraw";

    public static final Event ALLOCATIONPERTEAMUPDATED_EVENT = new Event("AllocationPerTeamUpdated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event CAPITALALLOCATED_EVENT = new Event("CapitalAllocated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event CAPITALRELEASED_EVENT = new Event("CapitalReleased", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event DEPOSITED_EVENT = new Event("Deposited", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event WITHDRAWN_EVENT = new Event("Withdrawn", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected HackathonVault(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected HackathonVault(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected HackathonVault(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected HackathonVault(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<AllocationPerTeamUpdatedEventResponse> getAllocationPerTeamUpdatedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ALLOCATIONPERTEAMUPDATED_EVENT, transactionReceipt);
        ArrayList<AllocationPerTeamUpdatedEventResponse> responses = new ArrayList<AllocationPerTeamUpdatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AllocationPerTeamUpdatedEventResponse typedResponse = new AllocationPerTeamUpdatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.oldAmount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.newAmount = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static AllocationPerTeamUpdatedEventResponse getAllocationPerTeamUpdatedEventFromLog(
            Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ALLOCATIONPERTEAMUPDATED_EVENT, log);
        AllocationPerTeamUpdatedEventResponse typedResponse = new AllocationPerTeamUpdatedEventResponse();
        typedResponse.log = log;
        typedResponse.oldAmount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.newAmount = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<AllocationPerTeamUpdatedEventResponse> allocationPerTeamUpdatedEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getAllocationPerTeamUpdatedEventFromLog(log));
    }

    public Flowable<AllocationPerTeamUpdatedEventResponse> allocationPerTeamUpdatedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ALLOCATIONPERTEAMUPDATED_EVENT));
        return allocationPerTeamUpdatedEventFlowable(filter);
    }

    public static List<CapitalAllocatedEventResponse> getCapitalAllocatedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(CAPITALALLOCATED_EVENT, transactionReceipt);
        ArrayList<CapitalAllocatedEventResponse> responses = new ArrayList<CapitalAllocatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            CapitalAllocatedEventResponse typedResponse = new CapitalAllocatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static CapitalAllocatedEventResponse getCapitalAllocatedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(CAPITALALLOCATED_EVENT, log);
        CapitalAllocatedEventResponse typedResponse = new CapitalAllocatedEventResponse();
        typedResponse.log = log;
        typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<CapitalAllocatedEventResponse> capitalAllocatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getCapitalAllocatedEventFromLog(log));
    }

    public Flowable<CapitalAllocatedEventResponse> capitalAllocatedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(CAPITALALLOCATED_EVENT));
        return capitalAllocatedEventFlowable(filter);
    }

    public static List<CapitalReleasedEventResponse> getCapitalReleasedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(CAPITALRELEASED_EVENT, transactionReceipt);
        ArrayList<CapitalReleasedEventResponse> responses = new ArrayList<CapitalReleasedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            CapitalReleasedEventResponse typedResponse = new CapitalReleasedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static CapitalReleasedEventResponse getCapitalReleasedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(CAPITALRELEASED_EVENT, log);
        CapitalReleasedEventResponse typedResponse = new CapitalReleasedEventResponse();
        typedResponse.log = log;
        typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<CapitalReleasedEventResponse> capitalReleasedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getCapitalReleasedEventFromLog(log));
    }

    public Flowable<CapitalReleasedEventResponse> capitalReleasedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(CAPITALRELEASED_EVENT));
        return capitalReleasedEventFlowable(filter);
    }

    public static List<DepositedEventResponse> getDepositedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(DEPOSITED_EVENT, transactionReceipt);
        ArrayList<DepositedEventResponse> responses = new ArrayList<DepositedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            DepositedEventResponse typedResponse = new DepositedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.from = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static DepositedEventResponse getDepositedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(DEPOSITED_EVENT, log);
        DepositedEventResponse typedResponse = new DepositedEventResponse();
        typedResponse.log = log;
        typedResponse.from = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<DepositedEventResponse> depositedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getDepositedEventFromLog(log));
    }

    public Flowable<DepositedEventResponse> depositedEventFlowable(DefaultBlockParameter startBlock,
            DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(DEPOSITED_EVENT));
        return depositedEventFlowable(filter);
    }

    public static List<WithdrawnEventResponse> getWithdrawnEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(WITHDRAWN_EVENT, transactionReceipt);
        ArrayList<WithdrawnEventResponse> responses = new ArrayList<WithdrawnEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            WithdrawnEventResponse typedResponse = new WithdrawnEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.to = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static WithdrawnEventResponse getWithdrawnEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(WITHDRAWN_EVENT, log);
        WithdrawnEventResponse typedResponse = new WithdrawnEventResponse();
        typedResponse.log = log;
        typedResponse.to = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<WithdrawnEventResponse> withdrawnEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getWithdrawnEventFromLog(log));
    }

    public Flowable<WithdrawnEventResponse> withdrawnEventFlowable(DefaultBlockParameter startBlock,
            DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(WITHDRAWN_EVENT));
        return withdrawnEventFlowable(filter);
    }

    public RemoteFunctionCall<String> agentRegistry() {
        final Function function = new Function(FUNC_AGENTREGISTRY, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> allocate(BigInteger agentId, BigInteger amount) {
        final Function function = new Function(
                FUNC_ALLOCATE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(agentId), 
                new org.web3j.abi.datatypes.generated.Uint256(amount)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> allocatedCapital(BigInteger param0) {
        final Function function = new Function(FUNC_ALLOCATEDCAPITAL, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> allocationPerTeam() {
        final Function function = new Function(FUNC_ALLOCATIONPERTEAM, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> claimAllocation(BigInteger agentId) {
        final Function function = new Function(
                FUNC_CLAIMALLOCATION, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(agentId)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> deposit(BigInteger weiValue) {
        final Function function = new Function(
                FUNC_DEPOSIT, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function, weiValue);
    }

    public RemoteFunctionCall<BigInteger> getBalance(BigInteger agentId) {
        final Function function = new Function(FUNC_GETBALANCE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(agentId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<Boolean> hasClaimed(BigInteger param0) {
        final Function function = new Function(FUNC_HASCLAIMED, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<String> owner() {
        final Function function = new Function(FUNC_OWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> release(BigInteger agentId, BigInteger amount) {
        final Function function = new Function(
                FUNC_RELEASE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(agentId), 
                new org.web3j.abi.datatypes.generated.Uint256(amount)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setAllocationPerTeam(BigInteger newAmount) {
        final Function function = new Function(
                FUNC_SETALLOCATIONPERTEAM, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(newAmount)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> totalAllocated() {
        final Function function = new Function(FUNC_TOTALALLOCATED, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> totalVaultBalance() {
        final Function function = new Function(FUNC_TOTALVAULTBALANCE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> unallocatedBalance() {
        final Function function = new Function(FUNC_UNALLOCATEDBALANCE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> withdraw(BigInteger amount) {
        final Function function = new Function(
                FUNC_WITHDRAW, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(amount)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static HackathonVault load(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        return new HackathonVault(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static HackathonVault load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new HackathonVault(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static HackathonVault load(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        return new HackathonVault(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static HackathonVault load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new HackathonVault(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static class AllocationPerTeamUpdatedEventResponse extends BaseEventResponse {
        public BigInteger oldAmount;

        public BigInteger newAmount;
    }

    public static class CapitalAllocatedEventResponse extends BaseEventResponse {
        public BigInteger agentId;

        public BigInteger amount;
    }

    public static class CapitalReleasedEventResponse extends BaseEventResponse {
        public BigInteger agentId;

        public BigInteger amount;
    }

    public static class DepositedEventResponse extends BaseEventResponse {
        public String from;

        public BigInteger amount;
    }

    public static class WithdrawnEventResponse extends BaseEventResponse {
        public String to;

        public BigInteger amount;
    }
}
