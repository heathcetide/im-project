package com.cetide.im.ws;

import com.alibaba.fastjson.JSON;
import com.cetide.im.config.GetHttpSessionConfig;
import com.cetide.im.model.Message;
import com.cetide.im.util.MessageUtils;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/chat", configurator = GetHttpSessionConfig.class)
@Component
public class ChatEndpoint {

    private static final String SYSTEM_NAME = "系统";
    private static final String SYSTEM_MESSAGE = "欢迎来到聊天室";
    private static final String SYSTEM_MESSAGE_LEAVE = "用户离开";
    private static final String SYSTEM_MESSAGE_ENTER = "用户进入";
    private static final String SYSTEM_MESSAGE_ERROR = "系统错误";
    private static final String SYSTEM_MESSAGE_RECEIVE = "收到消息";
    private static final String SYSTEM_MESSAGE_SEND = "发送消息";
    private static final String SYSTEM_MESSAGE_SEND_ERROR = "发送消息失败";
    private static final String SYSTEM_MESSAGE_SEND_SUCCESS = "发送消息成功";
    private static final String SYSTEM_MESSAGE_SEND_TO_USER_ERROR = "发送消息给用户失败";
    private static final String SYSTEM_MESSAGE_SEND_TO_USER_SUCCESS = "发送消息给用户成功";
    private static final String SYSTEM_MESSAGE_SEND_TO_ALL_ERROR = "发送消息给所有人失败";
    private static final String SYSTEM_MESSAGE_SEND_TO_ALL_SUCCESS = "发送消息给所有人成功";
    private static final String SYSTEM_MESSAGE_SEND_TO_ = "发送消息给";
    private static final String SYSTEM_MESSAGE_SEND_TO_ALL = "发送消息给所有人";
    private static final String SYSTEM_MESSAGE_SEND_TO_USER = "发送消息给用户";

    private HttpSession httpSession;

    /**
     * ConcurrentHashMap线程安全的hashmap用来存储用户信息
     */
    private static final Map<String, Session> onlineUsers = new ConcurrentHashMap<>();

    /**
     * 连接建立成功调用的方法
     *
     * @param session
     */
    @OnOpen
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        //1. 将session进行保存
        this.httpSession = (HttpSession) endpointConfig.getUserProperties().get(HttpSession.class.getName());
        onlineUsers.put((String) this.httpSession.getAttribute("userName"), session);
        //2. 广播消息，需要将登录的所有用户推送给用户
        String message = MessageUtils.getMessage(true, null, getFriends());
        broadcastAllUsers(message);
    }

    private Set getFriends() {
        return onlineUsers.keySet();
    }

    //session.setAttribute("userName",user.getUserName（）)
    private void broadcastAllUsers(String message) {
        onlineUsers.forEach((userName, session) -> {
            session.getAsyncRemote().sendText(message);
        });
        try {
            Set<Map.Entry<String, Session>> entries = onlineUsers.entrySet();
            for (Map.Entry<String, Session> entry : entries) {
                //获取到所有用户对应的session兑现
                Session session = entry.getValue();
                //发送消息
                session.getBasicRemote().sendText(message);
            }
        } catch (Exception e) {
            System.out.println(SYSTEM_MESSAGE_SEND_TO_ALL_ERROR);
        }
    }


    /**
     * 收到客户端消息后调用的方法
     *
     * @param message
     */
    @OnMessage
    public void onMessage(String message) {
        try {
            //1. 将消息推送给指定的用户
            Message parse = JSON.parseObject(message, Message.class);

            String toName = parse.getToName();
            String msg = parse.getMessage();
            //2. 获取消息接收方的用户名
            Session objSession = onlineUsers.get(toName);
            String toMsg = MessageUtils.getMessage(false, (String) this.httpSession.getAttribute("userName"), msg);
            objSession.getBasicRemote().sendText(toMsg);
        } catch (Exception e) {
            System.out.println(SYSTEM_MESSAGE_SEND_TO_USER_ERROR);
        }
    }

    /**
     * 连接关闭调用的方法
     *
     * @param session
     */
    @OnClose
    public void onClose(Session session) {
        //1. 移除用户
        onlineUsers.remove(this.httpSession.getAttribute("userName"));
        //2. 广播消息
        String message = MessageUtils.getMessage(true, null, getFriends());
        broadcastAllUsers(message);
    }
}
