package eu.netide.core.caos.resolution;

import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.MessageType;
import eu.netide.lib.netip.NetIPUtils;
import eu.netide.lib.netip.OpenFlowMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;

import java.util.*;

/**
 * Created by root on 12/15/15.
 */
public class DefaultOFMimicDefender implements IMimicDefender{

    private static final Logger log = LoggerFactory.getLogger(DefaultOFMimicDefender.class);

    public Message[] mimicCompare(Message[] messageForOneSwitch, Map<Integer, String> moduleIDToName){

        log.info("into mimicCompare");
        List<Integer> openFlowModAndPacketOutInex = new ArrayList<Integer>();
        for(int i = 0 ; i < messageForOneSwitch.length ; i++)
        {
            if(messageForOneSwitch[i].getHeader().getMessageType() != MessageType.OPENFLOW) {
                log.info("not OPENFLOW TYPE");
                continue;
            }
            OpenFlowMessage handleOFMessage = (OpenFlowMessage) NetIPUtils.ConcretizeMessage(messageForOneSwitch[i]);
            if( (handleOFMessage.getOfMessage().getType() == OFType.FLOW_MOD)
                    || (handleOFMessage.getOfMessage().getType() == OFType.PACKET_OUT) ){
                openFlowModAndPacketOutInex.add(i);
            }
        }


        if(openFlowModAndPacketOutInex.size() == 0){
            log.info("DefaultOFMimicDefender mimicCompare no openflow modification or packetOut message");
            return messageForOneSwitch;
        }

        log.info("all msg numbers: " + messageForOneSwitch.length);
        log.info("modification and packet out sum number: " + openFlowModAndPacketOutInex.size());

        //classify openflow modification message according to their module type
        Map<ModuleType, List<Integer>> msgClassified = classifyMsgByModuleType(messageForOneSwitch,
                openFlowModAndPacketOutInex, moduleIDToName);

        log.info("moduleType number is : " + msgClassified.size());

        for (Map.Entry<ModuleType, List<Integer>> entry : msgClassified.entrySet()) {
           mimicCompareMsg(entry.getValue(), entry.getKey(), messageForOneSwitch);


        }

        return Arrays.stream(messageForOneSwitch).filter(m -> m != null).toArray(Message[]::new);
    }

    private void mimicCompareMsg(List<Integer> msgIndexList, ModuleType mt, Message[] msg){
        /*int firstPacketOutindex = -1;
        int firstModIndex = -1;
        for (int i = 0 ; i < msgIndexList.size() ; i++){
            OpenFlowMessage handleOFMessage = (OpenFlowMessage) NetIPUtils.ConcretizeMessage(msg[msgIndexList.get(i)]);
            if(handleOFMessage.getOfMessage().getType() == OFType.PACKET_OUT){
                if (firstPacketOutindex == -1)
                    firstPacketOutindex = msgIndexList.get(i);
                else
                    msg[msgIndexList.get(i)] = null;
            }
            else if(handleOFMessage.getOfMessage().getType() == OFType.FLOW_MOD){
                if (firstModIndex == -1)
                    firstModIndex = msgIndexList.get(i);
                else
                    msg[msgIndexList.get(i)] = null;
            }
            else
                log.info("mimicCompareMsg error packet type");
        }*/

        int OFPacketOutNum = 0;
        int firstOFPacketOutIndex = -1;
        int firstModMessageIndex = -1;
        int msgIndexListSize = msgIndexList.size();
        for(int i = 0 ; i < msgIndexListSize ; i++) {
            OpenFlowMessage handleOFMessage = (OpenFlowMessage) NetIPUtils.ConcretizeMessage(msg[msgIndexList.get(i)]);
            if(handleOFMessage.getOfMessage().getType() == OFType.PACKET_OUT){
                OFPacketOutNum++;
                if(firstOFPacketOutIndex == -1)
                    firstOFPacketOutIndex = msgIndexList.get(i);
                else {
                    msg[msgIndexList.get(i)] = null;
                }
            }
            if(handleOFMessage.getOfMessage().getType() == OFType.FLOW_MOD){
                if(firstModMessageIndex == -1)
                    firstModMessageIndex = msgIndexList.get(i);
            }
        }

        int OFModNum = msgIndexListSize-OFPacketOutNum;
        int ModMajorNum = (OFModNum/2)+1;

        if (OFModNum==1){
            for (int i = 0 ; i < msgIndexListSize ; i++){
                if(msgIndexList.get(i) == firstModMessageIndex){
                    continue;
                }
                msg[msgIndexList.get(i)] = null;
            }
            return;
        }
        else {
            if (Utils.DEBUG) {
                log.info("OFModNum : " + OFModNum + " msgIndexListSize : " + msgIndexListSize + " OFPacketOutNum : " + OFPacketOutNum);
                log.info("ModMajorNum : " + ModMajorNum);
            }

//        if(ModMajorNum == 1)
//            return;

            Map<Integer, Integer> ModMsgIndexAndMatchNum = new HashMap<>();
            for (int i = 0; i < msgIndexListSize; i++) {
                if (msg[msgIndexList.get(i)] != null && msgIndexList.get(i) != firstOFPacketOutIndex) {
                    ModMsgIndexAndMatchNum.put(msgIndexList.get(i), 0);
                }
            }
            log.info("ModMsgIndexAndMatchNum :" + ModMsgIndexAndMatchNum.size());
            int reserveModMessageIndex = -1;

            for (int i = 0; i < msgIndexListSize; i++) {
                if (msg[msgIndexList.get(i)] != null) {
                    log.info(msg[msgIndexList.get(i)].toString());
                }
            }
            log.info("1flowmod :" + msg[msgIndexList.get(0)].getPayload());
            log.info("2flowmod :" + msg[msgIndexList.get(2)].getPayload());


            for (int i = 0; i < msgIndexListSize; i++) {
                if (msgIndexList.get(i) == firstOFPacketOutIndex) {
                    log.info("1.1");
                    continue;
                } else if (msg[msgIndexList.get(i)] == null) {
                    log.info("1.2");
                    continue;
                }

                for (int j = i + 1; j < msgIndexListSize; j++) {
                    if (msgIndexList.get(j) == firstOFPacketOutIndex) {
                        log.info("2.1");
                        continue;
                    } else if (msg[msgIndexList.get(j)] == null) {
                        log.info("2.2");
                        continue;
                    } else if (msg[msgIndexList.get(i)].getPayload().length != msg[msgIndexList.get(j)].getPayload().length) {
                        log.info("2.3");
                        continue;
                    }
//                else if(0 == compareOFModPayload(msg[msgIndexList.get(i)].getPayload(),
//                        msg[msgIndexList.get(j)].getPayload())){
                    else if (Arrays.equals(msg[msgIndexList.get(i)].getPayload(),
                            msg[msgIndexList.get(j)].getPayload())) {
                        log.info("hasEqual");
                        int replaceIntegeri = ModMsgIndexAndMatchNum.get(msgIndexList.get(i)) + 1;
                        ModMsgIndexAndMatchNum.replace(msgIndexList.get(i), replaceIntegeri);
                        log.info("OutReserveModMessageIndex : " + reserveModMessageIndex);
//                    int replaceIntegerj = ModMsgIndexAndMatchNum.get(msgIndexList.get(j))+1;
//                    ModMsgIndexAndMatchNum.replace(msgIndexList.get(j), replaceIntegerj);
                        if (replaceIntegeri >= ModMajorNum - 1) {
                            reserveModMessageIndex = msgIndexList.get(i);
                            log.info("reserveModMessageIndex : " + reserveModMessageIndex);
                            break;
                        }
                    }
                }
            }

            for (int i = 0; i < msgIndexListSize; i++) {
                if (msgIndexList.get(i) == reserveModMessageIndex) {
                    continue;
                }

                msg[msgIndexList.get(i)] = null;
            }
            return;
        }
    }

    private Map<ModuleType, List<Integer>> classifyMsgByModuleType(Message[] msg, List<Integer> ofModPacketOutIndex,
                                                                   Map<Integer, String> moduleIDToName){
        Map<ModuleType, List<Integer>> MsgPerType = new HashMap<>();
        int ofModPacketOutIndexSize = ofModPacketOutIndex.size();
        for(int i = 0 ; i < ofModPacketOutIndexSize ; i++)
        {
            ModuleType mt = getModuleTypeByID(msg[ofModPacketOutIndex.get(i)].getHeader().getModuleId(), moduleIDToName);
            List<Integer> typeMsg = MsgPerType.get(mt);
            if(typeMsg == null){
                typeMsg = new ArrayList<>();
                typeMsg.add(ofModPacketOutIndex.get(i));
                MsgPerType.put(mt, typeMsg);
            }
            else{
                typeMsg.add(ofModPacketOutIndex.get(i));
                MsgPerType.replace(mt, typeMsg);
            }
        }
        return MsgPerType;
    }

    private ModuleType getModuleTypeByID(int moduleID, Map<Integer, String> moduleIDToName){
        return ModuleType.SIMPLE_SWITCH;
    }

    /**
     * param messages to compare
     * return 0 if no differences in messages, else false
     */
    private int compareOFModPayload(byte[] message1, byte[] message2){
        return message1.toString().compareTo(message2.toString());
    }
}
