package net.sf.l2j.gameserver.handler;

import net.sf.l2j.gameserver.model.actor.Player;

public interface IVoicedCommandHandler {
    boolean useVoicedCommand(String command, Player activeChar, String params);
    String[] getVoicedCommandList();
}