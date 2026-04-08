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
    public static final String BINARY = "0x60806040523480156200001157600080fd5b50336040518060400160405280600d81526020016c4167656e744964656e7469747960981b8152506040518060400160405280600381526020016210525160ea1b8152508160009081620000669190620001b7565b506001620000758282620001b7565b5050506001600160a01b038116620000a757604051631e4fbdf760e01b81526000600482015260240160405180910390fd5b620000b281620000be565b50600160075562000283565b600680546001600160a01b038381166001600160a01b0319831681179093556040519116919082907f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e090600090a35050565b634e487b7160e01b600052604160045260246000fd5b600181811c908216806200013b57607f821691505b6020821081036200015c57634e487b7160e01b600052602260045260246000fd5b50919050565b601f821115620001b2576000816000526020600020601f850160051c810160208610156200018d5750805b601f850160051c820191505b81811015620001ae5782815560010162000199565b5050505b505050565b81516001600160401b03811115620001d357620001d362000110565b620001eb81620001e4845462000126565b8462000162565b602080601f8311600181146200022357600084156200020a5750858301515b600019600386901b1c1916600185901b178555620001ae565b600085815260208120601f198616915b82811015620002545788860151825594840194600190910190840162000233565b5085821015620002735787850151600019600388901b60f8161c191681555b5050505050600190811b01905550565b61144e80620002936000396000f3fe608060405234801561001057600080fd5b50600436106101425760003560e01c80636352211e116100b857806395d89b411161007c57806395d89b41146102ea578063a22cb465146102f2578063b88d4fde14610305578063c87b56dd14610318578063e985e9c51461032b578063f2fde38b1461033e57600080fd5b80636352211e146102825780636f6bf1181461029557806370a08231146102be578063715018a6146102d15780638da5cb5b146102d957600080fd5b80631c453c651161010a5780631c453c65146101d7578063237f1a21146101f857806323b872dd1461021b5780632de5aaf71461022e57806342842e0e1461024f578063568d39001461026257600080fd5b806301ffc9a71461014757806306fdde031461016f578063081812fc14610184578063095ea7b3146101af5780630af28bd3146101c4575b600080fd5b61015a610155366004610eeb565b610351565b60405190151581526020015b60405180910390f35b6101776103a3565b6040516101669190610f55565b610197610192366004610f68565b610435565b6040516001600160a01b039091168152602001610166565b6101c26101bd366004610f9d565b61045e565b005b6101c26101d2366004611073565b61046d565b6101ea6101e53660046110ba565b6104e1565b604051908152602001610166565b61015a6102063660046110ef565b600b6020526000908152604090205460ff1681565b6101c261022936600461110a565b6105e2565b61024161023c366004610f68565b61066d565b604051610166929190611146565b6101c261025d36600461110a565b61071c565b6101ea6102703660046110ef565b600a6020526000908152604090205481565b610197610290366004610f68565b610737565b6101976102a3366004610f68565b6009602052600090815260409020546001600160a01b031681565b6101ea6102cc3660046110ef565b610742565b6101c261078a565b6006546001600160a01b0316610197565b61017761079e565b6101c261030036600461116a565b6107ad565b6101c26103133660046111a6565b6107b8565b610177610326366004610f68565b6107d0565b61015a610339366004611222565b61087a565b6101c261034c3660046110ef565b6108a8565b60006001600160e01b031982166380ac58cd60e01b148061038257506001600160e01b03198216635b5e139f60e01b145b8061039d57506301ffc9a760e01b6001600160e01b03198316145b92915050565b6060600080546103b290611255565b80601f01602080910402602001604051908101604052809291908181526020018280546103de90611255565b801561042b5780601f106104005761010080835404028352916020019161042b565b820191906000526020600020905b81548152906001019060200180831161040e57829003601f168201915b5050505050905090565b6000610440826108e6565b506000828152600460205260409020546001600160a01b031661039d565b61046982823361091f565b5050565b3361047783610737565b6001600160a01b0316146104c45760405162461bcd60e51b815260206004820152600f60248201526e2737ba1030b3b2b73a1037bbb732b960891b60448201526064015b60405180910390fd5b60008281526008602052604090206104dc82826112d7565b505050565b336000908152600b602052604081205460ff16156105415760405162461bcd60e51b815260206004820152601f60248201527f4164647265737320616c72656164792068617320616e206964656e746974790060448201526064016104bb565b600780546000918261055283611397565b919050559050610562338261092c565b600081815260086020526040902061057a84826112d7565b50336000818152600a60209081526040808320859055600b90915290819020805460ff191660011790555182907f32d55b29ad8af7e18a168120f093abe134dbcbbe232113b2aa8e5e9f5dd8f81b906105d4908790610f55565b60405180910390a392915050565b6001600160a01b03821661060c57604051633250574960e11b8152600060048201526024016104bb565b6000610619838333610946565b9050836001600160a01b0316816001600160a01b031614610667576040516364283d7b60e01b81526001600160a01b03808616600483015260248201849052821660448201526064016104bb565b50505050565b6000606061067a83610737565b600084815260086020526040902080549193509061069790611255565b80601f01602080910402602001604051908101604052809291908181526020018280546106c390611255565b80156107105780601f106106e557610100808354040283529160200191610710565b820191906000526020600020905b8154815290600101906020018083116106f357829003601f168201915b50505050509050915091565b6104dc838383604051806020016040528060008152506107b8565b600061039d826108e6565b60006001600160a01b03821661076e576040516322718ad960e21b8152600060048201526024016104bb565b506001600160a01b031660009081526003602052604090205490565b610792610a3f565b61079c6000610a6c565b565b6060600180546103b290611255565b610469338383610abe565b6107c38484846105e2565b6106673385858585610b5d565b60606107db826108e6565b50600082815260086020526040902080546107f590611255565b80601f016020809104026020016040519081016040528092919081815260200182805461082190611255565b801561086e5780601f106108435761010080835404028352916020019161086e565b820191906000526020600020905b81548152906001019060200180831161085157829003601f168201915b50505050509050919050565b6001600160a01b03918216600090815260056020908152604080832093909416825291909152205460ff1690565b6108b0610a3f565b6001600160a01b0381166108da57604051631e4fbdf760e01b8152600060048201526024016104bb565b6108e381610a6c565b50565b6000818152600260205260408120546001600160a01b03168061039d57604051637e27328960e01b8152600481018490526024016104bb565b6104dc8383836001610c88565b610469828260405180602001604052806000815250610d8e565b6000828152600260205260408120546001600160a01b039081169083161561097357610973818486610da6565b6001600160a01b038116156109b157610990600085600080610c88565b6001600160a01b038116600090815260036020526040902080546000190190555b6001600160a01b038516156109e0576001600160a01b0385166000908152600360205260409020805460010190555b60008481526002602052604080822080546001600160a01b0319166001600160a01b0389811691821790925591518793918516917fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef91a4949350505050565b6006546001600160a01b0316331461079c5760405163118cdaa760e01b81523360048201526024016104bb565b600680546001600160a01b038381166001600160a01b0319831681179093556040519116919082907f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e090600090a35050565b6001600160a01b038216610af057604051630b61174360e31b81526001600160a01b03831660048201526024016104bb565b6001600160a01b03838116600081815260056020908152604080832094871680845294825291829020805460ff191686151590811790915591519182527f17307eab39ab6107e8899845ad3d59bd9653f200f220920489ca2b5937696c31910160405180910390a3505050565b6001600160a01b0383163b15610c8157604051630a85bd0160e11b81526001600160a01b0384169063150b7a0290610b9f9088908890879087906004016113be565b6020604051808303816000875af1925050508015610bda575060408051601f3d908101601f19168201909252610bd7918101906113fb565b60015b610c43573d808015610c08576040519150601f19603f3d011682016040523d82523d6000602084013e610c0d565b606091505b508051600003610c3b57604051633250574960e11b81526001600160a01b03851660048201526024016104bb565b805160208201fd5b6001600160e01b03198116630a85bd0160e11b14610c7f57604051633250574960e11b81526001600160a01b03851660048201526024016104bb565b505b5050505050565b8080610c9c57506001600160a01b03821615155b15610d5e576000610cac846108e6565b90506001600160a01b03831615801590610cd85750826001600160a01b0316816001600160a01b031614155b8015610ceb5750610ce9818461087a565b155b15610d145760405163a9fbf51f60e01b81526001600160a01b03841660048201526024016104bb565b8115610d5c5783856001600160a01b0316826001600160a01b03167f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b92560405160405180910390a45b505b5050600090815260046020526040902080546001600160a01b0319166001600160a01b0392909216919091179055565b610d988383610e0a565b6104dc336000858585610b5d565b610db1838383610e6f565b6104dc576001600160a01b038316610ddf57604051637e27328960e01b8152600481018290526024016104bb565b60405163177e802f60e01b81526001600160a01b0383166004820152602481018290526044016104bb565b6001600160a01b038216610e3457604051633250574960e11b8152600060048201526024016104bb565b6000610e4283836000610946565b90506001600160a01b038116156104dc576040516339e3563760e11b8152600060048201526024016104bb565b60006001600160a01b03831615801590610ecd5750826001600160a01b0316846001600160a01b03161480610ea95750610ea9848461087a565b80610ecd57506000828152600460205260409020546001600160a01b038481169116145b949350505050565b6001600160e01b0319811681146108e357600080fd5b600060208284031215610efd57600080fd5b8135610f0881610ed5565b9392505050565b6000815180845260005b81811015610f3557602081850181015186830182015201610f19565b506000602082860101526020601f19601f83011685010191505092915050565b602081526000610f086020830184610f0f565b600060208284031215610f7a57600080fd5b5035919050565b80356001600160a01b0381168114610f9857600080fd5b919050565b60008060408385031215610fb057600080fd5b610fb983610f81565b946020939093013593505050565b634e487b7160e01b600052604160045260246000fd5b600067ffffffffffffffff80841115610ff857610ff8610fc7565b604051601f8501601f19908116603f0116810190828211818310171561102057611020610fc7565b8160405280935085815286868601111561103957600080fd5b858560208301376000602087830101525050509392505050565b600082601f83011261106457600080fd5b610f0883833560208501610fdd565b6000806040838503121561108657600080fd5b82359150602083013567ffffffffffffffff8111156110a457600080fd5b6110b085828601611053565b9150509250929050565b6000602082840312156110cc57600080fd5b813567ffffffffffffffff8111156110e357600080fd5b610ecd84828501611053565b60006020828403121561110157600080fd5b610f0882610f81565b60008060006060848603121561111f57600080fd5b61112884610f81565b925061113660208501610f81565b9150604084013590509250925092565b6001600160a01b0383168152604060208201819052600090610ecd90830184610f0f565b6000806040838503121561117d57600080fd5b61118683610f81565b91506020830135801515811461119b57600080fd5b809150509250929050565b600080600080608085870312156111bc57600080fd5b6111c585610f81565b93506111d360208601610f81565b925060408501359150606085013567ffffffffffffffff8111156111f657600080fd5b8501601f8101871361120757600080fd5b61121687823560208401610fdd565b91505092959194509250565b6000806040838503121561123557600080fd5b61123e83610f81565b915061124c60208401610f81565b90509250929050565b600181811c9082168061126957607f821691505b60208210810361128957634e487b7160e01b600052602260045260246000fd5b50919050565b601f8211156104dc576000816000526020600020601f850160051c810160208610156112b85750805b601f850160051c820191505b81811015610c7f578281556001016112c4565b815167ffffffffffffffff8111156112f1576112f1610fc7565b611305816112ff8454611255565b8461128f565b602080601f83116001811461133a57600084156113225750858301515b600019600386901b1c1916600185901b178555610c7f565b600085815260208120601f198616915b828110156113695788860151825594840194600190910190840161134a565b50858210156113875787850151600019600388901b60f8161c191681555b5050505050600190811b01905550565b6000600182016113b757634e487b7160e01b600052601160045260246000fd5b5060010190565b6001600160a01b03858116825284166020820152604081018390526080606082018190526000906113f190830184610f0f565b9695505050505050565b60006020828403121561140d57600080fd5b8151610f0881610ed556fea2646970667358221220bdb809bc6eb8c9060f13a5c3904912b04fb5cbdb1677a4f5aab99ea87b4b64dc64736f6c63430008180033\n";

    private static String librariesLinkedBinary;

    public static final String FUNC_AGENTOWNER = "agentOwner";

    public static final String FUNC_APPROVE = "approve";

    public static final String FUNC_BALANCEOF = "balanceOf";

    public static final String FUNC_GETAGENT = "getAgent";

    public static final String FUNC_GETAGENTIDBYADDRESS = "addressToAgentId";

    public static final String FUNC_GETAPPROVED = "getApproved";

    public static final String FUNC_HASIDENTITY = "hasIdentity";

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

//    public static final CustomError ERC721INCORRECTOWNER_ERROR = new CustomError("ERC721IncorrectOwner",
//            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Uint256>() {}, new TypeReference<Address>() {}));
//    ;
//
//    public static final CustomError ERC721INSUFFICIENTAPPROVAL_ERROR = new CustomError("ERC721InsufficientApproval",
//            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Uint256>() {}));
//    ;
//
//    public static final CustomError ERC721INVALIDAPPROVER_ERROR = new CustomError("ERC721InvalidApprover",
//            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
//    ;
//
//    public static final CustomError ERC721INVALIDOPERATOR_ERROR = new CustomError("ERC721InvalidOperator",
//            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
//    ;
//
//    public static final CustomError ERC721INVALIDOWNER_ERROR = new CustomError("ERC721InvalidOwner",
//            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
//    ;
//
//    public static final CustomError ERC721INVALIDRECEIVER_ERROR = new CustomError("ERC721InvalidReceiver",
//            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
//    ;
//
//    public static final CustomError ERC721INVALIDSENDER_ERROR = new CustomError("ERC721InvalidSender",
//            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
//    ;
//
//    public static final CustomError ERC721NONEXISTENTTOKEN_ERROR = new CustomError("ERC721NonexistentToken",
//            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
//    ;
//
//    public static final CustomError OWNABLEINVALIDOWNER_ERROR = new CustomError("OwnableInvalidOwner",
//            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
//    ;
//
//    public static final CustomError OWNABLEUNAUTHORIZEDACCOUNT_ERROR = new CustomError("OwnableUnauthorizedAccount",
//            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
//    ;

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

    public RemoteFunctionCall<BigInteger> getAgentIdByAddress(String param0) {
        final Function function = new Function(FUNC_GETAGENTIDBYADDRESS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<String> getApproved(BigInteger tokenId) {
        final Function function = new Function(FUNC_GETAPPROVED, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(tokenId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<Boolean> hasIdentity(String param0) {
        final Function function = new Function(FUNC_HASIDENTITY, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
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
