package net.sf.l2j.gameserver.model.actor.instance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.math.MathUtil;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.manager.BufferManager;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.skills.L2Skill;

public class PlayerBuffer extends Folk
{
   private static final int PAGE_LIMIT = 6;
   
   public PlayerBuffer(int objectId, NpcTemplate template)
   {
       super(objectId, template);
   }
   
   private void showMainWindow(Player activeChar)
   {
       final NpcHtmlMessage html = new NpcHtmlMessage(0);
       html.setFile(getHtmlPath(getNpcId(), 0));
       html.replace("%objectId%", String.valueOf(getObjectId()));
       html.replace("%name%", activeChar.getName());
       html.replace("%buffcount%", "You have " + activeChar.getBuffCount() + "/" + activeChar.getMaxBuffCount() + " buffs.");
       
       activeChar.sendPacket(html);
   }
   
   @Override
   public void onBypassFeedback(Player player, String command)
   {
       if (player.getPvpFlag() > 0 && Config.RESTRICT_USE_BUFFER_ON_PVPFLAG)
       {
           player.sendMessage("You can't use buffer when you are pvp flagged.");
           return;
       }
       
       if (player.isInCombat() && Config.RESTRICT_USE_BUFFER_IN_COMBAT)
       {
           player.sendMessage("You can't use buffer when you are in combat.");
           return;
       }
       
       if (player.isDead())
           return;
       
       StringTokenizer st = new StringTokenizer(command, " ");
       String currentCommand = st.nextToken();
       
       if (currentCommand.startsWith("bufflist"))
       {
           autoBuffFunction(player, st.nextToken());
       }
       else if (currentCommand.startsWith("restore"))
       {
           String noble = st.nextToken();
           player.getStatus().setMaxCpHpMp();
           
           if (noble.equals("true"))
           {
               SkillTable.getInstance().getInfo(1323, 1).getEffects(player, player);
               player.broadcastPacket(new MagicSkillUse(this, player, 1323, 1, 850, 0));
           }
           
           final Summon summon = player.getSummon();
           if (summon != null)
               summon.getStatus().setMaxHpMp();
           
           showMainWindow(player);
       }
       else if (currentCommand.startsWith("cancellation"))
       {
           L2Skill buff;
           buff = SkillTable.getInstance().getInfo(1056, 1);
           buff.getEffects(this, player);
           player.stopAllEffectsExceptThoseThatLastThroughDeath();
           player.broadcastPacket(new MagicSkillUse(this, player, 1056, 1, 850, 0));
           player.stopAllEffects();
           
           final Summon summon = player.getSummon();
           if (summon != null)
               summon.stopAllEffects();
           
           showMainWindow(player);
       }
       else if (currentCommand.startsWith("openlist"))
       {
           String category = st.nextToken();
           String htmfile = st.nextToken();
           
           NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
           
           if (category.startsWith("null"))
           {
               html.setFile("data/html/mods/buffer/" + htmfile + ".htm");
               
               // First Page
               if (htmfile.equals("index"))
               {
                   html.replace("%name%", player.getName());
                   html.replace("%buffcount%", "You have " + player.getBuffCount() + "/" + player.getMaxBuffCount() + " buffs.");
               }
           }
           else
               html.setFile("data/html/mods/buffer/" + category + "/" + htmfile + ".htm");
           
           html.replace("%objectId%", String.valueOf(getObjectId()));
           player.sendPacket(html);
       }
       
       else if (currentCommand.startsWith("dobuff"))
       {
           int buffid = Integer.valueOf(st.nextToken());
           int bufflevel = Integer.valueOf(st.nextToken());
           String category = st.nextToken();
           String windowhtml = st.nextToken();
           String votebuff = null;
           
           if (st.hasMoreTokens())
               votebuff = st.nextToken();
           
           if (windowhtml.equals("malaria"))
           {
               if (player.getInventory().getItemCount(Config.PVOTE_BUFF_ITEM_ID, 0) >= 1)
               {
                   player.getInventory().destroyItemByItemId(Config.PVOTE_BUFF_ITEM_ID, 1);
                   player.sendPacket(new ItemList(player, true));
                   player.sendMessage(1 + " Vote eye destroyed.");
               }
               else
               {
                   player.sendMessage("You dont have enough (" + 1 + ") vote item for buff.");
                   return;
               }
           }
           
           if (votebuff != null)
           {
               if (player.getInventory().getItemCount(Config.PVOTE_BUFF_ITEM_ID, 0) >= Config.PVOTE_BUFF_ITEM_COUNT)
               {
                   player.getInventory().destroyItemByItemId(Config.PVOTE_BUFF_ITEM_ID, Config.PVOTE_BUFF_ITEM_COUNT);
                   player.sendPacket(new ItemList(player, true));
                   player.sendMessage(Config.PVOTE_BUFF_ITEM_COUNT + " vote stone destroyed.");
               }
               else
               {
                   player.sendMessage("You dont have enough (" + Config.PVOTE_BUFF_ITEM_COUNT + ") vote item for buff.");
                   return;
               }
           }
           
           Creature target = player;
           if (category.startsWith("pet"))
           {
               if (player.getSummon() == null)
               {
                   player.sendMessage("Incorrect Pet");
                   showMainWindow(player);
                   return;
               }
               target = player.getSummon();
           }
           
           MagicSkillUse mgc = new MagicSkillUse(this, target, buffid, bufflevel, 1150, 0);
           player.sendPacket(mgc);
           player.broadcastPacket(mgc);
           SkillTable.getInstance().getInfo(buffid, bufflevel).getEffects(this, target);
           
           NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
           html.setFile("data/html/mods/buffer/" + category + "/" + windowhtml + ".htm");
           html.replace("%objectId%", String.valueOf(getObjectId()));
           html.replace("%name%", player.getName());
           player.sendPacket(html);
       }
       else if (currentCommand.startsWith("getbuff"))
       {
           int buffid = Integer.valueOf(st.nextToken());
           int bufflevel = Integer.valueOf(st.nextToken());
           if (buffid != 0)
           {
               SkillTable.getInstance().getInfo(buffid, bufflevel).getEffects(this, player);
               broadcastPacket(new MagicSkillUse(this, player, buffid, bufflevel, 450, 0));
               showMainWindow(player);
           }
       }
       else if (currentCommand.startsWith("support"))
       {
           showGiveBuffsWindow(player);
       }
       else if (currentCommand.startsWith("givebuffs"))
       {
           final String schemeName = st.nextToken();
           final int cost = Integer.parseInt(st.nextToken());
           
           Creature target = null;
           if (st.hasMoreTokens())
           {
               final String targetType = st.nextToken();
               if (targetType != null && targetType.equalsIgnoreCase("pet"))
                   target = player.getSummon();
           }
           else
               target = player;
           
           if (target == null)
               player.sendMessage("You don't have a pet.");
           else if (cost == 0 || player.reduceAdena(cost, true))
               BufferManager.getInstance().applySchemeEffects(this, target, player.getObjectId(), schemeName);
       }
       else if (currentCommand.startsWith("editschemes"))
       {
           showEditSchemeWindow(player, st.nextToken(), st.nextToken(), Integer.parseInt(st.nextToken()));
       }
       else if (currentCommand.startsWith("skill"))
       {
           final String groupType = st.nextToken();
           final String schemeName = st.nextToken();
           
           final int skillId = Integer.parseInt(st.nextToken());
           final int page = Integer.parseInt(st.nextToken());
           
           final List<Integer> skills = BufferManager.getInstance().getScheme(player.getObjectId(), schemeName);
           
           if (currentCommand.startsWith("skillselect") && !schemeName.equalsIgnoreCase("none"))
           {
               if (skills.size() < player.getMaxBuffCount())
                   skills.add(skillId);
               else
                   player.sendMessage("This scheme has reached the maximum amount of buffs.");
           }
           else if (currentCommand.startsWith("skillunselect"))
               skills.remove(Integer.valueOf(skillId));
           
           showEditSchemeWindow(player, groupType, schemeName, page);
       }
       else if (currentCommand.startsWith("createscheme"))
       {
           try
           {
               final String schemeName = st.nextToken();
               if (schemeName.length() > 14)
               {
                   player.sendMessage("Scheme's name must contain up to 14 chars. Spaces are trimmed.");
                   return;
               }
               
               final Map<String, ArrayList<Integer>> schemes = BufferManager.getInstance().getPlayerSchemes(player.getObjectId());
               if (schemes != null)
               {
                   if (schemes.size() == Config.BUFFER_MAX_SCHEMES)
                   {
                       player.sendMessage("Maximum schemes amount is already reached.");
                       return;
                   }
                   
                   if (schemes.containsKey(schemeName))
                   {
                       player.sendMessage("The scheme name already exists.");
                       return;
                   }
               }
               
               BufferManager.getInstance().setScheme(player.getObjectId(), schemeName.trim(), new ArrayList<>());
               showGiveBuffsWindow(player);
           }
           catch (Exception e)
           {
               player.sendMessage("Scheme's name must contain up to 14 chars. Spaces are trimmed.");
           }
       }
       else if (currentCommand.startsWith("deletescheme"))
       {
           try
           {
               final String schemeName = st.nextToken();
               final Map<String, ArrayList<Integer>> schemes = BufferManager.getInstance().getPlayerSchemes(player.getObjectId());
               
               if (schemes != null && schemes.containsKey(schemeName))
                   schemes.remove(schemeName);
           }
           catch (Exception e)
           {
               player.sendMessage("This scheme name is invalid.");
           }
           showGiveBuffsWindow(player);
       }
       
       super.onBypassFeedback(player, command);
   }
   
   @Override
   public String getHtmlPath(int npcId, int val)
   {
       String filename = "";
       if (val == 0)
           filename = "" + npcId;
       else
           filename = npcId + "-" + val;
       
       return "data/html/mods/buffer/" + filename + ".htm";
   }
   
   /**
    * Send an html packet to the {@link Player} set a parameter with Give Buffs menu info for player and pet, depending on targetType parameter {player, pet}.
    * @param player : The {@link Player} to make checks on.
    */
   private void showGiveBuffsWindow(Player player)
   {
       final StringBuilder sb = new StringBuilder(200);
       
       final Map<String, ArrayList<Integer>> schemes = BufferManager.getInstance().getPlayerSchemes(player.getObjectId());
       if (schemes == null || schemes.isEmpty())
           sb.append("<font color=\"LEVEL\">You haven't defined any scheme.</font>");
       else
       {
           for (Map.Entry<String, ArrayList<Integer>> scheme : schemes.entrySet())
           {
               final int cost = getFee(scheme.getValue());
               StringUtil.append(sb, "<font color=\"LEVEL\">", scheme.getKey(), " [", scheme.getValue().size(), " / ", player.getMaxBuffCount(), "]", ((cost > 0) ? " - cost: " + StringUtil.formatNumber(cost) : ""), "</font><br1>");
               StringUtil.append(sb, "<a action=\"bypass npc_%objectId%_givebuffs ", scheme.getKey(), " ", cost, "\">Use on Me</a>&nbsp;|&nbsp;");
               StringUtil.append(sb, "<a action=\"bypass npc_%objectId%_givebuffs ", scheme.getKey(), " ", cost, " pet\">Use on Pet</a>&nbsp;|&nbsp;");
               StringUtil.append(sb, "<a action=\"bypass npc_%objectId%_editschemes Buffs ", scheme.getKey(), " 1\">Edit</a>&nbsp;|&nbsp;");
               StringUtil.append(sb, "<a action=\"bypass npc_%objectId%_deletescheme ", scheme.getKey(), "\">Delete</a><br>");
           }
       }
       
       final NpcHtmlMessage html = new NpcHtmlMessage(0);
       html.setFile(getHtmlPath(getNpcId(), 1));
       html.replace("%schemes%", sb.toString());
       html.replace("%max_schemes%", Config.BUFFER_MAX_SCHEMES);
       html.replace("%objectId%", getObjectId());
       player.sendPacket(html);
   }
   
   /**
    * Send an html packet to the {@link Player} set as parameter with Edit Scheme Menu info. This allows the {@link Player} to edit each created scheme (add/delete skills)
    * @param player : The {@link Player} to make checks on.
    * @param groupType : The group of skills to select.
    * @param schemeName : The scheme to make check.
    * @param page : The current checked page.
    */
   private void showEditSchemeWindow(Player player, String groupType, String schemeName, int page)
   {
       final NpcHtmlMessage html = new NpcHtmlMessage(0);
       final List<Integer> schemeSkills = BufferManager.getInstance().getScheme(player.getObjectId(), schemeName);
       
       html.setFile(getHtmlPath(getNpcId(), 2));
       html.replace("%schemename%", schemeName);
       html.replace("%count%", schemeSkills.size() + " / " + player.getMaxBuffCount());
       html.replace("%typesframe%", getTypesFrame(groupType, schemeName));
       html.replace("%skilllistframe%", getGroupSkillList(player, groupType, schemeName, page));
       html.replace("%objectId%", getObjectId());
       player.sendPacket(html);
   }
   
   /**
    * @param player : The {@link Player} to make checks on.
    * @param groupType : The group of skills to select.
    * @param schemeName : The scheme to make check.
    * @param page : The current checked page.
    * @return A {@link String} representing skills available for selection for a given groupType.
    */
   private String getGroupSkillList(Player player, String groupType, String schemeName, int page)
   {
       // Retrieve the entire skills list based on group type.
       List<Integer> skills = BufferManager.getInstance().getSkillsIdsByType(groupType);
       if (skills.isEmpty())
           return "That group doesn't contain any skills.";
       
       // Calculate page number.
       final int max = MathUtil.countPagesNumber(skills.size(), PAGE_LIMIT);
       if (page > max)
           page = max;
       
       // Cut skills list up to page number.
       skills = skills.subList((page - 1) * PAGE_LIMIT, Math.min(page * PAGE_LIMIT, skills.size()));
       
       final List<Integer> schemeSkills = BufferManager.getInstance().getScheme(player.getObjectId(), schemeName);
       final StringBuilder sb = new StringBuilder(skills.size() * 150);
       
       int row = 0;
       for (int skillId : skills)
       {
           final String icon = (skillId < 100) ? "icon.skill00" + skillId : (skillId < 1000) ? "icon.skill0" + skillId : "icon.skill" + skillId;
           
           sb.append(((row % 2) == 0 ? "<table width=\"280\" bgcolor=\"000000\"><tr>" : "<table width=\"280\"><tr>"));
           
           if (schemeSkills.contains(skillId))
               StringUtil.append(sb, "<td height=40 width=40><img src=\"", icon, "\" width=32 height=32></td><td width=190>", SkillTable.getInstance().getInfo(skillId, 1).getName(), "<br1><font color=\"B09878\">", BufferManager.getInstance().getAvailableBuff(skillId).description(), "</font></td><td><button action=\"bypass npc_%objectId%_skillunselect ", groupType, " ", schemeName, " ", skillId, " ", page, "\" width=32 height=32 back=\"L2UI_CH3.mapbutton_zoomout2\" fore=\"L2UI_CH3.mapbutton_zoomout1\"></td>");
           else
               StringUtil.append(sb, "<td height=40 width=40><img src=\"", icon, "\" width=32 height=32></td><td width=190>", SkillTable.getInstance().getInfo(skillId, 1).getName(), "<br1><font color=\"B09878\">", BufferManager.getInstance().getAvailableBuff(skillId).description(), "</font></td><td><button action=\"bypass npc_%objectId%_skillselect ", groupType, " ", schemeName, " ", skillId, " ", page, "\" width=32 height=32 back=\"L2UI_CH3.mapbutton_zoomin2\" fore=\"L2UI_CH3.mapbutton_zoomin1\"></td>");
           
           sb.append("</tr></table><img src=\"L2UI.SquareGray\" width=280 height=1>");
           row++;
       }
       
       for (int i = PAGE_LIMIT; i > row; i--)
           StringUtil.append(sb, "<img height=41>");
       
       // Build page footer.
       sb.append("<br><img src=\"L2UI.SquareGray\" width=280 height=1><table width=\"100%\" bgcolor=000000><tr>");
       
       if (page > 1)
           StringUtil.append(sb, "<td align=left width=70><a action=\"bypass npc_" + getObjectId() + "_editschemes ", groupType, " ", schemeName, " ", page - 1, "\">Previous</a></td>");
       else
           StringUtil.append(sb, "<td align=left width=70>Previous</td>");
       
       StringUtil.append(sb, "<td align=center width=100>Page ", page, "</td>");
       
       if (page < max)
           StringUtil.append(sb, "<td align=right width=70><a action=\"bypass npc_" + getObjectId() + "_editschemes ", groupType, " ", schemeName, " ", page + 1, "\">Next</a></td>");
       else
           StringUtil.append(sb, "<td align=right width=70>Next</td>");
       
       sb.append("</tr></table><img src=\"L2UI.SquareGray\" width=280 height=1>");
       
       return sb.toString();
   }
   
   /**
    * @param groupType : The group of skills to select.
    * @param schemeName : The scheme to make check.
    * @return A {@link String} representing all groupTypes available. The group currently on selection isn't linkable.
    */
   private static String getTypesFrame(String groupType, String schemeName)
   {
       final StringBuilder sb = new StringBuilder(500);
       sb.append("<table>");
       
       int count = 0;
       for (String type : BufferManager.getInstance().getSkillTypes())
       {
           if (count == 0)
               sb.append("<tr>");
           
           if (groupType.equalsIgnoreCase(type))
               StringUtil.append(sb, "<td width=65>", type, "</td>");
           else
               StringUtil.append(sb, "<td width=65><a action=\"bypass npc_%objectId%_editschemes ", type, " ", schemeName, " 1\">", type, "</a></td>");
           
           count++;
           if (count == 4)
           {
               sb.append("</tr>");
               count = 0;
           }
       }
       
       if (!sb.toString().endsWith("</tr>"))
           sb.append("</tr>");
       
       sb.append("</table>");
       
       return sb.toString();
   }
   
   /**
    * @param list : A {@link List} of skill ids.
    * @return a global fee for all skills contained in the {@link List}.
    */
   private static int getFee(ArrayList<Integer> list)
   {
       if (Config.BUFFER_STATIC_BUFF_COST > 0)
           return list.size() * Config.BUFFER_STATIC_BUFF_COST;
       
       int fee = 0;
       for (int sk : list)
           fee += BufferManager.getInstance().getAvailableBuff(sk).price();
       
       return fee;
   }
   
   private void autoBuffFunction(Player player, String bufflist)
   {
       ArrayList<L2Skill> skills_to_buff = new ArrayList<>();
       List<Integer> list = null;
       
       if (bufflist.equalsIgnoreCase("fighter"))
           list = Config.FIGHTER_SKILL_LIST;
       else if (bufflist.equalsIgnoreCase("mage"))
           list = Config.MAGE_SKILL_LIST;
       
       if (list != null)
       {
           for (int skillId : list)
           {
               L2Skill skill = SkillTable.getInstance().getInfo(skillId, SkillTable.getInstance().getMaxLevel(skillId));
               if (skill != null)
                   skills_to_buff.add(skill);
           }
           
           for (L2Skill sk : skills_to_buff)
               sk.getEffects(player, player);
           
           player.updateEffectIcons();
           
           list = null;
       }
       
       skills_to_buff.clear();
       
       showMainWindow(player);
   }
   
}
