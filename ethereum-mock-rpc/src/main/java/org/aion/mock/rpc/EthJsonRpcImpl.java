/*
 * Copyright 2015, 2016 Ether.Camp Inc. (US)
 * This file is part of Ethereum Harmony.
 *
 * Ethereum Harmony is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Ethereum Harmony is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ethereum Harmony.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.aion.mock.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.NonNull;
import org.apache.commons.collections4.map.LRUMap;
import org.ethereum.core.*;
import org.ethereum.crypto.ECKey;
import org.ethereum.crypto.HashUtil;
import org.ethereum.db.BlockStore;
import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.mine.MinerIfc;
import org.ethereum.mine.MinerListener;
import org.ethereum.solidity.compiler.CompilationResult;
import org.ethereum.util.BuildInfo;
import org.ethereum.util.ByteUtil;
import org.ethereum.vm.DataWord;

import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.aion.mock.rpc.TypeConverter.*;
import static org.ethereum.util.ByteUtil.EMPTY_BYTE_ARRAY;
import static org.ethereum.util.ByteUtil.bigIntegerToBytes;

/**
 * @author Anton Nashatyrev
 */
public class EthJsonRpcImpl implements JsonRpc {

    private static final String BLOCK_LATEST = "latest";

    public class BinaryCallArguments {
        public long nonce;
        public long gasPrice;
        public long gasLimit;
        public String toAddress;
        public String fromAddress;
        public long value;
        public byte[] data;
        public void setArguments(CallArguments args) throws Exception {
            nonce = 0;
            if (args.nonce != null && args.nonce.length() != 0)
                nonce = jsonHexToLong(args.nonce);

            gasPrice = 0;
            if (args.gasPrice != null && args.gasPrice.length()!=0)
                gasPrice = jsonHexToLong(args.gasPrice);

            gasLimit = 4_000_000;
            if (args.gas != null && args.gas.length()!=0)
                gasLimit = jsonHexToLong(args.gas);

            toAddress = null;
            if (args.to != null && !args.to.isEmpty())
                toAddress = jsonHexToHex(args.to);

            fromAddress = null;
            if (args.from != null && !args.from.isEmpty())
                fromAddress = jsonHexToHex(args.from);

            value=0;
            if (args.value != null && args.value.length()!=0)
                value = jsonHexToLong(args.value);

            data = null;

            if (args.data != null && args.data.length()!=0)
                data = hexToByteArray(args.data);
        }
    }
    /**
     * State fields
     */
    protected volatile long initialBlockNumber;

    AtomicInteger filterCounter = new AtomicInteger(1);
    Map<Integer, Filter> installedFilters = new Hashtable<>();
    Map<ByteArrayWrapper, TransactionReceipt> pendingReceipts = Collections.synchronizedMap(new LRUMap<>(1024));

    Map<ByteArrayWrapper, Block> miningBlocks = new ConcurrentHashMap<>();

    volatile Block miningBlock;

    volatile SettableFuture<MinerIfc.MiningResult> miningTask;

    final MinerIfc externalMiner = new MinerIfc() {
        @Override
        public ListenableFuture<MiningResult> mine(Block block) {
            miningBlock = block;
            miningTask = SettableFuture.create();
            return miningTask;
        }

        @Override
        public boolean validate(BlockHeader blockHeader) {
            return false;
        }

        @Override
        public void setListeners(Collection<MinerListener> listeners) {}
    };

    boolean minerInitialized = false;

    private long jsonHexToLong(String x) throws Exception {
        if (!x.startsWith("0x"))
            throw new Exception("Incorrect hex syntax");
        x = x.substring(2);
        return Long.parseLong(x, 16);
    }

    private int jsonHexToInt(String x) throws Exception {
        if (!x.startsWith("0x"))
            throw new Exception("Incorrect hex syntax");
        x = x.substring(2);
        return Integer.parseInt(x, 16);
    }

    private String jsonHexToHex(String x) {
        if (!x.startsWith("0x"))
            throw new RuntimeException("Incorrect hex syntax");
        x = x.substring(2);
        return x;
    }

    private Block getBlockByJSonHash(String blockHash) throws Exception {
        byte[] bhash = hexToByteArray(blockHash);
        return worldManager.getBlockchain().getBlockByHash(bhash);
    }

    private Block getByJsonBlockId(String id) {
        if ("earliest".equalsIgnoreCase(id)) {
            return blockchain.getBlockByNumber(0);
        } else if ("latest".equalsIgnoreCase(id)) {
            return blockchain.getBestBlock();
        } else if ("pending".equalsIgnoreCase(id)) {
            return null;
        } else {
            long blockNumber = hexToBigInteger(id).longValue();
            return blockchain.getBlockByNumber(blockNumber);
        }
    }

    private Repository getRepoByJsonBlockId(String id) {
        if ("pending".equalsIgnoreCase(id)) {
            return pendingState.getRepository();
        } else {
            Block block = getByJsonBlockId(id);
            return this.repository.getSnapshotTo(block.getStateRoot());
        }
    }

    private List<Transaction> getTransactionsByJsonBlockId(String id) {
        if ("pending".equalsIgnoreCase(id)) {
            return pendingState.getPendingTransactions();
        } else {
            Block block = getByJsonBlockId(id);
            return block != null ? block.getTransactionsList() : null;
        }
    }

    public String web3_clientVersion() {
        return "aion_eth_mock";
    }

    public String web3_sha3(String data) throws Exception {
        byte[] result = HashUtil.sha3(hexToByteArray(data));
        return toJsonHex(result);
    }

    /**
     * Returns the current network id.
     */
    public String net_version() {
        return String.valueOf(63);
    }

    public String net_peerCount(){
        return toJsonHex(0);
    }

    public boolean net_listening() {
        return false;
    }

    public String eth_protocolVersion(){
        return "63";
    }

    public Object eth_syncing() {
        return false;
    }

    public String eth_coinbase() {
        throw new toJsonHex(EMPTY_BYTE_ARRAY);
    }

    public boolean eth_mining() {
        return false;
    }

    public String eth_hashrate() {
        throw new UnsupportedOperationException();
    }

    public String eth_gasPrice(){
        throw new UnsupportedOperationException();
    }

    public String[] eth_accounts() {
        return personal_listAccounts();
    }

    public String eth_blockNumber() {
        return toJsonHex(blockchain.getBestBlock().getNumber());
    }

    public String eth_getBalance(String address, String blockId) throws Exception {
        Objects.requireNonNull(address, "address is required");
        blockId = blockId == null ? BLOCK_LATEST : blockId;

        byte[] addressAsByteArray = hexToByteArray(address);
        BigInteger balance = getRepoByJsonBlockId(blockId).getBalance(addressAsByteArray);
        return toJsonHex(balance);
    }

    public String eth_getLastBalance(String address) throws Exception {
        return eth_getBalance(address, BLOCK_LATEST);
    }

    @Override
    public String eth_getStorageAt(String address, String storageIdx, String blockId) throws Exception {
        byte[] addressAsByteArray = hexToByteArray(address);
        DataWord storageValue = getRepoByJsonBlockId(blockId).
                getStorageValue(addressAsByteArray, new DataWord(hexToByteArray(storageIdx)));
        return storageValue != null ? toJsonHex(storageValue.getData()) : null;
    }

    @Override
    public String eth_getTransactionCount(String address, String blockId) throws Exception {
        byte[] addressAsByteArray = hexToByteArray(address);
        BigInteger nonce = getRepoByJsonBlockId(blockId).getNonce(addressAsByteArray);
        return toJsonHex(nonce);
    }

    public String eth_getBlockTransactionCountByHash(String blockHash) throws Exception {
        Block b = getBlockByJSonHash(blockHash);
        if (b == null) return null;
        long n = b.getTransactionsList().size();
        return toJsonHex(n);
    }

    public String eth_getBlockTransactionCountByNumber(String bnOrId) throws Exception {
        List<Transaction> list = getTransactionsByJsonBlockId(bnOrId);
        if (list == null) return null;
        long n = list.size();
        return toJsonHex(n);
    }

    public String eth_getUncleCountByBlockHash(String blockHash) throws Exception {
        Block b = getBlockByJSonHash(blockHash);
        if (b == null) return null;
        long n = b.getUncleList().size();
        return toJsonHex(n);
    }

    public String eth_getUncleCountByBlockNumber(String bnOrId) throws Exception {
        Block b = getByJsonBlockId(bnOrId);
        if (b == null) return null;
        long n = b.getUncleList().size();
        return toJsonHex(n);
    }

    public String eth_getCode(String address, String blockId) throws Exception {
        byte[] addressAsByteArray = hexToByteArray(address);
        byte[] code = getRepoByJsonBlockId(blockId).getCode(addressAsByteArray);
        return toJsonHex(code);
    }

    /**
     * Sign message hash with key to produce Elliptic Curve Digital Signature (ECDSA) signature.
     *
     * The sign method calculates an Ethereum specific signature with:
     * sign(keccak256("\x19Ethereum Signed Message:\n" + len(message) + message))).
     *
     * @param address - address to sign. Account must be unlocked
     * @param msg - message
     * @return ECDSA signature (in hex)
     * @throws Exception
     */
    public String eth_sign(String address, String msg) throws Exception {
        throw new UnsupportedOperationException();
    }

    private byte[] toByteArray(ECKey.ECDSASignature signature) {
        return ByteUtil.merge(
                bigIntegerToBytes(signature.r),
                bigIntegerToBytes(signature.s),
                new byte[] {signature.v});
    }

    protected TransactionReceipt createCallTxAndExecute(CallArguments args, Block block) throws Exception {
        throw new UnsupportedOperationException();
    }

    protected TransactionReceipt createCallTxAndExecute(CallArguments args, Block block, Repository repository, BlockStore blockStore) throws Exception {
        throw new UnsupportedOperationException();
    }

    public String eth_call(CallArguments args, String bnOrId) throws Exception {
        throw new UnsupportedOperationException();
    }

    public String eth_estimateGas(CallArguments args) throws Exception {
        throw new UnsupportedOperationException();
    }


    protected BlockResult getBlockResult(Block block, boolean fullTx) {
        if (block==null)
            return null;
        boolean isPending = ByteUtil.byteArrayToLong(block.getNonce()) == 0;
        BlockResult br = new BlockResult();
        br.number = isPending ? null : toJsonHex(block.getNumber());
        br.hash = isPending ? null : toJsonHex(block.getHash());
        br.parentHash = toJsonHex(block.getParentHash());
        br.nonce = isPending ? null : toJsonHex(block.getNonce());
        br.sha3Uncles= toJsonHex(block.getUnclesHash());
        br.logsBloom = isPending ? null : toJsonHex(block.getLogBloom());
        br.transactionsRoot = toJsonHex(block.getTxTrieRoot());
        br.stateRoot = toJsonHex(block.getStateRoot());
        br.receiptsRoot = toJsonHex(block.getReceiptsRoot());
        br.miner = isPending ? null : toJsonHex(block.getCoinbase());
        br.difficulty = toJsonHex(block.getDifficultyBI());
        br.totalDifficulty = toJsonHex(blockStore.getTotalDifficultyForHash(block.getHash()));
        if (block.getExtraData() != null)
            br.extraData = toJsonHex(block.getExtraData());
        br.size = toJsonHex(block.getEncoded().length);
        br.gasLimit = toJsonHex(block.getGasLimit());
        br.gasUsed = toJsonHex(block.getGasUsed());
        br.timestamp = toJsonHex(block.getTimestamp());

        List<Object> txes = new ArrayList<>();
        if (fullTx) {
            for (int i = 0; i < block.getTransactionsList().size(); i++) {
                txes.add(new TransactionResultDTO(block, i, block.getTransactionsList().get(i)));
            }
        } else {
            for (Transaction tx : block.getTransactionsList()) {
                txes.add(toJsonHex(tx.getHash()));
            }
        }
        br.transactions = txes.toArray();

        List<String> ul = new ArrayList<>();
        for (BlockHeader header : block.getUncleList()) {
            ul.add(toJsonHex(header.getHash()));
        }
        br.uncles = ul.toArray(new String[ul.size()]);

        return br;
    }

    public BlockResult eth_getBlockByHash(String blockHash, Boolean fullTransactionObjects) throws Exception {
        final Block b = getBlockByJSonHash(blockHash);
        return getBlockResult(b, fullTransactionObjects);
    }

    public BlockResult eth_getBlockByNumber(String bnOrId, Boolean fullTransactionObjects) throws Exception {
        final Block b;
        if ("pending".equalsIgnoreCase(bnOrId)) {
            b = blockchain.createNewBlock(blockchain.getBestBlock(), pendingState.getPendingTransactions(), Collections.<BlockHeader>emptyList());
        } else {
            b = getByJsonBlockId(bnOrId);
        }
        return (b == null ? null : getBlockResult(b, fullTransactionObjects));
    }

    public TransactionResultDTO eth_getTransactionByHash(String transactionHash) throws Exception {
        final byte[] txHash = hexToByteArray(transactionHash);

        final TransactionInfo txInfo = blockchain.getTransactionInfo(txHash);
        if (txInfo == null) {
            return null;
        }

        final Block block = blockchain.getBlockByHash(txInfo.getBlockHash());
        // need to return txes only from main chain
        final Block mainBlock = blockchain.getBlockByNumber(block.getNumber());
        if (!Arrays.equals(block.getHash(), mainBlock.getHash())) {
            return null;
        }
        txInfo.setTransaction(block.getTransactionsList().get(txInfo.getIndex()));

        return new TransactionResultDTO(block, txInfo.getIndex(), txInfo.getReceipt().getTransaction());
    }

    public TransactionResultDTO eth_getTransactionByBlockHashAndIndex(String blockHash, String index) throws Exception {
        Block b = getBlockByJSonHash(blockHash);
        if (b == null) return null;
        int idx = jsonHexToInt(index);
        if (idx >= b.getTransactionsList().size()) return null;
        Transaction tx = b.getTransactionsList().get(idx);
        return new TransactionResultDTO(b, idx, tx);
    }

    public TransactionResultDTO eth_getTransactionByBlockNumberAndIndex(String bnOrId, String index) throws Exception {
        Block b = getByJsonBlockId(bnOrId);
        List<Transaction> txs = getTransactionsByJsonBlockId(bnOrId);
        if (txs == null) return null;
        int idx = jsonHexToInt(index);
        if (idx >= txs.size()) return null;
        Transaction tx = txs.get(idx);
        return new TransactionResultDTO(b, idx, tx);
    }

    public TransactionReceiptDTO eth_getTransactionReceipt(String transactionHash) throws Exception {
        final byte[] hash = hexToByteArray(transactionHash);

        final TransactionInfo txInfo = blockchain.getTransactionInfo(hash);

        if (txInfo == null)
            return null;

        final Block block = blockchain.getBlockByHash(txInfo.getBlockHash());
        final Block mainBlock = blockchain.getBlockByNumber(block.getNumber());

        // need to return txes only from main chain
        if (!Arrays.equals(block.getHash(), mainBlock.getHash())) {
            return null;
        }

        return new TransactionReceiptDTO(block, txInfo);
    }

    @Override
    public TransactionReceiptDTOExt ethj_getTransactionReceipt(String transactionHash) throws Exception {
        byte[] hash = hexToByteArray(transactionHash);

        TransactionReceipt pendingReceipt = pendingReceipts.get(new ByteArrayWrapper(hash));

        TransactionInfo txInfo;
        Block block;

        if (pendingReceipt != null) {
            txInfo = new TransactionInfo(pendingReceipt);
            block = null;
        } else {
            txInfo = blockchain.getTransactionInfo(hash);

            if (txInfo == null)
                return null;

            block = blockchain.getBlockByHash(txInfo.getBlockHash());

            // need to return txes only from main chain
            Block mainBlock = blockchain.getBlockByNumber(block.getNumber());
            if (!Arrays.equals(block.getHash(), mainBlock.getHash())) {
                return null;
            }
        }

        return new TransactionReceiptDTOExt(block, txInfo);
    }

    @Override
    public BlockResult eth_getUncleByBlockHashAndIndex(String blockHash, String uncleIdx) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public BlockResult eth_getUncleByBlockNumberAndIndex(String blockId, String uncleIdx) throws Exception {
        Block block = getByJsonBlockId(blockId);
        return block == null ? null :
                eth_getUncleByBlockHashAndIndex(toJsonHex(block.getHash()), uncleIdx);
    }

    @Override
    public String[] eth_getCompilers() {
        return new String[] {"solidity"};
    }

    @Override
    public CompilationResult eth_compileSolidity(String contract) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompilationResult eth_compileLLL(String contract) {
        throw new UnsupportedOperationException("LLL compiler not supported");
    }

    @Override
    public CompilationResult eth_compileSerpent(String contract){
        throw new UnsupportedOperationException("Serpent compiler not supported");
    }
//
//    @Override
//    public String eth_resend() {
//        throw new UnsupportedOperationException("JSON RPC method eth_resend not implemented yet");
//    }
//
//    @Override
//    public String eth_pendingTransactions() {
//        throw new UnsupportedOperationException("JSON RPC method eth_pendingTransactions not implemented yet");
//    }

    static class Filter {
        static final int MAX_EVENT_COUNT = 1024; // prevent OOM when Filers are forgotten
        private int pollStart = 0;
        static abstract class FilterEvent {
            public abstract Object getJsonEventObject();
        }
        List<FilterEvent> events = new LinkedList<>();

        public synchronized boolean hasNew() { return !events.isEmpty();}

        public synchronized Object[] poll() {
            Object[] ret = new Object[events.size() - pollStart];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = events.get(i + pollStart).getJsonEventObject();
            }
            pollStart += ret.length;
            return ret;
        }

        public synchronized Object[] getAll() {
            Object[] ret = new Object[events.size()];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = events.get(i).getJsonEventObject();
            }
            return ret;
        }

        protected synchronized void add(FilterEvent evt) {
            events.add(evt);
            if (events.size() > MAX_EVENT_COUNT) {
                events.remove(0);
                if (pollStart > 0) {
                    --pollStart;
                }
            }
        }

        public void newBlockReceived(Block b) {}
        public void newPendingTx(Transaction tx) {}
        public void updatePendingTx(TransactionReceipt txReceipt) {}
    }

    static class NewBlockFilter extends Filter {
        class NewBlockFilterEvent extends FilterEvent {
            private final String blockHash;
            NewBlockFilterEvent(Block b) {this.blockHash = toJsonHex(b.getHash());}

            @Override
            public String getJsonEventObject() {
                return blockHash;
            }
        }

        public void newBlockReceived(Block b) {
            add(new NewBlockFilterEvent(b));
        }
    }

    static class PendingTransactionFilter extends Filter {
        class PendingTransactionFilterEvent extends FilterEvent {
            private final String txHash;

            PendingTransactionFilterEvent(Transaction tx) {this.txHash = toJsonHex(tx.getHash());}

            @Override
            public String getJsonEventObject() {
                return txHash;
            }
        }

        public void newPendingTx(Transaction tx) {
            add(new PendingTransactionFilterEvent(tx));
        }

        @Override
        public void updatePendingTx(TransactionReceipt txReceipt) {}
    }

    @Override
    public String eth_newFilter(FilterRequest fr) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public String eth_newBlockFilter() {
        int id = filterCounter.getAndIncrement();
        installedFilters.put(id, new NewBlockFilter());
        return toJsonHex(id);
    }

    @Override
    public String eth_newPendingTransactionFilter() {
        int id = filterCounter.getAndIncrement();
        installedFilters.put(id, new PendingTransactionFilter());
        return toJsonHex(id);
    }

    @Override
    public boolean eth_uninstallFilter(String id) {
        if (id == null) return false;
        return installedFilters.remove(hexToBigInteger(id).intValue()) != null;
    }

    @Override
    public Object[] eth_getFilterChanges(String id) {
        Filter filter = installedFilters.get(hexToBigInteger(id).intValue());
        if (filter == null) return null;
        return filter.poll();
    }

    @Override
    public Object[] eth_getFilterLogs(String id) {
        Filter filter = installedFilters.get(hexToBigInteger(id).intValue());
        if (filter == null) return null;
        return filter.getAll();
    }

    @Override
    public Object[] eth_getLogs(FilterRequest filterRequest) throws Exception {
        log.debug("eth_getLogs ...");
        String id = eth_newFilter(filterRequest);
        Object[] ret = eth_getFilterChanges(id);
        eth_uninstallFilter(id);
        return ret;
    }

    @Override
    public List<Object> eth_getWork() {
        throw new UnsupportedOperationException();
    }


    @Override
    public boolean eth_submitWork(String nonceHex, String headerHex, String digestHex) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean eth_submitHashrate(String hashrate, String id) {
        // EthereumJ doesn't support changing of miner's hashrate
        return false;
    }

    @Override
    public String shh_version() {
        throw new UnsupportedOperationException();
    }

    /**
     * TODO
     */
//    @Override
//    public String shh_post() {
//        throw new UnsupportedOperationException("JSON RPC method shh_post not implemented yet");
//    }
//
//    @Override
//    public String shh_newIdentity() {
//        throw new UnsupportedOperationException("JSON RPC method shh_newIdentity not implemented yet");
//    }
//
//    @Override
//    public String shh_hasIdentity() {
//        throw new UnsupportedOperationException("JSON RPC method shh_hasIdentity not implemented yet");
//    }
//
//    @Override
//    public String shh_newGroup() {
//        throw new UnsupportedOperationException("JSON RPC method shh_newGroup not implemented yet");
//    }
//
//    @Override
//    public String shh_addToGroup() {
//        throw new UnsupportedOperationException("JSON RPC method shh_addToGroup not implemented yet");
//    }
//
//    @Override
//    public String shh_newFilter() {
//        throw new UnsupportedOperationException("JSON RPC method shh_newFilter not implemented yet");
//    }
//
//    @Override
//    public String shh_uninstallFilter() {
//        throw new UnsupportedOperationException("JSON RPC method shh_uninstallFilter not implemented yet");
//    }
//
//    @Override
//    public String shh_getFilterChanges() {
//        throw new UnsupportedOperationException("JSON RPC method shh_getFilterChanges not implemented yet");
//    }
//
//    @Override
//    public String shh_getMessages() {
//        throw new UnsupportedOperationException("JSON RPC method shh_getMessages not implemented yet");
//    }
//
    @Override
    public boolean admin_addPeer(String enodeUrl) {
        throw new UnsupportedOperationException();
    }
//
//    @Override
//    public String admin_exportChain() {
//        throw new UnsupportedOperationException("JSON RPC method admin_exportChain not implemented yet");
//    }
//
//    @Override
//    public String admin_importChain() {
//        throw new UnsupportedOperationException("JSON RPC method admin_importChain not implemented yet");
//    }
//
//    @Override
//    public String admin_sleepBlocks() {
//        throw new UnsupportedOperationException("JSON RPC method admin_sleepBlocks not implemented yet");
//    }
//
//    @Override
//    public String admin_verbosity() {
//        throw new UnsupportedOperationException("JSON RPC method admin_verbosity not implemented yet");
//    }
//
//    @Override
//    public String admin_setSolc() {
//        throw new UnsupportedOperationException("JSON RPC method admin_setSolc not implemented yet");
//    }
//
//    @Override
//    public String admin_startRPC() {
//        throw new UnsupportedOperationException("JSON RPC method admin_startRPC not implemented yet");
//    }
//
//    @Override
//    public String admin_stopRPC() {
//        throw new UnsupportedOperationException("JSON RPC method admin_stopRPC not implemented yet");
//    }
//
//    @Override
//    public String admin_setGlobalRegistrar() {
//        throw new UnsupportedOperationException("JSON RPC method admin_setGlobalRegistrar not implemented yet");
//    }
//
//    @Override
//    public String admin_setHashReg() {
//        throw new UnsupportedOperationException("JSON RPC method admin_setHashReg not implemented yet");
//    }
//
//    @Override
//    public String admin_setUrlHint() {
//        throw new UnsupportedOperationException("JSON RPC method admin_setUrlHint not implemented yet");
//    }
//
//    @Override
//    public String admin_saveInfo() {
//        throw new UnsupportedOperationException("JSON RPC method admin_saveInfo not implemented yet");
//    }
//
//    @Override
//    public String admin_register() {
//        throw new UnsupportedOperationException("JSON RPC method admin_register not implemented yet");
//    }
//
//    @Override
//    public String admin_registerUrl() {
//        throw new UnsupportedOperationException("JSON RPC method admin_registerUrl not implemented yet");
//    }
//
//    @Override
//    public String admin_startNatSpec() {
//        throw new UnsupportedOperationException("JSON RPC method admin_startNatSpec not implemented yet");
//    }
//
//    @Override
//    public String admin_stopNatSpec() {
//        throw new UnsupportedOperationException("JSON RPC method admin_stopNatSpec not implemented yet");
//    }
//
//    @Override
//    public String admin_getContractInfo() {
//        throw new UnsupportedOperationException("JSON RPC method admin_getContractInfo not implemented yet");
//    }
//
//    @Override
//    public String admin_httpGet() {
//        throw new UnsupportedOperationException("JSON RPC method admin_httpGet not implemented yet");
//    }
//
    @Override
    public Map<String, ?> admin_nodeInfo() throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Map<String, ?>> admin_peers() {
        throw new UnsupportedOperationException();
    }
//
//    @Override
//    public String admin_datadir() {
//        throw new UnsupportedOperationException("JSON RPC method admin_datadir not implemented yet");
//    }
//
//    @Override
//    public String net_addPeer() {
//        throw new UnsupportedOperationException("JSON RPC method net_addPeer not implemented yet");
//    }

    @Override
    public boolean miner_start() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean miner_stop() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean miner_setEtherbase(String coinBase) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean miner_setExtra(String data) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean miner_setGasPrice(String newMinGasPrice) {
        throw new UnsupportedOperationException();
    }

//    @Override
//    public boolean miner_startAutoDAG() {
//        return false;
//    }
//
//    @Override
//    public boolean miner_stopAutoDAG() {
//        return false;
//    }
//
//    @Override
//    public boolean miner_makeDAG() {
//        return false;
//    }
//
//    @Override
//    public String miner_hashrate() {
//        return "0x01";
//    }

//    @Override
//    public String debug_printBlock() {
//        throw new UnsupportedOperationException("JSON RPC method debug_printBlock not implemented yet");
//    }
//
//    @Override
//    public String debug_getBlockRlp() {
//        throw new UnsupportedOperationException("JSON RPC method debug_getBlockRlp not implemented yet");
//    }
//
//    @Override
//    public String debug_setHead() {
//        throw new UnsupportedOperationException("JSON RPC method debug_setHead not implemented yet");
//    }
//
//    @Override
//    public String debug_processBlock() {
//        throw new UnsupportedOperationException("JSON RPC method debug_processBlock not implemented yet");
//    }

//    @Override
//    public String debug_seedHash() {
//        throw new UnsupportedOperationException("JSON RPC method debug_seedHash not implemented yet");
//    }
//
//    @Override
//    public String debug_dumpBlock() {
//        throw new UnsupportedOperationException("JSON RPC method debug_dumpBlock not implemented yet");
//    }
//
//    @Override
//    public String debug_metrics() {
//        throw new UnsupportedOperationException("JSON RPC method debug_metrics not implemented yet");
//    }

    @Override
    public String personal_newAccount(@NonNull String password) {
        throw new UnsupportedOperationException();
    }

    public String personal_importRawKey(String keydata, String passphrase) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean personal_unlockAccount(String address, String password, String duration) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean personal_lockAccount(String address) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] personal_listAccounts() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String personal_signAndSendTransaction(CallArguments tx, String password) {
        throw new UnsupportedOperationException();
    }

    /**
     * List method names for client side terminal competition.
     * @return array in format: `["methodName arg1 arg2", "methodName2"]`
     */
    @Override
    public String[] ethj_listAvailableMethods() {
        final Set<String> ignore = Arrays.asList(Object.class.getMethods()).stream()
                .map(method -> method.getName())
                .collect(Collectors.toSet());

        return Arrays.asList(EthJsonRpcImpl.class.getMethods()).stream()
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> !ignore.contains(method.getName()))
                .map(method -> {
                     List<String> params = Arrays.asList(method.getParameters())
                            .stream()
                            .map(parameter ->
                                    parameter.isNamePresent() ? parameter.getName() : parameter.getType().getSimpleName())
                            .collect(Collectors.toList());
                    params.add(0, method.getName());
                    return params.stream().collect(Collectors.joining(" "));
                })
                .sorted(String::compareTo)
                .toArray(size -> new String[size]);
    }
}
