/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.media.control.mgcp.command;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mobicents.media.control.mgcp.command.param.NotifiedEntity;
import org.mobicents.media.control.mgcp.endpoint.MediaGroup;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpoint;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.mobicents.media.control.mgcp.exception.MgcpParseException;
import org.mobicents.media.control.mgcp.message.MessageDirection;
import org.mobicents.media.control.mgcp.message.MgcpMessageObserver;
import org.mobicents.media.control.mgcp.message.MgcpMessageParser;
import org.mobicents.media.control.mgcp.message.MgcpResponse;
import org.mobicents.media.control.mgcp.message.MgcpResponseCode;
import org.mobicents.media.control.mgcp.pkg.MgcpSignal;
import org.mobicents.media.control.mgcp.pkg.MgcpSignalProvider;
import org.mobicents.media.control.mgcp.pkg.exception.UnrecognizedMgcpPackageException;
import org.mobicents.media.control.mgcp.pkg.exception.UnsupportedMgcpSignalException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RequestNotificationCommandTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testNotificationRequest()
            throws MgcpParseException, UnrecognizedMgcpPackageException, UnsupportedMgcpSignalException {
        // given
        final StringBuilder request = new StringBuilder("RQNT 12345 mobicents/ivr/10@127.0.0.1:2427 MGCP 1.0").append("\n");
        request.append("N:restcomm@10.229.72.130:2727").append("\n");
        request.append("X:10").append("\n");
        request.append("S:AU/pa(an=http://127.0.0.1:8080/restcomm/cache/ACae6e420f/5a26d1299.wav it=1)").append("\n");
        request.append("R:AU/oc(N),AU/of(N)");
        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpMessageObserver listener = mock(MgcpMessageObserver.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final RequestNotificationCommand rqnt = new RequestNotificationCommand(endpointManager, signalProvider);

        // when
        when(endpointManager.getEndpoint("mobicents/ivr/10")).thenReturn(endpoint);
        when(endpoint.getMediaGroup()).thenReturn(mediaGroup);
        when(signalProvider.provide(eq("AU"), eq("pa"), any(Map.class), eq(mediaGroup))).thenReturn(mock(MgcpSignal.class));
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // Assert
                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
                Assert.assertNotNull(response);
                Assert.assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), response.getCode());
                return null;
            }

        }).when(listener).onMessage(any(MgcpResponse.class), eq(MessageDirection.OUTGOING));

        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // Assert
                NotificationRequest rqnt = invocation.getArgumentAt(0, NotificationRequest.class);
                int transactionId = rqnt.getTransactionId();
                String requestId = rqnt.getRequestIdentifier();
                NotifiedEntity notifiedEntity = rqnt.getNotifiedEntity();

                Assert.assertEquals(12345, transactionId);
                Assert.assertEquals("10", requestId);
                Assert.assertNotNull(notifiedEntity);
                Assert.assertEquals("restcomm@10.229.72.130:2727", notifiedEntity.toString());
                Assert.assertEquals("10", requestId);
                Assert.assertTrue(rqnt.isListening("AU/oc"));
                Assert.assertTrue(rqnt.isListening("AU/of"));
                Assert.assertEquals(1, rqnt.countSignals());
                Assert.assertNotNull(rqnt.pollSignal());
                Assert.assertNull(rqnt.pollSignal());
                return null;
            }

        }).when(endpoint).requestNotification(any(NotificationRequest.class));

        rqnt.observe(listener);
        rqnt.execute(parser.parseRequest(request.toString()));

        // then
        verify(endpoint, times(1)).requestNotification(any(NotificationRequest.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNotificationRequestWithMultipleSignals()
            throws MgcpParseException, UnrecognizedMgcpPackageException, UnsupportedMgcpSignalException {
        // given
        final StringBuilder request = new StringBuilder("RQNT 12345 mobicents/ivr/10@127.0.0.1:2427 MGCP 1.0").append("\n");
        request.append("N:restcomm@10.229.72.130:2727").append("\n");
        request.append("X:10").append("\n");
        request.append("S:AU/pa(an=http://127.0.0.1:8080/restcomm/cache/ACae6e420f/5a26d1299.wav it=1)").append(",");
        request.append("AU/pa(an=http://127.0.0.1:8080/restcomm/cache/ACfe453rb3/ea3422f11.wav it=5)").append("\n");
        request.append("R:AU/oc(N),AU/of(N)");
        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpMessageObserver listener = mock(MgcpMessageObserver.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final RequestNotificationCommand rqnt = new RequestNotificationCommand(endpointManager, signalProvider);

        // when
        when(endpointManager.getEndpoint("mobicents/ivr/10")).thenReturn(endpoint);
        when(endpoint.getMediaGroup()).thenReturn(mediaGroup);
        when(signalProvider.provide(eq("AU"), eq("pa"), any(Map.class), eq(mediaGroup))).thenReturn(mock(MgcpSignal.class));
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // Assert
                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
                Assert.assertNotNull(response);
                Assert.assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), response.getCode());
                return null;
            }

        }).when(listener).onMessage(any(MgcpResponse.class), eq(MessageDirection.OUTGOING));

        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // Assert
                NotificationRequest rqnt = invocation.getArgumentAt(0, NotificationRequest.class);
                int transactionId = rqnt.getTransactionId();
                String requestId = rqnt.getRequestIdentifier();
                NotifiedEntity notifiedEntity = rqnt.getNotifiedEntity();

                Assert.assertEquals(12345, transactionId);
                Assert.assertEquals("10", requestId);
                Assert.assertNotNull(notifiedEntity);
                Assert.assertEquals("restcomm@10.229.72.130:2727", notifiedEntity.toString());
                Assert.assertEquals("10", requestId);
                Assert.assertTrue(rqnt.isListening("AU/oc"));
                Assert.assertTrue(rqnt.isListening("AU/of"));
                Assert.assertEquals(2, rqnt.countSignals());
                Assert.assertNotNull(rqnt.pollSignal());
                Assert.assertNotNull(rqnt.pollSignal());
                Assert.assertNull(rqnt.pollSignal());
                return null;
            }

        }).when(endpoint).requestNotification(any(NotificationRequest.class));

        rqnt.observe(listener);
        rqnt.execute(parser.parseRequest(request.toString()));

        // then
        verify(endpoint, times(1)).requestNotification(any(NotificationRequest.class));
    }

    @Test
    public void testNotificationRequestWithAnyWildcard() throws MgcpParseException {
        // given
        final StringBuilder request = new StringBuilder("RQNT 12345 mobicents/ivr/$@127.0.0.1:2427 MGCP 1.0").append("\n");
        request.append("N:restcomm@10.229.72.130:2727").append("\n");
        request.append("X:10").append("\n");
        request.append("S:AU/pa(an=http://127.0.0.1:8080/restcomm/cache/ACae6e420f/5a26d1299.wav it=1)").append("\n");
        request.append("R:AU/oc(N),AU/of(N)");
        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpMessageObserver listener = mock(MgcpMessageObserver.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final RequestNotificationCommand rqnt = new RequestNotificationCommand(endpointManager, signalProvider);

        // when
        when(endpointManager.getEndpoint("mobicents/ivr/10")).thenReturn(endpoint);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // Assert
                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
                Assert.assertNotNull(response);
                Assert.assertEquals(MgcpResponseCode.WILDCARD_TOO_COMPLICATED.code(), response.getCode());
                return null;
            }

        }).when(listener).onMessage(any(MgcpResponse.class), eq(MessageDirection.OUTGOING));

        rqnt.observe(listener);
        rqnt.execute(parser.parseRequest(request.toString()));
    }

    @Test
    public void testNotificationRequestMissingRequestIdentifier() throws MgcpParseException {
        // given
        final StringBuilder request = new StringBuilder("RQNT 12345 mobicents/ivr/10@127.0.0.1:2427 MGCP 1.0").append("\n");
        request.append("N:restcomm@10.229.72.130:2727").append("\n");
        request.append("S:AU/pa(an=http://127.0.0.1:8080/restcomm/cache/ACae6e420f/5a26d1299.wav it=1)").append("\n");
        request.append("R:AU/oc(N),AU/of(N)");
        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpMessageObserver listener = mock(MgcpMessageObserver.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final RequestNotificationCommand rqnt = new RequestNotificationCommand(endpointManager, signalProvider);

        // when
        when(endpointManager.getEndpoint("mobicents/ivr/10")).thenReturn(endpoint);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // Assert
                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
                Assert.assertNotNull(response);
                Assert.assertEquals(MgcpResponseCode.PROTOCOL_ERROR.code(), response.getCode());
                return null;
            }

        }).when(listener).onMessage(any(MgcpResponse.class), eq(MessageDirection.OUTGOING));

        rqnt.observe(listener);
        rqnt.execute(parser.parseRequest(request.toString()));
    }

    @Test
    public void testNotificationWithMissingEndpoint() throws MgcpParseException {
        // given
        final StringBuilder request = new StringBuilder("RQNT 12345 mobicents/ivr/10@127.0.0.1:2427 MGCP 1.0").append("\n");
        request.append("N:restcomm@10.229.72.130:2727").append("\n");
        request.append("X:10").append("\n");
        request.append("S:AU/pa(an=http://127.0.0.1:8080/restcomm/cache/ACae6e420f/5a26d1299.wav it=1)").append("\n");
        request.append("R:AU/oc(N),AU/of(N)");
        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpMessageObserver listener = mock(MgcpMessageObserver.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final RequestNotificationCommand rqnt = new RequestNotificationCommand(endpointManager, signalProvider);

        // when
        when(endpointManager.getEndpoint("mobicents/ivr/10")).thenReturn(null);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // Assert
                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
                Assert.assertNotNull(response);
                Assert.assertEquals(MgcpResponseCode.ENDPOINT_UNKNOWN.code(), response.getCode());
                return null;
            }

        }).when(listener).onMessage(any(MgcpResponse.class), eq(MessageDirection.OUTGOING));

        rqnt.observe(listener);
        rqnt.execute(parser.parseRequest(request.toString()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNotificationRequestWithUnrecognizedSignalPackage()
            throws MgcpParseException, UnrecognizedMgcpPackageException, UnsupportedMgcpSignalException {
        // given
        final StringBuilder request = new StringBuilder("RQNT 12345 mobicents/ivr/10@127.0.0.1:2427 MGCP 1.0").append("\n");
        request.append("N:restcomm@10.229.72.130:2727").append("\n");
        request.append("X:10").append("\n");
        request.append("S:AX/pa(an=http://127.0.0.1:8080/restcomm/cache/ACae6e420f/5a26d1299.wav it=1)").append("\n");
        request.append("R:AX/oc(N),AX/of(N)");
        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpMessageObserver listener = mock(MgcpMessageObserver.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final RequestNotificationCommand rqnt = new RequestNotificationCommand(endpointManager, signalProvider);

        // when
        when(endpointManager.getEndpoint("mobicents/ivr/10")).thenReturn(endpoint);
        when(endpoint.getMediaGroup()).thenReturn(mediaGroup);
        when(signalProvider.provide(eq("AX"), eq("pa"), any(Map.class), eq(mediaGroup))).thenThrow(new UnrecognizedMgcpPackageException(""));
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // Assert
                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
                Assert.assertNotNull(response);
                Assert.assertEquals(MgcpResponseCode.UNKNOWN_PACKAGE.code(), response.getCode());
                return null;
            }

        }).when(listener).onMessage(any(MgcpResponse.class), eq(MessageDirection.OUTGOING));

        rqnt.observe(listener);
        rqnt.execute(parser.parseRequest(request.toString()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNotificationRequestWithUnrecognizedSignalType()
            throws MgcpParseException, UnrecognizedMgcpPackageException, UnsupportedMgcpSignalException {
        // given
        final StringBuilder request = new StringBuilder("RQNT 12345 mobicents/ivr/10@127.0.0.1:2427 MGCP 1.0").append("\n");
        request.append("N:restcomm@10.229.72.130:2727").append("\n");
        request.append("X:10").append("\n");
        request.append("S:AU/xyz(an=http://127.0.0.1:8080/restcomm/cache/ACae6e420f/5a26d1299.wav it=1)").append("\n");
        request.append("R:AU/oc(N),AU/of(N)");
        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpMessageObserver listener = mock(MgcpMessageObserver.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final RequestNotificationCommand rqnt = new RequestNotificationCommand(endpointManager, signalProvider);

        // when
        when(endpointManager.getEndpoint("mobicents/ivr/10")).thenReturn(endpoint);
        when(endpoint.getMediaGroup()).thenReturn(mediaGroup);
        when(signalProvider.provide(eq("AU"), eq("xyz"), any(Map.class), eq(mediaGroup))).thenThrow(new UnsupportedMgcpSignalException(""));
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // Assert
                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
                Assert.assertNotNull(response);
                Assert.assertEquals(MgcpResponseCode.NO_SUCH_EVENT_OR_SIGNAL.code(), response.getCode());
                return null;
            }

        }).when(listener).onMessage(any(MgcpResponse.class), eq(MessageDirection.OUTGOING));

        rqnt.observe(listener);
        rqnt.execute(parser.parseRequest(request.toString()));
    }

    @Test
    public void testNotificationRequestWithUnrecognizedPackageOnEvent() throws MgcpParseException {
        // given
        final StringBuilder request = new StringBuilder("RQNT 12345 mobicents/ivr/10@127.0.0.1:2427 MGCP 1.0").append("\n");
        request.append("N:restcomm@10.229.72.130:2727").append("\n");
        request.append("X:10").append("\n");
        request.append("S:AU/pa(an=http://127.0.0.1:8080/restcomm/cache/ACae6e420f/5a26d1299.wav it=1)").append("\n");
        request.append("R:XYZ/oc(N),AU/of(N)");
        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpMessageObserver listener = mock(MgcpMessageObserver.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final RequestNotificationCommand rqnt = new RequestNotificationCommand(endpointManager, signalProvider);

        // when
        when(endpointManager.getEndpoint("mobicents/ivr/10")).thenReturn(endpoint);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // Assert
                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
                Assert.assertNotNull(response);
                Assert.assertEquals(MgcpResponseCode.UNKNOWN_PACKAGE.code(), response.getCode());
                return null;
            }

        }).when(listener).onMessage(any(MgcpResponse.class), eq(MessageDirection.OUTGOING));

        rqnt.observe(listener);
        rqnt.execute(parser.parseRequest(request.toString()));
    }

    @Test
    public void testNotificationRequestWithUnrecognizedEvent() throws MgcpParseException {
        // given
        final StringBuilder request = new StringBuilder("RQNT 12345 mobicents/ivr/10@127.0.0.1:2427 MGCP 1.0").append("\n");
        request.append("N:restcomm@10.229.72.130:2727").append("\n");
        request.append("X:10").append("\n");
        request.append("S:AU/pa(an=http://127.0.0.1:8080/restcomm/cache/ACae6e420f/5a26d1299.wav it=1)").append("\n");
        request.append("R:AU/xyz(N),AU/of(N)");
        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpMessageObserver listener = mock(MgcpMessageObserver.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final RequestNotificationCommand rqnt = new RequestNotificationCommand(endpointManager, signalProvider);

        // when
        when(endpointManager.getEndpoint("mobicents/ivr/10")).thenReturn(endpoint);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // Assert
                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
                Assert.assertNotNull(response);
                Assert.assertEquals(MgcpResponseCode.NO_SUCH_EVENT_OR_SIGNAL.code(), response.getCode());
                return null;
            }

        }).when(listener).onMessage(any(MgcpResponse.class), eq(MessageDirection.OUTGOING));

        rqnt.observe(listener);
        rqnt.execute(parser.parseRequest(request.toString()));
    }

    @Test
    public void testNotificationRequestWithUnrecognizedAction() throws MgcpParseException {
        // given
        final StringBuilder request = new StringBuilder("RQNT 12345 mobicents/ivr/10@127.0.0.1:2427 MGCP 1.0").append("\n");
        request.append("N:restcomm@10.229.72.130:2727").append("\n");
        request.append("X:10").append("\n");
        request.append("S:AU/pa(an=http://127.0.0.1:8080/restcomm/cache/ACae6e420f/5a26d1299.wav it=1)").append("\n");
        request.append("R:AU/oc(XYZ),AU/of(N)");
        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpMessageObserver listener = mock(MgcpMessageObserver.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final RequestNotificationCommand rqnt = new RequestNotificationCommand(endpointManager, signalProvider);

        // when
        when(endpointManager.getEndpoint("mobicents/ivr/10")).thenReturn(endpoint);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // Assert
                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
                Assert.assertNotNull(response);
                Assert.assertEquals(MgcpResponseCode.EVENT_OR_SIGNAL_PARAMETER_ERROR.code(), response.getCode());
                return null;
            }

        }).when(listener).onMessage(any(MgcpResponse.class), eq(MessageDirection.OUTGOING));

        rqnt.observe(listener);
        rqnt.execute(parser.parseRequest(request.toString()));
    }

    @Test
    public void testNotificationRequestWithMalformedRequestedEvents() throws MgcpParseException {
        // given
        final StringBuilder request = new StringBuilder("RQNT 12345 mobicents/ivr/10@127.0.0.1:2427 MGCP 1.0").append("\n");
        request.append("N:restcomm@10.229.72.130:2727").append("\n");
        request.append("X:10").append("\n");
        request.append("S:AU/pa(an=http://127.0.0.1:8080/restcomm/cache/ACae6e420f/5a26d1299.wav it=1)").append("\n");
        request.append("R:AU/oc(N),AU/of(N");
        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpMessageObserver listener = mock(MgcpMessageObserver.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final RequestNotificationCommand rqnt = new RequestNotificationCommand(endpointManager, signalProvider);

        // when
        when(endpointManager.getEndpoint("mobicents/ivr/10")).thenReturn(endpoint);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // Assert
                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
                Assert.assertNotNull(response);
                Assert.assertEquals(MgcpResponseCode.PROTOCOL_ERROR.code(), response.getCode());
                return null;
            }

        }).when(listener).onMessage(any(MgcpResponse.class), eq(MessageDirection.OUTGOING));

        rqnt.observe(listener);
        rqnt.execute(parser.parseRequest(request.toString()));
    }

    @Test
    public void testNotificationRequestWithUnexpectedError() throws MgcpParseException {
        // given
        final StringBuilder request = new StringBuilder("RQNT 12345 mobicents/ivr/10@127.0.0.1:2427 MGCP 1.0").append("\n");
        request.append("N:restcomm@10.229.72.130:2727").append("\n");
        request.append("X:10").append("\n");
        request.append("S:AU/xyz(an=http://127.0.0.1:8080/restcomm/cache/ACae6e420f/5a26d1299.wav it=1)").append("\n");
        request.append("R:AU/oc(N),AU/of(N)");
        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpMessageObserver listener = mock(MgcpMessageObserver.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final RequestNotificationCommand rqnt = new RequestNotificationCommand(endpointManager, signalProvider);

        // when
        when(endpointManager.getEndpoint("mobicents/ivr/10")).thenThrow(new RuntimeException());
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // Assert
                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
                Assert.assertNotNull(response);
                Assert.assertEquals(MgcpResponseCode.PROTOCOL_ERROR.code(), response.getCode());
                return null;
            }

        }).when(listener).onMessage(any(MgcpResponse.class), eq(MessageDirection.OUTGOING));

        rqnt.observe(listener);
        rqnt.execute(parser.parseRequest(request.toString()));
    }

}
