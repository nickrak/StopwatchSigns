package nickrak.stopwatchsigns;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.SignChangeEvent;

public final class StopwatchBlockListener extends BlockListener implements Runnable
{
	private final static String marker = ChatColor.AQUA + "[Stopwatch]";
	private final FileConfiguration signs;
	private final Server server;

	public StopwatchBlockListener()
	{
		this.signs = StopwatchSigns.instance.getConfig();
		this.server = StopwatchSigns.instance.getServer();
		
		final Set<String> keys = this.signs.getKeys(true);
		for (final String key : keys) StopwatchSigns.instance.prepWatch(key);
	}

	private final void updateSign(final String name)
	{
		final List<String> signList = this.loadSigns(name);
		for (final String signPosition : signList)
		{
			final String[] parts = signPosition.split(",");
			if (parts.length == 4)
			{
				final int x = Integer.parseInt(parts[0]);
				final int y = Integer.parseInt(parts[1]);
				final int z = Integer.parseInt(parts[2]);
				final String w = parts[3];
				this.updateSign(x, y, z, w);
			}
		}
	}

	private final void updateSign(final int x, final int y, final int z, final String w)
	{
		final Block b = this.server.getWorld(w).getBlockAt(x, y, z);
		final BlockState bs = b.getState();

		if (bs instanceof Sign)
		{
			final Sign s = (Sign) bs;
			s.setLine(2, StopwatchSigns.instance.getState(s.getLine(1)));
			s.setLine(3, StopwatchSigns.instance.getTime(s.getLine(1)));
			bs.update();
		}
	}

	@Override
	public final void run()
	{
		final Set<String> watches = this.signs.getKeys(true);
		for (final String name : watches)
			this.updateSign(name);
	}

	private final List<String> loadSigns(final String name)
	{
		final List<String> items = new ArrayList<String>();
		final String[] parts = this.signs.getString(name, "").split(";");
		for (final String part : parts)
			items.add(part);
		return items;
	}

	private final void saveSigns(final String name, final List<String> items)
	{
		final StringBuilder sb = new StringBuilder();
		for (final String item : items)
			sb.append(String.format(";%s", item));
		this.signs.set(name, sb.toString().substring(1));

		StopwatchSigns.instance.saveConfig();
	}

	@Override
	public final void onBlockBreak(BlockBreakEvent event)
	{
		if (event.isCancelled()) return;
		final Block b = event.getBlock();
		final BlockState bs = b.getState();

		if (bs instanceof Sign)
		{
			final Sign s = (Sign) bs;
			final String tag = s.getLine(0);
			final String name = s.getLine(1);

			if (tag.equals(marker))
			{
				if (!event.getPlayer().hasPermission("stopwatchsigns.destroy"))
				{
					event.setCancelled(true);
				}

				final String val = String.format("%d,%d,%d,%s", b.getX(), b.getY(), b.getZ(), b.getWorld().getName());
				final List<String> signList = this.loadSigns(name);
				signList.remove(val);
				this.saveSigns(name, signList);
			}
		}
	}

	@Override
	public void onSignChange(SignChangeEvent event)
	{
		if (event.isCancelled())
		{
			event.getPlayer().sendMessage("Was Cancelled");
			return;
		}

		final String tag = event.getLine(0);
		final String name = event.getLine(1);


		if (tag.equalsIgnoreCase("[Stopwatch]"))
		{
			if (event.getPlayer().hasPermission("stopwatchsigns.create"))
			{
				event.setLine(0, marker);
				final List<String> signList = this.loadSigns(name);
				final Block b = event.getBlock();
				final String val = String.format("%d,%d,%d,%s", b.getX(), b.getY(), b.getZ(), b.getWorld().getName());
				if (!signList.contains(val))
				{
					signList.add(val);
				}
				this.saveSigns(name, signList);
				StopwatchSigns.instance.prepWatch(name);
			}
		}
	}
}
