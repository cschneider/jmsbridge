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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.support.JmsUtils;

public class BridgeServlet extends HttpServlet {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ConnectionPool connectionPool;

    @Override
    public void init() throws ServletException {
        connectionPool = new ConnectionPool();
        super.init();
    }

    /**
     * Forward HTTP request to a jms queue and listen on a temporary queue for the reply.
     * Connects to the jms server by using the username and password from the HTTP basic auth.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
        IOException {
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null) {
            resp.setHeader("WWW-Authenticate", "Basic realm=\"Bridge\"");
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "auth");
            return;
        }
        UserNameAndPassword auth = extractUserNamePassword(authHeader);
        PrintStream out = new PrintStream(resp.getOutputStream());
        String contextPath = req.getContextPath();
        String uri = req.getRequestURI();
        String queueName = uri.substring(contextPath.length() + 1);
        final StringBuffer reqContent = retrieveRequestContent(req.getReader());

        ConnectionFactory cf = connectionPool.getConnectionFactory(auth);
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setConnectionFactory(cf);
        jmsTemplate.setReceiveTimeout(10000);
        final Destination replyDest = connectionPool.getReplyDestination(cf, auth);
        jmsTemplate.send(queueName, new MessageCreator() {

            @Override
            public Message createMessage(Session session) throws JMSException {
                TextMessage message = session.createTextMessage(reqContent.toString());
                message.setJMSReplyTo(replyDest);
                return message;
            }

        });
        Message replyMsg = jmsTemplate.receive(replyDest);
        if (replyMsg instanceof TextMessage) {
            TextMessage replyTMessage = (TextMessage)replyMsg;
            try {
                out.print(replyTMessage.getText());
            } catch (JMSException e) {
                JmsUtils.convertJmsAccessException(e);
            }
        }
    }

    /**
     * Read the request content into a StringBuffer and return it
     * 
     * @param reader
     * @return
     * @throws IOException
     */
    private StringBuffer retrieveRequestContent(BufferedReader reader) throws IOException {
        final StringBuffer reqContent = new StringBuffer();
        String line;
        while ((line = reader.readLine()) != null) {
            reqContent.append(line);
        }
        reader.close();
        return reqContent;
    }

    /**
     * Extract username and password from the HTTP Basic auth header
     * @param auth
     * @return
     * @throws IOException
     */
    private UserNameAndPassword extractUserNamePassword(String auth) throws IOException {
        if (!auth.startsWith("Basic ")) {
            throw new SecurityException("Invalid auth scheme");
        }
        String base64Auth = auth.substring(6);
        String decodedSt = new String(Base64.decodeBase64(base64Auth));
        String[] userPasswd = decodedSt.split(":");
        String userName = userPasswd[0];
        String passwd = (userPasswd.length >= 2) ? userPasswd[1] : "";
        return new UserNameAndPassword(userName, passwd);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
        IOException {
        doGet(req, resp);
    }

}
