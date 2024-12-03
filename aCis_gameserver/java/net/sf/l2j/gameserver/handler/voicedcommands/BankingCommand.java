package net.sf.l2j.gameserver.handler.voicedcommands;

import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;

public class BankingCommand implements IVoicedCommandHandler {
    private static final String[] COMMANDS = { "bank" };

    @Override
    public boolean useVoicedCommand(String command, Player activeChar, String params) {
        activeChar.sendMessage("Banking command executed!");
        return true;
    }

    @Override
    public String[] getVoicedCommandList() {
        return COMMANDS;
    }
}
