package eu.netide.core.management.cli;

import eu.netide.core.api.IBackendManager;
import eu.netide.core.api.IShimManager;
import jline.console.completer.FileNameCompleter;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

import java.util.function.Consumer;

/**
 * Created by arne on 04.02.16.
 */

@Command(scope = "netide", name = "listModules", description = "List connected/known backend")
public class ListModules extends OsgiCommandSupport {

    @Override
    protected Object doExecute() throws Exception {
        IBackendManager backendManager = getService(IBackendManager.class);
        IShimManager shimManager = getService(IShimManager.class);

        System.out.format("%5s %20s %20s %20s\n", "Id", "Name", "Backend", "Last message (s ago)");
        long now = System.currentTimeMillis();
        backendManager.getModuleIds().forEach(i -> {

                                                  String backendname = backendManager.getBackend(i);
                                                  String moduleName = backendManager.getModuleName(i);
                                                  String lastMessage;
                                                  if (backendManager.getLastMessageTime(i) != null) {
                                                      lastMessage = String.format("%.2f", (now - backendManager.getLastMessageTime(i)) / 1000f);
                                                  } else {
                                                      lastMessage = "-";
                                                  }
                                                  String lastHBMessage;
                                                  System.out.format("%5d %20s %20s %20s\n", i, moduleName, backendname, lastMessage);

                                              }
        );
        String shimLastMessage = "-";
        if (shimManager.getLastMessageTime() != 0) {
            shimLastMessage = String.format("%.2f", (now - shimManager.getLastMessageTime()) / 1000f);
        }
        System.out.format("%5s %20s %20s %20s\n", "-", "shim", "-", shimLastMessage);
        return null;
    }

}
