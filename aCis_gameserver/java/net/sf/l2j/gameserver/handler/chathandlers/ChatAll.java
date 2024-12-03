package net.sf.l2j.gameserver.handler.chathandlers;

import net.sf.l2j.gameserver.enums.FloodProtector;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.handler.VoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;

import java.util.StringTokenizer;

public class ChatAll implements IChatHandler
{
    private static final SayType[] COMMAND_IDS =
    {
        SayType.ALL
    };

    @Override
    public void handleChat(SayType type, Player player, String target, String text)
    {

        if (!player.getClient().performAction(FloodProtector.GLOBAL_CHAT))
            return;


        if (text.startsWith("."))
        {

            StringTokenizer st = new StringTokenizer(text);
            

            if (!st.hasMoreTokens())
            {
                player.sendMessage("You must specify a command after the dot.");
                return;
            }
            
            String command = st.nextToken().substring(1);
            String params = st.hasMoreTokens() ? text.substring(command.length() + 2) : null;


            IVoicedCommandHandler vch = VoicedCommandHandler.getInstance().getHandler(command);
            if (vch != null)
            {

                vch.useVoicedCommand(command, player, params);
                return;
            }


            player.sendMessage("The voiced command '" + command + "' does not exist.");
            return;
        }


        final CreatureSay cs = new CreatureSay(player, type, text);
        player.sendPacket(cs);


        player.forEachKnownTypeInRadius(Player.class, 1250, p -> p.sendPacket(cs));
    }

    @Override
    public SayType[] getChatTypeList()
    {
        return COMMAND_IDS;
    }
}
