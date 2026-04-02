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
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Int256;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple3;
import org.web3j.tuples.generated.Tuple4;
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
    public static final String BINARY = "608060405234801561001057600080fd5b50338061003757604051631e4fbdf760e01b81526000600482015260240160405180910390fd5b61004081610046565b50610096565b600080546001600160a01b038381166001600160a01b0319831681178455604051919092169283917f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e09190a35050565b6107d5806100a56000396000f3fe608060405234801561001057600080fd5b50600436106100935760003560e01c806389370d8b1161006657806389370d8b146101235780638da5cb5b1461017057806391b198741461018b578063944e8c39146101c9578063f2fde38b146101dc57600080fd5b80634b56175314610098578063715018a6146100ad57806372fa9b85146100b557806388cde1c814610110575b600080fd5b6100ab6100a63660046105a8565b6101ef565b005b6100ab610227565b6100eb6100c33660046105d4565b6001602081905260009182526040909120805491810154600282015460039092015490919084565b6040805194855260208501939093529183015260608201526080015b60405180910390f35b6100ab61011e3660046105ed565b61023b565b6101556101313660046105d4565b60008181526001602081905260409091208054918101546002909101549193909250565b60408051938452602084019290925290820152606001610107565b6000546040516001600160a01b039091168152602001610107565b6101b96101993660046105a8565b600260209081526000928352604080842090915290825290205460ff1681565b6040519015158152602001610107565b6100ab6101d736600461060f565b610287565b6100ab6101ea366004610645565b6103d8565b6101f7610416565b60009182526002602090815260408084206001600160a01b0390931684529190529020805460ff19166001179055565b61022f610416565b6102396000610443565b565b6000828152600160205260408120600281018054919261025a8361067d565b9190505550818160030160008282546102739190610696565b90915550610282905083610493565b505050565b600082815260026020908152604080832033845290915290205460ff166102f55760405162461bcd60e51b815260206004820152601e60248201527f4e6f7420612076616c696461746f7220666f722074686973206167656e74000060448201526064015b60405180910390fd5b60008281526001602052604081206003810154600282015491929161031a9190610507565b90506000606461032e60ff8616601e6106be565b6103398460466106be565b61034391906106d5565b61034d91906106fe565b808455426001850181905560408051838152602081019290925291925086917fd9f66501c000653403d74ddfa62a75c8dd44cc05d910ee81bf1d852262dd5fa1910160405180910390a260405160ff85168152339086907fe55f0d96c4bb7228e9eed1db8ccc3fff202c8e39157e8c6cda8ffa697df1b9589060200160405180910390a35050505050565b6103e0610416565b6001600160a01b03811661040a57604051631e4fbdf760e01b8152600060048201526024016102ec565b61041381610443565b50565b6000546001600160a01b031633146102395760405163118cdaa760e01b81523360048201526024016102ec565b600080546001600160a01b038381166001600160a01b0319831681178455604051919092169283917f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e09190a35050565b6000818152600160205260408120600381015460028201549192916104b89190610507565b808355426001840181905560408051838152602081019290925291925084917fd9f66501c000653403d74ddfa62a75c8dd44cc05d910ee81bf1d852262dd5fa1910160405180910390a2505050565b60008160000361051957506032610586565b60006105258385610712565b90506000811261055a576103e861053d826032610740565b6105479190610712565b610552906032610696565b915050610586565b6103e861056682610770565b6105719060326106be565b61057b91906106fe565b61055290603261078c565b92915050565b80356001600160a01b03811681146105a357600080fd5b919050565b600080604083850312156105bb57600080fd5b823591506105cb6020840161058c565b90509250929050565b6000602082840312156105e657600080fd5b5035919050565b6000806040838503121561060057600080fd5b50508035926020909101359150565b6000806040838503121561062257600080fd5b82359150602083013560ff8116811461063a57600080fd5b809150509250929050565b60006020828403121561065757600080fd5b6106608261058c565b9392505050565b634e487b7160e01b600052601160045260246000fd5b60006001820161068f5761068f610667565b5060010190565b80820182811260008312801582168215821617156106b6576106b6610667565b505092915050565b808202811582820484141761058657610586610667565b8082018082111561058657610586610667565b634e487b7160e01b600052601260045260246000fd5b60008261070d5761070d6106e8565b500490565b600082610721576107216106e8565b600160ff1b82146000198414161561073b5761073b610667565b500590565b80820260008212600160ff1b8414161561075c5761075c610667565b818105831482151761058657610586610667565b6000600160ff1b820161078557610785610667565b5060000390565b818103818111156105865761058661066756fea2646970667358221220cbb322da05c6fb9cbb62388c872727e832378a41275ec4c5bc41340ff12cfbde64736f6c63430008180033\n";

    private static String librariesLinkedBinary;

    public static final String FUNC_ADDVALIDATOR = "addValidator";

    public static final String FUNC_GETREPUTATION = "getReputation";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_RECORDPNL = "recordPnL";

    public static final String FUNC_RECORDVALIDATORSCORE = "recordValidatorScore";

    public static final String FUNC_RENOUNCEOWNERSHIP = "renounceOwnership";

    public static final String FUNC_REPUTATIONS = "reputations";

    public static final String FUNC_TRANSFEROWNERSHIP = "transferOwnership";

    public static final String FUNC_VALIDATORS = "validators";

    public static final Event OWNERSHIPTRANSFERRED_EVENT = new Event("OwnershipTransferred", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event PNLRECORDED_EVENT = new Event("PnLRecorded", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Int256>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event REPUTATIONUPDATED_EVENT = new Event("ReputationUpdated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event VALIDATORSCOREPOSTED_EVENT = new Event("ValidatorScorePosted", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint8>() {}));
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

    public static List<OwnershipTransferredEventResponse> getOwnershipTransferredEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(OWNERSHIPTRANSFERRED_EVENT, transactionReceipt);
        ArrayList<OwnershipTransferredEventResponse> responses = new ArrayList<OwnershipTransferredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OwnershipTransferredEventResponse typedResponse = new OwnershipTransferredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.previousOwner = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.newOwner = (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static OwnershipTransferredEventResponse getOwnershipTransferredEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(OWNERSHIPTRANSFERRED_EVENT, log);
        OwnershipTransferredEventResponse typedResponse = new OwnershipTransferredEventResponse();
        typedResponse.log = log;
        typedResponse.previousOwner = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.newOwner = (String) eventValues.getIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<OwnershipTransferredEventResponse> ownershipTransferredEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getOwnershipTransferredEventFromLog(log));
    }

    public Flowable<OwnershipTransferredEventResponse> ownershipTransferredEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(OWNERSHIPTRANSFERRED_EVENT));
        return ownershipTransferredEventFlowable(filter);
    }

    public static List<PnLRecordedEventResponse> getPnLRecordedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(PNLRECORDED_EVENT, transactionReceipt);
        ArrayList<PnLRecordedEventResponse> responses = new ArrayList<PnLRecordedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            PnLRecordedEventResponse typedResponse = new PnLRecordedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.pnl = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static PnLRecordedEventResponse getPnLRecordedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(PNLRECORDED_EVENT, log);
        PnLRecordedEventResponse typedResponse = new PnLRecordedEventResponse();
        typedResponse.log = log;
        typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.pnl = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<PnLRecordedEventResponse> pnLRecordedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getPnLRecordedEventFromLog(log));
    }

    public Flowable<PnLRecordedEventResponse> pnLRecordedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PNLRECORDED_EVENT));
        return pnLRecordedEventFlowable(filter);
    }

    public static List<ReputationUpdatedEventResponse> getReputationUpdatedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(REPUTATIONUPDATED_EVENT, transactionReceipt);
        ArrayList<ReputationUpdatedEventResponse> responses = new ArrayList<ReputationUpdatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ReputationUpdatedEventResponse typedResponse = new ReputationUpdatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.newScore = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ReputationUpdatedEventResponse getReputationUpdatedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(REPUTATIONUPDATED_EVENT, log);
        ReputationUpdatedEventResponse typedResponse = new ReputationUpdatedEventResponse();
        typedResponse.log = log;
        typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.newScore = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<ReputationUpdatedEventResponse> reputationUpdatedEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getReputationUpdatedEventFromLog(log));
    }

    public Flowable<ReputationUpdatedEventResponse> reputationUpdatedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(REPUTATIONUPDATED_EVENT));
        return reputationUpdatedEventFlowable(filter);
    }

    public static List<ValidatorScorePostedEventResponse> getValidatorScorePostedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(VALIDATORSCOREPOSTED_EVENT, transactionReceipt);
        ArrayList<ValidatorScorePostedEventResponse> responses = new ArrayList<ValidatorScorePostedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ValidatorScorePostedEventResponse typedResponse = new ValidatorScorePostedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.validator = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.score = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ValidatorScorePostedEventResponse getValidatorScorePostedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(VALIDATORSCOREPOSTED_EVENT, log);
        ValidatorScorePostedEventResponse typedResponse = new ValidatorScorePostedEventResponse();
        typedResponse.log = log;
        typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.validator = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.score = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<ValidatorScorePostedEventResponse> validatorScorePostedEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getValidatorScorePostedEventFromLog(log));
    }

    public Flowable<ValidatorScorePostedEventResponse> validatorScorePostedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(VALIDATORSCOREPOSTED_EVENT));
        return validatorScorePostedEventFlowable(filter);
    }

    public RemoteFunctionCall<TransactionReceipt> addValidator(BigInteger agentId,
            String validator) {
        final Function function = new Function(
                FUNC_ADDVALIDATOR, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(agentId), 
                new org.web3j.abi.datatypes.Address(160, validator)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Tuple3<BigInteger, BigInteger, BigInteger>> getReputation(
            BigInteger agentId) {
        final Function function = new Function(FUNC_GETREPUTATION, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(agentId)), 
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

    public RemoteFunctionCall<String> owner() {
        final Function function = new Function(FUNC_OWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> recordPnL(BigInteger agentId, BigInteger pnl) {
        final Function function = new Function(
                FUNC_RECORDPNL, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(agentId), 
                new org.web3j.abi.datatypes.generated.Int256(pnl)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> recordValidatorScore(BigInteger agentId,
            BigInteger score) {
        final Function function = new Function(
                FUNC_RECORDVALIDATORSCORE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(agentId), 
                new org.web3j.abi.datatypes.generated.Uint8(score)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> renounceOwnership() {
        final Function function = new Function(
                FUNC_RENOUNCEOWNERSHIP, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>> reputations(
            BigInteger param0) {
        final Function function = new Function(FUNC_REPUTATIONS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Int256>() {}));
        return new RemoteFunctionCall<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>>(function,
                new Callable<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>>() {
                    @Override
                    public Tuple4<BigInteger, BigInteger, BigInteger, BigInteger> call() throws
                            Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>(
                                (BigInteger) results.get(0).getValue(), 
                                (BigInteger) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue(), 
                                (BigInteger) results.get(3).getValue());
                    }
                });
    }

    public RemoteFunctionCall<TransactionReceipt> transferOwnership(String newOwner) {
        final Function function = new Function(
                FUNC_TRANSFEROWNERSHIP, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, newOwner)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> validators(BigInteger param0, String param1) {
        final Function function = new Function(FUNC_VALIDATORS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0), 
                new org.web3j.abi.datatypes.Address(160, param1)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
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

    public static RemoteCall<ReputationRegistry> deploy(Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        return deployRemoteCall(ReputationRegistry.class, web3j, credentials, contractGasProvider, getDeploymentBinary(), "");
    }

    public static RemoteCall<ReputationRegistry> deploy(Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(ReputationRegistry.class, web3j, transactionManager, contractGasProvider, getDeploymentBinary(), "");
    }

    @Deprecated
    public static RemoteCall<ReputationRegistry> deploy(Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(ReputationRegistry.class, web3j, credentials, gasPrice, gasLimit, getDeploymentBinary(), "");
    }

    @Deprecated
    public static RemoteCall<ReputationRegistry> deploy(Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(ReputationRegistry.class, web3j, transactionManager, gasPrice, gasLimit, getDeploymentBinary(), "");
    }

    public static void linkLibraries(List<Contract.LinkReference> references) {
        librariesLinkedBinary = linkBinaryWithReferences(BINARY, references);
    }

    private static String getDeploymentBinary() {
        if (librariesLinkedBinary != null) {
            return librariesLinkedBinary;
        } else {
            return BINARY;
        }
    }

    public static class OwnershipTransferredEventResponse extends BaseEventResponse {
        public String previousOwner;

        public String newOwner;
    }

    public static class PnLRecordedEventResponse extends BaseEventResponse {
        public BigInteger agentId;

        public BigInteger pnl;

        public BigInteger timestamp;
    }

    public static class ReputationUpdatedEventResponse extends BaseEventResponse {
        public BigInteger agentId;

        public BigInteger newScore;

        public BigInteger timestamp;
    }

    public static class ValidatorScorePostedEventResponse extends BaseEventResponse {
        public BigInteger agentId;

        public String validator;

        public BigInteger score;
    }
}
