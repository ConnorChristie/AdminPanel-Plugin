package com.mcadminpanel.plugin;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.mcadminpanel.plugin.events.ServerEventListener;
import com.mcadminpanel.plugin.ghost.GhostFactory;

public class AdminPanel extends JavaPlugin
{
	private static AdminPanel instance;
	
	private boolean connected;
	private BukkitTask task;
	
	protected boolean whitelist = true;
	protected int appCount = 0;
	protected boolean hasTinyUrl = true;
	
	protected String tinyUrl;
	protected JSONArray users = new JSONArray();
	
	private PrintStream originalOut;
	private GhostFactory ghostFactory;
	
	private JSONParser jsonParser = new JSONParser();
	private MethodHandler methodHandler = new MethodHandler(this);
	
	private List<String[]> chats = new ArrayList<String[]>();
	private Map<String, String> returns = new HashMap<String, String>();
	
	public AdminPanel()
	{
		instance = this;
	}
	
	public void onEnable()
	{
		ghostFactory = new GhostFactory(this);
		originalOut = new PrintStream(new FileOutputStream(FileDescriptor.out));
		
		getCommand("mcadminpanelplugincmd").setExecutor(this);
		getCommand("mcapanel").setExecutor(this);
		getCommand("ap").setExecutor(this);
		
		getServer().getPluginManager().registerEvents(new ServerEventListener(this), this);
		
		originalOut.println("{\"plugin\":\"McAdminPanel\",\"type\":\"connect\",\"connected\":true}");
		
		//System.out.println("UUID: " + Bukkit.getOfflinePlayer("ChillerCraft").getUniqueId().toString());
		
		Bukkit.getScheduler().runTaskLater(this, new Runnable() {
			public void run()
			{
				if (!connected)
				{
					getLogger().warning("Could not connect to McAdminPanel...");
				} else
				{
					task = Bukkit.getScheduler().runTaskTimerAsynchronously(instance, new Runnable() {
						public void run()
						{
							String initStr = sendMethodResponse("getInitial");
							
							try
							{
								JSONObject init = (JSONObject) jsonParser.parse(initStr);
								
								whitelist = (Boolean) init.get("whitelist");
								appCount = Integer.parseInt(init.get("appCount").toString());
								hasTinyUrl = (Boolean) init.get("hasTinyUrl");
								tinyUrl = (String) init.get("tinyUrl");
								users = (JSONArray) init.get("users");
							} catch (Exception e) { } catch (Error er) { }
						}
					}, 0, 5 * 20);
				}
			}
		}, 10);
	}
	
	public void onDisable()
	{
		if (task != null) task.cancel();
		
		originalOut.println("{\"plugin\":\"McAdminPanel\",\"type\":\"connect\",\"connected\":false}");
		
		connected = false;
	}
	
	public boolean groupHasPermission(String perms, String perm)
	{
		return perms.contains("*") || perms.contains(perm);
	}
	
	public boolean enableWhitelist()
	{
		return whitelist;
	}
	
	public int getAppCount()
	{
		return appCount;
	}
	
	public boolean hasTinyUrl()
	{
		return hasTinyUrl;
	}
	
	public String getTinyUrl()
	{
		return tinyUrl;
	}
	
	public boolean isConnected()
	{
		return connected;
	}
	
	public JSONObject getUserFromIp(String ip)
	{
		for (Object obj : users)
		{
			JSONObject user = (JSONObject) obj;
			
			if (user.get("ipAddress").equals(ip))
			{
				return user;
			}
		}
		
		return null;
	}
	
	//Change to get from UUID
	public JSONObject getUserFromName(String name)
	{
		for (Object obj : users)
		{
			JSONObject user = (JSONObject) obj;
			
			if (user.get("username").equals(name))
			{
				return user;
			}
		}
		
		return null;
	}
	
	public GhostFactory getGhostFactory()
	{
		return ghostFactory;
	}
	
	public List<String[]> getChats()
	{
		return chats;
	}
	
	public JSONParser getJParser()
	{
		return jsonParser;
	}
	
	@SuppressWarnings("unchecked")
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if (sender instanceof ConsoleCommandSender && label.equalsIgnoreCase("mcadminpanelplugincmd") && args.length >= 1)
		{
			String arguments = StringUtils.join(args, " ");
			
			if (arguments.startsWith("{") && arguments.endsWith("}"))
			{
				try
				{
					JSONObject obj = (JSONObject) jsonParser.parse(arguments);
					
					if (obj.containsKey("plugin") && obj.get("plugin").equals("McAdminPanel") && obj.containsKey("type"))
					{
						if (obj.get("type").equals("method"))
						{
							final Long time = (Long) obj.get("time");
							final String method = (String) obj.get("method");
							final String paramStr = (String) obj.get("params");
							
							final String[] params = paramStr.length() != 0 ? paramStr.split(", ") : new String[0];
							
							try
							{
								Class<?>[] paramClasses = new Class<?>[params.length];
								
								for (int i = 0; i < params.length; i++)
									paramClasses[i] = Class.forName("java.lang.String");
								
								final Method m = methodHandler.getClass().getDeclaredMethod(method, paramClasses);
								
								Bukkit.getScheduler().runTask(this, new Runnable()
								{
									public void run()
									{
										JSONObject out = new JSONObject();
										
										out.put("type", "response");
										out.put("time", time);
										out.put("plugin", "McAdminPanel");
										out.put("method", method);
										
										try
										{
											String ret = (String) m.invoke(methodHandler, (Object[]) params);
											
											if (m.getReturnType().equals(Void.TYPE))
												return;
											
											out.put("response", ret);
										} catch (InvocationTargetException e)
										{
											e.printStackTrace();
										} catch (IllegalAccessException e)
										{
											e.printStackTrace();
										} catch (IllegalArgumentException e)
										{
											e.printStackTrace();
										}
										
										if (!out.containsKey("response"))
											out.put("response", "No such method");
										
										originalOut.println(out.toJSONString());
									}
								});
							} catch (Exception e)
							{
								System.out.println("Error: " + arguments);
								
								e.printStackTrace();
							}
						} else if (obj.get("type").equals("response"))
						{
							returns.put((String) obj.get("method"), (String) obj.get("response"));
						} else if (obj.get("type").equals("connect"))
						{
							if ((Boolean) obj.get("connected"))
							{
								connected = true;
							}
						}
					}
				} catch (Error e) { } catch (Exception e1) { }
			}
		} else if (label.equalsIgnoreCase("mcapanel"))
		{
			if (args.length == 1)
			{
				if (sender instanceof Player && args[0].equalsIgnoreCase("resetpassword"))
				{
					Player player = (Player) sender;
					
					String randomStr = RandomStringUtils.randomAlphanumeric(8).toLowerCase();
					
					sendMethod("passwordCode", player.getName(), randomStr);
					
					player.sendMessage(ChatColor.GOLD + "[MCAPanel] " + ChatColor.GREEN + "Reset Code: " + ChatColor.AQUA + randomStr);
				}
			}
		}
		
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public String sendMethod(String method, String... params)
	{
		if (connected)
		{
			String paramStr = StringUtils.join(params, ", ");
			
			JSONObject obj = new JSONObject();
			
			obj.put("plugin", "McAdminPanel");
			obj.put("type", "method");
			
			obj.put("method", method);
			obj.put("params", paramStr);
			
			originalOut.println(obj.toJSONString());
			
			return method;
		}
		
		return null;
	}
	
	public String sendMethodResponse(String method, String... params)
	{
		if (connected)
		{
			if (returns.containsKey(method))
				returns.remove(method);
			
			method = sendMethod(method, params);
			
			long start = System.currentTimeMillis();
			
			synchronized (returns)
			{
				while (System.currentTimeMillis() - start < 2000)
				{
					if (returns.containsKey(method))
					{
						String ret = returns.get(method);
						
						returns.remove(method);
						
						return ret;
					}
				}
			}
		}
		
		return null;
	}
}