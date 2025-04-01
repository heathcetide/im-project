package com.cetide.im.config;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class GetHttpSessionConfig extends ServerEndpointConfig.Configurator {

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        //获取HttpSession对象
        HttpSession httpSession = (HttpSession)request.getHttpSession();
        // 将HttpSession对象放入到ServerEndpointConfig的userProperties中
        if (httpSession == null){
            throw new RuntimeException("HttpSession is null");
        }
        sec.getUserProperties().put(HttpSession.class.getName(),httpSession);
    }
}
