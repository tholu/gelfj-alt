package org.graylog2.logging;

import java.io.IOException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.graylog2.GelfAMQPSender;
import org.graylog2.GelfSender;
import org.graylog2.GelfTCPSender;
import org.graylog2.GelfUDPSender;

public class GelfSenderFactory {
	public GelfSender createSender(SenderConfiguration configuration) {
		GelfSender gelfSender = null;
		if (configuration.getGraylogHost() == null && configuration.getAmqpURI() == null) {
			throw new GelfSenderConfigurationException("Graylog2 hostname and amqp uri are empty!");
		}
		if (configuration.getGraylogHost() != null && configuration.getAmqpURI() != null) {
			throw new GelfSenderConfigurationException("Graylog2 hostname and amqp uri are both informed!");
		}
		try {
			if (configuration.getGraylogHost().startsWith("tcp:")) {
				String tcpGraylogHost = configuration.getGraylogHost().substring(4,
						configuration.getGraylogHost().length());
				gelfSender = new GelfTCPSender(tcpGraylogHost, configuration.getGraylogPort());
			} else if (configuration.getGraylogHost().startsWith("udp:")) {
				String udpGraylogHost = configuration.getGraylogHost().substring(4,
						configuration.getGraylogHost().length());
				gelfSender = new GelfUDPSender(udpGraylogHost, configuration.getGraylogPort());
			} else if (configuration.getAmqpURI() != null) {
				gelfSender = new GelfAMQPSender(configuration.getAmqpURI(), configuration.getAmqpExchangeName(),
						configuration.getAmqpRoutingKey(), configuration.getAmqpMaxRetries());
			} else {
				gelfSender = new GelfUDPSender(configuration.getGraylogHost(), configuration.getGraylogPort());
			}
			return gelfSender;
		} catch (UnknownHostException e) {
			throw new GelfSenderConfigurationException("Unknown Graylog2 hostname:" + configuration.getGraylogHost(),
					e);
		} catch (SocketException e) {
			throw new GelfSenderConfigurationException("Socket exception", e);
		} catch (IOException e) {
			throw new GelfSenderConfigurationException("IO exception", e);
		} catch (URISyntaxException e) {
			throw new GelfSenderConfigurationException("AMQP uri exception", e);
		} catch (NoSuchAlgorithmException e) {
			throw new GelfSenderConfigurationException("AMQP algorithm exception", e);
		} catch (KeyManagementException e) {
			throw new GelfSenderConfigurationException("AMQP key exception", e);
		} catch (Exception e) {
			throw new GelfSenderConfigurationException("Unknown exception while configuring GelfSender", e);
		}
	}
}
