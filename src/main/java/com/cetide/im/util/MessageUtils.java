package com.cetide.im.util;

import com.alibaba.fastjson.JSON;
import com.cetide.im.model.ResultMessage;

public class MessageUtils {
    public static String getMessage(boolean isSystemMessage, String fromName, Object message){

        ResultMessage resultMessage = new ResultMessage();
        resultMessage.setSystem(isSystemMessage);
        resultMessage.setMessage(message);
        if (fromName != null){
            resultMessage.setFromName(fromName);
        }
        return JSON.toJSONString(resultMessage);
    }
}
