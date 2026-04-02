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
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.StaticStruct;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
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
public class ValidationRegistry extends Contract {
    public static final String BINARY = "608060405234801561001057600080fd5b50338061003757604051631e4fbdf760e01b81526000600482015260240160405180910390fd5b61004081610046565b50610096565b600080546001600160a01b038381166001600160a01b0319831681178455604051919092169283917f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e09190a35050565b61069a806100a56000396000f3fe608060405234801561001057600080fd5b50600436106100885760003560e01c8063715018a61161005b578063715018a61461011c5780638da5cb5b1461012457806391b198741461013f578063f2fde38b1461017d57600080fd5b8063434dbaee1461008d5780634b561753146100b65780634b5a82d1146100cb5780635c5e6fcd14610109575b600080fd5b6100a061009b366004610510565b610190565b6040516100ad9190610529565b60405180910390f35b6100c96100c43660046105b5565b610230565b005b6100de6100d93660046105e1565b610268565b6040805194855260ff9093166020850152918301526001600160a01b031660608201526080016100ad565b6100c9610117366004610603565b6102ba565b6100c9610441565b6000546040516001600160a01b0390911681526020016100ad565b61016d61014d3660046105b5565b600260209081526000928352604080842090915290825290205460ff1681565b60405190151581526020016100ad565b6100c961018b366004610642565b610455565b606060016000838152602001908152602001600020805480602002602001604051908101604052809291908181526020016000905b8282101561022557600084815260209081902060408051608081018252600486029092018054835260018082015460ff1684860152600282015492840192909252600301546001600160a01b0316606083015290835290920191016101c5565b505050509050919050565b610238610493565b60009182526002602090815260408084206001600160a01b0390931684529190529020805460ff19166001179055565b6001602052816000526040600020818154811061028457600080fd5b6000918252602090912060049091020180546001820154600283015460039093015491945060ff1692506001600160a01b031684565b600083815260026020908152604080832033845290915290205460ff166103285760405162461bcd60e51b815260206004820152601e60248201527f4e6f7420612076616c696461746f7220666f722074686973206167656e74000060448201526064015b60405180910390fd5b60648160ff1611156103715760405162461bcd60e51b815260206004820152601260248201527153636f7265206f7574206f662072616e676560701b604482015260640161031f565b6000838152600160208181526040808420815160808101835287815260ff8781168286018181524284870190815233606086018181528754808c018955978c529a899020955160049097029095019586559051978501805460ff19169890931697909717909155945160028301559451600390910180546001600160a01b0319166001600160a01b0390921691909117905580518681529182019290925285917f1a6078c320bd4b9eb3bf5436c2a989b079716aed318bcef40ce6c8b41279d0e0910160405180910390a3505050565b610449610493565b61045360006104c0565b565b61045d610493565b6001600160a01b03811661048757604051631e4fbdf760e01b81526000600482015260240161031f565b610490816104c0565b50565b6000546001600160a01b031633146104535760405163118cdaa760e01b815233600482015260240161031f565b600080546001600160a01b038381166001600160a01b0319831681178455604051919092169283917f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e09190a35050565b60006020828403121561052257600080fd5b5035919050565b602080825282518282018190526000919060409081850190868401855b8281101561058c578151805185528681015160ff168786015285810151868601526060908101516001600160a01b03169085015260809093019290850190600101610546565b5091979650505050505050565b80356001600160a01b03811681146105b057600080fd5b919050565b600080604083850312156105c857600080fd5b823591506105d860208401610599565b90509250929050565b600080604083850312156105f457600080fd5b50508035926020909101359150565b60008060006060848603121561061857600080fd5b8335925060208401359150604084013560ff8116811461063757600080fd5b809150509250925092565b60006020828403121561065457600080fd5b61065d82610599565b939250505056fea2646970667358221220b9f5d85dc5bca7723076bbd2bbbd271c7c3271c5887b66924b9137dded1f8e9364736f6c63430008180033\n";

    private static String librariesLinkedBinary;

    public static final String FUNC_ADDVALIDATOR = "addValidator";

    public static final String FUNC_GETVALIDATIONS = "getValidations";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_POSTVALIDATION = "postValidation";

    public static final String FUNC_RENOUNCEOWNERSHIP = "renounceOwnership";

    public static final String FUNC_TRANSFEROWNERSHIP = "transferOwnership";

    public static final String FUNC_VALIDATIONS = "validations";

    public static final String FUNC_VALIDATORS = "validators";

    public static final Event OWNERSHIPTRANSFERRED_EVENT = new Event("OwnershipTransferred", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event VALIDATIONPOSTED_EVENT = new Event("ValidationPosted", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Bytes32>() {}, new TypeReference<Uint8>() {}, new TypeReference<Address>(true) {}));
    ;

    @Deprecated
    protected ValidationRegistry(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected ValidationRegistry(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected ValidationRegistry(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected ValidationRegistry(String contractAddress, Web3j web3j,
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

    public static List<ValidationPostedEventResponse> getValidationPostedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(VALIDATIONPOSTED_EVENT, transactionReceipt);
        ArrayList<ValidationPostedEventResponse> responses = new ArrayList<ValidationPostedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ValidationPostedEventResponse typedResponse = new ValidationPostedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.validator = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.artifactHash = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.score = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ValidationPostedEventResponse getValidationPostedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(VALIDATIONPOSTED_EVENT, log);
        ValidationPostedEventResponse typedResponse = new ValidationPostedEventResponse();
        typedResponse.log = log;
        typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.validator = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.artifactHash = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.score = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<ValidationPostedEventResponse> validationPostedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getValidationPostedEventFromLog(log));
    }

    public Flowable<ValidationPostedEventResponse> validationPostedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(VALIDATIONPOSTED_EVENT));
        return validationPostedEventFlowable(filter);
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

    public RemoteFunctionCall<List> getValidations(BigInteger agentId) {
        final Function function = new Function(FUNC_GETVALIDATIONS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(agentId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Validation>>() {}));
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

    public RemoteFunctionCall<String> owner() {
        final Function function = new Function(FUNC_OWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> postValidation(BigInteger agentId,
            byte[] artifactHash, BigInteger score) {
        final Function function = new Function(
                FUNC_POSTVALIDATION, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(agentId), 
                new org.web3j.abi.datatypes.generated.Bytes32(artifactHash), 
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

    public RemoteFunctionCall<TransactionReceipt> transferOwnership(String newOwner) {
        final Function function = new Function(
                FUNC_TRANSFEROWNERSHIP, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, newOwner)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Tuple4<byte[], BigInteger, BigInteger, String>> validations(
            BigInteger param0, BigInteger param1) {
        final Function function = new Function(FUNC_VALIDATIONS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0), 
                new org.web3j.abi.datatypes.generated.Uint256(param1)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}, new TypeReference<Uint8>() {}, new TypeReference<Uint256>() {}, new TypeReference<Address>() {}));
        return new RemoteFunctionCall<Tuple4<byte[], BigInteger, BigInteger, String>>(function,
                new Callable<Tuple4<byte[], BigInteger, BigInteger, String>>() {
                    @Override
                    public Tuple4<byte[], BigInteger, BigInteger, String> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple4<byte[], BigInteger, BigInteger, String>(
                                (byte[]) results.get(0).getValue(), 
                                (BigInteger) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue(), 
                                (String) results.get(3).getValue());
                    }
                });
    }

    public RemoteFunctionCall<Boolean> validators(BigInteger param0, String param1) {
        final Function function = new Function(FUNC_VALIDATORS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0), 
                new org.web3j.abi.datatypes.Address(160, param1)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    @Deprecated
    public static ValidationRegistry load(String contractAddress, Web3j web3j,
            Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new ValidationRegistry(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static ValidationRegistry load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new ValidationRegistry(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static ValidationRegistry load(String contractAddress, Web3j web3j,
            Credentials credentials, ContractGasProvider contractGasProvider) {
        return new ValidationRegistry(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static ValidationRegistry load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new ValidationRegistry(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<ValidationRegistry> deploy(Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        return deployRemoteCall(ValidationRegistry.class, web3j, credentials, contractGasProvider, getDeploymentBinary(), "");
    }

    public static RemoteCall<ValidationRegistry> deploy(Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(ValidationRegistry.class, web3j, transactionManager, contractGasProvider, getDeploymentBinary(), "");
    }

    @Deprecated
    public static RemoteCall<ValidationRegistry> deploy(Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(ValidationRegistry.class, web3j, credentials, gasPrice, gasLimit, getDeploymentBinary(), "");
    }

    @Deprecated
    public static RemoteCall<ValidationRegistry> deploy(Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(ValidationRegistry.class, web3j, transactionManager, gasPrice, gasLimit, getDeploymentBinary(), "");
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

    public static class Validation extends StaticStruct {
        public byte[] artifactHash;

        public BigInteger score;

        public BigInteger timestamp;

        public String validator;

        public Validation(byte[] artifactHash, BigInteger score, BigInteger timestamp,
                String validator) {
            super(new org.web3j.abi.datatypes.generated.Bytes32(artifactHash), 
                    new org.web3j.abi.datatypes.generated.Uint8(score), 
                    new org.web3j.abi.datatypes.generated.Uint256(timestamp), 
                    new org.web3j.abi.datatypes.Address(160, validator));
            this.artifactHash = artifactHash;
            this.score = score;
            this.timestamp = timestamp;
            this.validator = validator;
        }

        public Validation(Bytes32 artifactHash, Uint8 score, Uint256 timestamp, Address validator) {
            super(artifactHash, score, timestamp, validator);
            this.artifactHash = artifactHash.getValue();
            this.score = score.getValue();
            this.timestamp = timestamp.getValue();
            this.validator = validator.getValue();
        }
    }

    public static class OwnershipTransferredEventResponse extends BaseEventResponse {
        public String previousOwner;

        public String newOwner;
    }

    public static class ValidationPostedEventResponse extends BaseEventResponse {
        public BigInteger agentId;

        public String validator;

        public byte[] artifactHash;

        public BigInteger score;
    }
}
