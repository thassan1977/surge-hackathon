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
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.StaticStruct;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes1;
import org.web3j.abi.datatypes.generated.Bytes32;
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
public class MockRiskRouter extends Contract {
    public static final String BINARY = "6101606040526402540be400600655633b9aca006007553480156200002357600080fd5b50604051620013b6380380620013b68339810160408190526200004691620002a4565b604080518082018252600e81526d26b7b1b5a934b9b5a937baba32b960911b602080830191909152825180840190935260018352603160f81b90830152339162000092826000620001b9565b61012052620000a3816001620001b9565b61014052815160208084019190912060e052815190820120610100524660a0526200013160e05161010051604080517f8b73c3c69bb8fe3d512ecc4cf759cc79239f7b179b0ffacaa9a75d522b39400f60208201529081019290925260608201524660808201523060a082015260009060c00160405160208183030381529060405280519060200120905090565b60805250503060c0526001600160a01b0381166200016a57604051631e4fbdf760e01b8152600060048201526024015b60405180910390fd5b6200017581620001f2565b50600380546001600160a01b039485166001600160a01b031991821617909155600480549385169382169390931790925560058054919093169116179055620004d7565b6000602083511015620001d957620001d18362000244565b9050620001ec565b81620001e6848262000395565b5060ff90505b92915050565b600280546001600160a01b038381166001600160a01b0319831681179093556040519116919082907f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e090600090a35050565b600080829050601f8151111562000272578260405163305a27a960e01b815260040162000161919062000461565b80516200027f82620004b2565b179392505050565b80516001600160a01b03811681146200029f57600080fd5b919050565b600080600060608486031215620002ba57600080fd5b620002c58462000287565b9250620002d56020850162000287565b9150620002e56040850162000287565b90509250925092565b634e487b7160e01b600052604160045260246000fd5b600181811c908216806200031957607f821691505b6020821081036200033a57634e487b7160e01b600052602260045260246000fd5b50919050565b601f82111562000390576000816000526020600020601f850160051c810160208610156200036b5750805b601f850160051c820191505b818110156200038c5782815560010162000377565b5050505b505050565b81516001600160401b03811115620003b157620003b1620002ee565b620003c981620003c2845462000304565b8462000340565b602080601f831160018114620004015760008415620003e85750858301515b600019600386901b1c1916600185901b1785556200038c565b600085815260208120601f198616915b82811015620004325788860151825594840194600190910190840162000411565b5085821015620004515787850151600019600388901b60f8161c191681555b5050505050600190811b01905550565b60006020808352835180602085015260005b81811015620004915785810183015185820160400152820162000473565b506000604082860101526040601f19601f8301168501019250505092915050565b805160208083015191908110156200033a5760001960209190910360031b1b16919050565b60805160a05160c05160e051610100516101205161014051610e8462000532600039600061076a01526000610738015260006109720152600061094a015260006108a5015260006108cf015260006108f90152610e846000f3fe608060405234801561001057600080fd5b50600436106100cf5760003560e01c806392c166261161008c578063d10ff52b11610066578063d10ff52b146101a0578063e9cbd822146101b3578063f2fde38b146101c6578063fbfa77cf146101d957600080fd5b806392c166261461017b5780639ea8741c1461018e578063a329dc571461019757600080fd5b806305c829ef146100d4578063134e18f41461010757806326a9c12114610132578063715018a61461014757806384b0196e1461014f5780638da5cb5b1461016a575b600080fd5b6100f46100e2366004610c03565b60086020526000908152604090205481565b6040519081526020015b60405180910390f35b60055461011a906001600160a01b031681565b6040516001600160a01b0390911681526020016100fe565b610145610140366004610c03565b6101ec565b005b6101456101f9565b61015761020d565b6040516100fe9796959493929190610c62565b6002546001600160a01b031661011a565b610145610189366004610c03565b610253565b6100f460075481565b6100f460065481565b6101456101ae366004610cfb565b61026c565b60045461011a906001600160a01b031681565b6101456101d4366004610d9c565b610674565b60035461011a906001600160a01b031681565b6101f46106b2565b600655565b6102016106b2565b61020b60006106df565b565b600060608060008060006060610221610731565b610229610763565b60408051600080825260208201909252600f60f81b9b939a50919850469750309650945092509050565b61025b6106b2565b600090815260086020526040812055565b428360a0013510156102b85760405162461bcd60e51b815260206004820152601060248201526f111958591b1a5b9948195e1c1a5c995960821b60448201526064015b60405180910390fd5b60006103837f9672b7ab9018778b1d3ac332d78c6ab56416d6fc5ac874576b80508c6bb64d8185356102f06040880160208901610d9c565b6103006060890160408a01610d9c565b886060013589608001358a60a001358b60c0013560405160200161036898979695949392919097885260208801969096526001600160a01b039485166040880152929093166060860152608085015260a084019190915260c083015260e08201526101000190565b60405160208183030381529060405280519060200120610790565b905060006103c78285858080601f0160208091040260200160405190810160405280939291908181526020018383808284376000920191909152506107c392505050565b6005546040516331a9108f60e11b8152873560048201529192506001600160a01b031690636352211e90602401602060405180830381865afa158015610411573d6000803e3d6000fd5b505050506040513d601f19601f820116820180604052508101906104359190610dc0565b6001600160a01b0316816001600160a01b0316146104895760405162461bcd60e51b8152602060048201526011602482015270496e76616c6964207369676e617475726560781b60448201526064016102af565b600654856060013511156104df5760405162461bcd60e51b815260206004820152601960248201527f45786365656473206d617820706f736974696f6e2073697a650000000000000060448201526064016102af565b600754853560009081526008602052604090205460608701359190610505908390610ddd565b11156105535760405162461bcd60e51b815260206004820152601960248201527f4461696c79206c6f7373206c696d69742065786365656465640000000000000060448201526064016102af565b853560009081526008602052604081208054839290610573908490610ddd565b90915550506003546040516326b6f25b60e11b81528735600482015260608801356024820181905260448201819052916001600160a01b031690634d6de4b690606401600060405180830381600087803b1580156105d057600080fd5b505af11580156105e4573d6000803e3d6000fd5b5050604080516060808c013582526020820186905242928201929092528a3593507fdebfa329389a6f20d44c8ae61763ba5f5ddfe7f867d953dbcd740a80568fcbd292500160405180910390a260405160c088013581528735907f97b07a387ebabf3ca254bd6a040dff8fa5d3f817460b355a0b1edb518b2685649060200160405180910390a250505050505050565b61067c6106b2565b6001600160a01b0381166106a657604051631e4fbdf760e01b8152600060048201526024016102af565b6106af816106df565b50565b6002546001600160a01b0316331461020b5760405163118cdaa760e01b81523360048201526024016102af565b600280546001600160a01b038381166001600160a01b0319831681179093556040519116919082907f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e090600090a35050565b606061075e7f000000000000000000000000000000000000000000000000000000000000000060006107ed565b905090565b606061075e7f000000000000000000000000000000000000000000000000000000000000000060016107ed565b60006107bd61079d610898565b8360405161190160f01b8152600281019290925260228201526042902090565b92915050565b6000806000806107d386866109c3565b9250925092506107e38282610a10565b5090949350505050565b606060ff83146108075761080083610acd565b90506107bd565b81805461081390610dfe565b80601f016020809104026020016040519081016040528092919081815260200182805461083f90610dfe565b801561088c5780601f106108615761010080835404028352916020019161088c565b820191906000526020600020905b81548152906001019060200180831161086f57829003601f168201915b505050505090506107bd565b6000306001600160a01b037f0000000000000000000000000000000000000000000000000000000000000000161480156108f157507f000000000000000000000000000000000000000000000000000000000000000046145b1561091b57507f000000000000000000000000000000000000000000000000000000000000000090565b61075e604080517f8b73c3c69bb8fe3d512ecc4cf759cc79239f7b179b0ffacaa9a75d522b39400f60208201527f0000000000000000000000000000000000000000000000000000000000000000918101919091527f000000000000000000000000000000000000000000000000000000000000000060608201524660808201523060a082015260009060c00160405160208183030381529060405280519060200120905090565b600080600083516041036109fd5760208401516040850151606086015160001a6109ef88828585610b0c565b955095509550505050610a09565b50508151600091506002905b9250925092565b6000826003811115610a2457610a24610e38565b03610a2d575050565b6001826003811115610a4157610a41610e38565b03610a5f5760405163f645eedf60e01b815260040160405180910390fd5b6002826003811115610a7357610a73610e38565b03610a945760405163fce698f760e01b8152600481018290526024016102af565b6003826003811115610aa857610aa8610e38565b03610ac9576040516335e2f38360e21b8152600481018290526024016102af565b5050565b60606000610ada83610bdb565b604080516020808252818301909252919250600091906020820181803683375050509182525060208101929092525090565b600080807f7fffffffffffffffffffffffffffffff5d576e7357a4501ddfe92f46681b20a0841115610b475750600091506003905082610bd1565b604080516000808252602082018084528a905260ff891692820192909252606081018790526080810186905260019060a0016020604051602081039080840390855afa158015610b9b573d6000803e3d6000fd5b5050604051601f1901519150506001600160a01b038116610bc757506000925060019150829050610bd1565b9250600091508190505b9450945094915050565b600060ff8216601f8111156107bd57604051632cd44ac360e21b815260040160405180910390fd5b600060208284031215610c1557600080fd5b5035919050565b6000815180845260005b81811015610c4257602081850181015186830182015201610c26565b506000602082860101526020601f19601f83011685010191505092915050565b60ff60f81b881681526000602060e06020840152610c8360e084018a610c1c565b8381036040850152610c95818a610c1c565b606085018990526001600160a01b038816608086015260a0850187905284810360c08601528551808252602080880193509091019060005b81811015610ce957835183529284019291840191600101610ccd565b50909c9b505050505050505050505050565b6000806000838503610100811215610d1257600080fd5b60e0811215610d2057600080fd5b5083925060e084013567ffffffffffffffff80821115610d3f57600080fd5b818601915086601f830112610d5357600080fd5b813581811115610d6257600080fd5b876020828501011115610d7457600080fd5b6020830194508093505050509250925092565b6001600160a01b03811681146106af57600080fd5b600060208284031215610dae57600080fd5b8135610db981610d87565b9392505050565b600060208284031215610dd257600080fd5b8151610db981610d87565b808201808211156107bd57634e487b7160e01b600052601160045260246000fd5b600181811c90821680610e1257607f821691505b602082108103610e3257634e487b7160e01b600052602260045260246000fd5b50919050565b634e487b7160e01b600052602160045260246000fdfea264697066735822122092fe5379051fe2f46679524a13ef5010efacc711cf01ca1483620bac669cd9c964736f6c63430008180033\n";

    private static String librariesLinkedBinary;

    public static final String FUNC_DAILYLOSS = "dailyLoss";

    public static final String FUNC_EIP712DOMAIN = "eip712Domain";

    public static final String FUNC_EXECUTETRADE = "executeTrade";

    public static final String FUNC_IDENTITYREGISTRY = "identityRegistry";

    public static final String FUNC_MAXDAILYLOSS = "maxDailyLoss";

    public static final String FUNC_MAXPOSITIONSIZE = "maxPositionSize";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_RENOUNCEOWNERSHIP = "renounceOwnership";

    public static final String FUNC_RESETDAILYLOSS = "resetDailyLoss";

    public static final String FUNC_SETMAXPOSITIONSIZE = "setMaxPositionSize";

    public static final String FUNC_STABLECOIN = "stablecoin";

    public static final String FUNC_TRANSFEROWNERSHIP = "transferOwnership";

    public static final String FUNC_VAULT = "vault";

    public static final Event EIP712DOMAINCHANGED_EVENT = new Event("EIP712DomainChanged", 
            Arrays.<TypeReference<?>>asList());
    ;

    public static final Event OWNERSHIPTRANSFERRED_EVENT = new Event("OwnershipTransferred", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event RISKCHECKPASSED_EVENT = new Event("RiskCheckPassed", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Bytes32>() {}));
    ;

    public static final Event TRADEEXECUTED_EVENT = new Event("TradeExecuted", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected MockRiskRouter(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected MockRiskRouter(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected MockRiskRouter(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected MockRiskRouter(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
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

    public static List<RiskCheckPassedEventResponse> getRiskCheckPassedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(RISKCHECKPASSED_EVENT, transactionReceipt);
        ArrayList<RiskCheckPassedEventResponse> responses = new ArrayList<RiskCheckPassedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RiskCheckPassedEventResponse typedResponse = new RiskCheckPassedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.riskParams = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RiskCheckPassedEventResponse getRiskCheckPassedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(RISKCHECKPASSED_EVENT, log);
        RiskCheckPassedEventResponse typedResponse = new RiskCheckPassedEventResponse();
        typedResponse.log = log;
        typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.riskParams = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<RiskCheckPassedEventResponse> riskCheckPassedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRiskCheckPassedEventFromLog(log));
    }

    public Flowable<RiskCheckPassedEventResponse> riskCheckPassedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(RISKCHECKPASSED_EVENT));
        return riskCheckPassedEventFlowable(filter);
    }

    public static List<TradeExecutedEventResponse> getTradeExecutedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(TRADEEXECUTED_EVENT, transactionReceipt);
        ArrayList<TradeExecutedEventResponse> responses = new ArrayList<TradeExecutedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            TradeExecutedEventResponse typedResponse = new TradeExecutedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.amountIn = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.amountOut = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static TradeExecutedEventResponse getTradeExecutedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(TRADEEXECUTED_EVENT, log);
        TradeExecutedEventResponse typedResponse = new TradeExecutedEventResponse();
        typedResponse.log = log;
        typedResponse.agentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.amountIn = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.amountOut = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<TradeExecutedEventResponse> tradeExecutedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getTradeExecutedEventFromLog(log));
    }

    public Flowable<TradeExecutedEventResponse> tradeExecutedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(TRADEEXECUTED_EVENT));
        return tradeExecutedEventFlowable(filter);
    }

    public RemoteFunctionCall<BigInteger> dailyLoss(BigInteger param0) {
        final Function function = new Function(FUNC_DAILYLOSS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
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

    public RemoteFunctionCall<TransactionReceipt> executeTrade(TradeIntent intent,
            byte[] signature) {
        final Function function = new Function(
                FUNC_EXECUTETRADE, 
                Arrays.<Type>asList(intent, 
                new org.web3j.abi.datatypes.DynamicBytes(signature)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> identityRegistry() {
        final Function function = new Function(FUNC_IDENTITYREGISTRY, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<BigInteger> maxDailyLoss() {
        final Function function = new Function(FUNC_MAXDAILYLOSS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> maxPositionSize() {
        final Function function = new Function(FUNC_MAXPOSITIONSIZE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<String> owner() {
        final Function function = new Function(FUNC_OWNER, 
                Arrays.<Type>asList(), 
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

    public RemoteFunctionCall<TransactionReceipt> resetDailyLoss(BigInteger agentId) {
        final Function function = new Function(
                FUNC_RESETDAILYLOSS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(agentId)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setMaxPositionSize(BigInteger newMax) {
        final Function function = new Function(
                FUNC_SETMAXPOSITIONSIZE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(newMax)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> stablecoin() {
        final Function function = new Function(FUNC_STABLECOIN, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> transferOwnership(String newOwner) {
        final Function function = new Function(
                FUNC_TRANSFEROWNERSHIP, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, newOwner)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> vault() {
        final Function function = new Function(FUNC_VAULT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    @Deprecated
    public static MockRiskRouter load(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        return new MockRiskRouter(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static MockRiskRouter load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new MockRiskRouter(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static MockRiskRouter load(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        return new MockRiskRouter(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static MockRiskRouter load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new MockRiskRouter(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<MockRiskRouter> deploy(Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider, String _vault, String _stablecoin,
            String _identityRegistry) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _vault), 
                new org.web3j.abi.datatypes.Address(160, _stablecoin), 
                new org.web3j.abi.datatypes.Address(160, _identityRegistry)));
        return deployRemoteCall(MockRiskRouter.class, web3j, credentials, contractGasProvider, getDeploymentBinary(), encodedConstructor);
    }

    public static RemoteCall<MockRiskRouter> deploy(Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider,
            String _vault, String _stablecoin, String _identityRegistry) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _vault), 
                new org.web3j.abi.datatypes.Address(160, _stablecoin), 
                new org.web3j.abi.datatypes.Address(160, _identityRegistry)));
        return deployRemoteCall(MockRiskRouter.class, web3j, transactionManager, contractGasProvider, getDeploymentBinary(), encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<MockRiskRouter> deploy(Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit, String _vault, String _stablecoin,
            String _identityRegistry) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _vault), 
                new org.web3j.abi.datatypes.Address(160, _stablecoin), 
                new org.web3j.abi.datatypes.Address(160, _identityRegistry)));
        return deployRemoteCall(MockRiskRouter.class, web3j, credentials, gasPrice, gasLimit, getDeploymentBinary(), encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<MockRiskRouter> deploy(Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit,
            String _vault, String _stablecoin, String _identityRegistry) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _vault), 
                new org.web3j.abi.datatypes.Address(160, _stablecoin), 
                new org.web3j.abi.datatypes.Address(160, _identityRegistry)));
        return deployRemoteCall(MockRiskRouter.class, web3j, transactionManager, gasPrice, gasLimit, getDeploymentBinary(), encodedConstructor);
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

    public static class TradeIntent extends StaticStruct {
        public BigInteger agentId;

        public String tokenIn;

        public String tokenOut;

        public BigInteger amountIn;

        public BigInteger minAmountOut;

        public BigInteger deadline;

        public byte[] riskParams;

        public TradeIntent(BigInteger agentId, String tokenIn, String tokenOut, BigInteger amountIn,
                BigInteger minAmountOut, BigInteger deadline, byte[] riskParams) {
            super(new org.web3j.abi.datatypes.generated.Uint256(agentId), 
                    new org.web3j.abi.datatypes.Address(160, tokenIn), 
                    new org.web3j.abi.datatypes.Address(160, tokenOut), 
                    new org.web3j.abi.datatypes.generated.Uint256(amountIn), 
                    new org.web3j.abi.datatypes.generated.Uint256(minAmountOut), 
                    new org.web3j.abi.datatypes.generated.Uint256(deadline), 
                    new org.web3j.abi.datatypes.generated.Bytes32(riskParams));
            this.agentId = agentId;
            this.tokenIn = tokenIn;
            this.tokenOut = tokenOut;
            this.amountIn = amountIn;
            this.minAmountOut = minAmountOut;
            this.deadline = deadline;
            this.riskParams = riskParams;
        }

        public TradeIntent(Uint256 agentId, Address tokenIn, Address tokenOut, Uint256 amountIn,
                Uint256 minAmountOut, Uint256 deadline, Bytes32 riskParams) {
            super(agentId, tokenIn, tokenOut, amountIn, minAmountOut, deadline, riskParams);
            this.agentId = agentId.getValue();
            this.tokenIn = tokenIn.getValue();
            this.tokenOut = tokenOut.getValue();
            this.amountIn = amountIn.getValue();
            this.minAmountOut = minAmountOut.getValue();
            this.deadline = deadline.getValue();
            this.riskParams = riskParams.getValue();
        }
    }

    public static class EIP712DomainChangedEventResponse extends BaseEventResponse {
    }

    public static class OwnershipTransferredEventResponse extends BaseEventResponse {
        public String previousOwner;

        public String newOwner;
    }

    public static class RiskCheckPassedEventResponse extends BaseEventResponse {
        public BigInteger agentId;

        public byte[] riskParams;
    }

    public static class TradeExecutedEventResponse extends BaseEventResponse {
        public BigInteger agentId;

        public BigInteger amountIn;

        public BigInteger amountOut;

        public BigInteger timestamp;
    }
}
