package eu.netide.core.caos.resolution;

import eu.netide.lib.netip.Message;

import java.util.Map;

/**
 * Created by root on 12/15/15.
 */
public interface IMimicDefender {
    /**
     * param messages to one switch
     * return true if no differences in messages, else false
     */
    Message[] mimicCompare(Message[] messageForOneSwitch, Map<Integer, String> moduleIDToName);
}
