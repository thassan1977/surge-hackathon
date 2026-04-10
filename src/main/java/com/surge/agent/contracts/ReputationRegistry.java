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
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple3;
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
public class ReputationRegistry extends Contract {
    public static final String BINARY = "Bin file was not provided";

    public static final String FUNC_AGENTREGISTRY = "agentRegistry";

    public static final String FUNC_GETAVERAGESCORE = "getAverageScore";

    public static final String FUNC_GETFEEDBACKHISTORY = "getFeedbackHistory";

    public static final String FUNC_GETFEEDBACKPAGE = "getFeedbackPage";

    public static final String FUNC_HASRATED = "hasRated";

    public static final String FUNC_REPUTATION = "reputation";

    public static final String FUNC_SUBMITFEEDBACK = "submitFeedback";

    public static final Event FEEDBACKSUBMITTED_EVENT = new Event("FeedbackSubmitted", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint8>() {}, new TypeReference<Bytes32>() {}, new TypeReference<Uint8>() {}));
    ;

    @Deprecated
    protected ReputationRegistry(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected ReputationRegistry(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected ReputationRegistry(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected ReputationRegistry(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<FeedbackSubmittedEventResponse> getFeedbackSubmittedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(FEEDBACKSUBMITTED_EVENT, transactionReceipt);
        ArrayList<FeedbackSubmittedEventResponse> responses = new ArrayList<FeedbackSubmittedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            FeedbackSubmittedEventResponse typedResponse = new FeedbackSubmittedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.rater = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.score = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.outcomeRef = (byte[]) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.feedbackType = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static FeedbackSubmittedEventResponse getFeedbackSubmittedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(FEEDBACKSUBMITTED_EVENT, log);
        FeedbackSubmittedEventResponse typedResponse = new FeedbackSubmittedEventResponse();
        typedResponse.log = log;
        typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.rater = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.score = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.outcomeRef = (byte[]) eventValues.getNonIndexedValues().get(1).getValue();
        typedResponse.feedbackType = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<FeedbackSubmittedEventResponse> feedbackSubmittedEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getFeedbackSubmittedEventFromLog(log));
    }

    public Flowable<FeedbackSubmittedEventResponse> feedbackSubmittedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(FEEDBACKSUBMITTED_EVENT));
        return feedbackSubmittedEventFlowable(filter);
    }

    public RemoteFunctionCall<String> agentRegistry() {
        final Function function = new Function(FUNC_AGENTREGISTRY, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<BigInteger> getAverageScore(BigInteger agentId) {
        final Function function = new Function(FUNC_GETAVERAGESCORE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(agentId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<List> getFeedbackHistory(BigInteger agentId) {
        final Function function = new Function(FUNC_GETFEEDBACKHISTORY, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(agentId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<FeedbackEntry>>() {}));
        return new RemoteFunctionCall<List>(function,
                new Callable<List>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public List call() throws Exception {
                        List<Type> result = (List<Type>) executeCallSingleValueReturn(function, List.class);
                        return convertToNative(result);
                    }
                });
    }

    public RemoteFunctionCall<List> getFeedbackPage(BigInteger agentId, BigInteger offset,
            BigInteger limit) {
        final Function function = new Function(FUNC_GETFEEDBACKPAGE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(agentId), 
                new org.web3j.abi.datatypes.generated.Uint256(offset), 
                new org.web3j.abi.datatypes.generated.Uint256(limit)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<FeedbackEntry>>() {}));
        return new RemoteFunctionCall<List>(function,
                new Callable<List>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public List call() throws Exception {
                        List<Type> result = (List<Type>) executeCallSingleValueReturn(function, List.class);
                        return convertToNative(result);
                    }
                });
    }

    public RemoteFunctionCall<Boolean> hasRated(BigInteger agentId, String rater) {
        final Function function = new Function(FUNC_HASRATED, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(agentId), 
                new org.web3j.abi.datatypes.Address(160, rater)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<Tuple3<BigInteger, BigInteger, BigInteger>> reputation(
            BigInteger param0) {
        final Function function = new Function(FUNC_REPUTATION, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
        return new RemoteFunctionCall<Tuple3<BigInteger, BigInteger, BigInteger>>(function,
                new Callable<Tuple3<BigInteger, BigInteger, BigInteger>>() {
                    @Override
                    public Tuple3<BigInteger, BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple3<BigInteger, BigInteger, BigInteger>(
                                (BigInteger) results.get(0).getValue(), 
                                (BigInteger) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue());
                    }
                });
    }

    public RemoteFunctionCall<TransactionReceipt> submitFeedback(BigInteger agentId,
            BigInteger score, byte[] outcomeRef, String comment, BigInteger feedbackType) {
        final Function function = new Function(
                FUNC_SUBMITFEEDBACK, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(agentId), 
                new org.web3j.abi.datatypes.generated.Uint8(score), 
                new org.web3j.abi.datatypes.generated.Bytes32(outcomeRef), 
                new org.web3j.abi.datatypes.Utf8String(comment), 
                new org.web3j.abi.datatypes.generated.Uint8(feedbackType)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static ReputationRegistry load(String contractAddress, Web3j web3j,
            Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new ReputationRegistry(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static ReputationRegistry load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new ReputationRegistry(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static ReputationRegistry load(String contractAddress, Web3j web3j,
            Credentials credentials, ContractGasProvider contractGasProvider) {
        return new ReputationRegistry(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static ReputationRegistry load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new ReputationRegistry(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static class FeedbackEntry extends DynamicStruct {
        public String rater;

        public BigInteger score;

        public byte[] outcomeRef;

        public String comment;

        public BigInteger timestamp;

        public BigInteger feedbackType;

        public FeedbackEntry(String rater, BigInteger score, byte[] outcomeRef, String comment,
                BigInteger timestamp, BigInteger feedbackType) {
            super(new org.web3j.abi.datatypes.Address(160, rater), 
                    new org.web3j.abi.datatypes.generated.Uint8(score), 
                    new org.web3j.abi.datatypes.generated.Bytes32(outcomeRef), 
                    new org.web3j.abi.datatypes.Utf8String(comment), 
                    new org.web3j.abi.datatypes.generated.Uint256(timestamp), 
                    new org.web3j.abi.datatypes.generated.Uint8(feedbackType));
            this.rater = rater;
            this.score = score;
            this.outcomeRef = outcomeRef;
            this.comment = comment;
            this.timestamp = timestamp;
            this.feedbackType = feedbackType;
        }

        public FeedbackEntry(Address rater, Uint8 score, Bytes32 outcomeRef, Utf8String comment,
                Uint256 timestamp, Uint8 feedbackType) {
            super(rater, score, outcomeRef, comment, timestamp, feedbackType);
            this.rater = rater.getValue();
            this.score = score.getValue();
            this.outcomeRef = outcomeRef.getValue();
            this.comment = comment.getValue();
            this.timestamp = timestamp.getValue();
            this.feedbackType = feedbackType.getValue();
        }
    }

    public static class FeedbackSubmittedEventResponse extends BaseEventResponse {
        public BigInteger agentId;

        public String rater;

        public BigInteger score;

        public byte[] outcomeRef;

        public BigInteger feedbackType;
    }
}
