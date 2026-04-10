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
import org.web3j.abi.datatypes.DynamicBytes;
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
import org.web3j.tuples.generated.Tuple8;
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
    public static final String BINARY = "Bin file was not provided";

    public static final String FUNC_ADDVALIDATOR = "addValidator";

    public static final String FUNC_AGENTREGISTRY = "agentRegistry";

    public static final String FUNC_ATTESTATIONCOUNT = "attestationCount";

    public static final String FUNC_CHECKPOINTATTESTATIONS = "checkpointAttestations";

    public static final String FUNC_GETATTESTATION = "getAttestation";

    public static final String FUNC_GETATTESTATIONS = "getAttestations";

    public static final String FUNC_GETAVERAGEVALIDATIONSCORE = "getAverageValidationScore";

    public static final String FUNC_OPENVALIDATION = "openValidation";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_POSTATTESTATION = "postAttestation";

    public static final String FUNC_POSTEIP712ATTESTATION = "postEIP712Attestation";

    public static final String FUNC_REMOVEVALIDATOR = "removeValidator";

    public static final String FUNC_SETOPENVALIDATION = "setOpenValidation";

    public static final String FUNC_VALIDATORS = "validators";

    public static final Event ATTESTATIONPOSTED_EVENT = new Event("AttestationPosted", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Bytes32>(true) {}, new TypeReference<Uint8>() {}, new TypeReference<Uint8>() {}));
    ;

    public static final Event VALIDATORADDED_EVENT = new Event("ValidatorAdded", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}));
    ;

    public static final Event VALIDATORREMOVED_EVENT = new Event("ValidatorRemoved", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}));
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

    public static List<AttestationPostedEventResponse> getAttestationPostedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ATTESTATIONPOSTED_EVENT, transactionReceipt);
        ArrayList<AttestationPostedEventResponse> responses = new ArrayList<AttestationPostedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AttestationPostedEventResponse typedResponse = new AttestationPostedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.validator = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.checkpointHash = (byte[]) eventValues.getIndexedValues().get(2).getValue();
            typedResponse.score = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.proofType = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static AttestationPostedEventResponse getAttestationPostedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ATTESTATIONPOSTED_EVENT, log);
        AttestationPostedEventResponse typedResponse = new AttestationPostedEventResponse();
        typedResponse.log = log;
        typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.validator = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.checkpointHash = (byte[]) eventValues.getIndexedValues().get(2).getValue();
        typedResponse.score = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.proofType = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<AttestationPostedEventResponse> attestationPostedEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getAttestationPostedEventFromLog(log));
    }

    public Flowable<AttestationPostedEventResponse> attestationPostedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ATTESTATIONPOSTED_EVENT));
        return attestationPostedEventFlowable(filter);
    }

    public static List<ValidatorAddedEventResponse> getValidatorAddedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(VALIDATORADDED_EVENT, transactionReceipt);
        ArrayList<ValidatorAddedEventResponse> responses = new ArrayList<ValidatorAddedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ValidatorAddedEventResponse typedResponse = new ValidatorAddedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.validator = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ValidatorAddedEventResponse getValidatorAddedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(VALIDATORADDED_EVENT, log);
        ValidatorAddedEventResponse typedResponse = new ValidatorAddedEventResponse();
        typedResponse.log = log;
        typedResponse.validator = (String) eventValues.getIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<ValidatorAddedEventResponse> validatorAddedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getValidatorAddedEventFromLog(log));
    }

    public Flowable<ValidatorAddedEventResponse> validatorAddedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(VALIDATORADDED_EVENT));
        return validatorAddedEventFlowable(filter);
    }

    public static List<ValidatorRemovedEventResponse> getValidatorRemovedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(VALIDATORREMOVED_EVENT, transactionReceipt);
        ArrayList<ValidatorRemovedEventResponse> responses = new ArrayList<ValidatorRemovedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ValidatorRemovedEventResponse typedResponse = new ValidatorRemovedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.validator = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ValidatorRemovedEventResponse getValidatorRemovedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(VALIDATORREMOVED_EVENT, log);
        ValidatorRemovedEventResponse typedResponse = new ValidatorRemovedEventResponse();
        typedResponse.log = log;
        typedResponse.validator = (String) eventValues.getIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<ValidatorRemovedEventResponse> validatorRemovedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getValidatorRemovedEventFromLog(log));
    }

    public Flowable<ValidatorRemovedEventResponse> validatorRemovedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(VALIDATORREMOVED_EVENT));
        return validatorRemovedEventFlowable(filter);
    }

    public RemoteFunctionCall<TransactionReceipt> addValidator(String validator) {
        final Function function = new Function(
                FUNC_ADDVALIDATOR, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, validator)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> agentRegistry() {
        final Function function = new Function(FUNC_AGENTREGISTRY, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<BigInteger> attestationCount(BigInteger param0) {
        final Function function = new Function(FUNC_ATTESTATIONCOUNT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<Tuple8<BigInteger, String, byte[], BigInteger, BigInteger, byte[], String, BigInteger>> checkpointAttestations(
            byte[] param0) {
        final Function function = new Function(FUNC_CHECKPOINTATTESTATIONS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Address>() {}, new TypeReference<Bytes32>() {}, new TypeReference<Uint8>() {}, new TypeReference<Uint8>() {}, new TypeReference<DynamicBytes>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}));
        return new RemoteFunctionCall<Tuple8<BigInteger, String, byte[], BigInteger, BigInteger, byte[], String, BigInteger>>(function,
                new Callable<Tuple8<BigInteger, String, byte[], BigInteger, BigInteger, byte[], String, BigInteger>>() {
                    @Override
                    public Tuple8<BigInteger, String, byte[], BigInteger, BigInteger, byte[], String, BigInteger> call(
                            ) throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple8<BigInteger, String, byte[], BigInteger, BigInteger, byte[], String, BigInteger>(
                                (BigInteger) results.get(0).getValue(), 
                                (String) results.get(1).getValue(), 
                                (byte[]) results.get(2).getValue(), 
                                (BigInteger) results.get(3).getValue(), 
                                (BigInteger) results.get(4).getValue(), 
                                (byte[]) results.get(5).getValue(), 
                                (String) results.get(6).getValue(), 
                                (BigInteger) results.get(7).getValue());
                    }
                });
    }

    public RemoteFunctionCall<Attestation> getAttestation(byte[] checkpointHash) {
        final Function function = new Function(FUNC_GETATTESTATION, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(checkpointHash)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Attestation>() {}));
        return executeRemoteCallSingleValueReturn(function, Attestation.class);
    }

    public RemoteFunctionCall<List> getAttestations(BigInteger agentId) {
        final Function function = new Function(FUNC_GETATTESTATIONS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(agentId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Attestation>>() {}));
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

    public RemoteFunctionCall<BigInteger> getAverageValidationScore(BigInteger agentId) {
        final Function function = new Function(FUNC_GETAVERAGEVALIDATIONSCORE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(agentId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<Boolean> openValidation() {
        final Function function = new Function(FUNC_OPENVALIDATION, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<String> owner() {
        final Function function = new Function(FUNC_OWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> postAttestation(BigInteger agentId,
            byte[] checkpointHash, BigInteger score, BigInteger proofType, byte[] proof,
            String notes) {
        final Function function = new Function(
                FUNC_POSTATTESTATION, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(agentId), 
                new org.web3j.abi.datatypes.generated.Bytes32(checkpointHash), 
                new org.web3j.abi.datatypes.generated.Uint8(score), 
                new org.web3j.abi.datatypes.generated.Uint8(proofType), 
                new org.web3j.abi.datatypes.DynamicBytes(proof), 
                new org.web3j.abi.datatypes.Utf8String(notes)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> postEIP712Attestation(BigInteger agentId,
            byte[] checkpointHash, BigInteger score, String notes) {
        final Function function = new Function(
                FUNC_POSTEIP712ATTESTATION, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(agentId), 
                new org.web3j.abi.datatypes.generated.Bytes32(checkpointHash), 
                new org.web3j.abi.datatypes.generated.Uint8(score), 
                new org.web3j.abi.datatypes.Utf8String(notes)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> removeValidator(String validator) {
        final Function function = new Function(
                FUNC_REMOVEVALIDATOR, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, validator)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setOpenValidation(Boolean open) {
        final Function function = new Function(
                FUNC_SETOPENVALIDATION, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Bool(open)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> validators(String param0) {
        final Function function = new Function(FUNC_VALIDATORS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, param0)), 
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

    public static class Attestation extends DynamicStruct {
        public BigInteger agentId;

        public String validator;

        public byte[] checkpointHash;

        public BigInteger score;

        public BigInteger proofType;

        public byte[] proof;

        public String notes;

        public BigInteger timestamp;

        public Attestation(BigInteger agentId, String validator, byte[] checkpointHash,
                BigInteger score, BigInteger proofType, byte[] proof, String notes,
                BigInteger timestamp) {
            super(new org.web3j.abi.datatypes.generated.Uint256(agentId), 
                    new org.web3j.abi.datatypes.Address(160, validator), 
                    new org.web3j.abi.datatypes.generated.Bytes32(checkpointHash), 
                    new org.web3j.abi.datatypes.generated.Uint8(score), 
                    new org.web3j.abi.datatypes.generated.Uint8(proofType), 
                    new org.web3j.abi.datatypes.DynamicBytes(proof), 
                    new org.web3j.abi.datatypes.Utf8String(notes), 
                    new org.web3j.abi.datatypes.generated.Uint256(timestamp));
            this.agentId = agentId;
            this.validator = validator;
            this.checkpointHash = checkpointHash;
            this.score = score;
            this.proofType = proofType;
            this.proof = proof;
            this.notes = notes;
            this.timestamp = timestamp;
        }

        public Attestation(Uint256 agentId, Address validator, Bytes32 checkpointHash, Uint8 score,
                Uint8 proofType, DynamicBytes proof, Utf8String notes, Uint256 timestamp) {
            super(agentId, validator, checkpointHash, score, proofType, proof, notes, timestamp);
            this.agentId = agentId.getValue();
            this.validator = validator.getValue();
            this.checkpointHash = checkpointHash.getValue();
            this.score = score.getValue();
            this.proofType = proofType.getValue();
            this.proof = proof.getValue();
            this.notes = notes.getValue();
            this.timestamp = timestamp.getValue();
        }
    }

    public static class AttestationPostedEventResponse extends BaseEventResponse {
        public BigInteger agentId;

        public String validator;

        public byte[] checkpointHash;

        public BigInteger score;

        public BigInteger proofType;
    }

    public static class ValidatorAddedEventResponse extends BaseEventResponse {
        public String validator;
    }

    public static class ValidatorRemovedEventResponse extends BaseEventResponse {
        public String validator;
    }
}
