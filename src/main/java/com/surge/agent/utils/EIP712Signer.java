package com.surge.agent.utils;

import com.surge.agent.model.TradeIntent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import org.bouncycastle.crypto.digests.KeccakDigest;


import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * EIP-712 TradeIntent Signer — production grade.
 *
 * Replaces the web3j StructuredDataEncoder approach which has known issues:
 *   - StructuredDataEncoder requires exact JSON format matching (brittle)
 *   - bytes32 encoding is inconsistent across web3j versions
 *   - No control over field ordering → wrong struct hash
 *
 * This implementation encodes the EIP-712 hash directly in Java:
 *   DOMAIN_SEPARATOR = keccak256(abi.encode(domainTypeHash, name, version, chainId, contract))
 *   STRUCT_HASH      = keccak256(abi.encode(typeHash, agentId, tokenIn, ..., riskParams))
 *   DIGEST           = keccak256("\x19\x01" || domainSeparator || structHash)
 *   SIGNATURE        = secp256k1.sign(digest) → [r(32) || s(32) || v(1)] = 65 bytes
 *
 * Solidity counterpart (must match exactly):
 * ─────────────────────────────────────────────────────────────────────────────
 *   bytes32 constant DOMAIN_TYPEHASH = keccak256(
 *     "EIP712Domain(string name,string version,uint256 chainId,address verifyingContract)"
 *   );
 *   bytes32 constant TRADE_INTENT_TYPEHASH = keccak256(
 *     "TradeIntent(uint256 agentId,address tokenIn,address tokenOut,"
 *     "uint256 amountIn,uint256 minAmountOut,uint256 deadline,bytes32 riskParams)"
 *   );
 *   bytes32 DOMAIN_SEPARATOR = keccak256(abi.encode(
 *     DOMAIN_TYPEHASH,
 *     keccak256("MockRiskRouter"),
 *     keccak256("1"),
 *     block.chainid,
 *     address(this)
 *   ));
 *   function verify(TradeIntent calldata intent, bytes calldata sig) public view returns (bool) {
 *     bytes32 structHash = keccak256(abi.encode(TRADE_INTENT_TYPEHASH,
 *       intent.agentId, intent.tokenIn, intent.tokenOut, intent.amountIn,
 *       intent.minAmountOut, intent.deadline, intent.riskParams));
 *     bytes32 digest = keccak256(abi.encodePacked("\x19\x01", DOMAIN_SEPARATOR, structHash));
 *     address signer = ECDSA.recover(digest, sig);
 *     return signer == agentWallet;
 *   }
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * Configuration (application.properties):
 *   eip712.chainId=31337          # Hardhat local
 *   eip712.chainId=11155111       # Sepolia
 *   eip712.chainId=8453           # Base mainnet
 *   contract.router=0xYOUR_ROUTER_ADDRESS
 */
@Slf4j
@Component
public class EIP712Signer {

    // ── EIP-712 type strings — must be identical byte-for-byte to Solidity ──

    private static final String DOMAIN_TYPE =
            "EIP712Domain(string name,string version,uint256 chainId,address verifyingContract)";

    private static final String TRADE_INTENT_TYPE =
            "TradeIntent(uint256 agentId,address tokenIn,address tokenOut," +
                    "uint256 amountIn,uint256 minAmountOut,uint256 deadline,bytes32 riskParams)";

    // ── Pre-computed type hashes (constant — computed once at class load) ────

    private static final byte[] DOMAIN_TYPEHASH       = keccak256(utf8(DOMAIN_TYPE));
    private static final byte[] TRADE_INTENT_TYPEHASH = keccak256(utf8(TRADE_INTENT_TYPE));

    // ── Domain name / version — must match Solidity constructor args ─────────

    private static final byte[] NAME_HASH    = keccak256(utf8("MockRiskRouter"));
    private static final byte[] VERSION_HASH = keccak256(utf8("1"));

    // ── Runtime config ───────────────────────────────────────────────────────

    @Value("${eip712.chainId:31337}")
    private long chainId;

    @Value("${contract.router}")
    private String verifyingContract;

    private final Credentials credentials;

    public EIP712Signer(Credentials credentials) {
        this.credentials = credentials;
    }

    // ════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Signs a TradeIntent using EIP-712 typed data.
     *
     * Returns a 65-byte [r ‖ s ‖ v] array, exactly as expected by
     * Solidity's ECDSA.recover() / ecrecover().
     *
     * Usage in AutonomousTradingOrchestrator:
     *   byte[] signature = eip712Signer.signTradeIntent(intent);
     *   blockchainService.executeTrade(intent, signature);
     */
    public byte[] signTradeIntent(TradeIntent intent) {
        try {
            byte[] domainSeparator = buildDomainSeparator();
            byte[] structHash      = buildStructHash(intent);
            byte[] digest          = buildDigest(domainSeparator, structHash);

            Sign.SignatureData sig = Sign.signMessage(digest, credentials.getEcKeyPair(), false);

            byte[] result = new byte[65];
            System.arraycopy(sig.getR(), 0, result, 0,  32);
            System.arraycopy(sig.getS(), 0, result, 32, 32);
            result[64] = sig.getV()[0];

            log.debug("EIP-712 signed TradeIntent | agentId={} digest=0x{}",
                    intent.getAgentId(),
                    Numeric.toHexString(digest).substring(2, 14));

            return result;

        } catch (Exception e) {
            log.error("EIP-712 signing failed for agentId={}: {}", intent.getAgentId(), e.getMessage());
            throw new RuntimeException("EIP-712 signing error", e);
        }
    }

    /**
     * Returns the hex-encoded signature string (for logging / debugging).
     */
    public String signTradeIntentHex(TradeIntent intent) {
        return Numeric.toHexString(signTradeIntent(intent));
    }

    /**
     * Exposes the domain separator bytes (useful in tests to verify
     * it matches the on-chain DOMAIN_SEPARATOR).
     */
    public byte[] getDomainSeparator() {
        return buildDomainSeparator();
    }

    /**
     * Exposes the full EIP-712 digest without signing (useful for tests
     * that verify ecrecover produces the expected signer address).
     */
    public byte[] buildDigestForIntent(TradeIntent intent) {
        return buildDigest(buildDomainSeparator(), buildStructHash(intent));
    }

    // ════════════════════════════════════════════════════════════════════════
    // PRIVATE: EIP-712 ENCODING
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Domain separator:
     *   keccak256(abi.encode(
     *     DOMAIN_TYPEHASH,
     *     keccak256("MockRiskRouter"),
     *     keccak256("1"),
     *     chainId,             ← EIP-155 replay protection
     *     verifyingContract    ← binds to this specific router contract
     *   ))
     */
    private byte[] buildDomainSeparator() {
        // 5 fields × 32 bytes each = 160 bytes
        ByteBuffer buf = ByteBuffer.allocate(160);
        buf.put(DOMAIN_TYPEHASH);
        buf.put(NAME_HASH);
        buf.put(VERSION_HASH);
        buf.put(uint256(BigInteger.valueOf(chainId)));
        buf.put(address(verifyingContract));
        return keccak256(buf.array());
    }

    /**
     * Struct hash for TradeIntent:
     *   keccak256(abi.encode(
     *     TRADE_INTENT_TYPEHASH,
     *     agentId, tokenIn, tokenOut,
     *     amountIn, minAmountOut, deadline, riskParams
     *   ))
     *
     * Field order MUST match the Solidity struct field order exactly.
     */
    private byte[] buildStructHash(TradeIntent intent) {
        // 8 fields × 32 bytes each = 256 bytes
        ByteBuffer buf = ByteBuffer.allocate(256);
        buf.put(TRADE_INTENT_TYPEHASH);
        buf.put(uint256(intent.getAgentId()));
        buf.put(address(intent.getTokenIn()));
        buf.put(address(intent.getTokenOut()));
        buf.put(uint256(intent.getAmountIn()));
        buf.put(uint256(intent.getMinAmountOut()));
        buf.put(uint256(intent.getDeadline()));
        buf.put(bytes32(intent.getRiskParams()));
        return keccak256(buf.array());
    }

    /**
     * Final EIP-712 digest:
     *   keccak256(abi.encodePacked("\x19\x01", domainSeparator, structHash))
     *
     * The "\x19\x01" prefix prevents this from being a valid Ethereum
     * transaction and prevents replay across different EIP-712 domains.
     */
    private byte[] buildDigest(byte[] domainSeparator, byte[] structHash) {
        ByteBuffer buf = ByteBuffer.allocate(66); // 2 + 32 + 32
        buf.put((byte) 0x19);
        buf.put((byte) 0x01);
        buf.put(domainSeparator);
        buf.put(structHash);
        return keccak256(buf.array());
    }

    // ════════════════════════════════════════════════════════════════════════
    // PRIVATE: ABI ENCODING UTILITIES
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Encodes a BigInteger as a 32-byte big-endian padded value.
     * Equivalent to abi.encode(uint256 value).
     */
    private static byte[] uint256(BigInteger value) {
        if (value == null) value = BigInteger.ZERO;
        byte[] raw = value.toByteArray();
        byte[] padded = new byte[32];
        // toByteArray() may have a leading 0x00 sign byte for positive values
        int srcStart = (raw.length > 32) ? raw.length - 32 : 0;
        int dstStart = 32 - (raw.length - srcStart);
        System.arraycopy(raw, srcStart, padded, dstStart, raw.length - srcStart);
        return padded;
    }

    /**
     * Encodes a hex Ethereum address as a 32-byte left-zero-padded value.
     * Equivalent to abi.encode(address addr) — addresses are 20 bytes,
     * padded to 32 with leading zeros.
     */
    private static byte[] address(String hex) {
        if (hex == null || hex.isBlank()) return new byte[32];
        byte[] addr = Numeric.hexStringToByteArray(
                hex.startsWith("0x") || hex.startsWith("0X") ? hex.substring(2) : hex
        );
        byte[] padded = new byte[32];
        // Address is 20 bytes, right-aligned in 32-byte slot
        int srcLen = Math.min(addr.length, 20);
        System.arraycopy(addr, addr.length - srcLen, padded, 32 - srcLen, srcLen);
        return padded;
    }

    /**
     * Encodes a raw byte array as bytes32.
     * Accepts:
     *   - byte[] (raw bytes, right-padded to 32 if shorter)
     *   - null   (returns 32 zero bytes)
     *
     * Unlike bytes (dynamic), bytes32 is a fixed 32-byte slot in abi.encode.
     */
    private static byte[] bytes32(byte[] raw) {
        if (raw == null) return new byte[32];
        if (raw.length == 32) return Arrays.copyOf(raw, 32);
        byte[] padded = new byte[32];
        System.arraycopy(raw, 0, padded, 0, Math.min(raw.length, 32));
        return padded;
    }

    /**
     * Keccak-256 hash using Bouncy Castle (already in web3j's classpath).
     * Equivalent to Solidity's keccak256().
     */
    static byte[] keccak256(byte[] input) {
        KeccakDigest digest = new KeccakDigest(256);
        digest.update(input, 0, input.length);
        byte[] output = new byte[32];
        digest.doFinal(output, 0);
        return output;
    }

    /** UTF-8 bytes — used for hashing type strings. */
    private static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}