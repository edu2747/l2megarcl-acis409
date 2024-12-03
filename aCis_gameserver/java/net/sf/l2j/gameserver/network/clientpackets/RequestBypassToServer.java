package net.sf.l2j.gameserver.network.clientpackets;

import java.util.StringTokenizer;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.communitybbs.CommunityBoard;
import net.sf.l2j.gameserver.data.manager.HeroManager;
import net.sf.l2j.gameserver.data.xml.AdminData;
import net.sf.l2j.gameserver.enums.FloodProtector;
import net.sf.l2j.gameserver.handler.AdminCommandHandler;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;  // Import do handler de comandos de voz
import net.sf.l2j.gameserver.handler.VoicedCommandHandler;  // Import da classe que gerencia comandos de voz
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.OlympiadManagerNpc;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.scripting.QuestState;

public final class RequestBypassToServer extends L2GameClientPacket
{
    private static final Logger GMAUDIT_LOG = Logger.getLogger("gmaudit");
    
    private String _command;
    
    @Override
    protected void readImpl()
    {
        _command = readS();
    }
    
    @Override
    protected void runImpl()
    {
        if (_command.isEmpty())
            return;
        
        if (!getClient().performAction(FloodProtector.SERVER_BYPASS))
            return;
        
        final Player player = getClient().getPlayer();
        if (player == null)
            return;
        
        // Processamento de comando admin
        if (_command.startsWith("admin_"))
        {
            String command = _command.split(" ")[0];
            
            final IAdminCommandHandler ach = AdminCommandHandler.getInstance().getHandler(command);
            if (ach == null)
            {
                if (player.isGM())
                    player.sendMessage("The command " + command.substring(6) + " doesn't exist.");
                return;
            }
            
            if (!AdminData.getInstance().hasAccess(command, player.getAccessLevel()))
            {
                player.sendMessage("You don't have the access rights to use this command.");
                LOGGER.warn("{} tried to use admin command '{}' without proper Access Level.", player.getName(), command);
                return;
            }
            
            if (Config.GMAUDIT)
                GMAUDIT_LOG.info(player.getName() + " [" + player.getObjectId() + "] used '" + _command + "' command on: " + ((player.getTarget() != null) ? player.getTarget().getName() : "none"));
            
            ach.useAdminCommand(_command, player);
        }
        // Comando de ajuda do jogador
        else if (_command.startsWith("player_help "))
        {
            final String path = _command.substring(12);
            if (path.indexOf("..") != -1)
                return;
            
            final StringTokenizer st = new StringTokenizer(path);
            final String[] cmd = st.nextToken().split("#");
            
            final NpcHtmlMessage html = new NpcHtmlMessage(0);
            html.setFile("data/html/help/" + cmd[0]);
            if (cmd.length > 1)
            {
                final int itemId = Integer.parseInt(cmd[1]);
                html.setItemId(itemId);
                
                if (itemId == 7064 && cmd[0].equalsIgnoreCase("lidias_diary/7064-16.htm"))
                {
                    final QuestState qs = player.getQuestList().getQuestState("Q023_LidiasHeart");
                    if (qs != null && qs.getCond() == 5 && qs.getInteger("diary") == 0)
                        qs.set("diary", "1");
                }
            }
            html.disableValidation();
            player.sendPacket(html);
        }
        // Processamento de comando npc
        else if (_command.startsWith("npc_"))
        {
            if (!player.validateBypass(_command))
                return;
            
            int endOfId = _command.indexOf('_', 5);
            String id;
            if (endOfId > 0)
                id = _command.substring(4, endOfId);
            else
                id = _command.substring(4);
            
            try
            {
                final WorldObject object = World.getInstance().getObject(Integer.parseInt(id));
                if (object instanceof Npc npc && endOfId > 0 && player.getAI().canDoInteract(npc))
                    npc.onBypassFeedback(player, _command.substring(endOfId + 1));
                
                player.sendPacket(ActionFailed.STATIC_PACKET);
            }
            catch (NumberFormatException nfe)
            {
                // Do nothing.
            }
        }
        // Novo bloco para processar comandos de voz
        else if (_command.startsWith("voiced_"))
        {
            String command = _command.substring(7); // Remove o prefixo "voiced_"
            
            IVoicedCommandHandler vch = VoicedCommandHandler.getInstance().getHandler(command);
            if (vch != null)
            {
                String params = _command.length() > 7 ? _command.substring(7 + command.length()) : null;
                vch.useVoicedCommand(command, player, params);
            }
            else
            {
                player.sendMessage("The voiced command '" + command + "' does not exist.");
            }
        }

        else if (_command.startsWith("manor_menu_select?"))
        {
            WorldObject object = player.getTarget();
            if (object instanceof Npc targetNpc)
                targetNpc.onBypassFeedback(player, _command);
        }
    }
}
