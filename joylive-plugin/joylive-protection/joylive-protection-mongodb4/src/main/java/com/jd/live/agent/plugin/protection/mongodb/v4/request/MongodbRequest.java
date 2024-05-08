/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.plugin.protection.mongodb.v4.request;

import com.jd.live.agent.bootstrap.util.AttributeAccessorSupport;
import com.jd.live.agent.governance.request.DbRequest.SQLRequest;
import com.mongodb.ServerAddress;

public class MongodbRequest extends AttributeAccessorSupport implements SQLRequest {

    private final ServerAddress serverAddress;
    private final String database;

    public MongodbRequest(ServerAddress serverAddress, String database) {
        this.serverAddress = serverAddress;
        this.database = database;
    }

    @Override
    public String getHost() {
        return serverAddress == null ? null : serverAddress.getHost();
    }

    @Override
    public int getPort() {
        return serverAddress == null ? 0 : serverAddress.getPort();
    }

    @Override
    public String getDatabase() {
        return database;
    }

    @Override
    public String getSql() {
        return null;
    }

    @Override
    public boolean isWrite() {
        return true;
    }
}
