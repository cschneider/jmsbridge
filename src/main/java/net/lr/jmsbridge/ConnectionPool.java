/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.lr.jmsbridge;

import java.util.HashMap;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.SessionCallback;

/**
 * Caches one connection per combination of username and password
 */
public class ConnectionPool {
    private Map<UserNameAndPassword, ConnectionFactory> connectionFactoryMap;
    private Map<UserNameAndPassword, Destination> replyDestMap;

    public ConnectionPool() {
        connectionFactoryMap = new HashMap<UserNameAndPassword, ConnectionFactory>();
        replyDestMap = new HashMap<UserNameAndPassword, Destination>();
    }

    public ConnectionFactory getConnectionFactory(UserNameAndPassword auth) {
        ConnectionFactory connectionFactory = connectionFactoryMap.get(auth);
        if (connectionFactory != null) {
            return connectionFactory;
        }
        try {
            Context context = new InitialContext();
            Context envContext = (Context)context.lookup("java:comp/env");
            ConnectionFactory innerConnectionFactory = (ConnectionFactory)envContext.lookup("jms/ConnectionFactory");
            UserCredentialsConnectionFactoryAdapter credConnectionFactory = new UserCredentialsConnectionFactoryAdapter();
            credConnectionFactory.setTargetConnectionFactory(innerConnectionFactory);
            credConnectionFactory.setUsername(auth.getUserName());
            credConnectionFactory.setPassword(auth.getPassword());
            connectionFactory = new SingleConnectionFactory(innerConnectionFactory);
            connectionFactoryMap.put(auth, connectionFactory);
            return connectionFactory;
        } catch (NamingException e) {
            throw new RuntimeException("Could not find Connectionfactory in JNDI", e);
        }
    }

    public Destination getReplyDestination(ConnectionFactory connectionFactory, UserNameAndPassword auth) {
        Destination replyDest = replyDestMap.get(auth);
        if (replyDest != null) {
            return replyDest;
        }
        
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setConnectionFactory(connectionFactory);
        replyDest = (Destination)jmsTemplate.execute(new SessionCallback() {

            @Override
            public Object doInJms(Session session) throws JMSException {
                return session.createTemporaryQueue();
            }

        });
        replyDestMap.put(auth, replyDest);
        return replyDest;
    }
}
