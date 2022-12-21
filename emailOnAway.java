package com.tempstop;

import org.jivesoftware.openfire.MessageRouter;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.PresenceManager;
import org.jivesoftware.openfire.vcard.VCardManager;
//import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.EmailService;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

//import java.io.File;
import java.io.*;

public class emailOnAway implements Plugin, PacketInterceptor {

    private InterceptorManager interceptorManager;
    private UserManager userManager;
    private PresenceManager presenceManager;
    private EmailService emailService;
    private MessageRouter messageRouter;
    private VCardManager vcardManager;
    final char dm = (char) 34;
    
    public emailOnAway() {
        interceptorManager = InterceptorManager.getInstance();
    emailService = EmailService.getInstance();
        messageRouter = XMPPServer.getInstance().getMessageRouter();
    presenceManager = XMPPServer.getInstance().getPresenceManager();
    userManager = XMPPServer.getInstance().getUserManager();
    vcardManager = VCardManager.getInstance();
    
    }

    public void initializePlugin(PluginManager pManager, File pluginDirectory) {
        // register with interceptor manager
        interceptorManager.addInterceptor(this);
    }

    public void destroyPlugin() {
        // unregister with interceptor manager
        interceptorManager.removeInterceptor(this);
    }

    private Message createServerMessage(String to, String from, String emailTo, Boolean checkEmail, Integer fromconversejs, String urllink) {
        Message message = new Message();
        message.setType(Message.Type.chat);
      //drlight 20.03.2018 check if message came from conversejs web client
        if (fromconversejs == -1) {
        	message.setID("emailOnAway");
        }
        message.setTo(to);
        message.setFrom(from);

      //drlight 16.03.2018 check if user has an email
        //if (JiveGlobals.getBooleanProperty("plugin.emailonaway.showemail", true)) {
        if (checkEmail) {
        	message.setBody("В настоящий момент пользователь недоступен. Ваше сообщение было переадресовано на почтовый ящик пользователя " + emailTo);
        	//}
            } else {
            	message.setBody("В настоящий момент пользователь недоступен. У пользователя нет почтового ящика, поэтому ваше сообщение будет получено только когда пользователь войдет в чат.");
            }
        return message;
    }

    public void interceptPacket(Packet packet, Session session, boolean read,
            boolean processed) throws PacketRejectedException {
    
    String emailTo = null;
    String emailFrom = null;
    //drlight 16.03.2018 check if user has an email variable
    Boolean has_email = null;
    //drlight 20.03.2018 check int if message came from conversejs web client
    Integer fromconversejs = null;
    //drlight 20.03.2018 var for website contact link
    String urllink = null;
    
   /* if((!processed) && 
        (!read) && 
        (packet instanceof Message) && 
        (packet.getTo() != null)) { */
    
    if (processed
            && read
    		//&& session.isClosed()
            && packet instanceof Message
            && packet.getTo() != null
            //drlight 20.03.2018 check of emailOnAway plugin packet ID
            && packet.getID() != "emailOnAway") {
    	
        Message msg = (Message) packet;
        
        fromconversejs = msg.getFrom().getResource().toString().toLowerCase().indexOf("converse.js");
        
        
       if(msg.getType() == Message.Type.chat) {
        try {
            User userTo = userManager.getUser(packet.getTo().getNode());
          //drlight 16.03.2018 check if user is XA, AWAY, DND or OFFLINE
            if(!presenceManager.isAvailable(userTo) || presenceManager.getPresence(userTo).toString().toLowerCase().indexOf("xa") != -1 || presenceManager.getPresence(userTo).toString().toLowerCase().indexOf("away") != -1 || presenceManager.getPresence(userTo).toString().toLowerCase().indexOf("dnd") != -1)  { 
                if(msg.getBody() != null) {
                    // Build email/sms
                    // The to email address
                    emailTo = vcardManager.getVCardProperty(userTo.getUsername(), "EMAIL");
                    if(emailTo == null || emailTo.length() == 0) {
                    emailTo = vcardManager.getVCardProperty(userTo.getUsername(), "EMAIL:USERID");
                    if(emailTo == null || emailTo.length() == 0) {
                        emailTo = userTo.getEmail();
                        if(emailTo == null || emailTo.length() == 0) {
                        emailTo = packet.getTo().getNode() + "@" + packet.getTo().getDomain();
                        has_email = false;
                        } else {
                        	has_email = true;
                        	}
                    } else {
                    	has_email = true;
                    	}
                    } else {
                    	has_email = true;
                    	}
                    // The From email address
                    User userFrom = userManager.getUser(packet.getFrom().getNode());
                    emailFrom = vcardManager.getVCardProperty(userFrom.getUsername(), "EMAIL");
                    if(emailFrom == null || emailFrom.length() == 0) {
                    emailFrom = vcardManager.getVCardProperty(userFrom.getUsername(), "EMAIL:USERID");
                    if(emailFrom == null || emailFrom.length() == 0) {
                        emailFrom = userFrom.getEmail();
                        if(emailFrom == null || emailFrom.length() == 0) {
                        emailFrom = packet.getFrom().getNode() + "@" + packet.getFrom().getDomain();
                        }
                    }
                    }
                  //drlight 20.03.2018 var for website contact link
                    //urllink = vcardManager.getVCardProperty(userTo.getUsername(), "ADR:LOCALITY");
                    urllink = emailTo.substring(0,emailTo.length()-8).toString().toLowerCase();
                    
                    if (has_email) {
                    	if (emailFrom.toString().toLowerCase().indexOf("conversejs") != -1) {
                    	emailService.sendMessage(userTo.getName(), 
                    			emailTo, 
                    			userFrom.getName(), 
                    			emailFrom,
                    			"Сообщение из корпоративного мессенджера ФИЦ КНЦ РАН",
                    			"Здравствуйте!\n\nНе отвечайте на это письмо!\n\nВ корпоративном мессенджере ФИЦ КНЦ РАН" + " " + userFrom.getName() + " оставил(а) для Вас следующее сообщение:" + "\n" + dm + msg.getBody() + dm + "\n\n--\nof.ksc.ru",
                    			null);
                    	} else {
                    		emailService.sendMessage(userTo.getName(), 
                        			emailTo, 
                        			userFrom.getName(), 
                        			emailFrom,
                        			"Сообщение из корпоративного мессенджера ФИЦ КНЦ РАН",
                        			"Здравствуйте!\n\nВ корпоративном мессенджере ФИЦ КНЦ РАН" + " " + userFrom.getName() + " оставил(а) для Вас следующее сообщение:" + "\n" + dm + msg.getBody() + dm + "\n\n--\nof.ksc.ru",
                        			null);
                    	}
                    }
                    
                    
                    
                    //drlight 20.03.2018 check if resource packet string is empty or not
                   if (msg.getFrom().getResource() == null || msg.getTo().getResource() == null) {
                    	messageRouter.route(createServerMessage(packet.getFrom().getNode() + "@" + packet.getFrom().getDomain(), packet.getTo().getNode() + "@" + packet.getTo().getDomain(), emailTo, has_email, fromconversejs, urllink));
                    } else {
                    	messageRouter.route(createServerMessage(packet.getFrom().getNode() + "@" + packet.getFrom().getDomain() + "/" + packet.getFrom().getResource(), packet.getTo().getNode() + "@" + packet.getTo().getDomain() + "/" + packet.getTo().getResource(), emailTo, has_email, fromconversejs, urllink));                    
                    }
                }
            }
        } catch (UserNotFoundException e) {
        } 
       }
    }
    }
}
