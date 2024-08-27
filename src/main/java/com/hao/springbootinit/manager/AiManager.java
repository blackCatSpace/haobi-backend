package com.hao.springbootinit.manager;

import com.yupi.yucongming.dev.client.YuCongMingClient;
import com.yupi.yucongming.dev.common.BaseResponse;
import com.yupi.yucongming.dev.model.DevChatRequest;
import com.yupi.yucongming.dev.model.DevChatResponse;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
@Service
public class AiManager {
    @Resource
    private YuCongMingClient client;

    public String doChat(Long modelId, String message) {
        // 构造请求
        DevChatRequest devChatRequest = new DevChatRequest();
//        devChatRequest.setModelId(1651468516836098050L);
        devChatRequest.setModelId(modelId);
        devChatRequest.setMessage(message);

        // 获取响应
        BaseResponse<DevChatResponse> response = client.doChat(devChatRequest);
        return response.getData().getContent();
    }

}
