package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WarriorBase.Warrior.PartyPrivateWarrior;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.skills.L2Skill;

public class PartyPrivateCouplePhysicalSpecial extends PartyPrivateWarrior
{
	public PartyPrivateCouplePhysicalSpecial()
	{
		super("ai/individual/Monster/WarriorBase/Warrior/PartyPrivateWarrior");
	}
	
	public PartyPrivateCouplePhysicalSpecial(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		21433
	};
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (attacker instanceof Playable && npc._i_ai0 == 0)
		{
			final Creature topDesireTarget = npc.getAI().getTopDesireTarget();
			if (topDesireTarget == attacker && npc.getStatus().getHpRatio() < 0.2 && Rnd.get(100) < 33)
			{
				npc.getAI().addCastDesire(attacker, getNpcSkillByType(npc, NpcSkillType.PHYSICAL_SPECIAL), 1000000);
				
				npc._i_ai0 = 1;
			}
		}
		super.onAttacked(npc, attacker, damage, skill);
	}
	
	@Override
	public void onClanAttacked(Npc caller, Npc called, Creature attacker, int damage, L2Skill skill)
	{
		if (attacker instanceof Playable && called.getAI().getLifeTime() > 7 && called._i_ai0 == 0 && Rnd.get(100) < 33)
		{
			called.getAI().addCastDesire(attacker, getNpcSkillByType(called, NpcSkillType.PHYSICAL_SPECIAL), 1000000);
			
			called._i_ai0 = 1;
		}
		super.onClanAttacked(caller, called, attacker, damage, skill);
	}
	
	@Override
	public void onPartyDied(Npc caller, Npc called)
	{
		if (caller != called && called.distance2D(caller) < 100)
		{
			called.getAI().addCastDesire(called, getNpcSkillByType(called, NpcSkillType.MAGIC_HEAL), 1000000);
			called.getAI().addCastDesire(called, getNpcSkillByType(called, NpcSkillType.SELF_BUFF), 1000000);
			
			final Creature topDesireTarget = called.getAI().getTopDesireTarget();
			if (topDesireTarget != null)
			{
				called.removeAllAttackDesire();
				called.getAI().addAttackDesire(topDesireTarget, 1000);
			}
		}
		super.onPartyDied(caller, called);
	}
}