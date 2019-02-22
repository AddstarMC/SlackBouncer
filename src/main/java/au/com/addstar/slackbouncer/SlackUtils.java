package au.com.addstar.slackbouncer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.com.addstar.slackapi.objects.*;
import au.com.addstar.slackapi.RealTimeSession;
import net.md_5.bungee.api.ChatColor;

public class SlackUtils
{
	private static Pattern groupPattern = Pattern.compile("<(.*?)>");
	
	public static String resolveGroups(String message, RealTimeSession session)
	{
		StringBuffer buffer = new StringBuffer();
		Matcher m = groupPattern.matcher(message);
		while (m.find())
		{
			String arg = m.group(1);
			if (arg.startsWith("#"))
			{
				String id = arg.substring(1);
				if (id.contains("|"))
					m.appendReplacement(buffer, "#" + id.substring(id.indexOf("|")+1));
				else
				{
					Conversation c = session.getChannelById(new ObjectID(id));
					if (c != null && c.isChannel() && c.isGeneral())
						m.appendReplacement(buffer, "#" + c.getName());
					else
						m.appendReplacement(buffer, "#" + id);
				}
			}
			else if (arg.startsWith("@"))
			{
				String id = arg.substring(1);
				if (id.contains("|"))
					m.appendReplacement(buffer, "@" + id.substring(id.indexOf("|")+1));
				else
				{
					User user = session.getUserById(new ObjectID(id));
					if (user != null)
						m.appendReplacement(buffer, "@" + user.getName());
					else
						m.appendReplacement(buffer, "@" + id);
				}
			}
			else if (arg.startsWith("!"))
			{
				switch (arg) {
					case "!everyone":
						m.appendReplacement(buffer, "@everyone");
						break;
					case "!channel":
						m.appendReplacement(buffer, "@channel");
						break;
					case "!group":
						m.appendReplacement(buffer, "@group");
						break;
				}
			}
			else
			{
				String url = arg;
				if (arg.contains("|"))
					url = arg.substring(0, arg.indexOf("|"));
				
				m.appendReplacement(buffer, url);
			}
		}
		m.appendTail(buffer);
		
		return buffer.toString();
	}
	
	public static String toMC(String message)
	{
		return message;
		// TODO: Handle formatting. Right now I cant think of a way to do it
		// * around text means bold
		//   - only applies if at least one is touching non whitespace
		// _ around text means italic
		// ` around text means code block
		// > before text means indent with border
		// >>> before text results with above
	}
	
	public static String toSlack(String message)
	{
		StringBuffer buffer = new StringBuffer(message);
		int pos = buffer.indexOf(String.valueOf(ChatColor.COLOR_CHAR));
		boolean isBold = false;
		boolean isItalic = false;
		boolean boldFirst = true;
		
		while(pos != -1)
		{
			if (buffer.length() > pos+1)
			{
				ChatColor color = ChatColor.getByChar(buffer.charAt(pos+1));
				if (color != null)
				{
					if (color == ChatColor.BOLD)
					{
						if (!isBold)
						{
							boldFirst = !isItalic;
							isBold = true;
							buffer.replace(pos, pos+2, "*");
						}
					}
					else if (color == ChatColor.ITALIC)
					{
						if (!isItalic)
						{
							boldFirst = isBold;
							isItalic = true;
							buffer.replace(pos, pos+2, "_");
						}
					}
					else
					{
						if (color == ChatColor.UNDERLINE || color == ChatColor.STRIKETHROUGH || color == ChatColor.MAGIC)
						{
							buffer.replace(pos, pos+2, "");
						}
						else // Color that will end italic / bold
						{
							String text;
							if (boldFirst)
							{
								text = "";
								if (isItalic)
									text += "_";
								if (isBold)
									text += "*";
							}
							else
							{
								text = "";
								if (isBold)
									text += "*";
								if (isItalic)
									text += "_";
							}
							
							isBold = false;
							isItalic = false;
							
							buffer.replace(pos, pos+2, text);
						}
					}
				}
			}
			else
				buffer.replace(pos, pos+1, "");
			
			pos = buffer.indexOf(String.valueOf(ChatColor.COLOR_CHAR), pos);
		}
		
		
		return buffer.toString();
	}
}
