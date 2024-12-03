package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.data.xml.RestartPointData;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.group.PartyMatchRoom;
import net.sf.l2j.gameserver.model.restart.RestartPoint;

public class ExPartyRoomMember extends L2GameServerPacket
{
	private final PartyMatchRoom _room;
	private final int _mode;
	
	public ExPartyRoomMember(PartyMatchRoom room, int mode)
	{
		_room = room;
		_mode = mode;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x0e);
		writeD(_mode);
		writeD(_room.getMembersCount());
		
		for (Player member : _room.getMembers())
		{
			writeD(member.getObjectId());
			writeS(member.getName());
			writeD(member.getActiveClass());
			writeD(member.getStatus().getLevel());
			
			final RestartPoint rp = RestartPointData.getInstance().getRestartPoint(member);
			writeD((rp == null) ? 100 : rp.getBbs());
			
			if (_room.isLeader(member))
				writeD(1);
			else
			{
				if ((_room.getLeader().isInParty() && member.isInParty()) && (_room.getLeader().getParty().getLeaderObjectId() == member.getParty().getLeaderObjectId()))
					writeD(2);
				else
					writeD(0);
			}
		}
	}
}