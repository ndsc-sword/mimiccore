package eu.netide.core.caos.execution;

import eu.netide.core.api.IBackendManager;
import eu.netide.core.api.IShimManager;
import eu.netide.core.api.RequestResult;
import eu.netide.core.caos.composition.ExecutionFlowNode;
import eu.netide.core.caos.composition.ExecutionFlowStatus;
import eu.netide.core.caos.composition.ModuleCall;
import eu.netide.lib.netip.*;
import org.projectfloodlight.openflow.protocol.OFType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by timvi on 31.08.2015.
 */
public class ModuleCallNodeExecutor implements IFlowNodeExecutor {

    private final Logger logger = LoggerFactory.getLogger(ModuleCallNodeExecutor.class);
    static int xid = (int) (Math.random() * 3000);


    @Override
    public boolean canExecute(ExecutionFlowNode node) {
        return node instanceof ModuleCall;
    }

    @Override
    public ExecutionFlowStatus execute(ExecutionFlowNode node, ExecutionFlowStatus status, IShimManager shimManager, IBackendManager backendManager) {
        if (!canExecute(node)) throw new UnsupportedOperationException("Cannot execute this type of node!");

        ModuleCall mc = (ModuleCall) node;

        logger.info("module + callCondition + priority + allowVetoResult " + mc.toString());

        logger.info("module.condition toString " + mc.getModule().toString());

        // check for conditions
        if ((mc.getCallCondition() != null && !ConditionEvaluator.evaluate(mc.getCallCondition(), status)
                || (mc.getModule().getCallCondition() != null && !ConditionEvaluator.evaluate(mc.getModule().getCallCondition(), status)))) {
            return status;
        }

        int moduleId = backendManager.getModuleId(mc.getModule().getId());
        if (backendManager.isModuleDead(mc.getModule().getId(), mc.getModule().getDeadTimeOut())) {
            // Module is dead.
            logger.warn("Not sending Request to dead module '" + moduleId + "', assuming empty result set.");
            return ExecutionUtils.mergeMessagesIntoStatus(status, new Message[] {});
        }

        MessageHeader header = new MessageHeader();
        header.setMessageType(MessageType.OPENFLOW);
        header.setTransactionId(status.getCurrentMessage().getHeader().getTransactionId());
        header.setDatapathId(status.getCurrentMessage().getHeader().getDatapathId());

        if (header.getTransactionId()==0)
            header.setTransactionId(++xid);




        header.setModuleId(moduleId);

        logger.info("Sending Request to module '" + moduleId + "'..." + " trasID: " + status.getCurrentMessage().getHeader().getTransactionId());
        Message message = new Message(header, status.getCurrentMessage().getPayload());
        RequestResult result = backendManager.sendRequest(message);
        logger.info("ModuleCallNodeExecutor execute Request returned from module '" + moduleId + "'.");
        logger.info("ModuleCallNodeExecutor execute result num: " + result.getResultMessagesNum());
        for (int i = 0 ; i < result.getResultMessagesNum() ; i++){
            Message tmp = result.getResultMessage().get(i);
            if(tmp.getHeader().getMessageType() == MessageType.OPENFLOW) {
                OpenFlowMessage handleOFMessage = (OpenFlowMessage) NetIPUtils.ConcretizeMessage(tmp);
                if (handleOFMessage.getOfMessage().getType() == OFType.PACKET_OUT)
                    logger.info("ModuleCallNodeExecutor execute PACKET_OUT msg from moduleID : " + moduleId + "tid : " + tmp.getHeader().getTransactionId());
                else if (handleOFMessage.getOfMessage().getType() == OFType.FLOW_MOD)
                    logger.info("ModuleCallNodeExecutor execute FLOW_MOD msg from moduleID: " + moduleId + "tid : " + tmp.getHeader().getTransactionId());
            }
        }
        return ExecutionUtils.mergeMessagesIntoStatus(status, result.getResultMessages().toArray(Message[]::new));
    }
}
