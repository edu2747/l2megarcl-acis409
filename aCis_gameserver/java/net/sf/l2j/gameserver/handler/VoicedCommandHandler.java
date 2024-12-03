package net.sf.l2j.gameserver.handler;

import java.util.HashMap;
import java.util.Map;


import net.sf.l2j.gameserver.handler.voicedcommands.BankingCommand;

public class VoicedCommandHandler {
    private final Map<Integer, IVoicedCommandHandler> _datatable = new HashMap<>();

    public static VoicedCommandHandler getInstance() {
        return SingletonHolder._instance;
    }

    protected VoicedCommandHandler() {

        registerHandler(new BankingCommand());
    }

    public void registerHandler(IVoicedCommandHandler handler) {
        String[] ids = handler.getVoicedCommandList();
        for (String id : ids) {
            _datatable.put(id.hashCode(), handler);
        }
    }

    public IVoicedCommandHandler getHandler(String voicedCommand) {
        String command = voicedCommand.split(" ")[0];
        return _datatable.get(command.hashCode());
    }

    public int size() {
        return _datatable.size();
    }

    private static class SingletonHolder {
        protected static final VoicedCommandHandler _instance = new VoicedCommandHandler();
    }
}
