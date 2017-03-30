package eu.netide.core.caos.resolution;

import eu.netide.lib.netip.MessageType;

/**
 * Created by root on 12/15/15.
 */
public class MimicDefenders {

    public static final IMimicDefender DEFAULT_OF_MIMICDEFENDER = new DefaultOFMimicDefender();

    private static final IMimicDefender[] allDefenders = new IMimicDefender[]{DEFAULT_OF_MIMICDEFENDER};

    public static IMimicDefender getDefaultOfMimicdefender(MessageType messageType){
        switch (messageType){
            case OPENFLOW:
                return DEFAULT_OF_MIMICDEFENDER;
            default:
                throw new UnsupportedOperationException("No default mimic defender known for MessageType '" + messageType.name());
        }
    }
}
