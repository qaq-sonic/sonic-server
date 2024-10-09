/*
 *   sonic-server  Sonic Cloud Real Machine Platform.
 *   Copyright (C) 2022 SonicCloudOrg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.cloud.sonic.controller.transport;

import com.alibaba.fastjson.JSONObject;
import org.cloud.sonic.controller.tools.SpringTool;
import org.springframework.web.client.RestTemplate;

import java.util.List;

public class TransportWorker {
    private static RestTemplate restTemplate = SpringTool.getBean(RestTemplate.class);

    public static void send(int agentId, JSONObject jsonObject) {
        restTemplate.postForEntity(
                String.format("http://localhost:80/exchange/send?id=%d", agentId),
                jsonObject, JSONObject.class);
    }
}
