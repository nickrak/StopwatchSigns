package nickrak.stopwatchsigns;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;

public final class StopwatchPlayerListener extends PlayerListener
{
	@Override
	public final void onPlayerInteract(PlayerInteractEvent event)
	{
		final Player p = event.getPlayer();
		if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK)
		{
			final Block b = event.getClickedBlock();
			final BlockState bs = b.getState();

			if (bs instanceof Sign)
			{
				final Sign s = (Sign) bs;

				if (p.hasPermission("stopwatchsigns.use"))
				{
					StopwatchSigns.instance.toggleWatch(s.getLine(1));
				}
				else
				{
					p.sendMessage(ChatColor.RED + "You don't have permission to use stopwatch signs.");
				}
			}
		}
	}
}
