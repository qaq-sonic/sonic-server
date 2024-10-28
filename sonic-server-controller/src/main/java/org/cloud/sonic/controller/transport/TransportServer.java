package org.cloud.sonic.controller.transport;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cloud.sonic.controller.config.WsEndpointConfigure;
import org.cloud.sonic.controller.models.domain.Agents;
import org.cloud.sonic.controller.models.dto.ElementsDTO;
import org.cloud.sonic.controller.models.dto.StepsDTO;
import org.cloud.sonic.controller.models.interfaces.AgentStatus;
import org.cloud.sonic.controller.models.interfaces.ConfType;
import org.cloud.sonic.controller.services.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@RequiredArgsConstructor
@Component
@ServerEndpoint(value = "/agent/{agentKey}", configurator = WsEndpointConfigure.class)
public class TransportServer {
    private static final ConcurrentMap<Integer, Session> agentSessionMap = new ConcurrentHashMap<>();
    private final AgentsService agentsService;
    private final DevicesService devicesService;
    private final ResultsService resultsService;
    private final ResultDetailService resultDetailService;
    private final TestCasesService testCasesService;
    private final ConfListService confListService;

    public void send(int id, JSONObject jsonObject) {
        Session agentSession = agentSessionMap.get(id);
        if (agentSession != null) {
            sendText(agentSession, jsonObject.toJSONString());
        }
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("agentKey") String agentKey) throws IOException {
        log.info("Session: {} is requesting auth server.", session.getId());
        if (agentKey == null || agentKey.isEmpty()) {
            log.info("Session: {} missing key.", session.getId());
            session.close();
            return;
        }
        Agents authResult = agentsService.auth(agentKey, devicesService);
        if (authResult == null) {
            log.info("Session: {} auth failed...", session.getId());
            JSONObject auth = new JSONObject();
            auth.put("msg", "auth");
            auth.put("result", "fail");
            sendText(session, auth.toJSONString());
            session.close();
            return;
        }
        log.info("Session: {} auth successful!", session.getId());
        JSONObject auth = new JSONObject();
        auth.put("msg", "auth");
        auth.put("result", "pass");
        auth.put("id", authResult.getId());
        auth.put("highTemp", authResult.getHighTemp());
        auth.put("highTempTime", authResult.getHighTempTime());
        auth.put("remoteTimeout", confListService.searchByKey(ConfType.REMOTE_DEBUG_TIMEOUT).getContent());
        sendText(session, auth.toJSONString());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        JSONObject jsonMsg = JSON.parseObject(message);
        log.info("Session :{} send message: {}", session.getId(), jsonMsg);

        String msgType = jsonMsg.getString("msg");

        switch (msgType) {
            case "ping": {
                JSONObject pong = new JSONObject();
                pong.put("msg", "pong");
                sendText(session, pong.toJSONString());
                break;
            }
            case "battery": {
                devicesService.refreshDevicesBattery(jsonMsg);
                break;
            }
            case "debugUser":
                devicesService.updateDevicesUser(jsonMsg);
                break;
            case "heartBeat":
                Agents agentsOnline = agentsService.findById(jsonMsg.getInteger("agentId"));
                if (agentsOnline.getStatus() != AgentStatus.ONLINE) {
                    agentsOnline.setStatus(AgentStatus.ONLINE);
                    agentsService.saveAgents(agentsOnline);
                }
                break;
            case "agentInfo":
                agentSessionMap.put(jsonMsg.getInteger("agentId"), session);
                jsonMsg.remove("msg");
                agentsService.saveAgents(jsonMsg);
                break;
            case "subResultCount":
                resultsService.subResultCount(jsonMsg.getInteger("rid"));
                break;
            case "deviceDetail":
                devicesService.deviceStatus(jsonMsg);
                break;
            case "step":
            case "perform":
            case "record":
            case "status":
                resultDetailService.saveByTransport(jsonMsg, resultsService);
                break;
            case "findSteps":
                JSONObject steps = findSteps(jsonMsg, "runStep");
                sendText(session, steps.toJSONString());
                break;
            case "errCall":
                agentsService.errCall(jsonMsg.getInteger("agentId"), jsonMsg.getString("udId"), jsonMsg.getInteger("tem"), jsonMsg.getInteger("type"));
                break;
            case "generateStep":
                JSONObject step = generateStep(jsonMsg, "runStep");
                sendText(session, step.toJSONString());
                break;
        }
    }

    private void sendText(Session session, String message) {
        if (session == null || !session.isOpen()) {
            return;
        }
        synchronized (session) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IllegalStateException | IOException e) {
                log.error("WebSocket send msg failed: {}", e.getMessage());
            }
        }
    }

    /**
     * 查找 & 封装步骤对象
     *
     * @param jsonMsg websocket消息
     * @return 步骤对象
     */
    private JSONObject findSteps(JSONObject jsonMsg, String msg) {
        JSONObject j = testCasesService.findSteps(jsonMsg.getInteger("caseId"));
        JSONObject steps = new JSONObject();
        steps.put("cid", jsonMsg.getInteger("caseId"));
        steps.put("msg", msg);
        steps.put("pf", j.get("pf"));
        steps.put("steps", j.get("steps"));
        steps.put("gp", j.get("gp"));
        steps.put("sessionId", jsonMsg.getString("sessionId"));
        steps.put("pwd", jsonMsg.getString("pwd"));
        steps.put("udId", jsonMsg.getString("udId"));
        return steps;
    }

    /**
     * 增加元素定位时，为了方便验证定位是否有效，此时可能还没有入库，所以先生成一个临时的步骤
     *
     * @param jsonMsg
     * @param msg
     * @return
     */
    private JSONObject generateStep(JSONObject jsonMsg, String msg) {
        StepsDTO stepsDTO = new StepsDTO();
        ElementsDTO elementsDTO = new ElementsDTO();
        elementsDTO.setEleType(jsonMsg.getString("eleType"))
                .setEleValue(jsonMsg.getString("element"))
                .setEleName("定位控件测试");
        List<ElementsDTO> elements = new ArrayList<>();
        elements.add(elementsDTO);
        String stepType = jsonMsg.getString("eleType").equals("point") ? "tap" : "click";
        stepsDTO.setStepType(stepType)
                .setConditionType(0)
                .setElements(elements)
                .setPlatform(jsonMsg.getInteger("pf"));
        JSONArray step = new JSONArray();
        step.add(stepsDTO);
        JSONObject stepObj = new JSONObject();
        stepObj.put("msg", msg);
        stepObj.put("pf", jsonMsg.getInteger("pf"));
        stepObj.put("steps", step);
        stepObj.put("sessionId", jsonMsg.getString("sessionId"));
        stepObj.put("pwd", jsonMsg.getString("pwd"));
        stepObj.put("udId", jsonMsg.getString("udId"));
        return stepObj;
    }


    @OnClose
    public void onClose(Session session) {
        log.info("Agent: {} disconnected.", session.getId());
        agentSessionMap.entrySet().removeIf(entry -> {
            if (entry.getValue().equals(session)) {
                int agentId = entry.getKey();
                agentsService.offLine(agentId, devicesService);
                return true;
            }
            return false;
        });
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.info("Agent: {},on error", session.getId());
        log.error(error.getMessage());
    }
}
