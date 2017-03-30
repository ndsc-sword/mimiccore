package eu.netide.core.caos.execution;

import eu.netide.core.api.IBackendManager;
import eu.netide.core.api.IShimManager;
import eu.netide.core.caos.composition.ExecutionFlowNode;
import eu.netide.core.caos.composition.ExecutionFlowStatus;
import eu.netide.core.caos.composition.ModuleCall;
import eu.netide.core.caos.composition.ParallelCall;
import eu.netide.core.caos.resolution.*;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Executor for ParallelCall nodes.
 * <p>
 * Created by timvi on 31.08.2015.
 */
public class ParallelCallNodeExecutor implements IFlowNodeExecutor {

    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final Logger log = LoggerFactory.getLogger(ParallelCall.class);

    @Override
    public boolean canExecute(ExecutionFlowNode node) {
        return node instanceof ParallelCall;
    }

    @Override
    public ExecutionFlowStatus execute(ExecutionFlowNode node, ExecutionFlowStatus status, IShimManager shimManager, IBackendManager backendManager) {
        if (!canExecute(node)) throw new UnsupportedOperationException("Cannot execute this type of node!");

        ParallelCall pc = (ParallelCall) node;

        log.info("ParallelCallNodeExecutor.execute()");

        List<Future<ExecutionFlowStatus>> futures = new ArrayList<>();
        for (ModuleCall mc : pc.getModuleCalls()) {
            futures.add(pool.submit(() -> FlowNodeExecutors.getExecutor(mc).execute(mc, new ExecutionFlowStatus(status.getCurrentMessage()), shimManager, backendManager)));
        }

        Message[] results = futures.stream().map(f -> {
            try {
                return f.get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("Unable to get result of ModuleCall", e);
                throw new RuntimeException("Unable to get result of ModuleCall", e);
            }
        }).flatMap(executionFlowStatus -> executionFlowStatus.getResultMessages().values().stream().flatMap(Collection::stream)).toArray(Message[]::new);

        log.info("results message after thread wait: " + results.length);

        // Resolve conflicts per datapath
        List<Message> allResolvedMessages = new ArrayList<>();

        Map<Long, List<Message>> messagesByDatapath = Arrays.stream(results).collect(Collectors.groupingBy(m -> m.getHeader().getDatapathId()));
        for (Long datapathId : messagesByDatapath.keySet()) {
            Message[] currentMessages = messagesByDatapath.get(datapathId).stream().toArray(Message[]::new);

            //added by Yuxing Mao for mimic defense; 2016-12-16
            IMimicDefender mimicDefender = MimicDefenders.getDefaultOfMimicdefender(MessageType.OPENFLOW);
            Message[] messagesAfterMimicDefense = mimicDefender.mimicCompare(currentMessages, backendManager.getModuleToNameMappings());

            log.info("messagesAfterMimicDefense : " + messagesAfterMimicDefense.length);
            IConflictResolver resolver = ConflictResolvers.getMatchingResolver(messagesAfterMimicDefense);
            Message[] resolvedMessages = resolver.resolve(messagesAfterMimicDefense, pc.getResolutionPolicy(), PriorityInfo.fromModuleCalls(pc.getModuleCalls(), backendManager)).getResultingMessagesToSend();
            Arrays.stream(resolvedMessages).forEach(allResolvedMessages::add);
        }

        // merge results per datapath
        return ExecutionUtils.mergeMessagesIntoStatus(status, allResolvedMessages.stream().toArray(Message[]::new));
    }
}
