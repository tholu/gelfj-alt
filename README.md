# GELFJ-ALT - A GELF Appender for Log4j and a GELF Handler for JDK Logging

## What is GELFJ-ALT

It's a GELF implementation in pure Java with the Log4j appender and JDK Logging Handler. It has following features:
 * UDP, TCP, HTTP and AMQP transports
 * Large message support (chunked compressed messages)
 * Configurable buffer sizes and timeouts
 * Network failure tolerant (a fault barrier and retry mechanism)
 * Custom field extraction from messages (extracted using reflection)
 * Static custom fields (system properties and default log system configuration options)
 * Asynchronous sending (configurable queue size and timeout)
 * Configuration using System properties

gelfj-alt is based on gelfj

## How to use GELFJ-ALT

Drop the latest JAR into your classpath and configure your logging system to use it.

## Downloading

Add the following dependency section to your pom.xml:

    <dependencies>
      ...
      <dependency>
        <groupId>org.graylog2</groupId>
        <artifactId>gelfj-alt</artifactId>
        <version>2.0.0</version>
        <scope>compile</scope>
      </dependency>
      ...
    </dependencies>

## Configuration options

Following configuration options are supported:

- **targetURI**: Target uri where client sends the GELF messages to. Currently supported schemes are TCP, UDP, HTTP and AMQP
- **originHost**: Name of the originating host; defaults to the local hostname (*optional*)
- **extractStacktrace** (true/false): Add stacktraces to the GELF message; default true (*optional*)
- **addExtendedInformation** (true/false): Add extended information like Log4j's NDC/MDC; default false (*optional*)
- **fieldExtractor** (class name): Field extractor, which is used to extract additional fields from log events / records; default org.graylog2.field.ReflectionFieldExtractor
- **includeLocation** (true/false): Include caller location. Generating caller location information is relatively slow and should be avoided unless execution speed is not an issue; default false (*optional*)
- **reenableTimeout** (integer): Timeout in milliseconds for setting open circuit breaker in half-open state; default 1000 (*optional*)
- **errorCountThreshold** (integer): Maximum number of errors before opening circuit breaker; default 5 (*optional*)
- **maxRetries** (integer): Maximum number of send retries before giving up with error; default 5 (*optional*)
- **threaded** (true/false): Dispatch messages asynchronously using dedicated sender thread; default false (*optional*)
- **threadedQueueMaxDepth** (integer): Maximum queue depth; default 1000 (*optional*)
- **threadedQueueTimeout** (integer): Timeout in milliseconds for waiting free space in the queue; default 100 (*optional*)

### targetURI format

**UDP**

    udp://<host>[:<port>][?sendBufferSize=<size>&sendTimeout=<timeout in ms>]

**TCP**

    tcp://<host>[:<port>][?sendBufferSize=<size>&sendTimeout=<timeout in ms>&keepalive=<true/false>]

**HTTP**

    http://<host>[:<port>][/path][?sendBufferSize=<size>&sendTimeout=<timeout in ms>]

**AMQP**

    http://<host>[:<port>][?exchange=<exchange name>&routingKey=<routing key>]

## Log4j

### Appender

GelfAppender will use the log message as a short message and a stacktrace (if exception available) as a long message if "extractStacktrace" is true.

To use GELF Facility as appender in Log4j (XML configuration format):

    <appender name="graylog2" class="org.graylog2.log.GelfAppender">
        <param name="targetURI" value="udp://192.168.0.201:12001"/>
        <param name="originHost" value="my.machine.example.com"/>
        <param name="extractStacktrace" value="true"/>
        <param name="addExtendedInformation" value="true"/>
        <param name="Threshold" value="INFO"/>
        <param name="fields" value="environment=DEV, application=MyAPP"/>
    </appender>

and then add it as a one of appenders:

    <root>
        <priority value="INFO"/>
        <appender-ref ref="graylog2"/>
    </root>

Or, in the log4j.properties format:

    # Define the graylog2 destination
    log4j.appender.graylog2=org.graylog2.log.GelfAppender
    log4j.appender.graylog2.targetURI=tcp://graylog2.example.com:12001
    log4j.appender.graylog2.originHost=my.machine.example.com
    log4j.appender.graylog2.extractStacktrace=true
    log4j.appender.graylog2.addExtendedInformation=true
    log4j.appender.graylog2.fields=environment=DEV, application=MyAPP

    # Send all INFO logs to graylog2
    log4j.rootLogger=INFO, graylog2
    
### Layout

Configured via log4j.properties:

    # Define the console destination
    log4j.appender.console=org.apache.log4j.ConsoleAppender
    log4j.appender.console.layout=org.graylog2.log.GelfLayout
    log4j.appender.console.layout.originHost=my.machine.example.com
    log4j.appender.console.layout.extractStacktrace=true
    log4j.appender.console.layout.addExtendedInformation=true
    log4j.appender.console.layout.fields=environment=DEV, application=MyAPP

    # Send all INFO logs to console
    log4j.rootLogger=INFO, console'
    
### Sending custom fields

Sending custom fields using default reflection based FieldExtrator is very simple:

    Logger logger = Logger.getLogger("org.example");
    Map<String, Object> fields = Collections.<String, Object>singletonMap("applicationName", "LOG4JTest");
    logger.info(new Message("This message contains additional fields", fields));
	
    public static class Message {
        private String description;
        private Map<String, Object> fields;
		
        public Message(String description, Map<String, Object> fields) {
            this.description = description;
            this.fields = fields;
        }
		
        public Map<String, Object> getFields() {
            return fields;
        }
		
        @Override
        public String toString() {
            return description;
        }
        }

    
## Log4j2

### Appender

GelfAppender will use the log message as a short message and a stacktrace (if exception available) as a long message if "extractStacktrace" is true.

    # Define the graylog2 destination
    appender.graylog2.type=Gelf
    appender.graylog2.name=graylog2
    appender.graylog2.targetURI=tcp://graylog2.example.com:12001
    appender.graylog2.layout.type=ExtGelfLayout
    appender.graylog2.layout.originHost=my.machine.example.com
    appender.graylog2.layout.extractStacktrace=true

    appenders=graylog2
    
    # Send all INFO logs to graylog2
    rootLogger.level=info
    rootLogger.appenderRefs=graylog2
    rootLogger.appenderRef.graylog2.ref=graylog2
    
### Sending custom fields

    Logger logger = LogManager.getLogger("org.example");
    Map<String, Object> fields = Collections.<String, Object>singletonMap("applicationName", "LOG4J2Test");
    Message msg = new ExtendedMessageFormatMessage(fields, "This message contains additional fields");
    logger.info(msg);
	
    public static class ExtendedMessageFormatMessage extends MessageFormatMessage {
        private Map<String, Object> fields;

        public ExtendedMessageFormatMessage(Map<String, Object> fields, String messagePattern, Object... parameters) {
            super(messagePattern, parameters);
            this.fields = fields;
        }

        public Map<String, ? extends Object> getFields() {
            return fields;
        }
    }


## Java util logging

### Handler

Configured via properties as a standard Handler like

    handlers = org.graylog2.logging.GelfHandler

    .level = ALL

    org.graylog2.logging.GelfHandler.level = ALL
    org.graylog2.logging.GelfHandler.targetURI = udp://syslog.example.com:12201
    #org.graylog2.logging.GelfHandler.extractStacktrace = true
    #org.graylog2.logging.GelfHandler.additionalField.0 = foo=bah
    #org.graylog2.logging.GelfHandler.additionalField.1 = foo2=bah2

    .handlers=org.graylog2.logging.GelfHandler
    
### Formatter

Configured via properties

    handlers = java.util.logging.ConsoleHandler
    
    java.util.logging.ConsoleHandler.formatter=org.graylog2.logging.GelfFormatter
    
    org.graylog2.logging.GelfFormatter.extractStacktrace = true
    org.graylog2.logging.GelfFormatter.additionalField.0 = foo=bar
    org.graylog2.logging.GelfFormatter.additionalField.1 = bar=heck
    org.graylog2.logging.GelfFormatter.facility = test
    
### Sending custom fields

    Logger logger = Logger.getLogger("org.example");
    Map<String, Object> fields = Collections.<String, Object>singletonMap("applicationName", "JULTest");
    logger.log(new ExtendedLogRecord(Level.INFO, "This message contains additional fields", fields));
	
    public static class ExtendedLogRecord extends LogRecord {
        private static final long serialVersionUID = 1L;
        private Map<String, Object> fields;

        public ExtendedLogRecord(Level level, String msg, Map<String, Object> fields) {
            super(level, msg);
            this.fields = fields;
        }
		
        public Map<String, Object> getFields() {
            return fields;
        }
    }
