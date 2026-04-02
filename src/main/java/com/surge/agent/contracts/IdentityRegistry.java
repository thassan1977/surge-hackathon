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
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple2;
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
public class IdentityRegistry extends Contract {
    public static final String BINARY = "60806040523480156200001157600080fd5b50336040518060400160405280600d81526020016c4167656e744964656e7469747960981b8152506040518060400160405280600381526020016210525160ea1b8152508160009081620000669190620001b2565b506001620000758282620001b2565b5050506001600160a01b038116620000a757604051631e4fbdf760e01b81526000600482015260240160405180910390fd5b620000b281620000b9565b506200027e565b600680546001600160a01b038381166001600160a01b0319831681179093556040519116919082907f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e090600090a35050565b634e487b7160e01b600052604160045260246000fd5b600181811c908216806200013657607f821691505b6020821081036200015757634e487b7160e01b600052602260045260246000fd5b50919050565b601f821115620001ad576000816000526020600020601f850160051c81016020861015620001885750805b601f850160051c820191505b81811015620001a95782815560010162000194565b5050505b505050565b81516001600160401b03811115620001ce57620001ce6200010b565b620001e681620001df845462000121565b846200015d565b602080601f8311600181146200021e5760008415620002055750858301515b600019600386901b1c1916600185901b178555620001a9565b600085815260208120601f198616915b828110156200024f578886015182559484019460019091019084016200022e565b50858210156200026e5787850151600019600388901b60f8161c191681555b5050505050600190811b01905550565b611393806200028e6000396000f3fe608060405234801561001057600080fd5b506004361061012c5760003560e01c80636f6bf118116100ad578063a22cb46511610071578063a22cb46514610299578063b88d4fde146102ac578063c87b56dd146102bf578063e985e9c5146102d2578063f2fde38b146102e557600080fd5b80636f6bf1181461023c57806370a0823114610265578063715018a6146102785780638da5cb5b1461028057806395d89b411461029157600080fd5b80631c453c65116100f45780631c453c65146101c157806323b872dd146101e25780632de5aaf7146101f557806342842e0e146102165780636352211e1461022957600080fd5b806301ffc9a71461013157806306fdde0314610159578063081812fc1461016e578063095ea7b3146101995780630af28bd3146101ae575b600080fd5b61014461013f366004610e30565b6102f8565b60405190151581526020015b60405180910390f35b61016161034a565b6040516101509190610e9a565b61018161017c366004610ead565b6103dc565b6040516001600160a01b039091168152602001610150565b6101ac6101a7366004610ee2565b610405565b005b6101ac6101bc366004610fb8565b610414565b6101d46101cf366004610fff565b610488565b604051908152602001610150565b6101ac6101f0366004611034565b610527565b610208610203366004610ead565b6105b2565b604051610150929190611070565b6101ac610224366004611034565b610661565b610181610237366004610ead565b61067c565b61018161024a366004610ead565b6009602052600090815260409020546001600160a01b031681565b6101d4610273366004611094565b610687565b6101ac6106cf565b6006546001600160a01b0316610181565b6101616106e3565b6101ac6102a73660046110af565b6106f2565b6101ac6102ba3660046110eb565b6106fd565b6101616102cd366004610ead565b610715565b6101446102e0366004611167565b6107bf565b6101ac6102f3366004611094565b6107ed565b60006001600160e01b031982166380ac58cd60e01b148061032957506001600160e01b03198216635b5e139f60e01b145b8061034457506301ffc9a760e01b6001600160e01b03198316145b92915050565b6060600080546103599061119a565b80601f01602080910402602001604051908101604052809291908181526020018280546103859061119a565b80156103d25780601f106103a7576101008083540402835291602001916103d2565b820191906000526020600020905b8154815290600101906020018083116103b557829003601f168201915b5050505050905090565b60006103e78261082b565b506000828152600460205260409020546001600160a01b0316610344565b610410828233610864565b5050565b3361041e8361067c565b6001600160a01b03161461046b5760405162461bcd60e51b815260206004820152600f60248201526e2737ba1030b3b2b73a1037bbb732b960891b60448201526064015b60405180910390fd5b6000828152600860205260409020610483828261121c565b505050565b600780546000918291908261049c836112dc565b9190505590506104ac3382610871565b60008181526008602052604090206104c4848261121c565b506000818152600960205260409081902080546001600160a01b03191633908117909155905182907f32d55b29ad8af7e18a168120f093abe134dbcbbe232113b2aa8e5e9f5dd8f81b90610519908790610e9a565b60405180910390a392915050565b6001600160a01b03821661055157604051633250574960e11b815260006004820152602401610462565b600061055e83833361088b565b9050836001600160a01b0316816001600160a01b0316146105ac576040516364283d7b60e01b81526001600160a01b0380861660048301526024820184905282166044820152606401610462565b50505050565b600060606105bf8361067c565b60008481526008602052604090208054919350906105dc9061119a565b80601f01602080910402602001604051908101604052809291908181526020018280546106089061119a565b80156106555780601f1061062a57610100808354040283529160200191610655565b820191906000526020600020905b81548152906001019060200180831161063857829003601f168201915b50505050509050915091565b610483838383604051806020016040528060008152506106fd565b60006103448261082b565b60006001600160a01b0382166106b3576040516322718ad960e21b815260006004820152602401610462565b506001600160a01b031660009081526003602052604090205490565b6106d7610984565b6106e160006109b1565b565b6060600180546103599061119a565b610410338383610a03565b610708848484610527565b6105ac3385858585610aa2565b60606107208261082b565b506000828152600860205260409020805461073a9061119a565b80601f01602080910402602001604051908101604052809291908181526020018280546107669061119a565b80156107b35780601f10610788576101008083540402835291602001916107b3565b820191906000526020600020905b81548152906001019060200180831161079657829003601f168201915b50505050509050919050565b6001600160a01b03918216600090815260056020908152604080832093909416825291909152205460ff1690565b6107f5610984565b6001600160a01b03811661081f57604051631e4fbdf760e01b815260006004820152602401610462565b610828816109b1565b50565b6000818152600260205260408120546001600160a01b03168061034457604051637e27328960e01b815260048101849052602401610462565b6104838383836001610bcd565b610410828260405180602001604052806000815250610cd3565b6000828152600260205260408120546001600160a01b03908116908316156108b8576108b8818486610ceb565b6001600160a01b038116156108f6576108d5600085600080610bcd565b6001600160a01b038116600090815260036020526040902080546000190190555b6001600160a01b03851615610925576001600160a01b0385166000908152600360205260409020805460010190555b60008481526002602052604080822080546001600160a01b0319166001600160a01b0389811691821790925591518793918516917fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef91a4949350505050565b6006546001600160a01b031633146106e15760405163118cdaa760e01b8152336004820152602401610462565b600680546001600160a01b038381166001600160a01b0319831681179093556040519116919082907f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e090600090a35050565b6001600160a01b038216610a3557604051630b61174360e31b81526001600160a01b0383166004820152602401610462565b6001600160a01b03838116600081815260056020908152604080832094871680845294825291829020805460ff191686151590811790915591519182527f17307eab39ab6107e8899845ad3d59bd9653f200f220920489ca2b5937696c31910160405180910390a3505050565b6001600160a01b0383163b15610bc657604051630a85bd0160e11b81526001600160a01b0384169063150b7a0290610ae4908890889087908790600401611303565b6020604051808303816000875af1925050508015610b1f575060408051601f3d908101601f19168201909252610b1c91810190611340565b60015b610b88573d808015610b4d576040519150601f19603f3d011682016040523d82523d6000602084013e610b52565b606091505b508051600003610b8057604051633250574960e11b81526001600160a01b0385166004820152602401610462565b805160208201fd5b6001600160e01b03198116630a85bd0160e11b14610bc457604051633250574960e11b81526001600160a01b0385166004820152602401610462565b505b5050505050565b8080610be157506001600160a01b03821615155b15610ca3576000610bf18461082b565b90506001600160a01b03831615801590610c1d5750826001600160a01b0316816001600160a01b031614155b8015610c305750610c2e81846107bf565b155b15610c595760405163a9fbf51f60e01b81526001600160a01b0384166004820152602401610462565b8115610ca15783856001600160a01b0316826001600160a01b03167f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b92560405160405180910390a45b505b5050600090815260046020526040902080546001600160a01b0319166001600160a01b0392909216919091179055565b610cdd8383610d4f565b610483336000858585610aa2565b610cf6838383610db4565b610483576001600160a01b038316610d2457604051637e27328960e01b815260048101829052602401610462565b60405163177e802f60e01b81526001600160a01b038316600482015260248101829052604401610462565b6001600160a01b038216610d7957604051633250574960e11b815260006004820152602401610462565b6000610d878383600061088b565b90506001600160a01b03811615610483576040516339e3563760e11b815260006004820152602401610462565b60006001600160a01b03831615801590610e125750826001600160a01b0316846001600160a01b03161480610dee5750610dee84846107bf565b80610e1257506000828152600460205260409020546001600160a01b038481169116145b949350505050565b6001600160e01b03198116811461082857600080fd5b600060208284031215610e4257600080fd5b8135610e4d81610e1a565b9392505050565b6000815180845260005b81811015610e7a57602081850181015186830182015201610e5e565b506000602082860101526020601f19601f83011685010191505092915050565b602081526000610e4d6020830184610e54565b600060208284031215610ebf57600080fd5b5035919050565b80356001600160a01b0381168114610edd57600080fd5b919050565b60008060408385031215610ef557600080fd5b610efe83610ec6565b946020939093013593505050565b634e487b7160e01b600052604160045260246000fd5b600067ffffffffffffffff80841115610f3d57610f3d610f0c565b604051601f8501601f19908116603f01168101908282118183101715610f6557610f65610f0c565b81604052809350858152868686011115610f7e57600080fd5b858560208301376000602087830101525050509392505050565b600082601f830112610fa957600080fd5b610e4d83833560208501610f22565b60008060408385031215610fcb57600080fd5b82359150602083013567ffffffffffffffff811115610fe957600080fd5b610ff585828601610f98565b9150509250929050565b60006020828403121561101157600080fd5b813567ffffffffffffffff81111561102857600080fd5b610e1284828501610f98565b60008060006060848603121561104957600080fd5b61105284610ec6565b925061106060208501610ec6565b9150604084013590509250925092565b6001600160a01b0383168152604060208201819052600090610e1290830184610e54565b6000602082840312156110a657600080fd5b610e4d82610ec6565b600080604083850312156110c257600080fd5b6110cb83610ec6565b9150602083013580151581146110e057600080fd5b809150509250929050565b6000806000806080858703121561110157600080fd5b61110a85610ec6565b935061111860208601610ec6565b925060408501359150606085013567ffffffffffffffff81111561113b57600080fd5b8501601f8101871361114c57600080fd5b61115b87823560208401610f22565b91505092959194509250565b6000806040838503121561117a57600080fd5b61118383610ec6565b915061119160208401610ec6565b90509250929050565b600181811c908216806111ae57607f821691505b6020821081036111ce57634e487b7160e01b600052602260045260246000fd5b50919050565b601f821115610483576000816000526020600020601f850160051c810160208610156111fd5750805b601f850160051c820191505b81811015610bc457828155600101611209565b815167ffffffffffffffff81111561123657611236610f0c565b61124a81611244845461119a565b846111d4565b602080601f83116001811461127f57600084156112675750858301515b600019600386901b1c1916600185901b178555610bc4565b600085815260208120601f198616915b828110156112ae5788860151825594840194600190910190840161128f565b50858210156112cc5787850151600019600388901b60f8161c191681555b5050505050600190811b01905550565b6000600182016112fc57634e487b7160e01b600052601160045260246000fd5b5060010190565b6001600160a01b038581168252841660208201526040810183905260806060820181905260009061133690830184610e54565b9695505050505050565b60006020828403121561135257600080fd5b8151610e4d81610e1a56fea26469706673582212206d224ef93e125d51ccb19cdc2f359007498d8020b7b8bc145786ca6b61feaab064736f6c63430008180033\n";

    private static String librariesLinkedBinary;

    public static final String FUNC_AGENTOWNER = "agentOwner";

    public static final String FUNC_APPROVE = "approve";

    public static final String FUNC_BALANCEOF = "balanceOf";

    public static final String FUNC_GETAGENT = "getAgent";

    public static final String FUNC_GETAPPROVED = "getApproved";

    public static final String FUNC_ISAPPROVEDFORALL = "isApprovedForAll";

    public static final String FUNC_MINTIDENTITY = "mintIdentity";

    public static final String FUNC_NAME = "name";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_OWNEROF = "ownerOf";

    public static final String FUNC_RENOUNCEOWNERSHIP = "renounceOwnership";

    public static final String FUNC_safeTransferFrom = "safeTransferFrom";

    public static final String FUNC_SETAGENTURI = "setAgentURI";

    public static final String FUNC_SETAPPROVALFORALL = "setApprovalForAll";

    public static final String FUNC_SUPPORTSINTERFACE = "supportsInterface";

    public static final String FUNC_SYMBOL = "symbol";

    public static final String FUNC_TOKENURI = "tokenURI";

    public static final String FUNC_TRANSFERFROM = "transferFrom";

    public static final String FUNC_TRANSFEROWNERSHIP = "transferOwnership";

    public static final Event APPROVAL_EVENT = new Event("Approval", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>(true) {}));
    ;

    public static final Event APPROVALFORALL_EVENT = new Event("ApprovalForAll", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Bool>() {}));
    ;

    public static final Event IDENTITYREGISTERED_EVENT = new Event("IdentityRegistered", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Utf8String>() {}));
    ;

    public static final Event OWNERSHIPTRANSFERRED_EVENT = new Event("OwnershipTransferred", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event TRANSFER_EVENT = new Event("Transfer", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>(true) {}));
    ;

    @Deprecated
    protected IdentityRegistry(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected IdentityRegistry(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected IdentityRegistry(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected IdentityRegistry(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<ApprovalEventResponse> getApprovalEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(APPROVAL_EVENT, transactionReceipt);
        ArrayList<ApprovalEventResponse> responses = new ArrayList<ApprovalEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ApprovalEventResponse typedResponse = new ApprovalEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.owner = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.approved = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.tokenId = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ApprovalEventResponse getApprovalEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(APPROVAL_EVENT, log);
        ApprovalEventResponse typedResponse = new ApprovalEventResponse();
        typedResponse.log = log;
        typedResponse.owner = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.approved = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.tokenId = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<ApprovalEventResponse> approvalEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getApprovalEventFromLog(log));
    }

    public Flowable<ApprovalEventResponse> approvalEventFlowable(DefaultBlockParameter startBlock,
            DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(APPROVAL_EVENT));
        return approvalEventFlowable(filter);
    }

    public static List<ApprovalForAllEventResponse> getApprovalForAllEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(APPROVALFORALL_EVENT, transactionReceipt);
        ArrayList<ApprovalForAllEventResponse> responses = new ArrayList<ApprovalForAllEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ApprovalForAllEventResponse typedResponse = new ApprovalForAllEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.owner = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.operator = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.approved = (Boolean) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ApprovalForAllEventResponse getApprovalForAllEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(APPROVALFORALL_EVENT, log);
        ApprovalForAllEventResponse typedResponse = new ApprovalForAllEventResponse();
        typedResponse.log = log;
        typedResponse.owner = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.operator = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.approved = (Boolean) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<ApprovalForAllEventResponse> approvalForAllEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getApprovalForAllEventFromLog(log));
    }

    public Flowable<ApprovalForAllEventResponse> approvalForAllEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(APPROVALFORALL_EVENT));
        return approvalForAllEventFlowable(filter);
    }

    public static List<IdentityRegisteredEventResponse> getIdentityRegisteredEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(IDENTITYREGISTERED_EVENT, transactionReceipt);
        ArrayList<IdentityRegisteredEventResponse> responses = new ArrayList<IdentityRegisteredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            IdentityRegisteredEventResponse typedResponse = new IdentityRegisteredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.owner = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.uri = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static IdentityRegisteredEventResponse getIdentityRegisteredEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(IDENTITYREGISTERED_EVENT, log);
        IdentityRegisteredEventResponse typedResponse = new IdentityRegisteredEventResponse();
        typedResponse.log = log;
        typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.owner = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.uri = (String) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<IdentityRegisteredEventResponse> identityRegisteredEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getIdentityRegisteredEventFromLog(log));
    }

    public Flowable<IdentityRegisteredEventResponse> identityRegisteredEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(IDENTITYREGISTERED_EVENT));
        return identityRegisteredEventFlowable(filter);
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

    public static List<TransferEventResponse> getTransferEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(TRANSFER_EVENT, transactionReceipt);
        ArrayList<TransferEventResponse> responses = new ArrayList<TransferEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            TransferEventResponse typedResponse = new TransferEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.from = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.to = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.tokenId = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static TransferEventResponse getTransferEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(TRANSFER_EVENT, log);
        TransferEventResponse typedResponse = new TransferEventResponse();
        typedResponse.log = log;
        typedResponse.from = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.to = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.tokenId = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<TransferEventResponse> transferEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getTransferEventFromLog(log));
    }

    public Flowable<TransferEventResponse> transferEventFlowable(DefaultBlockParameter startBlock,
            DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(TRANSFER_EVENT));
        return transferEventFlowable(filter);
    }

    public RemoteFunctionCall<String> agentOwner(BigInteger param0) {
        final Function function = new Function(FUNC_AGENTOWNER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> approve(String to, BigInteger tokenId) {
        final Function function = new Function(
                FUNC_APPROVE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, to), 
                new org.web3j.abi.datatypes.generated.Uint256(tokenId)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> balanceOf(String owner) {
        final Function function = new Function(FUNC_BALANCEOF, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, owner)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<Tuple2<String, String>> getAgent(BigInteger agentId) {
        final Function function = new Function(FUNC_GETAGENT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(agentId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Utf8String>() {}));
        return new RemoteFunctionCall<Tuple2<String, String>>(function,
                new Callable<Tuple2<String, String>>() {
                    @Override
                    public Tuple2<String, String> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple2<String, String>(
                                (String) results.get(0).getValue(), 
                                (String) results.get(1).getValue());
                    }
                });
    }

    public RemoteFunctionCall<String> getApproved(BigInteger tokenId) {
        final Function function = new Function(FUNC_GETAPPROVED, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(tokenId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<Boolean> isApprovedForAll(String owner, String operator) {
        final Function function = new Function(FUNC_ISAPPROVEDFORALL, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, owner), 
                new org.web3j.abi.datatypes.Address(160, operator)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<TransactionReceipt> mintIdentity(String uri) {
        final Function function = new Function(
                FUNC_MINTIDENTITY, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(uri)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> name() {
        final Function function = new Function(FUNC_NAME, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> owner() {
        final Function function = new Function(FUNC_OWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> ownerOf(BigInteger tokenId) {
        final Function function = new Function(FUNC_OWNEROF, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(tokenId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> renounceOwnership() {
        final Function function = new Function(
                FUNC_RENOUNCEOWNERSHIP, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> safeTransferFrom(String from, String to,
            BigInteger tokenId) {
        final Function function = new Function(
                FUNC_safeTransferFrom, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, from), 
                new org.web3j.abi.datatypes.Address(160, to), 
                new org.web3j.abi.datatypes.generated.Uint256(tokenId)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> safeTransferFrom(String from, String to,
            BigInteger tokenId, byte[] data) {
        final Function function = new Function(
                FUNC_safeTransferFrom, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, from), 
                new org.web3j.abi.datatypes.Address(160, to), 
                new org.web3j.abi.datatypes.generated.Uint256(tokenId), 
                new org.web3j.abi.datatypes.DynamicBytes(data)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setAgentURI(BigInteger agentId, String uri) {
        final Function function = new Function(
                FUNC_SETAGENTURI, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(agentId), 
                new org.web3j.abi.datatypes.Utf8String(uri)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setApprovalForAll(String operator,
            Boolean approved) {
        final Function function = new Function(
                FUNC_SETAPPROVALFORALL, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, operator), 
                new org.web3j.abi.datatypes.Bool(approved)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> supportsInterface(byte[] interfaceId) {
        final Function function = new Function(FUNC_SUPPORTSINTERFACE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes4(interfaceId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<String> symbol() {
        final Function function = new Function(FUNC_SYMBOL, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> tokenURI(BigInteger tokenId) {
        final Function function = new Function(FUNC_TOKENURI, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(tokenId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> transferFrom(String from, String to,
            BigInteger tokenId) {
        final Function function = new Function(
                FUNC_TRANSFERFROM, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, from), 
                new org.web3j.abi.datatypes.Address(160, to), 
                new org.web3j.abi.datatypes.generated.Uint256(tokenId)), 
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

    @Deprecated
    public static IdentityRegistry load(String contractAddress, Web3j web3j,
            Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new IdentityRegistry(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static IdentityRegistry load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new IdentityRegistry(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static IdentityRegistry load(String contractAddress, Web3j web3j,
            Credentials credentials, ContractGasProvider contractGasProvider) {
        return new IdentityRegistry(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static IdentityRegistry load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new IdentityRegistry(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<IdentityRegistry> deploy(Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        return deployRemoteCall(IdentityRegistry.class, web3j, credentials, contractGasProvider, getDeploymentBinary(), "");
    }

    public static RemoteCall<IdentityRegistry> deploy(Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(IdentityRegistry.class, web3j, transactionManager, contractGasProvider, getDeploymentBinary(), "");
    }

    @Deprecated
    public static RemoteCall<IdentityRegistry> deploy(Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(IdentityRegistry.class, web3j, credentials, gasPrice, gasLimit, getDeploymentBinary(), "");
    }

    @Deprecated
    public static RemoteCall<IdentityRegistry> deploy(Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(IdentityRegistry.class, web3j, transactionManager, gasPrice, gasLimit, getDeploymentBinary(), "");
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

    public static class ApprovalEventResponse extends BaseEventResponse {
        public String owner;

        public String approved;

        public BigInteger tokenId;
    }

    public static class ApprovalForAllEventResponse extends BaseEventResponse {
        public String owner;

        public String operator;

        public Boolean approved;
    }

    public static class IdentityRegisteredEventResponse extends BaseEventResponse {
        public BigInteger agentId;

        public String owner;

        public String uri;
    }

    public static class OwnershipTransferredEventResponse extends BaseEventResponse {
        public String previousOwner;

        public String newOwner;
    }

    public static class TransferEventResponse extends BaseEventResponse {
        public String from;

        public String to;

        public BigInteger tokenId;
    }
}
